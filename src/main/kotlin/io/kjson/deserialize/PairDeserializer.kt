/*
 * @(#) PairDeserializer.kt
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

class PairDeserializer<F, S>(
    private val firstDeserializer: Deserializer<F>,
    private val firstNullable: Boolean,
    private val secondDeserializer: Deserializer<S>,
    private val secondNullable: Boolean,
) : Deserializer<Pair<F?, S?>> {

    override fun deserialize(json: JSONValue?): Pair<F?, S?>? {
        if (json == null)
            return null
        if (json !is JSONArray || json.size != 2)
            typeError("array of length 2")
        val first = firstDeserializer.deserializeValue(json[0], firstNullable, "Pair item", "0")
        val second = secondDeserializer.deserializeValue(json[1], secondNullable, "Pair item", "1")
        return first to second
    }

    companion object {

        fun <FF : Any, SS : Any> create(
            resultType: KType,
            config: JSONConfig,
            references: MutableList<KType>,
        ) : PairDeserializer<FF, SS>? {
            val typeArguments = resultType.arguments
            val firstType = getTypeParam(typeArguments, 0).applyTypeParameters(resultType)
            val firstDeserializer = findDeserializer<FF>(firstType, config, references) ?: return null
            val secondType = getTypeParam(typeArguments, 1).applyTypeParameters(resultType)
            val secondDeserializer = findDeserializer<SS>(secondType, config, references) ?: return null
            return PairDeserializer(
                firstDeserializer = firstDeserializer,
                firstNullable = firstType.isMarkedNullable,
                secondDeserializer = secondDeserializer,
                secondNullable = secondType.isMarkedNullable,
            )
        }

    }

}
