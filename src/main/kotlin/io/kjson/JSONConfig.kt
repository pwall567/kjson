/*
 * @(#) JSONConfig.kt
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

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.staticFunctions

import java.lang.reflect.InvocationTargetException

import io.kjson.JSON.displayValue
import io.kjson.JSONDeserializer.deserialize
import io.kjson.JSONDeserializerFunctions.callWithSingle
import io.kjson.JSONDeserializerFunctions.displayName
import io.kjson.JSONDeserializerFunctions.findSingleParameterConstructor
import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.annotation.JSONAllowExtra
import io.kjson.annotation.JSONIgnore
import io.kjson.annotation.JSONIncludeAllProperties
import io.kjson.annotation.JSONIncludeIfNull
import io.kjson.annotation.JSONName
import io.kjson.deserialize.Deserializer
import io.kjson.parser.ParseOptions
import io.kjson.pointer.JSONPointer
import io.kjson.pointer.existsIn
import io.kjson.pointer.find
import io.kjson.serialize.Serializer
import io.kjson.util.TypeMap

/**
 * Configuration class for reflection-based JSON serialization and deserialization for Kotlin.
 *
 * @author  Peter Wall
 */
class JSONConfig(configurator: JSONConfig.() -> Unit = {}) {

    /** Name of property to store sealed class subclass name as discriminator */
    var sealedClassDiscriminator = defaultSealedClassDiscriminator
        set(value) {
            if (value != field) {
                if (value.isNotEmpty()) field = value else fatal("Sealed class discriminator invalid")
                deserializerMapReset()
            }
        }

    /** Read buffer size (for `kjson-ktor`), arbitrarily limited to multiple of 16, not greater than 256K */
    var readBufferSize = defaultBufferSize
        set (value) {
            if ((value and 15) == 0 && value in 16..(256 * 1024))
                field = value
            else
                fatal("Read buffer size invalid - $value")
        }

    /** Initial allocation size for stringify operations, arbitrarily limited to 16 to 256K */
    var stringifyInitialSize = defaultStringifyInitialSize
        set (value) {
            if (value in 16..(256 * 1024))
                field = value
            else
                fatal("Stringify initial allocation size invalid - $value")
        }

    /** Character set (for `kjson-ktor`) */
    var charset = defaultCharset

    /** Parse options (for lenient parsing) */
    var parseOptions = defaultParseOptions

    /** Switch to control how `BigInteger` is serialized / deserialized: `true` -> string, `false` -> number */
    var bigIntegerString = defaultBigIntegerString

    /** Switch to control how `BigDecimal` is serialized / deserialized: `true` -> string, `false` -> number */
    var bigDecimalString = defaultBigDecimalString

    /** Switch to control whether null fields in objects are output as "null": `true` -> yes, `false` -> no */
    var includeNulls = defaultIncludeNulls

    /** Switch to control whether extra fields are allowed on deserialization: `true` -> yes, `false` -> no */
    var allowExtra = defaultAllowExtra
        set(value) {
            if (value != field) {
                field = value
                deserializerMapReset()
            }
        }

    /** Switch to control whether to output non-ASCII characters or use escape sequences */
    var stringifyNonASCII = defaultStringifyNonASCII

    /** Switch to control whether `kjson-ktor` uses streamed output */
    var streamOutput = defaultStreamOutput

    private val fromJSONMap: MutableMap<KType, FromJSONMapping> = LinkedHashMap()

    private val toJSONMap: MutableMap<KType, ToJSONMapping> = LinkedHashMap()

    private val nameAnnotations: MutableList<Pair<KClass<*>, KProperty.Getter<String>>> =
            arrayListOf(namePropertyPair(JSONName::class, "name"))

    private val ignoreAnnotations: MutableList<KClass<*>> = arrayListOf(JSONIgnore::class, Transient::class)

    private val includeIfNullAnnotations: MutableList<KClass<*>> = arrayListOf(JSONIncludeIfNull::class)

