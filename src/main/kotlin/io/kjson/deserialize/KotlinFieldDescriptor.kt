/*
 * @(#) KotlinFieldDescriptor.kt
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

import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import io.kjson.JSONConfig
import io.kjson.JSONDeserializer.applyTypeParameters
import io.kjson.JSONDeserializer.findDeserializer

data class KotlinFieldDescriptor<T>(
    override val propertyName: String,
    override val ignore: Boolean,
    override val nullable: Boolean,
    override val deserializer: Deserializer<T>,
    private val kProperty: KProperty<T?>,
) : FieldDescriptor<T> {

    override val returnType: KType
        get() = kProperty.returnType

    override fun isMutable(): Boolean = kProperty is KMutableProperty

    override fun getValue(instance: Any): Any? = kProperty.getter.call(instance)

    override fun setValue(instance: Any, value: Any?) {
        (kProperty as KMutableProperty).setter.call(instance, value)
    }

    companion object {

        fun create(
            member: KProperty<*>,
            resultType: KType,
            config: JSONConfig,
            references: MutableList<KType>,
        ):  KotlinFieldDescriptor<*> {
            val propertyName = config.findNameFromAnnotation(member.annotations) ?: member.name
            val fieldType = member.getter.returnType.applyTypeParameters(resultType)
            val deserializer = findDeserializer<Any>(fieldType, config, references)
            return KotlinFieldDescriptor(
                propertyName = propertyName,
                ignore = config.hasIgnoreAnnotation(member.annotations),
                nullable = fieldType.isMarkedNullable,
                deserializer = deserializer,
                kProperty = member,
            )
        }

    }

}
