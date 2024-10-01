/*
 * @(#) JSONDeserializer.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023, 2024 Peter Wall
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

package io.kjson

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.KVisibility
import kotlin.reflect.typeOf
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.staticFunctions

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.util.HashMap
import java.util.LinkedList
import java.util.stream.Stream

import io.kjson.JSON.displayValue
import io.kjson.JSONDeserializerFunctions.displayName
import io.kjson.JSONDeserializerFunctions.findAllInClassFromJSON
import io.kjson.JSONDeserializerFunctions.findSingleParameterConstructor
import io.kjson.JSONDeserializerFunctions.hasArrayParameter
import io.kjson.JSONDeserializerFunctions.hasNumberParameter
import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.deserialize.AnyDeserializer
import io.kjson.deserialize.ArrayConstructorDeserializer
import io.kjson.deserialize.ArrayDeserializer
import io.kjson.deserialize.BigDecimalConstructorDeserializer
import io.kjson.deserialize.BigIntegerConstructorDeserializer
import io.kjson.deserialize.ByteConstructorDeserializer
import io.kjson.deserialize.ClassMultiConstructorDeserializer
import io.kjson.deserialize.ClassSingleConstructorDeserializer
import io.kjson.deserialize.ConfigFromJSONDeserializer
import io.kjson.deserialize.Deserializer
import io.kjson.deserialize.DoubleConstructorDeserializer
import io.kjson.deserialize.EnumDeserializer
import io.kjson.deserialize.FieldDescriptor
import io.kjson.deserialize.FloatConstructorDeserializer
import io.kjson.deserialize.InClassMultiFromJSONDeserializer
import io.kjson.deserialize.IntConstructorDeserializer
import io.kjson.deserialize.KotlinFieldDescriptor
import io.kjson.deserialize.ListConstructorDeserializer
import io.kjson.deserialize.ConstructorDescriptor.Companion.createConstructorDescriptor
import io.kjson.deserialize.DeferredDeserializer
import io.kjson.deserialize.DelegatingMapConstructorDeserializer
import io.kjson.deserialize.DeserializationException
import io.kjson.deserialize.IntDeserializer
import io.kjson.deserialize.JavaClassDeserializer.Companion.createJavaClassDeserializer
import io.kjson.deserialize.JavaClassDeserializer.Companion.getJavaFieldDescriptors
import io.kjson.deserialize.JavaNamedArgConstructorDescriptor
import io.kjson.deserialize.JavaNoArgConstructorDescriptor
import io.kjson.deserialize.JavaParameterDescriptor
import io.kjson.deserialize.JavaSingleArgConstructorDescriptor
import io.kjson.deserialize.LongConstructorDeserializer
import io.kjson.deserialize.LongDeserializer
import io.kjson.deserialize.MapConstructorDeserializer
import io.kjson.deserialize.ObjectDeserializer
import io.kjson.deserialize.OptDeserializer
import io.kjson.deserialize.PairDeserializer.Companion.createPairDeserializer
import io.kjson.deserialize.SealedClassDeserializer.Companion.createSealedClassDeserializer
import io.kjson.deserialize.ShortConstructorDeserializer
import io.kjson.deserialize.StringConstructorDeserializer
import io.kjson.deserialize.StringDeserializer
import io.kjson.deserialize.TripleDeserializer.Companion.createTripleDeserializer
import io.kjson.deserialize.UByteConstructorDeserializer
import io.kjson.deserialize.UIntConstructorDeserializer
import io.kjson.deserialize.ULongConstructorDeserializer
import io.kjson.deserialize.UShortConstructorDeserializer
import io.kjson.deserialize.createCollectionDeserializer
import io.kjson.deserialize.createMapDeserializer
import io.kjson.deserialize.createSequenceDeserializer
import io.kjson.deserialize.createStreamDeserializer
import io.kjson.optional.Opt
import io.kjson.pointer.JSONPointer
import io.kjson.util.isKotlinClass
import io.kjson.util.isPublic
import java.beans.ConstructorProperties
import java.lang.reflect.Constructor

/**
 * Reflection-based JSON deserialization for Kotlin.
 *
 * @author  Peter Wall
 */
