/*
 * @(#) TripleDeserializer.kt
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

import kotlin.reflect.KType

import io.kjson.JSONArray
import io.kjson.JSONConfig
import io.kjson.JSONDeserializer.applyTypeParameters
import io.kjson.JSONDeserializer.findDeserializer
import io.kjson.JSONDeserializer.getTypeParam
import io.kjson.JSONValue

class TripleDeserializer<F, S, T>(
    private val firstDeserializer: Deserializer<F>,
    private val firstNullable: Boolean,
    private val secondDeserializer: Deserializer<S>,
    private val secondNullable: Boolean,
    private val thirdDeserializer: Deserializer<T>,
    private val thirdNullable: Boolean,
) : Deserializer<Triple<F?, S?, T?>> {

    override fun deserialize(json: JSONValue?): Triple<F?, S?, T?>? {
        if (json == null)
            return null
        if (json !is JSONArray || json.size != 3)
            typeError("array of length 3")
        val first = firstDeserializer.deserializeValue(json[0], firstNullable, "Triple item", "0")
        val second = secondDeserializer.deserializeValue(json[1], secondNullable, "Triple item", "1")
        val third = thirdDeserializer.deserializeValue(json[2], thirdNullable, "Triple item", "2")
        return Triple(first, second, third)
    }

    companion object {

        fun <FF : Any, SS : Any, TT : Any> create(
            resultType: KType,
            config: JSONConfig,
            references: MutableList<KType>,
        ) : TripleDeserializer<FF, SS, TT>? {
            val typeArguments = resultType.arguments
            val firstType = getTypeParam(typeArguments, 0).applyTypeParameters(resultType)
            val firstDeserializer = findDeserializer<FF>(firstType, config, references) ?: return null
            val secondType = getTypeParam(typeArguments, 1).applyTypeParameters(resultType)
            val secondDeserializer = findDeserializer<SS>(secondType, config, references) ?: return null
            val thirdType = getTypeParam(typeArguments, 2).applyTypeParameters(resultType)
            val thirdDeserializer = findDeserializer<TT>(thirdType, config, references) ?: return null
            return TripleDeserializer(
                firstDeserializer = firstDeserializer,
                firstNullable = firstType.isMarkedNullable,
                secondDeserializer = secondDeserializer,
                secondNullable = secondType.isMarkedNullable,
                thirdDeserializer = thirdDeserializer,
                thirdNullable = thirdType.isMarkedNullable,
            )
        }

    }

}
