package com.cacaoscan.di

import com.cacaoscan.domain.repository.ScanRepository
import com.cacaoscan.domain.usecase.ClassifyPodImageUseCase
import com.cacaoscan.domain.usecase.DeleteAllScansUseCase
import com.cacaoscan.domain.usecase.DeleteScanUseCase
import com.cacaoscan.domain.usecase.GetScanByIdUseCase
import com.cacaoscan.domain.usecase.GetScanHistoryUseCase
import com.cacaoscan.domain.usecase.ResolveScanImageUseCase
import com.cacaoscan.domain.usecase.SaveScanUseCase
import com.cacaoscan.ml.PodClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideClassifyPodImageUseCase(
        classifier: PodClassifier,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    ): ClassifyPodImageUseCase = ClassifyPodImageUseCase(classifier, defaultDispatcher)

    @Provides
    fun provideSaveScanUseCase(repository: ScanRepository): SaveScanUseCase =
        SaveScanUseCase(repository)

    @Provides
    fun provideGetScanHistoryUseCase(repository: ScanRepository): GetScanHistoryUseCase =
        GetScanHistoryUseCase(repository)

    @Provides
    fun provideGetScanByIdUseCase(repository: ScanRepository): GetScanByIdUseCase =
        GetScanByIdUseCase(repository)

    @Provides
    fun provideResolveScanImageUseCase(repository: ScanRepository): ResolveScanImageUseCase =
        ResolveScanImageUseCase(repository)

    @Provides
    fun provideDeleteScanUseCase(repository: ScanRepository): DeleteScanUseCase =
        DeleteScanUseCase(repository)

    @Provides
    fun provideDeleteAllScansUseCase(repository: ScanRepository): DeleteAllScansUseCase =
        DeleteAllScansUseCase(repository)
}