object JSONDeserializer {

    internal val anyQType = Any::class.createType(emptyList(), true)

    /**
     * Deserialize a parsed [JSONValue] to the inferred [KType].
     */
    inline fun <reified T> deserialize(
        json: JSONValue?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ): T = deserialize(typeOf<T>(), json, config) as T

    /**
     * Deserialize a parsed [JSONValue] to a specified [KType].
     */
    fun deserialize(
        resultType: KType,
        json: JSONValue?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ): Any? {
        val resultClass = resultType.classifier as? KClass<*> ?: fatal("Can't deserialize $resultType")
        if (json == null) {
            return when {
                resultClass == Opt::class -> Opt.UNSET
                resultType.isMarkedNullable -> null
                else -> fatal("Can't deserialize null as ${resultType.displayName()}")
            }
        }
        return deserialize(resultType, resultClass, json, config)
    }

    /**
     * Deserialize a parsed [JSONValue] to a specified [KType] (specifying [JSONContext]).
     */
    @Deprecated(
        message = "JSONContext is deprecated; use functions taking JSONConfig",
        replaceWith = ReplaceWith("deserialize(resultType, json, context.config"),
    )
    fun deserialize(
        resultType: KType,
        json: JSONValue?,
        @Suppress("deprecation")
        context: JSONContext,
    ): Any? = deserialize(resultType, json, context.config)

    /**
     * Deserialize a parsed [JSONValue] to a specified [KClass].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> deserialize(
        resultClass: KClass<T>,
        json: JSONValue?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ): T? {
        if (json == null)
            return if (resultClass == Opt::class) Opt.UNSET as T else null
        return deserialize(resultClass.starProjectedType, resultClass, json, config)
    }

    /**
     * Deserialize a parsed [JSONValue] to a specified [KClass], using a [JSONContext].
     */
    @Deprecated(
        message = "JSONContext is deprecated; use functions taking JSONConfig",
        replaceWith = ReplaceWith("deserialize(resultClass, json, context.config"),
    )
    fun <T : Any> deserialize(
        resultClass: KClass<T>,
        json: JSONValue?,
        @Suppress("deprecation")
        context: JSONContext,
    ): T? = deserialize(resultClass, json, context.config)

    /**
     * Deserialize a parsed [JSONValue] to a specified Java [Class].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> deserialize(
        javaClass: Class<T>,
        json: JSONValue?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ): T? = deserialize(javaClass.toKType(nullable = true), json, config)  as T?

    /**
     * Deserialize a parsed [JSONValue] to a specified Java [Type].
     */
    fun deserialize(
        javaType: Type,
        json: JSONValue?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ): Any? = deserialize(javaType.toKType(nullable = true), json, config)

    /**
     * Deserialize a parsed [JSONValue] to an unspecified([Any]) type.  Strings will be converted to `String`, numbers
     * to `Int`, `Long` or `BigDecimal`, booleans to `Boolean`, arrays to `ArrayList<Any?>` and objects to
     * `LinkedHashMap<String, Any?>` (an implementation of `Map` that preserves order).
     */
    fun deserializeAny(json: JSONValue?): Any? = AnyDeserializer.deserialize(json)

    /**
     * Deserialize a parsed [JSONValue] to a result [KType] and [KClass].
     */
    internal fun <T : Any> deserialize(
        resultType: KType,
        resultClass: KClass<T>,
        json: JSONValue,
        config: JSONConfig,
    ): T? {

        val deserializer = config.findDeserializer(resultClass) ?:
                determineDeserializer(resultType, config, references = mutableListOf(), json = json)

        if (deserializer != null) {
            try {
                val result = deserializer.deserialize(json)
                if (result == null && !resultType.isMarkedNullable)
                    fatal("Can't deserialize null as ${resultClass.qualifiedName}")
                return result
            }
            catch (de: DeserializationException) {
                fatal(de.messageFunction(resultType, json), de.pointer, de.underlying)
            }
            catch (je: JSONException) {
                throw je
            }
            catch (ite: InvocationTargetException) {
                fatal(
                    text = "Error deserializing $resultType - " + (ite.cause?.message ?: "InvocationTargetException"),
                    cause = ite.cause ?: ite,
                )
            }
            catch (e: Exception) {
                fatal("Error deserializing $resultType - ${e.message ?: e::class.simpleName}", cause = e)
            }
        }
        fatal("Can't deserialize ${json.displayValue()} as ${resultClass.qualifiedName}")
    }

