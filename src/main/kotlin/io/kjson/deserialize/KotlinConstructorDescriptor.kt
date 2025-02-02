/*
 * @(#) KotlinConstructorDescriptor.kt
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

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType

import java.lang.reflect.InvocationTargetException

import io.jstuff.util.ImmutableMap
import io.jstuff.util.ImmutableMapEntry

import io.kjson.JSONDeserializer.deserializeFields
import io.kjson.JSONObject
import io.kjson.JSONValue
import io.kjson.optional.Opt

data class KotlinConstructorDescriptor<T : Any>(
    val resultType: KType,
    val constructor: KFunction<T>,
    override val parameterDescriptors: List<KotlinParameterDescriptor<*>>,
    val fieldDescriptors: List<FieldDescriptor<*>>,
    val allowExtra: Boolean,
): ConstructorDescriptor<T> {

    override val targetName: String
        get() = resultType.toString()

    override fun matches(json: JSONValue): Int {
        if (json !is JSONObject)
            return -1
        if (parameterDescriptors.any { !json.containsKey(it.propertyName) && !(it.optional || it.nullable) })
            return -1
        if (!allowExtra) {
            for (property in json) {
                if (parameterDescriptors.none { it.propertyName == property.name } &&
                    fieldDescriptors.none { it.isMutable() && it.propertyName == property.name })
                    return -1
            }
        }
        return parameterDescriptors.count { json.containsKey(it.propertyName) } +
                fieldDescriptors.count { json.containsKey(it.propertyName) }
    }

    override fun instantiate(json: JSONValue): T {
        if (json !is JSONObject)
            throw typeException("object")
        val args = mutableListOf<ImmutableMapEntry<KParameter, Any?>>()
        val properties = json.toMutableList()
        val missing = mutableListOf<String>()
        for (parameterDescriptor in parameterDescriptors) {
            val propertyIndex = properties.indexOfFirst { it.name == parameterDescriptor.propertyName }
            if (!parameterDescriptor.ignore) {
                val kParameter = parameterDescriptor.kParameter
                val type = parameterDescriptor.type
                if (propertyIndex >= 0) {
                    val deserializer = parameterDescriptor.deserializer
                    val property = properties[propertyIndex].value
                    val value = try {
                        deserializer.deserialize(property)
                    } catch (de: DeserializationException) {
                        throw de.nested(parameterDescriptor.propertyName)
                    }
                    if (value == null && !parameterDescriptor.nullable)
                        throw DeserializationException("Property may not be null", parameterDescriptor.propertyName)
                    val parameterValue = if (parameterDescriptor.optClass) Opt.of(value) else value
                    args.add(ImmutableMap.entry(kParameter, parameterValue))
                }
                else {
                    if (!kParameter.isOptional) {
                        when {
                            parameterDescriptor.optClass -> args.add(ImmutableMap.entry(kParameter, Opt.UNSET))
                            type.isMarkedNullable -> args.add(ImmutableMap.entry(kParameter, null))
                            else -> missing.add(parameterDescriptor.propertyName)
                        }
                    }
                }
            }
            if (propertyIndex >= 0)
                properties.removeAt(propertyIndex)
        }
        if (missing.isNotEmpty())
            throw DeserializationException(
                text = "Can't create $targetName - missing " +
                        if (missing.size == 1) "property ${missing[0]}" else "properties ${missing.joinToString()}",
            )
        val result = try {
            if (args.size == parameterDescriptors.size)
                constructor.call(*(args.map { it.value }.toTypedArray()))
            else
                constructor.callBy(ImmutableMap.from(args))
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
        if (properties.isNotEmpty())
            deserializeFields(properties, targetName, fieldDescriptors, allowExtra, result)
        return result
    }

}
