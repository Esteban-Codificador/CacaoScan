package com.cacaoscan.di

import android.content.Context
import com.cacaoscan.data.remote.ClienteHttp
import com.cacaoscan.data.remote.ConfiguracionServicios
import com.cacaoscan.data.remote.MonitorConectividad
import com.cacaoscan.data.remote.MonitorConectividadAndroid
import com.cacaoscan.data.remote.PreferenciasAdmin
import com.cacaoscan.data.remote.TransporteHttp
import com.cacaoscan.data.repository.MicroserviciosRepositoryImpl
import com.cacaoscan.domain.repository.MicroserviciosRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Dependencias del modulo de administracion: todo lo que toca la red.
 *
 * Se agrupa en un modulo aparte para que sea evidente donde empieza y donde
 * acaba el acceso a red. Ninguna clase del flujo del productor recibe nada de
 * lo que se provee aqui.
 */
@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    @Provides
    @Singleton
    fun provideTransporteHttp(@IoDispatcher ioDispatcher: CoroutineDispatcher): TransporteHttp =
        ClienteHttp(ioDispatcher)

    @Provides
    @Singleton
    fun provideConfiguracionServicios(@ApplicationContext context: Context): ConfiguracionServicios =
        ConfiguracionServicios(PreferenciasAdmin(context))

    @Provides
    @Singleton
    fun provideMonitorConectividad(@ApplicationContext context: Context): MonitorConectividad =
        MonitorConectividadAndroid(context)

    @Provides
    @Singleton
    fun provideMicroserviciosRepository(
        transporte: TransporteHttp,
        configuracion: ConfiguracionServicios,
        conectividad: MonitorConectividad,
    ): MicroserviciosRepository = MicroserviciosRepositoryImpl(transporte, configuracion, conectividad)
}