    @Suppress("unchecked_cast")
    internal fun <T : Any> findDeserializer(
        resultType: KType,
        config: JSONConfig,
        references: MutableList<KType>,
        json: JSONValue? = null,
    ): Deserializer<T>? {
        config.findDeserializer(resultType)?.let { return it as Deserializer<T> }
        return determineDeserializer(resultType, config, references, json)
    }

    internal fun <T : Any> determineDeserializer(
        resultType: KType,
        config: JSONConfig,
        references: MutableList<KType>,
        json: JSONValue? = null,
    ): Deserializer<T>? {

        if (resultType in references)
            return DeferredDeserializer(resultType, config)
        references.add(resultType)

        try {

            // common cacheable types - custom deserialization, enum, List, Map etc.

            determineDeserializerForCacheableTypes<T>(resultType, config, references)?.let {
                return it.andStore(resultType, config)
            }

            // deserializers dependent on input JSON type? (not cacheable)

            if (json != null) {
                determineDeserializerForSpecificJSON<T>(resultType, config, references, json)?.let { return it }
            }

            // user classes

            return determineDeserializerForUserTypes<T>(resultType, config, references)?.andStore(resultType, config)

        }
        finally {
            references.removeLast()
        }
    }

    @Suppress("unchecked_cast")
    private fun <T : Any> determineDeserializerForCacheableTypes(
        resultType: KType,
        config: JSONConfig,
        references: MutableList<KType>,
    ): Deserializer<T>? {

        val resultClass = resultType.classifier
        if (resultClass !is KClass<*>)
            return null
        resultClass as KClass<T>

        // find JSONConfig custom deserializer

        val fromJSONMapping = config.findFromJSONMapping(resultType)
        if (fromJSONMapping != null)
            return ConfigFromJSONDeserializer(resultType, config, fromJSONMapping)

        // find in-class custom deserializer

        try {
            resultClass.companionObject?.let { companionObject ->
                val invokers = findAllInClassFromJSON(resultClass, companionObject)
                if (invokers.isNotEmpty())
                    return InClassMultiFromJSONDeserializer(resultClass, config, invokers)
            }
        }
        catch (_: Exception) {} // some classes don't allow getting companion object (Kotlin bug?)

        // is it an enum?

        if (resultClass.isSubclassOf(Enum::class))
            resultClass.staticFunctions.find { it.name == "valueOf" }?.let { valueOfFunction ->
                return (EnumDeserializer(valueOfFunction) as Deserializer<T>)
            }

        // is it an Array?

        if (resultClass.java.isArray) {
            val itemType = getTypeParam(resultType).applyTypeParameters(resultType)
            val itemDeserializer = findDeserializer<Any>(itemType, config, references) ?:
                    throw DeserializationException("Can't deserialize array of $itemType")
            val itemClass = itemType.classifier as? KClass<Any> ?:
                    throw DeserializationException("Can't determine array item type")
            return ArrayDeserializer(itemClass, itemDeserializer, itemType.isMarkedNullable) as Deserializer<T>
        }

        // is it a List?

        if (resultClass == List::class || resultClass == Collection::class || resultClass == ArrayList::class ||
            resultClass == MutableList::class || resultClass == MutableCollection::class ||
            resultClass == Iterable::class)
            return createCollectionDeserializer(resultType, config, references) { size ->
                ArrayList(size)
            } as Deserializer<T>?

        if (resultClass == LinkedList::class)
            return createCollectionDeserializer(resultType, config, references) { LinkedList() } as Deserializer<T>?

        // is it a Sequence?

        if (resultClass == Sequence::class)
            return createSequenceDeserializer<Any>(resultType, config, references) as Deserializer<T>?

        // is it a Set?

        if (resultClass == Set::class || resultClass == MutableSet::class || resultClass == LinkedHashSet::class)
            return createCollectionDeserializer(resultType, config, references) { size ->
                LinkedHashSet(size)
            } as Deserializer<T>?

        if (resultClass == HashSet::class)
            return createCollectionDeserializer(resultType, config, references) { size ->
                HashSet(size)
            } as Deserializer<T>?

        // is it a LinkedHashMap?

        if (resultClass == Map::class || resultClass == LinkedHashMap::class || resultClass == MutableMap::class)
            return createMapDeserializer(resultType, config, references) { size ->
                LinkedHashMap(size)
            } as Deserializer<T>?

        // is it a HashMap?

        if (resultClass == HashMap::class)
            return createMapDeserializer(resultType, config, references) { size ->
                HashMap(size)
            } as Deserializer<T>?

        // is it a Pair?

        if (resultClass == Pair::class)
            return createPairDeserializer<Any, Any>(resultType, config, references) as Deserializer<T>?

        // is it a Triple?

        if (resultClass == Triple::class)
            return createTripleDeserializer<Any, Any, Any>(resultType, config, references) as Deserializer<T>?

        // is it an Opt?

        if (resultClass == Opt::class) {
            val optType = getTypeParam(resultType.arguments).applyTypeParameters(resultType)
            val optDeserializer = findDeserializer<Any>(optType, config, references) ?: return null
            return OptDeserializer(optDeserializer) as Deserializer<T>
        }

        // is it a sealed class

        if (resultClass.isSealed)
            return createSealedClassDeserializer(resultClass, config, references)

        // is it a java.util.stream.Stream?

        if (resultClass == Stream::class)
            return createStreamDeserializer<Any>(resultType, config, references) as Deserializer<T>

        return null
    }

