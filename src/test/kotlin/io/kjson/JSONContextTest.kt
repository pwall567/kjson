/*
 * @(#) JSONContextTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2023, 2024 Peter Wall
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

import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.expect

import java.math.BigDecimal
import java.math.BigInteger

import io.kjson.JSON.asInt
import io.kjson.JSON.asString
import io.kjson.pointer.JSONPointer
import io.kjson.testclasses.BigHolder
import io.kjson.testclasses.Derived
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.Dummy4
import io.kjson.testclasses.Super

class JSONContextTest {

    @Test fun `should create JSONContext with config only`() {
        val config = JSONConfig()
        @Suppress("deprecation")
        val context = JSONContext(config)
        assertSame(config, context.config)
        assertSame(JSONPointer.root, context.pointer)
    }

    @Test fun `should create JSONContext with config and pointer`() {
        val config = JSONConfig()
        val pointer = JSONPointer("/abc/def")
        @Suppress("deprecation")
        val context = JSONContext(config, pointer)
        assertSame(config, context.config)
        expect(pointer) { context.pointer }
    }

    @Test fun `should create JSONContext with pointer only`() {
        val pointer = JSONPointer("/abc/def")
        @Suppress("deprecation")
        val context = JSONContext(pointer)
        assertSame(JSONConfig.defaultConfig, context.config)
        expect(pointer) { context.pointer }
    }

//    @Test fun `should create child JSONContext with property name`() {
//        val config = JSONConfig()
//        val context = JSONContext(config, JSONPointer("/abc/def"))
//        val child = context.child("xyz")
//        assertSame(config, child.config)
//        expect(JSONPointer("/abc/def/xyz")) { child.pointer }
//    }

//    @Test fun `should create child JSONContext with array index`() {
//        val config = JSONConfig()
//        val context = JSONContext(config, JSONPointer("/abc/def"))
//        val child = context.child(1)
//        assertSame(config, child.config)
//        expect(JSONPointer("/abc/def/1")) { child.pointer }
//    }

    @Test fun `should serialise properties using JSONContext`() {
        val config = JSONConfig {
            toJSON<Dummy1> {
                JSONObject.build {
                    addProperty("field1", it.field1)
                    addProperty("field2", it.field2)
                    add("marker", "XXX") // just to confirm it isn't using default serialisation
                }
            }
        }
        with(JSONSerializer.serialize(Dummy1("xyz", 12345), config)) {
            assertIs<JSONObject>(this)
            expect(3) { size }
            expect(JSONString("xyz")) { this["field1"] }
            expect(JSONInt(12345)) { this["field2"] }
            expect(JSONString("XXX")) { this["marker"] }
        }
    }

    @Test fun `should serialise array items using JSONContext`() {
        val config = JSONConfig {
            toJSON<List<Dummy1>> {
                JSONArray.build {
                    addItem(1, it[1])
                    addItem(0, it[0])
                }
            }
        }
        with(JSONSerializer.serialize(listOf(Dummy1("xyz", 12345), Dummy1("a", 9)), config)) {
            assertIs<JSONArray>(this)
            expect(2) { size }
            with(this[0]) {
                assertIs<JSONObject>(this)
                expect(2) { size }
                expect(JSONString("a")) { this["field1"] }
                expect(JSONInt(9)) { this["field2"] }
            }
            with(this[1]) {
                assertIs<JSONObject>(this)
                expect(2) { size }
                expect(JSONString("xyz")) { this["field1"] }
                expect(JSONInt(12345)) { this["field2"] }
            }
        }
    }

    @Test fun `should serialise using JSONContext with explicit serializeProperty`() {
        val config = JSONConfig {
            toJSON<Dummy1> {
                JSONObject.build {
                    add("a", serializeProperty("field1", it.field1))
                    add("b", serializeProperty("field2", it.field2))
                }
            }
        }
        with(JSONSerializer.serialize(Dummy1("xyz", 12345), config)) {
            assertIs<JSONObject>(this)
            expect(2) { size }
            expect(JSONString("xyz")) { this["a"] }
            expect(JSONInt(12345)) { this["b"] }
        }
    }

    @Test fun `should serialise array using JSONContext`() {
        val config = JSONConfig {
            toJSON<List<Dummy1>> {
                JSONObject.build {
                    listOf("first", "second", "third").forEachIndexed { i, s ->
                        if (i < it.size)
                            add(s, serializeItem(i, it[i]))
                    }
                }
            }
        }
        val list = listOf(Dummy1("xyz", 12345), Dummy1("abc", 888))
        with(JSONSerializer.serialize(list, config)) {
            assertIs<JSONObject>(this)
            expect(2) { size }
            with(this["first"]) {
                assertIs<JSONObject>(this)
                expect(2) { size }
                expect(JSONString("xyz")) { this["field1"] }
                expect(JSONInt(12345)) { this["field2"] }
            }
            with(this["second"]) {
                assertIs<JSONObject>(this)
                expect(2) { size }
                expect(JSONString("abc")) { this["field1"] }
                expect(JSONInt(888)) { this["field2"] }
            }
        }
    }

    @Test fun `should deserialize using JSONContext`() {
        val config = JSONConfig {
            fromJSONObject<Super> { json ->
                deserialize<Derived>(json)
            }
        }
        val json = JSONObject.build {
            add("field1", "abc")
            add("field2", 888)
            add("field3", 10)
        }
        with(json.fromJSONValue<Super>(config)) {
            assertTrue(this is Derived)
            expect("abc") { field1 }
            expect(888) { field2 }
            expect(10.0) { field3 }
        }
    }

    @Test fun `should report error with pointer when using JSONContext`() {
        val config = JSONConfig {
            fromJSONObject<Super> { json ->
                deserialize<Derived>(json)
            }
        }
        val json = JSONObject.build {
            add("field1", "abc")
            add("field2", "wrong")
            add("field3", 10)
        }
        assertFailsWith<JSONKotlinException> { json.fromJSONValue<Super>(config) }.let {
            expect("Incorrect type, expected Int but was \"wrong\", at /field2") { it.message }
            expect(JSONPointer("/field2")) { it.pointer }
        }
    }

    @Test fun `should deserialize using JSONContext with KType`() {
        val config = JSONConfig {
            fromJSONObject<Super> { json ->
                deserialize(typeOf<Derived>(), json) as Super
            }
        }
        val json = JSONObject.build {
            add("field1", "xyz")
            add("field2", 21)
            add("field3", 1)
        }
        with(json.fromJSONValue<Super>(config)) {
            assertTrue(this is Derived)
            expect("xyz") { field1 }
            expect(21) { field2 }
            expect(1.0) { field3 }
        }
    }

    @Test fun `should deserialize using JSONContext with KClass`() {
        val config = JSONConfig {
            fromJSONObject<Super> { json ->
                deserialize(Derived::class, json) as Super
            }
        }
        val json = JSONObject.build {
            add("field1", "ggg")
            add("field2", 42)
            add("field3", 9)
        }
        with(json.fromJSONValue<Super>(config)) {
            assertTrue(this is Derived)
            expect("ggg") { field1 }
            expect(42) { field2 }
            expect(9.0) { field3 }
        }
    }

    @Test fun `should deserialize child property using JSONContext`() {
        val config = JSONConfig {
            fromJSONObject<Super> { json ->
                deserializeProperty<Derived>("inner", json)
            }
        }
        val json = JSONObject.build {
            add("inner", JSONObject.build {
                add("field1", "abc")
                add("field2", 888)
                add("field3", 10)
            })
        }
        with(json.fromJSONValue<Super>(config)) {
            assertTrue(this is Derived)
            expect("abc") { field1 }
            expect(888) { field2 }
            expect(10.0) { field3 }
        }
    }

    @Test fun `should report error with pointer for child property using JSONContext`() {
        val config = JSONConfig {
            fromJSONObject<Super> { json ->
                deserializeProperty<Derived>("inner", json)
            }
        }
        val json = JSONObject.build {
            add("inner", JSONObject.build {
                add("field1", "abc")
                add("field2", "bad")
                add("field3", 10)
            })
        }
        assertFailsWith<JSONKotlinException> { json.fromJSONValue<Super>(config) }.let {
            expect("Incorrect type, expected Int but was \"bad\", at /inner/field2") { it.message }
            expect(JSONPointer("/inner/field2")) { it.pointer }
        }
    }

    @Test fun `should deserialize child property using JSONContext with KType`() {
        val config = JSONConfig {
            fromJSONObject<Super> { json ->
                deserializeProperty(typeOf<Derived>(), "inner", json) as Super
            }
        }
        val json = JSONObject.build {
            add("inner", JSONObject.build {
                add("field1", "ace")
                add("field2", -5)
                add("field3", 15)
            })
        }
        with(json.fromJSONValue<Super>(config)) {
            assertTrue(this is Derived)
            expect("ace") { field1 }
            expect(-5) { field2 }
            expect(15.0) { field3 }
        }
    }

    @Test fun `should deserialize child property using JSONContext with KClass`() {
        val config = JSONConfig {
            fromJSONObject<Super> { json ->
                deserializeProperty(Derived::class, "inner", json) as Super
            }
        }
        val json = JSONObject.build {
            add("inner", JSONObject.build {
                add("field1", "one")
                add("field2", 1)
                add("field3", 1)
            })
        }
        with(json.fromJSONValue<Super>(config)) {
            assertTrue(this is Derived)
            expect("one") { field1 }
            expect(1) { field2 }
            expect(1.0) { field3 }
        }
    }

    @Test fun `should deserialize child array item using JSONContext`() {
        val config = JSONConfig {
            fromJSONArray { array ->
                Dummy4(List(array.size) { deserializeItem(it, array) }, "default")
            }
        }
        val json = JSONArray.build {
            add(JSONObject.build {
                add("field1", "one")
                add("field2", 111)
            })
            add(JSONObject.build {
                add("field1", "two")
                add("field2", 222)
            })
        }
        with(json.fromJSONValue<Dummy4>(config)) {
            with(listDummy1) {
                expect(2) { size }
                with(this[0]) {
                    expect("one") { field1 }
                    expect(111) { field2 }
                }
                with(this[1]) {
                    expect("two") { field1 }
                    expect(222) { field2 }
                }
            }
            expect("default") { text }
        }
    }

    @Test fun `should report error with pointer child array item using JSONContext`() {
        val config = JSONConfig {
            fromJSONArray { array ->
                Dummy4(List(array.size) { deserializeItem(it, array) }, "default")
            }
        }
        val json = JSONArray.build {
            add(JSONObject.build {
                add("field1", "one")
                add("field2", "111")
            })
        }
        assertFailsWith<JSONKotlinException> { json.fromJSONValue<Dummy4>(config) }.let {
            expect("Incorrect type, expected Int but was \"111\", at /0/field2") { it.message }
            expect(JSONPointer("/0/field2")) { it.pointer }
        }
    }

    @Test fun `should deserialize child array item using JSONContext with KType`() {
        val config = JSONConfig {
            fromJSONArray { array ->
                Dummy4(List(array.size) { deserializeItem(typeOf<Dummy1>(), it, array) as Dummy1 }, "default")
            }
        }
        val json = JSONArray.build {
            add(JSONObject.build {
                add("field1", "un")
                add("field2", 111)
            })
            add(JSONObject.build {
                add("field1", "deux")
                add("field2", 222)
            })
        }
        with(json.fromJSONValue<Dummy4>(config)) {
            with(listDummy1) {
                expect(2) { size }
                with(this[0]) {
                    expect("un") { field1 }
                    expect(111) { field2 }
                }
                with(this[1]) {
                    expect("deux") { field1 }
                    expect(222) { field2 }
                }
            }
            expect("default") { text }
        }
    }

    @Test fun `should deserialize child array item using JSONContext with KClass`() {
        val config = JSONConfig {
            fromJSONArray { array ->
                Dummy4(List(array.size) { deserializeItem(Dummy1::class, it, array)!! }, "default")
            }
        }
        val json = JSONArray.build {
            add(JSONObject.build {
                add("field1", "ein")
                add("field2", 111)
            })
            add(JSONObject.build {
                add("field1", "zwei")
                add("field2", 222)
            })
        }
        with(json.fromJSONValue<Dummy4>(config)) {
            with(listDummy1) {
                expect(2) { size }
                with(this[0]) {
                    expect("ein") { field1 }
                    expect(111) { field2 }
                }
                with(this[1]) {
                    expect("zwei") { field1 }
                    expect(222) { field2 }
                }
            }
            expect("default") { text }
        }
    }

    @Test fun `should throw exception including pointer`() {
        @Suppress("deprecation")
        val context = JSONContext(JSONPointer("/abc/def"))
        assertFailsWith<JSONKotlinException> { context.fatal("Dummy message") }.let {
            expect("Dummy message, at /abc/def") { it.message }
        }
    }

    @Test fun `should throw exception including pointer and nested exception`() {
        @Suppress("deprecation")
        val context = JSONContext(JSONPointer("/abc/def"))
        val nested = NullPointerException("dummy")
        assertFailsWith<JSONKotlinException> { context.fatal("Dummy message", nested) }.let {
            expect("Dummy message, at /abc/def") { it.message }
            assertSame(nested, it.cause)
        }
    }

    @Test fun `should allow config to be modified`() {
        val config = JSONConfig {
            bigDecimalString = false
            bigIntegerString = true
            toJSON<BigHolder> {
                with(copy { bigDecimalString = true }) {
                    JSONObject.build {
                        addProperty("bi", it.bi)
                        addProperty("bd", it.bd)
                    }
                }
            }
        }
        with(JSONSerializer.serialize(BigHolder(BigInteger("123"), BigDecimal("2.5")), config)) {
            assertIs<JSONObject>(this)
            expect(2) { size }
            expect("123") { this["bi"].asString }
            expect("2.5") { this["bd"].asString }
        }
    }

    @Test fun `should allow config to be replaced`() {
        val config = JSONConfig {
            bigDecimalString = false
            bigIntegerString = true
            toJSON<BigHolder> {
                with(JSONConfig { bigDecimalString = true }) {
                    JSONObject.build {
                        addProperty("bi", it.bi)
                        addProperty("bd", it.bd)
                    }
                }
            }
        }
        with(JSONSerializer.serialize(BigHolder(BigInteger("123"), BigDecimal("2.5")), config)) {
            assertIs<JSONObject>(this)
            expect(2) { size }
            expect(123) { this["bi"].asInt }
            expect("2.5") { this["bd"].asString }
        }
    }

}
