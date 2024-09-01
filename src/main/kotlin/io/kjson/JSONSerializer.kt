/*
 * @(#) JSONSerializer.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023, 2024 Peter Wall
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
 * Reflection-based JSON serialization for Kotlin.
 *
 * @author  Peter Wall
 */
object JSONSerializer {

    /**
     * Serialize the given object to a [JSONValue].
     */
    inline fun <reified T> serialize(obj: T, config: JSONConfig = JSONConfig.defaultConfig): JSONValue? {
        return serialize(typeOf<T>(), obj, config)
    }

    /**
     * Serialize the given object to a [JSONValue].
     */
    fun serialize(kType: KType, obj: Any?, config: JSONConfig = JSONConfig.defaultConfig): JSONValue? {
        return serialize(kType, obj, config, mutableListOf())
    }

    internal fun serialize(kType: KType, obj: Any?, config: JSONConfig, references: MutableList<Any>): JSONValue? =
        when (obj) {
            null -> null
            is JSONValue -> obj
            in references -> throw JSONKotlinException("Circular reference to ${obj::class.simpleName}")
            else -> {
                references.add(obj)
                try {
                    val serializer = Serializer.findSerializer(kType, config)
                    serializer.serialize(obj, config, references)
                }
                finally {
                    references.removeLast()
                }
            }
        }

}
