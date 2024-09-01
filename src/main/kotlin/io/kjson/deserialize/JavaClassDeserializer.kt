/*
 * @(#) JavaClassDeserializer.kt
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
import kotlin.reflect.KType

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Locale

import io.kjson.JSONConfig
import io.kjson.JSONDeserializer
import io.kjson.JSONKotlinException
import io.kjson.JSONObject
import io.kjson.JSONValue
import io.kjson.toKType
import io.kjson.util.getJavaClassHierarchy
import io.kjson.util.isPublic
import io.kjson.util.isStaticOrTransient

class JavaClassDeserializer<T : Any>(
    private val resultType: KType,
    private val constructor: Constructor<T>,
    private val fieldDescriptors: List<FieldDescriptor<*>>,
    private val allowExtra: Boolean,
) : Deserializer<T> {

    override fun deserialize(json: JSONValue?): T? {
        if (json == null)
            return null
        if (json !is JSONObject)
            typeError("object")
        val instance = constructor.newInstance()
        JSONDeserializer.deserializeFields(json, resultType, fieldDescriptors, allowExtra, instance)
        return instance
    }

    companion object {

        private val booleanType: Class<Boolean> = java.lang.Boolean.TYPE

        fun <T : Any> createJavaClassDeserializer(
            resultType: KType,
            resultClass: KClass<T>,
            config: JSONConfig,
            references: MutableList<KType>,
        ) : Deserializer<T> {
            // TODO observe org.jetbrains.annotations.Nullable and NotNull annotations?
            // TODO check whether constructor has @java.beans.ConstructorProperties
            val javaClass = resultClass.java
            val constructor = findJavaNoArgConstructor(javaClass) ?:
                throw JSONKotlinException("Java class $javaClass requires no-arg constructor") // TODO context
            val fieldDescriptors = mutableListOf<FieldDescriptor<*>>()
            val classHierarchy = getJavaClassHierarchy(javaClass)
            while (classHierarchy.isNotEmpty()) {
                val classHierarchyEntry = classHierarchy.removeFirst()
                val methods = classHierarchyEntry.declaredMethods
                val fields = classHierarchyEntry.declaredFields
                for (method in methods) {
                    if (!method.isStaticOrTransient() && method.isPublic() && method.parameters.isEmpty()) {
                        val methodName = method.name
                        if (methodName.length > 3 && methodName.startsWith("get") && methodName[3] in 'A'..'Z') {
                            val returnType = method.returnType
                            val deserializer = JSONDeserializer.findDeserializer<Any>(
                                returnType.toKType(nullable = true),
                                config,
                                references
                            )
                            val name = methodName[3].lowercase(Locale.US) + methodName.substring(4)
                            val annotations = mutableListOf(*method.annotations)
                            val setter = findJavaSetter(methods, methodName.substring(3), returnType)?.also {
                                annotations += it.annotations
                            }
                            findJavaField(fields, name, returnType)?.let { annotations += it.annotations }
                            fieldDescriptors.removeIf { it.propertyName == name }
                            fieldDescriptors.add(
                                JavaFieldDescriptor(
                                    propertyName = config.findNameFromAnnotation(annotations) ?: name, // TODO test this
                                    ignore = config.hasIgnoreAnnotation(annotations), // TODO test this
                                    nullable = true,
                                    deserializer = deserializer as Deserializer<Any>,
                                    getter = method,
                                    setter = setter,
                                )
                            )
                        }
                        else if (methodName.length > 2 && methodName.startsWith("is") && methodName[2] in 'A'..'Z' &&
                                method.returnType == booleanType) {
                            val name = methodName[2].lowercase(Locale.US) + methodName.substring(3)
                            val annotations = mutableListOf(*method.annotations)
                            val setter = findJavaSetter(methods, methodName.substring(2), booleanType)?.also {
                                annotations += it.annotations
                            }
                            findJavaField(fields, name, booleanType)?.let { annotations += it.annotations }
                            fieldDescriptors.removeIf { it.propertyName == name }
                            fieldDescriptors.add(
                                JavaFieldDescriptor(
                                    propertyName = config.findNameFromAnnotation(annotations) ?: name, // TODO test this
                                    ignore = config.hasIgnoreAnnotation(annotations), // TODO test this
                                    nullable = true,
                                    deserializer = BooleanDeserializer,
                                    getter = method,
                                    setter = setter
                                )
                            )
                        }
                    }
                }
            }
            return JavaClassDeserializer(
                resultType = resultType,
                constructor = constructor,
                fieldDescriptors = fieldDescriptors,
                allowExtra = config.allowExtra,
            )
        }

        @Suppress("unchecked_cast")
        private fun <T : Any> findJavaNoArgConstructor(javaClass: Class<T>): Constructor<T>? {
            for (constructor in javaClass.constructors)
                if (constructor.parameters.isEmpty())
                    return constructor as Constructor<T>
            return null
        }

        private fun findJavaSetter(methods: Array<Method>, nameSuffix: String, type: Class<*>): Method? {
            val setterName = "set$nameSuffix"
            return methods.find { !it.isStaticOrTransient() && it.name == setterName &&
                    it.parameters.size == 1 && it.parameters[0].type == type } // check return type void?
        }

        private fun findJavaField(fields: Array<Field>, name: String, type: Class<*>): Field? =
            fields.find { !it.isStaticOrTransient() && it.name == name && it.type == type }

    }

}
