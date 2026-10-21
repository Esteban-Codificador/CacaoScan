package com.cacaoscan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.cacaoscan.presentation.admin.AdminScreen
import com.cacaoscan.presentation.admin.CalidadScreen
import com.cacaoscan.presentation.admin.ModelosScreen
import com.cacaoscan.presentation.admin.MonitorScreen
import com.cacaoscan.presentation.auth.LoginScreen
import com.cacaoscan.presentation.history.HistoryDetailScreen
import com.cacaoscan.presentation.history.HistoryScreen
import com.cacaoscan.presentation.home.HomeScreen
import com.cacaoscan.presentation.info.InfoScreen
import com.cacaoscan.presentation.result.ResultScreen
import com.cacaoscan.presentation.scan.ScanScreen
import com.cacaoscan.presentation.scan.ScanViewModel
import com.cacaoscan.presentation.security.SecurityScreen

/**
 * Grafo de navegacion de CacaoScan.
 *
 *   Inicio
 *     |-- Escanear  (subgrafo: captura -> resultado)
 *     |-- Historial (lista -> detalle)
 *     |-- Informacion
 *     +-- Administracion (microservicios: modelos, calidad, monitoreo)
 *
 * Captura y resultado viven en un subgrafo propio y comparten un mismo
 * ScanViewModel, obtenido a partir de la entrada del subgrafo. Asi el resultado
 * pertenece a la sesion de escaneo y desaparece al salir de ella: no puede
 * ocurrir que el resultado de una vaina reaparezca al escanear la siguiente.
 */
@Composable
fun CacaoScanApp(navController: NavHostController = rememberNavController()) {

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = hiltViewModel(),
                onAuthenticated = {
                    // Se elimina el login de la pila: el boton Atras desde Inicio
                    // debe salir de la aplicacion, no volver a la contrasena.
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onScanClick = { navController.navigate(Routes.SCAN_GRAPH) },
                onHistoryClick = { navController.navigate(Routes.HISTORY) },
                onInfoClick = { navController.navigate(Routes.INFO) },
                onSecurityClick = { navController.navigate(Routes.SECURITY) },
                onAdminClick = { navController.navigate(Routes.ADMIN) },
            )
        }

        navigation(startDestination = Routes.SCAN, route = Routes.SCAN_GRAPH) {
            composable(Routes.SCAN) { entry ->
                val parentEntry = remember(entry) {
                    navController.getBackStackEntry(Routes.SCAN_GRAPH)
                }
                val viewModel = hiltViewModel<ScanViewModel>(parentEntry)
                ScanScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onResultReady = {
                        navController.navigate(Routes.RESULT) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.RESULT) { entry ->
                val parentEntry = remember(entry) {
                    navController.getBackStackEntry(Routes.SCAN_GRAPH)
                }
                val viewModel = hiltViewModel<ScanViewModel>(parentEntry)
                ResultScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onDone = {
                        navController.popBackStack(route = Routes.SCAN_GRAPH, inclusive = true)
                    },
                )
            }
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onScanClick = { scanId -> navController.navigate(Routes.historyDetail(scanId)) },
                onStartScan = { navController.navigate(Routes.SCAN_GRAPH) },
            )
        }

        composable(
            route = Routes.HISTORY_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_SCAN_ID) { type = NavType.LongType }),
        ) {
            HistoryDetailScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Routes.INFO) {
            InfoScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.SECURITY) {
            SecurityScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ---------------------------------------------------- administracion

        // Si el token caduco, se vuelve al login vaciando la pila: no debe
        // quedar ninguna pantalla de administracion accesible con Atras.
        val iniciarSesion: () -> Unit = {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }

        composable(Routes.ADMIN) {
            AdminScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onModelosClick = { navController.navigate(Routes.ADMIN_MODELOS) },
                onCalidadClick = { navController.navigate(Routes.ADMIN_CALIDAD) },
                onMonitorClick = { navController.navigate(Routes.ADMIN_MONITOR) },
                onIniciarSesion = iniciarSesion,
            )
        }

        composable(Routes.ADMIN_MODELOS) {
            ModelosScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onIniciarSesion = iniciarSesion,
            )
        }

        composable(Routes.ADMIN_CALIDAD) {
            CalidadScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onIniciarSesion = iniciarSesion,
            )
        }

        composable(Routes.ADMIN_MONITOR) {
            MonitorScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onIniciarSesion = iniciarSesion,
            )
        }
    }
}
