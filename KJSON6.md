# `kjson` 6

## Background

Prior to version 5.0, the main deserialization functions of the `kjson` library were designed to return a nullable
value.
For example, the principal deserialization function is `parseJSON`:
```kotlin
    inline fun <reified T : Any> CharSequence.parseJSON(config: JSONConfig): T?
```
(Note the question mark on the return type of the function &ndash; whatever the type parameter of the function, the
return type will always be nullable.)

This meant that most deserialization calls had to allow for a possible `null` result, for example:
```kotlin
    val account = json.parseJSON<Account>() ?: throw RuntimeException("Account was \"null\"")
```

The reason for this stemmed from the fact that the implementation was originally converted from an earlier Java library.
In Java, a function returning an object of a nominated class may also return `null`, and it was decided that in order to
maintain compatibility with the Java version, the Kotlin functions would behave similarly.

In retrospect, this can be seen to have been an unfortunate decision, because it meant that the Kotlin library could not
take advantage of the explicit nullability which is an important feature of the Kotlin type system.
In `kjson` version 5 or greater, the type parameter of the conversion functions will be used as specified; if a null
result is expected, the type parameter must be specified as nullable.

A change to the API of this nature is a breaking change; that is, code that was written to use the earlier version may
need to be modified to work with the new version.
In most cases, the only change required will be the removal of a null check on the result of a `parseJSON()` function
call, but the change in the form of error reporting may cause unit tests to require modification.

(This document describes `kjson` version 6.0; an interim version 5.0 implemented most of these changes, but further
changes were found to be advantageous, and these were also breaking changes, so an additional major version number
increment was necessary.)

## Changes

When deserializing JSON into a Kotlin target type, the type may be specified in one of four ways:

- as a Kotlin `KType` (often implied by the context of the operation)
- as a Kotlin `KClass`
- as a Java `Type`
- as a Java `Class`

Of these four, the first is overwhelmingly the most common; it is the form behind these examples:
```kotlin
    val person: Person = json.parseJSON()
```
or:
```kotlin
    val person = json.parseJSON<Person>()
```

This form has been changed to return exactly the type specified; if the type is nullable (_e.g._ `Person?`), the result
may be null, if not the function will report a null value as an error.
The function signature of this function has been changed, so that the IDE and the compiler will now issue warnings if
the call to the function is followed by a null check on the result.

The other three means of specifying the target type do not include any indication of nullability, and therefore all of
those forms will return a potentially-null result.

The new API is expected be simpler to understand, easier to use and more consistent with general Kotlin practice.

2023-07-09
