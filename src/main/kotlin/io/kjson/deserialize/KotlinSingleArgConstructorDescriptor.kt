/*
 * @(#) KotlinSingleArgConstructorDescriptor.kt
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
import kotlin.reflect.KFunction

import java.lang.reflect.InvocationTargetException

import io.kjson.JSONDeserializerFunctions.callWithSingle
import io.kjson.JSONValue

class KotlinSingleArgConstructorDescriptor<T : Any>(
    val resultClass: KClass<T>,
    val constructor: KFunction<T>,
    val deserializer: Deserializer<*>,
    val matchFunction: (JSONValue) -> Boolean
) : ConstructorDescriptor<T> {

    override val targetName: String
        get() = "class ${resultClass.qualifiedName}"

    override val parameterDescriptors: List<ParameterDescriptor<*>>
        get() = emptyList()

    override fun instantiate(json: JSONValue): T = try {
        constructor.callWithSingle(deserializer.deserialize(json))
    } catch (ite: InvocationTargetException) {
        val cause = ite.targetException
        throw DeserializationException(
            text = "Error deserializing $targetName - " + (cause?.message ?: "InvocationTargetException"),
            underlying = cause ?: ite,
        )
    } catch (e: Exception) {
        throw DeserializationException(
            text = "Error deserializing $targetName - ${e.message ?: e::class.simpleName}",
            underlying = e,
        )
    }

    override fun matches(json: JSONValue): Int = if (matchFunction(json)) 1 else -1

}
