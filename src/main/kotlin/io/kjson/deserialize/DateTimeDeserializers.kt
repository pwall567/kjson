/*
 * @(#) DateTimeDeserializers.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2024 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.kjson.deserialize

import kotlin.time.Duration

import java.time.Duration as JavaDuration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date

import io.kjson.JSONDeserializerFunctions.parseCalendar
import io.kjson.JSONString
import io.kjson.JSONValue

data object InstantDeserializer : Deserializer<Instant> {
    override fun deserialize(json: JSONValue?): Instant? = when (json) {
        null -> null
        is JSONString -> Instant.parse(json.value)
        else -> typeError("string")
    }
}

data object LocalDateDeserializer : Deserializer<LocalDate> {
    override fun deserialize(json: JSONValue?): LocalDate? = when (json) {
        null -> null
        is JSONString -> LocalDate.parse(json.value)
        else -> typeError("string")
    }
}

data object LocalDateTimeDeserializer : Deserializer<LocalDateTime> {
    override fun deserialize(json: JSONValue?): LocalDateTime? = when (json) {
        null -> null
        is JSONString -> LocalDateTime.parse(json.value)
        else -> typeError("string")
    }
}

data object LocalTimeDeserializer : Deserializer<LocalTime> {
    override fun deserialize(json: JSONValue?): LocalTime? = when (json) {
        null -> null
        is JSONString -> LocalTime.parse(json.value)
        else -> typeError("string")
    }
}

data object OffsetTimeDeserializer : Deserializer<OffsetTime> {
    override fun deserialize(json: JSONValue?): OffsetTime? = when (json) {
        null -> null
        is JSONString -> OffsetTime.parse(json.value)
        else -> typeError("string")
    }
}

data object OffsetDateTimeDeserializer : Deserializer<OffsetDateTime> {
    override fun deserialize(json: JSONValue?): OffsetDateTime? = when (json) {
        null -> null
        is JSONString -> OffsetDateTime.parse(json.value)
        else -> typeError("string")
    }
}

data object ZonedDateTimeDeserializer : Deserializer<ZonedDateTime> {
    override fun deserialize(json: JSONValue?): ZonedDateTime? = when (json) {
        null -> null
        is JSONString -> ZonedDateTime.parse(json.value)
        else -> typeError("string")
    }
}

data object YearDeserializer : Deserializer<Year> {
    override fun deserialize(json: JSONValue?): Year? = when (json) {
        null -> null
        is JSONString -> Year.parse(json.value)
        else -> typeError("string")
    }
}

data object YearMonthDeserializer : Deserializer<YearMonth> {
    override fun deserialize(json: JSONValue?): YearMonth? = when (json) {
        null -> null
        is JSONString -> YearMonth.parse(json.value)
        else -> typeError("string")
    }
}

data object MonthDayDeserializer : Deserializer<MonthDay> {
    override fun deserialize(json: JSONValue?): MonthDay? = when (json) {
        null -> null
        is JSONString -> MonthDay.parse(json.value)
        else -> typeError("string")
    }
}

data object JavaDurationDeserializer : Deserializer<JavaDuration> {
    override fun deserialize(json: JSONValue?): JavaDuration? = when (json) {
        null -> null
        is JSONString -> JavaDuration.parse(json.value)
        else -> typeError("string")
    }
}

data object PeriodDeserializer : Deserializer<Period> {
    override fun deserialize(json: JSONValue?): Period? = when (json) {
        null -> null
        is JSONString -> Period.parse(json.value)
        else -> typeError("string")
    }
}

data object DurationDeserializer : Deserializer<Duration> {
    override fun deserialize(json: JSONValue?): Duration? = when (json) {
        null -> null
        is JSONString -> Duration.parseIsoString(json.value)
        else -> typeError("string")
    }
}

data object DateDeserializer : Deserializer<Date> {
    override fun deserialize(json: JSONValue?): Date? = when (json) {
        null -> null
        is JSONString -> parseCalendar(json.value).time
        else -> typeError("string")
    }
}

data object CalendarDeserializer : Deserializer<Calendar> {
    override fun deserialize(json: JSONValue?): Calendar? = when (json) {
        null -> null
        is JSONString -> parseCalendar(json.value)
        else -> typeError("string")
    }
}

data object JavaSQLDateDeserializer : Deserializer<java.sql.Date> {
    override fun deserialize(json: JSONValue?): java.sql.Date? = when (json) {
        null -> null
        is JSONString -> java.sql.Date.valueOf(json.value)
        else -> typeError("string")
    }
}

data object JavaSQLTimeDeserializer : Deserializer<java.sql.Time> {
    override fun deserialize(json: JSONValue?): java.sql.Time? = when (json) {
        null -> null
        is JSONString -> java.sql.Time.valueOf(json.value)
        else -> typeError("string")
    }
}

data object JavaSQLTimestampDeserializer : Deserializer<java.sql.Timestamp> {
    override fun deserialize(json: JSONValue?): java.sql.Timestamp? = when (json) {
        null -> null
        is JSONString -> java.sql.Timestamp.valueOf(json.value)
        else -> typeError("string")
    }
}
