package io.kjson.deserialize

import java.lang.reflect.Constructor

import io.kjson.JSONValue

data class JavaNoArgConstructorDescriptor<T : Any>(
    val resultClass: Class<T>,
    val constructor: Constructor<T>,
    val fieldDescriptors: List<FieldDescriptor<*>>,
    val allowExtra: Boolean,
) : JavaConstructorDescriptor<T> {

    override fun instantiate(json: JSONValue): T {
        TODO("Not yet implemented")
    }

}
