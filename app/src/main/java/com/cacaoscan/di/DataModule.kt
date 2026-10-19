package com.cacaoscan.di

import android.content.Context
import androidx.room.Room
import com.cacaoscan.data.local.db.CacaoScanDatabase
import com.cacaoscan.data.local.db.ScanDao
import com.cacaoscan.data.local.file.BitmapLoader
import com.cacaoscan.data.local.file.ImageStore
import com.cacaoscan.data.local.file.InternalImageStorage
import com.cacaoscan.data.repository.ScanRepositoryImpl
import com.cacaoscan.domain.repository.ScanRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CacaoScanDatabase =
        Room.databaseBuilder(
            context,
            CacaoScanDatabase::class.java,
            CacaoScanDatabase.NAME,
        ).build()

    @Provides
    fun provideScanDao(database: CacaoScanDatabase): ScanDao = database.scanDao()

    @Provides
    @Singleton
    fun provideImageStore(@ApplicationContext context: Context): ImageStore =
        InternalImageStorage(context)

    @Provides
    @Singleton
    fun provideBitmapLoader(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): BitmapLoader = BitmapLoader(context, ioDispatcher)

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideScanRepository(
        scanDao: ScanDao,
        imageStore: ImageStore,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        clock: Clock,
    ): ScanRepository = ScanRepositoryImpl(scanDao, imageStore, ioDispatcher, clock)
}
