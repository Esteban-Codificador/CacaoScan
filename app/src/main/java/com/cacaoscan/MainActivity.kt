package com.cacaoscan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.cacaoscan.navigation.CacaoScanApp
import com.cacaoscan.presentation.theme.CacaoScanTheme
import com.cacaoscan.security.lock.ActivityHolder
import dagger.hilt.android.AndroidEntryPoint

/**
 * Unica Activity de la aplicacion.
 *
 * Toda la navegacion ocurre en Compose (Navigation Compose). Mantener una sola
 * Activity simplifica el ciclo de vida y evita duplicar la gestion de permisos
 * de camara entre pantallas.
 *
 * Hereda de FragmentActivity, y no de ComponentActivity, porque BiometricPrompt
 * lo exige para poder mostrar el dialogo de autenticacion del sistema
 * (ver com.cacaoscan.security.lock.AppLock). FragmentActivity extiende
 * ComponentActivity, de modo que setContent y Compose funcionan igual.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Necesario para que BiometricPrompt pueda mostrar el dialogo del sistema
        // sin que la Activity tenga que atravesar los ViewModels.
        ActivityHolder.attach(this)
        setContent {
            CacaoScanTheme {
                CacaoScanApp()
            }
        }
    }

    override fun onDestroy() {
        ActivityHolder.detach(this)
        super.onDestroy()
    }
}
