package com.cacaoscan.presentation.history

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatea la fecha de una revision para mostrarla al productor.
 *
 * Las marcas de tiempo se guardan en UTC y se presentan en la zona horaria del
 * dispositivo: guardar en local haria que el historial se desordenara al viajar
 * o al cambiar la hora del telefono.
 */
object ScanDateFormatter {

    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-CO"))

    fun format(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        formatter.format(instant.atZone(zone))
}
