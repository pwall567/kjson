/*
 * @(#) JSONDeserializerObjectTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022 Peter Wall
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.expect
import kotlin.test.fail

import java.time.LocalDate

import io.kjson.Constants.jsonObjectInt
import io.kjson.Constants.linkedHashMapStringIntType
import io.kjson.Constants.mapStringInt
import io.kjson.Constants.mapStringIntType
import io.kjson.testclasses.CustomName
import io.kjson.testclasses.Derived
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.Dummy2
import io.kjson.testclasses.Dummy3
import io.kjson.testclasses.Dummy4
import io.kjson.testclasses.DummyMap
import io.kjson.testclasses.DummyObject
import io.kjson.testclasses.DummyWithCustomNameAnnotation
import io.kjson.testclasses.DummyWithNameAnnotation
import io.kjson.testclasses.DummyWithParamNameAnnotation
import io.kjson.testclasses.Super

import net.pwall.util.ImmutableMap
import net.pwall.util.ImmutableMapEntry

class JSONDeserializerObjectTest {

    @Test fun `should return map of String to Int from JSONObject`() {
        expect(mapStringInt) { JSONDeserializer.deserialize(mapStringIntType, jsonObjectInt)}
    }

    @Test fun `should return LinkedHashMap of String to Int from JSONObject`() {
        val linkedHashMapStringInt = LinkedHashMap(mapStringInt)
        val result = JSONDeserializer.deserialize(linkedHashMapStringIntType, jsonObjectInt)
        assertEquals(linkedHashMapStringInt, result)
        assertTrue(result is LinkedHashMap<*, *>)
    }

    @Test fun `should return simple data class from JSONObject`() {
        val json = JSONObject.build {
            add("field1", "Hello")
            add("field2", 12345)
        }
        val expected = Dummy1("Hello", 12345)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return simple data class with default parameter from JSONObject`() {
        val json = JSONObject.build { add("field1", "Hello") }
        val expected = Dummy1("Hello")
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return data class with extra values from JSONObject`() {
        val json = JSONObject.build {
            add("field1", "Hello")
            add("field2", 12345)
            add("extra", "XXX")
        }
        val expected = Dummy2("Hello", 12345)
        expected.extra = "XXX"
        val result = JSONDeserializer.deserialize<Dummy2>(json)
        assertEquals(expected, result)
        assertEquals("XXX", result?.extra)
    }

    @Test fun `should return nested data class from JSONObject`() {
        val json1 = JSONObject.build {
            add("field1", "Whoa")
            add("field2", 98765)
        }
        val json2 = JSONObject.build {
            add("dummy1", json1)
            add("text", "special")
        }
        val expected = Dummy3(Dummy1("Whoa", 98765), "special")
        expect(expected) { JSONDeserializer.deserialize(json2) }
    }

    @Test fun `should return nested data class with list from JSONObject`() {
        val json1 = JSONObject.build {
            add("field1", "Whoa")
            add("field2", 98765)
        }
        val json2 = JSONObject.build {
            add("field1", "Hi!")
            add("field2", 333)
        }
        val json3 = JSONArray.build {
            add(json1)
            add(json2)
        }
        val json4 = JSONObject.build {
            add("listDummy1", json3)
            add("text", "special")
        }
        val expected = Dummy4(listOf(Dummy1("Whoa", 98765), Dummy1("Hi!", 333)), "special")
        expect(expected) { JSONDeserializer.deserialize(json4) }
    }

    @Test fun `should return simple class with properties fromJSONObject`() {
        val json = JSONObject.build {
            add("field1", "qqq")
            add("field2", 888)
        }
        val expected = Super()
        expected.field1 = "qqq"
        expected.field2 = 888
        expect(expected) { JSONDeserializer.deserialize(Super::class, json) }
    }

    @Test fun `should return derived class with properties from JSONObject`() {
        // also test parsing from String
        val str = """{"field1":"qqq","field2":888,"field3":12345.0}"""
        val expected = Derived()
        expected.field1 = "qqq"
        expected.field2 = 888
        expected.field3 = 12345.0
        expect(expected) { JSONDeserializer.deserialize(Derived::class, JSON.parse(str)) }
    }

    @Test fun `should return simple class with properties using name annotation from JSONObject`() {
        val json = JSONObject.build {
            add("field1", "qqq")
            add("fieldX", 888)
        }
        val expected = DummyWithNameAnnotation()
        expected.field1 = "qqq"
        expected.field2 = 888
        expect(expected) { JSONDeserializer.deserialize(DummyWithNameAnnotation::class, json) }
    }

    @Test fun `should return data class using name annotation from JSONObject`() {
        val json = JSONObject.build {
            add("field1", "qqq")
            add("fieldX", 888)
        }
        expect(DummyWithParamNameAnnotation("qqq", 888)) {
            JSONDeserializer.deserialize(DummyWithParamNameAnnotation::class, json)
        }
    }

    @Test fun `should return data class using custom name annotation from JSONObject`() {
        val json = JSONObject.build {
            add("field1", "qqq")
            add("fieldX", 888)
        }
        val expected = DummyWithCustomNameAnnotation("qqq", 888)
        val config = JSONConfig().apply {
            addNameAnnotation(CustomName::class, "symbol")
        }
        expect(expected) { JSONDeserializer.deserialize(DummyWithCustomNameAnnotation::class, json, config) }
    }

    @Test fun `should deserialize to object from JSONObject`() {
        val json = JSONObject.build { add("field1", "abc") }
        expect(DummyObject) { JSONDeserializer.deserialize(DummyObject::class, json) }
    }

    @Test fun `should deserialize JSONObject into Map derived type`() {
        val json = JSONObject.build {
            add("aaa", "2019-10-06")
            add("bbb", "2019-10-05")
        }
        val expected = DummyMap(emptyMap()).apply {
            put("aaa", LocalDate.of(2019, 10, 6))
            put("bbb", LocalDate.of(2019, 10, 5))
        }
        expect(expected) { JSONDeserializer.deserialize(DummyMap::class, json) }
    }

    @Test fun `should deserialize JSONObject to Any`() {
        val json = JSONObject.build {
            add("aaa", 1234)
            add("ccc", 9999)
            add("bbb", 5678)
            add("abc", 8888)
        }
        val result = JSONDeserializer.deserializeAny(json)
        // check that the result is a map in the correct order
        if (result is Map<*, *>) {
            val iterator = result.keys.iterator()
            assertTrue(iterator.hasNext())
            iterator.next().let {
                expect("aaa") { it }
                expect(1234) { result[it] }
            }
            assertTrue(iterator.hasNext())
            iterator.next().let {
                expect("ccc") { it }
                expect(9999) { result[it] }
            }
            assertTrue(iterator.hasNext())
            iterator.next().let {
                expect("bbb") { it }
                expect(5678) { result[it] }
            }
            assertTrue(iterator.hasNext())
            iterator.next().let {
                expect("abc") { it }
                expect(8888) { result[it] }
            }
            assertFalse(iterator.hasNext())
        }
        else
            fail("Not a Map - $result")
    }

    @Test fun `should deserialize Map taking Map constructor parameter`() {
        val json = JSONObject.build {
            add("aaa", 1234)
            add("ccc", 9999)
            add("bbb", 5678)
            add("abc", 8888)
        }
        val immutableMap = ImmutableMap.from(
            listOf(
                ImmutableMapEntry("aaa", 1234),
                ImmutableMapEntry("ccc", 9999),
                ImmutableMapEntry("bbb", 5678),
                ImmutableMapEntry("abc", 8888),
            )
        )
        expect(immutableMap) { json.deserialize<ImmutableMap<String, Int>>() }
    }

}
