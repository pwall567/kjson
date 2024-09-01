/*
 * @(#) JSONCoStringify.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2022, 2023, 2024 Peter Wall
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
import net.pwall.util.CoOutput
import net.pwall.util.output

/**
 * Reflection-based non-blocking JSON serialization for Kotlin - serialize direct to a non-blocking function.
 *
 * @author  Peter Wall
 */
object JSONCoStringify {

    /**
     * Stringify an object to JSON, using a non-blocking output function.  The output of the serialization process will
     * be supplied to the output function a character at a time.
     */
    suspend inline fun <reified T> coStringify(
        obj: T,
        config: JSONConfig = JSONConfig.defaultConfig,
        noinline out: CoOutput,
    ) {
        when (obj) {
            null -> out.output("null")
            else -> out.outputJSON(typeOf<T>(), obj, config)
        }
    }

    /**
     * Stringify an object to JSON, using a non-blocking output function.  The output of the serialization process will
     * be supplied to the output function a character at a time.
     */
    suspend fun coStringify(
        kType: KType,
        obj: Any?,
        config: JSONConfig = JSONConfig.defaultConfig,
        out: CoOutput,
    ) {
        when (obj) {
            null -> out.output("null")
            else -> out.outputJSON(kType, obj, config)
        }
    }

    /**
     * Stringify an object to JSON, as an extension function to a non-blocking output function.  The output of the
     * serialization process will be supplied to the output function a character at a time.
     */
    suspend inline fun <reified T> CoOutput.outputJSON(
        obj: T,
        config: JSONConfig = JSONConfig.defaultConfig,
    ) {
        outputJSON(typeOf<T>(), obj, config, mutableListOf())
    }

    /**
     * Stringify an object to JSON, as an extension function to a non-blocking output function.  The output of the
     * serialization process will be supplied to the output function a character at a time.
     */
    suspend fun CoOutput.outputJSON(
        kType: KType,
        obj: Any?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ) {
        outputJSON(kType, obj, config, mutableListOf())
    }

    suspend fun CoOutput.outputJSON(
        kType: KType,
        obj: Any?,
        config: JSONConfig,
        references: MutableList<Any>,
    ) {
        when (obj) {
            null -> output("null")
            is JSONValue -> obj.coOutputTo(this)
            in references -> throw JSONKotlinException("Circular reference to ${obj::class.simpleName}")
            else -> {
                references.add(obj)
                try {
                    val serializer = Serializer.findSerializer(kType, config)
                    serializer.output(this, obj, config, references)
                }
                finally {
                    references.removeLast()
                }
            }
        }
    }

}
