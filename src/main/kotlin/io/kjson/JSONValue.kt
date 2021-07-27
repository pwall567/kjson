/*
 * @(#) JSONValue.kt
 *
 * kjson  JSON functions for Kotlin
 * Copyright (c) 2021 Peter Wall
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

import java.math.BigDecimal

sealed interface JSONValue {
    fun appendTo(a: Appendable)
    fun toJSON(): String = StringBuilder().also { appendTo(it) }.toString()
}

@JvmInline
value class JSONInt(val value: Int) : JSONValue {

    override fun toString(): String = value.toString()

    override fun appendTo(a: Appendable) {
        a.append(value.toString())
    }

    operator fun component1(): Int = value

}

@JvmInline
value class JSONLong(val value: Long) : JSONValue {

    override fun toJSON(): String = value.toString()

    override fun appendTo(a: Appendable) {
        a.append(value.toString())
    }

}

data class JSONDecimal(val value: BigDecimal) : JSONValue {

    // value classes can't have "equals" functions, so this has to be a data class

    constructor(str: String): this(BigDecimal(str))

    override fun toJSON(): String = value.toString()

    override fun appendTo(a: Appendable) {
        a.append(value.toString())
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is JSONDecimal && value.compareTo(other.value) == 0

    override fun hashCode(): Int = value.hashCode()

}

@JvmInline
value class JSONBoolean private constructor(val value: Boolean) : JSONValue {

    override fun toJSON(): String = value.toString()

    override fun appendTo(a: Appendable) {
        a.append(value.toString())
    }

    companion object {
        val TRUE = JSONBoolean(true)
        val FALSE = JSONBoolean(false)
    }

}

@JvmInline
value class JSONString(val value: String) : JSONValue {

    override fun appendTo(a: Appendable) {
        a.append('"')
        for (ch in value) {
            when (ch) {
                '"', '\\' -> a.append('\\').append(ch)
                '\b' -> a.append("\\b")
                '\u000C' -> a.append("\\f")
                '\n' -> a.append("\\n")
                '\r' -> a.append("\\r")
                '\t' -> a.append("\\t")
                !in '\u0020'..'\u007E' -> {
                    a.append("\\u")
                    a.append(hexDigits[(ch.code shr 12) and 0xF])
                    a.append(hexDigits[(ch.code shr 8) and 0xF])
                    a.append(hexDigits[(ch.code shr 4) and 0xF])
                    a.append(hexDigits[ch.code and 0xF])
                }
                else -> a.append(ch)
            }
        }
        a.append('"')
    }

    companion object {
        const val hexDigits = "0123456789ABCDEF"
    }

}

fun JSONValue?.toJSON(): String = this?.toJSON() ?: "null"

fun JSONValue?.appendTo(a: Appendable) {
    if (this == null)
        a.append("null")
    else
        appendTo(a)
}

fun Appendable.appendJSON(json: JSONValue?) = apply {
    if (json == null)
        append("null")
    else
        json.appendTo(this)
}
