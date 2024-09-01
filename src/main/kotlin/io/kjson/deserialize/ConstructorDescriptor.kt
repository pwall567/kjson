/*
 * @(#) ConstructorDescriptor.kt
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
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KVisibility

import java.lang.reflect.InvocationTargetException

import io.kjson.JSONConfig
import io.kjson.JSONDeserializer.deserializeFields
import io.kjson.JSONDeserializerFunctions.callWithSingle
import io.kjson.JSONDeserializerFunctions.findParameterName
import io.kjson.JSONObject
import io.kjson.JSONValue
import io.kjson.deserialize.ParameterDescriptor.Companion.createParameterDescriptor
import io.kjson.optional.Opt
import net.pwall.util.ImmutableMap
import net.pwall.util.ImmutableMapEntry

data class ConstructorDescriptor<T : Any>(
    val resultType: KType,
    val kFunction: KFunction<T>,
    val parameterDescriptors: List<ParameterDescriptor<*>>,
    val fieldDescriptors: List<FieldDescriptor<*>>,
    val allowExtra: Boolean,
) {

    fun instantiate(json: JSONValue): T {
        if (json !is JSONObject) {
            if (hasSingleParameter()) {
                val deserializer = parameterDescriptors[0].deserializer
                try {
                    return kFunction.callWithSingle(deserializer.deserialize(json))
                }
                catch (_: Exception) {
                    // ignore exception throw by alternative, throw exception below
                }
            }
            throw typeException("object")
        }
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
                text = "Can't create $resultType - missing " + if (missing.size == 1) "property ${missing[0]}" else
                        "properties ${missing.joinToString()}",
            )
        val result = try {
            if (args.size == parameterDescriptors.size)
                kFunction.call(*(args.map { it.value }.toTypedArray()))
            else
                kFunction.callBy(ImmutableMap.from(args))
        } catch (ite: InvocationTargetException) {
            val cause = ite.cause
            throw DeserializationException(
                text = "Error deserializing $resultType - " + (cause?.message ?: "InvocationTargetException"),
                underlying = cause ?: ite,
            )
        } catch (e: Exception) {
            throw DeserializationException(
                text = "Error deserializing $resultType - ${e.message ?: e::class.simpleName}",
                underlying = e,
            )
        }
        if (properties.isNotEmpty())
            deserializeFields(properties, resultType, fieldDescriptors, allowExtra, result)
        return result
    }

    private fun hasSingleParameter(): Boolean {
        if (parameterDescriptors.size == 1)
            return true
        if (parameterDescriptors.size > 1)
            for (i in 1 until parameterDescriptors.size)
                if (!parameterDescriptors[i].optional)
                    return false
        return true
    }

    companion object {

        fun <TT : Any> createConstructorDescriptor(
            constructor: KFunction<TT>,
            resultType: KType,
            resultClass: KClass<*>,
            fields: List<FieldDescriptor<*>>,
            config: JSONConfig,
            references: MutableList<KType>,
        ) : ConstructorDescriptor<TT>? {
            if (constructor.visibility != KVisibility.PUBLIC)
                return null
            val parameters = constructor.parameters
            if (parameters.any { findParameterName(it, config) == null || it.kind != KParameter.Kind.VALUE })
                return null
            val parameterDescriptors = mutableListOf<ParameterDescriptor<*>>()
            for (parameter in parameters) {
                val parameterDescriptor = createParameterDescriptor<Any>(
                    parameter = parameter,
                    resultType = resultType,
                    config = config,
                    references = references,
                )
                if (parameterDescriptor == null)
                    return null
                parameterDescriptors.add(parameterDescriptor)
            }
            return ConstructorDescriptor(
                resultType = resultType,
                kFunction = constructor,
                parameterDescriptors = parameterDescriptors.ifEmpty { emptyList() },
                fieldDescriptors = fields.filter { f ->
                    parameterDescriptors.none { p -> f.propertyName == p.propertyName  }
                },
                allowExtra = config.allowExtra || config.hasAllowExtraPropertiesAnnotation(resultClass.annotations),
            )
        }

    }

}
