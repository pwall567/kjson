package io.kjson.parser

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.JSONArray

class ParserArrayTest {

    @Test fun `should parse empty array`() {
        val result = Parser.parse("[]")
        assertTrue(result is JSONArray)
        expect(0) { result.value.size }
    }

    @Test fun `should parse array of string`() {
        val result = Parser.parse("""["simple"]""")
        assertTrue(result is JSONArray)
        expect(1) { result.value.size }
    }

}
