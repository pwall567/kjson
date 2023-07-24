/*
 * @(#) JSONSerializer.kt
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
import io.kjson.pointer.JSONPointer
import net.pwall.util.DateOutput

/**
 * Reflection-based JSON serialization for Kotlin.
 *
 * @author  Peter Wall
 */
object JSONSerializer {

    /**
     * Serialize the given object to a [JSONValue].
     *
     * @param   obj         the object to be serialized
     * @param   config      an optional [JSONConfig]
     * @return              the [JSONValue] (or `null` if the input is `null`)
     */
    fun serialize(obj: Any?, config: JSONConfig = JSONConfig.defaultConfig): JSONValue? =
            serialize(obj, config, mutableListOf(), mutableListOf())

    private fun serializeChild(
        obj: Any?,
        config: JSONConfig,
        references: MutableList<Any>,
        pointer: MutableList<String>,
        child: String,
    ): JSONValue? {
        pointer.add(child)
        return serialize(obj, config, references, pointer).also { pointer.removeLast() }
    }

    private fun serialize(
        obj: Any?,
        config: JSONConfig,
        references: MutableList<Any>,
        pointer: MutableList<String>,
    ): JSONValue? {

        if (obj == null)
            return null

        config.findToJSONMapping(obj::class)?.let { return serialize(config.it(obj), config, references, pointer) }

        if (obj is Enum<*> || obj::class.isToStringClass())
            return JSONString.of(obj.toString())

        return when (obj) {
            is JSONValue -> obj
            is CharSequence -> JSONString.of(obj)
            is CharArray -> JSONString.of(StringBuilder().append(obj))
            is Char -> JSONString.of(StringBuilder().append(obj))
            is Number -> serializeNumber(obj, config, references, pointer)
            is Boolean -> JSONBoolean.of(obj)
            is UInt -> if (obj.toInt() >= 0) JSONInt(obj.toInt()) else JSONLong(obj.toLong())
            is UShort -> JSONInt(obj.toInt())
            is UByte -> JSONInt(obj.toInt())
            is ULong -> if (obj.toLong() >= 0) JSONLong(obj.toLong()) else JSONDecimal(obj.toString())
            is BitSet -> serializeBitSet(obj)
            is Calendar -> serializeSystemClass(29, obj) { DateOutput.appendCalendar(this, it) }
            is Date -> serializeSystemClass(24, obj) { DateOutput.appendDate(this, it) }
            is Duration -> JSONString(obj.toIsoString())
            is Instant -> serializeSystemClass(30, obj) { DateOutput.appendInstant(this, it) }
            is OffsetDateTime -> serializeSystemClass(35, obj) { DateOutput.appendOffsetDateTime(this, it) }
            is OffsetTime -> serializeSystemClass(24, obj) { DateOutput.appendOffsetTime(this, it) }
            is LocalDateTime -> serializeSystemClass(29, obj) { DateOutput.appendLocalDateTime(this, it) }
            is LocalDate -> serializeSystemClass(10, obj) { DateOutput.appendLocalDate(this, it) }
            is LocalTime -> serializeSystemClass(18, obj) { DateOutput.appendLocalTime(this, it) }
            is Year -> serializeSystemClass(4, obj) { DateOutput.appendYear(this, it) }
            is YearMonth -> serializeSystemClass(7, obj) { DateOutput.appendYearMonth(this, it) }
            is MonthDay -> serializeSystemClass(7, obj) { DateOutput.appendMonthDay(this, it) }
            is UUID -> serializeSystemClass(36, obj) { appendUUID(it) }
            is IntArray -> serializeTypedArray(obj.size) { JSONInt.of(obj[it]) }
            is LongArray -> serializeTypedArray(obj.size) { JSONLong.of(obj[it]) }
            is ByteArray -> serializeTypedArray(obj.size) { JSONInt.of(obj[it].toInt()) }
            is ShortArray -> serializeTypedArray(obj.size) { JSONInt.of(obj[it].toInt()) }
            is FloatArray -> serializeTypedArray(obj.size) { JSONDecimal.of(BigDecimal(obj[it].toDouble())) }
            is DoubleArray -> serializeTypedArray(obj.size) { JSONDecimal.of(BigDecimal(obj[it])) }
            is BooleanArray -> serializeTypedArray(obj.size) { JSONBoolean.of(obj[it]) }
            else -> serializeObject(obj, config, references, pointer)
        }

    }

