package io.kjson

import java.math.BigDecimal

// Is this really worth the effort?

// If a value class object is passed to a function as an implementation of an interface it will be "boxed" anyway
// (and that is likely to be a frequent or even predominant usage).

interface JSONValue {
    fun appendToJSON(a: Appendable)
    fun toJSON(): String = StringBuilder().also { appendToJSON(it) }.toString()
}

@JvmInline
value class JSONInt(val value: Int) : JSONValue {

    override fun toString(): String = value.toString()

    override fun appendToJSON(a: Appendable) {
        a.append(value.toString())
    }

    operator fun component1(): Int = value

}

@JvmInline
value class JSONLong(val value: Long) : JSONValue {

    override fun toJSON(): String = value.toString()

    override fun appendToJSON(a: Appendable) {
        a.append(value.toString())
    }

}

data class JSONDecimal(val value: BigDecimal) : JSONValue {

    // value classes can't have "equals" functions, so this has to be a data class

    constructor(str: String): this(BigDecimal(str))

    override fun toJSON(): String = value.toString()

    override fun appendToJSON(a: Appendable) {
        a.append(value.toString())
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is JSONDecimal && value.compareTo(other.value) == 0

    override fun hashCode(): Int = value.hashCode()

}

@JvmInline
value class JSONBoolean private constructor(val value: Boolean) : JSONValue {

    override fun toJSON(): String = value.toString()

    override fun appendToJSON(a: Appendable) {
        a.append(value.toString())
    }

    companion object {
        val TRUE = JSONBoolean(true)
        val FALSE = JSONBoolean(false)
    }

}

@JvmInline
value class JSONString(val value: String) : JSONValue {

    override fun appendToJSON(a: Appendable) {
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

@JvmInline
value class JSONArray(val value: List<JSONValue?>) : JSONValue {

    override fun appendToJSON(a: Appendable) {
        a.append('[')
        if (value.isNotEmpty()) {
            val iterator = value.iterator()
            while (true) {
                iterator.next().appendToJSON(a)
                if (!iterator.hasNext())
                    break
                a.append(',')
            }
        }
        a.append(']')
    }

}

@JvmInline
value class JSONObjectX(val value: Map<String, JSONValue?>) : JSONValue {

    override fun appendToJSON(a: Appendable) {
        a.append('{')
        if (value.isNotEmpty()) {
            val iterator = value.entries.iterator()
            while (true) {
                val entry = iterator.next()
                JSONString(entry.key).appendToJSON(a)
                a.append(':')
                entry.value.appendToJSON(a)
                if (!iterator.hasNext())
                    break
                a.append(',')
            }
        }
        a.append('}')
    }

}

fun JSONValue?.toJSON(): String = this?.toJSON() ?: "null"

fun JSONValue?.appendToJSON(a: Appendable) {
    if (this == null)
        a.append("null")
    else
        appendToJSON(a)
}

fun Appendable.appendJSON(json: JSONValue?): Appendable {
    if (json == null)
        append("null")
    else
        json.appendToJSON(this)
    return this
}
