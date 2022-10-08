/*
 * @(#) JSONStringify.kt
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

import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.time.Duration

import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Year
import java.time.YearMonth
import java.util.BitSet
import java.util.Calendar
import java.util.Date
import java.util.Enumeration
import java.util.UUID
import java.util.stream.BaseStream

import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.JSONSerializerFunctions.appendUUID
import io.kjson.JSONSerializerFunctions.findSealedClass
import io.kjson.JSONSerializerFunctions.findToJSON
import io.kjson.JSONSerializerFunctions.isToStringClass
import io.kjson.annotation.JSONDiscriminator
import io.kjson.annotation.JSONIdentifier

import net.pwall.json.JSONFunctions
import net.pwall.util.DateOutput
import net.pwall.util.IntOutput.appendInt
import net.pwall.util.IntOutput.appendLong
import net.pwall.util.IntOutput.appendUnsignedInt
import net.pwall.util.IntOutput.appendUnsignedLong

/**
 * Reflection-based JSON serialization for Kotlin - serialize direct to `String`.
 *
 * @author  Peter Wall
 */
object JSONStringify {

    /**
     * Serialize an object to JSON. (The word "stringify" is borrowed from the JavaScript implementation of JSON.)
     *
     * @param   obj     the object
     * @param   config  an optional [JSONConfig] to customise the conversion
     * @return          the JSON form of the object
     */
    fun stringify(obj: Any?, config: JSONConfig = JSONConfig.defaultConfig): String {
        return when (obj) {
            null -> "null"
            else -> buildString(config.stringifyInitialSize) {
                appendJSON(obj, config)
            }
        }
    }

    /**
     * Append the serialized form of an object to an [Appendable] in JSON.
     *
     * @receiver        the [Appendable] (e.g. [StringBuilder])
     * @param   obj     the object
     * @param   config  an optional [JSONConfig] to customise the conversion
     */
    fun Appendable.appendJSON(obj: Any?, config: JSONConfig = JSONConfig.defaultConfig) {
        appendJSON(obj, config, mutableSetOf())
    }

    private fun Appendable.appendJSON(obj: Any?, config: JSONConfig, references: MutableSet<Any>) {

        if (obj == null) {
            append("null")
            return
        }

        config.findToJSONMapping(obj::class)?.let {
            appendJSON(config.it(obj), config, references)
            return
        }

        when (obj) {
            is JSONValue -> obj.appendTo(this)
            is CharSequence -> JSONFunctions.appendString(this, obj, config.stringifyNonASCII)
            is CharArray -> appendQuoted {
                for (ch in obj)
                    JSONFunctions.appendChar(this, ch, config.stringifyNonASCII)
            }
            is Char -> appendQuoted { JSONFunctions.appendChar(this, obj, config.stringifyNonASCII) }
            is Number -> appendJSONNumber(obj, config, references)
            is Boolean -> append(obj.toString())
            is UInt -> appendUnsignedInt(this, obj.toInt())
            is UShort -> appendInt(this, obj.toInt())
            is UByte -> appendInt(this, obj.toInt())
            is ULong -> appendUnsignedLong(this, obj.toLong())
            is Array<*> -> appendJSONArray(obj, config, references)
            is Pair<*, *> -> appendJSONPair(obj, config, references)
            is Triple<*, *, *> -> appendJSONTriple(obj, config, references)
            is IntArray -> appendJSONTypedArray(obj.size) { appendInt(this, obj[it]) }
            is LongArray -> appendJSONTypedArray(obj.size) { appendLong(this, obj[it]) }
            is ByteArray -> appendJSONTypedArray(obj.size) { appendInt(this, obj[it].toInt()) }
            is ShortArray -> appendJSONTypedArray(obj.size) { appendInt(this, obj[it].toInt()) }
            is FloatArray -> appendJSONTypedArray(obj.size) { append(obj[it].toString()) }
            is DoubleArray -> appendJSONTypedArray(obj.size) { append(obj[it].toString()) }
            is BooleanArray -> appendJSONTypedArray(obj.size) { append(obj[it].toString()) }
            else -> appendJSONObject(obj, config, references)
        }

    }