    @Suppress("unchecked_cast")
    private fun <T : Any> determineDeserializerForSpecificJSON(
        resultType: KType,
        config: JSONConfig,
        references: MutableList<KType>,
        json: JSONValue,
    ): Deserializer<T>? {

        val resultClass = resultType.classifier
        if (resultClass !is KClass<*>)
            return null
        resultClass as KClass<T>

        // is the JSON a string, and does the class have a single parameter constructor taking String?

        if (json is JSONString) {
            resultClass.findSingleParameterConstructor(String::class)?.let { constructor ->
                return StringConstructorDeserializer(constructor)
            }
        }

        // is the JSON a number, with a single parameter constructor taking a Number?

        if (json is JSONNumber) {
            resultClass.constructors.singleOrNull { it.hasNumberParameter() }?.let {
                when (it.parameters[0].type.classifier) {
                    Int::class -> if (json.isInt()) return IntConstructorDeserializer(it)
                    Long::class -> if (json.isLong()) return LongConstructorDeserializer(it)
                    Short::class -> if (json.isShort()) return ShortConstructorDeserializer(it)
                    Byte::class -> if (json.isByte()) return ByteConstructorDeserializer(it)
                    UInt::class -> if (json.isUInt()) return UIntConstructorDeserializer(it)
                    ULong::class -> if (json.isULong()) return ULongConstructorDeserializer(it)
                    UShort::class -> if (json.isUShort()) return UShortConstructorDeserializer(it)
                    UByte::class -> if (json.isUByte()) return UByteConstructorDeserializer(it)
                    Double::class -> return DoubleConstructorDeserializer(it)
                    Float::class -> return FloatConstructorDeserializer(it)
                    BigInteger::class -> if (json.isIntegral()) return BigIntegerConstructorDeserializer(it)
                    BigDecimal::class -> return BigDecimalConstructorDeserializer(it)
                }
            }
        }

        // likewise for JSON array

        if (json is JSONArray) {
            resultClass.constructors.singleOrNull { it.hasArrayParameter() }?.let { constructor ->
                val arrayType = constructor.parameters[0].type
                val itemType = getTypeParam(arrayType).applyTypeParameters(resultType)
                val itemClass = itemType.classifier as? KClass<Any> ?:
                        throw DeserializationException("Can't determine array item type")
                val itemDeserializer = findDeserializer<Any>(itemType, config, references)
                if (itemDeserializer != null)
                    return ArrayConstructorDeserializer(
                        constructor = constructor,
                        itemClass = itemClass,
                        itemDeserializer = itemDeserializer,
                        itemNullable = itemType.isMarkedNullable,
                    )
            }
            resultClass.findSingleParameterConstructor(List::class)?.let { constructor ->
                val listType = constructor.parameters[0].type
                val itemType = getTypeParam(listType).applyTypeParameters(resultType)
                val itemDeserializer = findDeserializer<Any>(itemType, config, references)
                if (itemDeserializer != null)
                    return ListConstructorDeserializer(
                        constructor = constructor,
                        itemDeserializer = itemDeserializer,
                        itemNullable = itemType.isMarkedNullable,
                        listNullable = listType.isMarkedNullable,
                    )
            }
        }

        return null
    }

