/*
 * @(#) KotlinParameterDescriptor.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2024, 2025 Peter Wall
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

import kotlin.reflect.KParameter
import kotlin.reflect.KType

import io.kjson.JSONConfig
import io.kjson.JSONDeserializer
import io.kjson.JSONDeserializer.applyTypeParameters
import io.kjson.JSONDeserializer.findDeserializer
import io.kjson.JSONDeserializerFunctions.findParameterName
import io.kjson.optional.Opt

data class KotlinParameterDescriptor<T>(
    override val propertyName: String,
    val kParameter: KParameter,
    val type: KType,
    val optClass: Boolean,
    override val optional: Boolean,
    override val ignore: Boolean,
    override val nullable: Boolean,
    override val deserializer: Deserializer<T>,
) : ParameterDescriptor<T> {

    companion object {

        fun <TT : Any> create(
            parameter: KParameter,
            resultType: KType,
            config: JSONConfig,
            references: MutableList<KType>,
        ) : KotlinParameterDescriptor<TT>? {
            if (parameter.kind != KParameter.Kind.VALUE)
                return null
            val propertyName = findParameterName(parameter, config) ?: return null
            val type = parameter.type
            val optClass = type.classifier == Opt::class
            val optional = parameter.isOptional || type.isMarkedNullable || optClass
            val ignore = config.hasIgnoreAnnotation(parameter.annotations)
            val typeExOpt = if (optClass) JSONDeserializer.getTypeParam(type.arguments) else type
            val targetType = typeExOpt.applyTypeParameters(resultType)
            val deserializer = findDeserializer<TT>(targetType, config, references)
            return KotlinParameterDescriptor(
                propertyName = propertyName,
                kParameter = parameter,
                type = targetType,
                optClass = optClass,
                optional = optional,
                ignore = ignore,
                nullable = targetType.isMarkedNullable,
                deserializer = deserializer,
            )
        }

    }

}
