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

import io.kjson.JSONSerializerFunctions.appendUUID
import io.kjson.JSONSerializerFunctions.discriminatorName
import io.kjson.JSONSerializerFunctions.discriminatorValue
import io.kjson.JSONSerializerFunctions.findSealedClass
import io.kjson.JSONSerializerFunctions.findToJSON
import io.kjson.JSONSerializerFunctions.getCombinedAnnotations
import io.kjson.JSONSerializerFunctions.isFinalClass
import io.kjson.JSONSerializerFunctions.isToStringClass
import io.kjson.optional.Opt
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
            if (obj == null) null else serialize(obj, JSONContext(config), mutableListOf())

    /**
     * Serialize the given object to a [JSONValue], using a [JSONContext].
     *
     * @param   obj         the object to be serialized
     * @param   context     a [JSONContext]
     * @return              the [JSONValue] (or `null` if the input is `null`)
     */
    fun serialize(obj: Any?, context: JSONContext): JSONValue? =
            if (obj == null) null else serialize(obj, context, mutableListOf())

    private fun serialize(
        obj: Any?,
        context: JSONContext,
        references: MutableList<Any>,
    ): JSONValue? {

        if (obj == null)
            return null

        if (obj is JSONValue)
            return obj

        val objClass = obj::class
        context.config.findToJSONMapping(objClass)?.let {
            return serialize(context.it(obj), context, references)
        }

        return when {
            obj is CharSequence -> JSONString.of(obj)
            obj is Number -> serializeNumber(obj, context, references)
            objClass.isFinalClass() -> serializeFinalClass(obj)
            obj is Enum<*> || objClass.isToStringClass() -> JSONString.of(obj.toString())
            obj is BitSet -> serializeBitSet(obj)
            obj is Calendar -> serializeSystemClass(29, obj) { DateOutput.appendCalendar(this, it) }
            obj is Date -> serializeSystemClass(24, obj) { DateOutput.appendDate(this, it) }
            else -> serializeObject(obj, context, references)
        }

    }

    private fun serializeFinalClass(obj: Any): JSONValue = when (obj) {
        is Boolean -> JSONBoolean.of(obj)
        is UUID -> serializeSystemClass(36, obj) { appendUUID(it) }
        is OffsetDateTime -> serializeSystemClass(35, obj) { DateOutput.appendOffsetDateTime(this, it) }
        is LocalDate -> serializeSystemClass(10, obj) { DateOutput.appendLocalDate(this, it) }
        is Instant -> serializeSystemClass(30, obj) { DateOutput.appendInstant(this, it) }
        is OffsetTime -> serializeSystemClass(24, obj) { DateOutput.appendOffsetTime(this, it) }
        is LocalDateTime -> serializeSystemClass(29, obj) { DateOutput.appendLocalDateTime(this, it) }
        is LocalTime -> serializeSystemClass(18, obj) { DateOutput.appendLocalTime(this, it) }
        is Year -> serializeSystemClass(4, obj) { DateOutput.appendYear(this, it) }
        is YearMonth -> serializeSystemClass(7, obj) { DateOutput.appendYearMonth(this, it) }
        is MonthDay -> serializeSystemClass(7, obj) { DateOutput.appendMonthDay(this, it) }
        is Duration -> JSONString(obj.toIsoString())
        is Char -> JSONString.of(StringBuilder(1).append(obj))
        is CharArray -> JSONString.of(StringBuilder(obj.size).append(obj))
        is IntArray -> serializeTypedArray(obj.size) { JSONInt.of(obj[it]) }
        is LongArray -> serializeTypedArray(obj.size) { JSONLong.of(obj[it]) }
        is ByteArray -> serializeTypedArray(obj.size) { JSONInt.of(obj[it].toInt()) }
        is ShortArray -> serializeTypedArray(obj.size) { JSONInt.of(obj[it].toInt()) }
        is FloatArray -> serializeTypedArray(obj.size) { JSONDecimal.of(BigDecimal(obj[it].toDouble())) }
        is DoubleArray -> serializeTypedArray(obj.size) { JSONDecimal.of(BigDecimal(obj[it])) }
        is BooleanArray -> serializeTypedArray(obj.size) { JSONBoolean.of(obj[it]) }
        is UInt -> if (obj.toInt() >= 0) JSONInt(obj.toInt()) else JSONLong(obj.toLong())
        is UShort -> JSONInt(obj.toInt())
        is UByte -> JSONInt(obj.toInt())
        is ULong -> if (obj.toLong() >= 0) JSONLong(obj.toLong()) else JSONDecimal(obj.toString())
        else -> throw JSONException("Internal error serializing ${obj::class}")
    }

    private fun serializeNumber(
        number: Number,
        context: JSONContext,
        references: MutableList<Any>,
    ): JSONValue? = when (number) {
        is Int -> JSONInt.of(number)
        is Long -> JSONLong.of(number)
        is Short -> JSONInt.of(number.toInt())
        is Byte -> JSONInt.of(number.toInt())
        is Double -> JSONDecimal.of(BigDecimal(number))
        is Float -> JSONDecimal.of(BigDecimal(number.toDouble()))
        is BigInteger -> if (context.config.bigIntegerString) JSONString(number.toString()) else
                JSONDecimal(BigDecimal(number))
        is BigDecimal -> if (context.config.bigDecimalString) JSONString(number.toString()) else JSONDecimal(number)
        else -> serializeObject(number, context, references)
    }

    private fun serializeObject(
        obj: Any,
        context: JSONContext,
        references: MutableList<Any>,
    ): JSONValue? {
        if (obj in references)
            context.fatal("Circular reference to ${obj::class.simpleName}")
        references.add(obj)
        try {
            val objClass = obj::class

            try {
                objClass.findToJSON()?.let { return serialize(it.call(obj), context, references) }
            }
            catch (e: JSONException) {
                throw e
            }
            catch (e: Exception) {
                context.fatal("Error in custom toJSON - ${objClass.simpleName}", e)
            }

            return when (obj) {
                is List<*> -> serializeIterator(obj.iterator(), obj.size, context, references)
                is Iterable<*> -> serializeIterator(obj.iterator(), 8, context, references)
                is Array<*> -> serializeArray(obj, context, references)
                is Sequence<*> -> serializeIterator(obj.iterator(), 8, context, references)
                is BaseStream<*, *> -> serializeIterator(obj.iterator(), 8, context, references)
                is Iterator<*> -> serializeIterator(obj, 8, context, references)
                is Enumeration<*> -> serializeEnumeration(obj, context, references)
                is Map<*, *> -> serializeMap(obj, context, references)
                is Pair<*, *> -> serializePair(obj, context, references)
                is Triple<*, *, *> -> serializeTriple(obj, context, references)
                is Opt<*> -> serialize(obj.orNull, context, references)
                else -> JSONObject.build {
                    val skipName = objClass.findSealedClass()?.let {
                        it.discriminatorName(context).also {
                            name -> add(name, objClass.discriminatorValue())
                        }
                    }
                    val includeAll = context.config.includeNullFields(objClass)
                    if (objClass.isData && objClass.constructors.isNotEmpty()) {
                        // data classes will be a frequent use of serialization, so optimise for them
                        val constructor = objClass.constructors.first()
                        for (parameter in constructor.parameters) {
                            val member = objClass.members.find { it.name == parameter.name }
                            if (member is KProperty<*>)
                                addUsingGetter(
                                    member = member,
                                    annotations = parameter.annotations,
                                    obj = obj,
                                    context = context,
                                    references = references,
                                    includeAll = includeAll,
                                    skipName = skipName,
                                )
                        }
                        // now check whether there are any more properties not in constructor
                        val statics: Collection<KProperty<*>> = objClass.staticProperties
                        for (member in objClass.members) {
                            if (member is KProperty<*> && !statics.contains(member) &&
                                    !constructor.parameters.any { it.name == member.name })
                                addUsingGetter(
                                    member = member,
                                    annotations = member.annotations,
                                    obj = obj,
                                    context = context,
                                    references = references,
                                    includeAll = includeAll,
                                    skipName = skipName,
                                )
                        }
                    }
                    else {
                        val statics: Collection<KProperty<*>> = objClass.staticProperties
                        for (member in objClass.members) {
                            if (member is KProperty<*> && !statics.contains(member))
                                addUsingGetter(
                                    member = member,
                                    annotations = member.getCombinedAnnotations(objClass),
                                    obj = obj,
                                    context = context,
                                    references = references,
                                    includeAll = includeAll,
                                    skipName = skipName,
                                )
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
        context: JSONContext,
        references: MutableList<Any>,
        includeAll: Boolean,
        skipName: String?,
    ) {
        val config = context.config
        if (!config.hasIgnoreAnnotation(annotations)) {
            val name = config.findNameFromAnnotation(annotations) ?: member.name
            if (name != skipName) {
                val wasAccessible = member.isAccessible
                member.isAccessible = true
                try {
                    val v = member.getter.call(obj)
                    if (v != null || includeAll || config.hasIncludeIfNullAnnotation(annotations)) {
                        if (v is Opt<*>)
                            v.ifSet { add(name, serialize(it, context.child(name), references)) }
                        else
                            add(name, serialize(v, context.child(name), references))
                    }
                }
                catch (e: JSONException) {
                    throw e
                }
                catch (e: Exception) {
                    context.fatal("Error getting property ${member.name} from ${obj::class.simpleName}", e)
                }
                finally {
                    member.isAccessible = wasAccessible
                }
            }
        }
    }

    private fun serializeArray(
        array: Array<*>,
        context: JSONContext,
        references: MutableList<Any>,
    ): JSONValue = if (array.isArrayOf<Char>())
        JSONString.of(StringBuilder(array.size).apply {
            for (ch in array)
                append(ch)
        })
    else
        JSONArray.Builder(array.size) {
            for (i in array.indices)
                add(serialize(array[i], context.child(i), references))
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
        context: JSONContext,
        references: MutableList<Any>,
    ) = JSONArray.Builder(2) {
        add(serialize(pair.first, context.child(0), references))
        add(serialize(pair.second, context.child(1), references))
    }.build()

    private fun serializeTriple(
        triple: Triple<*, *, *>,
        context: JSONContext,
        references: MutableList<Any>,
    ) = JSONArray.Builder(3) {
        add(serialize(triple.first, context.child(0), references))
        add(serialize(triple.second, context.child(1), references))
        add(serialize(triple.third, context.child(2), references))
    }.build()

    private fun serializeIterator(
        iterator: Iterator<*>,
        size: Int,
        context: JSONContext,
        references: MutableList<Any>,
    ) = JSONArray.Builder(size) {
        var index = 0
        while (iterator.hasNext())
            add(serialize(iterator.next(), context.child(index++), references))
    }.build()

    private fun serializeEnumeration(
        enumeration: Enumeration<*>,
        context: JSONContext,
        references: MutableList<Any>,
    ) = JSONArray.build {
        var index = 0
        while (enumeration.hasMoreElements())
            add(serialize(enumeration.nextElement(), context.child(index++), references))
    }

    private fun serializeMap(
        map: Map<*, *>,
        context: JSONContext,
        references: MutableList<Any>,
    ) = JSONObject.Builder(map.size) {
        for (entry in map.entries) {
            val keyString = entry.key.toString()
            add(keyString, serialize(entry.value, context.child(keyString), references))
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
