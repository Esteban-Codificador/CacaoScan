package com.cacaoscan.security.credentials

import java.time.Clock
import java.time.Duration
import java.util.Base64

/** Resultado de un intento de acceso. */
sealed interface LoginResult {
    data object Success : LoginResult

    /** Contrasena incorrecta. [remainingAttempts] antes del bloqueo temporal. */
    data class Wrong(val remainingAttempts: Int) : LoginResult

    /** Demasiados fallos: acceso bloqueado durante [secondsRemaining]. */
    data class Locked(val secondsRemaining: Long) : LoginResult

    /** Todavia no se ha creado ninguna contrasena. */
    data object NotConfigured : LoginResult
}

/** Almacenamiento de clave y valor. Se abstrae para poder probar sin Android. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
}

/** Cifrado simetrico de bytes. Lo implementa KeystoreCipher. */
interface ByteCipher {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(blob: ByteArray): ByteArray
}

/**
 * Credencial del administrador, con bloqueo por intentos fallidos.
 *
 * Decisiones de seguridad:
 *
 *  - La contrasena NUNCA se guarda. Se guarda su derivacion PBKDF2 con sal.
 *  - El registro completo se cifra ademas con una clave del Android Keystore,
 *    para encarecer un ataque de diccionario offline sobre el hash.
 *  - Tras varios fallos seguidos se bloquea el acceso durante un tiempo. El
 *    bloqueo se persiste: reiniciar la aplicacion no lo reinicia, que es
 *    precisamente lo que intentaria un atacante.
 *  - El contador de fallos se guarda cifrado junto al resto, de modo que
 *    tampoco puede manipularse editando las preferencias.
 */
interface AdminCredentialStore {

    /** true si ya existe una contrasena creada. */
    fun isConfigured(): Boolean

    /**
     * Crea la contrasena en el primer arranque.
     * @throws IllegalArgumentException si no cumple la longitud minima.
     */
    fun createPassword(password: CharArray)

    /** Comprueba la contrasena y actualiza el estado de bloqueo. */
    fun verify(password: CharArray): LoginResult

    /** Segundos que faltan para que expire el bloqueo, o 0 si no hay bloqueo. */
    fun lockedSecondsRemaining(): Long
}

class DefaultAdminCredentialStore(
    private val store: KeyValueStore,
    private val cipher: ByteCipher,
    private val clock: Clock,
) : AdminCredentialStore {

    override fun isConfigured(): Boolean = readRecord() != null

    override fun createPassword(password: CharArray) {
        require(password.size >= PasswordHasher.MIN_LENGTH) {
            "La contrasena debe tener al menos ${PasswordHasher.MIN_LENGTH} caracteres."
        }
        val stored = PasswordHasher.hash(password)
        writeRecord(Record(stored = stored, failedAttempts = 0, lockedUntilEpochMs = 0L))
    }

    override fun verify(password: CharArray): LoginResult {
        val record = readRecord() ?: return LoginResult.NotConfigured

        val remainingLock = secondsUntil(record.lockedUntilEpochMs)
        if (remainingLock > 0) return LoginResult.Locked(remainingLock)

        if (PasswordHasher.verify(password, record.stored)) {
            writeRecord(record.copy(failedAttempts = 0, lockedUntilEpochMs = 0L))
            return LoginResult.Success
        }

        val failures = record.failedAttempts + 1
        if (failures >= MAX_ATTEMPTS) {
            val until = clock.millis() + LOCKOUT.toMillis()
            writeRecord(record.copy(failedAttempts = 0, lockedUntilEpochMs = until))
            return LoginResult.Locked(secondsUntil(until))
        }

        writeRecord(record.copy(failedAttempts = failures))
        return LoginResult.Wrong(remainingAttempts = MAX_ATTEMPTS - failures)
    }

    override fun lockedSecondsRemaining(): Long =
        readRecord()?.let { secondsUntil(it.lockedUntilEpochMs) } ?: 0L

    private fun secondsUntil(epochMs: Long): Long {
        val diff = epochMs - clock.millis()
        return if (diff <= 0) 0L else (diff + 999) / 1000
    }

    /** Registro serializado como sal|hash|iteraciones|fallos|bloqueoHasta. */
    private data class Record(
        val stored: StoredPassword,
        val failedAttempts: Int,
        val lockedUntilEpochMs: Long,
    )

    private fun readRecord(): Record? {
        val raw = store.getString(KEY_RECORD) ?: return null
        return runCatching {
            val plain = cipher.decrypt(Base64.getDecoder().decode(raw))
            val parts = String(plain, Charsets.UTF_8).split(SEPARATOR)
            require(parts.size == 5)
            Record(
                stored = StoredPassword(
                    saltBase64 = parts[0],
                    hashBase64 = parts[1],
                    iterations = parts[2].toInt(),
                ),
                failedAttempts = parts[3].toInt(),
                lockedUntilEpochMs = parts[4].toLong(),
            )
        }.getOrNull()
    }

    private fun writeRecord(record: Record) {
        val plain = listOf(
            record.stored.saltBase64,
            record.stored.hashBase64,
            record.stored.iterations.toString(),
            record.failedAttempts.toString(),
            record.lockedUntilEpochMs.toString(),
        ).joinToString(SEPARATOR)
        val encrypted = cipher.encrypt(plain.toByteArray(Charsets.UTF_8))
        store.putString(KEY_RECORD, Base64.getEncoder().encodeToString(encrypted))
    }

    private companion object {
        const val KEY_RECORD = "admin_credential"
        const val SEPARATOR = "|"

        /** Intentos fallidos consecutivos antes de bloquear. */
        const val MAX_ATTEMPTS = 5

        /**
         * Bloqueo corto a proposito: frena la fuerza bruta sin dejar al productor
         * fuera de su propia aplicacion por un error de tecleo en campo.
         */
        val LOCKOUT: Duration = Duration.ofSeconds(60)
    }
}
