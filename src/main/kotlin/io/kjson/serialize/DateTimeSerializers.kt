/*
 * @(#) DateTimeSerializers.kt
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

package io.kjson.serialize

import kotlin.time.Duration

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Year
import java.time.YearMonth
import java.util.Calendar
import java.util.Date

import io.kjson.JSONConfig
import io.kjson.JSONString
import net.pwall.util.CoDateOutput.outputCalendar
import net.pwall.util.CoDateOutput.outputDate
import net.pwall.util.CoDateOutput.outputInstant
import net.pwall.util.CoDateOutput.outputLocalDate
import net.pwall.util.CoDateOutput.outputLocalDateTime
import net.pwall.util.CoDateOutput.outputLocalTime
import net.pwall.util.CoDateOutput.outputMonthDay
import net.pwall.util.CoDateOutput.outputOffsetDateTime
import net.pwall.util.CoDateOutput.outputOffsetTime
import net.pwall.util.CoDateOutput.outputYear
import net.pwall.util.CoDateOutput.outputYearMonth
import net.pwall.util.CoOutput
import net.pwall.util.DateOutput
import net.pwall.util.output

object OffsetDateTimeSerializer : StringSerializer<OffsetDateTime> {

    override fun serialize(value: OffsetDateTime, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { DateOutput.appendOffsetDateTime(this, value) }

    override fun appendString(a: Appendable, value: OffsetDateTime, config: JSONConfig) {
        DateOutput.appendOffsetDateTime(a, value)
    }

    override suspend fun output(out: CoOutput, value: OffsetDateTime, config: JSONConfig,
            references: MutableList<Any>) {
        out.output('"')
        out.outputOffsetDateTime(value)
        out.output('"')
    }

}

object LocalDateSerializer : StringSerializer<LocalDate> {

    override fun serialize(value: LocalDate, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { DateOutput.appendLocalDate(this, value) }

    override fun appendString(a: Appendable, value: LocalDate, config: JSONConfig) {
        DateOutput.appendLocalDate(a, value)
    }

    override suspend fun output(out: CoOutput, value: LocalDate, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.outputLocalDate(value)
        out.output('"')
    }

}

object InstantSerializer : StringSerializer<Instant> {

    override fun serialize(value: Instant, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { DateOutput.appendInstant(this, value) }

    override fun appendString(a: Appendable, value: Instant, config: JSONConfig) {
        DateOutput.appendInstant(a, value)
    }

    override suspend fun output(out: CoOutput, value: Instant, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.outputInstant(value)
        out.output('"')
    }

}

object OffsetTimeSerializer : StringSerializer<OffsetTime> {

    override fun serialize(value: OffsetTime, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { DateOutput.appendOffsetTime(this, value) }

    override fun appendString(a: Appendable, value: OffsetTime, config: JSONConfig) {
        DateOutput.appendOffsetTime(a, value)
    }

    override suspend fun output(out: CoOutput, value: OffsetTime, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.outputOffsetTime(value)
        out.output('"')
    }

}

object LocalDateTimeSerializer : StringSerializer<LocalDateTime> {

    override fun serialize(value: LocalDateTime, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { DateOutput.appendLocalDateTime(this, value) }

    override fun appendString(a: Appendable, value: LocalDateTime, config: JSONConfig) {
        DateOutput.appendLocalDateTime(a, value)
    }

    override suspend fun output(out: CoOutput, value: LocalDateTime, config: JSONConfig,
            references: MutableList<Any>) {
        out.output('"')
        out.outputLocalDateTime(value)
        out.output('"')
    }

}

object LocalTimeSerializer : StringSerializer<LocalTime> {

    override fun serialize(value: LocalTime, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { DateOutput.appendLocalTime(this, value) }

    override fun appendString(a: Appendable, value: LocalTime, config: JSONConfig) {
        DateOutput.appendLocalTime(a, value)
    }

    override suspend fun output(out: CoOutput, value: LocalTime, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.outputLocalTime(value)
        out.output('"')
    }

}

object YearSerializer : StringSerializer<Year> {

    override fun serialize(value: Year, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { DateOutput.appendYear(this, value) }

    override fun appendString(a: Appendable, value: Year, config: JSONConfig) {
        DateOutput.appendYear(a, value)
    }

    override suspend fun output(out: CoOutput, value: Year, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.outputYear(value)
        out.output('"')
    }

}

object YearMonthSerializer : StringSerializer<YearMonth> {

    override fun serialize(value: YearMonth, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { DateOutput.appendYearMonth(this, value) }

    override fun appendString(a: Appendable, value: YearMonth, config: JSONConfig) {
        DateOutput.appendYearMonth(a, value)
    }

    override suspend fun output(out: CoOutput, value: YearMonth, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.outputYearMonth(value)
        out.output('"')
    }

}

object MonthDaySerializer : StringSerializer<MonthDay> {

    override fun serialize(value: MonthDay, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { DateOutput.appendMonthDay(this, value) }

    override fun appendString(a: Appendable, value: MonthDay, config: JSONConfig) {
        DateOutput.appendMonthDay(a, value)
    }

    override suspend fun output(out: CoOutput, value: MonthDay, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.outputMonthDay(value)
        out.output('"')
    }

}

object CalendarSerializer : StringSerializer<Calendar> {

    override fun serialize(value: Calendar, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { DateOutput.appendCalendar(this, value) }

    override fun appendString(a: Appendable, value: Calendar, config: JSONConfig) {
        DateOutput.appendCalendar(a, value)
    }

    override suspend fun output(out: CoOutput, value: Calendar, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.outputCalendar(value)
        out.output('"')
    }

}

object DateSerializer : StringSerializer<Date> {

    override fun serialize(value: Date, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { DateOutput.appendDate(this, value) }

    override fun appendString(a: Appendable, value: Date, config: JSONConfig) {
        DateOutput.appendDate(a, value)
    }

    override suspend fun output(out: CoOutput, value: Date, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.outputDate(value)
        out.output('"')
    }

}

object DurationSerializer : StringSerializer<Duration> {

    override fun serialize(value: Duration, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString(value.toIsoString())

    override fun appendString(a: Appendable, value: Duration, config: JSONConfig) {
        a.append(value.toIsoString())
    }

    override suspend fun output(out: CoOutput, value: Duration, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.output(value.toIsoString())
        out.output('"')
    }

}
