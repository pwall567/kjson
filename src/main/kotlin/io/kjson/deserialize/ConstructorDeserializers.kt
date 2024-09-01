/*
 * @(#) ConstructorDeserializers.kt
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

package io.kjson.deserialize

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

import io.kjson.JSON.asByteOr
import io.kjson.JSON.asDecimalOr
import io.kjson.JSON.asIntOr
import io.kjson.JSON.asLongOr
import io.kjson.JSON.asShortOr
import io.kjson.JSON.asStringOr
import io.kjson.JSON.asUByteOr
import io.kjson.JSON.asUIntOr
import io.kjson.JSON.asULongOr
import io.kjson.JSON.asUShortOr
import io.kjson.JSONConfig
import io.kjson.JSONDeserializer.findDeserializer
import io.kjson.JSONDeserializerFunctions.callWithSingle
import io.kjson.JSONObject
import io.kjson.JSONValue
import net.pwall.util.ImmutableMap
import net.pwall.util.ImmutableMapEntry

class StringConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T = constructor.callWithSingle(json.asStringOr { typeError("String") })

}

class IntConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T = constructor.callWithSingle(json.asIntOr { typeError("Int") })

}

class LongConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T = constructor.callWithSingle(json.asLongOr { typeError("Long") })

}

class ShortConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T = constructor.callWithSingle(json.asShortOr { typeError("Short") })

}

class ByteConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T = constructor.callWithSingle(json.asByteOr { typeError("Byte") })

}

class UIntConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T = constructor.callWithSingle(json.asUIntOr { typeError("UInt") })

}

class ULongConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T = constructor.callWithSingle(json.asULongOr { typeError("ULong") })

}

class UShortConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T = constructor.callWithSingle(json.asUShortOr { typeError("UShort") })

}

class UByteConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T = constructor.callWithSingle(json.asUByteOr { typeError("UByte") })

}

class DoubleConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T =
        constructor.callWithSingle(json.asDecimalOr { typeError("Double") }.toDouble())

}

class FloatConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T =
        constructor.callWithSingle(json.asDecimalOr { typeError("Float") }.toFloat())

}

class BigIntegerConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T =
        constructor.callWithSingle(json.asDecimalOr { typeError("BigInteger") }.toBigInteger())

}

class BigDecimalConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T =
        constructor.callWithSingle(json.asDecimalOr { typeError("BigDecimal") })

}

class ArrayConstructorDeserializer<T : Any>(
    private val constructor: KFunction<T>,
    itemClass: KClass<Any>,
    itemDeserializer: Deserializer<Any>,
    itemNullable: Boolean,
): Deserializer<T> {

    private val arrayDeserializer = ArrayDeserializer(itemClass, itemDeserializer, itemNullable)

    override fun deserialize(json: JSONValue?): T? =
        arrayDeserializer.deserialize(json)?.let { constructor.callWithSingle(it) }

}

class ListConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
    itemDeserializer: Deserializer<Any>,
    itemNullable: Boolean,
    private val listNullable: Boolean,
): Deserializer<T> {

    private val listDeserializer = CollectionDeserializer(itemDeserializer, itemNullable) { n -> ArrayList(n) }

    override fun deserialize(json: JSONValue?): T? {
        val list = listDeserializer.deserialize(json)
        if (list == null && !listNullable)
            throw DeserializationException("List may not be null")
        return constructor.callWithSingle(list)
    }

}

class MapConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
    keyDeserializer: Deserializer<Any>,
    keyNullable: Boolean,
    valueDeserializer: Deserializer<Any>,
    valueNullable: Boolean,
    private val mapNullable: Boolean,
): Deserializer<T> {

    private val mapDeserializer = MapDeserializer(
        keyDeserializer = keyDeserializer,
        keyNullable = keyNullable,
        valueDeserializer = valueDeserializer,
        valueNullable= valueNullable,
    ) { size -> LinkedHashMap(size) }

    override fun deserialize(json: JSONValue?): T? {
        val map = mapDeserializer.deserialize(json)
        if (map == null && !mapNullable)
            throw DeserializationException("Map may not be null")
        return constructor.callWithSingle(map)
    }

}

class DelegatingMapConstructorDeserializer<T>(
    private val constructor: KFunction<T>,
    private val valueDeserializer: Deserializer<Any>,
    private val valueNullable: Boolean,
    private val members: Collection<KCallable<*>>,
    private val config: JSONConfig,
): Deserializer<T> {

    override fun deserialize(json: JSONValue?): T? {
        if (json == null)
            return null
        if (json !is JSONObject)
            typeError("object")
        val array: Array<ImmutableMapEntry<String, Any?>> = ImmutableMap.createArray(json.size)
        for (index in 0 until json.size) {
            val property = json[index]
            val name = property.name
            val member = members.find {
                it is KProperty<*> && (config.findNameFromAnnotation(it.annotations) ?: it.name) == name
            }
            val memberType = member?.returnType
            val memberDeserializer = memberType?.let { findDeserializer(it, config, mutableListOf()) } ?:
                    valueDeserializer
            val memberNullable = memberType?.isMarkedNullable ?: valueNullable
            val value = try {
                memberDeserializer.deserialize(property.value)
            } catch (de: DeserializationException) {
                throw de.nested(name)
            }
            if (value == null && !memberNullable)
                throw DeserializationException("Property may not be null", name)
            array[index] = ImmutableMap.entry(name, value)
        }
        return constructor.callWithSingle(ImmutableMap(array))
    }

}
