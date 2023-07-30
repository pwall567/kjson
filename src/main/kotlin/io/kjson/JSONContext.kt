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
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

import io.kjson.pointer.JSONPointer

/**
 * A class to hold the current serialization or deserialization context.
 *
 * @author  Peter Wall
 */
class JSONContext private constructor(
    val config: JSONConfig,
    private val pointerTokens: Array<String>,
) {

    constructor(config: JSONConfig) : this(config, emptyArray())

    constructor(config: JSONConfig, pointer: JSONPointer) : this(config, pointer.tokensAsArray())

    val pointer: JSONPointer
        get() = JSONPointer.from(pointerTokens)

    fun child(token: String): JSONContext {
        val n = pointerTokens.size
        val tokens = Array(n + 1) { if (it < n) pointerTokens[it] else token }
        return JSONContext(config, tokens)
    }

    fun child(index: Int): JSONContext = child(index.toString())

    fun serialize(obj: Any?): JSONValue? = JSONSerializer.serialize(obj, config) // TODO change JSONSerializer to take JSONContext

    /**
     * Deserialize a [JSONValue] to the inferred type using this context.
     */
    inline fun <reified T> deserialize(json: JSONValue?): T = JSONDeserializer.deserialize(typeOf<T>(), json, this) as T

    @Suppress("UNCHECKED_CAST")
    fun <T> deserialize(type: KType, json: JSONValue?): T = JSONDeserializer.deserialize(type, json, this) as T

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> deserialize(resultClass: KClass<T>, json: JSONValue?): T =
            JSONDeserializer.deserialize(resultClass.starProjectedType, json, this) as T

}
