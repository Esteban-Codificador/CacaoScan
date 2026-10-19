package com.cacaoscan.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base de datos local de CacaoScan (RNF05: el historial persiste entre sesiones).
 *
 * Vive unicamente en el dispositivo. No hay replica remota ni sincronizacion:
 * esta explicitamente fuera del alcance del proyecto y contradiria RNF06.
 */
@Database(
    entities = [ScanEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CacaoScanDatabase : RoomDatabase() {

    abstract fun scanDao(): ScanDao

    companion object {
        const val NAME = "cacaoscan.db"
    }
}
