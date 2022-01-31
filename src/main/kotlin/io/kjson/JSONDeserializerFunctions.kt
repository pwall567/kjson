/*
 * @(#) JSONDeserializerFunctions.kt
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

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import java.math.BigInteger

import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

import net.pwall.json.validation.JSONValidation
import net.pwall.text.TextMatcher
import net.pwall.util.IntOutput.append2Digits

object JSONDeserializerFunctions {

    private val fromJsonCache = HashMap<Pair<KClass<*>, KClass<*>>, KFunction<*>>()

    fun findFromJSON(resultClass: KClass<*>, parameterClass: KClass<*>): KFunction<*>? {
        val cacheKey = resultClass to parameterClass
        return fromJsonCache[cacheKey] ?: try {
            resultClass.companionObject?.functions?.find { function ->
                function.name == "fromJSON" &&
                        function.parameters.size == 2 &&
                        function.parameters[0].type.classifier == resultClass.companionObject &&
                        (function.parameters[1].type.classifier as KClass<*>).isSuperclassOf(parameterClass) &&
                        function.returnType.classifier == resultClass
            }
        }
        catch (e: Exception) {
            null
        }?.also { fromJsonCache[cacheKey] = it }
    }

    fun KFunction<*>.hasSingleParameter(paramClass: KClass<*>) =
            parameters.size == 1 && (parameters[0].type.classifier as? KClass<*>)?.isSuperclassOf(paramClass) ?: false

    fun KFunction<*>.hasNumberParameter() =
            parameters.size == 1 && (parameters[0].type.classifier as? KClass<*>)?.isNumberClass() ?: false

    private fun KClass<*>.isNumberClass() = this.isSubclassOf(Number::class) || this == UInt::class ||
            this == ULong::class || this == UShort::class || this == UByte::class

    fun JSONNumberValue.toBigInteger(): BigInteger = when (this) {
        is JSONDecimal -> value.toBigInteger()
        is JSONLong -> BigInteger.valueOf(value)
        is JSONInt -> BigInteger.valueOf(toLong())
    }

    fun findParameterName(parameter: KParameter, config: JSONConfig): String? =
            config.findNameFromAnnotation(parameter.annotations) ?: parameter.name

    fun createUUID(string: String): UUID {
        if (!JSONValidation.isUUID(string))
            throw IllegalArgumentException("Not a valid UUID - $string")
        return UUID.fromString(string)
    }

    /**
     * Parse a string to a [Calendar].  This may never be used, but it's important to have coverage of all the standard
     * classes that could be part of a serialised object.  This function expects the date format to correspond to either
     * the `date-time` or the `full-date` production of the [RFC 3339](https://tools.ietf.org/html/rfc3339) standard for
     * date/time representations on the Internet, [section 5.6](https://tools.ietf.org/html/rfc3339#section-5.6).
     *
     * @param   string      the input string
     * @return              the [Calendar]
     * @throws  IllegalArgumentException    if the string does not represent a valid date
     */
    fun parseCalendar(string: String): Calendar {
        val calendar = Calendar.getInstance()
        calendar.isLenient = false
        calendar.clear()
        val tm = TextMatcher(string)
        if (!tm.matchDec(4, 4))
            calendarError(tm)
        val year = tm.resultInt
        calendar.set(Calendar.YEAR, year)
        if (!tm.match('-') || !tm.matchDec(2, 2))
            calendarError(tm)
        val month = tm.resultInt
        if (month < 1 || month > 12)
            calendarError(tm)
        calendar.set(Calendar.MONTH, month - 1)
        if (!tm.match('-') || !tm.matchDec(2, 2))
            calendarError(tm)
        val day = tm.resultInt
        if (day < 1 || day > JSONValidation.monthLength(year, month))
            calendarError(tm)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        if (tm.matchAny("Tt")) {
            if (!tm.matchDec(2, 2))
                calendarError(tm)
            val hours = tm.resultInt
            if (hours > 23)
                calendarError(tm)
            calendar.set(Calendar.HOUR_OF_DAY, hours)
            if (!tm.match(':') || !tm.matchDec(2, 2))
                calendarError(tm)
            val minutes = tm.resultInt
            if (minutes > 59)
                calendarError(tm)
            calendar.set(Calendar.MINUTE, minutes)
            if (!tm.match(':'))
                calendarError(tm)
            if (!tm.matchDec(2, 2))
                calendarError(tm)
            val seconds = tm.resultInt
            if (seconds > 60)
                calendarError(tm)
            calendar.set(Calendar.SECOND, seconds)
            if (tm.match('.')) {
                if (!tm.matchDec())
                    calendarError(tm)
                var numberEnd = tm.index.coerceAtMost(tm.start + 3)
                var millis = tm.getInt(tm.start, numberEnd)
                while (numberEnd < tm.start + 3) {
                    millis *= 10
                    numberEnd++
                }
                calendar.set(Calendar.MILLISECOND, millis)
            }
            if (tm.matchAny("Zz")) {
                calendar.timeZone = TimeZone.getTimeZone("GMT")
                calendar.set(Calendar.ZONE_OFFSET, 0)
            }
            else if (tm.matchAny("+-")) {
                val sb = StringBuilder("GMT")
                val sign = tm.resultChar
                sb.append(sign)
                if (!tm.matchDec(2))
                    calendarError(tm)
                val zoneHours = tm.resultInt
                if (zoneHours > 13)
                    calendarError(tm)
                append2Digits(sb, zoneHours)
                sb.append(':')
                val zoneMinutes = if (tm.match(':')) {
                    if (!tm.matchDec(2, 2))
                        calendarError(tm)
                    tm.resultInt
                }
                else
                    0
                append2Digits(sb, zoneMinutes)
                if (zoneMinutes > 59)
                    calendarError(tm)
                calendar.set(Calendar.ZONE_OFFSET, (zoneHours * 60 + zoneMinutes) * if (sign == '-') -60000 else 60000)
                calendar.timeZone = TimeZone.getTimeZone(sb.toString())
            }
        }
        if (!tm.isAtEnd)
            calendarError(tm)
        return calendar
    }

    private fun calendarError(tm: TextMatcher): Nothing {
        throw IllegalArgumentException("Error in calendar string at offset ${tm.index} - ${tm.getString(0, tm.length)}")
    }

}