    @Suppress("unchecked_cast")
    private fun <T : Any> determineDeserializerForUserTypes(
        resultType: KType,
        config: JSONConfig,
        references: MutableList<KType>,
    ): Deserializer<T>? {

        val resultClass = resultType.classifier
        if (resultClass !is KClass<*>)
            return null
        resultClass as KClass<T>

        // is it a delegating Map class?

        if (resultClass.isSubclassOf(Map::class)) {
            resultClass.findSingleParameterConstructor(Map::class)?.let { constructor ->
                val mapType = constructor.parameters[0].type.applyTypeParameters(resultType)
                val mapTypeArguments = mapType.arguments
                val keyType = getTypeParam(mapTypeArguments, 0).applyTypeParameters(resultType)
                val keyDeserializer = findDeserializer<Any>(keyType, config, references)
                val valueType = getTypeParam(mapTypeArguments, 1).applyTypeParameters(resultType)
                val valueDeserializer = findDeserializer<Any>(valueType, config, references)
                if (keyDeserializer != null && valueDeserializer != null) {
                    return if (keyType.classifier == String::class && !mapType.isMarkedNullable)
                        DelegatingMapConstructorDeserializer(
                            constructor = constructor,
                            valueDeserializer = valueDeserializer,
                            valueNullable = valueType.isMarkedNullable,
                            members = resultClass.members,
                            config = config,
                        )
                    else
                        MapConstructorDeserializer(
                            constructor = constructor,
                            keyDeserializer = keyDeserializer,
                            keyNullable = keyType.isMarkedNullable,
                            valueDeserializer = valueDeserializer,
                            valueNullable = valueType.isMarkedNullable,
                            mapNullable = mapType.isMarkedNullable,
                        )
                }
            }
        }

        // is it an object?

        resultClass.objectInstance?.let { instance ->
            return ObjectDeserializer(
                resultType = resultType,
                fieldDescriptors = resultClass.getFields(resultType, config, references),
                allowExtra = config.allowExtra || config.hasAllowExtraPropertiesAnnotation(resultClass.annotations),
                instance = instance,
            ) as Deserializer<T>
        }

        // is it a Java class?

        if (!resultClass.isKotlinClass()) {
            val resultJavaClass = resultClass.java
            val constructorDescriptors = resultJavaClass.constructors.filter {
                it.isPublic()
            }.mapNotNull {
                val parameterTypes = it.parameterTypes
                when (parameterTypes.size) {
                    0 -> JavaNoArgConstructorDescriptor(
                        resultClass = resultJavaClass,
                        constructor = it as Constructor<T>,
                        fieldDescriptors = getJavaFieldDescriptors(resultJavaClass, config, references),
                        allowExtra = config.allowExtra ||
                                config.hasAllowExtraPropertiesAnnotation(resultClass.annotations),
                    )
                    1 -> {
                        val deserializer = when (parameterTypes[0]) {
                            java.lang.String::class.java -> StringDeserializer
                            java.lang.Integer::class.java,
                            java.lang.Integer.TYPE -> IntDeserializer
                            java.lang.Long::class.java,
                            java.lang.Long.TYPE -> LongDeserializer
                            else -> fatal("Can't deserialize using single-arg constructor other than String or Int or Long")
                        }
                        JavaSingleArgConstructorDescriptor(
                            resultClass = resultJavaClass,
                            constructor = it as Constructor<T>,
                            deserializer = deserializer,
                        )
                    }
                    else -> {
                        it.getAnnotation(ConstructorProperties::class.java)?.value?.let { parameterNames ->
                            // TODO check that parameterNames.length == parameterTypes.length
                            JavaNamedArgConstructorDescriptor(
                                resultClass = resultJavaClass,
                                constructor = it as Constructor<T>,
                                parameters = parameterTypes.indices.map { index ->
                                    JavaParameterDescriptor(
                                        name = parameterNames[index],
                                        deserializer = findDeserializer<Any>(
                                            resultType = parameterTypes[index].toKType(),
                                            config = config,
                                            references = references,
                                        ) as Deserializer,
                                        ignore = config.hasIgnoreAnnotation(it.parameterAnnotations[0].asList()),
                                    )
                                },
                                fieldDescriptors = emptyList(),
                                allowExtra = config.allowExtra ||
                                        config.hasAllowExtraPropertiesAnnotation(resultClass.annotations),
                            )
                        }
                    }
                }
            }
            // TODO The possibilities are:
            //   1. A single no-arg constructor (as per current code)
            //   2. A single constructor taking a single parameter
            //   3. A single constructor with a @java.beans.ConstructorProperties annotation
            //   4. A combination of the above
            when (constructorDescriptors.size) {
                0 -> return null
                1 -> {

                }
                else -> {

                }
            }
            return createJavaClassDeserializer(resultType, resultClass, config, references)
        }

        // deserialize using constructor

        val publicConstructors = resultClass.constructors.filter {
            it.visibility == KVisibility.PUBLIC
        }.mapNotNull {
            createConstructorDescriptor(
                constructor = it,
                resultType = resultType,
                resultClass = resultClass,
                fields = resultClass.getFields(resultType, config, references),
                config = config,
                references = references,
            )
        }
        if (publicConstructors.isNotEmpty()) {
            return if (publicConstructors.size == 1) {
                ClassSingleConstructorDeserializer(
                    constructorDescriptor = publicConstructors[0],
                ).andStore(resultClass, config)
            } else {
                ClassMultiConstructorDeserializer(
                    resultClass = resultClass,
                    constructorDescriptors = publicConstructors,
                ).andStore(resultClass, config)
            }
        }

        return null
    }

