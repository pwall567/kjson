package io.kjson.parser

class ParseException(val text: String, val pointer: String = "") :
    RuntimeException(if (pointer.isEmpty()) text else "$text at $pointer")
