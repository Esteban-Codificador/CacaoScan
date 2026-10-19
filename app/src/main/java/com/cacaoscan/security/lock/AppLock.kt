package com.cacaoscan.security.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Disponibilidad de la autenticacion local en este dispositivo. */
enum class LockAvailability {
    /** Hay huella, rostro o credencial del dispositivo configurada. */
    AVAILABLE,

    /** El hardware existe pero el usuario no ha configurado ningun metodo. */
    NOT_ENROLLED,

    /** El dispositivo no puede autenticar al usuario localmente. */
    UNAVAILABLE,
}

/** Resultado de un intento de autenticacion. */
sealed interface AuthenticationResult {
    data object Succeeded : AuthenticationResult
    data object Cancelled : AuthenticationResult
    data class Failed(val message: String) : AuthenticationResult
}

/**
 * Autenticacion local del usuario (AUTENTICACION real, sin servidor).
 *
 * Usa BiometricPrompt con huella, rostro o la credencial del dispositivo
 * (PIN, patron o contrasena). Quien verifica la identidad es el sistema
 * operativo, no CacaoScan: la aplicacion nunca ve ni almacena el factor
 * biometrico ni el PIN, solo recibe un resultado de exito o fallo.
 *
 * Por que esto cumple el requisito academico de autenticacion sin romper el
 * IEEE830: no crea cuentas de usuario, no gestiona multiples usuarios, no
 * necesita servidor y funciona sin conexion. Es autenticacion de un solo
 * usuario, el dueno del dispositivo.
 *
 * Es OPCIONAL: si el usuario no tiene bloqueo configurado, la aplicacion sigue
 * siendo plenamente utilizable. El flujo de escaneo (RF01 a RF05) nunca se
 * bloquea detras de esta autenticacion.
 */
interface AppLock {

    /** Indica si el dispositivo puede autenticar localmente al usuario. */
    fun availability(): LockAvailability

    /**
     * Muestra el dialogo del sistema y espera el resultado.
     *
     * @param reason texto que explica al usuario por que se le pide identificarse.
     */
    suspend fun authenticate(reason: String): AuthenticationResult
}

/**
 * Implementacion sobre BiometricPrompt.
 *
 * Se separa de la interfaz [AppLock] para que los ViewModels puedan probarse en
 * la JVM: BiometricPrompt necesita un dispositivo real.
 */
class BiometricAppLock(private val context: Context) : AppLock {

    override fun availability(): LockAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> LockAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> LockAvailability.NOT_ENROLLED
            else -> LockAvailability.UNAVAILABLE
        }
    }

    override suspend fun authenticate(reason: String): AuthenticationResult {
        val activity = ActivityHolder.current()
            ?: return AuthenticationResult.Failed("No hay una pantalla activa donde mostrar el dialogo.")
        return promptOn(activity, reason)
    }

    private suspend fun promptOn(
        activity: FragmentActivity,
        reason: String,
    ): AuthenticationResult = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(AuthenticationResult.Succeeded)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!continuation.isActive) return
                val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                continuation.resume(
                    if (cancelled) {
                        AuthenticationResult.Cancelled
                    } else {
                        AuthenticationResult.Failed("No se pudo verificar su identidad.")
                    },
                )
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirme su identidad")
            .setSubtitle(reason)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()

        prompt.authenticate(info)
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
    }

    private companion object {
        /**
         * Se acepta tambien la credencial del dispositivo: exigir huella dejaria
         * fuera a usuarios con telefonos sin sensor o con las manos sucias por
         * el trabajo en campo, que es el escenario real de uso.
         */
        const val ALLOWED_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
