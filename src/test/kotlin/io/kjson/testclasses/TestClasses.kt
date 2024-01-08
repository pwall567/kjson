/*
 * @(#) TestClasses.kt
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

package io.kjson.testclasses

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

import io.kjson.JSON.asObject
import io.kjson.JSON.asString
import io.kjson.JSONContext
import io.kjson.JSONException
import io.kjson.JSONInt
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.JSONValue
import io.kjson.annotation.JSONAllowExtra
import io.kjson.annotation.JSONIgnore
import io.kjson.annotation.JSONIncludeAllProperties
import io.kjson.annotation.JSONIncludeIfNull
import io.kjson.annotation.JSONName
import io.kjson.optional.Opt

data class Dummy1(val field1: String, val field2: Int = 999)

data class Dummy2(val field1: String, val field2: Int = 999) {
    var extra: String? = null
}

data class Dummy3(val dummy1: Dummy1, val text: String)

data class Dummy4(val listDummy1: List<Dummy1>, val text: String)

data class Dummy5(val field1: String?, val field2: Int)

data class DummyFromJSON(val int1: Int) {

    @Suppress("unused")
    fun toJSON(): JSONObject {
        return JSONObject.build {
            add("dec", int1.toString())
            add("hex", int1.toString(16))
        }
    }

    companion object {
        @Suppress("unused")
        fun fromJSON(json: JSONValue): DummyFromJSON {
            val jsonObject = json.asObject
            val dec = jsonObject["dec"].asString.toInt()
            val hex = jsonObject["hex"].asString.toInt(16)
            if (dec != hex)
                throw JSONException("Inconsistent values")
            return DummyFromJSON(dec)
        }
    }

}

data class DummyFromJSONWithContext(val dummy1: Dummy1) {

    companion object {
        @Suppress("unused")
        fun JSONContext.fromJSON(json: JSONValue): DummyFromJSONWithContext {
            val jsonObject = json as JSONObject
            val dummy1: Dummy1 = deserializeProperty("aaa", jsonObject)
            return DummyFromJSONWithContext(dummy1)
        }
    }

}

data class DummyMultipleFromJSON(val int1: Int) {

    @Suppress("unused")
    fun toJSON(): JSONObject {
        return JSONObject.build {
            add("dec", int1.toString())
            add("hex", int1.toString(16))
        }
    }

    companion object {
        @Suppress("unused")
        fun fromJSON(json: JSONObject): DummyMultipleFromJSON {
            val dec = json["dec"].asString.toInt() // json.getString("dec").toInt()
            val hex = json["hex"].asString.toInt(16) // json.getString("hex").toInt(16)
            if (dec != hex)
                throw JSONException("Inconsistent values")
            return DummyMultipleFromJSON(dec)
        }
        @Suppress("unused")
        fun fromJSON(json: JSONInt): DummyMultipleFromJSON {
            return DummyMultipleFromJSON(json.value)
        }
        @Suppress("unused")
        fun fromJSON(json: JSONString): DummyMultipleFromJSON {
            return DummyMultipleFromJSON(json.value.toInt(16))
        }
    }

}

@Suppress("unused")
enum class DummyEnum { ALPHA, BETA, GAMMA }

open class Super {

    var field1: String = "xxx"
    var field2: Int = 111

    override fun equals(other: Any?): Boolean {
        return other is Super && field1 == other.field1 && field2 == other.field2
    }

    override fun hashCode(): Int {
        return field1.hashCode() xor field2.hashCode()
    }

}

class Derived : Super() {

    var field3: Double = 0.1

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is Derived && field3 == other.field3
    }

    override fun hashCode(): Int {
        return super.hashCode() xor field3.toInt()
    }

}

interface DummyInterface

object DummyObject : DummyInterface {

    @Suppress("unused")
    val field1: String = "abc"

}

class NestedDummy {

    @Suppress("unused")
    val obj = DummyObject

}

class DummyWithVal {

    val field8: String = "blert"

    @Suppress("KotlinConstantConditions")
    override fun equals(other: Any?): Boolean {
        return other is DummyWithVal && other.field8 == field8
    }

    override fun hashCode(): Int {
        return field8.hashCode()
    }

}

class DummyList(content: List<LocalDate>) : ArrayList<LocalDate>(content)

class DummyMap(content: Map<String, LocalDate>) : HashMap<String, LocalDate>(content)

data class DummyWithIgnore(val field1: String, @JSONIgnore val field2: String = "defaulted", val field3: String)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomIgnore

data class DummyWithCustomIgnore(val field1: String, @CustomIgnore val field2: String = "special", val field3: String)

class DummyWithNameAnnotation {

    var field1: String = "xxx"
    @JSONName("fieldX")
    var field2: Int = 111

    override fun equals(other: Any?): Boolean {
        return other is DummyWithNameAnnotation && field1 == other.field1 && field2 == other.field2
    }

    override fun hashCode(): Int {
        return field1.hashCode() xor field2.hashCode()
    }

}

data class DummyWithParamNameAnnotation(val field1: String, @JSONName("fieldX") val field2: Int)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@Suppress("unused")
annotation class CustomName(val symbol: String)

data class DummyWithCustomNameAnnotation(val field1: String, @CustomName("fieldX") val field2: Int)

data class DummyWithIncludeIfNull(val field1: String, @JSONIncludeIfNull val field2: String?, val field3: String)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomIncludeIfNull

data class DummyWithCustomIncludeIfNull(val field1: String, @CustomIncludeIfNull val field2: String?,
        val field3: String)

@JSONIncludeAllProperties
data class DummyWithIncludeAllProperties(val field1: String, val field2: String?, val field3: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomIncludeAllProperties

@CustomIncludeAllProperties
data class DummyWithCustomIncludeAllProperties(val field1: String, val field2: String?, val field3: String)

@JSONAllowExtra
data class DummyWithAllowExtra(val field1: String, val field2: Int)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomAllowExtraProperties

@CustomAllowExtraProperties
data class DummyWithCustomAllowExtra(val field1: String, val field2: Int)

@Suppress("unused")
class MultiConstructor(val aaa: String) {

    constructor(bbb: Int) : this(bbb.toString())

}

open class DummyA

open class DummyB : DummyA()

open class DummyC : DummyB()

class DummyD : DummyC()

data class Dummy9(val str: String) {

    override fun toString(): String {
        return str
    }

}

class Circular1 {
    var ref2: Circular2? = null
}

class Circular2 {
    var ref1: Circular1? = null
}

open class PolymorphicBase

data class PolymorphicDerived1(val type: String, val extra1: Int) : PolymorphicBase()

data class PolymorphicDerived2(val type: String, val extra2: String) : PolymorphicBase()

data class PolymorphicGeneric<out T : PolymorphicBase>(
    val code: String,
    val data: T,
)

data class ConstructLong(val v: Long)

data class ConstructInt(val v: Int)

data class ConstructShort(val v: Short)

data class ConstructByte(val v: Byte)

data class ConstructULong(val v: ULong)

data class ConstructUInt(val v: UInt)

data class ConstructUShort(val v: UShort)

data class ConstructUByte(val v: UByte)

data class ConstructDouble(val v: Double)

data class ConstructFloat(val v: Float)

data class ConstructBigDecimal(val v: BigDecimal)

data class ConstructBigInteger(val v: BigInteger)

typealias MapStringInt = Map<String, Int>

data class TypeAliasData(
    val aaa: String,
    val bbb: MapStringInt,
)

data class OptData(
    val aaa: Opt<Int> = Opt.unset(),
)

data class OptComplexData(
    val aaa: Opt<List<String>>,
)

data class BigHolder(
    val bi: BigInteger,
    val bd: BigDecimal,
)
