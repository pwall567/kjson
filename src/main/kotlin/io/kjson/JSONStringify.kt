/*
 * @(#) JSONStringify.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023 Peter Wall
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
import io.kjson.optional.Opt

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
        appendJSON(obj, JSONContext(config), mutableListOf())
    }

    private fun Appendable.appendJSON(
        obj: Any?,
        context: JSONContext,
        references: MutableList<Any>,
    ) {

        if (obj == null) {
            append("null")
            return
        }

        val config = context.config
        config.findToJSONMapping(obj::class)?.let {
            appendJSON(context.it(obj), context, references)
            return
        }

        if (obj is Enum<*> || obj::class.isToStringClass()) {
            JSONFunctions.appendString(this, obj.toString(), config.stringifyNonASCII)
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
            is Number -> appendJSONNumber(obj, context, references)
            is Boolean -> append(obj.toString())
            is UInt -> appendUnsignedInt(this, obj.toInt())
            is UShort -> appendInt(this, obj.toInt())
            is UByte -> appendInt(this, obj.toInt())
            is ULong -> appendUnsignedLong(this, obj.toLong())
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
            is IntArray -> appendJSONTypedArray(obj.size) { appendInt(this, obj[it]) }
            is LongArray -> appendJSONTypedArray(obj.size) { appendLong(this, obj[it]) }
            is ByteArray -> appendJSONTypedArray(obj.size) { appendInt(this, obj[it].toInt()) }
            is ShortArray -> appendJSONTypedArray(obj.size) { appendInt(this, obj[it].toInt()) }
            is FloatArray -> appendJSONTypedArray(obj.size) { append(obj[it].toString()) }
            is DoubleArray -> appendJSONTypedArray(obj.size) { append(obj[it].toString()) }
            is BooleanArray -> appendJSONTypedArray(obj.size) { append(obj[it].toString()) }
            else -> appendJSONObject(obj, context, references)
        }

    }

    private fun Appendable.appendJSONNumber(
        number: Number,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        when (number) {
            is Int -> appendInt(this, number)
            is Short, is Byte -> appendInt(this, number.toInt())
            is Long -> appendLong(this, number)
            is Float, is Double -> append(number.toString())
            is BigInteger -> {
                if (context.config.bigIntegerString)
                    JSONFunctions.appendString(this, number.toString(), false)
                else
                    append(number.toString())
            }
            is BigDecimal -> {
                if (context.config.bigDecimalString)
                    JSONFunctions.appendString(this, number.toString(), false)
                else
                    append(number.toString())
            }
            else -> appendJSONObject(number, context, references)
        }
    }

    private fun Appendable.appendJSONObject(
        obj: Any,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        if (obj in references)
            fatal("Circular reference to ${obj::class.simpleName}", context)
        references.add(obj)
        try {
            val objClass = obj::class
            objClass.findToJSON()?.let {
                try {
                    appendJSON(it.call(obj), context, references)
                    return
                }
                catch (e: Exception) {
                    fatal("Error in custom toJSON - ${objClass.simpleName}", context, e)
                }
            }
            when (obj) {
                is Array<*> -> appendJSONArray(obj, context, references)
                is Pair<*, *> -> appendJSONPair(obj, context, references)
                is Triple<*, *, *> -> appendJSONTriple(obj, context, references)
                is Iterable<*> -> appendJSONIterator(obj.iterator(), context, references)
                is Iterator<*> -> appendJSONIterator(obj, context, references)
                is Sequence<*> -> appendJSONIterator(obj.iterator(), context, references)
                is Enumeration<*> -> appendJSONIterator(obj.iterator(), context, references)
                is BaseStream<*, *> -> appendJSONIterator(obj.iterator(), context, references)
                is Map<*, *> -> appendJSONMap(obj, context, references)
                is Opt<*> -> appendJSON(obj.orNull, context, references)
                else -> {
                    val config = context.config
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
                                continuation = appendUsingGetter(member, parameter.annotations, obj, context,
                                        references, includeAll, skipName, continuation)
                        }
                        // now check whether there are any more properties not in constructor
                        for (member in objClass.members) {
                            if (member is KProperty<*> && !statics.contains(member) &&
                                    !constructor.parameters.any { it.name == member.name })
                                continuation = appendUsingGetter(member, member.annotations, obj, context, references,
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
                                continuation = appendUsingGetter(member, combinedAnnotations, obj, context, references,
                                        includeAll, skipName, continuation)
                            }
                        }
                    }
                    append('}')
                }
            }
        }
        finally {
            references.remove(obj)
        }
    }

    private fun Appendable.appendUsingGetter(
        member: KProperty<*>,
        annotations: List<Annotation>?,
        obj: Any,
        context: JSONContext,
        references: MutableList<Any>,
        includeAll: Boolean,
        skipName: String?,
        continuation: Boolean,
    ): Boolean {
        val config = context.config
        if (!config.hasIgnoreAnnotation(annotations)) {
            val name = config.findNameFromAnnotation(annotations) ?: member.name
            if (name != skipName) {
                val wasAccessible = member.isAccessible
                member.isAccessible = true
                try {
                    val v = member.getter.call(obj)
                    if (v != null || config.hasIncludeIfNullAnnotation(annotations) || config.includeNulls ||
                            includeAll) {
                        if (v is Opt<*>) {
                            if (v.isSet) {
                                appendObjectValue(name, v.value, context, references, continuation)
                                return true
                            }
                        }
                        else {
                            appendObjectValue(name, v, context, references, continuation)
                            return true
                        }
                    }
                }
                catch (e: JSONException) {
                    throw e
                }
                catch (e: Exception) {
                    fatal("Error getting property ${member.name} from ${obj::class.simpleName}", context, e)
                }
                finally {
                    member.isAccessible = wasAccessible
                }
            }
        }
        return continuation
    }

    private fun Appendable.appendObjectValue(
        name: String,
        value: Any?,
        context: JSONContext,
        references: MutableList<Any>,
        continuation: Boolean,
    ) {
        if (continuation)
            append(',')
        JSONFunctions.appendString(this, name, context.config.stringifyNonASCII)
        append(':')
        appendJSON(value, context.child(name), references)
    }

    private fun Appendable.appendJSONArray(
        array: Array<*>,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        if (array.isArrayOf<Char>()) {
            appendQuoted {
                for (ch in array)
                    JSONFunctions.appendChar(this, ch as Char, context.config.stringifyNonASCII)
            }
        }
        else
            appendJSONTypedArray(array.size) { appendJSON(array[it], context.child(it), references) }
    }

    private fun Appendable.appendJSONTypedArray(
        size: Int,
        itemFunction: Appendable.(Int) -> Unit,
    ) {
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

    private fun Appendable.appendJSONPair(
        pair: Pair<*, *>,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        append('[')
        appendJSON(pair.first, context.child(0), references)
        append(',')
        appendJSON(pair.second, context.child(1), references)
        append(']')
    }

    private fun Appendable.appendJSONTriple(
        pair: Triple<*, *, *>,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        append('[')
        appendJSON(pair.first, context.child(0), references)
        append(',')
        appendJSON(pair.second, context.child(1), references)
        append(',')
        appendJSON(pair.third, context.child(2), references)
        append(']')
    }

    private fun Appendable.appendJSONIterator(
        iterator: Iterator<*>,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        append('[')
        if (iterator.hasNext()) {
            var index = 0
            while (true) {
                appendJSON(iterator.next(), context.child(index++), references)
                if (!iterator.hasNext())
                    break
                append(',')
            }
        }
        append(']')
    }

    private fun Appendable.appendJSONMap(
        map: Map<*, *>,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        append('{')
        map.entries.iterator().let {
            if (it.hasNext()) {
                while (true) {
                    val (key, value) = it.next()
                    val keyString = key.toString()
                    JSONFunctions.appendString(this, keyString, context.config.stringifyNonASCII)
                    append(':')
                    appendJSON(value, context.child(keyString), references)
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
