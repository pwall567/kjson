/*
 * @(#) ObjectSerializer.kt
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

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

import java.lang.reflect.Method

import io.kjson.JSONConfig
import io.kjson.JSONException
import io.kjson.JSONKotlinException
import io.kjson.JSONObject
import io.kjson.JSONValue
import io.kjson.optional.Opt
import io.kjson.util.NameValuePair
import net.pwall.json.JSONCoFunctions.outputString
import net.pwall.json.JSONFunctions
import net.pwall.util.CoOutput
import net.pwall.util.output

class ObjectSerializer<T : Any>(
    private val kClass: KClass<T>,
    private val sealedClassDiscriminator: NameValuePair?,
    private val propertyDescriptors: List<PropertyDescriptor<Any>>,
): Serializer<Any> {

    override fun serialize(value: Any, config: JSONConfig, references: MutableList<Any>): JSONValue? {
        // first check that value class same as kClass, otherwise it's a derived class and we need to analyse its
        // properties again
        if (value::class != kClass) {
            @Suppress("unchecked_cast")
            val serializer = (config.findSerializer(value::class) ?: AnySerializer) as Serializer<Any>
            return serializer.serialize(value, config, references)
        }
        return JSONObject.build {
            if (sealedClassDiscriminator != null)
                add(sealedClassDiscriminator.name, sealedClassDiscriminator.value.toString())
            for (propertyDescriptor in propertyDescriptors) {
                val name = propertyDescriptor.name
                try {
                    val property = propertyDescriptor.getValue(value)
                    if (property != null || propertyDescriptor.includeIfNull) {
                        if (property !is Opt<*> || property.isSet)
                            add(name, propertyDescriptor.serializer.serializeNested(property, config, references, name))
                    }
                }
                catch (e: JSONException) {
                    throw e
                }
                catch (e: Exception) {
                    throw JSONKotlinException(
                        text = "Error getting property $name from ${value::class.simpleName}",
                        cause = e,
                    )
                }
            }
        }
    }

    @Suppress("unchecked_cast")
    override fun appendTo(a: Appendable, value: Any, config: JSONConfig, references: MutableList<Any>) {
        if (value::class != kClass) {
            val serializer = (config.findSerializer(value::class) ?: AnySerializer) as Serializer<Any>
            serializer.appendTo(a, value, config, references)
        }
        else {
            a.append('{')
            var continuation = false
            if (sealedClassDiscriminator != null) {
                JSONFunctions.appendString(a, sealedClassDiscriminator.name, config.stringifyNonASCII)
                a.append(':')
                JSONFunctions.appendString(a, sealedClassDiscriminator.value.toString(), config.stringifyNonASCII)
                continuation = true
            }
            for (propertyDescriptor in propertyDescriptors) {
                val name = propertyDescriptor.name
                try {
                    val property = propertyDescriptor.getValue(value)
                    if (property != null || propertyDescriptor.includeIfNull) {
                        if (property !is Opt<*> || property.isSet) {
                            if (continuation)
                                a.append(',')
                            JSONFunctions.appendString(a, name, config.stringifyNonASCII)
                            a.append(':')
                            propertyDescriptor.serializer.appendNestedTo(a, property, config, references, name)
                            continuation = true
                        }
                    }
                }
                catch (e: JSONException) {
                    throw e
                }
                catch (e: Exception) {
                    throw JSONKotlinException(
                        text = "Error getting property $name from ${value::class.simpleName}",
                        cause = e,
                    )
                }
            }
            a.append('}')
        }
    }

    override suspend fun output(out: CoOutput, value: Any, config: JSONConfig, references: MutableList<Any>) {
        if (value::class != kClass) {
            @Suppress("unchecked_cast")
            val serializer = (config.findSerializer(value::class) ?: AnySerializer) as Serializer<Any>
            serializer.output(out, value, config, references)
        }
        else {
            out.output('{')
            var continuation = false
            if (sealedClassDiscriminator != null) {
                out.outputString(sealedClassDiscriminator.name, config.stringifyNonASCII)
                out.output(':')
                out.outputString(sealedClassDiscriminator.value.toString(), config.stringifyNonASCII)
                continuation = true
            }
            for (propertyDescriptor in propertyDescriptors) {
                val name = propertyDescriptor.name
                try {
                    val property = propertyDescriptor.getValue(value)
                    if (property != null || propertyDescriptor.includeIfNull) {
                        if (property !is Opt<*> || property.isSet) {
                            if (continuation)
                                out.output(',')
                            out.outputString(name, config.stringifyNonASCII)
                            out.output(':')
                            propertyDescriptor.serializer.outputNestedTo(out, property, config, references, name)
                            continuation = true
                        }
                    }
                }
                catch (e: JSONException) {
                    throw e
                }
                catch (e: Exception) {
                    throw JSONKotlinException(
                        text = "Error getting property $name from ${value::class.simpleName}",
                        cause = e,
                    )
                }
            }
            out.output('}')
        }
    }

    sealed interface PropertyDescriptor<T : Any> {
        val name: String
        val kClass: KClass<T>
        val serializer: Serializer<T>
        val includeIfNull: Boolean
        fun getValue(obj: Any): T?
    }

    class KotlinPropertyDescriptor<T : Any>(
        override val name: String,
        override val kClass: KClass<T>,
        override val serializer: Serializer<T>,
        override val includeIfNull: Boolean,
        private val getter: KProperty.Getter<T?>,
    ) : PropertyDescriptor<T> {
        override fun getValue(obj: Any): T? = getter.call(obj)
    }

    class JavaPropertyDescriptor<T : Any>(
        override val name: String,
        override val kClass: KClass<T>,
        override val serializer: Serializer<T>,
        override val includeIfNull: Boolean,
        private val getter: Method,
    ) : PropertyDescriptor<T> {
        @Suppress("unchecked_cast")
        override fun getValue(obj: Any): T? = getter.invoke(obj) as T?
    }

}
