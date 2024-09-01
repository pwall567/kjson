/*
 * @(#) JSONValueDeserializers.kt
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

import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONLong
import io.kjson.JSONNumber
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.JSONValue

data object JSONStringDeserializer : Deserializer<JSONString> {
    override fun deserialize(json: JSONValue?): JSONString? = when (json) {
        null -> null
        is JSONString -> json
        else -> typeError("JSONString")
    }
}

data object JSONIntDeserializer : Deserializer<JSONInt> {
    override fun deserialize(json: JSONValue?): JSONInt? = when {
        json == null -> null
        json is JSONInt -> json
        json is JSONNumber && json.isInt() -> JSONInt(json.toInt())
        else -> typeError("JSONInt")
    }
}

data object JSONLongDeserializer : Deserializer<JSONLong> {
    override fun deserialize(json: JSONValue?): JSONLong? = when {
        json == null -> null
        json is JSONLong -> json
        json is JSONNumber && json.isLong() -> JSONLong(json.toLong())
        else -> typeError("JSONLong")
    }
}

data object JSONDecimalDeserializer : Deserializer<JSONDecimal> {
    override fun deserialize(json: JSONValue?): JSONDecimal? = when (json) {
        null -> null
        is JSONDecimal -> json
        is JSONNumber -> JSONDecimal(json.toDecimal())
        else -> typeError("JSONDecimal")
    }
}

data object JSONNumberDeserializer : Deserializer<JSONNumber> {
    override fun deserialize(json: JSONValue?): JSONNumber? = when (json) {
        null -> null
        is JSONNumber -> json
        else -> typeError("JSONNumber")
    }
}

data object JSONBooleanDeserializer : Deserializer<JSONBoolean> {
    override fun deserialize(json: JSONValue?): JSONBoolean? = when (json) {
        null -> null
        is JSONBoolean -> json
        else -> typeError("JSONBoolean")
    }
}

data object JSONArrayDeserializer : Deserializer<JSONArray> {
    override fun deserialize(json: JSONValue?): JSONArray? = when (json) {
        null -> null
        is JSONArray -> json
        else -> typeError("JSONArray")
    }
}

data object JSONObjectDeserializer : Deserializer<JSONObject> {
    override fun deserialize(json: JSONValue?): JSONObject? = when (json) {
        null -> null
        is JSONObject -> json
        else -> typeError("JSONObject")
    }
}

data object JSONValueDeserializer : Deserializer<JSONValue> {
    override fun deserialize(json: JSONValue?): JSONValue? = json
}
