/*
 * @(#) JSONConfigTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023, 2024, 2025 Peter Wall
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
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.fail

import java.time.LocalTime

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeEqual
import io.kstuff.test.shouldBeNonNull
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldThrow

import io.kjson.JSON.asInt
import io.kjson.JSON.asObject
import io.kjson.JSON.asString
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
import io.kjson.testclasses.DummyWithIncludeAllProperties
import io.kjson.testclasses.PolymorphicBase
import io.kjson.testclasses.PolymorphicDerived1
import io.kjson.testclasses.PolymorphicDerived2
import io.kjson.testclasses.PolymorphicGeneric
import io.kjson.testclasses.TestGenericClass

class JSONConfigTest {

    private val stringType = String::class.createType()

    @Test fun `should construct default config`() {
        val config = JSONConfig()
        config.sealedClassDiscriminator shouldBe "class"
        config.readBufferSize shouldBe 8192
        config.stringifyInitialSize shouldBe 2048
        config.charset shouldBe Charsets.UTF_8
        config.bigIntegerString shouldBe false
        config.bigDecimalString shouldBe false
        config.includeNulls shouldBe false
        config.allowExtra shouldBe false
        config.stringifyNonASCII shouldBe false
        config.streamOutput shouldBe false
    }

    @Test fun `should add fromJSON mapping`() {
        val config = JSONConfig()
        config.findFromJSONMapping(stringType) shouldBe null
        config.findFromJSONMapping(String::class) shouldBe null
        config.fromJSON { json -> json.toString() }
        config.findFromJSONMapping(stringType).shouldBeNonNull()
        config.findFromJSONMapping(String::class).shouldBeNonNull()
    }

    @Test fun `should map simple data class using fromJSON mapping`() {
        val config = JSONConfig {
            fromJSON { json ->
                if (json !is JSONObject)
                    throw JSONKotlinException("Must be JSONObject")
                Dummy1(json["a"].asString, json["b"].asInt)
            }
        }
        val json = JSONObject.build {
            add("a", "xyz")
            add("b", 888)
        }
        JSONDeserializer.deserialize(Dummy1::class.createType(), json, config) shouldBe Dummy1("xyz", 888)
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
        JSONDeserializer.deserialize(Dummy1::class.createType(), json, config) shouldBe Dummy1("xyz", 888)
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
        shouldBeEqual(LocalTime.parse("18:25:00"), JSONDeserializer.deserialize(json, config))
    }

    @Test fun `should not interfere with other deserialization when using fromJSON mapping`() {
        val config = JSONConfig {
            fromJSON { json ->
                if (json !is JSONObject)
                    throw JSONKotlinException("Must be JSONObject")
                Dummy1(json["a"].asString, json["b"].asInt)
            }
        }
        val json = JSONArray.of(JSONString("AAA"), JSONString("BBB"))
        val result = JSONDeserializer.deserialize<List<Any>>(json, config)
        result shouldBe listOf("AAA", "BBB")
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
        JSONDeserializer.deserialize(Dummy3::class.createType(), json2, config) shouldBe
                Dummy3(Dummy1("xyz", 888), "Hello!")
    }

    @Test fun `should correctly report error in fromJSON`() {
        val config = JSONConfig {
            fromJSONString<Dummy1> {
                throw IllegalStateException("Wrong")
            }
        }
        shouldThrow<JSONKotlinException>("Error in custom fromJSON mapping of ${Dummy1::class.qualifiedName}") {
            JSONString("data").fromJSONValue<Dummy1>(config)
        }.let {
            with(it.cause) {
                shouldBeType<IllegalStateException>()
                message shouldBe "Wrong"
            }
        }
    }

    @Test fun `should add toJSON mapping`() {
        val config = JSONConfig()
        config.findToJSONMapping(stringType) shouldBe null
        config.findToJSONMapping(String::class) shouldBe null
        config.toJSON<String> { str -> JSONString(str) }
        config.findToJSONMapping(stringType).shouldBeNonNull()
        config.findToJSONMapping(String::class).shouldBeNonNull()
    }

    @Test fun `should map simple data class using toJSON mapping`() {
        val config = JSONConfig {
            toJSON<Dummy1> { obj ->
                obj.let {
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
        JSONSerializer.serialize(Dummy1("xyz", 888), config) shouldBe expected
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
        JSONSerializer.serialize(Dummy3(Dummy1("xyz", 888), "Hi there!"), config) shouldBe expected
    }

    @Test fun `should select correct toJSON mapping of nullable type`() {
        val config = JSONConfig {
            toJSON(Dummy1::class.createType(nullable = true)) { JSONString("A") }
        }
        JSONSerializer.serialize(Dummy1("X", 0), config) shouldBe JSONString("A")
    }

    @Test fun `should select correct toJSON mapping of non-nullable type`() {
        val config = JSONConfig {
            toJSON(Dummy1::class.createType(nullable = false)) { JSONString("A") }
        }
        JSONSerializer.serialize(Dummy1("X", 0), config) shouldBe JSONString("A")
    }

    @Test fun `should select correct function among derived classes for toJSON mapping`() {
        val config = JSONConfig {
            toJSON<DummyA> { JSONString("A") }
            toJSON<DummyB> { JSONString("B") }
            toJSON<DummyC> { JSONString("C") }
            toJSON<DummyD> { JSONString("D") }
        }
        JSONSerializer.serialize(DummyA(), config) shouldBe JSONString("A")
        JSONSerializer.serialize(DummyB(), config) shouldBe JSONString("B")
        JSONSerializer.serialize(DummyC(), config) shouldBe JSONString("C")
        JSONSerializer.serialize(DummyD(), config) shouldBe JSONString("D")
    }

    @Test fun `should select correct function when order is reversed for toJSON mapping`() {
        val config = JSONConfig {
            toJSON<DummyD> { JSONString("D") }
            toJSON<DummyC> { JSONString("C") }
            toJSON<DummyB> { JSONString("B") }
            toJSON<DummyA> { JSONString("A") }
        }
        JSONSerializer.serialize(DummyA(), config) shouldBe JSONString("A")
        JSONSerializer.serialize(DummyB(), config) shouldBe JSONString("B")
        JSONSerializer.serialize(DummyC(), config) shouldBe JSONString("C")
        JSONSerializer.serialize(DummyD(), config) shouldBe JSONString("D")
    }

    @Test fun `should select correct function when exact match not present for toJSON mapping`() {
        val config = JSONConfig {
            toJSON<DummyA> { JSONString("A") }
            toJSON<DummyB> { JSONString("B") }
            toJSON<DummyC> { JSONString("C") }
        }
        JSONSerializer.serialize(DummyA(), config) shouldBe JSONString("A")
        JSONSerializer.serialize(DummyB(), config) shouldBe JSONString("B")
        JSONSerializer.serialize(DummyC(), config) shouldBe JSONString("C")
        JSONSerializer.serialize(DummyD(), config) shouldBe JSONString("C")
    }

    @Test fun `should select correct function among derived classes for fromJSON mapping`() {
        val config = JSONConfig {
            fromJSON { if (it == JSONString("A")) DummyA() else fail() }
            fromJSON { if (it == JSONString("B")) DummyB() else fail() }
            fromJSON { if (it == JSONString("C")) DummyC() else fail() }
            fromJSON { if (it == JSONString("D")) DummyD() else fail() }
        }
        JSONDeserializer.deserialize(DummyA::class.createType(), JSONString("A"), config).shouldBeType<DummyA>()
        JSONDeserializer.deserialize(DummyB::class.createType(), JSONString("B"), config).shouldBeType<DummyB>()
        JSONDeserializer.deserialize(DummyC::class.createType(), JSONString("C"), config).shouldBeType<DummyC>()
        JSONDeserializer.deserialize(DummyD::class.createType(), JSONString("D"), config).shouldBeType<DummyD>()
    }

    @Test fun `should select correct function when order is reversed for fromJSON mapping`() {
        val config = JSONConfig {
            fromJSON { if (it == JSONString("D")) DummyD() else fail() }
            fromJSON { if (it == JSONString("C")) DummyC() else fail() }
            fromJSON { if (it == JSONString("B")) DummyB() else fail() }
            fromJSON { if (it == JSONString("A")) DummyA() else fail() }
        }
        JSONDeserializer.deserialize(DummyA::class.createType(), JSONString("A"), config).shouldBeType<DummyA>()
        JSONDeserializer.deserialize(DummyB::class.createType(), JSONString("B"), config).shouldBeType<DummyB>()
        JSONDeserializer.deserialize(DummyC::class.createType(), JSONString("C"), config).shouldBeType<DummyC>()
        JSONDeserializer.deserialize(DummyD::class.createType(), JSONString("D"), config).shouldBeType<DummyD>()
    }

    @Test fun `should select correct function when exact match not present for fromJSON mapping`() {
        val config = JSONConfig {
            fromJSON { if (it == JSONString("B")) DummyB() else fail() }
            fromJSON { if (it == JSONString("C")) DummyC() else fail() }
            fromJSON { if (it == JSONString("D")) DummyD() else fail() }
        }
        JSONDeserializer.deserialize(DummyA::class.createType(), JSONString("B"), config).shouldBeType<DummyB>()
        JSONDeserializer.deserialize(DummyB::class.createType(), JSONString("B"), config).shouldBeType<DummyB>()
        JSONDeserializer.deserialize(DummyC::class.createType(), JSONString("C"), config).shouldBeType<DummyC>()
        JSONDeserializer.deserialize(DummyD::class.createType(), JSONString("D"), config).shouldBeType<DummyD>()
    }

    @Test fun `should use toJSON mapping with JSONConfig toJSONString`() {
        val config = JSONConfig {
            toJSONString<Dummy9>()
        }
        JSONSerializer.serialize(Dummy9("abcdef"), config) shouldBe JSONString("abcdef")
    }

    @Test fun `should use String constructor for fromJSON mapping with JSONConfig fromJSONString`() {
        val config = JSONConfig {
            fromJSONString<Dummy9>()
        }
        shouldBeEqual(Dummy9("abcdef"), JSONDeserializer.deserialize(JSONString("abcdef"), config))
    }

    @Test fun `should use fromJSONString mapping for fromJSON mapping`() {
        val config = JSONConfig {
            fromJSONString<Dummy9> { Dummy9(it.value.reversed()) }
        }
        shouldBeEqual(Dummy9("fedcba"), JSONDeserializer.deserialize(JSONString("abcdef"), config))
    }

    @Test fun `should use String constructor with extra parameters for fromJSON mapping`() {
        val config = JSONConfig {
            fromJSONString<Dummy1>()
        }
        shouldBeEqual(Dummy1("abcdef"), JSONDeserializer.deserialize(JSONString("abcdef"), config))
    }

    @Test fun `should distinguish between polymorphic mappings`() {
        val config = JSONConfig {
            fromJSONPolymorphic(PolymorphicBase::class, "type",
                JSONString("TYPE1") to typeOf<PolymorphicDerived1>(),
                JSONString("TYPE2") to typeOf<PolymorphicDerived2>()
            )
        }
        """{"type":"TYPE1","extra1":1234}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived1("TYPE1", 1234)
        """{"type":"TYPE2","extra2":"hello"}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived2("TYPE2", "hello")
    }

    @Test fun `should distinguish between polymorphic mappings using raw discriminator values`() {
        val config = JSONConfig {
            fromJSONPolymorphic(PolymorphicBase::class, "type",
                "TYPE1" to typeOf<PolymorphicDerived1>(),
                "TYPE2" to typeOf<PolymorphicDerived2>()
            )
        }
        """{"type":"TYPE1","extra1":1234}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived1("TYPE1", 1234)
        """{"type":"TYPE2","extra2":"hello"}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived2("TYPE2", "hello")
    }

    enum class Types { TYPE1, TYPE2 }

    @Test fun `should distinguish between polymorphic mappings using enum discriminator values`() {
        val config = JSONConfig {
            fromJSONPolymorphic(PolymorphicBase::class, "type",
                Types.TYPE1 to typeOf<PolymorphicDerived1>(),
                Types.TYPE2 to typeOf<PolymorphicDerived2>()
            )
        }
        """{"type":"TYPE1","extra1":1234}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived1("TYPE1", 1234)
        """{"type":"TYPE2","extra2":"hello"}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived2("TYPE2", "hello")
    }

    @Test fun `should distinguish between polymorphic mappings using JSONPointer`() {
        val config = JSONConfig {
            fromJSONPolymorphic(PolymorphicBase::class, JSONPointer("/type"),
                JSONString("TYPE1") to typeOf<PolymorphicDerived1>(),
                JSONString("TYPE2") to typeOf<PolymorphicDerived2>()
            )
        }
        """{"type":"TYPE1","extra1":1234}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived1("TYPE1", 1234)
        """{"type":"TYPE2","extra2":"hello"}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived2("TYPE2", "hello")
    }

    @Test fun `should distinguish between polymorphic mappings using type`() {
        val config = JSONConfig {
            fromJSONPolymorphic(PolymorphicBase::class.starProjectedType, "type",
                JSONString("TYPE1") to JSONTypeRef.create<PolymorphicDerived1>().refType,
                JSONString("TYPE2") to JSONTypeRef.create<PolymorphicDerived2>().refType
            )
        }
        """{"type":"TYPE1","extra1":987}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived1("TYPE1", 987)
        """{"type":"TYPE2","extra2":"bye"}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived2("TYPE2", "bye")
    }

    @Test fun `should distinguish between polymorphic mappings using type and JSONPointer`() {
        val config = JSONConfig {
            fromJSONPolymorphic(PolymorphicBase::class.starProjectedType, JSONPointer("/type"),
                JSONString("TYPE1") to createRef<PolymorphicDerived1>(),
                JSONString("TYPE2") to createRef<PolymorphicDerived2>()
            )
        }
        """{"type":"TYPE1","extra1":987}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived1("TYPE1", 987)
        """{"type":"TYPE2","extra2":"bye"}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived2("TYPE2", "bye")
    }

    @Test fun `should distinguish between polymorphic mappings using reified type`() {
        val config = JSONConfig {
            fromJSONPolymorphic<PolymorphicBase>("type",
                JSONString("TYPE1") to typeOf<PolymorphicDerived1>(),
                JSONString("TYPE2") to typeOf<PolymorphicDerived2>()
            )
        }
        """{"type":"TYPE1","extra1":987}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived1("TYPE1", 987)
        """{"type":"TYPE2","extra2":"bye"}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived2("TYPE2", "bye")
    }

    @Test fun `should distinguish between polymorphic mappings using reified type and JSONPointer`() {
        val config = JSONConfig {
            fromJSONPolymorphic<PolymorphicBase>(JSONPointer("/type"),
                "TYPE1" to typeOf<PolymorphicDerived1>(),
                "TYPE2" to typeOf<PolymorphicDerived2>()
            )
        }
        """{"type":"TYPE1","extra1":987}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived1("TYPE1", 987)
        """{"type":"TYPE2","extra2":"bye"}""".parseJSON<PolymorphicBase>(config) shouldBe
                PolymorphicDerived2("TYPE2", "bye")
    }

    @Test fun `should distinguish between polymorphic mappings of generic types`() {
        val type1 = typeOf<Pair<Int, PolymorphicBase>>()
        val type2 = typeOf<Pair<Int, PolymorphicDerived1>>()
        type2.isSubtypeOf(type1) shouldBe true
        val config = JSONConfig {
            fromJSONPolymorphic<PolymorphicGeneric<PolymorphicBase>>("code",
                "CODE1" to typeOf<PolymorphicGeneric<PolymorphicDerived1>>(),
                "CODE2" to typeOf<PolymorphicGeneric<PolymorphicDerived2>>()
            )
        }
        val json1 = """{"code":"CODE1","data":{"type":"TYPE1","extra1":987}}"""
        json1.parseJSON<PolymorphicGeneric<PolymorphicBase>>(config) shouldBe
                PolymorphicGeneric<PolymorphicBase>("CODE1", PolymorphicDerived1("TYPE1", 987))
        val json2 = """{"code":"CODE2","data":{"type":"TYPE2","extra2":"bye"}}"""
        json2.parseJSON<PolymorphicGeneric<PolymorphicBase>>(config) shouldBe
                PolymorphicGeneric<PolymorphicBase>("CODE2", PolymorphicDerived2("TYPE2", "bye"))
    }

    @Test fun `should use multiple JSONConfig mappings`() {
        val config = JSONConfig {
            toJSON<Dummy1> {
                JSONObject.build {
                    add("a", it.field1)
                    add("b", it.field2)
                }
            }
            fromJSON { json ->
                require(json is JSONObject) { "Must be JSONObject" }
                Dummy1(json["a"].asString, json["b"].asInt)
            }
            toJSON<Dummy3> {
                JSONObject.build {
                    add("dummy", serialize(it.dummy1))
                    add("text", it.text)
                }
            }
            fromJSON { json ->
                require(json is JSONObject) { "Must be JSONObject" }
                Dummy3(deserialize(json["dummy"].asObject), json["text"].asString)
            }
        }
        val json1 = JSONObject.build {
            add("a", "xyz")
            add("b", 888)
        }
        val dummy1 = Dummy1("xyz", 888)
        JSONSerializer.serialize(dummy1, config) shouldBe json1
        shouldBeEqual(dummy1, JSONDeserializer.deserialize(json1, config))
        val json3 = JSONObject.build {
            add("dummy", json1)
            add("text", "excellent")
        }
        val dummy3 = Dummy3(dummy1, "excellent")
        JSONSerializer.serialize(dummy3, config) shouldBe json3
        shouldBeEqual(dummy3, JSONDeserializer.deserialize(json3, config))
    }

    @Test fun `should transfer toJSON mapping on combineMappings`() {
        val config = JSONConfig {
            toJSON<Dummy1> {
                JSONObject.build {
                    add("a", it.field1)
                    add("b", it.field2)
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
        JSONSerializer.serialize(Dummy1("xyz", 888), config2) shouldBe expected
    }

    @Test fun `should transfer toJSON mapping on combineAll`() {
        val config = JSONConfig {
            toJSON<Dummy1> {
                JSONObject.build {
                    add("a", it.field1)
                    add("b", it.field2)
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
        JSONSerializer.serialize(Dummy1("xyz", 888), config2) shouldBe expected
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
        JSONSerializer.serialize(obj, config2) shouldBe expected
    }

    @Test fun `should transfer switch settings and numeric values on combineAll`() {
        val options = ParseOptions(
            objectKeyDuplicate = JSONObject.DuplicateKeyOption.TAKE_LAST,
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
        config2.sealedClassDiscriminator shouldBe "??"
        config2.readBufferSize shouldBe 16384
        config2.stringifyInitialSize shouldBe 512
        config2.bigIntegerString shouldBe true
        config2.bigDecimalString shouldBe true
        config2.includeNulls shouldBe true
        config2.allowExtra shouldBe true
        config2.stringifyNonASCII shouldBe true
        config2.streamOutput shouldBe true
        config2.parseOptions shouldBe options
    }

    @Test fun `should copy config with specified changes`() {
        val config = JSONConfig {
            stringifyInitialSize = 256
            sealedClassDiscriminator = "?"
        }
        val copyConfig = config.copy {
            stringifyInitialSize = 128
        }
        copyConfig.stringifyInitialSize shouldBe 128
        copyConfig.sealedClassDiscriminator shouldBe "?"
        config.stringifyInitialSize shouldBe 256
    }

    @Test fun `should detect whether class has @JSONIncludeAllProperties`() {
        val annotations1 = DummyWithIncludeAllProperties::class.annotations
        JSONConfig.defaultConfig.hasIncludeAllPropertiesAnnotation(annotations1) shouldBe true
        val annotations2 = Dummy1::class.annotations
        JSONConfig.defaultConfig.hasIncludeAllPropertiesAnnotation(annotations2) shouldBe false
    }

    @Test fun `should deserialise interface with the right config`() {
        val config = JSONConfig {
            fromJSON<DummyInterface> { deserialize<DummyImplementation>(it) }
        }
        val json = JSONObject.build {
            add("name", "Fred")
            add("number", 123)
        }
        val result: DummyInterface = JSONDeserializer.deserialize(json, config)
        result.shouldBeType<DummyImplementation>()
        result.name shouldBe "Fred"
        result.number shouldBe 123
    }

    @Test fun `should serialise generic class with custom serialisation`() {
        val config = JSONConfig {
            addTestGenericToJSON<Int>()
        }
        val test = TestGenericClass(name = "Fred", data = 27)
        val json = JSONSerializer.serialize(test, config)
        json.shouldBeType<JSONObject>()
        json.size shouldBe 2
        json["NAME"] shouldBe JSONString("Fred")
        json["data"] shouldBe JSONInt(27)
    }

    @Test fun `should deserialise generic class with custom deserialisation`() {
        val config = JSONConfig {
            fromJSON { json ->
                require(json is JSONObject) { "Must be JSONObject" }
                val data = JSONDeserializer.deserialize<Int>(json["data"])
                TestGenericClass(
                    name = json["NAME"].asString,
                    data = data,
                )
            }
        }
        val json = JSONObject.build {
            add("NAME", "Fred")
            add("data", 27)
        }
        val test = JSONDeserializer.deserialize<TestGenericClass<Int>>(json, config)
        test.name shouldBe "Fred"
        test.data shouldBe 27
    }

    @Test fun `should deserialise generic class with custom deserialisation in function`() {
        val config = JSONConfig {
            addTestGenericFromJSON<Int>()
        }
        val json = JSONObject.build {
            add("NAME", "Fred")
            add("data", 27)
        }
        val test = JSONDeserializer.deserialize<TestGenericClass<Int>>(json, config)
        test.name shouldBe "Fred"
        test.data shouldBe 27
    }

    companion object {

        inline fun <reified T : Number> JSONConfig.addTestGenericToJSON() {
            toJSON<TestGenericClass<T>> {
                JSONObject.build {
                    addProperty("NAME", it.name)
                    addProperty("data", it.data)
                }
            }
        }

        inline fun <reified T : Number> JSONConfig.addTestGenericFromJSON() {
            fromJSON { json ->
                require(json is JSONObject) { "Must be JSONObject" }
                val data = JSONDeserializer.deserialize<T>(json["data"])
                TestGenericClass(
                    name = json["NAME"].asString,
                    data = data,
                )
            }
        }

    }

    interface DummyInterface {
        val name: String
    }

    data class DummyImplementation(
        override val name: String,
        val number: Int,
    ) : DummyInterface

}
