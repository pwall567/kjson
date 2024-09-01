/*
 * @(#) JSONContext.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2023, 2024 Peter Wall
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
 * **DEPRECATED** - As of version 8.0, this class is no longer in use.  Functions that take a [JSONConfig] parameter
 * should be used instead.
 *
 * @author  Peter Wall
 */
@Deprecated(message = "This class is no longer in use")
class JSONContext private constructor(
    val config: JSONConfig,
    private var pointerTokenArray: Array<String?>,
    private var pointerTokenArraySize: Int,
    private var pointerTokenArrayUsed: Int,
) {

    constructor(config: JSONConfig) : this(config, arrayOfNulls(tokenArraySize), tokenArraySize, 0)

    constructor(config: JSONConfig, pointer: JSONPointer) : this(config, pointer.tokensAsArray().copyOf(tokenArraySize),
        tokenArraySize, pointer.tokensAsArray().size)

    constructor(pointer: JSONPointer) : this(JSONConfig.defaultConfig, pointer.tokensAsArray().copyOf(tokenArraySize),
        tokenArraySize, pointer.tokensAsArray().size)

    /** The [JSONPointer] represented by this `JSONContext`. */
    @Suppress("unchecked_cast")
    val pointer: JSONPointer
        get() = JSONPointer.from(pointerTokenArray.copyOf(pointerTokenArrayUsed) as Array<String>)

    /**
     * Create a new `JSONContext` with the [JSONConfig] modified by the application of a lambda.
     */
    fun modifyConfig(block: JSONConfig.() -> Unit): JSONContext = JSONContext(config.copy(block), pointerTokenArray,
        pointerTokenArraySize, pointerTokenArrayUsed)

    /**
     * Create a new `JSONContext`, replacing the [JSONConfig] with the one supplied.
     */
    fun replaceConfig(config: JSONConfig): JSONContext = JSONContext(config, pointerTokenArray, pointerTokenArraySize,
        pointerTokenArrayUsed)

    /**
     * Switch this `JSONContext` to point to the nominated property child of an object, perform a set of actions using
     * that updated context, and on completion, return the context to its previous state.
     */
    fun <T> useChild(token: String, block: (JSONContext) -> T): T {
        if (pointerTokenArrayUsed == pointerTokenArraySize) {
            pointerTokenArray = pointerTokenArray.copyOf(pointerTokenArraySize + tokenArraySize)
            pointerTokenArraySize += tokenArraySize
        }
        pointerTokenArray[pointerTokenArrayUsed++] = token
        val result = block(this)
        pointerTokenArrayUsed--
        return result
    }

    /**
     * Switch this `JSONContext` to point to the nominated array item child of an array, perform a set of actions using
     * that updated context, and on completion, return the context to its previous state.
     */
    fun <T> useChild(index: Int, block: (JSONContext) -> T): T = useChild(index.toString(), block)

    /**
     * Switch this `JSONContext` to point to the nominated property child of an object, perform a set of actions as a
     * `suspend` function using that updated context, and on completion, return the context to its previous state.
     */
    suspend fun <T> coUseChild(token: String, block: suspend (JSONContext) -> T): T {
        if (pointerTokenArrayUsed == pointerTokenArraySize) {
            pointerTokenArray = pointerTokenArray.copyOf(pointerTokenArraySize + tokenArraySize)
            pointerTokenArraySize += tokenArraySize
        }
        pointerTokenArray[pointerTokenArrayUsed++] = token
        val result = block(this)
        pointerTokenArrayUsed--
        return result
    }

    /**
     * Switch this `JSONContext` to point to the nominated array item child of an array, perform a set of actions as a
     * `suspend` function using that updated context, and on completion, return the context to its previous state.
     */
    suspend fun <T> coUseChild(index: Int, block: suspend (JSONContext) -> T): T = coUseChild(index.toString(), block)

    /**
     * Create a `JSONContext` representing the nominated property child of an object.
     */
//    fun child(token: String): JSONContext = JSONContext(config, pointerTokens + token)

    /**
     * Create a `JSONContext` representing the nominated array item child of an array.
     */
//    fun child(index: Int): JSONContext = child(index.toString())

    /**
     * Serialize the given object to a [JSONValue], using this context (for use within a `toJSON` lambda).
     */
    fun serialize(obj: Any?): JSONValue? = if (obj == null) null else JSONSerializer.serialize(obj, config)

    /**
     * Deserialize a [JSONValue] to the inferred type using this context.
     */
    inline fun <reified T> deserialize(json: JSONValue?): T =
        JSONDeserializer.deserialize(typeOf<T>(), json, config) as T

    /**
     * Deserialize a [JSONValue] to the specified [KType] using this context.
     */
    fun deserialize(type: KType, json: JSONValue?): Any? = JSONDeserializer.deserialize(type, json, config)

    /**
     * Deserialize a [JSONValue] to the specified [KClass] using this context.
     */
    fun <T : Any> deserialize(resultClass: KClass<T>, json: JSONValue?): T? =
        JSONDeserializer.deserialize(resultClass, json, config)

    /**
     * Deserialize a child property of a [JSONObject] to the inferred type.
     */
    inline fun <reified T> deserializeProperty(name: String, json: JSONObject): T {
        try {
            return JSONDeserializer.deserialize(typeOf<T>(), json[name], config) as T
        }
        catch (e: JSONKotlinException) {
            throw e.nested(name)
        }
    }

    /**
     * Deserialize a child property of a [JSONObject] to the specified [KType].
     */
    fun deserializeProperty(type: KType, name: String, json: JSONObject): Any? {
        return try {
            JSONDeserializer.deserialize(type, json[name], config)
        }
        catch (e: JSONKotlinException) {
            throw e.nested(name)
        }
    }

    /**
     * Deserialize a child property of a [JSONObject] to the specified [KClass].
     */
    fun <T : Any> deserializeProperty(resultClass: KClass<T>, name: String, json: JSONObject): T? {
        return try {
            JSONDeserializer.deserialize(resultClass, json[name], config)
        }
        catch (e: JSONKotlinException) {
            throw e.nested(name)
        }
    }

    /**
     * Deserialize a child item of a [JSONArray] to the inferred type using a child context including the index.
     */
    inline fun <reified T> deserializeItem(index: Int, json: JSONArray): T {
        try {
            return JSONDeserializer.deserialize(typeOf<T>(), json[index], config) as T
        }
        catch (e: JSONKotlinException) {
            throw e.nested(index)
        }
    }

    /**
     * Deserialize a child item of a [JSONArray] to the specified [KType].
     */
    fun deserializeItem(type: KType, index: Int, json: JSONArray): Any? {
        try {
            return JSONDeserializer.deserialize(type, json[index], config)
        }
        catch (e: JSONKotlinException) {
            throw e.nested(index)
        }
    }

    /**
     * Deserialize a child item of a [JSONArray] to the specified [KClass].
     */
    fun <T : Any> deserializeItem(resultClass: KClass<T>, index: Int, json: JSONArray): T? {
        try {
            return JSONDeserializer.deserialize(resultClass, json[index], config)
        }
        catch (e: JSONKotlinException) {
            throw e.nested(index)
        }
    }

    /**
     * Throw an exception with a message including the current pointer.
     */
    fun fatal(message: String): Nothing {
        JSONKotlinException.fatal(message, pointer)
    }

    /**
     * Throw an exception with a message including the current pointer, specifying a nested cause [Throwable].
     */
    fun fatal(message: String, cause: Throwable): Nothing {
        JSONKotlinException.fatal(message, pointer, cause)
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
    fun fatal(cause: Throwable, messageFunction: () -> String): Nothing {
        JSONKotlinException.fatal(messageFunction(), pointer, cause)
    }

    companion object {
        @Suppress("ConstPropertyName")
        const val tokenArraySize = 20
    }

}
