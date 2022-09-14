/*
 * @(#) JSONConfigTest.kt
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

import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.expect
import kotlin.test.fail
import java.time.LocalTime

import io.kjson.JSON.asInt
import io.kjson.JSON.asObject
import io.kjson.JSON.asString
import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.JSONTypeRef.Companion.createRef
import io.kjson.parser.ParseOptions
import io.kjson.pointer.JSONPointer
import io.kjson.testclasses.CustomName
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.Dummy3
import io.kjson.testclasses.Dummy9
import io.kjson.testclasses.DummyA
import io.kjson.testclasses.DummyB
import io.kjson.testclasses.DummyC
import io.kjson.testclasses.DummyD
import io.kjson.testclasses.DummyWithCustomNameAnnotation
import io.kjson.testclasses.PolymorphicBase
import io.kjson.testclasses.PolymorphicDerived1
import io.kjson.testclasses.PolymorphicDerived2

class JSONConfigTest {

    private val stringType = String::class.createType()

    @Test fun `should construct default config`() {
        val config = JSONConfig()
        expect("class") { config.sealedClassDiscriminator }
        expect(8192) { config.readBufferSize }
        expect(1024) { config.stringifyInitialSize }
        expect(Charsets.UTF_8) { config.charset }
        assertFalse(config.bigIntegerString)
        assertFalse(config.bigDecimalString)
        assertFalse(config.includeNulls)
        assertFalse(config.allowExtra)
        assertFalse(config.stringifyNonASCII)
        assertFalse(config.streamOutput)
    }

    @Test fun `should add fromJSON mapping`() {
        val config = JSONConfig()
        assertNull(config.findFromJSONMapping(stringType))
        assertNull(config.findFromJSONMapping(String::class))
        config.fromJSON { json -> json?.toString() }
        assertNotNull(config.findFromJSONMapping(stringType))
        assertNotNull(config.findFromJSONMapping(String::class))
    }

    @Test fun `should map simple data class using fromJSON mapping`() {
        val config = JSONConfig {
            fromJSON { json ->
                if (json !is JSONObject)
                    fatal("Must be JSONObject")
                Dummy1(json["a"].asString, json["b"].asInt)
            }
        }
        val json = JSONObject.build {
            add("a", "xyz")
            add("b", 888)
        }
        expect(Dummy1("xyz", 888)) { JSONDeserializer.deserialize(Dummy1::class.createType(), json, config) }
    }

    @Test fun `should map simple data class using fromJSONObject mapping`() {
        val config = JSONConfig {
            fromJSONObject { json ->
                Dummy1(json["a"].asString, json["b"].asInt)
            }
        }
        val json = JSONObject.build {
            add("a", "xyz")
            add("b", 888)
        }
        expect(Dummy1("xyz", 888)) { JSONDeserializer.deserialize(Dummy1::class.createType(), json, config) }
    }

    @Test fun `should map array using fromJSONArray mapping`() {
        val config = JSONConfig {
            fromJSONArray {json ->
                val hours = json[0].asInt
                val minutes = json[1].asInt
                val seconds = json[2].asInt
                LocalTime.of(hours, minutes, seconds)
            }
        }
        val json = JSONArray.build {
            add(18)
            add(25)
            add(0)
        }
        expect(LocalTime.parse("18:25:00")) { JSONDeserializer.deserialize(json, config) }
    }

    @Test fun `should not interfere with other deserialization when using fromJSON mapping`() {
        val config = JSONConfig {
            fromJSON { json ->
                if (json !is JSONObject)
                    fatal("Must be JSONObject")
                Dummy1(json["a"].asString, json["b"].asInt)
            }
        }
        val json = JSONArray.of(JSONString("AAA"), JSONString("BBB"))
        val result = JSONDeserializer.deserialize<List<Any>>(json, config)
        expect(listOf("AAA", "BBB")) { result }
    }

    @Test fun `should map nested class using fromJSON mapping`() {
        val config = JSONConfig {
            fromJSON { json ->
                json.asObject.let { Dummy1(it["a"].asString, it["b"].asInt) }
            }
        }
        val json1 = JSONObject.build {
            add("a", "xyz")
            add("b", 888)
        }
        val json2 = JSONObject.build {
            add("dummy1", json1)
            add("text", "Hello!")
        }
        expect(Dummy3(Dummy1("xyz", 888), "Hello!")) {
            JSONDeserializer.deserialize(Dummy3::class.createType(), json2, config)
        }
    }

    @Test fun `should add toJSON mapping`() {
        val config = JSONConfig()
        assertNull(config.findToJSONMapping(stringType))
        assertNull(config.findToJSONMapping(String::class))
        config.toJSON<String> { str -> JSONString(str ?: fatal("String expected")) }
        assertNotNull(config.findToJSONMapping(stringType))
        assertNotNull(config.findToJSONMapping(String::class))
    }

    @Test fun `should map simple data class using toJSON mapping`() {
        val config = JSONConfig {
            toJSON<Dummy1> { obj ->
                obj?.let {
                    JSONObject.build {
                        add("a", it.field1)
                        add("b", it.field2)
                    }
                }
            }
        }
        val expected = JSONObject.build {
            add("a", "xyz")
            add("b", 888)
        }
        expect(expected) { JSONSerializer.serialize(Dummy1("xyz", 888), config) }
    }

    @Test fun `should map nested class using toJSON mapping`() {
        val config = JSONConfig {
            toJSON { obj: Dummy1? -> // change to allow non-nullable?
                obj?.let {
                    JSONObject.build {
                        add("a", it.field1)
                        add("b", it.field2)
                    }
                }
            }
        }
        val dummy1 = JSONObject.build {
            add("a", "xyz")
            add("b", 888)
        }
        val expected = JSONObject.build {
            add("dummy1", dummy1)
            add("text", "Hi there!")
        }
        expect(expected) { JSONSerializer.serialize(Dummy3(Dummy1("xyz", 888), "Hi there!"), config) }
    }

    @Test fun `should select correct toJSON mapping of nullable type`() {
        val config = JSONConfig {
            toJSON(Dummy1::class.createType(nullable = true)) { JSONString("A") }
        }
        expect(JSONString("A")) { JSONSerializer.serialize(Dummy1("X", 0), config) }
    }

    @Test fun `should select correct toJSON mapping of non-nullable type`() {
        val config = JSONConfig {
            toJSON(Dummy1::class.createType(nullable = false)) { JSONString("A") }
        }
        expect(JSONString("A")) { JSONSerializer.serialize(Dummy1("X", 0), config) }
    }

    @Test fun `should select correct function among derived classes for toJSON mapping`() {
        val config = JSONConfig {
            toJSON<DummyA> { JSONString("A") }
            toJSON<DummyB> { JSONString("B") }
            toJSON<DummyC> { JSONString("C") }
            toJSON<DummyD> { JSONString("D") }
        }
        expect(JSONString("A")) { JSONSerializer.serialize(DummyA(), config)}
        expect(JSONString("B")) { JSONSerializer.serialize(DummyB(), config)}
        expect(JSONString("C")) { JSONSerializer.serialize(DummyC(), config)}
        expect(JSONString("D")) { JSONSerializer.serialize(DummyD(), config)}
    }

    @Test fun `should select correct function when order is reversed for toJSON mapping`() {
        val config = JSONConfig {
            toJSON<DummyD> { JSONString("D") }
            toJSON<DummyC> { JSONString("C") }
            toJSON<DummyB> { JSONString("B") }
            toJSON<DummyA> { JSONString("A") }
        }
        expect(JSONString("A")) { JSONSerializer.serialize(DummyA(), config)}
        expect(JSONString("B")) { JSONSerializer.serialize(DummyB(), config)}
        expect(JSONString("C")) { JSONSerializer.serialize(DummyC(), config)}
        expect(JSONString("D")) { JSONSerializer.serialize(DummyD(), config)}
    }

    @Test fun `should select correct function when exact match not present for toJSON mapping`() {
        val config = JSONConfig {
            toJSON<DummyA> { JSONString("A") }
            toJSON<DummyB> { JSONString("B") }
            toJSON<DummyC> { JSONString("C") }
        }
        expect(JSONString("A")) { JSONSerializer.serialize(DummyA(), config)}
        expect(JSONString("B")) { JSONSerializer.serialize(DummyB(), config)}
        expect(JSONString("C")) { JSONSerializer.serialize(DummyC(), config)}
        expect(JSONString("C")) { JSONSerializer.serialize(DummyD(), config)}
    }

    @Test fun `should select correct function among derived classes for fromJSON mapping`() {
        val config = JSONConfig {
            fromJSON { if (it == JSONString("A")) DummyA() else fail() }
            fromJSON { if (it == JSONString("B")) DummyB() else fail() }
            fromJSON { if (it == JSONString("C")) DummyC() else fail() }
            fromJSON { if (it == JSONString("D")) DummyD() else fail() }
        }
        assertTrue(JSONDeserializer.deserialize(DummyA::class.createType(), JSONString("A"), config) is DummyA)
        assertTrue(JSONDeserializer.deserialize(DummyB::class.createType(), JSONString("B"), config) is DummyB)
        assertTrue(JSONDeserializer.deserialize(DummyC::class.createType(), JSONString("C"), config) is DummyC)
        assertTrue(JSONDeserializer.deserialize(DummyD::class.createType(), JSONString("D"), config) is DummyD)
    }

    @Test fun `should select correct function when order is reversed for fromJSON mapping`() {
        val config = JSONConfig {
            fromJSON { if (it == JSONString("D")) DummyD() else fail() }
            fromJSON { if (it == JSONString("C")) DummyC() else fail() }
            fromJSON { if (it == JSONString("B")) DummyB() else fail() }
            fromJSON { if (it == JSONString("A")) DummyA() else fail() }
        }
        assertTrue(JSONDeserializer.deserialize(DummyA::class.createType(), JSONString("A"), config) is DummyA)
        assertTrue(JSONDeserializer.deserialize(DummyB::class.createType(), JSONString("B"), config) is DummyB)
        assertTrue(JSONDeserializer.deserialize(DummyC::class.createType(), JSONString("C"), config) is DummyC)
        assertTrue(JSONDeserializer.deserialize(DummyD::class.createType(), JSONString("D"), config) is DummyD)
    }

    @Test fun `should select correct function when exact match not present for fromJSON mapping`() {
        val config = JSONConfig {
            fromJSON { if (it == JSONString("B")) DummyB() else fail() }
            fromJSON { if (it == JSONString("C")) DummyC() else fail() }
            fromJSON { if (it == JSONString("D")) DummyD() else fail() }
        }
        assertTrue(JSONDeserializer.deserialize(DummyA::class.createType(), JSONString("B"), config) is DummyB)
        assertTrue(JSONDeserializer.deserialize(DummyB::class.createType(), JSONString("B"), config) is DummyB)
        assertTrue(JSONDeserializer.deserialize(DummyC::class.createType(), JSONString("C"), config) is DummyC)
        assertTrue(JSONDeserializer.deserialize(DummyD::class.createType(), JSONString("D"), config) is DummyD)
    }

    @Test fun `should use toJSON mapping with JSONConfig toJSONString`() {
        val config = JSONConfig {
            toJSONString<Dummy9>()
        }
        expect(JSONString("abcdef")) { JSONSerializer.serialize(Dummy9("abcdef"), config) }
    }

    @Test fun `should use String constructor for fromJSON mapping with JSONConfig fromJSONString`() {
        val config = JSONConfig {
            fromJSONString<Dummy9>()
        }
        expect(Dummy9("abcdef")) { JSONDeserializer.deserialize(JSONString("abcdef"), config) }
    }

    @Test fun `should use fromJSONString mapping for fromJSON mapping`() {
        val config = JSONConfig {
            fromJSONString<Dummy9> { Dummy9(it.value.reversed()) }
        }
        expect(Dummy9("fedcba")) { JSONDeserializer.deserialize(JSONString("abcdef"), config) }
    }

    @Test fun `should distinguish between polymorphic mappings`() {
        val config = JSONConfig {
            fromJSONPolymorphic(PolymorphicBase::class, "type",
                JSONString("TYPE1") to typeOf<PolymorphicDerived1>(),
                JSONString("TYPE2") to typeOf<PolymorphicDerived2>()
            )
        }
        expect(PolymorphicDerived1("TYPE1", 1234)) {
            """{"type":"TYPE1","extra1":1234}""".parseJSON<PolymorphicBase>(config)
        }
        expect(PolymorphicDerived2("TYPE2", "hello")) {
            """{"type":"TYPE2","extra2":"hello"}""".parseJSON<PolymorphicBase>(config)
        }
    }

    @Test fun `should distinguish between polymorphic mappings using raw discriminator values`() {
        val config = JSONConfig {
            fromJSONPolymorphic(PolymorphicBase::class, "type",
                "TYPE1" to typeOf<PolymorphicDerived1>(),
                "TYPE2" to typeOf<PolymorphicDerived2>()
            )
        }
        expect(PolymorphicDerived1("TYPE1", 1234)) {
            """{"type":"TYPE1","extra1":1234}""".parseJSON<PolymorphicBase>(config)
        }
        expect(PolymorphicDerived2("TYPE2", "hello")) {
            """{"type":"TYPE2","extra2":"hello"}""".parseJSON<PolymorphicBase>(config)
        }
    }

    @Test fun `should distinguish between polymorphic mappings using JSONPointer`() {
        val config = JSONConfig {
            fromJSONPolymorphic(PolymorphicBase::class, JSONPointer("/type"),
                JSONString("TYPE1") to typeOf<PolymorphicDerived1>(),
                JSONString("TYPE2") to typeOf<PolymorphicDerived2>()
            )
        }
        expect(PolymorphicDerived1("TYPE1", 1234)) {
            """{"type":"TYPE1","extra1":1234}""".parseJSON<PolymorphicBase>(config)
        }
        expect(PolymorphicDerived2("TYPE2", "hello")) {
            """{"type":"TYPE2","extra2":"hello"}""".parseJSON<PolymorphicBase>(config)
        }
    }

    @Test fun `should distinguish between polymorphic mappings using type`() {
        val config = JSONConfig {
            fromJSONPolymorphic(PolymorphicBase::class.starProjectedType, "type",
                JSONString("TYPE1") to JSONTypeRef.create<PolymorphicDerived1>().refType,
                JSONString("TYPE2") to JSONTypeRef.create<PolymorphicDerived2>().refType
            )
        }
        expect(PolymorphicDerived1("TYPE1", 987)) {
            """{"type":"TYPE1","extra1":987}""".parseJSON<PolymorphicBase>(config)
        }
        expect(PolymorphicDerived2("TYPE2", "bye")) {
            """{"type":"TYPE2","extra2":"bye"}""".parseJSON<PolymorphicBase>(config)
        }
    }

    @Test fun `should distinguish between polymorphic mappings using type and JSONPointer`() {
        val config = JSONConfig {
            fromJSONPolymorphic(PolymorphicBase::class.starProjectedType, JSONPointer("/type"),
                JSONString("TYPE1") to createRef<PolymorphicDerived1>(),
                JSONString("TYPE2") to createRef<PolymorphicDerived2>()
            )
        }
        expect(PolymorphicDerived1("TYPE1", 987)) {
            """{"type":"TYPE1","extra1":987}""".parseJSON<PolymorphicBase>(config)
        }
        expect(PolymorphicDerived2("TYPE2", "bye")) {
            """{"type":"TYPE2","extra2":"bye"}""".parseJSON<PolymorphicBase>(config)
        }
    }

    @Test fun `should use multiple JSONConfig mappings`() {
        val config = JSONConfig {
            toJSON<Dummy1> { obj ->
                obj?.let {
                    JSONObject.build {
                        add("a", it.field1)
                        add("b", it.field2)
                    }
                }
            }
            fromJSON { json ->
                require(json is JSONObject) { "Must be JSONObject" }
                Dummy1(json["a"].asString, json["b"].asInt)
            }
            toJSON<Dummy3> { obj ->
                obj?.let {
                    JSONObject.build {
                        add("dummy", JSONSerializer.serialize(it.dummy1, this@toJSON))
                        add("text", it.text)
                    }
                }
            }
            fromJSON { json ->
                require(json is JSONObject) { "Must be JSONObject" }
                Dummy3(JSONDeserializer.deserialize(json["dummy"].asObject, this) ?: fail(), json["text"].asString)
            }
        }
        val json1 = JSONObject.build {
            add("a", "xyz")
            add("b", 888)
        }
        val dummy1 = Dummy1("xyz", 888)
        expect(json1) { JSONSerializer.serialize(dummy1, config) }
        expect(dummy1) { JSONDeserializer.deserialize(json1, config) }
        val json3 = JSONObject.build {
            add("dummy", json1)
            add("text", "excellent")
        }
        val dummy3 = Dummy3(dummy1, "excellent")
        expect(json3) { JSONSerializer.serialize(dummy3, config) }
        expect(dummy3) { JSONDeserializer.deserialize(json3, config) }
    }

    @Test fun `should transfer toJSON mapping on combineMappings`() {
        val config = JSONConfig {
            toJSON<Dummy1> { obj ->
                obj?.let {
                    JSONObject.build {
                        add("a", it.field1)
                        add("b", it.field2)
                    }
                }
            }
        }
        val config2 = JSONConfig {
            combineMappings(config)
        }
        val expected = JSONObject.build {
            add("a", "xyz")
            add("b", 888)
        }
        expect(expected) { JSONSerializer.serialize(Dummy1("xyz", 888), config2) }
    }

    @Test fun `should transfer toJSON mapping on combineAll`() {
        val config = JSONConfig {
            toJSON<Dummy1> { obj ->
                obj?.let {
                    JSONObject.build {
                        add("a", it.field1)
                        add("b", it.field2)
                    }
                }
            }
        }
        val config2 = JSONConfig {
            combineAll(config)
        }
        val expected = JSONObject.build {
            add("a", "xyz")
            add("b", 888)
        }
        expect(expected) { JSONSerializer.serialize(Dummy1("xyz", 888), config2) }
    }

    @Test fun `should transfer JSONName annotation on combineAll`() {
        val obj = DummyWithCustomNameAnnotation("abc", 123)
        val config = JSONConfig {
            addNameAnnotation(CustomName::class, "symbol")
        }
        val config2 = JSONConfig {
            combineAll(config)
        }
        val expected = JSONObject.build {
            add("field1", "abc")
            add("fieldX", 123)
        }
        expect(expected) { JSONSerializer.serialize(obj, config2) }
    }

    @Test fun `should transfer switch settings and numeric values on combineAll`() {
        val options = ParseOptions(
            objectKeyDuplicate = ParseOptions.DuplicateKeyOption.TAKE_LAST,
            arrayTrailingComma = true,
        )
        val config = JSONConfig {
            sealedClassDiscriminator = "??"
            readBufferSize = 16384
            stringifyInitialSize = 512
            bigIntegerString = true
            bigDecimalString = true
            includeNulls = true
            allowExtra = true
            stringifyNonASCII = true
            streamOutput = true
            parseOptions = options
        }
        val config2 = JSONConfig {
            combineAll(config)
        }
        expect("??") { config2.sealedClassDiscriminator }
        expect(16384) { config2.readBufferSize }
        expect(512) { config2.stringifyInitialSize }
        assertTrue(config2.bigIntegerString)
        assertTrue(config2.bigDecimalString)
        assertTrue(config2.includeNulls)
        assertTrue(config2.allowExtra)
        assertTrue(config2.stringifyNonASCII)
        assertTrue(config2.streamOutput)
        expect(options) { config2.parseOptions }
    }

}
