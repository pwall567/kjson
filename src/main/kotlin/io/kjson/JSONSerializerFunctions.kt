/*
 * @(#) JSONSerializerFunctions.kt
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

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.time.Duration

import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.Duration as JavaDuration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.LinkedList
import java.util.UUID

import io.jstuff.util.IntOutput.append4HexLC
import io.jstuff.util.IntOutput.append8HexLC
import io.jstuff.util.MiniSet
import io.kstuff.util.CoIntOutput.output4HexLC
import io.kstuff.util.CoIntOutput.output8HexLC
import io.kstuff.util.CoOutput
import io.kstuff.util.output

/**
 * Utility functions for JSON Serialization.  These functions are not expected to be of use outside the `kjson` family
 * of projects.
 *
 * @author  Peter Wall
 */
object JSONSerializerFunctions {

    private val toStringClasses = setOf(java.sql.Date::class, java.sql.Time::class, java.sql.Timestamp::class,
        ZonedDateTime::class, JavaDuration::class, Period::class, URI::class, URL::class)

    private val uncachedClasses = setOf(Any::class, BigDecimal::class, BigInteger::class,
        ArrayList::class, LinkedList::class, HashMap::class, LinkedHashMap::class, HashSet::class)

    private val finalClasses = setOf(Boolean::class, UInt::class, ULong::class, UShort::class, UByte::class,
        Instant::class, OffsetDateTime::class, OffsetTime::class, LocalDateTime::class,
        LocalDate::class, LocalTime::class, Year::class, YearMonth::class, MonthDay::class, UUID::class, Char::class,
        CharArray::class, Duration::class, IntArray::class, LongArray::class, ByteArray::class, ShortArray::class,
        FloatArray::class, DoubleArray::class, BooleanArray::class)

    private val impossibleClasses = MiniSet.of(Unit::class, Nothing::class, Void::class)

    private val toJsonCache = HashMap<KClass<*>, KFunction<Any?>?>()

    /**
     * Is the class best represented by a string of the `toString()` result?
     */
    fun KClass<*>.isToStringClass() = this in toStringClasses

    /**
     * Is the class a system class that will not have a `toJSON` or `fromJSON` function?
     */
    private fun KClass<*>.isUncachedClass() = this in finalClasses || this in uncachedClasses || this in toStringClasses

    /**
     * Is the class one that can't be deserialized?
     */
    private fun KClass<*>.isImpossible() = this in impossibleClasses

    fun KClass<*>.findToJSON(): KFunction<Any?>? {
        if (isUncachedClass())
            return null
        if (toJsonCache.containsKey(this))
            return toJsonCache[this]
        try {
            for (function in members) {
                if (function is KFunction<*> &&
                        function.name == "toJSON" &&
                        function.parameters.size == 1 &&
                        function.parameters[0].kind == KParameter.Kind.INSTANCE &&
                        function.returnType.isAcceptable())
                    return (function as KFunction<Any?>).also { toJsonCache[this] = it }
            }
        }
        catch (_: Throwable) {
        }
        toJsonCache[this] = null
        return null
    }

    private fun KType.isAcceptable(): Boolean {
        classifier?.let { if (it is KClass<*>) return !it.isImpossible() }
        return true
    }

    fun Appendable.appendUUID(uuid: UUID) {
        val highBits = uuid.mostSignificantBits
        append8HexLC(this, (highBits shr 32).toInt())
        append('-')
        append4HexLC(this, (highBits shr 16).toInt())
        append('-')
        append4HexLC(this, highBits.toInt())
        append('-')
        val lowBits = uuid.leastSignificantBits
        append4HexLC(this, (lowBits shr 48).toInt())
        append('-')
        append4HexLC(this, (lowBits shr 32).toInt())
        append8HexLC(this, lowBits.toInt())
    }

    suspend fun CoOutput.outputUUID(uuid: UUID) {
        val highBits = uuid.mostSignificantBits
        output8HexLC((highBits shr 32).toInt())
        output('-')
        output4HexLC((highBits shr 16).toInt())
        output('-')
        output4HexLC(highBits.toInt())
        output('-')
        val lowBits = uuid.leastSignificantBits
        output4HexLC((lowBits shr 48).toInt())
        output('-')
        output4HexLC((lowBits shr 32).toInt())
        output8HexLC(lowBits.toInt())
    }

}
