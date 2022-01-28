/*
 * @(#) JSONSerializer.kt
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
import java.util.BitSet
import java.util.Calendar
import java.util.Date
import java.util.Enumeration
import java.util.stream.BaseStream

import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.JSONSerializerFunctions.appendCalendar
import io.kjson.JSONSerializerFunctions.findSealedClass
import io.kjson.JSONSerializerFunctions.findToJSON
import io.kjson.JSONSerializerFunctions.isToStringClass
import io.kjson.annotation.JSONDiscriminator
import io.kjson.annotation.JSONIdentifier

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
            serialize(obj, config, mutableSetOf())

    private fun serialize(obj: Any?, config: JSONConfig, references: MutableSet<Any>): JSONValue? {

        if (obj == null)
            return null

        config.findToJSONMapping(obj::class)?.let { return serialize(it(obj), config, references) }

        return when (obj) {
            is JSONValue -> obj
            is CharSequence -> JSONString.of(obj)
            is Char -> JSONString.of(StringBuilder().append(obj))
            is Number -> serializeNumber(obj, config, references)
            is ULong -> if (obj.toLong() >= 0) JSONLong(obj.toLong()) else JSONDecimal(obj.toString())
            is UInt -> if (obj.toInt() >= 0) JSONInt(obj.toInt()) else JSONLong(obj.toLong())
            is UShort -> JSONInt(obj.toInt())
            is UByte -> JSONInt(obj.toInt())
            is Boolean -> JSONBoolean.of(obj)
            is CharArray -> JSONString.of(StringBuilder().append(obj))
            is Array<*> -> serializeArray(obj, config, references)
            is Pair<*, *> -> serializePair(obj, config, references)
            is Triple<*, *, *> -> serializeTriple(obj, config, references)
            else -> serializeObject(obj, config, references)
        }

    }

    private fun serializeNumber(number: Number, config: JSONConfig, references: MutableSet<Any>): JSONValue? {
        return when (number) {
            is Int -> JSONInt.of(number)
            is Long -> JSONLong.of(number)
            is Short -> JSONInt.of(number.toInt())
            is Byte -> JSONInt.of(number.toInt())
            is Double -> JSONDecimal.of(BigDecimal(number))
            is Float -> JSONDecimal.of(BigDecimal(number.toDouble()))
            is BigInteger -> if (config.bigIntegerString) JSONString(number.toString()) else
                    JSONDecimal(BigDecimal(number))
            is BigDecimal -> if (config.bigDecimalString) JSONString(number.toString()) else JSONDecimal(number)
            else -> serializeObject(number, config, references)
        }
    }

    private fun serializeArray(array: Array<*>, config: JSONConfig, references: MutableSet<Any>): JSONValue {
        return if (array.isArrayOf<Char>())
            JSONString.of(StringBuilder().apply {
                for (ch in array)
                    append(ch)
            })
        else
            JSONArray.Builder(array.size) {
                for (item in array)
                    add(serialize(item, config, references))
            }.build()
    }

    private fun serializePair(pair: Pair<*, *>, config: JSONConfig, references: MutableSet<Any>) =
            JSONArray.Builder(2) {
                add(serialize(pair.first, config, references))
                add(serialize(pair.second, config, references))
            }.build()

    private fun serializeTriple(triple: Triple<*, *, *>, config: JSONConfig, references: MutableSet<Any>) =
            JSONArray.Builder(3) {
                add(serialize(triple.first, config, references))
                add(serialize(triple.second, config, references))
                add(serialize(triple.third, config, references))
            }.build()

    private fun serializeObject(obj: Any, config: JSONConfig, references: MutableSet<Any>): JSONValue? {
        val objClass = obj::class

        if (objClass.isToStringClass() || obj is Enum<*>)
            return JSONString.of(obj.toString())

        try {
            objClass.findToJSON()?.let { return serialize(it.call(obj), config, references) }
        }
        catch (e: Exception) {
            fatal("Error in custom toJSON - ${objClass.simpleName}", e)
        }

        when (obj) {
            is List<*> -> return serializeIterator(obj.iterator(), obj.size, config, references)
            is Iterable<*> -> return serializeIterator(obj.iterator(), 8, config, references)
            is Sequence<*> -> return serializeIterator(obj.iterator(), 8, config, references)
            is BaseStream<*, *> -> return serializeIterator(obj.iterator(), 8, config, references)
            is Iterator<*> -> return serializeIterator(obj, 8, config, references)
            is Enumeration<*> -> return serializeEnumeration(obj, config, references)
            is Map<*, *> -> return serializeMap(obj, config, references)
            is Calendar -> return serializeCalendar(obj)
            is Date -> return serializeCalendar(Calendar.getInstance().apply { time = obj })
            is BitSet -> return serializeBitSet(obj)
            is Duration -> return JSONString(obj.toIsoString())
        }
        return JSONObject.Builder {
            try {
                references.add(obj)
                val skipName = objClass.findSealedClass()?.let {
                    val discriminator = it.findAnnotation<JSONDiscriminator>()?.id ?: config.sealedClassDiscriminator
                    add(discriminator, objClass.findAnnotation<JSONIdentifier>()?.id ?: objClass.simpleName ?: "null")
                    discriminator
                }
                val includeAll = config.hasIncludeAllPropertiesAnnotation(objClass.annotations)
                if (objClass.isData && objClass.constructors.isNotEmpty()) {
                    // data classes will be a frequent use of serialization, so optimise for them
                    val constructor = objClass.constructors.first()
                    for (parameter in constructor.parameters) {
                        val member = objClass.members.find { it.name == parameter.name }
                        if (member is KProperty<*>)
                            addUsingGetter(member, parameter.annotations, obj, config, references, includeAll, skipName)
                    }
                    // now check whether there are any more properties not in constructor
                    val statics: Collection<KProperty<*>> = objClass.staticProperties
                    for (member in objClass.members) {
                        if (member is KProperty<*> && !statics.contains(member) &&
                            !constructor.parameters.any { it.name == member.name })
                            addUsingGetter(member, member.annotations, obj, config, references, includeAll, skipName)
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
                            addUsingGetter(member, combinedAnnotations, obj, config, references, includeAll, skipName)
                        }
                    }
                }
            }
            finally {
                references.remove(obj)
            }
        }.build()
    }

    private fun JSONObject.Builder.addUsingGetter(member: KProperty<*>, annotations: List<Annotation>?, obj: Any,
            config: JSONConfig, references: MutableSet<Any>, includeAll: Boolean, skipName: String?) {
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
                            includeAll)
                        add(name, serialize(v, config, references))
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
    }

    private fun serializeIterator(iterator: Iterator<*>, size: Int, config: JSONConfig, references: MutableSet<Any>) =
            JSONArray.Builder(size) {
                while (iterator.hasNext())
                    add(serialize(iterator.next(), config, references))
            }.build()

    private fun serializeEnumeration(enumeration: Enumeration<*>, config: JSONConfig, references: MutableSet<Any>) =
            JSONArray.build {
                while (enumeration.hasMoreElements())
                    add(serialize(enumeration.nextElement(), config, references))
            }

    private fun serializeMap(map: Map<*, *>, config: JSONConfig, references: MutableSet<Any>) =
            JSONObject.Builder(map.size) {
                for (entry in map.entries)
                    add(entry.key.toString(), serialize(entry.value, config, references))
            }.build()

    private fun serializeCalendar(calendar: Calendar) =
            JSONString.of(StringBuilder().apply { appendCalendar(calendar) })

    private fun serializeBitSet(bitSet: BitSet)  =
            JSONArray.build {
                for (i in 0 until bitSet.length())
                    if (bitSet.get(i))
                        add(i)
            }

}
