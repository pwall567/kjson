/*
 * @(#) Deserializer.kt
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

package io.kjson.deserialize

import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.Duration
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
import java.util.BitSet
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream

import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONLong
import io.kjson.JSONNumber
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.JSONValue
import io.kjson.pointer.JSONPointer

sealed interface Deserializer<T> {

    fun deserialize(json: JSONValue?): T?

    fun typeError(expected: String): Nothing {
        throw typeException(expected)
    }

    fun typeError(expected: String, pointer: JSONPointer): Nothing {
        throw typeException(expected, pointer)
    }

    fun typeError(expected: String, index: Int): Nothing {
        throw typeException(expected, JSONPointer.root.child(index))
    }

    fun typeError(expected: String, name: String): Nothing {
        throw typeException(expected, JSONPointer.root.child(name))
    }

    companion object {

        val initialEntries = listOf(
            Any::class to AnyDeserializer,
            Unit::class to ImpossibleSerializer,
            Nothing::class to ImpossibleSerializer,
            Void::class to ImpossibleSerializer,
            Boolean::class to BooleanDeserializer,
            Int::class to IntDeserializer,
            Long::class to LongDeserializer,
            Double::class to DoubleDeserializer,
            Float::class to FloatDeserializer,
            Short::class to ShortDeserializer,
            Byte::class to ByteDeserializer,
            UInt::class to UIntDeserializer,
            ULong::class to ULongDeserializer,
            UShort::class to UShortDeserializer,
            UByte::class to UByteDeserializer,
            BigInteger::class to BigIntegerDeserializer,
            BigDecimal::class to BigDecimalDeserializer,
            Number::class to NumberDeserializer,
            String::class to StringDeserializer,
            StringBuilder::class to StringBuilderDeserializer,
            StringBuffer::class to StringBufferDeserializer,
            Char::class to CharDeserializer,
            CharArray::class to CharArrayDeserializer,
            Array<Char>::class to ArrayCharDeserializer,
            java.sql.Date::class to JavaSQLDateDeserializer,
            java.sql.Time::class to JavaSQLTimeDeserializer,
            java.sql.Timestamp::class to JavaSQLTimestampDeserializer,
            Calendar::class to CalendarDeserializer,
            Date::class to DateDeserializer,
            Instant::class to InstantDeserializer,
            LocalDate::class to LocalDateDeserializer,
            LocalDateTime::class to LocalDateTimeDeserializer,
            LocalTime::class to LocalTimeDeserializer,
            OffsetTime::class to OffsetTimeDeserializer,
            OffsetDateTime::class to OffsetDateTimeDeserializer,
            ZonedDateTime::class to ZonedDateTimeDeserializer,
            Year::class to YearDeserializer,
            YearMonth::class to YearMonthDeserializer,
            MonthDay::class to MonthDayDeserializer,
            Duration::class to JavaDurationDeserializer,
            Period::class to PeriodDeserializer,
            kotlin.time.Duration::class to DurationDeserializer,
            UUID::class to UUIDDeserializer,
            BitSet::class to BitSetDeserializer,
            BooleanArray::class to BooleanArrayDeserializer,
            ByteArray::class to ByteArrayDeserializer,
            DoubleArray::class to DoubleArrayDeserializer,
            FloatArray::class to FloatArrayDeserializer,
            IntArray::class to IntArrayDeserializer,
            LongArray::class to LongArrayDeserializer,
            ShortArray::class to ShortArrayDeserializer,
            IntStream::class to IntStreamDeserializer,
            LongStream::class to LongStreamDeserializer,
            DoubleStream::class to DoubleStreamDeserializer,
            JSONString::class to JSONStringDeserializer,
            JSONInt::class to JSONIntDeserializer,
            JSONLong::class to JSONLongDeserializer,
            JSONDecimal::class to JSONDecimalDeserializer,
            JSONNumber::class to JSONNumberDeserializer,
            JSONBoolean::class to JSONBooleanDeserializer,
            JSONArray::class to JSONArrayDeserializer,
            JSONObject::class to JSONObjectDeserializer,
            JSONValue::class to JSONValueDeserializer,
            URI::class to URIDeserializer,
            URL::class to URLDeserializer,
            // other classes that would use default string constructor in old version?
        )

    }

}
