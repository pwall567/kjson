package io.kjson.deserialize

import io.kjson.JSONValue

interface JavaConstructorDescriptor<T : Any> {

    fun instantiate(json: JSONValue): T

}
