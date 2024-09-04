/*
 * @(#) ObjectSerializerTest.kt
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

package io.kjson.serialize

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.fail
import kotlinx.coroutines.runBlocking

import io.kjson.JSONConfig
import io.kjson.JSONInt
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.serialize.Serializer.Companion.createObjectSerializer
import io.kjson.serialize.Serializer.Companion.findSerializer
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.Dummy3
import io.kjson.testclasses.JavaClass1
import io.kjson.util.CoCapture
import io.kjson.util.shouldBe

class ObjectSerializerTest {

    @Test fun `should serialize simple object`() {
        val testObject = Dummy1("ABC", 123)
        val serializer = createSerializerForDummy1()
        val result = serializer.serialize(testObject, config, mutableListOf())
        assertIs<JSONObject>(result)
        result.size shouldBe 2
        with(result[0]) {
            name shouldBe  "field1"
            value shouldBe JSONString("ABC")
        }
        with(result[1]) {
            name shouldBe  "field2"
            value shouldBe JSONInt(123)
        }
    }

    @Test fun `should stringify simple object`() {
        val testObject = Dummy1("ABC", 123)
        val serializer = createSerializerForDummy1()
        val result = buildString {
            serializer.appendTo(this, testObject, config, mutableListOf())
        }
        result shouldBe """{"field1":"ABC","field2":123}"""
    }

    @Test fun `should stringify simple object non-blocking`() = runBlocking {
        val coCapture = CoCapture()
        val testObject = Dummy1("ABC", 123)
        val serializer = createSerializerForDummy1()
        serializer.output(coCapture, testObject, config, mutableListOf())
        coCapture.toString() shouldBe """{"field1":"ABC","field2":123}"""
    }

    @Test fun `should serialize simple object using dynamic findSerializer`() {
        val testObject = Dummy1("ABC", 123)
        val serializer = createSerializerFor(Dummy1::class, "field1", "field2")
        val result = serializer.serialize(testObject, config, mutableListOf())
        assertIs<JSONObject>(result)
        result.size shouldBe 2
        with(result[0]) {
            name shouldBe  "field1"
            value shouldBe JSONString("ABC")
        }
        with(result[1]) {
            name shouldBe  "field2"
            value shouldBe JSONInt(123)
        }
    }

    @Test fun `should serialize simple object using createObjectSerializer`() {
        val testObject = Dummy1("ABC", 123)
        val serializer = createObjectSerializer(typeOf<Dummy1>(), Dummy1::class, config)
        val result = serializer.serialize(testObject, config, mutableListOf())
        assertIs<JSONObject>(result)
        result.size shouldBe 2
        with(result[0]) {
            name shouldBe  "field1"
            value shouldBe JSONString("ABC")
        }
        with(result[1]) {
            name shouldBe  "field2"
            value shouldBe JSONInt(123)
        }
    }

    @Test fun `should serialize nested object`() {
        val testObject = Dummy3(Dummy1("alpha", 99), "beta")
        val serializer = createSerializerForDummy3()
        val result = serializer.serialize(testObject, config, mutableListOf())
        assertIs<JSONObject>(result)
        result.size shouldBe 2
        with(result[0]) {
            name shouldBe "dummy1"
            with(value) {
                assertIs<JSONObject>(this)
                size shouldBe 2
                with(this[0]) {
                    name shouldBe "field1"
                    value shouldBe JSONString("alpha")
                }
                with(this[1]) {
                    name shouldBe "field2"
                    value shouldBe JSONInt(99)
                }
            }
        }
        with(result[1]) {
            name shouldBe "text"
            value shouldBe JSONString("beta")
        }
    }

    @Test fun `should stringify nested object`() {
        val testObject = Dummy3(Dummy1("alpha", 99), "beta")
        val serializer = createSerializerForDummy3()
        val result = buildString {
            serializer.appendTo(this, testObject, config, mutableListOf())
        }
        result shouldBe """{"dummy1":{"field1":"alpha","field2":99},"text":"beta"}"""
    }

    @Test fun `should stringify nested object non-blocking`() = runBlocking {
        val coCapture = CoCapture()
        val testObject = Dummy3(Dummy1("alpha", 99), "beta")
        val serializer = createSerializerForDummy3()
        serializer.output(coCapture, testObject, config, mutableListOf())
        coCapture.toString() shouldBe """{"dummy1":{"field1":"alpha","field2":99},"text":"beta"}"""
    }

    @Test fun `should serialize Java object`() {
        val testObject = JavaClass1(12345, "Java class")
        val serializer = findSerializer(typeOf<JavaClass1>(), config) as Serializer<JavaClass1>
        val result = serializer.serialize(testObject, config, mutableListOf())
        assertIs<JSONObject>(result)
        result.size shouldBe 2
        // unfortunately, we don't know what order the functions appear in the Java class members array (I suspect they
        // are stored internally in a HashMap), and hence what order the JSONObject entries appear in
        result["field1"] shouldBe JSONInt(12345)
        result["field2"] shouldBe JSONString("Java class")
    }

    companion object {

        val config = JSONConfig.defaultConfig

        @Suppress("unchecked_cast")
        fun createSerializerForDummy1(): ObjectSerializer<Dummy1> {
            val kClass = Dummy1::class
            val memberProperties = kClass.memberProperties
            val field1Getter = memberProperties.find { it.name == "field1" } ?: fail("Can't find getter for field1")
            val field2Getter = memberProperties.find { it.name == "field2" } ?: fail("Can't find getter for field2")
            return ObjectSerializer(
                kClass = kClass,
                sealedClassDiscriminator = null,
                propertyDescriptors = listOf(
                    ObjectSerializer.KotlinPropertyDescriptor(
                        name = "field1",
                        kClass = String::class,
                        serializer = CharSequenceSerializer,
                        includeIfNull = false,
                        getter = field1Getter.getter as KProperty.Getter<String>,
                    ) as ObjectSerializer.PropertyDescriptor<Any>,
                    ObjectSerializer.KotlinPropertyDescriptor(
                        name = "field2",
                        kClass = Int::class,
                        serializer = IntSerializer,
                        includeIfNull = false,
                        getter = field2Getter.getter as KProperty.Getter<Int>,
                    ) as ObjectSerializer.PropertyDescriptor<Any>,
                )
            )
        }

        @Suppress("unchecked_cast")
        fun createSerializerForDummy3(): ObjectSerializer<Dummy3> {
            val kClass = Dummy3::class
            val memberProperties = kClass.memberProperties
            val dummy1Getter = memberProperties.find { it.name == "dummy1" } ?: fail("Can't find getter for dummy1")
            val textGetter = memberProperties.find { it.name == "text" } ?: fail("Can't find getter for text")
            return ObjectSerializer(
                kClass = kClass,
                sealedClassDiscriminator = null,
                propertyDescriptors = listOf(
                    ObjectSerializer.KotlinPropertyDescriptor(
                        name = "dummy1",
                        kClass = Dummy1::class,
                        serializer = createSerializerForDummy1(),
                        includeIfNull = false,
                        getter = dummy1Getter.getter as KProperty.Getter<Dummy1>,
                    ) as ObjectSerializer.PropertyDescriptor<Any>,
                    ObjectSerializer.KotlinPropertyDescriptor(
                        name = "text",
                        kClass = String::class,
                        serializer = CharSequenceSerializer,
                        includeIfNull = false,
                        getter = textGetter.getter as KProperty.Getter<String>,
                    ) as ObjectSerializer.PropertyDescriptor<Any>,
                )
            )
        }

        @Suppress("unchecked_cast")
        fun <T : Any> createSerializerFor(
            kClass : KClass<T>,
            vararg names: String,
        ): ObjectSerializer<T> {
            val memberProperties = kClass.memberProperties
            val propertyDescriptors = mutableListOf<ObjectSerializer.PropertyDescriptor<Any>>()
            for (name in names) {
                val getter = memberProperties.find { it.name == name } ?: fail("Can't find property getter for $name")
                val returnType = getter.returnType
                val serializer = findSerializer(returnType, config)
                propertyDescriptors.add(
                    ObjectSerializer.KotlinPropertyDescriptor(
                        name = name,
                        kClass = returnType.classifier as KClass<Any>,
                        serializer = serializer,
                        includeIfNull = false,
                        getter = getter.getter,
                    )
                )
            }
            return ObjectSerializer(
                kClass = kClass,
                sealedClassDiscriminator = null,
                propertyDescriptors = propertyDescriptors,
            )
        }

    }

}