    private val includeAllPropertiesAnnotations: MutableList<KClass<*>> = arrayListOf(JSONIncludeAllProperties::class)

    private val allowExtraPropertiesAnnotations: MutableList<KClass<*>> = arrayListOf(JSONAllowExtra::class)

    private val deserializerMap = TypeMap(Deserializer.initialEntries)

    private fun deserializerMapReset() {
        deserializerMap.clear()
        for (type in fromJSONMap.keys)
            (type.classifier as? KClass<*>)?.let { deserializerMap.remove(it) }
    }

    private val serializerMap = TypeMap(Serializer.initialEntries)

    private fun serializerMapReset() {
        serializerMap.clear()
        for (type in toJSONMap.keys)
            (type.classifier as? KClass<*>)?.let { serializerMap.remove(it) }
    }

    init {
        apply(configurator)
    }

    @Suppress("unchecked_cast")
    internal fun <T : Any> findDeserializer(targetClass: KClass<T>): Deserializer<T>? {
        return deserializerMap[targetClass] as Deserializer<T>?
    }

    @Suppress("unchecked_cast")
    internal fun findDeserializer(targetType: KType): Deserializer<Any>? {
        return deserializerMap[targetType] as Deserializer<Any>?
    }

    internal fun <T : Any> addDeserializer(targetClass: KClass<T>, deserializer: Deserializer<T> ) {
        deserializerMap[targetClass] = deserializer
    }

    internal fun <T : Any> addDeserializer(type: KType, deserializer: Deserializer<T> ) {
        deserializerMap[type] = deserializer
    }

    @Suppress("unchecked_cast")
    internal fun <T : Any> findSerializer(targetClass: KClass<T>): Serializer<T>? {
        return serializerMap[targetClass] as Serializer<T>?
    }

    @Suppress("unchecked_cast")
    internal fun findSerializer(type: KType): Serializer<Any>? {
        return serializerMap[type] as Serializer<Any>?
    }

    internal fun <T : Any> addSerializer(targetClass: KClass<T>, serializer: Serializer<T>) {
        serializerMap[targetClass] = serializer
    }

    internal fun <T : Any> addSerializer(type: KType, serializer: Serializer<T>) {
        serializerMap[type] = serializer
    }

    /**
     * Find a `fromJSON` mapping function that will create the specified [KType], or the closest subtype of it.
     */
    internal fun findFromJSONMapping(type: KType): FromJSONMapping? {
        if (type.classifier == Any::class)
            return null
        var best: Map.Entry<KType, FromJSONMapping>? = null
        for (entry in fromJSONMap.entries) {
            if (entry.key.isSubtypeOf(type) && best.let { it == null || it.key.isSubtypeOf(entry.key) })
                best = entry
        }
        return best?.value
    }

    /**
     * Find a `fromJSON` mapping function that will create the specified [KClass], or the closest subclass of it.
     */
    internal fun findFromJSONMapping(targetClass: KClass<*>): FromJSONMapping? {
        if (targetClass == Any::class)
            return null
        var best: KClass<*>? = null
        var nullable = false
        var result: FromJSONMapping? = null
        for (entry in fromJSONMap.entries) {
            val classifier = entry.key.classifier as KClass<*>
            if (classifier.isSubclassOf(targetClass) &&
                    (best == null || if (best == classifier) !nullable else best.isSubclassOf(classifier))) {
                best = classifier
                nullable = entry.key.isMarkedNullable
                result = entry.value
            }
        }
        return result
    }

    /**
     * Find a `toJSON` mapping function that will accept the specified [KType], or the closest supertype of it.
     */
    internal fun findToJSONMapping(type: KType): ToJSONMapping? {
        var best: Map.Entry<KType, ToJSONMapping>? = null
        for (entry in toJSONMap.entries) {
            if (entry.key.isSupertypeOf(type) && (best == null || best.key.isSupertypeOf(entry.key)))
                best = entry
        }
        return best?.value
    }

