package com.vibecode.minitodo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

class DateTimeUtilsTest {

    private val zone: ZoneId = ZoneId.of("Asia/Taipei")

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
    ): Long = LocalDateTime.of(year, month, day, hour, minute)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `all day reminder is nine in the morning`() {
        val date = millis(2026, 11, 9)
        val expected = millis(2026, 11, 9, 9)
        assertEquals(expected, DateTimeUtils.reminderTime(date, allDay = true, zone = zone))
    }

    @Test
    fun `timed reminder keeps the original timestamp`() {
        val timestamp = millis(2026, 11, 9, 14, 30)
        assertEquals(
            timestamp,
            DateTimeUtils.reminderTime(timestamp, allDay = false, zone = zone),
        )
    }

    @Test
    fun `all day end is the last millisecond of the day`() {
        val date = millis(2026, 11, 9)
        val end = DateTimeUtils.effectiveEnd(date, allDay = true, zone = zone)
        assertEquals(millis(2026, 11, 9, 23, 59) + 59_999L, end)
        assertTrue(end > millis(2026, 11, 9, 23, 59))
    }

    @Test
    fun `date picker millis round trips to the same local date`() {
        val date = LocalDate.of(2026, 11, 9)
        val pickerMillis = DateTimeUtils.datePickerMillis(date)
        assertEquals(date, DateTimeUtils.dateFromPickerMillis(pickerMillis))
    }

    @Test
    fun `start of today matches local midnight`() {
        val now = millis(2026, 11, 9, 12, 30)
        assertEquals(
            millis(2026, 11, 9),
            DateTimeUtils.startOfToday(now, zone),
        )
        assertEquals(
            millis(2026, 11, 10),
            DateTimeUtils.startOfTomorrow(now, zone),
        )
    }

    @Test
    fun `toMillis combines date and time in the local zone`() {
        val date = LocalDate.of(2026, 11, 9)
        val time = LocalTime.of(8, 15)
        assertEquals(
            millis(2026, 11, 9, 8, 15),
            DateTimeUtils.toMillis(date, time, zone),
        )
    }

    @Test
    fun `same day range shows the date once without the current year`() {
        val range = DateTimeUtils.formatRange(
            startAt = millis(2026, 11, 9, 9),
            startAllDay = false,
            endAt = millis(2026, 11, 9, 10, 30),
            endAllDay = false,
            zone = zone,
            locale = Locale.TAIWAN,
            rangeSeparator = " – ",
            now = millis(2026, 11, 10, 12),
        )
        assertTrue(range != null)
        assertEquals(1, Regex("""11月9日""").findAll(range!!).count())
        assertFalse(range.contains("2026"))
        assertTrue(range.contains("9:00"))
        assertTrue(range.contains("10:30"))
    }

    @Test
    fun `same day all day range shows a single date`() {
        val range = DateTimeUtils.formatRange(
            startAt = millis(2026, 11, 9),
            startAllDay = true,
            endAt = millis(2026, 11, 9),
            endAllDay = true,
            zone = zone,
            locale = Locale.TAIWAN,
            now = millis(2026, 11, 10, 12),
        )
        assertEquals(DateTimeUtils.formatDateShort(millis(2026, 11, 9), zone, Locale.TAIWAN), range)
    }

    @Test
    fun `cross year range keeps both years`() {
        val range = DateTimeUtils.formatRange(
            startAt = millis(2026, 12, 31, 9),
            startAllDay = false,
            endAt = millis(2027, 1, 1, 10),
            endAllDay = false,
            zone = zone,
            locale = Locale.TAIWAN,
            now = millis(2026, 1, 1, 0),
        )
        assertTrue(range != null)
        assertTrue(range!!.contains("2026"))
        assertTrue(range.contains("2027"))
    }
}
