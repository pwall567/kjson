/*
 * @(#) CustomSerializers.kt
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

package io.kjson.serialize

import kotlin.reflect.KFunction
import kotlin.reflect.full.starProjectedType

import io.kjson.JSONConfig
import io.kjson.JSONException
import io.kjson.JSONKotlinException
import io.kjson.JSONValue
import io.kjson.ToJSONMapping
import io.kjson.serialize.Serializer.Companion.findSerializer
import net.pwall.util.CoOutput
import net.pwall.util.output

class ConfigToJSONMappingSerializer(
    private val toJSONMapping: ToJSONMapping,
    private val name: String,
) : Serializer<Any> {

    override fun serialize(value: Any, config: JSONConfig, references: MutableList<Any>): JSONValue? {
        try {
            val mapped = config.toJSONMapping(value)
            if (mapped == null)
                return null
            else {
                val serializer = findSerializer(mapped::class.starProjectedType, config)
                return serializer.serialize(mapped, config, references)
            }
        }
        catch (e: JSONException) {
            throw e
        }
        catch (e: Exception) {
            throw JSONKotlinException("Error in custom toJSON - $name", cause = e)
        }
    }

    override fun appendTo(a: Appendable, value: Any, config: JSONConfig, references: MutableList<Any>) {
        try {
            val mapped = config.toJSONMapping(value)
            if (mapped == null)
                a.append("null")
            else {
                val serializer = findSerializer(mapped::class.starProjectedType, config)
                serializer.appendTo(a, mapped, config, references)
            }
        }
        catch (e: JSONException) {
            throw e
        }
        catch (e: Exception) {
            throw JSONKotlinException("Error in custom toJSON - $name", cause = e)
        }
    }

    override suspend fun output(out: CoOutput, value: Any, config: JSONConfig, references: MutableList<Any>) {
        try {
            val mapped = config.toJSONMapping(value)
            if (mapped == null)
                out.output("null")
            else {
                val serializer = findSerializer(mapped::class.starProjectedType, config)
                serializer.output(out, mapped, config, references)
            }
        }
        catch (e: JSONException) {
            throw e
        }
        catch (e: Exception) {
            throw JSONKotlinException("Error in custom toJSON - $name", cause = e)
        }
    }

}

class InClassToJSONSerializer(
    private val function: KFunction<Any?>,
    private val name: String,
) : Serializer<Any> {

    override fun serialize(value: Any, config: JSONConfig, references: MutableList<Any>): JSONValue? {
        try {
            val mapped = function.call(value)
            if (mapped == null)
                return null
            else {
                val serializer = findSerializer(mapped::class.starProjectedType, config)
                return serializer.serialize(mapped, config, references)
            }
        }
        catch (e: JSONException) {
            throw e
        }
        catch (e: Exception) {
            throw JSONKotlinException("Error in custom toJSON - $name", cause = e)
        }
    }

    override fun appendTo(a: Appendable, value: Any, config: JSONConfig, references: MutableList<Any>) {
        try {
            val mapped = function.call(value)
            if (mapped == null)
                a.append("null")
            else {
                val serializer = findSerializer(mapped::class.starProjectedType, config)
                serializer.appendTo(a, mapped, config, references)
            }
        }
        catch (e: JSONException) {
            throw e
        }
        catch (e: Exception) {
            throw JSONKotlinException("Error in custom toJSON - $name", cause = e)
        }
    }

    override suspend fun output(out: CoOutput, value: Any, config: JSONConfig, references: MutableList<Any>) {
        try {
            val mapped = function.call(value)
            if (mapped == null)
                out.output("null")
            else {
                val serializer = findSerializer(mapped::class.starProjectedType, config)
                serializer.output(out, mapped, config, references)
            }
        }
        catch (e: JSONException) {
            throw e
        }
        catch (e: Exception) {
            throw JSONKotlinException("Error in custom toJSON - $name", cause = e)
        }
    }

}