    private fun <T : Any> Deserializer<T>.andStore(resultClass: KClass<T>, config: JSONConfig): Deserializer<T> {
        if (resultClass.typeParameters.isEmpty())
            config.addDeserializer(resultClass, this)
        return this
    }

    private fun <T : Any> Deserializer<T>.andStore(resultType: KType, config: JSONConfig): Deserializer<T> {
        config.addDeserializer(resultType, this)
        return this
    }

    private fun KClass<*>.getFields(
        resultType: KType,
        config: JSONConfig,
        references: MutableList<KType>,
    ): List<FieldDescriptor<*>> = members.mapNotNull { member ->
        if (member is KProperty<*> && member.visibility == KVisibility.PUBLIC) {
            val propertyName = config.findNameFromAnnotation(member.annotations) ?: member.name
            val fieldType = member.getter.returnType.applyTypeParameters(resultType)
            val deserializer = findDeserializer<Any>(fieldType, config, references) ?:
                    cantDeserialize(fieldType, JSONPointer.root.child(propertyName))
            KotlinFieldDescriptor(
                propertyName = propertyName,
                ignore = config.hasIgnoreAnnotation(member.annotations),
                nullable = fieldType.isMarkedNullable,
                deserializer = deserializer,
                kProperty = member,
            )
        }
        else
            null
    }

