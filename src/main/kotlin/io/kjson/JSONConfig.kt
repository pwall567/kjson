/*
 * @(#) JSONConfig.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022 Peter Wall
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
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.typeOf

import java.math.BigDecimal

import io.kjson.JSON.asBoolean
import io.kjson.JSON.asDecimal
import io.kjson.JSON.asInt
import io.kjson.JSON.asLong
import io.kjson.JSON.asString
import io.kjson.JSON.displayValue
import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.annotation.JSONAllowExtra
import io.kjson.annotation.JSONIgnore
import io.kjson.annotation.JSONIncludeAllProperties
import io.kjson.annotation.JSONIncludeIfNull
import io.kjson.annotation.JSONName
import io.kjson.parser.ParseOptions
import io.kjson.pointer.JSONPointer

/**
 * Configuration class for reflection-based JSON serialization and deserialization for Kotlin.
 *
 * @author  Peter Wall
 */
class JSONConfig(configurator: JSONConfig.() -> Unit = {}) {

    /** Name of property to store sealed class subclass name as discriminator */
    var sealedClassDiscriminator = defaultSealedClassDiscriminator
        set(newValue) {
            if (newValue.isNotEmpty()) field = newValue else fatal("Sealed class discriminator invalid")
        }

    /** Read buffer size (for `kjson-ktor`), arbitrarily limited to multiple of 16, not greater than 256K */
    var readBufferSize = defaultBufferSize
        set (newValue) {
            if ((newValue and 15) == 0 && newValue in 16..(256 * 1024))
                field = newValue
            else
                fatal("Read buffer size invalid - $newValue")
        }

    /** Initial allocation size for stringify operations, arbitrarily limited to 16 to 256K */
    var stringifyInitialSize = defaultStringifyInitialSize
        set (newValue) {
            if (newValue in 16..(256 * 1024))
                field = newValue
            else
                fatal("Stringify initial allocation size invalid - $newValue")
        }

    /** Character set (for `json-ktor` and  `json-ktor-client`) */
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

    /** Switch to control whether to output non-ASCII characters or use escape sequences */
    var stringifyNonASCII = defaultStringifyNonASCII

    /** Switch to control whether `json-ktor` uses streamed output */
    var streamOutput = defaultStreamOutput

    private val fromJSONMap: MutableMap<KType, FromJSONMapping> = LinkedHashMap()

    private val toJSONMap: MutableMap<KType, ToJSONMapping> = LinkedHashMap()

    private val nameAnnotations: MutableList<Pair<KClass<*>, KProperty.Getter<String>>> =
            arrayListOf(namePropertyPair(JSONName::class, "name"))

    private val ignoreAnnotations: MutableList<KClass<*>> = arrayListOf(JSONIgnore::class, Transient::class)

    private val includeIfNullAnnotations: MutableList<KClass<*>> = arrayListOf(JSONIncludeIfNull::class)

    private val includeAllPropertiesAnnotations: MutableList<KClass<*>> = arrayListOf(JSONIncludeAllProperties::class)

    private val allowExtraPropertiesAnnotations: MutableList<KClass<*>> = arrayListOf(JSONAllowExtra::class)

    init {
        apply(configurator)
    }

