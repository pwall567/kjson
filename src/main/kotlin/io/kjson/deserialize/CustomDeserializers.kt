/*
 * @(#) CustomDeserializers.kt
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
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

import java.lang.reflect.InvocationTargetException

import io.kjson.FromJSONMapping
import io.kjson.JSONConfig
import io.kjson.JSONDeserializerFunctions
import io.kjson.JSONException
import io.kjson.JSONKotlinException
import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.JSONValue

class ConfigFromJSONDeserializer<T : Any>(
    private val resultType: KType,
    private val config: JSONConfig,
    private val fromJSONMapping: FromJSONMapping,
) : Deserializer<T> {

    @Suppress("unchecked_cast")
    override fun deserialize(json: JSONValue?): T {
        return try {
            config.fromJSONMapping(json) as T
        } catch (de: DeserializationException) {
            throw de
        } catch (jke: JSONKotlinException) {
            val exception = DeserializationException(jke.text, underlying = jke.cause)
            val pointer = jke.pointer
            if (pointer != null)
                throw exception.nested(pointer)
            else
                throw exception
        } catch (jsonException: JSONException) {
            throw jsonException
        } catch (e: Exception) {
            fatal("Error in custom fromJSON mapping of $resultType", cause = e)
        }
    }

}

class InClassMultiFromJSONDeserializer<T : Any>(
    private val resultClass: KClass<T>,
    private val config: JSONConfig,
    private val inClassFromJSONList: List<JSONDeserializerFunctions.InClassFromJSON<T>>,
) : Deserializer<T> {

    override fun deserialize(json: JSONValue?): T {
        val inClassFromJSON = if (json == null)
            inClassFromJSONList.find { it.jsonNullable }
        else {
            val jsonClass = json::class
            var bestMatch: JSONDeserializerFunctions.InClassFromJSON<T>? = null
            for (candidate in inClassFromJSONList) {
                if (candidate.jsonValueClass.isSuperclassOf(jsonClass)) {
                    if (bestMatch == null || candidate.jsonValueClass.isSubclassOf(bestMatch.jsonValueClass))
                        bestMatch = candidate
                }
            }
            bestMatch
        }
        if (inClassFromJSON == null) {
            val jsonClassName = if (json == null) "null" else json::class.simpleName
            throw DeserializationException("Can't find deserializer for $jsonClassName")
        }
        try {
            return inClassFromJSON.invoke(json, config)
        }
        catch (ite: InvocationTargetException) {
            val cause = ite.cause
            if (cause is JSONException)
                throw cause
            throw DeserializationException("Error in custom in-class fromJSON - ${resultClass.qualifiedName}",
                    underlying = ite.cause ?: ite)
        }
        catch (e: Exception) {
            throw DeserializationException("Error in custom in-class fromJSON - ${resultClass.qualifiedName}", underlying = e)
        }
    }

}