    private fun serializeNumber(
        number: Number,
        config: JSONConfig,
        references: MutableList<Any>,
        pointer: MutableList<String>,
    ): JSONValue? = when (number) {
        is Int -> JSONInt.of(number)
        is Long -> JSONLong.of(number)
        is Short -> JSONInt.of(number.toInt())
        is Byte -> JSONInt.of(number.toInt())
        is Double -> JSONDecimal.of(BigDecimal(number))
        is Float -> JSONDecimal.of(BigDecimal(number.toDouble()))
        is BigInteger -> if (config.bigIntegerString) JSONString(number.toString()) else
                JSONDecimal(BigDecimal(number))
        is BigDecimal -> if (config.bigDecimalString) JSONString(number.toString()) else JSONDecimal(number)
        else -> serializeObject(number, config, references, pointer)
    }

    private fun serializeObject(
        obj: Any,
        config: JSONConfig,
        references: MutableList<Any>,
        pointer: MutableList<String>,
    ): JSONValue? {
        if (obj in references)
            fatal("Circular reference to ${obj::class.simpleName}", JSONPointer.from(pointer))
        references.add(obj)
        try {
            val objClass = obj::class

            try {
                objClass.findToJSON()?.let { return serialize(it.call(obj), config, references, pointer) }
            }
            catch (e: Exception) {
                fatal("Error in custom toJSON - ${objClass.simpleName}", JSONPointer.from(pointer), e)
            }

            return when (obj) {
                is Array<*> -> serializeArray(obj, config, references, pointer)
                is Pair<*, *> -> serializePair(obj, config, references, pointer)
                is Triple<*, *, *> -> serializeTriple(obj, config, references, pointer)
                is List<*> -> serializeIterator(obj.iterator(), obj.size, config, references, pointer)
                is Iterable<*> -> serializeIterator(obj.iterator(), 8, config, references, pointer)
                is Sequence<*> -> serializeIterator(obj.iterator(), 8, config, references, pointer)
                is BaseStream<*, *> -> serializeIterator(obj.iterator(), 8, config, references, pointer)
                is Iterator<*> -> serializeIterator(obj, 8, config, references, pointer)
                is Enumeration<*> -> serializeEnumeration(obj, config, references, pointer)
                is Map<*, *> -> serializeMap(obj, config, references, pointer)
                is Opt<*> -> serialize(obj.orNull, config, references, pointer)
                else -> JSONObject.build {
                    val skipName = objClass.findSealedClass()?.let {
                        val discriminator = it.findAnnotation<JSONDiscriminator>()?.id ?:
                                config.sealedClassDiscriminator
                        add(discriminator,
                                objClass.findAnnotation<JSONIdentifier>()?.id ?: objClass.simpleName ?: "null")
                        discriminator
                    }
                    val includeAll = config.hasIncludeAllPropertiesAnnotation(objClass.annotations)
                    if (objClass.isData && objClass.constructors.isNotEmpty()) {
                        // data classes will be a frequent use of serialization, so optimise for them
                        val constructor = objClass.constructors.first()
                        for (parameter in constructor.parameters) {
                            val member = objClass.members.find { it.name == parameter.name }
                            if (member is KProperty<*>)
                                addUsingGetter(member, parameter.annotations, obj, config, references, pointer,
                                        includeAll, skipName)
                        }
                        // now check whether there are any more properties not in constructor
                        val statics: Collection<KProperty<*>> = objClass.staticProperties
                        for (member in objClass.members) {
                            if (member is KProperty<*> && !statics.contains(member) &&
                                !constructor.parameters.any { it.name == member.name })
                                addUsingGetter(member, member.annotations, obj, config, references, pointer, includeAll,
                                        skipName)
                        }
                    }
                    else {
                        val statics: Collection<KProperty<*>> = objClass.staticProperties
                        for (member in objClass.members) {
                            if (member is KProperty<*> && !statics.contains(member)) {
                                val combinedAnnotations = ArrayList(member.annotations)
                                objClass.constructors.firstOrNull()?.parameters?.find { it.name == member.name }?.let {
                                    combinedAnnotations.addAll(it.annotations)
                                }
                                addUsingGetter(member, combinedAnnotations, obj, config, references, pointer,
                                        includeAll, skipName)
                            }
                        }
                    }
                }

            }
        }
        finally {
            references.remove(obj)
        }
    }

