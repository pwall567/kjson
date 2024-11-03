/*
 * @(#) JavaSingleArgConstructorDescriptor.kt
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

import java.lang.reflect.Constructor

import io.kjson.JSONValue

class JavaSingleArgConstructorDescriptor<T : Any>(
    resultClass: Class<T>,
    constructor: Constructor<T>,
    val deserializer: Deserializer<*>,
    val matchFunction: (JSONValue) -> Boolean
) : JavaConstructorDescriptor<T>(resultClass, constructor) {

    override val targetName: String
        get() = "Java class ${resultClass.canonicalName}"

    override val parameterDescriptors: List<ParameterDescriptor<*>>
        get() = emptyList()

    override fun instantiate(json: JSONValue): T = invokeConstructor(listOf(deserializer.deserialize(json)))

    override fun matches(json: JSONValue): Int = if (matchFunction(json)) 1 else -1

}
