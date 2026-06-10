package com.akiwiksten.awtimesheet.feature.absence

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal fun LocalDate.toUtcMillis(): Long {
    return atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}

internal fun Long.toLocalDateUtc(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}
