/*
 * @(#) CollectionDeserializers.kt
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

import kotlin.reflect.KClass
import kotlin.reflect.KType

import java.util.Arrays
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

import io.kjson.JSON.asByte
import io.kjson.JSON.asDecimal
import io.kjson.JSON.asInt
import io.kjson.JSON.asLong
import io.kjson.JSON.asShort
import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONConfig
import io.kjson.JSONDeserializer
import io.kjson.JSONDeserializer.applyTypeParameters
import io.kjson.JSONKotlinException
import io.kjson.JSONNumber
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.JSONValue
import io.kjson.util.SizedSequence

class CollectionDeserializer<T : Any, L : MutableCollection<T?>>(
    private val itemDeserializer: Deserializer<T>,
    private val itemNullable: Boolean,
    private val constructor: (Int) -> L,
): Deserializer<L> {

    override fun deserialize(json: JSONValue?): L? = when (json) {
        null -> null
        is JSONArray -> constructor(json.size).apply {
            fillCollection(itemDeserializer, itemNullable, json)
        }
        else -> typeError("array")
    }

}

class SequenceDeserializer<T : Any>(
    private val itemDeserializer: Deserializer<T>,
    private val itemType: KType,
) : Deserializer<Sequence<T?>> {

    override fun deserialize(json: JSONValue?): Sequence<T?>? = when (json) {
        null -> null
        is JSONArray -> SizedSequence(json.size) {
            for (index in json.indices) {
                // NOTE - exceptions are handled differently in this case because the deserialization of Sequence items
                // occurs in a separate thread, after the initial deserialization call has completed
                val item = json[index]
                val value = try {
                    itemDeserializer.deserialize(item)
                } catch (de: DeserializationException) {
                    throw JSONKotlinException(de.messageFunction(itemType, item), index)
                } catch (jke: JSONKotlinException) {
                    throw jke.nested(index)
                }
                if (value == null && !itemType.isMarkedNullable)
                    throw JSONKotlinException("Sequence item may not be null", index)
                yield(value)
            }
        }
        else -> typeError("array")
    }

}

class StreamDeserializer<T>(
    private val itemDeserializer: Deserializer<T>,
    private val itemNullable: Boolean,
): Deserializer<Stream<T?>> {

    override fun deserialize(json: JSONValue?): Stream<T?>? = when (json) {
        null -> null
        is JSONArray -> ArrayList<T?>(json.size).apply {
            fillCollection(itemDeserializer, itemNullable, json)
        }.stream()
        else -> typeError("array")
    }

}

class MapDeserializer<K : Any, V : Any, M : MutableMap<K?, V?>>(
    private val keyDeserializer: Deserializer<K>,
    private val keyNullable: Boolean,
    private val valueDeserializer: Deserializer<V>,
    private val valueNullable: Boolean,
    private val constructor: (Int) -> M,
) : Deserializer<M> {

    override fun deserialize(json: JSONValue?): M? = when (json) {
        null -> null
        is JSONObject -> constructor(json.size).apply {
            fillMap(keyDeserializer, keyNullable, valueDeserializer, valueNullable, json)
        }
        else -> typeError("object")
    }

}

class ArrayDeserializer<T : Any>(
    private val itemClass: KClass<T>,
    private val itemDeserializer: Deserializer<out T>,
    private val itemNullable: Boolean,
) : Deserializer<Array<T?>> {

    override fun deserialize(json: JSONValue?): Array<T?>? = when {
        json == null -> null
        itemDeserializer == CharDeserializer && json is JSONString -> {
            val string = json.value
            @Suppress("unchecked_cast")
            Array(string.length) { i -> string[i] } as Array<T?>
        }
        json is JSONArray -> {
            val array = JSONDeserializer.newArray(itemClass, json.size)
            for (i in json.indices)
                array[i] = itemDeserializer.deserializeValue(json[i], itemNullable, "Array item", i)
            array
        }
        else -> typeError("array")
    }

}

data object IntStreamDeserializer : Deserializer<IntStream> {
    override fun deserialize(json: JSONValue?): IntStream? = when (json) {
        null -> null
        is JSONArray -> if (json.isEmpty()) IntStream.empty() else Arrays.stream(IntArrayDeserializer.deserialize(json))
        else -> typeError("array")
    }
}

data object LongStreamDeserializer : Deserializer<LongStream> {
    override fun deserialize(json: JSONValue?): LongStream? = when (json) {
        null -> null
        is JSONArray -> if (json.isEmpty()) LongStream.empty() else
                Arrays.stream(LongArrayDeserializer.deserialize(json))
        else -> typeError("array")
    }
}

data object DoubleStreamDeserializer : Deserializer<DoubleStream> {
    override fun deserialize(json: JSONValue?): DoubleStream? = when (json) {
        null -> null
        is JSONArray -> if (json.isEmpty()) DoubleStream.empty() else
                Arrays.stream(DoubleArrayDeserializer.deserialize(json))
        else -> typeError("array")
    }
}

data object BooleanArrayDeserializer : Deserializer<BooleanArray> {
    override fun deserialize(json: JSONValue?): BooleanArray? = when (json) {
        null -> null
        is JSONArray -> BooleanArray(json.size) { index ->
            val item = json[index]
            if (item is JSONBoolean)
                item.value
            else
                typeError("Boolean", index)
        }
        else -> typeError("array")
    }
}

data object ByteArrayDeserializer : Deserializer<ByteArray> {
    override fun deserialize(json: JSONValue?): ByteArray? = when (json) {
        null -> null
        is JSONArray -> ByteArray(json.size) { index ->
            val item = json[index]
            if (item is JSONNumber && item.isByte())
                item.asByte
            else
                typeError("Byte", index)
        }
        else -> typeError("array")
    }
}

data object DoubleArrayDeserializer : Deserializer<DoubleArray> {
    override fun deserialize(json: JSONValue?): DoubleArray? = when (json) {
        null -> null
        is JSONArray -> DoubleArray(json.size) { index ->
            val item = json[index]
            if (item is JSONNumber)
                item.asDecimal.toDouble()
            else
                typeError("Double", index)
        }
        else -> typeError("array")
    }
}

data object FloatArrayDeserializer : Deserializer<FloatArray> {
    override fun deserialize(json: JSONValue?): FloatArray? = when (json) {
        null -> null
        is JSONArray -> FloatArray(json.size) { index ->
            val item = json[index]
            if (item is JSONNumber)
                item.asDecimal.toFloat()
            else
                typeError("Float", index)
        }
        else -> typeError("array")
    }
}

data object IntArrayDeserializer : Deserializer<IntArray> {
    override fun deserialize(json: JSONValue?): IntArray? = when (json) {
        null -> null
        is JSONArray -> IntArray(json.size) { index ->
            val item = json[index]
            if (item is JSONNumber && item.isInt())
                item.asInt
            else
                typeError("Int", index)
        }
        else -> typeError("array")
    }
}

data object LongArrayDeserializer : Deserializer<LongArray> {
    override fun deserialize(json: JSONValue?): LongArray? = when (json) {
        null -> null
        is JSONArray -> LongArray(json.size) { index ->
            val item = json[index]
            if (item is JSONNumber && item.isLong())
                item.asLong
            else
                typeError("Long", index)
        }
        else -> typeError("array")
    }
}

data object ShortArrayDeserializer : Deserializer<ShortArray> {
    override fun deserialize(json: JSONValue?): ShortArray? = when (json) {
        null -> null
        is JSONArray -> ShortArray(json.size) { index ->
            val item = json[index]
            if (item is JSONNumber && item.isShort())
                item.asShort
            else
                typeError("Short", index)
        }
        else -> typeError("array")
    }
}

private fun <T> MutableCollection<T?>.fillCollection(
    itemDeserializer: Deserializer<T>,
    itemNullable: Boolean,
    json: JSONArray,
) {
    for (i in json.indices) {
        val value = itemDeserializer.deserializeValue(json[i], itemNullable, "Collection item", i)
        if (!add(value))
            throw DeserializationException("Duplicate not allowed", i)
    }
}

internal fun <K, V> MutableMap<K?, V?>.fillMap(
    keyDeserializer: Deserializer<K>,
    keyNullable: Boolean,
    valueDeserializer: Deserializer<V>,
    valueNullable: Boolean,
    json: JSONObject,
) {
    if (keyDeserializer == StringDeserializer) {
        // the loop is repeated to avoid performing the above test on each iteration
        for (i in json.indices) {
            val property = json[i]
            val value = valueDeserializer.deserializeValue(property.value, valueNullable, "Map value", property.name)
            @Suppress("unchecked_cast")
            this[property.name as K] = value
        }
    }
    else {
        for (i in json.indices) {
            val property = json[i]
            val name = property.name
            val key = keyDeserializer.deserializeValue(JSONString.of(name), keyNullable, "Map key", name)
            this[key] = valueDeserializer.deserializeValue(property.value, valueNullable, "Map value", name)
        }
    }
}

fun <T> Deserializer<T>.deserializeValue(json: JSONValue?, nullable: Boolean, name: String, key: Any?): T? = try {
    deserialize(json)
} catch (e : DeserializationException) {
    throw e.nested(key.toString())
} catch (e: JSONKotlinException) {
    throw e.nested(key.toString())
} ?: if (nullable) null else throw DeserializationException("$name may not be null", key.toString())

fun <T : Any, L : MutableCollection<T?>> createCollectionDeserializer(
    resultType: KType,
    config: JSONConfig,
    references: MutableList<KType>,
    constructor: (Int) -> L,
): Deserializer<L>? {
    val itemType = JSONDeserializer.getTypeParam(resultType.arguments).applyTypeParameters(resultType)
    val itemDeserializer = JSONDeserializer.findDeserializer<T>(itemType, config, references) ?: return null
    return CollectionDeserializer(
        itemDeserializer = itemDeserializer,
        itemNullable = itemType.isMarkedNullable,
        constructor = constructor,
    )
}

fun <T : Any> createSequenceDeserializer(
    resultType: KType,
    config: JSONConfig,
    references: MutableList<KType>,
): Deserializer<Sequence<T?>>? {
    val itemType = JSONDeserializer.getTypeParam(resultType.arguments).applyTypeParameters(resultType)
    val itemDeserializer = JSONDeserializer.findDeserializer<T>(itemType, config, references) ?: return null
    return SequenceDeserializer(
        itemDeserializer = itemDeserializer,
        itemType = itemType,
    )
}

fun <T : Any> createStreamDeserializer(
    resultType: KType,
    config: JSONConfig,
    references: MutableList<KType>,
): Deserializer<Stream<T?>>? {
    val itemType = JSONDeserializer.getTypeParam(resultType.arguments).applyTypeParameters(resultType)
    val itemDeserializer = JSONDeserializer.findDeserializer<T>(itemType, config, references) ?: return null
    return StreamDeserializer(
        itemDeserializer = itemDeserializer,
        itemNullable = itemType.isMarkedNullable,
    )
}

fun <K : Any, V : Any, M : MutableMap<K?, V?>> createMapDeserializer(
    resultType: KType,
    config: JSONConfig,
    references: MutableList<KType>,
    constructor: (Int) -> M,
): Deserializer<M>? {
    val typeArguments = resultType.arguments
    val keyType = JSONDeserializer.getTypeParam(typeArguments, 0).applyTypeParameters(resultType)
    val keyDeserializer = JSONDeserializer.findDeserializer<K>(keyType, config, references) ?: return null
    val valueType = JSONDeserializer.getTypeParam(typeArguments, 1).applyTypeParameters(resultType)
    val valueDeserializer = JSONDeserializer.findDeserializer<V>(valueType, config, references) ?: return null
    return MapDeserializer(
        keyDeserializer = keyDeserializer,
        keyNullable = keyType.isMarkedNullable,
        valueDeserializer = valueDeserializer,
        valueNullable = valueType.isMarkedNullable,
        constructor = constructor,
    )
}
