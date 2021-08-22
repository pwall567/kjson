/*
 * @(#) JSONSerializerFunctions.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021 Peter Wall
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

package io.kjson

import kotlin.math.abs
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType

import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.Duration
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
import java.util.LinkedList
import java.util.UUID
import net.pwall.json.JSONFunctions

/**
 * Utility functions for JSON Serialization.  These functions are not expected to be of use outside the `kjson` family
 * of projects.
 *
 * @author  Peter Wall
 */
object JSONSerializerFunctions {

    private val toStringClasses = setOf(java.sql.Date::class, java.sql.Time::class, java.sql.Timestamp::class,
        Instant::class, LocalDate::class, LocalDateTime::class, LocalTime::class, OffsetTime::class,
        OffsetDateTime::class, ZonedDateTime::class, Year::class, YearMonth::class, MonthDay::class,
        Duration::class, Period::class, URI::class, URL::class, UUID::class)

    private val uncachedClasses = setOf(Any::class, String::class, Boolean::class, Int::class, Long::class, Byte::class,
        Short::class, Double::class, Float::class, BigDecimal::class, BigInteger::class, ArrayList::class,
        LinkedList::class, HashMap::class, LinkedHashMap::class, HashSet::class)

    private val toJsonCache = HashMap<KClass<*>, KFunction<Any?>?>()

    /**
     * Is the class best represented by a string of the `toString()` result?
     *
     * @receiver            the class of the object
     * @return              `true` if the object should be output as a string
     */
    fun KClass<*>.isToStringClass() = this in toStringClasses

    fun KClass<*>.findToJSON(): KFunction<Any?>? {
        if (this in uncachedClasses || this in toStringClasses)
            return null
        if (toJsonCache.containsKey(this))
            return toJsonCache[this]
        try {
            for (function in members) {
                if (function is KFunction<*> &&
                        function.name == "toJSON" &&
                        function.parameters.size == 1 &&
                        function.parameters[0].kind == KParameter.Kind.INSTANCE &&
                        function.returnType.isAcceptable())
                    @Suppress("UNCHECKED_CAST")
                    return (function as KFunction<Any?>).apply { toJsonCache[this@findToJSON] = this }
            }
        }
        catch (ignore: Exception) {
        }
        toJsonCache[this] = null
        return null
    }

    private fun KType.isAcceptable(): Boolean {
        classifier?.let { if (it is KClass<*>) return it != Unit::class && it != Nothing::class }
        return true
    }

    fun KClass<*>.isSealedSubclass(): Boolean {
        for (supertype in supertypes) {
            (supertype.classifier as? KClass<*>)?.let {
                if (it.isSealed)
                    return true
                if (it != Any::class)
                    if (it.isSealedSubclass())
                        return true
            }
        }
        return false
    }

    fun Appendable.appendCalendar(calendar: Calendar) {
        JSONFunctions.appendInt(this, calendar.get(Calendar.YEAR))
        append('-')
        JSONFunctions.append2Digits(this, calendar.get(Calendar.MONTH) + 1)
        append('-')
        JSONFunctions.append2Digits(this, calendar.get(Calendar.DAY_OF_MONTH))
        append('T')
        JSONFunctions.append2Digits(this, calendar.get(Calendar.HOUR_OF_DAY))
        append(':')
        JSONFunctions.append2Digits(this, calendar.get(Calendar.MINUTE))
        append(':')
        JSONFunctions.append2Digits(this, calendar.get(Calendar.SECOND))
        append('.')
        JSONFunctions.append3Digits(this, calendar.get(Calendar.MILLISECOND))
        val offsetMillis = calendar.get(Calendar.ZONE_OFFSET) +
                if (calendar.timeZone.inDaylightTime(calendar.time)) calendar.get(Calendar.DST_OFFSET) else 0
        if (offsetMillis == 0)
            append('Z')
        else {
            append(if (offsetMillis < 0) '-' else '+')
            val offsetMinutes = abs(offsetMillis / (60 * 1000))
            JSONFunctions.append2Digits(this, offsetMinutes / 60)
            append(':')
            JSONFunctions.append2Digits(this, offsetMinutes % 60)
        }
    }

}
