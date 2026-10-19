package com.cacaoscan.presentation.common

/**
 * Estado generico de una pantalla.
 *
 * Todas las pantallas de CacaoScan exponen uno de estos cuatro estados. Obligar
 * a que el estado sea exhaustivo evita el error mas comun en aplicaciones de
 * este tipo: pantallas que solo contemplan el camino feliz y se quedan en blanco
 * cuando falla la camara, falta el permiso o el historial esta vacio.
 */
sealed interface UiState<out T> {

    /** Operacion en curso. [message] permite explicar que se esta haciendo. */
    data class Loading(val message: String? = null) : UiState<Nothing>

    /** Resultado disponible. */
    data class Success<T>(val data: T) : UiState<T>

    /** No hay datos que mostrar, pero no es un error (historial vacio). */
    data class Empty(val message: String) : UiState<Nothing>

    /**
     * Fallo recuperable. [cause] se conserva solo para registro tecnico; nunca
     * debe mostrarse tal cual al productor.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : UiState<Nothing>
}
