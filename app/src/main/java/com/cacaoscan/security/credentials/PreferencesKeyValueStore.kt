package com.cacaoscan.security.credentials

import android.content.Context

/**
 * Almacenamiento en SharedPreferences privadas de la aplicacion.
 *
 * El contenido que se guarda aqui ya viaja cifrado desde
 * [DefaultAdminCredentialStore], asi que estas preferencias nunca contienen
 * texto legible. El archivo vive en el almacenamiento interno, aislado por el
 * sandbox de Android, y desaparece al desinstalar.
 */
class PreferencesKeyValueStore(context: Context) : KeyValueStore {

    private val preferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String?) {
        preferences.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    private companion object {
        const val FILE_NAME = "cacaoscan_security"
    }
}
