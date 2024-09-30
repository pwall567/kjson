package io.kjson.deserialize

data class JavaParameterDescriptor<T>(
    val name: String,
    val deserializer: Deserializer<T>,
    val ignore: Boolean,
)