    /**
     * Find a `toJSON` mapping function that will accept the specified [KClass], or the closest superclass of it.
     */
    internal fun findToJSONMapping(sourceClass: KClass<*>): ToJSONMapping? {
        var best: KClass<*>? = null
        var nullable = false
        var result: ToJSONMapping? = null
        for (entry in toJSONMap.entries) {
            val classifier = entry.key.classifier as KClass<*>
            if (classifier.isSuperclassOf(sourceClass) && best.let {
                    it == null || if (it == classifier) nullable else it.isSuperclassOf(classifier) }) {
                best = classifier
                nullable = entry.key.isMarkedNullable
                result = entry.value
            }
        }
        return result
    }

    /**
     * Add custom mapping from a specified type to JSON.
     */
    fun toJSON(type: KType, mapping: ToJSONMapping) {
        toJSONMap[type] = mapping
        serializerMapReset()
    }

    /**
     * Add custom mapping from a specified type to JSON using the `toString()` function to create a JSON string.
     */
    fun toJSONString(type: KType) {
        toJSONMap[type] = { obj -> obj?.let { JSONString(it.toString())} }
        serializerMapReset()
    }

    /**
     * Add custom mapping from an inferred type to JSON.
     */
    inline fun <reified T : Any> toJSON(noinline mapping: JSONConfig.(T) -> JSONValue?) {
        toJSON(typeOf<T>()) { mapping(it as T) }
    }

    /**
     * Add custom mapping from an inferred type to JSON using the `toString()` function to create a JSON string.
     */
    inline fun <reified T : Any> toJSONString() {
        toJSONString(typeOf<T>())
    }

    /**
     * Add custom mapping from JSON to the specified type.
     */
    fun fromJSON(type: KType, mapping: FromJSONMapping) {
        fromJSONMap[type] = mapping
        deserializerMapReset()
    }

    /**
     * Add custom mapping from JSON object to the specified type.
     */
    fun fromJSONObject(type: KType, mapping: JSONConfig.(JSONObject) -> Any) {
        fromSpecific(type, mapping)
    }

    /**
     * Add custom mapping from JSON array to the specified type.
     */
    fun fromJSONArray(type: KType, mapping: JSONConfig.(JSONArray) -> Any) {
        fromSpecific(type, mapping)
    }

    /**
     * Add custom mapping from JSON string to the specified type.  The default mapping looks for a constructor that
     * takes a single [String] parameter.
     */
    fun fromJSONString(type: KType, mapping: JSONConfig.(JSONString) -> Any = getDefaultStringMapping(type)) {
        fromSpecific(type, mapping)
    }

    private inline fun <reified T : JSONValue> fromSpecific(type: KType, crossinline mapping: JSONConfig.(T) -> Any) {
        fromJSON(type) { json ->
            when (json) {
                null -> if (type.isMarkedNullable) null else fatal("Can't deserialize null as $type")
                is T -> mapping(json)
                else -> fatal("Can't deserialize ${json::class.simpleName} as $type")
            }
        }
    }

    /**
     * Add custom mapping from JSON to the inferred type.
     */
    inline fun <reified T : Any> fromJSON(noinline mapping: JSONConfig.(JSONValue?) -> T) {
        fromJSON(typeOf<T>(), mapping as FromJSONMapping)
    }

    /**
     * Add custom mapping from JSON object to the inferred type.
     */
    inline fun <reified T : Any> fromJSONObject(noinline mapping: JSONConfig.(JSONObject) -> T) {
        fromJSONObject(typeOf<T>(), mapping)
    }

    /**
     * Add custom mapping from JSON array to the inferred type.
     */
    inline fun <reified T : Any> fromJSONArray(noinline mapping: JSONConfig.(JSONArray) -> T) {
        fromJSONArray(typeOf<T>(), mapping)
    }

    /**
     * Add custom mapping from JSON string to the specified type.  The default mapping looks for a constructor that
     * takes a single [String] parameter.
     */
    inline fun <reified T : Any> fromJSONString(
        noinline mapping: JSONConfig.(JSONString) -> Any = getDefaultStringMapping(typeOf<T>()),
    ) {
        fromJSONString(typeOf<T>(), mapping)
    }

