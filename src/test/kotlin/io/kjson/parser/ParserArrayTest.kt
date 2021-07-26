package io.kjson.parser

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.JSONArray
import io.kjson.JSONString

class ParserArrayTest {

    @Test fun `should parse empty array`() {
        val result = Parser.parse("[]")
        assertTrue(result is JSONArray)
        expect(0) { result.size }
    }

    @Test fun `should parse array of string`() {
        val result = Parser.parse("""["simple"]""")
        assertTrue(result is JSONArray)
        expect(1) { result.size }
        expect(JSONString("simple")) { result[0] }
    }

    @Test fun `should parse array of two strings`() {
        val result = Parser.parse("""["Hello","world"]""")
        assertTrue(result is JSONArray)
        expect(2) { result.size }
        expect(JSONString("Hello")) { result[0] }
        expect(JSONString("world")) { result[1] }
    }

    @Test fun `should parse array of arrays`() {
        val result = Parser.parse("""["Hello",["world","universe"]]""")
        assertTrue(result is JSONArray)
        expect(2) { result.size }
        expect(JSONString("Hello")) { result[0] }
        val inner = result[1]
        assertTrue(inner is JSONArray)
        expect(2) { inner.size }
        expect(JSONString("world")) { inner[0] }
        expect(JSONString("universe")) { inner[1] }
    }

}