    private fun JSONObject.Builder.addUsingGetter(
        member: KProperty<*>,
        annotations: List<Annotation>?,
        obj: Any,
        config: JSONConfig,
        references: MutableList<Any>,
        pointer: MutableList<String>,
        includeAll: Boolean,
        skipName: String?,
    ) {
        if (!config.hasIgnoreAnnotation(annotations)) {
            val name = config.findNameFromAnnotation(annotations) ?: member.name
            if (name != skipName) {
                val wasAccessible = member.isAccessible
                member.isAccessible = true
                try {
                    val v = member.getter.call(obj)
                    if (v != null || config.hasIncludeIfNullAnnotation(annotations) || config.includeNulls ||
                            includeAll) {
                        if (v is Opt<*>)
                            v.ifSet { add(name, serializeChild(it, config, references, pointer, name)) }
                        else
                            add(name, serializeChild(v, config, references, pointer, name))
                    }
                }
                catch (e: JSONException) {
                    throw e
                }
                catch (e: Exception) {
                    fatal("Error getting property ${member.name} from ${obj::class.simpleName}",
                            JSONPointer.from(pointer), e)
                }
                finally {
                    member.isAccessible = wasAccessible
                }
            }
        }
    }

    private fun serializeArray(
        array: Array<*>,
        config: JSONConfig,
        references: MutableList<Any>,
        pointer: MutableList<String>,
    ): JSONValue = if (array.isArrayOf<Char>())
        JSONString.of(StringBuilder(array.size).apply {
            for (ch in array)
                append(ch)
        })
    else
        JSONArray.Builder(array.size) {
            for (i in array.indices)
                add(serializeChild(array[i], config, references, pointer, i.toString()))
        }.build()

    private fun serializeTypedArray(
        size: Int,
        itemFunction: (Int) -> JSONValue?
    ): JSONArray = JSONArray.Builder(size) {
        for (i in 0 until size)
            add(itemFunction(i))
    }.build()

    private fun serializePair(
        pair: Pair<*, *>,
        config: JSONConfig,
        references: MutableList<Any>,
        pointer: MutableList<String>,
    ) = JSONArray.Builder(2) {
        add(serializeChild(pair.first, config, references, pointer, "0"))
        add(serializeChild(pair.second, config, references, pointer, "1"))
    }.build()

    private fun serializeTriple(
        triple: Triple<*, *, *>,
        config: JSONConfig,
        references: MutableList<Any>,
        pointer: MutableList<String>,
    ) = JSONArray.Builder(3) {
        add(serializeChild(triple.first, config, references, pointer, "0"))
        add(serializeChild(triple.second, config, references, pointer, "1"))
        add(serializeChild(triple.third, config, references, pointer, "2"))
    }.build()

    private fun serializeIterator(
        iterator: Iterator<*>,
        size: Int,
        config: JSONConfig,
        references: MutableList<Any>,
        pointer: MutableList<String>,
    ) = JSONArray.Builder(size) {
        var index = 0
        while (iterator.hasNext())
            add(serializeChild(iterator.next(), config, references, pointer, (index++).toString()))
    }.build()

    private fun serializeEnumeration(
        enumeration: Enumeration<*>,
        config: JSONConfig,
        references: MutableList<Any>,
        pointer: MutableList<String>,
    ) = JSONArray.build {
        var index = 0
        while (enumeration.hasMoreElements())
            add(serializeChild(enumeration.nextElement(), config, references, pointer, (index++).toString()))
    }

    private fun serializeMap(
        map: Map<*, *>,
        config: JSONConfig,
        references: MutableList<Any>,
        pointer: MutableList<String>,
    ) = JSONObject.Builder(map.size) {
        for (entry in map.entries) {
            val keyString = entry.key.toString()
            add(keyString, serializeChild(entry.value, config, references, pointer, keyString))
        }
    }.build()

    private fun serializeBitSet(bitSet: BitSet) = JSONArray.build {
        for (i in 0 until bitSet.length())
            if (bitSet.get(i))
                add(i)
    }

    private fun <T> serializeSystemClass(initialSize: Int, value: T, block: StringBuilder.(T) -> Unit) =
            JSONString.of(StringBuilder(initialSize).apply { block(value) })

}