    /**
     * Get a default mapping from [JSONString] to the specified type.  The default mapping looks for a constructor that
     * takes a single [String] parameter, and invokes that constructor with the string value.
     *
     * **NOTE:** This is public solely because it is used by a public inline function.
     */
    fun getDefaultStringMapping(type: KType): JSONConfig.(JSONString) -> Any = { json ->
        val resultClass = type.classifier as? KClass<*> ?:
                throw JSONKotlinException("Can't create ${type.displayName()} - insufficient type information")
        try {
            resultClass.findSingleParameterConstructor(String::class)?.callWithSingle(json.value) ?:
                    fatal("Can't deserialize string as $type")
        }
        catch (je: JSONException) {
            throw je
        }
        catch (ite: InvocationTargetException) {
            val cause = ite.cause
            if (cause is JSONException)
                throw cause
            fatal("Error deserializing string as $type - ${cause?.message ?: "InvocationTargetException"}",
                    cause = cause ?: ite)
        }
        catch (e: Exception) {
            fatal("Error deserializing string as $type - ${e.message ?: e::class.simpleName}", cause = e)
        }
    }

    /**
     * Add a polymorphic mapping, such that any request to deserialize the specified target base class will determine
     * the derived class using a nominated property and a list of pairs of property value and target type.
     */
    fun fromJSONPolymorphic(
        targetClass: KClass<*>,
        property: String,
        vararg mappings: Pair<Any, KType>,
    ) {
        fromJSONPolymorphic(targetClass.starProjectedType, JSONPointer.root.child(property), *mappings)
    }

    /**
     * Add a polymorphic mapping to the specified target class, using the value selected by a [JSONPointer] and a list
     * of pairs of property value and target type.
     */
    fun fromJSONPolymorphic(
        targetClass: KClass<*>,
        discriminator: JSONPointer,
        vararg mappings: Pair<Any, KType>,
    ) {
        fromJSONPolymorphic(targetClass.starProjectedType, discriminator, *mappings)
    }

    /**
     * Add a polymorphic mapping to the specified target type, using the nominated property and a list of pairs of
     * property value and target type.
     */
    fun fromJSONPolymorphic(
        type: KType,
        property: String,
        vararg mappings: Pair<Any, KType>,
    ) {
        fromJSONPolymorphic(type, JSONPointer.root.child(property), *mappings)
    }

    /**
     * Add a polymorphic mapping to the specified target type, using the value selected by a [JSONPointer] and a list of
     * pairs of property value and target type.
     */
    fun fromJSONPolymorphic(
        type: KType,
        discriminator: JSONPointer,
        vararg mappings: Pair<Any, KType>,
    ) {
        if (mappings.isEmpty())
            fatal("Illegal polymorphic mapping - list is empty")
        val mappingClass = mappings[0].first::class
        for (mapping in mappings) {
            if (mapping.first::class != mappingClass)
                fatal("Illegal polymorphic mapping - discriminator values must be same type")
            if (!mapping.second.isSubtypeOf(type) || mapping.second.isSupertypeOf(type))
                fatal("Illegal polymorphic mapping - ${mapping.second} is not a sub-type of $type")
        }
        fromJSON(type) { jsonValue ->
            if (jsonValue !is JSONObject || !discriminator.existsIn(jsonValue))
                fatal("Can't deserialize ${jsonValue.displayValue()} as $type")
            val discriminatorValue = deserialize(mappingClass, discriminator.find(jsonValue))
            val mapping = mappings.find { it.first == discriminatorValue } ?:
                    fatal("Can't deserialize ${jsonValue.displayValue()} as $type, discriminator value was " +
                                discriminatorValue)
            val resultClass = mapping.second.classifier as? KClass<*> ?: fatal("Can't deserialize ${mapping.second}")
            deserialize(mapping.second, resultClass, jsonValue, this)
        }
    }

