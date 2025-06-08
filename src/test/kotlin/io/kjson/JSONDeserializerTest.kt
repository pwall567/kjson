/*
 * @(#) JSONDeserializerTest.kt
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

import kotlin.reflect.full.createType
import kotlin.test.Test

import java.math.BigDecimal

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeEqual
import io.kstuff.test.shouldBeSameInstance
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldThrow

import io.kjson.pointer.JSONPointer
import io.kjson.testclasses.Const
import io.kjson.testclasses.Const2
import io.kjson.testclasses.Const3
import io.kjson.testclasses.CustomAllowExtraProperties
import io.kjson.testclasses.CustomIgnore
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.Dummy1a
import io.kjson.testclasses.Dummy5
import io.kjson.testclasses.DummyFromJSON
import io.kjson.testclasses.DummyFromJSONWithContext
import io.kjson.testclasses.DummyMultipleFromJSON
import io.kjson.testclasses.DummyValidated
import io.kjson.testclasses.DummyWithAllowExtra
import io.kjson.testclasses.DummyWithCustomAllowExtra
import io.kjson.testclasses.DummyWithCustomIgnore
import io.kjson.testclasses.DummyWithIgnore
import io.kjson.testclasses.DummyWithVal
import io.kjson.testclasses.Expr
import io.kjson.testclasses.Expr2
import io.kjson.testclasses.Expr3
import io.kjson.testclasses.MultiConstructor
import io.kjson.testclasses.NotANumber
import io.kjson.testclasses.Organization
import io.kjson.testclasses.Party
import io.kjson.testclasses.Person
import io.kjson.testclasses.SealedClassContainer

class JSONDeserializerTest {

    @Test fun `should return null from null input`() {
        JSONDeserializer.deserialize(String::class.createType(nullable = true), null) shouldBe null
    }

    @Test fun `should pass JSONValue through unchanged`() {
        val json = JSONDecimal("0.1")
        JSONDeserializer.deserialize(JSONValue::class,  json) shouldBeSameInstance json
        JSONDeserializer.deserialize(JSONDecimal::class,  json) shouldBeSameInstance json
        val json2 = JSONArray(JSONInt(123))
        JSONDeserializer.deserialize(JSONValue::class,  json2) shouldBeSameInstance json2
        JSONDeserializer.deserialize(JSONArray::class,  json2) shouldBeSameInstance json2
    }

    @Test fun `should use companion object fromJSON function`() {
        val json = JSONObject.build {
            add("dec", "17")
            add("hex", "11")
        }
        val expected = DummyFromJSON(17)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should use companion object fromJSON with JSONContext receiver`() {
        val config = JSONConfig {
            fromJSONObject { json ->
                val field1: String = deserializeProperty("a", json)
                val field2: Int = deserializeProperty("b", json)
                Dummy1(field1, field2)
            }
        }
        val inner = JSONObject.build {
            add("a", "Complex")
            add("b", 6789)
        }
        val json = JSONObject.build {
            add("aaa", inner)
        }
        val expected = DummyFromJSONWithContext(Dummy1("Complex", 6789))
        shouldBeEqual(expected, json.deserialize(config))
    }

    @Test fun `should report errors correctly when using companion object fromJSON`() {
        val json = JSONObject.build {
            add("aaa", JSONObject.build {
                add("field1", "abc")
                add("field2", "xyz")
            })
        }
        shouldThrow<JSONKotlinException>("Incorrect type, expected Int but was \"xyz\", at /aaa/field2") {
            JSONDeserializer.deserialize<DummyFromJSONWithContext>(json)
        }.let {
            it.pointer shouldBe JSONPointer("/aaa/field2")
        }
    }

    @Test fun `should select correct companion object fromJSON function`() {
        val json1 = JSONObject.build {
            add("dec", "17")
            add("hex", "11")
        }
        val expected1 = DummyMultipleFromJSON(17)
        shouldBeEqual(expected1, json1.deserialize())
        val json2 = JSONInt(300)
        val expected2 = DummyMultipleFromJSON(300)
        shouldBeEqual(expected2, json2.deserialize())
        val json3 = JSONString("FF")
        val expected3 = DummyMultipleFromJSON(255)
        shouldBeEqual(expected3, json3.deserialize())
    }

    @Test fun `should return null for nullable type from null`() {
        val json: JSONValue? = null
        JSONDeserializer.deserialize<Dummy1?>(json) shouldBe null
    }

    @Test fun `should return null for nullable String from null`() {
        val json: JSONValue? = null
        JSONDeserializer.deserialize<String?>(json) shouldBe null
    }

    @Test fun `should fail deserializing null for non-nullable String`() {
        shouldThrow<JSONKotlinException>("Can't deserialize null as String") {
            JSONDeserializer.deserialize<String>(null)
        }
    }

    @Test fun `should deserialize class with constant val correctly`() {
        val json = JSONObject.build { add("field8", "blert") }
        JSONDeserializer.deserialize<DummyWithVal>(json) shouldBe DummyWithVal()
    }

    @Test fun `should deserialize null as specified class correctly`() {
        JSONDeserializer.deserialize(Dummy1::class, null) shouldBe null
    }

    @Test fun `should deserialize JSONBoolean to Any`() {
        val json1 = JSONBoolean.TRUE
        val result1 = json1.deserializeAny()
        result1.shouldBeType<Boolean>() shouldBe true
        val json2 = JSONBoolean.FALSE
        val result2 = json2.deserializeAny()
        !result2.shouldBeType<Boolean>() shouldBe true
    }

    @Test fun `should deserialize sealed class to correct subclass`() {
        val json = JSONObject.build {
            add("class", "Const")
            add("number", BigDecimal("2.0"))
        }
        JSONDeserializer.deserialize<Expr>(json) shouldBe Const(2.0)
    }

    @Test fun `should deserialize sealed class to correct object subclass`() {
        val json = JSONObject.build {
            add("class", "NotANumber")
        }
        JSONDeserializer.deserialize<Expr>(json) shouldBe NotANumber
    }

    @Test fun `should deserialize sealed class with custom discriminator`() {
        val config = JSONConfig {
            sealedClassDiscriminator = "?"
        }
        val json = JSONObject.build {
            add("?", "Const")
            add("number", BigDecimal("2.0"))
        }
        JSONDeserializer.deserialize<Expr>(json, config) shouldBe Const(2.0)
    }

    @Test fun `should deserialize sealed class with class-specific discriminator`() {
        val json = JSONObject.build {
            add("type", "Const2")
            add("number", BigDecimal("2.0"))
        }
        JSONDeserializer.deserialize<Expr2>(json) shouldBe Const2(2.0)
    }

    @Test fun `should deserialize sealed class with class-specific discriminator and identifiers`() {
        val json = JSONObject.build {
            add("type", "CONST")
            add("number", BigDecimal("2.0"))
        }
        JSONDeserializer.deserialize<Expr3>(json) shouldBe Const3(2.0)
    }

    @Test fun `should deserialize sealed class with class-specific discriminator and identifier within class`() {
        val org = JSONObject.build {
            add("type", "ORGANIZATION")
            add("id", 123456)
            add("name", "Funny Company")
        }
        JSONDeserializer.deserialize<Party>(org) shouldBe Organization("ORGANIZATION", 123456, "Funny Company")
        val person = JSONObject.build {
            add("type", "PERSON")
            add("firstName", "William")
            add("lastName", "Wordsworth")
        }
        JSONDeserializer.deserialize<Party>(person) shouldBe Person("PERSON", "William", "Wordsworth")
    }

    @Test fun `should ignore additional fields when allowExtra set in config`() {
        val config = JSONConfig {
            allowExtra = true
        }
        val json = JSONObject.build {
            add("field1", "Hello")
            add("field2", 123)
            add("extra", "allow")
        }
        shouldBeEqual(Dummy1("Hello", 123), json.deserialize(config))
    }

    @Test fun `should ignore additional fields when class annotated with @JSONAllowExtra`() {
        val json = JSONObject.build {
            add("field1", "Hello")
            add("field2", 123)
            add("extra", "allow")
        }
        shouldBeEqual(DummyWithAllowExtra("Hello", 123), json.deserialize())
    }

    @Test fun `should ignore additional fields when class annotated with custom allow extra`() {
        val config = JSONConfig {
            addAllowExtraPropertiesAnnotation(CustomAllowExtraProperties::class)
        }
        val json = JSONObject.build {
            add("field1", "Hi")
            add("field2", 123)
            add("extra", "allow")
        }
        shouldBeEqual(DummyWithCustomAllowExtra("Hi", 123), json.deserialize(config))
    }

    @Test fun `field annotated with @JSONIgnore should be ignored on deserialization`() {
        val json = JSONObject.build {
            add("field1", "one")
            add("field2", "two")
            add("field3", "three")
        }
        shouldBeEqual(DummyWithIgnore(field1 = "one", field3 = "three"), json.deserialize())
    }

    @Test fun `field annotated with custom ignore annotation should be ignored on deserialization`() {
        val config = JSONConfig {
            addIgnoreAnnotation(CustomIgnore::class)
        }
        val json = JSONObject.build {
            add("field1", "one")
            add("field2", "two")
            add("field3", "three")
        }
        shouldBeEqual(DummyWithCustomIgnore(field1 = "one", field3 = "three"), json.deserialize(config))
    }

    @Test fun `should deserialize missing members as null where allowed`() {
        val json = JSONObject.of("field2" to JSONInt(123))
        shouldBeEqual(Dummy5(null, 123), json.deserialize())
    }

    @Test fun `should deserialize custom parameterised type`() {
        val json = JSONObject.build {
            add("lines", JSONArray.of(JSONString("abc"), JSONString("def")))
        }
        val expected = TestPage(lines = listOf("abc", "def"))
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should deserialize nested custom parameterised type`() {
        val json1 = JSONObject.build {
            add("lines", JSONArray.of(JSONString("abc"), JSONString("def")))
        }
        val json2 = JSONArray.of(json1, JSONString("xyz"))
        val expected = TestPage(lines = listOf("abc", "def")) to "xyz"
        shouldBeEqual(expected, json2.deserialize())
    }

    @Test fun `should deserialize differently nested custom parameterised type`() {
        val json = JSONObject.build {
            add("lines", JSONArray.of(JSONArray.of(JSONString("abc"), JSONString("ABC")),
                JSONArray.of(JSONString("def"), JSONString("DEF"))))
        }
        val expected = TestPage(lines = listOf("abc" to "ABC", "def" to "DEF"))
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should deserialize complex custom parameterised type`() {
        val obj1 = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
        }
        val obj2 = JSONObject.build {
            add("field1", "def")
            add("field2", 456)
        }
        val json = JSONObject.build {
            add("lines", JSONArray.of(obj1, obj2))
        }
        val expected = TestPage(lines = listOf(Dummy1("abc", 123), Dummy1("def", 456)))
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should deserialize another form of custom parameterised type`() {
        val obj1 = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
        }
        val json = JSONObject.build {
            add("description", "testing")
            add("data", obj1)
        }
        val dummy1 = Dummy1("abc", 123)
        val expected = TestDataHolder("testing", dummy1)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should deserialize yet another form of custom parameterised type`() {
        val json = JSONObject.build {
            add("lineLists", JSONArray.of(JSONArray.of(JSONString("lineA1"), JSONString("lineA2")),
                JSONArray.of(JSONString("lineB1"), JSONString("lineB2"))))
        }
        val expected = TestPage2(lineLists = listOf(listOf("lineA1", "lineA2"), listOf("lineB1", "lineB2")))
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should give error message with pointer`() {
        val json = JSON.parse("""{"field1":"abc","field2":"def"}""")
        shouldThrow<JSONKotlinException>("Incorrect type, expected Int but was \"def\", at /field2") {
            JSONDeserializer.deserialize<Dummy1>(json)
        }
        shouldThrow<JSONKotlinException>("Incorrect type, expected Int but was \"def\", at /0/field2") {
            JSONDeserializer.deserialize<List<Dummy1>>(JSONArray.of(json))
        }
    }

    @Test fun `should give expanded error message with pointer`() {
        val json = JSON.parse("""{"field2":1}""")
        val className = Dummy1a::class.qualifiedName
        shouldThrow<JSONKotlinException>("Can't create $className - missing property field1") {
            JSONDeserializer.deserialize<Dummy1a>(json)
        }
        shouldThrow<JSONKotlinException>("Can't create $className - missing property field1, at /0") {
            JSONDeserializer.deserialize<List<Dummy1a>>(JSONArray.of(json))
        }
    }

    @Test fun `should give expanded error message for multiple constructors`() {
        val json = JSON.parse("""[{"aaa":"X"},{"bbb":1},{"ccc":true,"ddd":0}]""")
        val className = MultiConstructor::class.qualifiedName
        shouldThrow<JSONKotlinException>("No matching constructor for class $className from { ccc, ddd }, at /2") {
            JSONDeserializer.deserialize<List<MultiConstructor>>(json)
        }
    }

    @Test fun `should use type projection upperBounds`() {
        val json = JSON.parse("""{"expr":{"class":"Const","number":20.0}}""")
        val expr = JSONDeserializer.deserialize<SealedClassContainer<*>>(json).expr
        expr.shouldBeType<Const>()
        expr.number shouldBe 20.0
    }

    @Test fun `should fail on use of private constructor`() {
        val json = JSON.parse("""{"xxx":123}""")
        val className = TestPrivate::class.qualifiedName
        shouldThrow<JSONKotlinException>("Can't deserialize { xxx } as $className") {
            JSONDeserializer.deserialize<TestPrivate>(json)
        }
    }

    @Test fun `should fail on attempt to deserialize impossible class`() {
        val json = JSON.parse("""{"xxx":123}""")
        shouldThrow<JSONKotlinException>("Can't deserialize Unit") {
            JSONDeserializer.deserialize<Unit>(json)
        }
    }

    @Test fun `should give helpful error message on deserialization error`() {
        val json = JSON.parse("""[{"field1":""}]""")
        shouldThrow<JSONKotlinException>(
            message = "Error deserializing io.kjson.testclasses.DummyValidated - field1 must not be empty, at /0",
        ) {
            JSONDeserializer.deserialize<List<DummyValidated>>(json)
        }.let {
            it.pointer shouldBe JSONPointer("/0")
            it.cause.shouldBeType<IllegalArgumentException>()
            it.cause?.message shouldBe "field1 must not be empty"
        }
    }

    class TestPrivate private constructor(xxx: Int) {
        @Suppress("unused")
        val yyy: Int = xxx
    }

    data class TestPage<T>(val header: String? = null, val lines: List<T>)

    data class TestDataHolder<T>(val description: String, val data: T)

    data class TestPage2<T>(val header: String? = null, val lineLists: List<List<T>>)

}
