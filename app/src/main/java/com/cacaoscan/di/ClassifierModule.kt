package com.cacaoscan.di

import android.content.Context
import com.cacaoscan.ml.ClassifierProvider
import com.cacaoscan.ml.ModelSpec
import com.cacaoscan.ml.PodClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provee el clasificador activo.
 *
 * Es singleton porque cargar un modelo y crear su interprete es costoso y no
 * debe repetirse en cada escaneo (RNF01: la clasificacion debe caber en cinco
 * segundos, y no tendria sentido gastar parte de ese presupuesto reinicializando
 * el interprete).
 */
@Module
@InstallIn(SingletonComponent::class)
object ClassifierModule {

    @Provides
    @Singleton
    fun provideModelSpec(): ModelSpec = ModelSpec()

    @Provides
    @Singleton
    fun providePodClassifier(
        @ApplicationContext context: Context,
        spec: ModelSpec,
    ): PodClassifier = ClassifierProvider(context, spec).create()
}
