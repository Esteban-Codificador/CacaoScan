package com.cacaoscan.navigation

/**
 * Rutas de navegacion.
 *
 * Se usan rutas de cadena en lugar de rutas tipadas con kotlinx-serialization
 * para no anadir un plugin de compilacion mas: el grafo de CacaoScan tiene seis
 * destinos y un unico argumento, por lo que el beneficio no compensaria el coste.
 */
object Routes {
    /**
     * Acceso administrativo. Es el destino inicial del grafo: ninguna otra
     * pantalla es alcanzable sin pasar por aqui.
     */
    const val LOGIN = "login"

    const val HOME = "home"

    /**
     * Subgrafo del flujo de escaneo. Agrupa captura y resultado para que ambos
     * compartan un mismo ScanViewModel: el resultado pertenece a la sesion de
     * escaneo y no debe sobrevivir a ella.
     */
    const val SCAN_GRAPH = "scan_graph"
    const val SCAN = "scan"
    const val RESULT = "result"

    const val HISTORY = "history"
    const val INFO = "info"

    /** Pantalla de demostracion academica de seguridad. No forma parte del flujo principal. */
    const val SECURITY = "security"

    /**
     * Modulo de administracion: los dos microservicios del proyecto. Es la unica
     * zona de la aplicacion que usa la red, y ninguna pantalla del productor
     * navega hacia ella salvo el acceso explicito desde Inicio.
     */
    const val ADMIN = "admin"
    const val ADMIN_MODELOS = "admin/modelos"
    const val ADMIN_CALIDAD = "admin/calidad"
    const val ADMIN_MONITOR = "admin/monitor"

    const val ARG_SCAN_ID = "scanId"
    private const val HISTORY_DETAIL_BASE = "history_detail"
    const val HISTORY_DETAIL = "$HISTORY_DETAIL_BASE/{$ARG_SCAN_ID}"

    fun historyDetail(scanId: Long): String = "$HISTORY_DETAIL_BASE/$scanId"
}
