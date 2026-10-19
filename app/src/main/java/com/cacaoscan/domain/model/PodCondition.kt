package com.cacaoscan.domain.model

/**
 * Estado sanitario de una vaina de cacao.
 *
 * El alcance del proyecto define exactamente dos categorias (IEEE830, RF03). No
 * se anaden mas: diagnosticar otras enfermedades esta explicitamente fuera del
 * alcance ("Won't have"), y anadir una tercera categoria aqui obligaria a
 * reentrenar el modelo y a modificar el documento de requisitos.
 */
enum class PodCondition {
    /** La vaina no presenta senales visibles de monilia. */
    HEALTHY,

    /** La vaina presenta senales compatibles con monilia (Moniliophthora roreri). */
    MONILIA_AFFECTED,
    ;

    companion object {
        /**
         * Orden canonico de las clases. Debe coincidir con el orden de las
         * etiquetas del modelo entrenado; ver [com.cacaoscan.ml.ModelSpec].
         */
        val CANONICAL_ORDER: List<PodCondition> = listOf(HEALTHY, MONILIA_AFFECTED)
    }
}
