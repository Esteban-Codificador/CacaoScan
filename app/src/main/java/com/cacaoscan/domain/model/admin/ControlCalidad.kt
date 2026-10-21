package com.cacaoscan.domain.model.admin

/** Datos que se envian en POST /validaciones. */
data class ImagenCandidata(
    val archivo: String,
    val ancho: Int,
    val alto: Int,
    val pesoKb: Double,
    val clases: List<String>,
)

/** Veredicto del microservicio de Control de Calidad sobre una imagen. */
data class ValidacionImagen(
    val archivo: String,
    val ancho: Int,
    val alto: Int,
    val pesoKb: Double,
    val clases: List<String>,
    val claseBinaria: String?,
    val aceptada: Boolean,
    val errores: List<String>,
    val advertencias: List<String>,
    val validadaEn: String,
)

data class ResumenCalidad(
    val totalValidadas: Int,
    val aceptadas: Int,
    val rechazadas: Int,
    val tasaAceptacionPct: Double,
    val conAdvertencias: Int,
    val motivosDeRechazo: Map<String, Int>,
    val aceptadasPorClase: Map<String, Int>,
)

/** Clases que anota el CocoaMoniliaDataSet: h0 sana, m1 a m3 grados de monilia. */
enum class ClaseAnotada(val codigo: String, val etiqueta: String) {
    H0("h0", "Sana"),
    M1("m1", "Monilia grado 1"),
    M2("m2", "Monilia grado 2"),
    M3("m3", "Monilia grado 3"),
}
