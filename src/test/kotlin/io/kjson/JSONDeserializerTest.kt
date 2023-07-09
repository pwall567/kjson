/*
 * @(#) JSONDeserializerTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023 Peter Wall
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.expect

import java.math.BigDecimal
import java.lang.reflect.Type

import io.kjson.Constants.stringType
import io.kjson.JSON.asInt
import io.kjson.JSON.asString
import io.kjson.testclasses.Const
import io.kjson.testclasses.Const2
import io.kjson.testclasses.Const3
import io.kjson.testclasses.CustomAllowExtraProperties
import io.kjson.testclasses.CustomIgnore
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.Dummy5
import io.kjson.testclasses.DummyFromJSON
import io.kjson.testclasses.DummyFromJSONWithConfig
import io.kjson.testclasses.DummyMultipleFromJSON
import io.kjson.testclasses.DummyWithAllowExtra
import io.kjson.testclasses.DummyWithCustomAllowExtra
import io.kjson.testclasses.DummyWithCustomIgnore
import io.kjson.testclasses.DummyWithIgnore
import io.kjson.testclasses.DummyWithVal
import io.kjson.testclasses.Expr
import io.kjson.testclasses.Expr2
import io.kjson.testclasses.Expr3
import io.kjson.testclasses.JavaClass1
import io.kjson.testclasses.JavaClass2
import io.kjson.testclasses.MultiConstructor
import io.kjson.testclasses.NotANumber
import io.kjson.testclasses.Organization
import io.kjson.testclasses.Party
import io.kjson.testclasses.Person
import io.kjson.testclasses.SealedClassContainer

class JSONDeserializerTest {

    @Test fun `should return null from null input`() {
        assertNull(JSONDeserializer.deserialize(String::class.createType(nullable = true), null))
    }

    @Test fun `should pass JSONValue through unchanged`() {
        val json = JSONDecimal("0.1")
        assertSame(json, JSONDeserializer.deserialize(JSONValue::class,  json))
        assertSame(json, JSONDeserializer.deserialize(JSONDecimal::class,  json))
        val json2 = JSONString("abc")
        assertSame(json2, JSONDeserializer.deserialize(JSONValue::class,  json2))
        assertSame(json2, JSONDeserializer.deserialize(JSONString::class,  json2))
    }

    @Test fun `should use companion object fromJSON function`() {
        val json = JSONObject.build {
            add("dec", "17")
            add("hex", "11")
        }
        val expected = DummyFromJSON(17)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should use companion object fromJSON with JSONConfig receiver`() {
        val config = JSONConfig {
            fromJSONObject { json ->
                val field1 = json["a"].asString
                val field2 = json["b"].asInt
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
        val expected = DummyFromJSONWithConfig(Dummy1("Complex", 6789))
        expect(expected) { JSONDeserializer.deserialize(json, config) }
    }

    @Test fun `should select correct companion object fromJSON function`() {
        val json1 = JSONObject.build {
            add("dec", "17")
            add("hex", "11")
        }
        val expected1 = DummyMultipleFromJSON(17)
        expect(expected1) { JSONDeserializer.deserialize(json1) }
        val json2 = JSONInt(300)
        val expected2 = DummyMultipleFromJSON(300)
        expect(expected2) { JSONDeserializer.deserialize(json2) }
        val json3 = JSONString("FF")
        val expected3 = DummyMultipleFromJSON(255)
        expect(expected3) { JSONDeserializer.deserialize(json3) }
    }

    @Test fun `should return null for nullable type from null`() {
        val json: JSONValue? = null
        assertNull(JSONDeserializer.deserialize<Dummy1?>(json))
    }

    @Test fun `should return null for nullable String from null`() {
        val json: JSONValue? = null
        assertNull(JSONDeserializer.deserialize(String::class.createType(emptyList(), true), json))
    }

    @Test fun `should fail deserializing null for non-nullable String`() {
        assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize(stringType, null) }.let {
            expect("Can't deserialize null as String") { it.message }
        }
    }

    @Test fun `should deserialize class with constant val correctly`() {
        val json = JSONObject.build { add("field8", "blert") }
        expect(DummyWithVal()) { JSONDeserializer.deserialize(DummyWithVal::class, json) }
    }

    @Test fun `should deserialize null as specified class correctly`() {
        assertNull(JSONDeserializer.deserialize(Dummy1::class, null))
    }

    @Test fun `should deserialize java class correctly`() {
        val json = JSONObject.build {
            add("field1", 1234)
            add("field2", "Hello!")
        }
        expect(JavaClass1(1234, "Hello!")) { JSONDeserializer.deserialize(JavaClass1::class, json) }
    }

    @Test fun `should deserialize object using Java Class correctly`() {
        val json = JSONObject.build {
            add("field1", 567)
            add("field2", "abcdef")
        }
        expect(JavaClass1(567, "abcdef")) { JSONDeserializer.deserialize(JavaClass1::class.java, json) }
    }

    @Test fun `should deserialize null using Java Class correctly`() {
        assertNull(JSONDeserializer.deserialize(JavaClass1::class.java, null))
    }

    @Test fun `should deserialize List using Java Type correctly`() {
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
        expect(listOf(JavaClass1(567, "abcdef"), JavaClass1(9999, "qwerty"))) {
            JSONDeserializer.deserialize(type, json)
        }
    }

    @Test fun `should deserialize null using Java Type correctly`() {
        val type: Type = JavaClass2::class.java.getField("field1").genericType
        assertNull(JSONDeserializer.deserialize(type, null))
    }

    @Test fun `should deserialize JSONBoolean to Any`() {
        val json1 = JSONBoolean.TRUE
        val result1 = JSONDeserializer.deserializeAny(json1)
        assertTrue(result1 is Boolean && result1)
        val json2 = JSONBoolean.FALSE
        val result2 = JSONDeserializer.deserializeAny(json2)
        assertTrue(result2 is Boolean && !result2)
    }

    @Test fun `should deserialize sealed class to correct subclass`() {
        val json = JSONObject.build {
            add("class", "Const")
            add("number", BigDecimal("2.0"))
        }
        expect(Const(2.0)) { JSONDeserializer.deserialize<Expr>(json) }
    }

    @Test fun `should deserialize sealed class to correct object subclass`() {
        val json = JSONObject.build {
            add("class", "NotANumber")
        }
        expect(NotANumber) { JSONDeserializer.deserialize<Expr>(json) }
    }

    @Test fun `should deserialize sealed class with custom discriminator`() {
        val config = JSONConfig {
            sealedClassDiscriminator = "?"
        }
        val json = JSONObject.build {
            add("?", "Const")
            add("number", BigDecimal("2.0"))
        }
        expect(Const(2.0)) { JSONDeserializer.deserialize<Expr>(json, config) }
    }

    @Test fun `should deserialize sealed class with class-specific discriminator`() {
        val json = JSONObject.build {
            add("type", "Const2")
            add("number", BigDecimal("2.0"))
        }
        expect(Const2(2.0)) { JSONDeserializer.deserialize<Expr2>(json) }
    }

    @Test fun `should deserialize sealed class with class-specific discriminator and identifiers`() {
        val json = JSONObject.build {
            add("type", "CONST")
            add("number", BigDecimal("2.0"))
        }
        expect(Const3(2.0)) { JSONDeserializer.deserialize<Expr3>(json) }
    }

    @Test fun `should deserialize sealed class with class-specific discriminator and identifier within class`() {
        val org = JSONObject.build {
            add("type", "ORGANIZATION")
            add("id", 123456)
            add("name", "Funny Company")
        }
        expect(Organization("ORGANIZATION", 123456, "Funny Company")) { JSONDeserializer.deserialize<Party>(org) }
        val person = JSONObject.build {
            add("type", "PERSON")
            add("firstName", "William")
            add("lastName", "Wordsworth")
        }
        expect(Person("PERSON", "William", "Wordsworth")) { JSONDeserializer.deserialize<Party>(person) }
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
        expect(Dummy1("Hello", 123)) { JSONDeserializer.deserialize(json, config) }
    }

    @Test fun `should ignore additional fields when class annotated with @JSONAllowExtra`() {
        val json = JSONObject.build {
            add("field1", "Hello")
            add("field2", 123)
            add("extra", "allow")
        }
        expect(DummyWithAllowExtra("Hello", 123)) { JSONDeserializer.deserialize(json) }
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
        expect(DummyWithCustomAllowExtra("Hi", 123)) { JSONDeserializer.deserialize(json, config) }
    }

    @Test fun `field annotated with @JSONIgnore should be ignored on deserialization`() {
        val json = JSONObject.build {
            add("field1", "one")
            add("field2", "two")
            add("field3", "three")
        }
        expect(DummyWithIgnore(field1 = "one", field3 = "three")) { JSONDeserializer.deserialize(json) }
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
        expect(DummyWithCustomIgnore(field1 = "one", field3 = "three")) { JSONDeserializer.deserialize(json, config) }
    }

    @Test fun `should deserialize missing members as null where allowed`() {
        val json = JSONObject.of("field2" to JSONInt(123))
        expect(Dummy5(null, 123)) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should deserialize custom parameterised type`() {
        val json = JSONObject.build {
            add("lines", JSONArray.of(JSONString("abc"), JSONString("def")))
        }
        val expected = TestPage(lines = listOf("abc", "def"))
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should deserialize nested custom parameterised type`() {
        val json1 = JSONObject.build {
            add("lines", JSONArray.of(JSONString("abc"), JSONString("def")))
        }
        val json2 = JSONArray.of(json1, JSONString("xyz"))
        val expected = TestPage(lines = listOf("abc", "def")) to "xyz"
        expect(expected) { JSONDeserializer.deserialize(json2) }
    }

    @Test fun `should deserialize differently nested custom parameterised type`() {
        val json = JSONObject.build {
            add("lines", JSONArray.of(JSONArray.of(JSONString("abc"), JSONString("ABC")),
                    JSONArray.of(JSONString("def"), JSONString("DEF"))))
        }
        val expected = TestPage(lines = listOf("abc" to "ABC", "def" to "DEF"))
        expect(expected) { JSONDeserializer.deserialize(json) }
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
        expect(expected) { JSONDeserializer.deserialize(json) }
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
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should deserialize yet another form of custom parameterised type`() {
        val json = JSONObject.build {
            add("lineLists", JSONArray.of(JSONArray.of(JSONString("lineA1"), JSONString("lineA2")),
                    JSONArray.of(JSONString("lineB1"), JSONString("lineB2"))))
        }
        val expected = TestPage2(lineLists = listOf(listOf("lineA1", "lineA2"), listOf("lineB1", "lineB2")))
        expect(expected) { JSONDeserializer.deserialize(json)}
    }

    @Test fun `should give error message with pointer`() {
        val json = JSON.parse("""{"field1":"abc","field2":"def"}""")
        assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize<Dummy1>(json) }.let {
            expect("Can't deserialize \"def\" as Int at /field2") { it.message }
        }
        assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize<List<Dummy1>>(JSONArray.of(json)) }.let {
            expect("Can't deserialize \"def\" as Int at /0/field2") { it.message }
        }
    }

    @Test fun `should give expanded error message with pointer`() {
        val json = JSON.parse("""{"field2":1}""")
        val className = Dummy1::class.qualifiedName
        assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize<Dummy1>(json) }.let {
            expect("Can't create $className; missing: field1") { it.message }
        }
        assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize<List<Dummy1>>(JSONArray.of(json)) }.let {
            expect("Can't create $className; missing: field1 at /0") { it.message }
        }
    }

    @Test fun `should give expanded error message for multiple constructors`() {
        val json = JSON.parse("""[{"aaa":"X"},{"bbb":1},{"ccc":true,"ddd":0}]""")
        assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize<List<MultiConstructor>>(json) }.let {
            val className = MultiConstructor::class.qualifiedName
            expect("Can't locate constructor for $className; properties: ccc, ddd at /2") { it.message }
        }
    }

    @Test fun `should use type projection upperBounds`() {
        val json = JSON.parse("""{"expr":{"class":"Const","number":20.0}}""")
        val expr = JSONDeserializer.deserialize<SealedClassContainer<*>>(json).expr
        assertTrue(expr is Const)
        expect(20.0) { expr.number }
    }

    data class TestPage<T>(val header: String? = null, val lines: List<T>)

    data class TestDataHolder<T>(val description: String, val data: T)

    data class TestPage2<T>(val header: String? = null, val lineLists: List<List<T>>)

}