    /**
     * Add a polymorphic mapping to the parameter target type, using the nominated property and a list of pairs of
     * property value and target type.
     */
    inline fun <reified T> fromJSONPolymorphic(
        property: String,
        vararg mappings: Pair<Any, KType>,
    ) {
        fromJSONPolymorphic(typeOf<T>(), JSONPointer.root.child(property), *mappings)
    }

    /**
     * Add a polymorphic mapping to the parameter target type, using the value selected by a [JSONPointer] and a list of
     * pairs of property value and target type.
     */
    inline fun <reified T> fromJSONPolymorphic(
        discriminator: JSONPointer,
        vararg mappings: Pair<Any, KType>,
    ) {
        fromJSONPolymorphic(typeOf<T>(), discriminator, *mappings)
    }

    /**
     * Add an annotation specification to the list of annotations that specify the name to be used when serializing or
     * deserializing a property.
     */
    fun <T : Annotation> addNameAnnotation(nameAnnotationClass: KClass<T>, argumentName: String) {
        nameAnnotations.add(namePropertyPair(nameAnnotationClass, argumentName))
        deserializerMapReset()
        serializerMapReset()
    }

    private fun <T : Annotation> namePropertyPair(
        nameAnnotationClass: KClass<T>,
        argumentName: String,
    ): Pair<KClass<*>, KProperty.Getter<String>> {
        return nameAnnotationClass to findAnnotationStringProperty(nameAnnotationClass, argumentName).getter
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Annotation> findAnnotationStringProperty(
        annotationClass: KClass<T>,
        argumentName: String,
    ): KProperty<String> {
        for (member in annotationClass.members) {
            if (member is KProperty<*> && member.name == argumentName && member.returnType == stringType) {
                return member as KProperty<String>
            }
        }
        throw IllegalArgumentException(
            "Annotation class ${annotationClass.simpleName} does not have a String property \"$argumentName\"")
    }

    /**
     * Find the name to be used when serializing or deserializing a property, from the annotations supplied.
     */
    fun findNameFromAnnotation(annotations: List<Annotation>?): String? {
        if (annotations != null) {
            for (entry in nameAnnotations) {
                for (annotation in annotations) {
                    if (annotation::class.isSubclassOf(entry.first))
                        return entry.second.call(annotation)
                }
            }
        }
        return null
    }

    /**
     * Add an annotation specification to the list of annotations that specify that the property is to be ignored when
     * serializing or deserializing.
     */
    fun <T : Annotation> addIgnoreAnnotation(ignoreAnnotationClass: KClass<T>) {
        ignoreAnnotations.add(ignoreAnnotationClass)
        deserializerMapReset()
        serializerMapReset()
    }

    /**
     * Test whether a property has an annotation to indicate that it is to be ignored when serializing or deserializing.
     */
    fun hasIgnoreAnnotation(annotations: List<Annotation>?) = hasBooleanAnnotation(ignoreAnnotations, annotations)

    /**
     * Add an annotation specification to the list of annotations that specify that the property is to be included when
     * serializing or deserializing even if `null`.
     */
    fun <T : Annotation> addIncludeIfNullAnnotation(includeIfNullAnnotationClass: KClass<T>) {
        includeIfNullAnnotations.add(includeIfNullAnnotationClass)
        serializerMapReset()
    }

    /**
     * Test whether a property has an annotation to indicate that it is to be included when serializing or deserializing
     * even if `null`.
     */
    fun hasIncludeIfNullAnnotation(annotations: List<Annotation>?) =
            hasBooleanAnnotation(includeIfNullAnnotations, annotations)

    /**
     * Add an annotation specification to the list of annotations that specify that all properties in a class are to be
     * included when serializing even if `null`.
     */
    fun <T : Annotation> addIncludeAllPropertiesAnnotation(ignoreAllPropertiesAnnotationClass: KClass<T>) {
        includeAllPropertiesAnnotations.add(ignoreAllPropertiesAnnotationClass)
        serializerMapReset()
    }

    /**
     * Test whether a property has an annotation to indicate that it is to be included when serializing even if `null`.
     */
    fun hasIncludeAllPropertiesAnnotation(annotations: List<Annotation>?) =
            hasBooleanAnnotation(includeAllPropertiesAnnotations, annotations)

    /**
     * Add an annotation specification to the list of annotations that specify that extra properties in a class are to
     * be ignored when deserializing.
     */
    fun <T : Annotation> addAllowExtraPropertiesAnnotation(allowExtraPropertiesAnnotationClass: KClass<T>) {
        allowExtraPropertiesAnnotations.add(allowExtraPropertiesAnnotationClass)
        deserializerMapReset()
    }

    /**
     * Test whether a property has an annotation to indicate that extra properties in a class are to be ignored when
     * deserializing.
     */
    fun hasAllowExtraPropertiesAnnotation(annotations: List<Annotation>?) =
            hasBooleanAnnotation(allowExtraPropertiesAnnotations, annotations)

    /**
     * Test whether a property has a boolean annotation matching the specified list.
     */
    private fun hasBooleanAnnotation(annotationList: List<KClass<*>>, annotations: Iterable<Annotation>?): Boolean {
        if (annotations != null) {
            for (entry in annotationList) {
                for (annotation in annotations) {
                    if (annotation::class.isSubclassOf(entry))
                        return true
                }
            }
        }
        return false
    }

    /**
     * Test whether the nominated class has a `@JSONIncludeAllProperties` annotation, or if the `includeNulls` switch is
     * set in this `JSONConfig`.
     */
    fun includeNullFields(objClass: KClass<*>): Boolean =
            includeNulls || hasIncludeAllPropertiesAnnotation(objClass.annotations)

    /**
     * Test whether the nominated Java class has a `@JSONIncludeAllProperties` annotation, or if the `includeNulls`
     * switch is set in this `JSONConfig`.
     */
    fun includeNullFields(jClass: Class<*>): Boolean =
            includeNulls || hasIncludeAllPropertiesAnnotation(jClass.annotations.toList())

    /**
     * Combine another `JSONConfig` into this one.
     */
    fun combineAll(config: JSONConfig) {
        sealedClassDiscriminator = config.sealedClassDiscriminator
        readBufferSize = config.readBufferSize
        stringifyInitialSize = config.stringifyInitialSize
        charset = config.charset
        parseOptions = config.parseOptions
        bigIntegerString = config.bigIntegerString
        bigDecimalString = config.bigDecimalString
        includeNulls = config.includeNulls
        allowExtra = config.allowExtra
        stringifyNonASCII = config.stringifyNonASCII
        streamOutput = config.streamOutput
        combineMappings(config)
        nameAnnotations.addAll(config.nameAnnotations)
        ignoreAnnotations.addAll(config.ignoreAnnotations)
        includeIfNullAnnotations.addAll(config.includeIfNullAnnotations)
        includeAllPropertiesAnnotations.addAll(config.includeAllPropertiesAnnotations)
        allowExtraPropertiesAnnotations.addAll(config.allowExtraPropertiesAnnotations)
        deserializerMapReset()
        serializerMapReset()
    }

    /**
     * Combine custom mappings from another `JSONConfig` into this one.
     */
    fun combineMappings(config: JSONConfig) {
        for ((key, value) in config.fromJSONMap.entries) {
            fromJSONMap[key] = value
            (key.classifier as? KClass<*>)?.let { deserializerMap.remove(it) }
        }
        fromJSONMap.putAll(config.fromJSONMap)
        toJSONMap.putAll(config.toJSONMap)
        deserializerMapReset()
        serializerMapReset()
    }

    /**
     * Create a copy of this `JSONConfig` with changes specified in a lambda.
     */
    fun copy(configurator: JSONConfig.() -> Unit = {}): JSONConfig = JSONConfig().also {
        it.combineAll(this)
        it.apply(configurator)
    }

    // functions to aid in creating "fromJSON" and "toJSON" lambdas.

    /**
     * Deserialize a [JSONValue] to the inferred type using this `JSONConfig`.
     */
    inline fun <reified T> deserialize(json: JSONValue?): T = deserialize(json, this)

    /**
     * Deserialize a [JSONValue] to the specified type using this `JSONConfig`.
     */
    fun deserialize(resultType: KType, json: JSONValue?): Any? = deserialize(resultType, json, this)

    /**
     * Deserialize a [JSONValue] to the specified type using this `JSONConfig`.
     */
    fun <T : Any> deserialize(resultClass: KClass<T>, json: JSONValue?): T? = deserialize(resultClass, json, this)

    /**
     * Deserialize a child property of a [JSONObject] of the inferred type using this `JSONConfig`.  Exceptions will
     * have the property name added.
     */
    inline fun <reified T> deserializeProperty(name: String, json: JSONObject): T {
        try {
            return deserialize(typeOf<T>(), json[name], this) as T
        }
        catch (e: JSONKotlinException) {
            throw e.nested(name)
        }
    }

    /**
     * Deserialize a child property of a [JSONObject] of the specified type using this `JSONConfig`.  Exceptions will
     * have the property name added.
     */
    fun deserializeProperty(resultType: KType, name: String, json: JSONObject): Any? {
        try {
            return deserialize(resultType, json[name], this)
        }
        catch (e: JSONKotlinException) {
            throw e.nested(name)
        }
    }

    /**
     * Deserialize a child property of a [JSONObject] of the specified class using this `JSONConfig`.  Exceptions will
     * have the property name added.
     */
    fun <T : Any> deserializeProperty(resultClass: KClass<T>, name: String, json: JSONObject): T? {
        try {
            return deserialize(resultClass, json[name], this)
        }
        catch (e: JSONKotlinException) {
            throw e.nested(name)
        }
    }

    /**
     * Deserialize a child item of a [JSONArray] of the inferred type using this `JSONConfig`.  Exceptions will have the
     * array index added.
     */
    inline fun <reified T> deserializeItem(index: Int, json: JSONArray): T {
        try {
            return deserialize(typeOf<T>(), json[index], this) as T
        }
        catch (e: JSONKotlinException) {
            throw e.nested(index)
        }
    }

    /**
     * Deserialize a child item of a [JSONArray] of the specified type using this `JSONConfig`.  Exceptions will have
     * the property name added.
     */
    fun deserializeItem(resultType: KType, index: Int, json: JSONArray): Any? {
        try {
            return deserialize(resultType, json[index], this)
        }
        catch (e: JSONKotlinException) {
            throw e.nested(index)
        }
    }

    /**
     * Deserialize a child item of a [JSONArray] of the specified class using this `JSONConfig`.  Exceptions will have
     * the property name added.
     */
    fun <T : Any> deserializeItem(resultClass: KClass<T>, index: Int, json: JSONArray): T? {
        try {
            return deserialize(resultClass, json[index], this)
        }
        catch (e: JSONKotlinException) {
            throw e.nested(index.toString())
        }
    }

    /**
     * Serialize a value using this `JSONConfig`.
     */
    fun serialize(obj: Any?): JSONValue? = JSONSerializer.serialize(obj, this)

    /**
     * Serialize a property using this `JSONConfig` and add it to a [JSONObject.Builder] with that name.  Exceptions
     * will have the property name added.
     */
    fun JSONObject.Builder.addProperty(name: String, obj: Any?) {
        when {
            obj != null -> try {
                add(name, JSONSerializer.serialize(obj, this@JSONConfig))
            } catch (e: JSONKotlinException) {
                throw e.nested(name)
            }
            includeNulls -> add(name, null)
        }
    }

    /**
     * Serialize an array item using this `JSONConfig` and add it to a [JSONArray.Builder] with that name.  Exceptions
     * will have the array index added.
     */
    fun JSONArray.Builder.addItem(index: Int, obj: Any?) {
        when {
            obj != null -> try {
                add(JSONSerializer.serialize(obj, this@JSONConfig))
            } catch (e: JSONKotlinException) {
                throw e.nested(index)
            }
            includeNulls -> add(null)
        }
    }

    /**
     * Serialize the given property to a [JSONValue], using this `JSONConfig`.  Exceptions will have the property name
     * added.
     */
    fun serializeProperty(name: String, obj: Any?): JSONValue? = if (obj == null)
        null
    else
        try {
            JSONSerializer.serialize(obj, this)
        } catch (e: JSONKotlinException) {
            throw e.nested(name)
        }

    /**
     * Serialize the given array item to a [JSONValue], using this `JSONConfig`.  Exceptions will have the array index
     * added.
     */
    fun serializeItem(index: Int, obj: Any?): JSONValue? = if (obj == null)
        null
    else
        try {
            JSONSerializer.serialize(obj, this)
        } catch (e: JSONKotlinException) {
            throw e.nested(index)
        }

    companion object {

        val stringType = String::class.createType()
        val defaultSealedClassDiscriminator = getDefaultDiscriminatorName()
        val defaultBufferSize = getIntProperty("io.kjson.defaultBufferSize", DEFAULT_BUFFER_SIZE)
        val defaultStringifyInitialSize = getIntProperty("io.kjson.defaultStringifyInitialSize", 2048)
        val defaultBigIntegerString = getBooleanPropertyOrFalse("io.kjson.defaultBigIntegerString")
        val defaultBigDecimalString = getBooleanPropertyOrFalse("io.kjson.defaultBigDecimalString")
        val defaultIncludeNulls = getBooleanPropertyOrFalse("io.kjson.defaultIncludeNulls")
        val defaultAllowExtra = getBooleanPropertyOrFalse("io.kjson.defaultAllowExtra")
        val defaultStringifyNonASCII = getBooleanPropertyOrFalse("io.kjson.defaultStringifyNonASCII")
        val defaultStreamOutput = getBooleanPropertyOrFalse("io.kjson.defaultStreamOutput")
        val defaultCharset = Charsets.UTF_8
        val defaultParseOptions = ParseOptions(
            objectKeyDuplicate = getEnumProperty("io.kjson.objectKeyDuplicate", JSONObject.DuplicateKeyOption.ERROR),
            objectKeyUnquoted = getBooleanPropertyOrFalse("io.kjson.objectKeyUnquoted"),
            objectTrailingComma = getBooleanPropertyOrFalse("io.kjson.objectTrailingComma"),
            arrayTrailingComma = getBooleanPropertyOrFalse("io.kjson.arrayTrailingComma"),
            maximumNestingDepth = getIntProperty("io.kjson.maximumNestingDepth", 1000),
        )
        val defaultConfig = JSONConfig()

        private fun getBooleanPropertyOrFalse(propertyName: String): Boolean {
            val property = System.getProperty(propertyName) ?: return false
            if (property.equals("true", ignoreCase = true))
                return true
            if (property.equals("false", ignoreCase = true))
                return false
            fatal("$propertyName property invalid - $property")
        }

        private fun getIntProperty(propertyName: String, defaultValue: Int): Int {
            val property = System.getProperty(propertyName) ?: return defaultValue
            return try {
                property.toInt()
            } catch (_: NumberFormatException) {
                fatal("$propertyName property invalid - $property")
            }
        }

        private fun getDefaultDiscriminatorName(): String {
            return System.getProperty("io.kjson.defaultSealedClassDiscriminator") ?: "class"
        }

        private inline fun <reified T : Enum<T>> getEnumProperty(propertyName: String, defaultValue: T): T {
            val property = System.getProperty(propertyName) ?: return defaultValue
            return try {
                T::class.staticFunctions.find { it.name == "valueOf" }?.call(property) as T
            } catch (_ : Exception) {
                fatal("$propertyName property invalid - $property")
            }
        }

    }

}
