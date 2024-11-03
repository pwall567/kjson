/*
 * @(#) JavaNamedArgConstructorDescriptor.kt
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

import io.kjson.JSONDeserializer.deserializeFields
import io.kjson.JSONObject
import io.kjson.JSONValue

class JavaNamedArgConstructorDescriptor<T : Any>(
    resultClass: Class<T>,
    constructor: Constructor<T>,
    val parameters: List<JavaParameterDescriptor<*>>,
    val fieldDescriptors: List<FieldDescriptor<*>>,
    val allowExtra: Boolean,
) : JavaConstructorDescriptor<T>(resultClass, constructor) {

    override val targetName: String
        get() = "Java class ${resultClass.canonicalName}"

    override val parameterDescriptors: List<ParameterDescriptor<*>>
        get() = emptyList()

    override fun instantiate(json: JSONValue): T {
        if (json !is JSONObject)
            throw typeException("object")
        val properties = json.toMutableList()
        val parameterValues = mutableListOf<Any?>()
        for (parameter in parameters) {
            val index = properties.indexOfFirst { it.name == parameter.propertyName }
            val property = if (index < 0) null else properties[index].value.also { properties.removeAt(index) }
            val propertyValue = parameter.deserializer.deserialize(property)
            if (propertyValue == null && !parameter.nullable)
                throw DeserializationException("Property may not be null - ${parameter.propertyName}")
            parameterValues.add(if (parameter.ignore) parameter.defaultValue else
                    parameter.deserializer.deserialize(property))
        }
        val instance = invokeConstructor(parameterValues)
        deserializeFields(properties, targetName, fieldDescriptors, allowExtra, instance)
        return instance
    }

    override fun matches(json: JSONValue): Int {
        if (json !is JSONObject)
            return -1
        if (!allowExtra) {
            for (property in json) {
                if (parameters.none { it.propertyName == property.name } &&
                        fieldDescriptors.none { it.propertyName == property.name })
                    return -1
            }
        }
        return parameters.count { json.containsKey(it.propertyName) } +
                fieldDescriptors.count { json.containsKey(it.propertyName) }
    }

    override fun throwDeserializationException(e: Throwable?): Nothing {
        throw DeserializationException { json: JSONValue? ->
            buildString {
                append("Can't deserialize ")
                append(targetName)
                append(", ")
                if (json !is JSONObject) {
                    append("expected object but was ")
                    append(json.errorDisplay())
                }
                else {
                    append("expected ")
                    append(if (parameters.size == 1) "property" else "properties")
                    append(' ')
                    append(if (parameters.isEmpty()) "[EMPTY]" else parameters.joinToString { it.propertyName })
                    append(" but found ")
                    append(if (json.isEmpty()) "[EMPTY]" else json.joinToString { it.name })
                }
            }
        }
    }

}
