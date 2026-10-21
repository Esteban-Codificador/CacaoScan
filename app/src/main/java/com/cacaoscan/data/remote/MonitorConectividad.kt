package com.cacaoscan.data.remote

import android.content.Context
import android.net.ConnectivityManager

/** Indica si el telefono tiene alguna red activa. */
fun interface MonitorConectividad {
    fun hayRed(): Boolean
}

/**
 * Implementacion sobre ConnectivityManager.
 *
 * No se exige que la red tenga salida a internet: los microservicios viven en la
 * red local, y una Wi-Fi sin internet sirve perfectamente para alcanzarlos.
 */
class MonitorConectividadAndroid(context: Context) : MonitorConectividad {

    private val conectividad =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun hayRed(): Boolean = conectividad.activeNetwork != null
}
