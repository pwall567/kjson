/*
 * @(#) ClassMultiConstructorDeserializer.kt
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

import io.kjson.JSONDeserializer.displayList
import io.kjson.JSONObject
import io.kjson.JSONValue

class ClassMultiConstructorDeserializer<T : Any>(
    private val resultClass: KClass<T>,
    private val constructorDescriptors: List<ConstructorDescriptor<T>>,
) : Deserializer<T> {

    override fun deserialize(json: JSONValue?): T? = when (json) {
        null -> null
        is JSONObject -> {
            findBestConstructor(json)?.let {
                return it.instantiate(json)
            }
            val resultName = resultClass.qualifiedName
            if (constructorDescriptors.size == 1) {
                val missing = constructorDescriptors[0].parameterDescriptors.filter {
                    !it.ignore && !json.containsKey(it.propertyName)
                }.map {
                    it.propertyName
                }
                throw DeserializationException("Can't create $resultName; missing: ${missing.displayList()}")
            }
            val propMessage = when {
                json.isNotEmpty() -> json.keys.displayList()
                else -> "none"
            }
            throw DeserializationException("Can't locate public constructor for $resultName; properties: $propMessage")
        }
        else -> typeError("object")
    }

    private fun findBestConstructor(json: JSONObject): ConstructorDescriptor<T>? {
        var bestSoFar: ConstructorDescriptor<T>? = null
        var numMatching = -1
        for (constructorDescriptor in constructorDescriptors) {
            val n = findMatchingParameters(constructorDescriptor.parameterDescriptors, json)
            if (n > numMatching) {
                bestSoFar = constructorDescriptor
                numMatching = n
            }
        }
        return bestSoFar
    }

    private fun findMatchingParameters(parameterDescriptors: List<ParameterDescriptor<*>>, json: JSONObject): Int {
        var n = 0
        for (parameterDescriptor in parameterDescriptors) {
            when {
                json.containsKey(parameterDescriptor.propertyName) -> n++
                !parameterDescriptor.optional -> return -1
            }
        }
        return n
    }

}