    internal fun deserializeFields(
        json: List<JSONObject.Property>,
        target: Any,
        fieldDescriptors: List<FieldDescriptor<*>>,
        allowExtra: Boolean,
        instance: Any,
    ) {
        for (property in json) {
            fieldDescriptors.find { it.propertyName == property.name }?.let { field ->
                if (!field.ignore) {
                    val value = try {
                        field.deserializer.deserialize(property.value)
                    } catch (de: DeserializationException) {
                        throw de.nested(property.name)
                    }
                    if (field.isMutable()) {
                        try {
                            field.setValue(instance, value)
                        }
                        catch (e: Exception) {
                            throw DeserializationException("Error setting property ${property.name} in $target",
                                underlying = e)
                        }
                    }
                    else {
                        if (field.getValue(instance) != value)
                            throw DeserializationException("Can't set property ${property.name} in $target")
                    }
                }
            } ?: run {
                if (!allowExtra)
                    throw DeserializationException("Can't find property ${property.name} in $target")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> newArray(
        itemClass: KClass<T>,
        size: Int,
    ): Array<T?> = java.lang.reflect.Array.newInstance(itemClass.java, size) as Array<T?>
    // there appears to be no way of creating an array of dynamic type in Kotlin other than to use Java reflection

    internal fun getTypeParam(type: KType, n: Int = 0): KType = type.arguments.getOrNull(n)?.type ?: anyQType

    internal fun getTypeParam(types: List<KTypeProjection>, n: Int = 0): KType = types.getOrNull(n)?.type ?: anyQType

    internal fun Collection<Any?>.displayList(): String = joinToString(", ")

    internal fun findField(members: Collection<KCallable<*>>, name: String, config: JSONConfig): KProperty<*>? {
        for (member in members)
            if (member is KProperty<*> && (config.findNameFromAnnotation(member.annotations) ?: member.name) == name)
                return member
        return null
    }

    internal fun KType.applyTypeParameters(enclosingType: KType): KType {
        // TODO - this implementation works for simple cases, but if the class parameter is itself a parameter to
        //    another class, it can result in a KType with a KTypeParameter, and we have no way of applying that
        //    KTypeParameter (see JSONDeserializerObjectTest)
        val typeClassifier = classifier
        if (typeClassifier is KTypeParameter) {
            val enclosingClass = enclosingType.classifier
            if (enclosingClass !is KClass<*>)
                cantDeserialize(enclosingType)
            val typeParameters = enclosingClass.typeParameters
            val index = typeParameters.indexOfFirst { it.name == typeClassifier.name }
            if (index >= 0) {
                val enclosingTypeArguments = enclosingType.arguments
                if (index < enclosingTypeArguments.size) {
                    val enclosingTypeArgumentType = enclosingTypeArguments[index].type
                    if (enclosingTypeArgumentType != null)
                        return enclosingTypeArgumentType
                }
                val typeParameterUpperBounds = typeParameters[index].upperBounds
                if (typeParameterUpperBounds.size == 1)
                    return typeParameterUpperBounds[0]
            }
        }

        if (arguments.isEmpty())
            return this

        if (typeClassifier !is KClass<*>)
            cantDeserialize(this)
        if (typeClassifier.typeParameters.isEmpty()) // has Array<Int> become IntArray?
            return this
        return typeClassifier.createType(arguments.map { (variance, type) ->
            if (variance == null || type == null)
                KTypeProjection.STAR
            else
                type.applyTypeParameters(enclosingType).let {
                    when (variance) {
                        KVariance.INVARIANT -> KTypeProjection.invariant(it)
                        KVariance.IN -> KTypeProjection.contravariant(it)
                        KVariance.OUT -> KTypeProjection.covariant(it)
                    }
                }
        }, isMarkedNullable, annotations)
    }

    private fun cantDeserialize(type: KType, pointer: JSONPointer? = null): Nothing {
        fatal("Can't deserialize $type - insufficient type information", pointer)
    }

}