    /**
     * Find a `fromJSON` mapping function that will create the specified [KType], or the closest subtype of it.
     *
     * @param   type    the target type
     * @return          the mapping function, or `null` if not found
     */
    fun findFromJSONMapping(type: KType): FromJSONMapping? {
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
     *
     * @param   targetClass the target class
     * @return              the mapping function, or `null` if not found
     */
    fun findFromJSONMapping(targetClass: KClass<*>): FromJSONMapping? {
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
     *
     * @param   type    the source type
     * @return          the mapping function, or `null` if not found
     */
    fun findToJSONMapping(type: KType): ToJSONMapping? {
        var best: Map.Entry<KType, ToJSONMapping>? = null
        for (entry in toJSONMap.entries) {
            if (entry.key.isSupertypeOf(type) && (best == null || best.key.isSupertypeOf(entry.key)))
                best = entry
        }
        return best?.value
    }

    /**
     * Find a `toJSON` mapping function that will accept the specified [KClass], or the closest superclass of it.
     *
     * @param   sourceClass the source class
     * @return              the mapping function, or `null` if not found
     */
    fun findToJSONMapping(sourceClass: KClass<*>): ToJSONMapping? {
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
     * Add custom mapping from JSON to the specified type.
     *
     * @param   type    the target type
     * @param   mapping the mapping function
     */
    fun fromJSON(type: KType, mapping: FromJSONMapping) {
        fromJSONMap[type] = mapping
    }

    /**
     * Add custom mapping from JSON to the specified type, using a constructor that takes a single [String] parameter.
     *
     * @param   type    the target type
     */
    fun fromJSONString(type: KType) {
        fromJSONMap[type] = { json ->
            when (json) {
                null -> if (type.isMarkedNullable) null else fatal("Can't deserialize null as $type")
                is JSONString -> {
                    val resultClass = type.classifier as? KClass<*> ?: fatal("Can't deserialize $type")
                    val constructor = resultClass.constructors.find {
                        it.parameters.size == 1 && it.parameters[0].type == stringType
                    }
                    constructor?.call(json.value) ?: fatal("Can't deserialize $type")
                }
                else -> fatal("Can't deserialize ${json::class.simpleName} as $type")
            }
        }
    }

    /**
     * Add custom mapping from a specified type to JSON.
     *
     * @param   type    the source type
     * @param   mapping the mapping function
     */
    fun toJSON(type: KType, mapping: ToJSONMapping) {
        toJSONMap[type] = mapping
    }

    /**
     * Add custom mapping from a specified type to JSON using the `toString()` function to create a JSON string.
     *
     * @param   type    the source type
     */
    fun toJSONString(type: KType) {
        toJSONMap[type] = { obj -> obj?.let { JSONString(it.toString())} }
    }

    /**
     * Add custom mapping from JSON to the inferred type.
     *
     * @param   mapping the mapping function
     * @param   T       the type to be mapped
     */
    inline fun <reified T : Any> fromJSON(noinline mapping: JSONConfig.(JSONValue?) -> T?) {
        fromJSON(typeOf<T>(), mapping as FromJSONMapping)
    }

    /**
     * Add custom mapping from JSON to the inferred type, using a constructor that takes a single [String] parameter.
     *
     * @param   T       the type to be mapped
     */
    inline fun <reified T : Any> fromJSONString() {
        fromJSONString(typeOf<T>())
    }

    /**
     * Add custom mapping from an inferred type to JSON.
     *
     * @param   mapping the mapping function
     * @param   T       the type to be mapped
     */
    inline fun <reified T : Any> toJSON(noinline mapping: JSONConfig.(T?) -> JSONValue?) {
        toJSON(typeOf<T>()) { mapping(it as T?) }
    }

    /**
     * Add custom mapping from an inferred type to JSON using the `toString()` function to create a JSON string.
     *
     * @param   T       the type to be mapped
     */
    inline fun <reified T : Any> toJSONString() {
        toJSONString(typeOf<T>())
    }

    /**
     * Add a polymorphic mapping to the specified target class, using the nominated property and a list of pairs of
     * property value and target type.
     *
     * @param   targetClass     the target class
     * @param   property        the property name
     * @param   mappings        a set of `Pair<Any, KType>` entries, each of which identifies a value for the
     *                          discriminator property and its associated type
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
     *
     * @param   targetClass     the target class
     * @param   discriminator   the "discriminator" [JSONPointer]
     * @param   mappings        a set of `Pair<Any, KType>` entries, each of which identifies a value for the
     *                          discriminator property and its associated type
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
     *
     * @param   type            the target type
     * @param   property        the property name
     * @param   mappings        a set of `Pair<Any, KType>` entries, each of which identifies a value for the
     *                          discriminator property and its associated type
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
     *
     * @param   type            the target type
     * @param   discriminator   the "discriminator" [JSONPointer]
     * @param   mappings        a set of `Pair<Any, KType>` entries, each of which identifies a value for the
     *                          discriminator property and its associated type
     */
    fun fromJSONPolymorphic(
        type: KType,
        discriminator: JSONPointer,
        vararg mappings: Pair<Any, KType>,
    ) {
        for (mapping in mappings)
            if (!mapping.second.isSubtypeOf(type))
                fatal("Illegal polymorphic mapping - ${mapping.second} is not a sub-type of $type")
        fromJSON(type) { jsonValue ->
            if (jsonValue !is JSONObject || !discriminator.existsIn(jsonValue))
                fatal("Can't deserialize ${jsonValue.displayValue()} as $type")
            val discriminatorValue = discriminator.find(jsonValue)
            val mapping = mappings.find { it.first.matchesJSONValue(discriminatorValue) } ?:
                fatal("Can't deserialize ${jsonValue.displayValue()} as $type")
            JSONDeserializer.deserialize(mapping.second, jsonValue, this)
        }
    }

    private fun Any.matchesJSONValue(value: JSONValue?) = when (this) {
        is JSONValue -> this == value
        is String -> this == value.asString
        is Int -> this == value.asInt
        is Long -> this == value.asLong
        is BigDecimal -> this == value.asDecimal
        is Boolean -> this == value.asBoolean
        else -> false
    }

    /**
     * Add an annotation specification to the list of annotations that specify the name to be used when serializing or
     * deserializing a property.
     *
     * @param   nameAnnotationClass the annotation class to specify the name
     * @param   argumentName        the name of the argument to the annotation that holds the name
     * @param   T                   the annotation class
     */
    fun <T : Annotation> addNameAnnotation(nameAnnotationClass: KClass<T>, argumentName: String) {
        nameAnnotations.add(namePropertyPair(nameAnnotationClass, argumentName))
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
     *
     * @param   annotations the [Annotation]s (from the parameter, or the property, or both)
     * @return              the name to be used, or `null` if no annotation found
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
     *
     * @param   ignoreAnnotationClass   the annotation class
     * @param   T                       the annotation class
     */
    fun <T : Annotation> addIgnoreAnnotation(ignoreAnnotationClass: KClass<T>) {
        ignoreAnnotations.add(ignoreAnnotationClass)
    }

    /**
     * Test whether a property has an annotation to indicate that it is to be ignored when serializing or deserializing.
     *
     * @param   annotations the [Annotation]s (from the parameter, or the property, or both)
     * @return              `true` if an "ignore" annotation appears in the supplied list
     */
    fun hasIgnoreAnnotation(annotations: List<Annotation>?) = hasBooleanAnnotation(ignoreAnnotations, annotations)

    /**
     * Add an annotation specification to the list of annotations that specify that the property is to be included when
     * serializing or deserializing even if `null`.
     *
     * @param   includeIfNullAnnotationClass    the annotation class
     * @param   T                               the annotation class
     */
    fun <T : Annotation> addIncludeIfNullAnnotation(includeIfNullAnnotationClass: KClass<T>) {
        includeIfNullAnnotations.add(includeIfNullAnnotationClass)
    }

    /**
     * Test whether a property has an annotation to indicate that it is to be included when serializing or deserializing
     * even if `null`.
     *
     * @param   annotations the [Annotation]s (from the parameter, or the property, or both)
     * @return              `true` if an "include if null" annotation appears in the supplied list
     */
    fun hasIncludeIfNullAnnotation(annotations: List<Annotation>?) =
            hasBooleanAnnotation(includeIfNullAnnotations, annotations)

    /**
     * Add an annotation specification to the list of annotations that specify that all properties in a class are to be
     * included when serializing even if `null`.
     *
     * @param   ignoreAllPropertiesAnnotationClass  the annotation class
     * @param   T                                   the annotation class
     */
    fun <T : Annotation> addIncludeAllPropertiesAnnotation(ignoreAllPropertiesAnnotationClass: KClass<T>) {
        includeAllPropertiesAnnotations.add(ignoreAllPropertiesAnnotationClass)
    }

    /**
     * Test whether a property has an annotation to indicate that it is to be included when serializing even if `null`.
     *
     * @param   annotations the [Annotation]s (from the class)
     * @return              `true` if an "include all properties" annotation appears in the supplied list
     */
    fun hasIncludeAllPropertiesAnnotation(annotations: List<Annotation>?) =
            hasBooleanAnnotation(includeAllPropertiesAnnotations, annotations)

    /**
     * Add an annotation specification to the list of annotations that specify that extra properties in a class are to
     * be ignored when deserializing.
     *
     * @param   allowExtraPropertiesAnnotationClass the annotation class
     * @param   T                                   the annotation class
     */
    fun <T : Annotation> addAllowExtraPropertiesAnnotation(allowExtraPropertiesAnnotationClass: KClass<T>) {
        allowExtraPropertiesAnnotations.add(allowExtraPropertiesAnnotationClass)
    }

    /**
     * Test whether a property has an annotation to indicate that extra properties in a class are to be ignored when
     * deserializing.
     *
     * @param   annotations the [Annotation]s (from the class)
     * @return              `true` if an "allow extra properties" annotation appears in the supplied list
     */
    fun hasAllowExtraPropertiesAnnotation(annotations: List<Annotation>?) =
            hasBooleanAnnotation(allowExtraPropertiesAnnotations, annotations)

    /**
     * Test whether a property has a boolean annotation matching the specified list.
     *
     * @param   annotationList  the list of pre-configured annotation classes.
     * @param   annotations     the [Annotation]s (from the parameter, or the property, or both)
     * @return                  `true` if an "ignore" annotation appears in the supplied list
     */
    private fun hasBooleanAnnotation(annotationList: List<KClass<*>>, annotations: List<Annotation>?): Boolean {
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
     * Combine another `JSONConfig` into this one.
     *
     * @param   config  the other `JSONConfig`
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
        fromJSONMap.putAll(config.fromJSONMap)
        toJSONMap.putAll(config.toJSONMap)
        nameAnnotations.addAll(config.nameAnnotations)
        ignoreAnnotations.addAll(config.ignoreAnnotations)
        includeIfNullAnnotations.addAll(config.includeIfNullAnnotations)
        includeAllPropertiesAnnotations.addAll(config.includeAllPropertiesAnnotations)
        allowExtraPropertiesAnnotations.addAll(config.allowExtraPropertiesAnnotations)
    }

    /**
     * Combine custom mappings from another `JSONConfig` into this one.
     *
     * @param   config  the other `JSONConfig`
     */
    fun combineMappings(config: JSONConfig) {
        fromJSONMap.putAll(config.fromJSONMap)
        toJSONMap.putAll(config.toJSONMap)
    }

    companion object {

        val stringType = String::class.createType()
        val defaultSealedClassDiscriminator = getDefaultDiscriminatorName()
        val defaultBufferSize = getIntProperty("io.kjson.defaultBufferSize", DEFAULT_BUFFER_SIZE)
        val defaultStringifyInitialSize = getIntProperty("io.kjson.defaultStringifyInitialSize", 1024)
        val defaultBigIntegerString = getBooleanPropertyOrFalse("io.kjson.defaultBigIntegerString")
        val defaultBigDecimalString = getBooleanPropertyOrFalse("io.kjson.defaultBigDecimalString")
        val defaultIncludeNulls = getBooleanPropertyOrFalse("io.kjson.defaultIncludeNulls")
        val defaultAllowExtra = getBooleanPropertyOrFalse("io.kjson.defaultAllowExtra")
        val defaultStringifyNonASCII = getBooleanPropertyOrFalse("io.kjson.defaultStringifyNonASCII")
        val defaultStreamOutput = getBooleanPropertyOrFalse("io.kjson.defaultStreamOutput")
        val defaultCharset = Charsets.UTF_8
        val defaultParseOptions = ParseOptions(
            objectKeyDuplicate = getEnumProperty("io.kjson.objectKeyDuplicate", ParseOptions.DuplicateKeyOption.ERROR),
            objectKeyUnquoted = getBooleanPropertyOrFalse("io.kjson.objectKeyUnquoted"),
            objectTrailingComma = getBooleanPropertyOrFalse("io.kjson.objectTrailingComma"),
            arrayTrailingComma = getBooleanPropertyOrFalse("io.kjson.arrayTrailingComma"),
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
