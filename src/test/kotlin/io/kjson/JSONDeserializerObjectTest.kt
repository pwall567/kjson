/*
 * @(#) JSONDeserializerObjectTest.kt
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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.expect
import kotlin.test.fail
import java.net.URI
import java.net.URL

import java.time.LocalDate
import java.util.UUID

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
import io.kjson.testclasses.DummyObject2
import io.kjson.testclasses.DummyWithCustomNameAnnotation
import io.kjson.testclasses.DummyWithNameAnnotation
import io.kjson.testclasses.DummyWithParamNameAnnotation
import io.kjson.testclasses.GenericCreator
import io.kjson.testclasses.Super
import io.kjson.testclasses.TestGenericClass
import io.kjson.testclasses.TestGenericClass2
import io.kjson.testclasses.TestMapClass
import io.kjson.testclasses.TypeAliasData
import io.kjson.testclasses.ValueClassHolder
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
        assertEquals("XXX", result.extra)
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
        val config = JSONConfig {
            addNameAnnotation(CustomName::class, "symbol")
        }
        expect(expected) { JSONDeserializer.deserialize(DummyWithCustomNameAnnotation::class, json, config) }
    }

    @Test fun `should deserialize to object from JSONObject`() {
        val json = JSONObject.build { add("field1", "abc") }
        expect(DummyObject) { JSONDeserializer.deserialize(DummyObject::class, json) }
    }

    @Test fun `should deserialize to object with variable from JSONObject`() {
        val json = JSONObject.build {
            add("field1", "abc")
            add("field2", 999)
        }
        DummyObject2.field2 = 123
        expect(DummyObject2) { JSONDeserializer.deserialize(DummyObject2::class, json) }
        expect(999) { DummyObject2.field2 }
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

    @Test fun `should deserialize into value class`() {
        val json = JSONObject.build {
            add("innerValue", "abc")
            add("number", 123)
        }
        val valueClassHolder = json.deserialize<ValueClassHolder>()
        expect("abc") { valueClassHolder.innerValue.string }
        expect(123) { valueClassHolder.number }
    }

    @Test fun `should deserialize into typealias Map`() {
        val json = JSONObject.build {
            add("aaa", "ttt")
            add("bbb", JSONObject.build {
                add("alpha", 111)
                add("beta", 222)
            })
        }
        val obj: TypeAliasData = json.deserialize()
        expect("ttt") { obj.aaa }
        with(obj.bbb) {
            expect(2) { size }
            expect(111) { this["alpha"] }
            expect(222) { this["beta"] }
        }
    }

    @Test fun `should deserialize into complex Map`() {
        val uuid = UUID.fromString("35c94940-acfe-11ed-89a2-b30027b2e14c")
        val json = JSONObject.build {
            add(uuid.toString(), "2023-02-15")
        }
        val map: Map<UUID, LocalDate> = json.deserialize()
        expect(LocalDate.of(2023, 2, 15)) { map[uuid] }
    }

    @Test fun `should deserialize into even more complex Map`() {
        val config = JSONConfig {
            fromJSONString<ObscureCase> {
                ObscureCase(it.value.toInt())
            }
        }
        val json = JSONObject.build {
            add("12345", "works")
        }
        val map: Map<ObscureCase, String> = json.deserialize(config)
        expect("works") { map[ObscureCase(12345)] }
    }

    class ObscureCase(val value: Int) {
        override fun equals(other: Any?): Boolean = other is ObscureCase && value == other.value
        override fun hashCode(): Int = value.hashCode()
    }

    @Test fun `should deserialize into map using delegation`() {
        val json = JSONObject.build {
            add("field1", "Hello")
            add("field2", "a20449ac-ade3-11ee-bdf5-139f8439485a")
            add("field3", 12345)
        }
        val mapClass: TestMapClass = json.deserialize()
        expect("Hello") { mapClass.field1 }
        expect(UUID.fromString("a20449ac-ade3-11ee-bdf5-139f8439485a")) { mapClass.field2 }
        expect(12345) { mapClass["field3"] }
    }

    @Test fun `should deserialize into generic class`() {
        val data = JSONObject.build {
            add("field1", "turnip")
            add("field2", 999)
        }
        val json = JSONObject.build {
            add("name", "alpha")
            add("data", data)
        }
        val generic: TestGenericClass<Dummy1> = json.deserialize()
        expect("alpha") { generic.name }
        expect(Dummy1("turnip", 999)) { generic.data }
    }

    @Test fun `should deserialize into generic class with member variables`() {
        val data = JSONObject.build {
            add("field1", "turnip")
            add("field2", 999)
        }
        val json = JSONObject.build {
            add("name", "alpha")
            add("data", data)
        }
        val generic: TestGenericClass2<Dummy1> = json.deserialize()
        expect("alpha") { generic.name }
        expect(Dummy1("turnip", 999)) { generic.data }
    }

    @Test fun `should report error when deserializing into generic class within another generic class`() {
        // TODO - find a way to handle this situation correctly - see comment in JSONDeserializer.applyTypeParameters
        val json = """{"name":"ZZZ","data":{"field1":"ace","field2":777}}"""
        assertFailsWith<JSONKotlinException> { GenericCreator<Dummy1>().parseString(json) }.let {
            expect("Can't deserialize TT - insufficient type information, at /data") { it.message }
        }
    }

    @Test fun `should deserialize into class containing URI`() {
        val json = JSONObject.build {
            add("name", "Fred")
            add("uri", "http://kjson.io")
        }
        with(json.deserialize<ClassWithURI>()) {
            expect("Fred") { name }
            expect(URI("http://kjson.io")) { uri }
        }
    }

    @Test fun `should deserialize into class containing URL`() {
        val json = JSONObject.build {
            add("name", "Fred")
            add("url", "http://kjson.io")
        }
        with(json.deserialize<ClassWithURL>()) {
            expect("Fred") { name }
            expect(URL("http://kjson.io")) { url }
        }
    }

    @Test fun `should analyse obscure KType`() {

    }

    data class ClassWithURI(
        val name: String,
        val uri: URI,
    )

    data class ClassWithURL(
        val name: String,
        val url: URL,
    )

}
