/*
 * @(#) JSONFun.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023 Peter Wall
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
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

import java.io.Reader
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.parser.Parser
import net.pwall.util.CoOutput

/** Type alias to simplify the definition of `fromJSON` mapping functions. */
typealias FromJSONMapping = JSONConfig.(JSONValue?) -> Any?

/** Type alias to simplify the definition of `toJSON` mapping functions. */
typealias ToJSONMapping = JSONConfig.(Any?) -> Any?

/**
 * Deserialize JSON from string ([CharSequence]) to a specified [KType].
 *
 * @receiver            the JSON in string form
 * @param   resultType  the target type
 * @param   config      an optional [JSONConfig] to customise the conversion
 * @return              the converted object
 */
fun CharSequence.parseJSON(
    resultType: KType,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(resultType, callParser(this.toString(), config), config)

/**
 * Deserialize JSON from string ([CharSequence]) to a specified [KClass].
 *
 * @receiver            the JSON in string form
 * @param   resultClass the target class
 * @param   config      an optional [JSONConfig] to customise the conversion
 * @param   T           the target class
 * @return              the converted object
 */
fun <T : Any> CharSequence.parseJSON(
    resultClass: KClass<T>,
    config: JSONConfig = JSONConfig.defaultConfig,
): T? = JSONDeserializer.deserialize(resultClass, callParser(this.toString(), config), config)

/**
 * Deserialize JSON from string ([CharSequence]) to the inferred [KType].
 *
 * @receiver        the JSON in string form
 * @param   config  an optional [JSONConfig] to customise the conversion
 * @param   T       the target class
 * @return          the converted object
 */
inline fun <reified T> CharSequence.parseJSON(
    config: JSONConfig = JSONConfig.defaultConfig,
): T = parseJSON(typeOf<T>(), config) as T

/**
 * Deserialize JSON from a [Reader] to a specified [KType].
 *
 * @receiver            a [Reader] containing the JSON
 * @param   resultType  the target type
 * @param   config      an optional [JSONConfig] to customise the conversion
 * @return              the converted object
 */
fun Reader.parseJSON(
    resultType: KType,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? {
    val json = JSONStreamer(config.parseOptions).also {
        it.accept(this)
    }.result
    return JSONDeserializer.deserialize(resultType, json, config)
}

/**
 * Deserialize JSON from a [Reader] to a specified [KClass].
 *
 * @receiver            a [Reader] containing the JSON
 * @param   resultClass the target class
 * @param   config      an optional [JSONConfig] to customise the conversion
 * @param   T           the target class
 * @return              the converted object
 */
fun <T : Any> Reader.parseJSON(
    resultClass: KClass<T>,
    config: JSONConfig = JSONConfig.defaultConfig,
): T? {
    val json = JSONStreamer(config.parseOptions).also {
        it.accept(this)
    }.result
    return JSONDeserializer.deserialize(resultClass, json, config)
}

/**
 * Deserialize JSON from a [Reader] to the inferred [KType].
 *
 * @receiver        a [Reader] containing the JSON
 * @param   config  an optional [JSONConfig] to customise the conversion
 * @param   T       the target class
 * @return          the converted object
 */
inline fun <reified T> Reader.parseJSON(
    config: JSONConfig = JSONConfig.defaultConfig,
): T = parseJSON(typeOf<T>(), config) as T

/**
 * Invoke parser with parsing options from config.
 */
private fun callParser(json: String, config: JSONConfig) = Parser.parse(json, config.parseOptions)

/**
 * Stringify any object to JSON.
 *
 * @receiver        the object to be converted to JSON (`null` will be converted to `"null"`)
 * @param   config  an optional [JSONConfig] to customise the conversion
 * @return          the JSON string
 */
fun Any?.stringifyJSON(
    config: JSONConfig = JSONConfig.defaultConfig,
): String = JSONStringify.stringify(this, config)

/**
 * Stringify any object to JSON, using a non-blocking output function.
 *
 * @receiver        the object to be converted to JSON (`null` will be converted to `"null"`)
 * @param   config  an optional [JSONConfig] to customise the conversion
 * @param   out     the output function (`(char) -> Unit`)
 */
suspend fun Any?.coStringifyJSON(
    config: JSONConfig = JSONConfig.defaultConfig,
    out: CoOutput
) = JSONCoStringify.coStringify(this, config, out)

/**
 * Helper method to create a [KType] for a parameterised type, for use as the target type of a deserialization.
 *
 * @param   mainClass       the parameterised class
 * @param   paramClasses    the parameter classes
 * @param   nullable        `true` if the [KType] is to be nullable
 * @return                  the [KType]
 */
fun targetKType(
    mainClass: KClass<*>,
    vararg paramClasses: KClass<*>,
    nullable: Boolean = false,
): KType = mainClass.createType(paramClasses.map { KTypeProjection.covariant(it.starProjectedType) }, nullable)

/**
 * Helper method to create a [KType] for a parameterised type, for use as the target type of a deserialization.
 *
 * @param   mainClass       the parameterised class
 * @param   paramTypes      the parameter types
 * @param   nullable        `true` if the [KType] is to be nullable
 * @return                  the [KType]
 */
fun targetKType(
    mainClass: KClass<*>,
    vararg paramTypes: KType,
    nullable: Boolean = false,
): KType = mainClass.createType(paramTypes.map { KTypeProjection.covariant(it) }, nullable)

/**
 * Convert a Java [Type] to a Kotlin [KType].  This allows Java [Type]s to be used as the target type of a
 * deserialization operation.
 *
 * @receiver    the Java [Type] to be converted
 * @param       nullable    `true` if the [KType] is to be nullable
 * @return      the resulting Kotlin [KType]
 * @throws      JSONKotlinException if the [Type] can not be converted
 */
fun Type.toKType(nullable: Boolean = false): KType = when (this) {
    is Class<*> -> this.kotlin.createType(nullable = nullable)
    is ParameterizedType -> (this.rawType as Class<*>).kotlin.createType(this.actualTypeArguments.map {
        when (it) {
            is WildcardType ->
                if (it.lowerBounds?.firstOrNull() == null)
                    KTypeProjection.covariant(it.upperBounds[0].toKType(true))
                else
                    KTypeProjection.contravariant(it.lowerBounds[0].toKType(true))
            else -> KTypeProjection.invariant(it.toKType(true))
        } }, nullable)
    else -> fatal("Can't handle type: $this")
}

/**
 * Deserialize a [JSONValue] to the inferred type.
 *
 * This function has been superseded by `fromJSONValue`, and may be deprecated or removed in future releases.
 *
 * @receiver    the [JSONValue] (or `null`)
 * @param       config      an optional [JSONConfig] to customise the conversion
 * @param       T           the target class
 * @return      the converted value (may be `null` if the inferred type is nullable)
 * @throws      JSONKotlinException if the value can not be converted
 */
inline fun <reified T> JSONValue?.deserialize(
    config: JSONConfig = JSONConfig.defaultConfig,
): T = JSONDeserializer.deserialize(typeOf<T>(), this, config) as T

/**
 * Deserialize a [JSONValue] to the nominated [KType].
 *
 * This function has been superseded by `fromJSONValue`, and may be deprecated or removed in future releases.
 *
 * @receiver    the [JSONValue] (or `null`)
 * @param       resultType  the target [KType]
 * @param       config      an optional [JSONConfig] to customise the conversion
 * @return      the converted value (may be `null` if the inferred type is nullable)
 * @throws      JSONKotlinException if the value can not be converted
 */
fun JSONValue?.deserialize(
    resultType: KType,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(resultType, this, config)

/**
 * Deserialize a [JSONValue] to the nominated [KClass].
 *
 * This function has been superseded by `fromJSONValue`, and may be deprecated or removed in future releases.
 *
 * @receiver    the [JSONValue] (or `null`)
 * @param       resultClass the target [KClass]
 * @param       config      an optional [JSONConfig] to customise the conversion
 * @param       T           the target class
 * @return      the converted value
 * @throws      JSONKotlinException if the value can not be converted
 */
fun <T : Any> JSONValue?.deserialize(
    resultClass: KClass<T>,
    config: JSONConfig = JSONConfig.defaultConfig,
): T? = JSONDeserializer.deserialize(resultClass, this, config)

/**
 * Deserialize a [JSONValue] to the nominated Java [Class].
 *
 * This function has been superseded by `fromJSONValue`, and may be deprecated or removed in future releases.
 *
 * @receiver    the [JSONValue] (or `null`)
 * @param       javaClass   the target [Class]
 * @param       config      an optional [JSONConfig] to customise the conversion
 * @return      the converted value
 * @throws      JSONKotlinException if the value can not be converted
 */
fun JSONValue?.deserialize(
    javaClass: Class<*>,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(javaClass.toKType(), this, config)

/**
 * Deserialize a [JSONValue] to the nominated Java [Type].
 *
 * This function has been superseded by `fromJSONValue`, and may be deprecated or removed in future releases.
 *
 * @receiver    the [JSONValue] (or `null`)
 * @param       javaType    the target [Type]
 * @param       config      an optional [JSONConfig] to customise the conversion
 * @return      the converted value
 * @throws      JSONKotlinException if the value can not be converted
 */
fun JSONValue?.deserialize(
    javaType: Type,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(javaType, this, config)

/**
 * Deserialize a [JSONValue] to the inferred type.
 *
 * @receiver    the [JSONValue] (or `null`)
 * @param       config      an optional [JSONConfig] to customise the conversion
 * @param       T           the target class
 * @return      the converted value (may be `null` if the inferred type is nullable)
 * @throws      JSONKotlinException if the value can not be converted
 */
inline fun <reified T> JSONValue?.fromJSONValue(
    config: JSONConfig = JSONConfig.defaultConfig,
): T = JSONDeserializer.deserialize(typeOf<T>(), this, config) as T

/**
 * Deserialize a [JSONValue] to the nominated [KType].
 *
 * @receiver    the [JSONValue] (or `null`)
 * @param       resultType  the target [KType]
 * @param       config      an optional [JSONConfig] to customise the conversion
 * @return      the converted value (may be `null` if the target type is nullable)
 * @throws      JSONKotlinException if the value can not be converted
 */
fun JSONValue?.fromJSONValue(
    resultType: KType,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(resultType, this, config)

/**
 * Deserialize a [JSONValue] to the nominated [KClass].
 *
 * @receiver    the [JSONValue] (or `null`)
 * @param       resultClass the target [KClass]
 * @param       config      an optional [JSONConfig] to customise the conversion
 * @param       T           the target class
 * @return      the converted value
 * @throws      JSONKotlinException if the value can not be converted
 */
fun <T : Any> JSONValue?.fromJSONValue(
    resultClass: KClass<T>,
    config: JSONConfig = JSONConfig.defaultConfig,
): T? = JSONDeserializer.deserialize(resultClass, this, config)

/**
 * Deserialize a [JSONValue] to the nominated Java [Class].
 *
 * @receiver    the [JSONValue] (or `null`)
 * @param       javaClass   the target [Class]
 * @param       config      an optional [JSONConfig] to customise the conversion
 * @return      the converted value
 * @throws      JSONKotlinException if the value can not be converted
 */
fun JSONValue?.fromJSONValue(
    javaClass: Class<*>,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(javaClass.toKType(), this, config)

/**
 * Deserialize a [JSONValue] to the nominated Java [Type].
 *
 * @receiver    the [JSONValue] (or `null`)
 * @param       javaType    the target [Type]
 * @param       config      an optional [JSONConfig] to customise the conversion
 * @return      the converted value
 * @throws      JSONKotlinException if the value can not be converted
 */
fun JSONValue?.fromJSONValue(
    javaType: Type,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(javaType.toKType(), this, config)
