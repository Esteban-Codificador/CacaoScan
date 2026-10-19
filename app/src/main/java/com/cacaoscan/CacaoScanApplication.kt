package com.cacaoscan

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de entrada de la aplicacion y raiz del grafo de dependencias.
 *
 * CacaoScan no realiza ninguna inicializacion de red, analitica ni servicios
 * remotos: la aplicacion es completamente local (RNF02, RNF06).
 */
@HiltAndroidApp
class CacaoScanApplication : Application()
