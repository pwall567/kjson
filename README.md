# kjson

[![Build Status](https://travis-ci.com/pwall567/kjson.svg?branch=main)](https://app.travis-ci.com/github/pwall567/kjson)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.6.10&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.6.10)
[![Maven Central](https://img.shields.io/maven-central/v/io.kjson/kjson?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.kjson%22%20AND%20a:%22kjson%22)

Reflection-based JSON serialization and deserialization for Kotlin.

This library is an evolution of the [json-kotlin](https://github.com/pwall567/json-kotlin) library.
Users of that library should find the transition relatively painless &ndash; in most cases just a change to the
dependency specifications and the `import` statements.

This document provides introductory information on the `kjson` library; fuller information is available in the
[User Guide](USERGUIDE.md).

## Background

This library provides JSON serialization and deserialization functionality for Kotlin.
It uses Kotlin reflection to serialize and deserialize arbitrary objects, and it includes code to handle most of the
Kotlin standard library classes.

When instantiating deserialized objects it does not require the class to have a no-argument constructor, and unlike some
JSON libraries it does not use the `sun.misc.Unsafe` class to force instantiation (which bypasses constructor validity
and consistency checks).

**New in Version 3.0** &ndash; the library now supports non-blocking output.
The `JSONCoStringify` object and the `coStringifyJSON` function allow the full functionality of the library to be used
in a coroutine setting.

## Supported Classes

Support is included for the following standard Kotlin classes:

- `String`, `StringBuilder`, `CharSequence`, `Char`, `CharArray`
- `Int`, `Long`, `Short`, `Byte`, `Double`, `Float`, `UInt`, `ULong`, `UShort`, `UByte`
- `Array`, `IntArray`, `LongArray`, `ShortArray`, `ByteArray`, `DoubleArray`, `FloatArray`
- `Boolean`, `BooleanArray`
- `Collection`, `List`, `ArrayList`, `LinkedList`, `Set`, `HashSet`, `LinkedHashSet`, `Sequence`
- `Map`, `HashMap`, `LinkedHashMap`
- `Pair`, `Triple`
- `Enum`
- `Duration`
- `Channel`, `Flow` (from version 3.0 on; output using `coStringifyJSON()` or `JSONCoStringify` only)

Also, support is included for the following standard Java classes:

- `java.math.BigDecimal`, `java.math.BigInteger`
- `java.net.URI`, `java.net.URL`
- `java.util.Enumeration`, `java.util.Bitset`, `java.util.UUID`, `java.util.Date`, `java.util.Calendar`
- `java.sql.Date`, `java.sql.Time`, `java.sql.Timestamp`
- `java.time.Instant`, `java.time.LocalDate`, `java.time.LocalTime`, `java.time.LocalDateTime`,
  `java.time.OffsetTime`, `java.time.OffsetDateTime`, `java.time.ZonedDateTime`, `java.time.Year`,
  `java.time.YearMonth`, `java.time.MonthDay`, `java.time.Duration`, `java.time.Period`
- `java.util.stream.Stream`, `java.util.stream.IntStream`, `java.util.stream.LongStream`,
  `java.util.stream.DoubleStream`

## Quick Start

### Serialization

To serialize any object (say, a `data class`):
```kotlin
    val json = dataClassInstance.stringifyJSON()
```
The result `json` is a `String` serialized from the object, recursively serializing any nested objects, collections
etc.
The JSON object will contain serialized forms of all of the properties of the object (as declared in `val` and `var`
statements).

For example, given the class:
```kotlin
    data class Example(val abc: String, val def: Int, val ghi: List<String>)
```
and the instantiation:
```kotlin
    val example = Example("hello", 12345, listOf("A", "B"))
```
then
```kotlin
    val jsonString = example.stringifyJSON()
```
will yield:
```json
{"abc":"hello","def":12345,"ghi":["A","B"]}
```

And starting with version 3.0 of this library, any object may be output to a non-blocking destination:
```kotlin
    example.coStringifyJSON { ch -> nonBlockingFunction(ch) }
```

### Deserialization

Deserialization is slightly more complicated, because the target data type must be specified to the function.
This can be achieved in a number of ways (the following examples assume `jsonString` is a `String` containing JSON):

The type can be inferred from the context:
```kotlin
    val example: Example? = jsonString.parseJSON()
```

The type may be specified as a type parameter:
```kotlin
    val example = jsonString.parseJSON<Example>()
```

The type may be specified as a `KClass`:
```kotlin
    val example = jsonString.parseJSON(Example::class)
```

The type may be specified as a `KType`:
```kotlin
    val example = jsonString.parseJSON(Example::class.starProjectedType) as Example
```
(This form is generally only needed when deserializing parameterized types where the parameter types can not be
inferred; the `as` expression is needed because `KType` does not convey inferred type information.)

## Sealed Classes

The library includes special handling for Kotlin sealed classes.
See the [User Guide](USERGUIDE.md#sealed-classes) for more details.

## Customization

### Annotations

#### Change the name used for a property

When serializing or deserializing a Kotlin object, the property name discovered by reflection will be used as the name
in the JSON object.
An alternative name may be specified if required, by the use of the `@JSONName` annotation:
```kotlin
    data class Example(val abc: String, @JSONName("xyz") val def: Int)
```

#### Ignore a property on serialization

If it is not necessary (or desirable) to output a particular field, the `@JSONIgnore` annotation may be used to prevent
serialization:
```kotlin
    data class Example(val abc: String, @JSONIgnore val def: Int)
```

#### Include properties when null

If a property is `null`, the default behaviour when serializing is to omit the property from the output object.
If this behaviour is not desired, the property may be annotated with the `@JSONIncludeIfNull` annotation to indicate
that it is to be included even if `null`:
```kotlin
    data class Example(@JSONIncludeIfNull val abc: String?, val def: Int)
```

To indicate that all properties in a class are to be included in the output even if null, the
`@JSONIncludeAllProperties` may be used on the class:
```kotlin
    @JSONIncludeAllProperties
    data class Example(val abc: String?, val def: Int)
```

And to specify that all properties in all classes are to be output if null, the `includeNulls` flag may be set in the
`JSONConfig`:
```kotlin
    val config = JSONConfig {
        includeNulls = true
    }
    val json = example.stringifyJSON(config)
```

#### Allow extra properties in a class to be ignored

The default behaviour when extra properties are found during deserialization is to throw an exception.
To allow (and ignore) any extra properties, the `@JSONAllowExtra` annotation may be added to the class:
```kotlin
    @JSONAllowExtra
    data class Example(val abc: String, val def: Int)
```

To allow (and ignore) extra properties throughout the deserialization process, the `allowExtra` flag may be set in the
`JSONConfig`:
```kotlin
    val config = JSONConfig {
        allowExtra = true
    }
    val json = example.stringifyJSON(config)
```

#### Using existing tags from other software

If you have classes that already contain annotations for the above purposes, you can tell `kjson` to use those
annotations by specifying them in a `JSONConfig`:
```kotlin
    val config = JSONConfig {
        addNameAnnotation(MyName::class, "name")
        addIgnoreAnnotation(MyIgnore::class)
        addIncludeIfNullAnnotation(MyIncludeIfNull::class)
        addIncludeAllPropertiesAnnotation(MyIncludeAllProperties::class)
        addAllowExtraPropertiesAnnotation(MyAllowExtraProperties::class)
    }
    val json = example.stringifyJSON(config)
```

The `JSONConfig` may be supplied as an optional final argument on most `kjson` function calls (see the
[User Guide](USERGUIDE.md) or the KDoc or source for more details).

### Custom Serialization

The `JSONConfig` is also used to specify custom serialization:
```kotlin
    val config = JSONConfig {
        toJSON<Example> { obj ->
            obj?.let {
                JSONObject.build {
                    add("custom1", it.abc)
                    add("custom2", it.def)
                }
            }
        }
    }
```
Or deserialization:
```kotlin
    val config = JSONConfig {
        fromJSON { json ->
            require(json is JSONObject) { "Must be JSONObject" }
            Example(json["custom1"].asInt, json["custom2"].asInt)
        }
    }
```

The `toJSON` function must supply a lambda with the signature `(Any?) -> JSONValue?` and the `fromJSON` function must
supply a lambda with the signature `(JSONValue?) -> Any?`.
`JSONValue` is the interface implemented by each node in the [`kjson-core`](https://github.com/pwall567/kjson-core)
library (see below).

Both `toJSON` and `fromJSON` may be specified repeatedly in the same `JSONConfig` to cover multiple classes.

## More Detail

The deserialization functions operate as a two-stage process.
The JSON string is first parsed into an internal form using the [`kjson-core`](https://github.com/pwall567/kjson-core)
library; the resulting tree of `JSONValue` objects is then traversed to create the desired classes.

It is possible to perform serialization using the same two-stage approach, but it is generally more convenient to use
the `JSONStringify` functions to stringify direct to a string, or, if the `appendJSON()` function is used, to any form
of `Appendable` including the various `Writer` classes.
As always, the KDoc, the source or the unit test classes provide more information.

This information is of significance when custom serialization and deserialization are required.
Regardless of whether the `JSONStringify` functions are used to output directly to a string, if custom serialization is
used it is still required to create the internal `JSONValue`-based form.
This ensures that errant serialization functions don&rsquo;t disrupt the remainder of the JSON, for example by omitting
a trailing quote or bracket character.

See the [Custom Serialization and Deserialization](CUSTOM.md) guide for more information.

## Dependency Specification

The latest version of the library is 3.1, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>kjson</artifactId>
      <version>3.1</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'io.kjson:kjson:3.1'
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:kjson:3.1")
```

Peter Wall

2022-06-23
