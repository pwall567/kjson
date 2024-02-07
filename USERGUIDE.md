# User Guide - `kjson`

## Contents

- [Introduction](#introduction)
- [Type Mapping](#type-mapping)
- [Optional Properties](#optional-properties)
- [Configuration](#configuration)
- [Serialize Kotlin object to String](#serialize-kotlin-object-to-string)
- [Deserialize String to Kotlin object](#deserialize-string-to-kotlin-object)
- [Serialize Kotlin object to `JSONValue`](#serialize-kotlin-object-to-jsonvalue)
- [Deserialize `JSONValue` to Kotlin object](#deserialize-jsonvalue-to-kotlin-object)
- [Exceptions](#exceptions)
- [Further Examples](#further-examples)


## Introduction

`kjson` is a reflection-based library that converts Kotlin objects to and from their JSON representations.

It is an evolution of an earlier library [`json-kotlin`](https://github.com/pwall567/json-kotlin); users migrating from
that library will find the functionality of `kjson` almost identical.

The underlying JSON parsing and formatting functionality is provided by the
[`kjson-core`](https://github.com/pwall567/kjson-core) library, which provides functions to parse JSON text into an
internal structure of `JSONValue` nodes, and to convert those nodes back into text.
`kjson` can serialize an arbitrary Kotlin (or Java) object into the `JSONValue` node structure ready for output,
and it can deserialize a node structure into an object of a specified (or implied) Kotlin type.

Most users of the library will not need to know about this two-stage process (string to `JSONValue` followed by
`JSONValue` to object, and vice versa) &mdash; first-time users will probably want to start with serializing Kotlin
objects to string and deserializing strings to Kotlin.
More complex uses are described in the sections covering serializing and deserializing to and from `JSONValue`.

As a native Kotlin library, `kjson` will respect the "nullability" of any object being deserialized;
that is to say, it will not deserialize `null` into a `String?`.
It also has built-in support for Kotlin utility classes such as `Sequence` and `Pair`.

## Type mapping

### Custom Serialization and Deserialization

For all classes, including standard classes, the serialization and deserialization processes will first look for a
custom serialization or deserialization function.
That means that custom serializations and deserializations may be specified for any class, even classes with obvious
default representations (like `UUID` or even `Boolean`).

For more information see [Custom Serialization and Deserialization](CUSTOM.md).

### Standard Types

#### Kotlin

`kjson` has built-in support for the following standard Kotlin types:

| Type            | JSON Representation                               |
|-----------------|---------------------------------------------------|
| `String`        | string                                            |
| `StringBuilder` | string                                            |
| `CharSequence`  | string                                            |
| `Char`          | string (of length 1)                              |
| `CharArray`     | string                                            |
| `Int`           | number                                            |
| `Long`          | number                                            |
| `Short`         | number                                            |
| `Byte`          | number                                            |
| `Double`        | number                                            |
| `Float`         | number                                            |
| `Boolean`       | boolean                                           |
| `Array`         | array                                             |
| `IntArray`      | array                                             |
| `LongArray`     | array                                             |
| `ShortArray`    | array                                             |
| `ByteArray`     | array                                             |
| `DoubleArray`   | array                                             |
| `FloatArray`    | array                                             |
| `BooleanArray`  | array                                             |
| `Collection`    | array                                             |
| `Iterable`      | array                                             |
| `Iterator`      | array                                             |
| `List`          | array                                             |
| `ArrayList`     | array                                             |
| `LinkedList`    | array                                             |
| `Set`           | array                                             |
| `HashSet`       | array                                             |
| `LinkedHashSet` | array                                             |
| `Sequence`      | array                                             |
| `Map`           | object                                            |
| `HashMapMap`    | object                                            |
| `LinkedHashMap` | object                                            |
| `Pair`          | array (of length 2)                               |
| `Triple`        | array (of length 3)                               |
| `Enum`          | string (using name)                               |
| `Duration`      | string (ISO form)                                 |
| `Channel`       | array (output only, using non-blocking functions) |
| `Flow`          | array (output only, using non-blocking functions) |

#### Java

The library also has built-in support for the following standard Java types:

| Type                            | JSON Representation                                   |
|---------------------------------|-------------------------------------------------------|
| `java.lang.StringBuffer`        | string                                                |
| `java.math.BigInteger`          | number ([optionally string](#bigintegerstring))       |
| `java.math.BigDecimal`          | number ([optionally string](#bigdecimalstring))       |
| `java.util.Enumeration`         | array                                                 |
| `java.util.Bitset`              | array of bit indices                                  |
| `java.util.UUID`                | string                                                |
| `java.util.Date`                | string (as yyyy-mm-ddThh:mm:ss.sssZ)                  |
| `java.util.Calendar`            | string (as yyyy-mm-ddThh:mm:ss.sss&#xB1;hh:mm)        |
| `java.sql.Date`                 | string (as yyyy-mm-dd)                                |
| `java.sql.Time`                 | string (as hh:mm:ss)                                  |
| `java.sql.Timestamp`            | string (as yyyy-mm-dd hh:mm:ss.sss)                   |
| `java.time.Instant`             | string (as yyyy-mm-ddThh:mm:ss.sssZ)                  |
| `java.time.LocalDate`           | string (as yyyy-mm-dd)                                |
| `java.time.LocalTime`           | string (as hh:mm:ss.sss)                              |
| `java.time.LocalDateTime`       | string (as yyyy-mm-ddThh:mm:ss.sss)                   |
| `java.time.OffsetTime`          | string (as hh:mm:ss.sss&#xB1;hh:mm)                   |
| `java.time.OffsetDateTime`      | string (as yyyy-mm-ddThh:mm:ss.sss&#xB1;hh:mm)        |
| `java.time.ZonedDateTime`       | string (as yyyy-mm-ddThh:mm:ss.sss&#xB1;hh:mm\[name]) |
| `java.time.Year`                | string (as yyyy)                                      |
| `java.time.YearMonth`           | string (as yyyy-mm)                                   |
| `java.time.MonthDay`            | string (as --mm-dd)                                   |
| `java.time.Duration`            | string (e.g. PT2M)                                    |
| `java.time.Period`              | string (e.g. P3M)                                     |
| `java.net.URI`                  | string                                                |
| `java.net.URL`                  | string                                                |
| `java.util.stream.Stream`       | array                                                 |
| `java.util.stream.IntStream`    | array                                                 |
| `java.util.stream.LongStream`   | array                                                 |
| `java.util.stream.DoubleStream` | array                                                 |

#### `kjson` Internal Types

If you have a property of an object that may be one of a number of different types, you can specify the type of the
property as `JSONValue` (or possibly `JSONValue?`), in which case the deserialization process will simply copy the
internal reference (as described in the [introduction](#introduction)), avoiding unnecessary processing until the type
of the property is known.

For example, a property containing an interest rate may be represented as a number (the rate) or a keyword such as
"MARKET" to indicate the use of the current market rate.  The `Customer` class might be:
```kotlin
data class Customer(
    val id: UUID,
    val name: String,
    val interestRate: JSONValue,
)
```

Then, when reading the data:
```kotlin
    val customer = json.parseJSON<Customer>()
    val rate = if (customer.interestRate is JSONString && customer.interestRate.asString == "MARKET")
        getMarketRate()
    else
        customer.interestRate.asDecimal
```

This technique can be useful when an object may be one of a number of types, and the actual type is determined by
examination of other properties, either within the object itself or elsewhere.
For example:
```kotlin
data class Person(
    val id: UUID,
    val name: String,
    val address: JSONObject,
)
```

Then, when reading:
```kotlin
    val person = json.parseJSON<Person>()
    val address = when (person.address["type"].asString) {
        "postal" -> person.address.fromJSONValue<PostalAddress>()
        "street" -> person.address.fromJSONValue<StreetAddress>()
        else -> throw IllegalArgumentException("Unknown address type - ${person.address["type"]}")
    }
```
An alternative way of handling this requirement may be found in the
[Custom Serialization and Deserialization](CUSTOM.md#fromjsonpolymorphic) Guide.

### Other Types

#### Serialization

Objects of other types are serialized as objects, using the public properties of the object as properties of the JSON
object.
In this case, as well as for the collection types, the serialization functions call themselves recursively, to the depth
required to represent all levels of the object.

#### Deserialization

Deserialization of classes other than those listed above is possibly the most complex aspect of the `kjson` library.

If the JSON is a string and the target class has a constructor that takes a single string parameter (possibly with
additional parameters provided they have default values), that constructor is invoked and the resulting object returned
(this is the mechanism used internally for classes such as `StringBuilder` and `URL`, but it may also be employed for
user-defined classes).

If the target class has a constructor that takes a single numeric parameter (where numeric in this context means a class
that derives from the system class `Number` &ndash; like `Int` or `Long` &ndash; or one of the unsigned integer classes
`UInt`, `ULong` _etc._) and the JSON is a number that matches the parameter type, that constructor is invoked and the
resulting object returned.
Again, the constructor may have additional parameters provided they have default values.

Otherwise, the JSON must be an object, and the deserialization functions will construct an object of the target class,
following this pseudo-code:
```text
create a shortlist of potential constructors
for each public constructor in the target class:
    for each parameter in the constructor (there may be no parameters):
        if there is no property in the JSON object with the same name, and the parameter has no default value,
                    and the parameter is not nullable:
            reject the constructor
    if the constructor has not been rejected add it to the shortlist
if there is no constructor in the shortlist:
    FAILURE - can't construct an object of the target type from the supplied JSON
if there is more than one constructor in the shortlist:
    select the constructor that uses the greatest number of properties from the JSON object
for each parameter in the selected constructor (there may be none in the case of a no-arg constructor):
    if the parameter has a matching property in the JSON:
        invoke the deserialization functions using the target type from the parameter and the property from the JSON
    if the parameter has no matching property in the JSON, and has no default value, but is nullable:
        set the parameter value to null
invoke the constructor using the parameter values derived as above
for each property in the JSON that has not been consumed by the constructor (there may be no such properties):
    locate a property in the instantiated object with the same name
    if no such property exists (and allowExtra not specified):
        FAILURE - can't construct an object of the target type from the supplied JSON
    if a property exists and has a setter (it is a var property):
        invoke the deserialization functions using the target type from the property and the property from the JSON
        invoke the setter function with the resulting value
    if a property exists but has no setter (it is a val property):
        invoke the deserialization functions using the target type from the property and the property from the JSON
        invoke the getter function
        if the value from the getter does not match the value from the deserialization of the JSON property:
            FAILURE - can't construct an object of the target type from the supplied JSON
return the instantiated object
```

The above steps allow for a wide variety of target classes, but in the common case of a Kotlin data class with a single
public constructor and no additional properties in the body of the class, the steps reduce to:
```text
for each parameter in the constructor:
    if the parameter has a matching property in the JSON:
        invoke the deserialization functions using the target type from the parameter and the property from the JSON
    if the parameter has no matching property in the JSON, and has no default value, but is nullable:
        set the parameter value to null
    if the parameter has no matching property in the JSON, the parameter has no default value,
                and the parameter is not nullable:
        FAILURE - can't construct an object of the target type from the supplied JSON
invoke the constructor using the parameter values derived as above
if there are properties in the JSON that have not been consumed by the constructor (and allowExtra not specified):
    FAILURE - can't construct an object of the target type from the supplied JSON
return the instantiated object
```

### `Any?`

When deserialising into the target type `Any?` (either as the initial target class or as a nested class in a recursive
invocation), `kjson` will return the following types:

| Input JSON                                           | Result Class        |
|------------------------------------------------------|---------------------|
| string                                               | `String`            |
| number (that will fit in an `Int`)                   | `Int`               |
| number (longer than `Int`, but will fit in a `Long`) | `Long`              |
| number (other than the above)                        | `BigDecimal`        |
| boolean                                              | `Boolean`           |
| array                                                | `List<Any?>`        |
| object                                               | `Map<String, Any?>` |

When the return class is `List` or `Map`, the type parameter is `Any?` but the actual type will itself be the result of
deserializing into `Any?`, and will therefore be one of the above.

### Objects

A Kotlin `object` may be the target of a deserialization operation (serialization requires no special processing).
The object instance will be returned, and all parameters in the JSON will be processed in the same way as additional
properties (those not used as constructor parameters) in the instantiation of an object &ndash; that is to say, the
values will be checked to be equal to the values in the object instance, and if any are not equal the deserialization
will fail.

### Sealed Classes

The library will handle [Kotlin sealed classes](https://kotlinlang.org/docs/reference/sealed-classes.html), by adding a
discriminator property to the JSON object to allow the deserialization to select the correct derived class.
The Kotlin documentation on sealed classes uses the following example:
```kotlin
    sealed class Expr
    data class Const(val number: Double) : Expr()
    data class Sum(val e1: Expr, val e2: Expr) : Expr()
    object NotANumber : Expr()
```

A `Const` instance from the example will serialize as:
```json
{"class":"Const","number":1.234}
```
and the `NotANumber` object will serialize as:
```json
{"class":"NotANumber"}
```
The discriminator property name (default "class") [may be chosen as required](#sealedclassdiscriminator).

Alternatively, the `JSONDiscriminator` annotation may be added to the sealed class definition, nominating the property
name to be used:
```kotlin
    @JSONDiscriminator("type")
    sealed class Expr
    // etc.
```

Instead of the name of the class, the identifier for the specific type may be nominated using the `@JSONIdentifier`
annotation:
```kotlin
    @JSONIdentifier("CONST")
    data class Const(val number: Double) : Expr()
```

## Optional Properties

The library includes special handling of optional properties defined using the `Opt` class of the
[`kjson-optional`](https://github.com/pwall567/kjson-optional) library.

Take this class as an example:
```kotlin
data class Address(
    val firstName: String,
    val middleName: Opt<String> = Opt.unset(),
    val surname: String,
)
```

When serializing or stringifying an `Address` object, if the `middleName` is set to a `String` value, it will be added
to the JSON as a string.
If it is unset, the property will be omitted from the JSON object.

When deserializing an `Address`, if `middleName` is present it will be expected to be a string, and will be deserialized
as an `Opt<String>`.
If it is not present, `Opt.unset()` will be stored in the`middleName` property of the `Address`.

The above examples use `Opt<String>`, but the `Opt` may be of any type, including complex objects.

The special treatment of `Opt` is relevant only when it is a property of an object, where it can be used to indicate the
presence or absence of the property.
In all other cases (_e.g._ as an array item, or as the main target class) the `Opt` class will serialize or deserialize
as the parameter type class, using `null` for output of an unset value, and converting `null` on input to unset.

See the [`kjson-optional`](https://github.com/pwall567/kjson-optional) library for more details.

## Configuration

Operation of the `kjson` library may be parameterised by the use of the `JSONConfig` class.
This form of configuration (as opposed to constructing and tailoring a converter object which then performs the
serialization and deserialization) has a number of advantages:
- simple uses of the library (the majority) may be expressed more concisely
- the pattern allows for the use of extension functions (e.g. `string.parseJSON(config)`)

Most of the serialization and deserialization functions take a `JSONConfig` as a parameter, and use
`JSONConfig.defaultConfig` as the default if the parameter is omitted.

To create a custom `JSONConfig`:
```kotlin
    val config = JSONConfig {
        // make changes to default settings as required, for example:
        includeNulls = true
    }
```
The constructor parameter is a block which is executed with the `JSONConfig` as `this` to initialise it.
The examples below assume that the `JSONConfig` has been created previously with the name `config`; if these
configuration options are being applied in the initialisation block, they do not need the `config.`.

### `includeNulls`

The default behaviour is to omit `null` fields when serializing an object.
To specify that all fields are to be output, even if `null`, set the `includeNulls` variable to `true`:
```kotlin
    config.includeNulls = true
```

### `allowExtra`

When deserializing a JSON object into an instance of a class, `kjson` will try to find a constructor parameter or
an instance variable for each property in the object, and will raise an exception if no matching parameter or property
exists.
To allow non-matching properties to be ignored, the `allowExtra` variable may be set to `true`:
```kotlin
    config.allowExtra = true
```

### `bigDecimalString`

By default `BigDecimal` objects will be serialized to and deserialized from JSON numbers.
To use strings, the `bigDecimalString` variable may be set to `true`:
```kotlin
    config.bigDecimalString = true
```

### `bigIntegerString`

By default `BigInteger` objects will be serialized to and deserialized from JSON numbers.
To use strings, the `bigIntegerString` variable may be set to `true`:
```kotlin
    config.bigIntegerString = true
```

### `stringifyNonASCII`

The default serialization of strings (both string values and object property names) will output Unicode sequences for
all characters above the ASCII subset, that is, all characters above `0x7E`.
This to ensure that the JSON will be read correctly even if the receiving software does not use the correct character
set to decode the data.
To output the full range of Unicode characters, the `stringifyNonASCII` variable may be set to `true`:
```kotlin
    config.stringifyNonASCII = true
```

### `stringifyInitialSize`

When using the "stringify" functions, a buffer is allocated to build the output.
This buffer will be extended as required, but the process may be more efficient if the buffer size is adequate for most
uses, while not being too large.
The default initial buffer size is 1024, but the size may be set by:
```kotlin
    config.stringifyInitialSize = 2048
```

### `sealedClassDiscriminator`

The default name for the discriminator property added to sealed classes is `class`.
This may be changed using the `sealedClassDiscriminator` variable:
```kotlin
    config.sealedClassDiscriminator = "?" // note that it does not need to be an alphanumeric name
```

### `fromJSON`

Function to add a custom deserialization lambda.
See [Custom Serialization and Deserialization](CUSTOM.md#fromjson-lambda-in-the-jsonconfig).

### `toJSON`

Function to add a custom serialization lambda.
See [Custom Serialization and Deserialization](CUSTOM.md#tojson-lambda-in-the-jsonconfig).

### `parseOptions`

By default, the library will use strict JSON parsing.
To use the [lenient parsing options](https://github.com/pwall567/kjson-core#lenient-parsing) in the
[`kjson-core`](https://github.com/pwall567/kjson-core) library, set the `parseOptions` to the required settings:
```kotlin
    config.parseOptions = ParseOptions(arrayTrailingComma = true)
```

### `defaultConfig`

The `defaultConfig` object that will be used when no `JSONConfig` is specified may be modified just like any other
`JSONConfig`.
To make a configuration option apply to all uses of the library in an application, you can set the value in the
default object and it will take effect for all functions where a `JSONConfig` is not specified explicitly, for example:
```kotlin
    JSONConfig.defaultConfig.allowExtra = true
```
If you have made such changes to the `defaultConfig`, you can still override them by specifying a "clean" `JSONConfig`
in the function call:
```kotlin
    val person: Person? = json.parseJSON(JSONConfig())
```

### Annotations

Many of the above options may be applied to an individual property or to a class by the use of annotations.
See the [kjson-annotations](https://github.com/pwall567/kjson-annotations) project for more details.


## Serialize Kotlin object to String

### Extension Functions

The simplest way to convert any object to JSON is to use the `stringifyJSON()` extension function on the object itself
(the receiver for the function is `Any?`, so it may even be applied to `null`):
```kotlin
    val obj = listOf("ABC", "DEF")
    println(obj.stringifyJSON())
```
This will output:
```json
["ABC","DEF"]
```
If required, a `JSONConfig` may be provided as a parameter to the function.

Alternatively, if you have an `Appendable` (for example, a `StringBuilder` or a `Writer`), you can serialize an object
directly using the `appendJSON()` extension function on the `Appendable`:
```kotlin
    File("path.to.file").writer().use {
        it.appendJSON(objectToBeSerialized)
    }
```
This avoids allocating a `StringBuilder` to hold the serialized data before it is output.
An optional `JSONConfig` may be supplied as a second parameter to the function.

### The `JSONStringify` object

For those who prefer to call a function with the object to be serialized as a parameter, the `JSONStringify` object has
a function `stringify` for this purpose (the word "stringify" is borrowed from the JavaScript implementation of JSON):
```kotlin
    val person = Person(surname = "Smith", firstName = "Bill")
    println(JSONStringify.stringify(person))
```
If required, a `JSONConfig` may be supplied as a second parameter to the function.


## Deserialize String to Kotlin object

Deserialization in all its forms requires the target type to be specified in some way.
In many cases the target type may be determined through Kotlin type inference, but there are also functions that allow
the target type to be specified explicitly as a `KType`, a `KClass` or even a Java `Class` or `Type`.

All of these functions will return `null` if the JSON string is `"null"`, and they all take a `JSONConfig` as an
optional final (or only) parameter.

The examples in this section assume the input is a JSON string as follows:
```kotlin
    val json = """{"surname":"Smith","firstName":"Bill"}"""
```

### Extension Functions

The extension function `parseJSON()` may be applied to a `CharSequence` (this is an interface that is implemented by
`String`, `StringBuilder` and other classes, so the extension function may be applied to any of those classes).
There are several forms of the function, allowing the target type to be specified in different ways:

#### Implied type

The simplest form of the function is the one that uses the implied type:
```kotlin
    val person: Person = json.parseJSON()
```

#### Kotlin reified type

The same function may be invoked using the target type as type parameter:
```kotlin
    val person = json.parseJSON<Person>()
```

#### Explicit `KClass`

The class may be specified as a parameter:
```kotlin
    val person = json.parseJSON(Person::class)
```
In this case, there is no way of specifying the nullability of the result, and the type of the result of this example
will be `Person?`.

#### Explicit `KType`

The type may be specified as a parameter (note that in this case, the type parameter does not convey enough information
to determine the result type at compile time, so an `as` construction is required):
```kotlin
    val person = json.parseJSON(Person::class.starProjectedType) as Person
```


## Serialize Kotlin object to `JSONValue`

### The `JSONSerializer` object

The `JSONSerializer` object is intended principally for use internally within the `kjson` library itself
(hence the not very user-friendly name).
It has a single public function `serialize()`, which takes the object to be serialized and returns a `JSONValue`
(or `null` if the input is `null`):
```kotlin
    val value: JSONValue? = JSONSerializer.serialize(anyObject)
```
As always, a `JSONConfig` may be specified as an optional final parameter.


## Deserialize `JSONValue` to Kotlin object

As with the deserialization of `String` to a Kotlin object, the functions will return `null` if the JSON string is
`"null"`, and they all take a `JSONConfig` as an optional final (or only) parameter.

The examples in this section assume the input is a `JSONValue` resulting from the following `parse()` operation
(see [`kjson-core`](https://github.com/pwall567/kjson-core) for more details):
```kotlin
    val jsonValue = JSON.parse("""{"surname":"Smith","firstName":"Bill"}""")
```

### Extension Functions

There are several forms of the `fromJSONValue()` extension function which may be applied to a `JSONValue` (actually a
`JSONValue?`, so the receiver may be null).

#### Implied type

As with deserialization from string, the simplest form of the function is the one that uses the implied type:
```kotlin
    val person: Person = jsonValue.fromJSONValue()
```

#### Kotlin reified type

The same function may be called with the target type as an explicit type parameter:
```kotlin
    val person = jsonValue.fromJSONValue<Person>()
```

#### Explicit `KClass`

The class may be specified as a parameter:
```kotlin
    val person = jsonValue.fromJSONValue(Person::class)
```

#### Explicit `KType`

The type may be specified as a parameter (see the note about the `as` clause in the description of the string function):
```kotlin
    val person = jsonValue.fromJSONValue(Person::class.starProjectedType) as Person
```

### The `JSONDeserializer` object

Like `JSONSerializer`, the `JSONDeserializer` object is intended principally for internal use within `kjson`.
And as with all the other forms of deserialization, there are several options for specifying the target type.
The `JSONDeserializer` object also has some additional functions to help with common cases.

#### Implied type

Again, the simplest form is the one that uses the implied type:
```kotlin
    val person: Person = JSONDeserializer.deserialize(jsonValue)
```

#### Kotlin reified type

And again, the same function may be called with the target type as an explicit type parameter:
```kotlin
    val person = JSONDeserializer.deserialize<Person>(jsonValue)
```

#### Explicit `KClass`

The class may be specified as a parameter:
```kotlin
    val person = JSONDeserializer.deserialize(Person::class, jsonValue)
```

#### Explicit `KType`

The type may be specified as a parameter (again, see the note about the `as` clause in the description of the string
function):
```kotlin
    val person = JSONDeserializer.deserialize(Person::class.starProjectedType, jsonValue) as Person
```

#### Java `Type`

The Java type may also be specified as a parameter (Java `Class` is a subclass of `Type`, but like `KType`, `Type` does
not convey sufficient information to determine the result type so `as` is required):
```kotlin
    val person = JSONDeserializer.deserialize(Person::class.java, jsonValue) as Person
```

#### `deserializeNonNull`

The `deserializeNonNull` function deserializes a `JSONValue` to specified `KClass`, throwing an exception if the result
would be `null`:
```kotlin
    val person = JSONDeserializer.deserializeNonNull(Person::class, jsonValue)
```
Only the form of the function taking a `KClass` is provided; in the case of a `KType` the nullability of the target is
specified in the `KType` itself.

#### `deserializeAny`

This is a shortcut form of the `deserialize` function when supplied with a type parameter of `Any?`.
See [here](#any) for the effect of specifying a target type of `Any?`.


## Exceptions

Most errors in serialization or deserialization cause a `JSONKotlinException` to be thrown.
The exception has the following available properties:

| Name      | Type          | Description                                                                        |
|-----------|---------------|------------------------------------------------------------------------------------|
| `text`    | `String`      | The specific text for the exception, not including pointer or `cause`              |
| `pointer` | `JSONPointer` | A [JSON Pointer](https://tools.ietf.org/html/rfc6901) to the location of the issue |
| `message` | `String`      | The conventional exception message, including the pointer                          |
| `cause`   | `Throwable`   | The original cause of the exception                                                |


## Environment Variables

Some of the defaults used by the library may be set using environment variables:

| Environment Variable                       | Type      | Default   | Used as the initial value for                                                                                       |
|--------------------------------------------|-----------|-----------|---------------------------------------------------------------------------------------------------------------------|
| `io.kjson.defaultAllowExtra`               | `Boolean` | `false`   | [`allowExtra`](#allowextra)                                                                                         |
| `io.kjson.defaultBigDecimalString`         | `Boolean` | `false`   | [`bigDecimalString`](#bigdecimalstring)                                                                             |
| `io.kjson.defaultBigIntegerString`         | `Boolean` | `false`   | [`bigIntegerString`](#bigintegerstring)                                                                             |
| `io.kjson.defaultIncludeNulls`             | `Boolean` | `false`   | [`includeNulls`](#includenulls)                                                                                     |
| `io.kjson.defaultStringifyNonASCII`        | `Boolean` | `false`   | [`stringifyNonASCII`](#stringifynonascii)                                                                           |
| `io.kjson.defaultStringifyInitialSize`     | `Int`     | `1024`    | [`stringifyInitialSize`](#stringifyinitialsize)                                                                     |
| `io.kjson.defaultSealedClassDiscriminator` | `String`  | `"class"` | [`sealedClassDiscriminator`](#sealedclassdiscriminator)                                                             |
| `io.kjson.objectKeyDuplicate`              | `String`  | `"ERROR"` | [`parseOptions`](#parseoptions).[`objectKeyDuplicate`](https://github.com/pwall567/kjson-core#objectkeyduplicate)   |
| `io.kjson.objectKeyUnquoted`               | `Boolean` | `false`   | [`parseOptions`](#parseoptions).[`objectKeyUnquoted`](https://github.com/pwall567/kjson-core#objectkeyunquoted)     |
| `io.kjson.objectTrailingComma`             | `Boolean` | `false`   | [`parseOptions`](#parseoptions).[`objectTrailingComma`](https://github.com/pwall567/kjson-core#objecttrailingcomma) |
| `io.kjson.arrayTrailingComma`              | `Boolean` | `false`   | [`parseOptions`](#parseoptions).[`arrayTrailingComma`](https://github.com/pwall567/kjson-core#arraytrailingcomma)   |

## Further Examples

### Conditional Deserialization

A common requirement is to deserialize an input object into one of a number of child classes in a hierarchy, depending
on a value in the JSON itself.
(Kotlin sealed classes provide one mechanism for this, but their use is not always appropriate.)

An easy way to accomplish this is to deserialize the input JSON into a `JSONValue` structure, and then examine values
directly in that structure to determine the specific target type.

For example:
```kotlin
    val json = JSON.parseObject(inputString) ?: throw NullPointerException("JSON must not be null")
    val event = when (json["type"].asString) {
        // the asString above is necessary because json["type"] returns a JSONValue
        "open" -> JSONDeserializer.deserialize<OpenEvent>(json)
        "close" -> JSONDeserializer.deserialize<CloseEvent>(json)
        else -> throw JSONException("Unknown event type")
    }
```

(Note that the above case is now handled automatically by the [`fromJSONPolymorphic`](CUSTOM.md#fromjsonpolymorphic)
function.)

### Working with Spring

Many users will wish to use `kjson` in conjunction with the
[Spring Framework](https://spring.io/projects/spring-framework).
An example `Service` class to provide default JSON serialization and deserialization for Spring applications is shown in
the [Spring and `kjson`](SPRING.md) guide.

**UPDATE:** the [`kjson-spring`](https://github.com/pwall567/kjson-spring) library now provides a simple way to
integrate with Spring.

2024-02-05
