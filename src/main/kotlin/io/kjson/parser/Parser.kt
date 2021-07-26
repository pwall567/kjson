package io.kjson.parser

import java.lang.NumberFormatException
import java.math.BigDecimal
import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONLong
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.JSONValue
import net.pwall.text.TextMatcher
import net.pwall.util.ImmutableMap

object Parser {

    const val rootPointer = ""

    const val MAX_INTEGER_DIGITS_LENGTH = 10
    const val EXCESS_CHARS = "Excess characters following JSON"
    const val ILLEGAL_NUMBER = "Illegal JSON number"
    const val ILLEGAL_SYNTAX = "Illegal JSON syntax"
    const val ILLEGAL_KEY = "Illegal key in JSON object"
    const val DUPLICATE_KEY = "Duplicate key in JSON object"
    const val MISSING_CLOSING_BRACE = "Missing closing brace in JSON object"
    const val MISSING_CLOSING_BRACKET = "Missing closing bracket in JSON array"
    const val UNTERMINATED_STRING = "Unterminated JSON string"
    const val ILLEGAL_CHAR = "Illegal character in JSON string"
    const val ILLEGAL_UNICODE_SEQUENCE = "Illegal Unicode sequence in JSON string"
    const val ILLEGAL_ESCAPE_SEQUENCE = "Illegal escape sequence in JSON string"

    fun parse(json: String): JSONValue? {
        val tm = TextMatcher(json)
        val result = parse(tm, rootPointer)
        tm.skip(Parser::isSpaceCharacter)
        if (!tm.isAtEnd)
            throw ParseException(EXCESS_CHARS)
        return result
    }

    private fun parse(tm: TextMatcher, pointer: String): JSONValue? {
        tm.skip(Parser::isSpaceCharacter)

        if (tm.match('{')) {
            var array = ImmutableMap.createArray<String, JSONValue?>(8)
            var index = 0
            tm.skip(Parser::isSpaceCharacter)
            if (!tm.match('}')) {
                while (true) {
                    if (!tm.match('"'))
                        throw ParseException(ILLEGAL_KEY, pointer)
                    val key = parseString(tm, pointer)
                    if (ImmutableMap.containsKey(array, index, key))
                        throw ParseException(DUPLICATE_KEY, pointer)
                    tm.skip(Parser::isSpaceCharacter)
                    if (index == array.size) {
                        val newArray =
                                ImmutableMap.createArray<String, JSONValue?>(array.size + array.size.coerceAtMost(4096))
                        System.arraycopy(array, 0, newArray, 0, array.size)
                        array = newArray
                    }
                    array[index++] = ImmutableMap.entry(key, parse(tm, "$pointer/$key"))
                    tm.skip(Parser::isSpaceCharacter)
                    if (!tm.match(','))
                        break
                    tm.skip(Parser::isSpaceCharacter)
                }
                if (!tm.match('}'))
                    throw ParseException(MISSING_CLOSING_BRACE, pointer)
            }
            return JSONObject(array, index)
        }

        if (tm.match('[')) {
            var array = Array<JSONValue?>(16) { null }
            var index = 0
            tm.skip(Parser::isSpaceCharacter)
            if (!tm.match(']')) {
                while (true) {
                    if (index == array.size) {
                        val newArray = Array(array.size + array.size.coerceAtMost(4096)) { n ->
                            if (n < array.size) array[n] else null
                        }
                        array = newArray
                    }
                    array[index] = parse(tm, "$pointer/$index")
                    index++
                    tm.skip(Parser::isSpaceCharacter)
                    if (!tm.match(','))
                        break
                }
                if (!tm.match(']'))
                    throw ParseException(MISSING_CLOSING_BRACKET, pointer)
            }
            return JSONArray(array, index)
        }

        if (tm.match('"'))
            return JSONString(parseString(tm, pointer))

        if (tm.match("true"))
            return JSONBoolean.TRUE

        if (tm.match("false"))
            return JSONBoolean.FALSE

        if (tm.match("null"))
            return null

        val numberStart = tm.index
        val negative = tm.match('-')
        if (tm.matchDec(0, 1)) {
            val integerLength = tm.resultLength
            if (integerLength > 1 && tm.resultChar == '0')
                throw ParseException(ILLEGAL_NUMBER, pointer)
            var floating = false
            if (tm.match('.')) {
                floating = true
                if (!tm.matchDec(0, 1))
                    throw ParseException(ILLEGAL_NUMBER, pointer)
            }
            if (tm.match('e') ||tm.match('E')) {
                floating = true
                tm.matchAny("-+") // ignore the result, just step the index
                if (!tm.matchDec(0, 1))
                    throw ParseException(ILLEGAL_NUMBER, pointer)
            }
            if (!floating) {
                if (integerLength < MAX_INTEGER_DIGITS_LENGTH)
                    return JSONInt(tm.getResultInt(negative))
                try {
                    val result = tm.getResultLong(negative)
                    if (result >= Int.MIN_VALUE && result <= Int.MAX_VALUE)
                        return JSONInt(result.toInt())
                    return JSONLong(result)
                }
                catch (ignore: NumberFormatException) {
                    // too big for long - drop through to BigDecimal
                }
            }
            return JSONDecimal(BigDecimal(tm.getString(numberStart, tm.index)))
        }

        throw ParseException(ILLEGAL_SYNTAX, pointer)
    }

    private fun parseString(tm: TextMatcher, pointer: String): String {
        val start = tm.index
        while (true) {
            if (tm.isAtEnd)
                throw ParseException(UNTERMINATED_STRING, pointer)
            when (tm.nextChar()) {
                '"' -> return tm.getString(start, tm.start)
                '\\' -> break
                in '\u0000'..'\u001F' -> throw ParseException(ILLEGAL_CHAR, pointer)
            }
        }
        StringBuilder(tm.getCharSeq(start, tm.start)).apply {
            while (true) {
                if (tm.isAtEnd)
                    throw ParseException(UNTERMINATED_STRING, pointer)
                when (tm.nextChar()) {
                    '"' -> append('"')
                    '\\' -> append('\\')
                    '/' -> append('/')
                    'b' -> append('\b')
                    'f' -> append('\u000C')
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    'u' -> {
                        if (!tm.matchHex(4, 4))
                            throw ParseException(ILLEGAL_UNICODE_SEQUENCE, pointer)
                        append(tm.resultHexInt.toChar())
                    }
                    else -> throw ParseException(ILLEGAL_ESCAPE_SEQUENCE, pointer)
                }
                while (true) {
                    if (tm.isAtEnd)
                        throw ParseException(UNTERMINATED_STRING, pointer)
                    when (val ch = tm.nextChar()) {
                        '"' -> return toString()
                        '\\' -> break
                        in '\u0000'..'\u001F' -> throw ParseException(ILLEGAL_CHAR, pointer)
                        else -> append(ch)
                    }
                }
            }
        }
    }

    private fun isSpaceCharacter(i: Int): Boolean {
        val ch = i.toChar()
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r'
    }

}
