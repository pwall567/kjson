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

import io.kjson.JSONValue

class ClassMultiConstructorDeserializer<T : Any>(
    private val constructorDescriptors: List<ConstructorDescriptor<T>>, // size must be > 1
) : Deserializer<T> {

    override fun deserialize(json: JSONValue?): T? {
        if (json == null)
            return null
        var bestMatch = constructorDescriptors[0]
        var bestMatchValue = bestMatch.matches(json)
        for (i in 1 until constructorDescriptors.size) {
            val contender = constructorDescriptors[i]
            val contenderMatchValue = contender.matches(json)
            if (contenderMatchValue > bestMatchValue) {
                bestMatch = contender
                bestMatchValue = contenderMatchValue
            }
        }
        if (bestMatchValue < 0)
            throw DeserializationException {
                "No matching constructor for ${constructorDescriptors[0].targetName} from ${json.errorDisplay()}"
            }
        if (bestMatchValue == 0) {
            try {
                return bestMatch.instantiate(json)
            }
            catch (_: DeserializationException) {
                throw DeserializationException {
                    "No matching constructor for ${constructorDescriptors[0].targetName} from ${json.errorDisplay()}"
                }
            }
        }
        return bestMatch.instantiate(json)
    }

}
