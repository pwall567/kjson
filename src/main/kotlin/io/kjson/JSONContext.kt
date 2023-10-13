/*
 * @(#) JSONContext.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2023 Peter Wall
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
import kotlin.reflect.KType
import kotlin.reflect.typeOf

import io.kjson.pointer.JSONPointer

/**
 * A class to hold the current serialization or deserialization context, containing the [JSONConfig] and a [JSONPointer]
 * representing the location currently being processed.
 *
 * @author  Peter Wall
 */
class JSONContext private constructor(
    val config: JSONConfig,
    private val pointerTokens: Array<String>,
) {

    constructor(config: JSONConfig) : this(config, emptyArray())

    constructor(config: JSONConfig, pointer: JSONPointer) : this(config, pointer.tokensAsArray())

    constructor(pointer: JSONPointer) : this(JSONConfig.defaultConfig, pointer.tokensAsArray())

    /** The [JSONPointer] represented by this `JSONContext`. */
    val pointer: JSONPointer
        get() = JSONPointer.from(pointerTokens)

    /**
     * Create a new `JSONContext` with the [JSONConfig] modified by the application of a lambda.
     */
    fun modifyConfig(block: JSONConfig.() -> Unit): JSONContext = JSONContext(config.copy(block), pointerTokens)

    /**
     * Create a new `JSONContext`, replacing the [JSONConfig] with the one supplied.
     */
    fun replaceConfig(config: JSONConfig): JSONContext = JSONContext(config, pointerTokens)

    /**
     * Create a `JSONContext` representing the nominated property child of an object.
     */
    fun child(token: String): JSONContext = JSONContext(config, pointerTokens + token)

    /**
     * Create a `JSONContext` representing the nominated array item child of an array.
     */
    fun child(index: Int): JSONContext = child(index.toString())

    /**
     * Serialize the given object to a [JSONValue], using this context (for use within a `toJSON` lambda).
     */
    fun serialize(obj: Any?): JSONValue? = if (obj == null) null else JSONSerializer.serialize(obj, this)

    /**
     * Serialize the given property to a [JSONValue], using a child context including the specified property name.
     */
    fun serializeProperty(name: String, obj: Any?): JSONValue? =
            if (obj == null) null else JSONSerializer.serialize(obj, child(name))

    /**
     * Serialize the given array item to a [JSONValue], using a child context including the specified array index.
     */
    fun serializeItem(index: Int, obj: Any?): JSONValue? =
            if (obj == null) null else JSONSerializer.serialize(obj, child(index))

    /**
     * Serialize a property using a child context which includes the specified property name and add it to a
     * [JSONObject.Builder] with that name.
     */
    fun JSONObject.Builder.addProperty(name: String, obj: Any?) {
        when {
            obj != null -> add(name, JSONSerializer.serialize(obj, child(name)))
            config.includeNulls -> add(name, null)
        }
    }

    /**
     * Serialize a property using a child context which includes the specified array index and add it to a
     * [JSONArray.Builder] (note that the index provided does not determine the index in the [JSONArray]).
     */
    fun JSONArray.Builder.addItem(index: Int, obj: Any?) {
        when {
            obj != null -> add(JSONSerializer.serialize(obj, child(index)))
            config.includeNulls -> add(null)
        }
    }

    /**
     * Deserialize a [JSONValue] to the inferred type using this context.
     */
    inline fun <reified T> deserialize(json: JSONValue?): T = JSONDeserializer.deserialize(typeOf<T>(), json, this) as T

    /**
     * Deserialize a [JSONValue] to the specified [KType] using this context.
     */
    fun deserialize(type: KType, json: JSONValue?): Any? = JSONDeserializer.deserialize(type, json, this)

    /**
     * Deserialize a [JSONValue] to the specified [KClass] using this context.
     */
    fun <T : Any> deserialize(resultClass: KClass<T>, json: JSONValue?): T? =
            JSONDeserializer.deserialize(resultClass, json, this)

    /**
     * Deserialize a child property of a [JSONObject] to the inferred type using a child context including this property
     * name.
     */
    inline fun <reified T> deserializeProperty(name: String, json: JSONObject): T =
            JSONDeserializer.deserialize(typeOf<T>(), json[name], child(name)) as T

    /**
     * Deserialize a child property of a [JSONObject] to the specified [KType] using a child context including this
     * property name.
     */
    fun deserializeProperty(type: KType, name: String, json: JSONObject): Any? =
            JSONDeserializer.deserialize(type, json[name], child(name))

    /**
     * Deserialize a child property of a [JSONObject] to the specified [KType] using a child context including this
     * property name.
     */
    fun <T : Any> deserializeProperty(resultClass: KClass<T>, name: String, json: JSONObject): T? =
            JSONDeserializer.deserialize(resultClass, json[name], child(name))

    /**
     * Deserialize a child item of a [JSONArray] to the inferred type using a child context including the index.
     */
    inline fun <reified T> deserializeItem(index: Int, json: JSONArray): T =
            JSONDeserializer.deserialize(typeOf<T>(), json[index], child(index)) as T

    /**
     * Deserialize a child item of a [JSONArray] to the specified [KType] using a child context including the index.
     */
    fun deserializeItem(type: KType, index: Int, json: JSONArray): Any? =
            JSONDeserializer.deserialize(type, json[index], child(index))

    /**
     * Deserialize a child item of a [JSONArray] to the specified [KType] using a child context including the index.
     */
    fun <T : Any> deserializeItem(resultClass: KClass<T>, index: Int, json: JSONArray): T? =
            JSONDeserializer.deserialize(resultClass, json[index], child(index))

    /**
     * Throw an exception with a message including the current pointer.
     */
    fun fatal(message: String): Nothing {
        JSONKotlinException.fatal(message, pointer)
    }

    /**
     * Throw an exception with a message including the current pointer, specifying a nested cause [Throwable].
     */
    fun fatal(message: String, nested: Throwable): Nothing {
        JSONKotlinException.fatal(message, pointer, nested)
    }

    /**
     * Throw an exception with a dynamically-generated message including the current pointer.
     */
    fun fatal(messageFunction: () -> String): Nothing {
        JSONKotlinException.fatal(messageFunction(), pointer)
    }

    /**
     * Throw an exception with a dynamically-generated message including the current pointer, specifying a nested cause
     * [Throwable].
     */
    fun fatal(nested: Throwable, messageFunction: () -> String): Nothing {
        JSONKotlinException.fatal(messageFunction(), pointer, nested)
    }

}