    private fun Appendable.appendJSONNumber(number: Number, config: JSONConfig, references: MutableSet<Any>) {
        when (number) {
            is Int -> appendInt(this, number)
            is Short, is Byte -> appendInt(this, number.toInt())
            is Long -> appendLong(this, number)
            is Float, is Double -> append(number.toString())
            is BigInteger -> {
                if (config.bigIntegerString)
                    JSONFunctions.appendString(this, number.toString(), false)
                else
                    append(number.toString())
            }
            is BigDecimal -> {
                if (config.bigDecimalString)
                    JSONFunctions.appendString(this, number.toString(), false)
                else
                    append(number.toString())
            }
            else -> appendJSONObject(number, config, references)
        }
    }

    private fun Appendable.appendJSONArray(array: Array<*>, config: JSONConfig, references: MutableSet<Any>) {
        if (array.isArrayOf<Char>()) {
            appendQuoted {
                for (ch in array)
                    JSONFunctions.appendChar(this, ch as Char, config.stringifyNonASCII)
            }
        }
        else
            appendJSONTypedArray(array.size) { appendJSON(array[it], config, references) }
    }

    private fun Appendable.appendJSONTypedArray(size: Int, itemFunction: Appendable.(Int) -> Unit) {
        append('[')
        if (size > 0) {
            for (i in 0 until size) {
                if (i > 0)
                    append(',')
                itemFunction(i)
            }
        }
        append(']')
    }

    private fun Appendable.appendJSONPair(pair: Pair<*, *>, config: JSONConfig, references: MutableSet<Any>) {
        append('[')
        appendJSON(pair.first, config, references)
        append(',')
        appendJSON(pair.second, config, references)
        append(']')
    }

    private fun Appendable.appendJSONTriple(pair: Triple<*, *, *>, config: JSONConfig, references: MutableSet<Any>) {
        append('[')
        appendJSON(pair.first, config, references)
        append(',')
        appendJSON(pair.second, config, references)
        append(',')
        appendJSON(pair.third, config, references)
        append(']')
    }

    private fun Appendable.appendJSONObject(obj: Any, config: JSONConfig, references: MutableSet<Any>) {
        val objClass = obj::class
        if (objClass.isToStringClass() || obj is Enum<*>) {
            JSONFunctions.appendString(this, obj.toString(), config.stringifyNonASCII)
            return
        }
        objClass.findToJSON()?.let {
            try {
                appendJSON(it.call(obj), config, references)
                return
            }
            catch (e: Exception) {
                fatal("Error in custom toJSON - ${objClass.simpleName}", e)
            }
        }
        when (obj) {
            is Iterable<*> -> appendJSONIterator(obj.iterator(), config, references)
            is Iterator<*> -> appendJSONIterator(obj, config, references)
            is Sequence<*> -> appendJSONIterator(obj.iterator(), config, references)
            is Enumeration<*> -> appendJSONIterator(obj.iterator(), config, references)
            is BaseStream<*, *> -> appendJSONIterator(obj.iterator(), config, references)
            is Map<*, *> -> appendJSONMap(obj, config, references)
            is BitSet -> appendJSONBitSet(obj)
            is Calendar -> appendQuoted { DateOutput.appendCalendar(this, obj) }
            is Date -> appendQuoted { DateOutput.appendDate(this, obj) }
            is Duration -> appendQuoted { append(obj.toIsoString()) }
            is Instant -> appendQuoted { DateOutput.appendInstant(this, obj) }
            is OffsetDateTime -> appendQuoted { DateOutput.appendOffsetDateTime(this, obj) }
            is OffsetTime -> appendQuoted { DateOutput.appendOffsetTime(this, obj) }
            is LocalDateTime -> appendQuoted { DateOutput.appendLocalDateTime(this, obj) }
            is LocalDate -> appendQuoted { DateOutput.appendLocalDate(this, obj) }
            is LocalTime -> appendQuoted { DateOutput.appendLocalTime(this, obj) }
            is Year -> appendQuoted { DateOutput.appendYear(this, obj) }
            is YearMonth -> appendQuoted { DateOutput.appendYearMonth(this, obj) }
            is MonthDay -> appendQuoted { DateOutput.appendMonthDay(this, obj) }
            is UUID -> appendQuoted { appendUUID(obj) }
            else -> {
                try {
                    references.add(obj)
                    append('{')
                    var continuation = false
                    val skipName = objClass.findSealedClass()?.let {
                        val discriminatorName = it.findAnnotation<JSONDiscriminator>()?.id ?:
                                config.sealedClassDiscriminator
                        JSONFunctions.appendString(
                            this,
                            discriminatorName,
                            config.stringifyNonASCII
                        )
                        append(':')
                        JSONFunctions.appendString(
                            this,
                            objClass.findAnnotation<JSONIdentifier>()?.id ?: objClass.simpleName ?: "null",
                            config.stringifyNonASCII
                        )
                        continuation = true
                        discriminatorName
                    }
                    val includeAll = config.hasIncludeAllPropertiesAnnotation(objClass.annotations)
                    val statics: Collection<KProperty<*>> = objClass.staticProperties
                    if (objClass.isData && objClass.constructors.isNotEmpty()) {
                        // data classes will be a frequent use of serialization, so optimise for them
                        val constructor = objClass.constructors.first()
                        for (parameter in constructor.parameters) {
                            val member = objClass.members.find { it.name == parameter.name }
                            if (member is KProperty<*>)
                                continuation = appendUsingGetter(member, parameter.annotations, obj, config, references,
                                        includeAll, skipName, continuation)
                        }
                        // now check whether there are any more properties not in constructor
                        for (member in objClass.members) {
                            if (member is KProperty<*> && !statics.contains(member) &&
                                    !constructor.parameters.any { it.name == member.name })
                                continuation = appendUsingGetter(member, member.annotations, obj, config, references,
                                        includeAll, skipName, continuation)
                        }
                    }
                    else {
                        for (member in objClass.members) {
                            if (member is KProperty<*> && !statics.contains(member)) {
                                val combinedAnnotations = ArrayList(member.annotations)
                                objClass.constructors.firstOrNull()?.parameters?.find { it.name == member.name }?.let {
                                    combinedAnnotations.addAll(it.annotations)
                                }
                                continuation = appendUsingGetter(member, combinedAnnotations, obj, config, references,
                                        includeAll, skipName, continuation)
                            }
                        }
                    }
                    append('}')
                }
                finally {
                    references.remove(obj)
                }
            }
        }
    }

