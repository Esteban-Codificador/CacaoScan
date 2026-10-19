package com.cacaoscan.presentation.history

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ScanDateFormatterTest {

    private val bogota = ZoneId.of("America/Bogota")

    @Test
    fun `presenta la fecha en la zona horaria indicada`() {
        // 2026-09-05T15:30Z equivale a las 10:30 en Colombia (UTC-5).
        val formatted = ScanDateFormatter.format(Instant.parse("2026-09-05T15:30:00Z"), bogota)

        assertTrue("Deberia mostrar la hora local: $formatted", formatted.contains("10:30"))
        assertTrue(formatted.contains("2026"))
        assertTrue(formatted.contains("5"))
    }

    @Test
    fun `una misma marca de tiempo cambia de dia segun la zona`() {
        val instant = Instant.parse("2026-09-06T02:00:00Z")

        val inBogota = ScanDateFormatter.format(instant, bogota)
        val inUtc = ScanDateFormatter.format(instant, ZoneId.of("UTC"))

        // En Bogota siguen siendo las 21:00 del dia 5; en UTC ya es el dia 6.
        assertTrue(inBogota.contains("21:00"))
        assertTrue(inUtc.contains("02:00"))
    }
}
