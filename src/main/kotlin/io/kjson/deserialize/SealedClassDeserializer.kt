/*
 * @(#) SealedClassDeserializer.kt
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
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.starProjectedType

import io.kjson.JSON.asStringOr
import io.kjson.JSONConfig
import io.kjson.JSONDeserializer.determineDeserializer
import io.kjson.JSONDeserializer.findField
import io.kjson.JSONDeserializerFunctions.displayName
import io.kjson.JSONObject
import io.kjson.JSONValue
import io.kjson.annotation.JSONDiscriminator
import io.kjson.annotation.JSONIdentifier

class SealedClassDeserializer<T : Any>(
    private val resultClass: KClass<T>,
    private val discriminatorName: String,
    private val removeDiscriminator: Boolean,
    private val subClassMap: Map<String, Deserializer<out T>>,
) : Deserializer<T> {

    override fun deserialize(json: JSONValue?): T? {
        if (json == null)
            return null
        if (json !is JSONObject)
            typeError("object")
        val identifier = json[discriminatorName].asStringOr {
            throw DeserializationException("No discriminator for sealed class ${resultClass.displayName()}")
        }
        val subClassDeserializer = subClassMap[identifier] ?:
            throw DeserializationException(
                text = "Can't find identifier $identifier for sealed class ${resultClass.displayName()}",
            )
        val trimmedObject = if (removeDiscriminator) {
            JSONObject.build {
                for (property in json) {
                    if (property.name != discriminatorName)
                        add(property)
                }
            }
        } else
            json
        return subClassDeserializer.deserialize(trimmedObject)
    }

    companion object {

        fun <TT : Any> createSealedClassDeserializer(
            resultClass: KClass<TT>,
            config: JSONConfig,
            references: MutableList<KType>,
        ): SealedClassDeserializer<TT> {
            val discriminatorName = resultClass.findAnnotation<JSONDiscriminator>()?.id ?:
                    config.sealedClassDiscriminator
            val subClassMap = resultClass.sealedSubclasses.associate { subClass ->
                subClass.identifierName() to (config.subClassDeserializer(subClass, references) ?:
                        throw DeserializationException("Can't deserialize ${resultClass.simpleName}"))
            }
            val removeDiscriminator = findField(resultClass.members, discriminatorName, config) == null
            return SealedClassDeserializer(resultClass, discriminatorName, removeDiscriminator, subClassMap)
        }

        private fun KClass<*>.identifierName(): String = findAnnotation<JSONIdentifier>()?.id ?: simpleName ?: "noname"

        private fun <T: Any> JSONConfig.subClassDeserializer(subClass: KClass<T>, references: MutableList<KType>):
                Deserializer<T>? = findDeserializer(subClass) ?: // Note - optimisation to avoid more complex call
                        determineDeserializer(subClass.starProjectedType, this, references)

    }

}
