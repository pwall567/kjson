/*
 * @(#) ObjectDeserializer.kt
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

import io.kjson.JSONDeserializer.deserializeFields
import io.kjson.JSONObject
import io.kjson.JSONValue

class ObjectDeserializer(
    val resultType: KType,
    val fieldDescriptors: List<FieldDescriptor<*>>,
    val allowExtra: Boolean,
    val instance: Any,
) : Deserializer<Any> {

    override fun deserialize(json: JSONValue?): Any {
        if (json !is JSONObject)
            typeError("object")
        deserializeFields(json, resultType, fieldDescriptors, allowExtra, instance)
        return instance
    }

}
