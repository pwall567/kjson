/*
 * @(#) JSONFunTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021 Peter Wall
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

import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.expect

import java.lang.reflect.Type

import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.JavaClass1
import io.kjson.testclasses.JavaClass2

class JSONFunTest {

    @Test fun `should correctly parse string`() {
        val json = """{"field1":"Hi there!","field2":888}"""
        val expected = Dummy1("Hi there!", 888)
        expect(expected) { json.parseJSON() }
    }

    @Test fun `should correctly parse string using parameterised type`() {
        val json = """{"field1":"Hi there!","field2":888}"""
        val actual = json.parseJSON<Dummy1>()
        expect(Dummy1("Hi there!", 888)) { actual }
    }

    @Test fun `should correctly parse string using explicit KClass`() {
        val json = """{"field1":"Hi there!","field2":888}"""
        expect(Dummy1("Hi there!", 888)) { json.parseJSON(Dummy1::class) }
    }

    @Test fun `should correctly parse string using explicit KType`() {
        val json = """{"field1":"Hi there!","field2":888}"""
        expect(Dummy1("Hi there!", 888)) { json.parseJSON(Dummy1::class.starProjectedType) }
    }

    @Test fun `should stringify any object`() {
        val dummy1 = Dummy1("Hi there!", 888)
        val expected = JSONObject.build {
            add("field1", "Hi there!")
            add("field2", 888)
        }
        expect(expected) { JSON.parse(dummy1.stringifyJSON()) }
    }

    @Test fun `should stringify null`() {
        val dummy1 = null
        expect("null") { dummy1.stringifyJSON() }
    }

    @Test fun `targetKType should create correct type`() {
        val listStrings = listOf("abc", "def")
        val jsonArrayString = JSONArray.build {
            add("abc")
            add("def")
        }
        expect(listStrings) { JSONDeserializer.deserialize(targetKType(List::class, String::class), jsonArrayString) }
    }

    @Test fun `targetKType should create correct complex type`() {
        val listStrings = listOf(listOf("abc", "def"))
        val jsonArrayArrayString = JSONArray.build {
            add(JSONArray.build {
                add("abc")
                add("def")
            })
        }
        expect(listStrings) {
            JSONDeserializer.deserialize(targetKType(List::class, targetKType(List::class, String::class)),
                jsonArrayArrayString)
        }
    }

    @Test fun `toKType should convert simple class`() {
        val type: Type = JavaClass1::class.java
        expect(JavaClass1::class.starProjectedType) { type.toKType() }
    }

    @Test fun `toKType should convert parameterized class`() {
        val field = JavaClass2::class.java.getField("field1")
        val type: Type = field.genericType
        val expected = java.util.List::class.createType(
            listOf(KTypeProjection.invariant(JavaClass1::class.createType(nullable = true))))
        expect(expected) { type.toKType() }
    }

    @Test fun `toKType should convert parameterized class with extends`() {
        val field = JavaClass2::class.java.getField("field2")
        val type: Type = field.genericType
        val expected = java.util.List::class.createType(
            listOf(KTypeProjection.covariant(JavaClass1::class.createType(nullable = true))))
        expect(expected) { type.toKType() }
    }

    @Test fun `toKType should convert parameterized class with super`() {
        val field = JavaClass2::class.java.getField("field3")
        val type: Type = field.genericType
        val expected = java.util.List::class.createType(
            listOf(KTypeProjection.contravariant(JavaClass1::class.createType(nullable = true))))
        expect(expected) { type.toKType() }
    }

    @Test fun `toKType should convert nested parameterized class`() {
        val field = JavaClass2::class.java.getField("field4")
        val type: Type = field.genericType
        val expected = java.util.List::class.createType(
            listOf(KTypeProjection.invariant(java.util.List::class.createType(
                listOf(KTypeProjection.invariant(JavaClass1::class.createType(nullable = true))),
                nullable = true))))
        expect(expected) { type.toKType() }
    }

    @Test fun `toKType should convert nested parameterized class with extends`() {
        val field = JavaClass2::class.java.getField("field5")
        val type: Type = field.genericType
        val expected = java.util.List::class.createType(
            listOf(KTypeProjection.invariant(java.util.List::class.createType(
                listOf(KTypeProjection.covariant(JavaClass1::class.createType(nullable = true))),
                nullable = true))))
        expect(expected) { type.toKType() }
    }

    @Test fun `decode should convert a JSONObject to a specified type`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.deserialize(Dummy1::class.starProjectedType) }
    }

    @Test fun `decode should convert a JSONObject to a specified class`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.deserialize(Dummy1::class) }
    }

    @Test fun `decode should convert a JSONObject to an implied class`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.deserialize() }
    }

    @Test fun `decode should convert a JSONObject to a specified Java class`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.deserialize(Dummy1::class.java) }
    }

    @Test fun `should convert a JSONObject to a specified type using fromJSONValueNullable`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.fromJSONValueNullable(Dummy1::class.starProjectedType) }
        val nullValue: JSONValue? = null
        assertNull(nullValue.fromJSONValueNullable(Dummy1::class.createType(nullable = true)))
    }

    @Test fun `should convert a JSONObject to a specified class using fromJSONValueNullable`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.fromJSONValueNullable(Dummy1::class) }
        val nullValue: JSONValue? = null
        assertNull(nullValue.fromJSONValueNullable(Dummy1::class))
    }

    @Test fun `should convert a JSONObject to an implied class using fromJSONValueNullable`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.fromJSONValueNullable() }
        val nullValue: JSONValue? = null
        assertNull(nullValue.fromJSONValueNullable<Dummy1?>())
    }

    @Test fun `should convert a JSONObject to a specified Java class using fromJSONValueNullable`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.fromJSONValueNullable(Dummy1::class.java) }
        val nullValue: JSONValue? = null
        assertNull(nullValue.fromJSONValueNullable(Dummy1::class.java))
    }

    @Test fun `should convert a JSONObject to a specified type using fromJSONValue`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.fromJSONValue(Dummy1::class.starProjectedType) }
    }

    @Test fun `should convert a JSONObject to a specified class using fromJSONValue`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.fromJSONValue(Dummy1::class) }
    }

    @Test fun `should convert a JSONObject to an implied class using fromJSONValue`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.fromJSONValue() }
    }

    @Test fun `should convert a JSONObject to a specified Java class using fromJSONValue`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        expect(expected) { json.fromJSONValue(Dummy1::class.java) }
    }

}
