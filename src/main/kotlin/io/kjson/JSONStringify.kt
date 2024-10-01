/*
 * @(#) JSONStringify.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023, 2924 Peter Wall
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

import kotlin.reflect.KType
import kotlin.reflect.typeOf

import io.kjson.serialize.Serializer

/**
 * Reflection-based JSON serialization for Kotlin - serialize direct to `String`.
 *
 * @author  Peter Wall
 */
object JSONStringify {

    /**
     * Serialize an object to JSON. (The word "stringify" is borrowed from the JavaScript implementation of JSON.)
     */
    inline fun <reified T> stringify(obj: T, config: JSONConfig = JSONConfig.defaultConfig): String = if (obj == null)
        "null"
    else
        buildString(config.stringifyInitialSize) { appendJSON(typeOf<T>(), obj, config) }

    /**
     * Serialize an object to JSON. (The word "stringify" is borrowed from the JavaScript implementation of JSON.)
     */
    fun stringify(kType: KType, obj: Any?, config: JSONConfig = JSONConfig.defaultConfig): String = if (obj == null)
        "null"
    else
        buildString(config.stringifyInitialSize) { appendJSON(kType, obj, config) }

    /**
     * Append the serialized form of an object to an [Appendable] in JSON, specifying the [KType].
     */
    inline fun <reified T> Appendable.appendJSON(obj: T, config: JSONConfig = JSONConfig.defaultConfig) {
        appendJSON(typeOf<T>(), obj, config)
    }

    /**
     * Append the serialized form of an object to an [Appendable] in JSON, specifying the [KType].
     */
    fun Appendable.appendJSON(kType: KType, obj: Any?, config: JSONConfig = JSONConfig.defaultConfig) {
        when (obj) {
            null -> append("null")
            is JSONValue -> obj.appendTo(this)
            else -> Serializer.findSerializer(kType, config).appendTo(this, obj, config, mutableListOf(obj))
        }
    }

}
