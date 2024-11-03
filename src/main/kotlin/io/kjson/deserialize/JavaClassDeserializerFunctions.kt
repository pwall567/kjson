/*
 * @(#) JavaClassDeserializerFunctions.kt
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

import java.beans.ConstructorProperties
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONConfig
import io.kjson.JSONDeserializer.applyTypeParameters
import io.kjson.JSONDeserializer.findDeserializer
import io.kjson.JSONDeserializer.getTypeParam
import io.kjson.JSONNumber
import io.kjson.JSONString
import io.kjson.toKType
import io.kjson.util.getJavaClassHierarchy
import io.kjson.util.isPublic
import io.kjson.util.isStaticOrTransient

object JavaClassDeserializerFunctions {

    private val booleanType: Class<Boolean> = java.lang.Boolean.TYPE

    @Suppress("unchecked_cast")
    fun <T : Any> createJavaClassDeserializer(
        resultClass: KClass<T>,
        resultType: KType,
        config: JSONConfig,
        references: MutableList<KType>,
    ): Deserializer<T>? {
        val resultJavaClass = resultClass.java
        val constructorDescriptors = mutableListOf<JavaConstructorDescriptor<T>>()
        for (constructor in resultJavaClass.constructors) {
            if (constructor.isPublic()) {
                val parameterTypes = constructor.genericParameterTypes
                if (parameterTypes.isEmpty()) {
                    constructorDescriptors.add(
                        JavaNoArgConstructorDescriptor(
                            resultClass = resultJavaClass,
                            constructor = constructor as Constructor<T>,
                            fieldDescriptors = getJavaFieldDescriptors(resultJavaClass, config, references),
                            allowExtra = config.allowExtra ||
                                    config.hasAllowExtraPropertiesAnnotation(resultClass.annotations),
                        )
                    )
                }
                else {
                    if (parameterTypes.size == 1) {
                        val parameterType = parameterTypes[0]
                        when (parameterType) {
                            java.lang.String::class.java -> constructorDescriptors.add(
                                JavaSingleArgConstructorDescriptor(
                                    resultClass = resultJavaClass,
                                    constructor = constructor as Constructor<T>,
                                    deserializer = StringDeserializer,
                                ) { it is JSONString }
                            )
                            Integer::class.java,
                            Integer.TYPE -> constructorDescriptors.add(
                                JavaSingleArgConstructorDescriptor(
                                    resultClass = resultJavaClass,
                                    constructor = constructor as Constructor<T>,
                                    deserializer = IntDeserializer,
                                ) { it is JSONNumber && it.isInt() }
                            )
                            Character::class.java,
                            Character.TYPE -> constructorDescriptors.add(
                                JavaSingleArgConstructorDescriptor(
                                    resultClass = resultJavaClass,
                                    constructor = constructor as Constructor<T>,
                                    deserializer = CharDeserializer,
                                ) { it is JSONString && it.value.length == 1 }
                            )
                            java.lang.Long::class.java,
                            java.lang.Long.TYPE -> constructorDescriptors.add(
                                JavaSingleArgConstructorDescriptor(
                                    resultClass = resultJavaClass,
                                    constructor = constructor as Constructor<T>,
                                    deserializer = LongDeserializer,
                                ) { it is JSONNumber && it.isLong() }
                            )
                            java.lang.Short::class.java,
                            java.lang.Short.TYPE -> constructorDescriptors.add(
                                JavaSingleArgConstructorDescriptor(
                                    resultClass = resultJavaClass,
                                    constructor = constructor as Constructor<T>,
                                    deserializer = ShortDeserializer,
                                ) { it is JSONNumber && it.isShort() }
                            )
                            java.lang.Byte::class.java,
                            java.lang.Byte.TYPE -> constructorDescriptors.add(
                                JavaSingleArgConstructorDescriptor(
                                    resultClass = resultJavaClass,
                                    constructor = constructor as Constructor<T>,
                                    deserializer = ByteDeserializer,
                                ) { it is JSONNumber && it.isByte() }
                            )
                            java.lang.Double::class.java,
                            java.lang.Double.TYPE -> constructorDescriptors.add(
                                JavaSingleArgConstructorDescriptor(
                                    resultClass = resultJavaClass,
                                    constructor = constructor as Constructor<T>,
                                    deserializer = DoubleDeserializer,
                                ) { it is JSONNumber }
                            )
                            java.lang.Float::class.java,
                            java.lang.Float.TYPE -> constructorDescriptors.add(
                                JavaSingleArgConstructorDescriptor(
                                    resultClass = resultJavaClass,
                                    constructor = constructor as Constructor<T>,
                                    deserializer = FloatDeserializer,
                                ) { it is JSONNumber }
                            )
                            java.lang.Boolean::class.java,
                            java.lang.Boolean.TYPE -> constructorDescriptors.add(
                                JavaSingleArgConstructorDescriptor(
                                    resultClass = resultJavaClass,
                                    constructor = constructor as Constructor<T>,
                                    deserializer = BooleanDeserializer,
                                ) { it is JSONBoolean }
                            )
                            BigDecimal::class.java -> constructorDescriptors.add(
                                JavaSingleArgConstructorDescriptor(
                                    resultClass = resultJavaClass,
                                    constructor = constructor as Constructor<T>,
                                    deserializer = BigDecimalDeserializer,
                                ) { it is JSONNumber }
                            )
                            BigInteger::class.java -> constructorDescriptors.add(
                                JavaSingleArgConstructorDescriptor(
                                    resultClass = resultJavaClass,
                                    constructor = constructor as Constructor<T>,
                                    deserializer = BigIntegerDeserializer,
                                ) { it is JSONNumber && it.isIntegral() }
                            )
                        }
                        if (parameterType is ParameterizedType) {
                            when (parameterType.rawType) {
                                java.util.List::class.java, java.util.Collection::class.java -> {
                                    createCollectionDeserializer(
                                        resultType = resultType,
                                        resultJavaClass = resultJavaClass,
                                        constructor = constructor as Constructor<T>,
                                        parameterType = parameterType,
                                        config = config,
                                        references = references
                                    ) { size -> ArrayList(size) }?.let { constructorDescriptors.add(it) }
                                }
                                java.util.Set::class.java -> {
                                    createCollectionDeserializer(
                                        resultType = resultType,
                                        resultJavaClass = resultJavaClass,
                                        constructor = constructor as Constructor<T>,
                                        parameterType = parameterType,
                                        config = config,
                                        references = references
                                    ) { size -> HashSet(size) }?.let { constructorDescriptors.add(it) }
                                }
                            }
                        }
                        if (parameterType is Class<*> && parameterType.isArray) {
                            val itemClass = parameterType.componentType
                            val itemDeserializer = findDeserializer<Any>(itemClass.toKType(), config, references)
                            if (itemDeserializer != null) {
                                val arrayDeserializer = ArrayDeserializer(
                                    itemClass = itemClass.kotlin as KClass<Any>,
                                    itemDeserializer = itemDeserializer,
                                    itemNullable = true,
                                )
                                constructorDescriptors.add(
                                    JavaSingleArgConstructorDescriptor(
                                        resultClass = resultJavaClass,
                                        constructor = constructor as Constructor<T>,
                                        deserializer = arrayDeserializer as Deserializer<*>,
                                    ) { it is JSONArray }
                                )
                            }
                        }
                        if (parameterType is GenericArrayType) {
                            val genericType = parameterType.genericComponentType
                            val index = resultJavaClass.typeParameters.indexOfFirst { it.name == genericType.typeName }
                            if (index >= 0) {
                                val itemType = getTypeParam(resultType, index).applyTypeParameters(resultType)
                                val itemDeserializer = findDeserializer<Any>(itemType, config, references)
                                if (itemDeserializer != null) {
                                    val arrayDeserializer = ArrayDeserializer(
                                        itemClass = itemType.classifier as KClass<Any>,
                                        itemDeserializer = itemDeserializer,
                                        itemNullable = itemType.isMarkedNullable,
                                    )
                                    constructorDescriptors.add(
                                        JavaSingleArgConstructorDescriptor(
                                            resultClass = resultJavaClass,
                                            constructor = constructor as Constructor<T>,
                                            deserializer = arrayDeserializer as Deserializer<*>,
                                        ) { it is JSONArray }
                                    )
                                }
                            }
                        }
                    }
                    constructor.getAnnotation(ConstructorProperties::class.java)?.let { annotation ->
                        val parameterNames = annotation.value
                        val parameters = parameterTypes.indices.map { index ->
                            val ignore = config.hasIgnoreAnnotation(
                                annotations = constructor.parameterAnnotations[index].asList(),
                            )
                            val parameterType = parameterTypes[index]
                            val deserializer: Deserializer<*> = if (parameterType is Class<*> &&
                                parameterType.isPrimitive)
                                findDeserializerForJavaPrimitive(parameterType)
                            else
                                findDeserializer<Any>(
                                    resultType = parameterType.toKType(),
                                    config = config,
                                    references = references,
                                ) as Deserializer
                            val name = parameterNames[index]
                            val notNull = parameterType is Class<*> && parameterType.isPrimitive
                            JavaParameterDescriptor(
                                propertyName = name,
                                deserializer = deserializer,
                                nullable = !notNull,
                                ignore = ignore,
                                defaultValue = if (ignore) getDefaultValue(parameterType) else null,
                            )
                        }
                        constructorDescriptors.add(
                            JavaNamedArgConstructorDescriptor(
                                resultClass = resultJavaClass,
                                constructor = constructor as Constructor<T>,
                                parameters = parameters,
                                fieldDescriptors = getJavaFieldDescriptors(resultJavaClass, config, references).filter {
                                    parameters.none { param -> param.propertyName == it.propertyName }
                                },
                                allowExtra = config.allowExtra ||
                                        config.hasAllowExtraPropertiesAnnotation(resultClass.annotations),
                            )
                        )
                    }
                }
            }
        }
        return when (constructorDescriptors.size) {
            0 -> null
            1 -> ClassSingleConstructorDeserializer(constructorDescriptors[0])
            else -> ClassMultiConstructorDeserializer(constructorDescriptors)
        }
    }

    private fun getDefaultValue(type: java.lang.reflect.Type?): Any? = when (type) {
        Integer.TYPE -> 0
        Character.TYPE -> '\u0000'
        java.lang.Long.TYPE -> 0L
        java.lang.Short.TYPE -> 0
        java.lang.Byte.TYPE -> 0
        java.lang.Float.TYPE -> 0.0F
        java.lang.Double.TYPE -> 0.0
        java.lang.Boolean.TYPE -> false
        else -> null
    }

    private fun <T : Any, L : MutableCollection<T?>> createCollectionDeserializer(
        resultType: KType,
        resultJavaClass: Class<T>,
        constructor: Constructor<T>,
        parameterType: ParameterizedType,
        config: JSONConfig,
        references: MutableList<KType>,
        collectionCreationFunction: (Int) -> L
    ): JavaConstructorDescriptor<T>? {
        val argType = parameterType.actualTypeArguments[0]
        if (argType is TypeVariable<*>) {
            val index = resultJavaClass.typeParameters.indexOfFirst { it.name == argType.name }
            if (index >= 0) {
                val itemType = getTypeParam(resultType, index).applyTypeParameters(resultType)
                val listDeserializer = createCollectionDeserializer(itemType, config,
                    references, collectionCreationFunction)
                return JavaSingleArgConstructorDescriptor(
                    resultClass = resultJavaClass,
                    constructor = constructor,
                    deserializer = listDeserializer as Deserializer<*>,
                ) { it is JSONArray }
            }
        }
        return null
    }

    private fun <T : Any> getJavaFieldDescriptors(
        javaClass: Class<T>,
        config: JSONConfig,
        references: MutableList<KType>,
    ) : List<FieldDescriptor<*>> {
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
                        val deserializer = if (returnType.isPrimitive)
                            findDeserializerForJavaPrimitive(returnType)
                        else
                            findDeserializer<Any>(
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
                        val notNull = returnType.isPrimitive
                        fieldDescriptors.removeIf { it.propertyName == name }
                        fieldDescriptors.add(
                            JavaFieldDescriptor(
                                propertyName = config.findNameFromAnnotation(annotations) ?: name,
                                ignore = config.hasIgnoreAnnotation(annotations),
                                nullable = !notNull,
                                deserializer = deserializer as Deserializer<*>,
                                getter = method,
                                setter = setter,
                            )
                        )
                    }
                    else if (methodName.length > 2 && methodName.startsWith("is") && methodName[2] in 'A'..'Z' &&
                        method.returnType == booleanType
                    ) {
                        val name = methodName[2].lowercase(Locale.US) + methodName.substring(3)
                        val annotations = mutableListOf(*method.annotations)
                        val setter = findJavaSetter(methods, methodName.substring(2), booleanType)?.also {
                            annotations += it.annotations
                        }
                        findJavaField(fields, name, booleanType)?.let { annotations += it.annotations }
                        fieldDescriptors.removeIf { it.propertyName == name }
                        fieldDescriptors.add(
                            JavaFieldDescriptor(
                                propertyName = config.findNameFromAnnotation(annotations) ?: name,
                                ignore = config.hasIgnoreAnnotation(annotations),
                                nullable = false,
                                deserializer = BooleanDeserializer,
                                getter = method,
                                setter = setter
                            )
                        )
                    }
                }
            }
        }
        return fieldDescriptors
    }

    private fun findDeserializerForJavaPrimitive(primitiveType: Class<*>): Deserializer<*> = when (primitiveType) {
        Integer.TYPE -> IntDeserializer
        Character.TYPE -> CharDeserializer
        java.lang.Long.TYPE -> LongDeserializer
        java.lang.Short.TYPE -> ShortDeserializer
        java.lang.Byte.TYPE -> ByteDeserializer
        java.lang.Float.TYPE -> FloatDeserializer
        java.lang.Double.TYPE -> DoubleDeserializer
        java.lang.Boolean.TYPE -> BooleanDeserializer
        else -> VoidSerializer
    }

    private fun findJavaSetter(methods: Array<Method>, nameSuffix: String, type: Class<*>): Method? {
        val setterName = "set$nameSuffix"
        return methods.find { !it.isStaticOrTransient() && it.name == setterName &&
                it.parameters.size == 1 && it.parameters[0].type == type } // check return type void?
    }

    private fun findJavaField(fields: Array<Field>, name: String, type: Class<*>): Field? =
            fields.find { !it.isStaticOrTransient() && it.name == name && it.type == type }

}
