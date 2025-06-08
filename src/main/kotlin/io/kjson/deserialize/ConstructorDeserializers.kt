/*
 * @(#) ConstructorDeserializers.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2024, 2025 Peter Wall
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

import kotlin.reflect.KFunction

import io.jstuff.util.ImmutableMap
import io.jstuff.util.ImmutableMapEntry

import io.kjson.JSONDeserializerFunctions.callWithSingle
import io.kjson.JSONObject
import io.kjson.JSONValue

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
    private val members: Collection<FieldDescriptor<*>>,
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
                it.propertyName == name
            }
            val memberDeserializer = member?.deserializer ?: valueDeserializer
            val memberNullable = member?.nullable ?: valueNullable
            val value = memberDeserializer.deserializeValue(property.value, memberNullable, "Property", name)
            array[index] = ImmutableMap.entry(name, value)
        }
        return constructor.callWithSingle(ImmutableMap(array))
    }

}
