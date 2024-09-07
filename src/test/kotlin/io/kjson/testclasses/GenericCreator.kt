package io.kjson.testclasses

import io.kjson.parseJSON
import io.kjson.stringifyJSON

class GenericCreator<TT : Any> {

    fun createAndStringify(data: TT): String {
        return TestGenericClass("XXX", data).stringifyJSON()
    }

    fun parseString(str: String): TestGenericClass<TT> {
        return str.parseJSON()
    }

}
