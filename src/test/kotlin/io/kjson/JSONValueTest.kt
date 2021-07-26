package io.kjson

import kotlin.test.Test
import kotlin.test.expect
import java.math.BigDecimal

class JSONValueTest {

    @Test fun `should create JSONInt`() {
        val test1 = JSONInt(12345)
        expect(12345) { test1.value }
        expect("12345") { test1.toJSON() }
    }

    @Test fun `should create JSONLong`() {
        val test1 = JSONLong(12345)
        expect(12345) { test1.value }
        expect("12345") { test1.toJSON() }
    }

    @Test fun `should create JSONDecimal`() {
        val testDecimal1 = JSONDecimal(BigDecimal.ZERO)
        expect(BigDecimal.ZERO) { testDecimal1.value }
        expect("0") { testDecimal1.toJSON() }
        val testDecimal2 = JSONDecimal("0.00")
        expect(testDecimal1) { testDecimal2 }
    }

    @Test fun `should use JSONBoolean`() {
        val testBoolean1 = JSONBoolean.TRUE
        expect(true) { testBoolean1.value }
        expect("true") { testBoolean1.toJSON() }
        val testBoolean2 = JSONBoolean.FALSE
        expect(false) { testBoolean2.value }
        expect("false") { testBoolean2.toJSON() }
    }

    @Test fun `should create JSONString`() {
        val testString = JSONString("ab\u2014c\n")
        expect("ab\u2014c\n") { testString.value }
        expect("\"ab\\u2014c\\n\"") { testString.toJSON() }
    }

    @Test fun `should create JSONArray`() {
        val testArray = JSONArray(listOf(JSONInt(123), JSONInt(456)))
        expect(listOf(JSONInt(123), JSONInt(456))) { testArray.value }
        expect("[123,456]") { testArray.toJSON() }
    }

    @Test fun `should use nullable functions`() {
        var testNull: JSONValue? = null
        expect("null") { testNull.toJSON() }
        val sb: StringBuilder = StringBuilder()
        testNull.appendToJSON(sb)
        expect("null") { sb.toString() }
        testNull = createJSONValue()
        expect("222") { testNull.toJSON() }
        sb.setLength(0)
        testNull.appendToJSON(sb)
    }

    @Suppress("RedundantNullableReturnType")
    private fun createJSONValue(): JSONValue? {
        return JSONInt(222)
    }

}
