package com.vibecode.minitodo.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

object DateTimeUtils {

    const val ALL_DAY_REMINDER_HOUR = 9
    const val DEFAULT_RANGE_SEPARATOR = " – "

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun localDate(millis: Long, zone: ZoneId = zone()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun localDateTime(millis: Long, zone: ZoneId = zone()): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    fun toMillis(date: LocalDate, time: LocalTime, zone: ZoneId = zone()): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    fun startOfDay(date: LocalDate, zone: ZoneId = zone()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfToday(now: Long = System.currentTimeMillis(), zone: ZoneId = zone()): Long =
        localDate(now, zone).toEpochMillis(zone)

    fun datePickerMillis(date: LocalDate): Long =
        date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

    fun dateFromPickerMillis(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()

    fun startOfTomorrow(now: Long = System.currentTimeMillis(), zone: ZoneId = zone()): Long =
        localDate(now, zone).plusDays(1).toEpochMillis(zone)

    fun endOfDay(date: LocalDate, zone: ZoneId = zone()): Long =
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    fun reminderTime(
        timestamp: Long,
        allDay: Boolean,
        zone: ZoneId = zone(),
    ): Long {
        if (!allDay) return timestamp
        val date = localDate(timestamp, zone)
        return toMillis(date, LocalTime.of(ALL_DAY_REMINDER_HOUR, 0), zone)
    }

    fun effectiveStart(timestamp: Long, allDay: Boolean, zone: ZoneId = zone()): Long =
        reminderTime(timestamp, allDay, zone)

    fun effectiveEnd(timestamp: Long, allDay: Boolean, zone: ZoneId = zone()): Long {
        if (!allDay) return timestamp
        return endOfDay(localDate(timestamp, zone), zone)
    }

    fun formatDate(
        millis: Long,
        zone: ZoneId = zone(),
        locale: Locale = Locale.getDefault(),
    ): String = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(localDate(millis, zone))

    fun formatDateShort(
        millis: Long,
        zone: ZoneId = zone(),
        locale: Locale = Locale.getDefault(),
    ): String = shortDateFormatter(locale).format(localDate(millis, zone))

    fun formatTime(
        millis: Long,
        zone: ZoneId = zone(),
        locale: Locale = Locale.getDefault(),
    ): String = DateTimeFormatter
        .ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(locale)
        .format(localDateTime(millis, zone))

    fun formatRange(
        startAt: Long?,
        startAllDay: Boolean,
        endAt: Long?,
        endAllDay: Boolean,
        zone: ZoneId = zone(),
        locale: Locale = Locale.getDefault(),
        rangeSeparator: String = DEFAULT_RANGE_SEPARATOR,
        now: Long = System.currentTimeMillis(),
    ): String? {
        val currentYear = localDate(now, zone).year
        val startDate = startAt?.let { localDate(it, zone) }
        val endDate = endAt?.let { localDate(it, zone) }
        val crossYear = startDate != null && endDate != null && startDate.year != endDate.year
        fun dateText(timestamp: Long): String =
            if (!crossYear && localDate(timestamp, zone).year == currentYear) {
                formatDateShort(timestamp, zone, locale)
            } else {
                formatDate(timestamp, zone, locale)
            }

        if (startDate != null && endDate != null && startDate == endDate) {
            val date = dateText(startAt)
            return when {
                startAllDay && endAllDay -> date
                startAllDay -> "$date$rangeSeparator${formatTime(endAt, zone, locale)}"
                endAllDay -> "$date ${formatTime(startAt, zone, locale)}"
                else -> "$date ${formatTime(startAt, zone, locale)}" +
                    "$rangeSeparator${formatTime(endAt, zone, locale)}"
            }
        }

        val startText = startAt?.let { timestamp ->
            if (startAllDay) dateText(timestamp)
            else "${dateText(timestamp)} ${formatTime(timestamp, zone, locale)}"
        }
        val endText = endAt?.let { timestamp ->
            if (endAllDay) dateText(timestamp)
            else "${dateText(timestamp)} ${formatTime(timestamp, zone, locale)}"
        }
        return when {
            startText != null && endText != null -> "$startText$rangeSeparator$endText"
            startText != null -> startText
            endText != null -> endText
            else -> null
        }
    }

    private fun shortDateFormatter(locale: Locale): DateTimeFormatter {
        val localizedPattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            FormatStyle.MEDIUM,
            null,
            IsoChronology.INSTANCE,
            locale,
        )
        val withoutYear = localizedPattern
            .replace(Regex("""[,\s]*[yYu]+年?[,\s]*"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim(',', '.', '，', '。', '/', '-')
            .ifBlank { localizedPattern }
        return DateTimeFormatter.ofPattern(withoutYear, locale)
    }

    private fun LocalDate.toEpochMillis(zone: ZoneId): Long =
        atStartOfDay(zone).toInstant().toEpochMilli()
}
