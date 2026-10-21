package com.cacaoscan.data.remote

import android.content.Context
import com.cacaoscan.domain.model.admin.ServicioRemoto
import com.cacaoscan.security.credentials.KeyValueStore

/**
 * Direccion del equipo donde corren los microservicios.
 *
 * Es lo unico que hay que configurar. Por omision vale 10.0.2.2, que es la
 * direccion con la que el emulador de Android ve al ordenador que lo ejecuta.
 * Desde un telefono real hay que escribir la IP del ordenador en la Wi-Fi
 * (por ejemplo 192.168.1.20); operar.py la muestra al arrancar.
 */
class ConfiguracionServicios(private val almacen: KeyValueStore) {

    var anfitrion: String
        get() = almacen.getString(CLAVE_ANFITRION) ?: ANFITRION_EMULADOR
        private set(valor) = almacen.putString(CLAVE_ANFITRION, valor)

    /**
     * Guarda una nueva direccion si es valida.
     *
     * @return la direccion normalizada que se guardo, o null si no es valida.
     */
    fun cambiarAnfitrion(entrada: String): String? {
        val normalizada = normalizarAnfitrion(entrada) ?: return null
        anfitrion = normalizada
        return normalizada
    }

    fun urlBase(servicio: ServicioRemoto): String = "http://$anfitrion:${servicio.puerto}"

    companion object {
        const val ANFITRION_EMULADOR = "10.0.2.2"
        private const val CLAVE_ANFITRION = "anfitrion_microservicios"

        private val IPV4 = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
        private val NOMBRE_EQUIPO = Regex("""^[a-zA-Z0-9]([a-zA-Z0-9-]{0,62})(\.[a-zA-Z0-9-]{1,63})*$""")

        /**
         * Limpia lo que escribe quien administra y decide si es una direccion.
         *
         * Acepta que se pegue una URL completa ("http://192.168.1.20:8001/docs")
         * y se queda solo con el anfitrion, porque es el error mas probable al
         * copiarla desde el navegador. El puerto lo fija cada servicio.
         */
        fun normalizarAnfitrion(entrada: String): String? {
            var valor = entrada.trim()
            valor = valor.removePrefix("http://").removePrefix("https://")
            valor = valor.substringBefore('/').substringBefore(':')
            if (valor.isEmpty()) return null

            IPV4.matchEntire(valor)?.let { coincidencia ->
                val octetosValidos = coincidencia.groupValues.drop(1).all { it.toInt() in 0..255 }
                return if (octetosValidos) valor else null
            }
            // Si parece una IP pero no encaja (p. ej. 192.168.1), no se acepta como nombre.
            if (valor.all { it.isDigit() || it == '.' }) return null
            return if (NOMBRE_EQUIPO.matches(valor)) valor.lowercase() else null
        }
    }
}

/**
 * Preferencias propias del modulo de administracion.
 *
 * Se separan de las de seguridad (cacaoscan_security) porque aquellas solo
 * guardan contenido cifrado, y la direccion de un servidor no lo necesita.
 */
class PreferenciasAdmin(context: Context) : KeyValueStore {

    private val preferencias =
        context.getSharedPreferences("cacaoscan_admin", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferencias.getString(key, null)

    override fun putString(key: String, value: String?) {
        preferencias.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }
}
