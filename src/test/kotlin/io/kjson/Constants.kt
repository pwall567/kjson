/*
 * @(#) Constants.kt
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

import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType

import java.util.LinkedList

object Constants {

    val stringType = String::class.starProjectedType
    val stringTypeProjection = KTypeProjection.invariant(stringType)
    private val intType = Int::class.starProjectedType
    private val intTypeProjection = KTypeProjection.invariant(intType)

    val listStringType = List::class.createType(listOf(stringTypeProjection))
    val arrayListStringType = ArrayList::class.createType(listOf(stringTypeProjection))
    val linkedListStringType = LinkedList::class.createType(listOf(stringTypeProjection))
    val setStringType = Set::class.createType(listOf(stringTypeProjection))
    val hashSetStringType = HashSet::class.createType(listOf(stringTypeProjection))
    val linkedHashSetStringType = LinkedHashSet::class.createType(listOf(stringTypeProjection))

    val pairStringStringType = Pair::class.createType(listOf(stringTypeProjection, stringTypeProjection))
    val pairStringIntType = Pair::class.createType(listOf(stringTypeProjection, intTypeProjection))
    val tripleStringStringStringType = Triple::class.createType(listOf(stringTypeProjection, stringTypeProjection,
            stringTypeProjection))
    val tripleStringIntStringType = Triple::class.createType(listOf(stringTypeProjection, intTypeProjection,
            stringTypeProjection))

    val listStrings = listOf("abc", "def")
    val jsonArrayString = JSONArray.build {
        add("abc")
        add("def")
    }

    val mapStringIntType = Map::class.createType(listOf(stringTypeProjection, intTypeProjection))
    val linkedHashMapStringIntType = LinkedHashMap::class.createType(listOf(stringTypeProjection, intTypeProjection))

    val mapStringInt = mapOf("abc" to 123, "def" to 456, "ghi" to 789)
    val jsonObjectInt = JSONObject.build {
        add("abc", 123)
        add("def", 456)
        add("ghi", 789)
    }

}
