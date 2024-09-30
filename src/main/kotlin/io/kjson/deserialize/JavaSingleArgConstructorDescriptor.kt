package io.kjson.deserialize

import java.lang.reflect.Constructor

import io.kjson.JSONValue

data class JavaSingleArgConstructorDescriptor<T : Any>(
    val resultClass: Class<T>,
    val constructor: Constructor<T>,
    val deserializer: Deserializer<*>,
) : JavaConstructorDescriptor<T> {

    override fun instantiate(json: JSONValue): T {
        val value = deserializer.deserialize(json)
        return constructor.newInstance(value)
    }

}
