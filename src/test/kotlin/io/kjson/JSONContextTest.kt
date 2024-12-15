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

import java.math.BigDecimal
import java.math.BigInteger

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeSameInstance
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldThrow

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
        context.config shouldBeSameInstance config
        context.pointer shouldBeSameInstance JSONPointer.root
    }

    @Test fun `should create JSONContext with config and pointer`() {
        val config = JSONConfig()
        val pointer = JSONPointer("/abc/def")
        @Suppress("deprecation")
        val context = JSONContext(config, pointer)
        context.config shouldBeSameInstance config
        context.pointer shouldBe pointer
    }

    @Test fun `should create JSONContext with pointer only`() {
        val pointer = JSONPointer("/abc/def")
        @Suppress("deprecation")
        val context = JSONContext(pointer)
        context.config shouldBeSameInstance JSONConfig.defaultConfig
        context.pointer shouldBe pointer
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
            shouldBeType<JSONObject>()
            size shouldBe 3
            this["field1"] shouldBe JSONString("xyz")
            this["field2"] shouldBe JSONInt(12345)
            this["marker"] shouldBe JSONString("XXX")
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
            shouldBeType<JSONArray>()
            size shouldBe 2
            with(this[0]) {
                shouldBeType<JSONObject>()
                size shouldBe 2
                this["field1"] shouldBe JSONString("a")
                this["field2"] shouldBe JSONInt(9)
            }
            with(this[1]) {
                shouldBeType<JSONObject>()
                size shouldBe 2
                this["field1"] shouldBe JSONString("xyz")
                this["field2"] shouldBe JSONInt(12345)
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
            shouldBeType<JSONObject>()
            size shouldBe 2
            this["a"] shouldBe JSONString("xyz")
            this["b"] shouldBe JSONInt(12345)
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
            shouldBeType<JSONObject>()
            size shouldBe 2
            with(this["first"]) {
                shouldBeType<JSONObject>()
                size shouldBe 2
                this["field1"] shouldBe JSONString("xyz")
                this["field2"] shouldBe JSONInt(12345)
            }
            with(this["second"]) {
                shouldBeType<JSONObject>()
                size shouldBe 2
                this["field1"] shouldBe JSONString("abc")
                this["field2"] shouldBe JSONInt(888)
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
            shouldBeType<Derived>()
            field1 shouldBe "abc"
            field2 shouldBe 888
            field3 shouldBe 10.0
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
        shouldThrow<JSONKotlinException>("Incorrect type, expected Int but was \"wrong\", at /field2") {
            json.fromJSONValue<Super>(config)
        }.let {
            it.pointer shouldBe JSONPointer("/field2")
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
            shouldBeType<Derived>()
            field1 shouldBe "xyz"
            field2 shouldBe 21
            field3 shouldBe 1.0
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
            shouldBeType<Derived>()
            field1 shouldBe "ggg"
            field2 shouldBe 42
            field3 shouldBe 9.0
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
            shouldBeType<Derived>()
            field1 shouldBe "abc"
            field2 shouldBe 888
            field3 shouldBe 10.0
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
        shouldThrow<JSONKotlinException>("Incorrect type, expected Int but was \"bad\", at /inner/field2") {
            json.fromJSONValue<Super>(config)
        }.let {
            it.pointer shouldBe JSONPointer("/inner/field2")
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
            shouldBeType<Derived>()
            field1 shouldBe "ace"
            field2 shouldBe -5
            field3 shouldBe 15.0
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
            shouldBeType<Derived>()
            field1 shouldBe "one"
            field2 shouldBe 1
            field3 shouldBe 1.0
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
                size shouldBe 2
                with(this[0]) {
                    field1 shouldBe "one"
                    field2 shouldBe 111
                }
                with(this[1]) {
                    field1 shouldBe "two"
                    field2 shouldBe 222
                }
            }
            text shouldBe "default"
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
        shouldThrow<JSONKotlinException>("Incorrect type, expected Int but was \"111\", at /0/field2") {
            json.fromJSONValue<Dummy4>(config)
        }.let {
            it.pointer shouldBe JSONPointer("/0/field2")
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
                size shouldBe 2
                with(this[0]) {
                    field1 shouldBe "un"
                    field2 shouldBe 111
                }
                with(this[1]) {
                    field1 shouldBe "deux"
                    field2 shouldBe 222
                }
            }
            text shouldBe "default"
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
                size shouldBe 2
                with(this[0]) {
                    field1 shouldBe "ein"
                    field2 shouldBe 111
                }
                with(this[1]) {
                    field1 shouldBe "zwei"
                    field2 shouldBe 222
                }
            }
            text shouldBe "default"
        }
    }

    @Test fun `should throw exception including pointer`() {
        @Suppress("deprecation")
        val context = JSONContext(JSONPointer("/abc/def"))
        shouldThrow<JSONKotlinException>("Dummy message, at /abc/def") {
            context.fatal("Dummy message")
        }
    }

    @Test fun `should throw exception including pointer and nested exception`() {
        @Suppress("deprecation")
        val context = JSONContext(JSONPointer("/abc/def"))
        val nested = NullPointerException("dummy")
        @Suppress("UNREACHABLE_CODE")
        shouldThrow<JSONKotlinException>("Dummy message, at /abc/def") {
            context.fatal("Dummy message", nested)
        }.let {
            it.cause shouldBeSameInstance nested
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
            shouldBeType<JSONObject>()
            size shouldBe 2
            this["bi"].asString shouldBe "123"
            this["bd"].asString shouldBe "2.5"
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
            shouldBeType<JSONObject>()
            size shouldBe 2
            this["bi"].asInt shouldBe 123
            this["bd"].asString shouldBe "2.5"
        }
    }

}
