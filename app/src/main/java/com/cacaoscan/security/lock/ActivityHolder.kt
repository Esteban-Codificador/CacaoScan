package com.cacaoscan.security.lock

import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

/**
 * Referencia a la Activity visible, necesaria para mostrar el dialogo del
 * sistema de BiometricPrompt.
 *
 * Existe para que la Activity NO tenga que viajar como parametro a traves de los
 * ViewModels. Pasarla por la API de un ViewModel invita a guardarla, y una
 * Activity guardada en un ViewModel sobrevive a su propia destruccion: es una
 * fuga de memoria clasica en Android.
 *
 * Se guarda como [WeakReference] y se limpia explicitamente cuando la Activity
 * se destruye, de modo que este objeto nunca puede impedir su recoleccion.
 */
object ActivityHolder {

    private var reference: WeakReference<FragmentActivity>? = null

    fun attach(activity: FragmentActivity) {
        reference = WeakReference(activity)
    }

    fun detach(activity: FragmentActivity) {
        if (reference?.get() === activity) reference = null
    }

    /** Activity actual, o null si no hay ninguna en pantalla. */
    fun current(): FragmentActivity? = reference?.get()?.takeUnless { it.isFinishing }
}
