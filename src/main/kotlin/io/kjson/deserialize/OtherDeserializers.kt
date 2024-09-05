/*
 * @(#) OtherDeserializers.kt
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

import java.net.URI
import java.net.URL
import java.util.BitSet
import java.util.UUID

import io.kjson.JSONArray
import io.kjson.JSONDeserializerFunctions
import io.kjson.JSONNumber
import io.kjson.JSONString
import io.kjson.JSONValue

data object UUIDDeserializer : Deserializer<UUID> {
    override fun deserialize(json: JSONValue?): UUID? = when (json) {
        null -> null
        is JSONString -> JSONDeserializerFunctions.createUUID(json.value)
        else -> typeError("string")
    }
}

data object BitSetDeserializer : Deserializer<BitSet> {
    override fun deserialize(json: JSONValue?): BitSet? {
        return when (json) {
            null -> null
            is JSONArray -> {
                val bitSet = BitSet()
                for (i in json.indices) {
                    val item = json[i]
                    if (item is JSONNumber && item.isInt())
                        bitSet.set(item.toInt())
                    else
                        typeError("integer", i)
                }
                bitSet
            }
            else -> typeError("array of integer")
        }
    }
}

data object URIDeserializer : Deserializer<URI> {
    override fun deserialize(json: JSONValue?): URI? = when (json) {
        null -> null
        is JSONString -> URI(json.value)
        else -> UUIDDeserializer.typeError("string")
    }
}

data object URLDeserializer : Deserializer<URL> {
    override fun deserialize(json: JSONValue?): URL? = when (json) {
        null -> null
        is JSONString -> URL(json.value)
        else -> UUIDDeserializer.typeError("string")
    }
}
