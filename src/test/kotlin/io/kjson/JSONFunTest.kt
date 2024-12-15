/*
 * @(#) JSONFunTest.kt
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

import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.test.Test

import java.io.File
import java.lang.reflect.Type

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeEqual
import io.kstuff.test.shouldThrow

import io.kjson.parser.ParseOptions
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.JavaClass1
import io.kjson.testclasses.JavaClass2

class JSONFunTest {

    @Test fun `should correctly parse string`() {
        val json = """{"field1":"Hi there!","field2":888}"""
        val expected = Dummy1("Hi there!", 888)
        shouldBeEqual(expected, json.parseJSON())
    }

    @Test fun `should parse null into nullable type`() {
        val string: String? = "null".parseJSON()
        string shouldBe null
    }

    @Test fun `should throw exception parsing null into non-nullable type`() {
        shouldThrow<JSONKotlinException>("Can't deserialize null as String") {
            "null".parseJSON<String>()
        }
    }

    @Test fun `should correctly parse string using parameterised type`() {
        val json = """{"field1":"Hi there!","field2":888}"""
        val actual = json.parseJSON<Dummy1>()
        actual shouldBe Dummy1("Hi there!", 888)
    }

    @Test fun `should correctly parse string using explicit KClass`() {
        val json = """{"field1":"Hi there!","field2":888}"""
        json.parseJSON(Dummy1::class) shouldBe Dummy1("Hi there!", 888)
    }

    @Test fun `should correctly parse null using explicit KClass`() {
        "null".parseJSON(String::class) shouldBe null
    }

    @Test fun `should correctly parse string using explicit KType`() {
        val json = """{"field1":"Hi there!","field2":888}"""
        json.parseJSON(Dummy1::class.starProjectedType) shouldBe Dummy1("Hi there!", 888)
    }

    @Test fun `should throw exception parsing null into non-nullable explicit KType`() {
        shouldThrow<JSONKotlinException>("Can't deserialize null as String") {
            "null".parseJSON(String::class.starProjectedType)
        }
    }

    @Test fun `should correctly parse from Reader`() {
        val reader = File("src/test/resources/testdata.json").reader()
        val expected = Dummy1("File test", 123)
        shouldBeEqual(expected, reader.parseJSON())
    }

    @Test fun `should correctly parse from Reader using parameterised type`() {
        val reader = File("src/test/resources/testdata.json").reader()
        val actual = reader.parseJSON<Dummy1>()
        actual shouldBe Dummy1("File test", 123)
    }

    @Test fun `should correctly parse from Reader using explicit KClass`() {
        val reader = File("src/test/resources/testdata.json").reader()
        reader.parseJSON(Dummy1::class) shouldBe Dummy1("File test", 123)
    }

    @Test fun `should correctly parse from Reader using explicit KType`() {
        val reader = File("src/test/resources/testdata.json").reader()
        reader.parseJSON(Dummy1::class.starProjectedType) shouldBe Dummy1("File test", 123)
    }

    @Test fun `should stringify any object`() {
        val dummy1 = Dummy1("Hi there!", 888)
        val expected = JSONObject.build {
            add("field1", "Hi there!")
            add("field2", 888)
        }
        JSON.parse(dummy1.stringifyJSON()) shouldBe expected
    }

    @Test fun `should stringify null`() {
        val dummy1: Dummy1? = null
        dummy1.stringifyJSON() shouldBe "null"
    }

    @Test fun `targetKType should create correct type`() {
        val listStrings = listOf("abc", "def")
        val jsonArrayString = JSONArray.build {
            add("abc")
            add("def")
        }
        JSONDeserializer.deserialize(targetKType(List::class, String::class), jsonArrayString) shouldBe listStrings
    }

    @Test fun `targetKType should create correct complex type`() {
        val listStrings = listOf(listOf("abc", "def"))
        val jsonArrayArrayString = JSONArray.build {
            add(JSONArray.build {
                add("abc")
                add("def")
            })
        }
        val targetType = targetKType(List::class, targetKType(List::class, String::class))
        JSONDeserializer.deserialize(targetType, jsonArrayArrayString) shouldBe listStrings
    }

    @Test fun `toKType should convert simple class`() {
        val type: Type = JavaClass1::class.java
        type.toKType() shouldBe JavaClass1::class.starProjectedType
    }

    @Test fun `toKType should convert parameterized class`() {
        val field = JavaClass2::class.java.getField("field1")
        val type: Type = field.genericType
        val expected = java.util.List::class.createType(
            listOf(KTypeProjection.invariant(JavaClass1::class.createType(nullable = true))))
        type.toKType() shouldBe expected
    }

    @Test fun `toKType should convert parameterized class with extends`() {
        val field = JavaClass2::class.java.getField("field2")
        val type: Type = field.genericType
        val expected = java.util.List::class.createType(
            listOf(KTypeProjection.covariant(JavaClass1::class.createType(nullable = true))))
        type.toKType() shouldBe expected
    }

    @Test fun `toKType should convert parameterized class with super`() {
        val field = JavaClass2::class.java.getField("field3")
        val type: Type = field.genericType
        val expected = java.util.List::class.createType(
            listOf(KTypeProjection.contravariant(JavaClass1::class.createType(nullable = true))))
        type.toKType() shouldBe expected
    }

    @Test fun `toKType should convert nested parameterized class`() {
        val field = JavaClass2::class.java.getField("field4")
        val type: Type = field.genericType
        val expected = java.util.List::class.createType(
            listOf(KTypeProjection.invariant(java.util.List::class.createType(
                listOf(KTypeProjection.invariant(JavaClass1::class.createType(nullable = true))),
                nullable = true))))
        type.toKType() shouldBe expected
    }

    @Test fun `toKType should convert nested parameterized class with extends`() {
        val field = JavaClass2::class.java.getField("field5")
        val type: Type = field.genericType
        val expected = java.util.List::class.createType(
            listOf(KTypeProjection.invariant(java.util.List::class.createType(
                listOf(KTypeProjection.covariant(JavaClass1::class.createType(nullable = true))),
                nullable = true))))
        type.toKType() shouldBe expected
    }

    @Test fun `decode should convert a JSONObject to a specified type`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        json.deserialize(Dummy1::class.starProjectedType) shouldBe expected
    }

    @Test fun `decode should convert a JSONObject to a specified class`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        json.deserialize(Dummy1::class) shouldBe expected
    }

    @Test fun `decode should convert a JSONObject to an implied class`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `decode should convert a JSONObject to a specified Java class`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        json.deserialize(Dummy1::class.java) shouldBe expected
    }

    @Test fun `should convert a JSONArray to a specified Java type`() {
        val json = JSONArray.build {
            add(JSONObject.build {
                add("field1", 567)
                add("field2", "abcdef")
            })
            add(JSONObject.build {
                add("field1", 9999)
                add("field2", "qwerty")
            })
        }
        val type: Type = JavaClass2::class.java.getField("field1").genericType
        shouldBeEqual(listOf(JavaClass1(567, "abcdef"), JavaClass1(9999, "qwerty")), json.deserialize(type))
    }

    @Test fun `should convert a JSONObject to a specified type using fromJSONValue`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        json.fromJSONValue(Dummy1::class.starProjectedType) shouldBe expected
    }

    @Test fun `should convert a JSONObject to a specified class using fromJSONValue`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        json.fromJSONValue(Dummy1::class) shouldBe expected
    }

    @Test fun `should convert a JSONObject to an implied class using fromJSONValue`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        shouldBeEqual(expected, json.fromJSONValue())
    }

    @Test fun `should convert a JSONObject to a specified Java class using fromJSONValue`() {
        val json = JSONObject.build {
            add("field1", "abdef")
            add("field2", 54321)
        }
        val expected = Dummy1("abdef", 54321)
        json.fromJSONValue(Dummy1::class.java) shouldBe expected
    }

    @Test fun `should convert a JSONArray to a specified Java type using fromJSONValue`() {
        val json = JSONArray.build {
            add(JSONObject.build {
                add("field1", 567)
                add("field2", "abcdef")
            })
            add(JSONObject.build {
                add("field1", 9999)
                add("field2", "qwerty")
            })
        }
        val type: Type = JavaClass2::class.java.getField("field1").genericType
        shouldBeEqual(listOf(JavaClass1(567, "abcdef"), JavaClass1(9999, "qwerty")), json.fromJSONValue(type))
    }

    @Test fun `should use lenient parsing options if provided`() {
        val config = JSONConfig {
            parseOptions = ParseOptions(JSONObject.DuplicateKeyOption.CHECK_IDENTICAL)
        }
        val expected = Dummy1("abc", 987)
        shouldBeEqual(expected, """{"field1":"abc","field2":987,"field1":"abc"}""".parseJSON(config))
    }

}
