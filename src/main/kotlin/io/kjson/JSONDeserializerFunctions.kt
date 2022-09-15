/*
 * @(#) JSONDeserializerFunctions.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022 Peter Wall
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

    abstract class FromJSONInvoker(val instance: Any?) {
        abstract fun invoke(json: JSONValue, config: JSONConfig): Any?
    }

    class FromJSONInvokerBasic(instance: Any?, val function: KFunction<*>) : FromJSONInvoker(instance) {

        override fun invoke(json: JSONValue, config: JSONConfig): Any? {
            return function.call(instance, json)
        }

    }

    class FromJSONInvokerWithConfig(instance: Any?, val function: KFunction<*>) : FromJSONInvoker(instance) {

        override fun invoke(json: JSONValue, config: JSONConfig): Any? {
            return function.call(instance, config, json)
        }

    }

    private val fromJsonCache = HashMap<Pair<KClass<*>, KClass<*>>, FromJSONInvoker>()

    fun findFromJSONInvoker(resultClass: KClass<*>, parameterClass: KClass<*>, companionObject: KClass<*>):
            FromJSONInvoker? {
        val cacheKey = resultClass to parameterClass
        return fromJsonCache[cacheKey] ?: run {
            findInvoker(companionObject.functions, resultClass, parameterClass, companionObject)
        }?.also { fromJsonCache[cacheKey] = it }
    }

    private fun findInvoker(
        functions: Collection<KFunction<*>>,
        resultClass: KClass<*>,
        parameterClass: KClass<*>,
        companionObjectClass: KClass<*>,
    ): FromJSONInvoker? {
        for (function in functions) {
            if (function.name == "fromJSON" && function.returnType.classifier == resultClass) {
                if (function.parameters.size == 2 &&
                    function.parameters[0].isInstanceParameter(companionObjectClass) &&
                    function.parameters[1].isValueParameter(parameterClass)) {
                    return FromJSONInvokerBasic(companionObjectClass.objectInstance, function)
                }
                if (function.parameters.size == 3 &&
                    function.parameters[0].isInstanceParameter(companionObjectClass) &&
                    function.parameters[1].isExtensionReceiverParameter(JSONConfig::class) &&
                    function.parameters[2].isValueParameter(parameterClass)) {
                    return FromJSONInvokerWithConfig(companionObjectClass.objectInstance, function)
                }
            }
        }
        return null
    }

    private fun KParameter.isInstanceParameter(instanceClass: KClass<*>?) =
            kind == KParameter.Kind.INSTANCE && type.classifier == instanceClass

    private fun KParameter.isExtensionReceiverParameter(receiverClass: KClass<*>) =
            kind == KParameter.Kind.EXTENSION_RECEIVER && type.classifier == receiverClass

    private fun KParameter.isValueParameter(valueClass: KClass<*>) =
            kind == KParameter.Kind.VALUE && (type.classifier as KClass<*>).isSuperclassOf(valueClass)

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
        if (!tm.match('-'))
            calendarError(tm)
        val month = tm.decimalField(12, 1)
        calendar.set(Calendar.MONTH, month - 1)
        if (!tm.match('-'))
            calendarError(tm)
        calendar.set(Calendar.DAY_OF_MONTH, tm.decimalField(JSONValidation.monthLength(year, month), 1))
        if (tm.matchAny("Tt")) {
            calendar.set(Calendar.HOUR_OF_DAY, tm.decimalField(23))
            if (!tm.match(':'))
                calendarError(tm)
            calendar.set(Calendar.MINUTE, tm.decimalField(59))
            if (!tm.match(':'))
                calendarError(tm)
            calendar.set(Calendar.SECOND, tm.decimalField(60))
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
                val zoneHours = tm.decimalField(13)
                append2Digits(sb, zoneHours)
                sb.append(':')
                val zoneMinutes = if (tm.match(':')) tm.decimalField(59) else 0
                append2Digits(sb, zoneMinutes)
                calendar.set(Calendar.ZONE_OFFSET, (zoneHours * 60 + zoneMinutes) * if (sign == '-') -60000 else 60000)
                calendar.timeZone = TimeZone.getTimeZone(sb.toString())
            }
        }
        if (!tm.isAtEnd)
            calendarError(tm)
        return calendar
    }

    private fun TextMatcher.decimalField(max: Int, min: Int = 0): Int {
        if (!matchDec(2))
            calendarError(this)
        return resultInt.also {
            if (it < min || it > max)
                calendarError(this)
        }
    }

    private fun calendarError(tm: TextMatcher): Nothing {
        throw IllegalArgumentException("Error in calendar string at offset ${tm.index} - ${tm.getString(0, tm.length)}")
    }

}
