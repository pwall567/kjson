# `kjson` 8

## Background

`kjson` version 8.0 is the result of a major restructure of the `kjson` library.
This new version is expected to out-perform the previous version significantly, while retaining all the capability of
the earlier version.

The new version passes all the unit tests created for previous versions, so users can have a high degree of confidence
that it will function identically to its predecessors, but anyone making use of some of the more complex aspects of the
library (in particular custom serialization and deserialization) may notice some differences, for example in error
reporting.

## History

The `kjson` library evolved from an earlier Java library for JSON parsing.
One of the principal attributes of that earlier library was that it out-performed most, if not all, of the competing
JSON serialization / deserialization libraries in popular use, but with the conversion to Kotlin, the focus on
performance was neglected.

Simply converting a Java library to Kotlin will incur some performance penalty, and the use of Kotlin reflection adds
further cost, so a reflection-based Kotlin library was always going to struggle in this regard.

`kjson` has always included automatic serialization and deserialization for a comprehensive set of classes, from the
core Kotlin classes such as `Int` and `String`, to the collection classes like `List` and `Map`, and including most of
the commonly-used standard classes like `UUID` and the `java.time` classes.
The original version of `kjson` used a lengthy sequence of comparisons to determine the function to be used to serialize
or deserialize a JSON value, and this implementation pattern, combined with the inherent performance issues of Kotlin
reflection, caused JSON conversion times to blow out to an unfortunate extent.

## Caching

The new version uses caching to bring about a spectacular improvement in serialization and deserialization performance.
The functions do a hashed lookup to find a previously stored function to perform the conversion, and only when a class
is encountered for the first time do they need to analyse how to serialize or deserialize that class.

## `JSONContext`

The `JSONContext` class was added recently to help with error reporting when serializing or deserializing nested
structures, but unfortunately this only added to the performance problems.
The new version of the library relies on the exception mechanism to add context information to the exception as it
passes up the call stack, thus ensuring that the cost of reporting an error correctly is incurred only when an error
actually occurs &ndash; after all, errors should occur in only a very small percentage of conversion invocations.

The `JSONContext` class is therefore now deprecated, and users of custom serialization or deserialization should use
extension functions on `JSONConfig`, as was the recommendation prior to the introduction of `JSONContext`.

2024-09-01
