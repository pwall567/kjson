/*
 * @(#) JSONFun.kt
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
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

import java.io.Reader
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

import io.kstuff.util.CoOutput

import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.parser.Parser

/** Type alias to simplify the definition of `fromJSON` mapping functions. */
typealias FromJSONMapping = JSONConfig.(JSONValue?) -> Any?

/** Type alias to simplify the definition of `toJSON` mapping functions. */
typealias ToJSONMapping = JSONConfig.(Any?) -> Any?

/**
 * Deserialize JSON from string ([CharSequence]) to a specified [KType].
 */
fun CharSequence.parseJSON(
    resultType: KType,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(resultType, callParser(this.toString(), config), config)

/**
 * Deserialize JSON from string ([CharSequence]) to a specified [KClass].
 */
fun <T : Any> CharSequence.parseJSON(
    resultClass: KClass<T>,
    config: JSONConfig = JSONConfig.defaultConfig,
): T? = JSONDeserializer.deserialize(resultClass, callParser(this.toString(), config), config)

/**
 * Deserialize JSON from string ([CharSequence]) to the inferred [KType].
 */
inline fun <reified T> CharSequence.parseJSON(
    config: JSONConfig = JSONConfig.defaultConfig,
): T = parseJSON(typeOf<T>(), config) as T

/**
 * Deserialize JSON from a [Reader] to a specified [KType].
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
 */
inline fun <reified T : Any?> T.stringifyJSON(
    config: JSONConfig = JSONConfig.defaultConfig,
): String = JSONStringify.stringify(typeOf<T>(), this, config)

/**
 * Stringify any object to JSON, using a non-blocking output function.
 */
suspend inline fun <reified T : Any?> T.coStringifyJSON(
    config: JSONConfig = JSONConfig.defaultConfig,
    noinline out: CoOutput
) {
    JSONCoStringify.coStringify(typeOf<T>(), this, config, out)
}

/**
 * Helper method to create a [KType] for a parameterised type, for use as the target type of a deserialization.
 */
fun targetKType(
    mainClass: KClass<*>,
    vararg paramClasses: KClass<*>,
    nullable: Boolean = false,
): KType = mainClass.createType(paramClasses.map { KTypeProjection.covariant(it.starProjectedType) }, nullable)

/**
 * Helper method to create a [KType] for a parameterised type, for use as the target type of a deserialization.
 */
fun targetKType(
    mainClass: KClass<*>,
    vararg paramTypes: KType,
    nullable: Boolean = false,
): KType = mainClass.createType(paramTypes.map { KTypeProjection.covariant(it) }, nullable)

/**
 * Convert a Java [Type] to a Kotlin [KType].  This allows Java [Type]s to be used as the target type of a
 * deserialization operation.
 */
fun Type.toKType(nullable: Boolean = false): KType = when (this) {
    is Class<*> -> this.kotlin.createType(typeParameters.map {
        // we probably can't do any better than this thanks to type erasure
        KTypeProjection.covariant(java.lang.Object::class.java.toKType())
    }, nullable = nullable)
    is ParameterizedType -> (this.rawType as Class<*>).kotlin.createType(this.actualTypeArguments.map {
        when (it) {
            is WildcardType ->
                if (it.lowerBounds?.firstOrNull() == null)
                    KTypeProjection.covariant(it.upperBounds[0].toKType(true))
                else
                    KTypeProjection.contravariant(it.lowerBounds[0].toKType(true))
            else -> KTypeProjection.invariant(it.toKType(true))
        }
    }, nullable = nullable)
    else -> fatal("Can't handle type: $this")
}

/**
 * Deserialize a [JSONValue] to the inferred type.
 *
 * This function has been superseded by `fromJSONValue`, and may be deprecated or removed in future releases.
 */
inline fun <reified T> JSONValue?.deserialize(
    config: JSONConfig = JSONConfig.defaultConfig,
): T = JSONDeserializer.deserialize(typeOf<T>(), this, config) as T

/**
 * Deserialize a [JSONValue] to the nominated [KType].
 *
 * This function has been superseded by `fromJSONValue`, and may be deprecated or removed in future releases.
 */
fun JSONValue?.deserialize(
    resultType: KType,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(resultType, this, config)

/**
 * Deserialize a [JSONValue] to the nominated [KClass].
 *
 * This function has been superseded by `fromJSONValue`, and may be deprecated or removed in future releases.
 */
fun <T : Any> JSONValue?.deserialize(
    resultClass: KClass<T>,
    config: JSONConfig = JSONConfig.defaultConfig,
): T? = JSONDeserializer.deserialize(resultClass, this, config)

/**
 * Deserialize a [JSONValue] to the nominated Java [Class].
 *
 * This function has been superseded by `fromJSONValue`, and may be deprecated or removed in future releases.
 */
fun JSONValue?.deserialize(
    javaClass: Class<*>,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(javaClass.toKType(), this, config)

/**
 * Deserialize a [JSONValue] to the nominated Java [Type].
 *
 * This function has been superseded by `fromJSONValue`, and may be deprecated or removed in future releases.
 */
fun JSONValue?.deserialize(
    javaType: Type,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(javaType, this, config)

/**
 * Deserialize a parsed [JSONValue] to an unspecified ([Any]) type.  Strings will be converted to `String`, numbers to
 * `Int`, `Long` or `BigDecimal`, booleans to `Boolean`, arrays to `ArrayList<Any?>` and objects to
 * `LinkedHashMap<String, Any?>` (an implementation of `Map` that preserves order).
 */
fun JSONValue?.deserializeAny(): Any? = JSONDeserializer.deserializeAny(this)

/**
 * Deserialize a [JSONValue] to the inferred type.
 */
inline fun <reified T> JSONValue?.fromJSONValue(
    config: JSONConfig = JSONConfig.defaultConfig,
): T = JSONDeserializer.deserialize(typeOf<T>(), this, config) as T

/**
 * Deserialize a [JSONValue] to the nominated [KType].
 */
fun JSONValue?.fromJSONValue(
    resultType: KType,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(resultType, this, config)

/**
 * Deserialize a [JSONValue] to the nominated [KClass].
 */
fun <T : Any> JSONValue?.fromJSONValue(
    resultClass: KClass<T>,
    config: JSONConfig = JSONConfig.defaultConfig,
): T? = JSONDeserializer.deserialize(resultClass, this, config)

/**
 * Deserialize a [JSONValue] to the nominated Java [Class].
 */
fun JSONValue?.fromJSONValue(
    javaClass: Class<*>,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(javaClass.toKType(), this, config)

/**
 * Deserialize a [JSONValue] to the nominated Java [Type].
 */
fun JSONValue?.fromJSONValue(
    javaType: Type,
    config: JSONConfig = JSONConfig.defaultConfig,
): Any? = JSONDeserializer.deserialize(javaType.toKType(), this, config)
