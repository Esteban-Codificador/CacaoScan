package com.cacaoscan.domain.model.admin

/** Un modelo tal como lo devuelve el microservicio de Registro de Modelos. */
data class ModeloRegistrado(
    val version: String,
    val exactitud: Double,
    val sensibilidad: Double,
    val hashSha256: String,
    val tamanoMb: Double,
    val arquitectura: String,
    val notas: String,
    val aptoParaPublicar: Boolean,
    val motivo: String,
    val registradoEn: String,
)

/** Datos que se envian en POST /modelos. La validacion la decide el servicio, no la app. */
data class NuevoModelo(
    val version: String,
    val exactitud: Double,
    val sensibilidad: Double,
    val hashSha256: String,
    val tamanoMb: Double,
    val arquitectura: String = "MobileNetV3-Small",
    val notas: String = "",
)

data class ResumenRegistro(
    val totalRegistrados: Int,
    val aptosParaPublicar: Int,
    val noAptos: Int,
    val mejorExactitud: Double?,
    val versionVigente: String?,
)
