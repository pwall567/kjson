/*
 * @(#) JSONDeserializerFunctions.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023, 2024 Peter Wall
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

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.isSupertypeOf

import java.math.BigInteger
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

import io.jstuff.json.validation.JSONValidation
import io.jstuff.text.TextMatcher
import io.jstuff.util.IntOutput.append2Digits

object JSONDeserializerFunctions {

    abstract class InClassFromJSON<T : Any>(
        val jsonValueClass: KClass<*>,
        val jsonNullable: Boolean,
        val companionInstance: Any?,
    ) {
        abstract fun invoke(json: JSONValue?, config: JSONConfig): T
    }

    class InClassFromJSONBasic<T : Any>(
        jsonValueClass: KClass<*>,
        jsonNullable: Boolean,
        companionInstance: Any?,
        private val function: KFunction<T>,
    ) : InClassFromJSON<T>(jsonValueClass, jsonNullable, companionInstance) {

        override fun invoke(json: JSONValue?, config: JSONConfig): T {
            return function.call(companionInstance, json)
        }

    }

    class InClassFromJSONWithContext<T : Any>(
        jsonValueClass: KClass<*>,
        jsonNullable: Boolean,
        companionInstance: Any?,
        private val function: KFunction<T>,
    ) : InClassFromJSON<T>(jsonValueClass, jsonNullable, companionInstance) {

        @Suppress("deprecation")
        override fun invoke(json: JSONValue?, config: JSONConfig): T {
            return function.call(companionInstance, JSONContext(config), json)
        }

    }

    class InClassFromJSONWithConfig<T : Any>(
        jsonValueClass: KClass<*>,
        jsonNullable: Boolean,
        companionInstance: Any?,
        private val function: KFunction<T>
    ) : InClassFromJSON<T>(jsonValueClass, jsonNullable, companionInstance) {

        override fun invoke(json: JSONValue?, config: JSONConfig): T {
            return function.call(companionInstance, config, json)
        }

    }

    private val inClassFromJSONCache = HashMap<KClass<*>, List<InClassFromJSON<*>>>()

    @Suppress("unchecked_cast", "deprecation")
    internal fun <T : Any> findAllInClassFromJSON(
        resultClass: KClass<T>,
        companionObjectClass: KClass<*>,
    ): List<InClassFromJSON<T>> {
        inClassFromJSONCache[resultClass]?.let { return it as List<InClassFromJSON<T>> }
        val result = mutableListOf<InClassFromJSON<T>>()
        for (function in companionObjectClass.members) {
            if (function is KFunction<*> && function.name == "fromJSON" && function.isPublic() &&
                function.returnType.classifier == resultClass) {
                function as KFunction<T>
                val parameters = function.parameters
                if (parameters.size == 2 &&
                    parameters[0].isInstanceParameter(companionObjectClass) &&
                    parameters[1].isValueSubclassParameter(JSONValue::class)) {
                    val jsonValueType = parameters[1].type
                    result.add(InClassFromJSONBasic(
                        jsonValueClass = jsonValueType.classifier as KClass<*>,
                        jsonNullable = jsonValueType.isMarkedNullable,
                        companionInstance = companionObjectClass.objectInstance,
                        function = function
                    ))
                }
                if (parameters.size == 3 &&
                    parameters[0].isInstanceParameter(companionObjectClass) &&
                    parameters[1].isExtensionReceiverParameter(JSONContext::class) &&
                    parameters[2].isValueSubclassParameter(JSONValue::class)) {
                    val jsonValueType = parameters[2].type
                    result.add(InClassFromJSONWithContext(
                        jsonValueClass = jsonValueType.classifier as KClass<*>,
                        jsonNullable = jsonValueType.isMarkedNullable,
                        companionInstance = companionObjectClass.objectInstance,
                        function = function
                    ))
                }
                if (parameters.size == 3 &&
                    parameters[0].isInstanceParameter(companionObjectClass) &&
                    parameters[1].isExtensionReceiverParameter(JSONConfig::class) &&
                    parameters[2].isValueSubclassParameter(JSONValue::class)) {
                    val jsonValueType = parameters[2].type
                    result.add(InClassFromJSONWithConfig(
                        jsonValueClass = jsonValueType.classifier as KClass<*>,
                        jsonNullable = jsonValueType.isMarkedNullable,
                        companionInstance = companionObjectClass.objectInstance,
                        function = function
                    ))
                }
            }
        }
        inClassFromJSONCache[resultClass] = result
        return result
    }

    private fun KParameter.isInstanceParameter(instanceClass: KClass<*>?) =
            kind == KParameter.Kind.INSTANCE && type.classifier == instanceClass

    private fun KParameter.isExtensionReceiverParameter(receiverClass: KClass<*>) =
            kind == KParameter.Kind.EXTENSION_RECEIVER && type.classifier == receiverClass

    private fun KParameter.isValueParameter(valueClass: KClass<*>) =
            kind == KParameter.Kind.VALUE && (type.classifier as KClass<*>).isSuperclassOf(valueClass)

    private fun KParameter.isValueParameter(valueType: KType) =
            kind == KParameter.Kind.VALUE && type.isSupertypeOf(valueType)

    private fun KParameter.isValueSubclassParameter(valueClass: KClass<*>) =
            kind == KParameter.Kind.VALUE && (type.classifier as KClass<*>).isSubclassOf(valueClass)

    fun <R : Any> KClass<R>.findSingleParameterConstructor(paramClass: KClass<*>): KFunction<R>? =
            constructors.singleOrNull { it.isPublic() && it.hasSingleParameter(paramClass) }

    fun KCallable<*>.isPublic(): Boolean = visibility == KVisibility.PUBLIC

    private fun KFunction<*>.hasSingleParameter(paramClass: KClass<*>): Boolean = parameters.isNotEmpty() &&
            ((parameters[0].type.classifier as? KClass<*>)?.isSuperclassOf(paramClass) ?: false) &&
            subsequentParametersOptional()

    fun KFunction<*>.hasNumberParameter(): Boolean = parameters.isNotEmpty() &&
            ((parameters[0].type.classifier as? KClass<*>)?.isNumberClass() ?: false) &&
            subsequentParametersOptional()

    fun KFunction<*>.hasArrayParameter(): Boolean = parameters.isNotEmpty() &&
            ((parameters[0].type.classifier as? KClass<*>)?.java?.isArray ?: false) &&
            subsequentParametersOptional()

    private fun KFunction<*>.subsequentParametersOptional(): Boolean {
        for (i in 1 until parameters.size)
            if (!parameters[i].isOptional)
                return false
        return true
    }

    private fun KClass<*>.isNumberClass() = this.isSubclassOf(Number::class) || this == UInt::class ||
            this == ULong::class || this == UShort::class || this == UByte::class

    fun <R> KCallable<R>.callWithSingle(arg: Any?): R = when (parameters.size) {
        1 -> call(arg)
        else -> callBy(mapOf(parameters[0] to arg))
    }

    fun KClass<*>.displayName(): String = qualifiedName?.displayName() ?: "<unknown>"

    fun KType.displayName(): String = toString().displayName()

    fun String.displayName(): String = if (startsWith("kotlin.") && indexOf('.', 7) < 0) substring(7) else this

    fun JSONNumber.toBigInteger(): BigInteger = when (this) {
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
        if (tm.match { it == 'T' || it == 't' }) {
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
            if (tm.match { it == 'Z' || it == 'z' }) {
                calendar.timeZone = TimeZone.getTimeZone("GMT")
                calendar.set(Calendar.ZONE_OFFSET, 0)
            }
            else if (tm.match { it == '+' || it == '-' }) {
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