    private fun Appendable.appendUsingGetter(member: KProperty<*>, annotations: List<Annotation>?, obj: Any,
            config: JSONConfig, references: MutableSet<Any>, includeAll: Boolean, skipName: String?,
            continuation: Boolean): Boolean {
        if (!config.hasIgnoreAnnotation(annotations)) {
            val name = config.findNameFromAnnotation(annotations) ?: member.name
            if (name != skipName) {
                val wasAccessible = member.isAccessible
                member.isAccessible = true
                try {
                    val v = member.getter.call(obj)
                    if (v != null && v in references)
                        fatal("Circular reference: field ${member.name} in ${obj::class.simpleName}")
                    if (v != null || config.hasIncludeIfNullAnnotation(annotations) || config.includeNulls ||
                            includeAll) {
                        if (continuation)
                            append(',')
                        JSONFunctions.appendString(this, name, config.stringifyNonASCII)
                        append(':')
                        appendJSON(v, config, references)
                        return true
                    }
                }
                catch (e: JSONException) {
                    throw e
                }
                catch (e: Exception) {
                    fatal("Error getting property ${member.name} from ${obj::class.simpleName}", e)
                }
                finally {
                    member.isAccessible = wasAccessible
                }
            }
        }
        return continuation
    }

    private fun Appendable.appendJSONIterator(iterator: Iterator<*>, config: JSONConfig, references: MutableSet<Any>) {
        append('[')
        if (iterator.hasNext()) {
            while (true) {
                appendJSON(iterator.next(), config, references)
                if (!iterator.hasNext())
                    break
                append(',')
            }
        }
        append(']')
    }

    private fun Appendable.appendJSONMap(map: Map<*, *>, config: JSONConfig, references: MutableSet<Any>) {
        append('{')
        map.entries.iterator().let {
            if (it.hasNext()) {
                while (true) {
                    val (key, value) = it.next()
                    JSONFunctions.appendString(this, key.toString(), config.stringifyNonASCII)
                    append(':')
                    appendJSON(value, config, references)
                    if (!it.hasNext())
                        break
                    append(',')
                }
            }
        }
        append('}')
    }

    private fun Appendable.appendJSONBitSet(bitSet: BitSet) {
        append('[')
        var continuation = false
        for (i in 0 until bitSet.length()) {
            if (bitSet.get(i)) {
                if (continuation)
                    append(',')
                appendInt(this, i)
                continuation = true
            }
        }
        append(']')
    }

    private inline fun Appendable.appendQuoted(block: Appendable.() -> Unit) {
        append('"')
        block()
        append('"')
    }

}
