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

## Serialization

Custom serialization converts the object to a `JSONValue`; the library will convert the `JSONValue` to string form if
that is required.
There are two ways of specifying custom serialization.

### `toJSON` function in the class

If the class of an object to be serialized has a member function named `toJSON`, taking no parameters and returning a
`JSONValue`, that function will be used for serialization.
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

The type may be specified as a type parameter (as above), or as type variable:
```kotlin
    val personType = Person::class.starProjectedType
    config.toJSON(personType) { p ->
        val person = p as Person // this is necessary because there is insufficient type information in this case
        JSONString("${person.firstName}|${person.surname}")
    }
```

The type may also be inferred from the parameter of the lambda:
```kotlin
    config.toJSON { p: Person? ->
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
If the source is a string, it will already have been converted to a structure of `JSONValue` objects.

### `fromJSON` function in the companion object

If the target class has a companion object with a function taking a `JSONValue` and returning an object of the target
class, that function will be used for deserialization.
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
            val names = json.get().split('|')
            require(names.length == 2) { "Person string has incorrect format" }
            return Person(names[0], names[1])
        }
    }

}
```

### `fromJSON` lambda in the `JSONConfig`

Again, it may not be possible to modify the class, so a `fromJSON` lambda may be specified in the `JSONConfig`.
The above example may be specified as:
```kotlin
    config.fromJSON { json ->
        require(json is JSONString) { "Can't deserialize ${json::class} as Person" }
        val names = json.get().split('|')
        require(names.length == 2) { "Person string has incorrect format" }
        Person(names[0], names[1])
    }
```

The result type in this example is implied by the return type of the lambda.
As with `toJSON`, the type may be specified explicitly:
```kotlin
    val personType = Person::class.starProjectedType
    config.fromJSON(personType) { json ->
        require(json is JSONString) { "Can't deserialize ${json::class} as Person" }
        val names = json.get().split('|')
        require(names.length == 2) { "Person string has incorrect format" }
        Person(names[0], names[1])
    }
```

### `fromJSONPolymorphic`

This function (in `JSONConfig`) provides a means of deserializing polymorphic types &ndash; an input object may be
deserialized into one of a number of possible derived types by examining the properties of the object:
```kotlin
 config.fromJSONPolymorphic(Party::class, "type",
        JSONString("PERSON") to typeOf<Person>(),
        JSONString("ORGANIZATION") to typeOf<Organization>()
 )
```

If the object has a property named "type" with a value (string) of "PERSON", the object will be deserialized as a
`Person`, and if the "type" property is "ORGANIZATION" the object will be deserialized as an `Organization`.

2021-08-22
