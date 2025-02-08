/*
 * @(#) JSONDeserializer.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023, 2024, 2025 Peter Wall
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
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
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

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.util.HashMap
import java.util.LinkedList
import java.util.stream.Stream

import io.kjson.JSONDeserializerFunctions.displayName
import io.kjson.JSONDeserializerFunctions.findAllInClassFromJSON
import io.kjson.JSONDeserializerFunctions.findSingleParameterConstructor
import io.kjson.JSONDeserializerFunctions.isPublic
import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.deserialize.AnyDeserializer
import io.kjson.deserialize.ArrayDeserializer
import io.kjson.deserialize.BigDecimalDeserializer
import io.kjson.deserialize.BigIntegerDeserializer
import io.kjson.deserialize.BooleanDeserializer
import io.kjson.deserialize.ByteDeserializer
import io.kjson.deserialize.ClassMultiConstructorDeserializer
import io.kjson.deserialize.KotlinSingleArgConstructorDescriptor
import io.kjson.deserialize.ClassSingleConstructorDeserializer
import io.kjson.deserialize.ConfigFromJSONDeserializer
import io.kjson.deserialize.ConstructorDescriptor
import io.kjson.deserialize.KotlinConstructorDescriptor
import io.kjson.deserialize.Deserializer
import io.kjson.deserialize.EnumDeserializer
import io.kjson.deserialize.FieldDescriptor
import io.kjson.deserialize.InClassMultiFromJSONDeserializer
import io.kjson.deserialize.KotlinFieldDescriptor
import io.kjson.deserialize.DeferredDeserializer
import io.kjson.deserialize.DelegatingMapConstructorDeserializer
import io.kjson.deserialize.DeserializationException
import io.kjson.deserialize.DoubleDeserializer
import io.kjson.deserialize.FloatDeserializer
import io.kjson.deserialize.IntDeserializer
import io.kjson.deserialize.JavaClassDeserializerFunctions.createJavaClassDeserializer
import io.kjson.deserialize.KotlinParameterDescriptor
import io.kjson.deserialize.LongDeserializer
import io.kjson.deserialize.MapConstructorDeserializer
import io.kjson.deserialize.ObjectDeserializer
import io.kjson.deserialize.OptDeserializer
import io.kjson.deserialize.PairDeserializer
import io.kjson.deserialize.SealedClassDeserializer
import io.kjson.deserialize.ShortDeserializer
import io.kjson.deserialize.StringDeserializer
import io.kjson.deserialize.TripleDeserializer
import io.kjson.deserialize.UByteDeserializer
import io.kjson.deserialize.UIntDeserializer
import io.kjson.deserialize.ULongDeserializer
import io.kjson.deserialize.UShortDeserializer
import io.kjson.deserialize.createCollectionDeserializer
import io.kjson.deserialize.createMapDeserializer
import io.kjson.deserialize.createSequenceDeserializer
import io.kjson.deserialize.createStreamDeserializer
import io.kjson.deserialize.errorDisplay
import io.kjson.optional.Opt
import io.kjson.pointer.JSONPointer
import io.kjson.pointer.find
import io.kjson.util.isKotlinClass

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
                determineDeserializer(resultType, config, references = mutableListOf())

        if (deserializer != null) {
            try {
                val result = deserializer.deserialize(json)
                if (result == null && !resultType.isMarkedNullable)
                    fatal("Can't deserialize null as ${resultClass.qualifiedName}")
                return result
            }
            catch (de: DeserializationException) {
                fatal(de.messageFunction(de.pointer.find(json)), de.pointer, de.underlying)
            }
            catch (je: JSONException) {
                throw je
            }
            catch (ite: InvocationTargetException) {
                val cause = ite.targetException
                fatal(
                    text = "Error deserializing $resultType - " + (cause?.message ?: "InvocationTargetException"),
                    cause = cause ?: ite,
                )
            }
            catch (e: Exception) {
                fatal("Error deserializing $resultType - ${e.message ?: e::class.simpleName}", cause = e)
            }
        }
        fatal("Can't deserialize ${json.errorDisplay()} as ${resultClass.qualifiedName}")
    }

    @Suppress("unchecked_cast")
    internal fun <T : Any> findDeserializer(
        resultType: KType,
        config: JSONConfig,
        references: MutableList<KType>,
    ): Deserializer<T>? {
        config.findDeserializer(resultType)?.let { return it as Deserializer<T> }
        return determineDeserializer(resultType, config, references)
    }

    internal fun <T : Any> determineDeserializer(
        resultType: KType,
        config: JSONConfig,
        references: MutableList<KType>,
    ): Deserializer<T>? {

        if (resultType in references)
            return DeferredDeserializer(resultType, config)
        references.add(resultType)

        try {

            // common cacheable types - custom deserialization, enum, List, Map etc.

            determineDeserializerForCacheableTypes<T>(resultType, config, references)?.let {
                return it.andStore(resultType, config)
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
            return EnumDeserializer(resultClass) as Deserializer<T>

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
            return PairDeserializer.create<Any, Any>(resultType, config, references) as Deserializer<T>?

        // is it a Triple?

        if (resultClass == Triple::class)
            return TripleDeserializer.create<Any, Any, Any>(resultType, config, references) as Deserializer<T>?

        // is it an Opt?

        if (resultClass == Opt::class) {
            val optType = getTypeParam(resultType.arguments).applyTypeParameters(resultType)
            val optDeserializer = findDeserializer<Any>(optType, config, references) ?: return null
            return OptDeserializer(optDeserializer) as Deserializer<T>
        }

        // is it a sealed class

        if (resultClass.isSealed)
            return SealedClassDeserializer.create(resultClass, config, references)

        // is it a java.util.stream.Stream?

        if (resultClass == Stream::class)
            return createStreamDeserializer<Any>(resultType, config, references) as Deserializer<T>

        return null
    }

    @Suppress("unchecked_cast")
    private fun <T : Enum<T>> KClass<*>.toEnumClass(): Class<Enum<T>> = java as Class<Enum<T>>

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

        if (!resultClass.isKotlinClass())
            return createJavaClassDeserializer(resultClass, resultType, config, references)

        // deserialize using constructor

        val constructorDescriptors = ConstructorDescriptorList(resultClass)
        val fields = resultClass.getFields(resultType, config, references)
        for (constructor in resultClass.constructors) {
            if (constructor.isPublic() && constructor.parameters.none {
                it.name == null || it.kind != KParameter.Kind.VALUE
            }) {
                if (constructor.hasSingleParameter())
                    constructorDescriptors.addSingleParameterDescriptors(constructor, resultType, config, references)
                val parameterDescriptors = mutableListOf<KotlinParameterDescriptor<*>>()
                for (parameter in constructor.parameters) {
                    val parameterDescriptor = KotlinParameterDescriptor.create<Any>(
                        parameter = parameter,
                        resultType = resultType,
                        config = config,
                        references = references,
                    )
                    if (parameterDescriptor == null)
                        return null
                    parameterDescriptors.add(parameterDescriptor)
                }
                constructorDescriptors.add(
                    KotlinConstructorDescriptor(
                        resultType = resultType,
                        constructor = constructor,
                        parameterDescriptors = parameterDescriptors.ifEmpty { emptyList() },
                        fieldDescriptors = fields.filter { f ->
                            parameterDescriptors.none { p -> f.propertyName == p.propertyName  }
                            // TODO - consider filtering out immutable fields
                        },
                        allowExtra = config.allowExtra ||
                                config.hasAllowExtraPropertiesAnnotation(resultClass.annotations),
                    )
                )
            }
        }
        if (constructorDescriptors.isNotEmpty()) {
            return if (constructorDescriptors.size == 1) {
                ClassSingleConstructorDeserializer(
                    constructorDescriptor = constructorDescriptors[0],
                )
            } else {
                ClassMultiConstructorDeserializer(
                    constructorDescriptors = constructorDescriptors,
                )
            }
        }

        return null
    }

    class ConstructorDescriptorList<T : Any>(
        val resultClass: KClass<T>,
    ) : MutableList<ConstructorDescriptor<T>> by mutableListOf() {

        @Suppress("unchecked_cast")
        fun addSingleParameterDescriptors(
            constructor: KFunction<T>,
            resultType: KType,
            config: JSONConfig,
            references: MutableList<KType>,
        ) {
            val parameterType = constructor.parameters[0].type
            if (!parameterType.isMarkedNullable) {
                val classifier = parameterType.classifier
                if (classifier is KClass<*>) {
                    when (parameterType.classifier) {
                        String::class -> add(constructor, StringDeserializer) { it is JSONString }
                        Boolean::class -> add(constructor, BooleanDeserializer) { it is JSONBoolean }
                        Int::class -> add(constructor, IntDeserializer) { it is JSONNumber && it.isInt() }
                        Long::class -> add(constructor, LongDeserializer) { it is JSONNumber && it.isLong() }
                        Short::class -> add(constructor, ShortDeserializer) { it is JSONNumber && it.isShort() }
                        Byte::class -> add(constructor, ByteDeserializer) { it is JSONNumber && it.isByte() }
                        UInt::class -> add(constructor, UIntDeserializer) { it is JSONNumber && it.isUInt() }
                        ULong::class -> add(constructor, ULongDeserializer) { it is JSONNumber && it.isULong() }
                        UShort::class -> add(constructor, UShortDeserializer) { it is JSONNumber && it.isUShort() }
                        UByte::class -> add(constructor, UByteDeserializer) { it is JSONNumber && it.isUByte() }
                        BigDecimal::class -> add(constructor, BigDecimalDeserializer) { it is JSONNumber }
                        BigInteger::class -> add(constructor, BigIntegerDeserializer) {
                            it is JSONNumber && it.isIntegral()
                        }
                        Double::class -> add(constructor, DoubleDeserializer) { it is JSONNumber }
                        Float::class -> add(constructor, FloatDeserializer) { it is JSONNumber }
                        List::class -> {
                            createCollectionDeserializer(parameterType, config, references) { ArrayList(it) }?.let {
                                add(constructor, it) { json -> json is JSONArray }
                            }
                        }
                        Set::class -> {
                            createCollectionDeserializer(parameterType, config, references) { LinkedHashSet(it) }?.let {
                                add(constructor, it) { json -> json is JSONArray }
                            }
                        }
                    }
                    if (classifier.java.isArray) {
                        val itemType = getTypeParam(parameterType).applyTypeParameters(resultType)
                        val itemClass = itemType.classifier
                        if (itemClass is KClass<*>) {
                            val itemDeserializer = findDeserializer<Any>(itemType, config, references)
                            if (itemDeserializer != null) {
                                val arrayDeserializer = ArrayDeserializer(
                                    itemClass = itemClass as KClass<Any>,
                                    itemDeserializer = itemDeserializer,
                                    itemNullable = itemType.isMarkedNullable,
                                )
                                add(constructor, arrayDeserializer) { it is JSONArray }
                            }
                        }
                    }
                }
            }
        }

        fun add(constructor: KFunction<T>, deserializer: Deserializer<*>, matchFunction: (JSONValue) -> Boolean) {
            add(
                KotlinSingleArgConstructorDescriptor(
                    resultClass = resultClass,
                    constructor = constructor,
                    deserializer = deserializer,
                    matchFunction = matchFunction,
                )
            )
        }

    }

    private fun KFunction<*>.hasSingleParameter(): Boolean {
        return when (parameters.size) {
            0 -> false
            1 -> true
            else -> (1 until parameters.size).all { parameters[it].isOptional }
        }
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
        // TODO - this implementation works for most cases, but if the class parameter is itself a parameter to
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
