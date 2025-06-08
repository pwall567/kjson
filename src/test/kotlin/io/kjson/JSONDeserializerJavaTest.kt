/*
 * @(#) JSONDeserializerJavaTest.kt
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

package io.kjson

import kotlin.test.Test

import java.lang.reflect.Type

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeEqual
import io.kstuff.test.shouldThrow

import io.kjson.testclasses.CustomIgnore
import io.kjson.testclasses.CustomName
import io.kjson.testclasses.JavaClass1
import io.kjson.testclasses.JavaClass2
import io.kjson.testclasses.JavaMultiConstructor
import io.kjson.testclasses.JavaNamedArg
import io.kjson.testclasses.JavaNamedArg1
import io.kjson.testclasses.JavaNamedField
import io.kjson.testclasses.JavaSingleArg

class JSONDeserializerJavaTest {

    @Test fun `should deserialize java class correctly`() {
        val json = JSONObject.build {
            add("field1", 1234)
            add("field2", "Hello!")
        }
        shouldBeEqual(JavaClass1(1234, "Hello!"), JSONDeserializer.deserialize(json))
    }

    @Test fun `should deserialize object using Java Class correctly`() {
        val json = JSONObject.build {
            add("field1", 567)
            add("field2", "abcdef")
        }
        JSONDeserializer.deserialize(JavaClass1::class.java, json) shouldBe JavaClass1(567, "abcdef")
    }

    @Test fun `should deserialize null using Java Class correctly`() {
        JSONDeserializer.deserialize(JavaClass1::class.java, null) shouldBe null
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
        JSONDeserializer.deserialize(type, json) shouldBe listOf(JavaClass1(567, "abcdef"), JavaClass1(9999, "qwerty"))
    }

    @Test fun `should deserialize null using Java Type correctly`() {
        val type: Type = JavaClass2::class.java.getField("field1").genericType
        JSONDeserializer.deserialize(type, null) shouldBe null
    }

    @Test fun `should deserialize Java class with single-arg constructor`() {
        val json = JSONString("nothing")
        json.deserialize<JavaSingleArg>() shouldBe JavaSingleArg("nothing")
    }

    @Test fun `should deserialize into class containing Java class with single-arg constructor`() {
        val json = JSONObject.build {
            add("name", "Fred")
            add("jsa", "anything")
        }
        with(json.deserialize<ClassWithJavaSingleArg>()) {
            name shouldBe "Fred"
            jsa.str shouldBe "anything"
        }
    }

    @Test fun `should report error correctly deserializing Java class with single-arg constructor`() {
        val json = JSONObject.build {
            add("name", "Fred")
            add("jsa", 1234)
        }
        shouldThrow<JSONKotlinException>("Incorrect type, expected string but was 1234, at /jsa") {
            json.deserialize<ClassWithJavaSingleArg>()
        }
    }

    @Test fun `should deserialize Java class with named arguments`() {
        val json = JSONObject.build {
            add("field1", "Fred")
            add("field2", 1234)
        }
        with(json.deserialize<JavaNamedArg>()) {
            field1 shouldBe "Fred"
            field2 shouldBe 1234
        }
    }

    @Test fun `should deserialize Java class with named arguments with one missing`() {
        val json = JSONObject.build {
            add("field2", 1234)
        }
        with(json.deserialize<JavaNamedArg>()) {
            field1 shouldBe null
            field2 shouldBe 1234
        }
    }

    @Test fun `should report error deserializing Java class with named arguments with primitive missing`() {
        val json = JSONObject.build {
            add("field1", "Fred")
        }
        shouldThrow<JSONKotlinException>("Mandatory constructor parameter missing - field2") {
            json.deserialize<JavaNamedArg>()
        }
    }

    @Test fun `should report error correctly deserializing Java class with named arguments`() {
        val json = JSONObject.build {
            add("field1", "Fred")
            add("field22", 1234)
        }
        shouldThrow<JSONKotlinException>("Mandatory constructor parameter missing - field2") {
            json.deserialize<JavaNamedArg>()
        }
    }

    @Test fun `should report error correctly deserializing Java class with named arguments 2`() {
        val json = JSONString("Fred")
        shouldThrow<JSONKotlinException>("Incorrect type, expected object but was \"Fred\"") {
            json.deserialize<JavaNamedArg>()
        }
    }

    @Test fun `should report error correctly deserializing Java class with named arguments with complex pointer`() {
        val json = JSONString("George")
        val jsonArray = JSONArray.build { add(json) }
        val jsonObject = JSONObject.build { add("inner", jsonArray) }
        shouldThrow<JSONKotlinException>("Incorrect type, expected object but was \"George\", at /inner/0") {
            jsonObject.deserialize<Map<String, Array<JavaNamedArg>>>()
        }
    }

    @Test fun `should deserialize Java class with named arguments with @JSONIgnore`() {
        val json = JSONObject.build {
            add("field1", "Fred")
            add("field2", 1234)
        }
        with(json.deserialize<JavaNamedArg1>()) {
            field1 shouldBe "Fred"
            field2 shouldBe 0
        }
    }

    @Test fun `should select between multiple constructors 1`() {
        val json = JSONObject.build {
            add("name", "Murphy")
            add("number", 27)
        }
        with(json.deserialize<JavaMultiConstructor>()) {
            type shouldBe "A"
            name shouldBe "Murphy"
            number shouldBe 27
        }
    }

    @Test fun `should select between multiple constructors 2`() {
        val json = JSONString("William")
        with(json.deserialize<JavaMultiConstructor>()) {
            type shouldBe "B"
            name shouldBe "William"
            number shouldBe 8
        }
    }

    @Test fun `should report error when no constructor found`() {
        val json = JSONInt(7777)
        shouldThrow<JSONKotlinException>(
            message = "No matching constructor for Java class io.kjson.testclasses.JavaMultiConstructor from 7777",
        ) {
            json.deserialize<JavaMultiConstructor>()
        }
    }

    @Test fun `should recognise annotations on Java classes`() {
        val json = JSONObject.build {
            add("alpha", "beta")
            add("field2", 123)
        }
        val config = JSONConfig {
            addNameAnnotation(CustomName::class, "symbol")
            addIgnoreAnnotation(CustomIgnore::class)
        }
        with(json.deserialize<JavaNamedField>(config)) {
            field1 shouldBe "beta"
            field2 shouldBe 0
        }
    }

    data class ClassWithJavaSingleArg(
        val name: String,
        val jsa: JavaSingleArg,
    )

}
