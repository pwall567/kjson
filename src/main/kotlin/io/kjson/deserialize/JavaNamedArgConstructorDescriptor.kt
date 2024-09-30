package io.kjson.deserialize

import java.lang.reflect.Constructor

import io.kjson.JSONValue

data class JavaNamedArgConstructorDescriptor<T : Any>(
    val resultClass: Class<T>,
    val constructor: Constructor<T>,
    val parameters: List<JavaParameterDescriptor<*>>,
    val fieldDescriptors: List<FieldDescriptor<*>>,
    val allowExtra: Boolean,
) : JavaConstructorDescriptor<T> {

    override fun instantiate(json: JSONValue): T {
        TODO("Not yet implemented")
    }

}
