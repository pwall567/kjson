# Custom Serialization and Deserialization - `kjson`

## Background

The library will use the obvious JSON serializations for the main Kotlin data types &mdash; `String`, `Int`, `Boolean`
_etc._
For arrays and collections, it wil use the JSON array or object syntax, recursively calling the library to process the
items.

Also, there are a number of standard classes for which there is an obvious JSON representation, for example `LocalDate`
and `UUID` (for a complete list, see the [User Guide](USERGUIDE.md#type-mapping)).

But the dominant use of `kjson` is expected to be for the serialization and deserialization of user-defined classes,
particularly Kotlin data classes.
In these cases, the library will serialize or deserialize the Kotlin object as a JSON object using the Kotlin property
names, recursively invoking the library to serialize or deserialize the properties (see the
[User Guide](USERGUIDE.md#deserialization) for more details).

If this is not the required behaviour, custom serialization and deserialization may be used to tailor the JSON
representation to the form needed.

**New in Version 3.0** &ndash; the `toJSON` and `fromJSON` functions are invoked as extension functions on `JSONConfig`.
This allows the function full access to the configuration options, which is particularly useful when invoking the
standard serialization and deserialization functions recursively.
The change should be transparent to most existing uses.

**New in Version 7.0** &ndash; the `toJSON` and `fromJSON` functions are now invoked as extension functions on
`JSONContext`, which provides access to both the `JSONConfig` and a `JSONPointer` describing the current location in a
complex object.
The `JSONContext` also provides `serialize()` and `deserialize()` functions that make use of the context.
The change may be transparent to most many uses, but existing uses that make use of the `JSONConfig` may be improved by
switching to the new functions.

## `kjson-core` Library

Custom serialization and deserialization converts user data structures into structures from the
[`kjson-core`](https://github.com/pwall567/kjson-core) library.
It will be helpful to have some understanding of that library when reading this document.

## `JSONContext`

Most of the custom serialization and deserialization functions are invoked as extension functions on the `JSONContext`
class.
That means that the following properties and functions of `JSONContext` are available to the custom serialization /
deserialization code.

### Properties

The `JSONContext` class exposes two properties:

- `config`: the `JSONConfig` in effect for the current serialization / deserialization process
- `pointer`: a `JSONPointer` representing the current location in the JSON structure (useful for error reporting)

### Functions

It also provides function for creating exceptions that contain the pointer:

- `fatal(message)`
- `fatal(message, cause)`



## Serialization

Serialization may convert directly to a string of JSON, or it may convert to the [`kjson-core`](#kjson-core-library)
internal form.
To simplify custom serialization, only a conversion to the internal form is required; the library will manage the
conversion to a text form if required.
And when the output is being streamed directly to an output stream (including non-blocking output), converting to the
internal form means that the custom serialization doesn't need to allow for all these forms; the library will be
responsible for the final output.

The result of custom serialization is therefore a `JSONValue`, and there are two ways of specifying it.

### `toJSON` function in the class

If the class of an object to be serialized has a public member function named `toJSON`, taking no parameters and
returning a `JSONValue`, that function will be used for serialization.
For example:
```kotlin
class Person(val firstName: String, val surname: String) {

    fun toJSON(): JSONValue {
        return JSONString("$firstName|$surname")
    }

}
```

Note that the result need not be a `JSONObject` (although it generally will be).

### `toJSON` lambda in the `JSONConfig`

There are many cases, for example when the class is part of an external library, when the above technique is not
possible.
Also, some architectural rules may demand that there be no external dependencies in the principal classes of the
published API of a system.
In such cases, a `toJSON` lambda may be specified in the [`JSONConfig`](USERGUIDE.md#configuration).

For example, if the `Person` class above did not have a `toJSON` function:
```kotlin
    config.toJSON<Person> { person ->
        require(person != null)
        JSONString("${person.firstName}|${person.surname}")
    }
```

The type may be specified as a type parameter (as above), or as [KType] value:
```kotlin
    val personType = Person::class.starProjectedType
    config.toJSON(personType) { p ->
        val person = p as Person // this is necessary because there is insufficient type information in this case
        JSONString("${person.firstName}|${person.surname}")
    }
```

The type may also be inferred from the parameter of the lambda:
```kotlin
    config.toJSON { person: Person? ->
        require(person != null)
        JSONString("${person.firstName}|${person.surname}")
    }
```

### `toJSONString`

This is a shortcut for `toJSON` where the lambda simply returns a `JSONString` containing a `toString()` of the object.
For Example:
```kotlin
    config.toJSONString<Person>()
```

## Deserialization

Custom deserialization converts a `JSONValue` to an object of the specified (or implied) type.
If the source is JSON text, it will already have been converted to a structure of `JSONValue` objects.

### Constructor taking `String`

If the target class has a public constructor which will accept a single `String` parameter, and the JSON to be decoded
is a string, then that constructor will be used.
This is the simplest means of providing custom deserialization, and the library uses this mechanism to deserialize some
system classes, for example `java.net.URL`.

### Constructor taking `Number`

If the target class has a public constructor taking a single parameter of one of the system number classes, and the JSON
to be decoded is a number that is valid for the specified number class, then that constructor will be used.

For the purposes of this functionality, the following are considered to be number classes:
- any class derived from the abstract class `Number` (this includes `Int`, `Long` _etc._, as well as `BigDecimal`
  _etc._)
- the unsigned integer classes `UInt`, `ULong`, `UShort` and `UByte`

The types of the input and the constructor parameter need not match exactly, provided that the input may be converted to
the parameter with no loss of precision.
For example, a constructor taking a `Long` will be used if the JSON has a value of 1, and a constructor taking an `Int`
will be used if the JSON is 5.0 (floating-point numbers are accepted as integer if the fractional part is zero).

### `fromJSON` function in the companion object

If the target class has a companion object with a public function named `fromJSON`, taking a `JSONValue` parameter and
returning an object of the target class, that function will be used for deserialization.
For example:
```kotlin
class Person(val firstName: String, val surname: String) {

    fun toJSON(): JSONValue {
        return JSONString("$firstName|$surname")
    }

    companion object {
        @Suppress("unused")
        fun fromJSON(json: JSONValue): DummyFromJSON {
            require(json is JSONString) { "Can't deserialize ${json::class} as Person" }
            val names = json.value.split('|')
            require(names.length == 2) { "Person string has incorrect format" }
            return Person(names[0], names[1])
        }
    }

}
```

The `fromJSON` in the companion object may optionally be specified as an extension function on `JSONContext`.
If this form is used, the context in effect at the time of deserialization is available as `this`, and it may be passed
on to nested deserialization calls.

### `fromJSON` lambda in the `JSONConfig`

Again, it may not be possible to modify the class, so a `fromJSON` lambda may be specified in the `JSONConfig`.
The above example may be specified as:
```kotlin
    config.fromJSON { json ->
        require(json is JSONString) { "Can't deserialize ${json::class} as Person" }
        val names = json.value.split('|')
        require(names.length == 2) { "Person string has incorrect format" }
        Person(names[0], names[1])
    }
```

The result type in this example is implied by the return type of the lambda, although for documentation purposes it may
be helpful to specify it:
```kotlin
    config.fromJSON<Person> { json ->
        require(json is JSONString) { "Can't deserialize ${json::class} as Person" }
        val names = json.value.split('|')
        require(names.length == 2) { "Person string has incorrect format" }
        Person(names[0], names[1])
    }
```

And as with `toJSON`, the type may be specified using a `KType`:
```kotlin
    val personType = Person::class.starProjectedType
    config.fromJSON(personType) { json ->
        require(json is JSONString) { "Can't deserialize ${json::class} as Person" }
        val names = json.value.split('|')
        require(names.length == 2) { "Person string has incorrect format" }
        Person(names[0], names[1])
    }
```

### `fromJSONObject`

Custom deserialization is often required to take a structure that has been output as a JSON object and convert it to a
Kotlin class.
Suppose you have an `Account` class that has no public constructor, only a `create` function, and it is represented in
JSON as an object.
The function `fromJSONObject` specifies that the input must be a `JSONObject` and not any other type of `JSONValue`:
```kotlin
    config.fromJSONObject { json ->
        val name = json["name"].asString
        val number = json["number"].asLong
        val address: Address? = deserializeProperty("address", json)
        Account.create(number, name, address)
    }
```
This also illustrates the use of `deserializeProperty` &ndash; the example assumes that the nested `Address` class can
be deserialized without custom code, and this function recursively invokes the main deserialization process with the
current context (that is, the object on which the `fromJSONObject` lambda is invoked as an extension function) adjusted
to point to the `address` child property of the current object.

If it is necessary to add settings to the `JSONConfig` used in nested calls, the `modifyConfig` function may be used:
```kotlin
         val nestedContext = modifyConfig {
            allowExtra = true
        }
        val address: Address? = nestedContext.deserializeProperty("address", json)
```
The `nestedContext` will have the same pointer as the original, along with a copy of the original `JSONConfig` with the
same settings except for those explicitly modified.

### `fromJSONString`

To specify that a string is input to the custom deserialization (avoiding the need to check the type), the
`fromJSONString` function may be used:
```kotlin
    config.fromJSONString { json ->
        val names = json.value.split('|')
        require(names.length == 2) { "Person string has incorrect format" }
        Person(names[0], names[1])
    }
```
This function allows the lambda to be omitted, in which case a default deserialization function will be used; this looks
for a constructor taking a single `String` parameter and invokes it.
```kotlin
    config.fromJSONString<Amount>()
```
In this case, the result type can not be inferred from the lambda result so it must be specified explicitly.

### `fromJSONPolymorphic`

This function (in `JSONConfig`) provides a means of deserializing polymorphic types &ndash; an input object may be
deserialized into one of a number of possible derived types by examining the properties of the object:
```kotlin
    config.fromJSONPolymorphic<Party>("type",
        "PERSON" to typeOf<Person>(),
        "ORGANIZATION" to typeOf<Organization>(),
    )
```
In this example, the base class `Party` has two derived types `Person` and `Organization`, and the containing class
(say, `Account`) has a reference only to the base class.
If the object has a property named &ldquo;type&rdquo; with a value (string) of &ldquo;PERSON&rdquo;, the object will be
deserialized as a `Person`, and if the &ldquo;type&rdquo; property is &ldquo;ORGANIZATION&rdquo; the object will be
deserialized as an `Organization`.

In some cases, the discriminator field might not be at the top level of the object.
In this case, a `JSONPointer` may be used to specify the location of the discriminator field, relative to the root of
object:
```kotlin
    config.fromJSONPolymorphic<Party>(JSONPointer("/type/name"),
        "PERSON" to typeOf<Person>(),
        "ORGANIZATION" to typeOf<Organization>(),
    )
```
In this example, a property located by the pointer &ldquo;`/type/name`&rdquo; will be tested against the values
specified.

Additional versions of the function take either a `KType` or a `KClass` as the first parameter, instead of using the
type parameter.

2023-10-12
