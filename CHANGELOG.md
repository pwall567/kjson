# Change Log

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [9.1] - 2024-11-03
### Changed
- `Serializer`, `JSONConfig`: make use of `Channel` amd `Flow` conditional (library is optional)
- `Serializer`: minor fix in serialization of Java classes

## [9.0] - 2024-11-03
### Changed
- `JSONDeserializer`, `deserialize` classes, `JSONConfig`: major restructure of user class deserialization

## [8.3] - 2024-09-07
### Changed
- `JSONDeserializer`, `Serializer`: fixed bug in serializing nested generic classes

## [8.2] - 2024-09-06
### Changed
- `Deserializer`, `OtherDeserializers.kt`: fixed bug in deserializing Java classes `URI`, `URL`

## [8.1] - 2024-09-04
### Changed
- `Serializer`: fixed bug in serializing parameterized classes
- `JSONDeserializer`: fixed code formatting issues
- `JSONContext`, `JSONDeserializerFunctions`, test: added `@Suppress("deprecation")` to references to `JSONContext`
- `pom.xml`: updated dependency version

## [8.0] - 2024-08-30
### Added
- individual deserializer classes/objects in `io.kjson.deserialize`
- individual serializer classes/objects in `io.kjson.serialize`
- `NameValuePair`
- `SizedSequence`
- `TypeMap`
- `reflectionUtils.kt`
- `build.yml`, `deploy.yml`: converted project to GitHub Actions
### Changed
- `JSONConfig`, `JSONCoStringify`, `JSONDeserializer`, `JSONDeserializerFunctions`, `JSONfun`, `JSONKotlinException`,
  `JSONSerializer`, `JSONSerializerFunctions`, `JSONStringify`: major restructure
- `JSONContext`: deprecated
- `pom.xml`: updated multiple dependency versions
- `pom.xml`: updated Kotlin version to 1.9.24
### Removed
- `.travis.yml`

## [7.6] - 2024-02-18
### Changed
- `pom.xml`: updated multiple dependency versions

## [7.5] - 2024-02-14
### Changed
- `pom.xml`: updated multiple dependency versions

## [7.4] - 2024-02-11
### Changed
- `JSONDeserializer`: optimisations (take advantage of new functionality in `JSONObject`)

## [7.3] - 2024-02-05
### Changed
- `JSONDeserializer`, `JSONConfig`: improved handling of errors in invoked constructors

## [7.2] - 2024-01-08
### Changed
- `JSONDeserializer`: improved handling of `Map` (including classes that delegate to `Map`)
- `pom.xml`: updated multiple dependency versions

## [7.1] - 2023-10-15
### Changed
- `JSONContext`: added `fatal()` to simplify error output
- `JSONContext`: fixed inconsistency in `deserialize()` taking `KClass`
- `JSONDeserializer`, `JSONSerializerFunctions`: added check for "impossible" classes (`Unit`, `Nothing` _etc._)
- `JSONDeserializer`, `JSONDeserializerFunctions`, `JSONKotlinException`, `JSONContext`, `JSONSerializer`,
  `JSONStringify`, `JSONCoStringify`: improved exception handling
- `JSONDeserializer`, `JSONConfig`: improved constructor invocation
- `JSONConfig`: fixed inconsistency in nullability and `toJSON()`
- `JSONContext`: added `deserializeProperty()` and `deserializeItem()`
- `JSONContext`: added `JSONObject.Builder.addChild()` and `JSONArray.Builder.addItem()`
- `JSONContext`: added `modifyConfig()` and `replaceConfig()`
- `JSONSerializer`, `JSONSerializerFunctions`, `JSONStringify`, `JSONCoStringify`: performance improvements
- `pom.xml`: updated multiple dependency versions

## [7.0] - 2023-07-30
### Added
- `JSONContext`: serialization / deserialization context
### Changed
- `JSONDeserializer`: block attempt to use private constructor
- `JSONDeserializer`: switch to use `JSONContext`
- `JSONConfig`, `JSONSerializer`, `JSONStringify`, `JSONCoStringify`, `JSONDeserializer`, `JSONSerializerFunctions`,
  `JSONFun`: changed `toJSON` and `fromJSON` functions to be extension functions on `JSONContext`, not `JSONConfig`
  (breaking change for users of custom serialization and/or deserialization)

## [6.1] - 2023-07-25
### Changed
- `JSONConfig`: improved error message on polymorphic deserialization
- `JSONSerializer`, `JSONStringify`, `JSONCoStringify`: improved recursion checking
- `pom.xml`: updated Kotlin version to 1.8.22
- `pom.xml`: updated multiple dependency versions

## [6.0] - 2023-07-09
### Changed
- `JSONDeserializer`, `JSONSerializerFunctions`, `JSONFun`: further changes to the specification of deserialization
  return types (another breaking change)

## [5.0] - 2023-07-07
### Changed
- `JSONDeserializer`, `JSONFun`: changed deserialization return types, removed some functions (breaking change)
- `JSONConfig`, `JSONFun`: changed custom deserialization return types (breaking change)
- `JSONConfig`: added version of `fromJSONPolymorphic` that takes reified type parameter
- `pom.xml`: updated multiple dependency versions

## [4.4] - 2023-04-23
### Changed
- `JSONCoStringify`: auto-flush `Channel` and `Flow`

## [4.3] - 2023-04-23
### Changed
- `JSONDeserializer`: fixed obscure bugs in deserialization of classes implementing `Map` interface
- `JSONDeserializer`: allow use of constructor with additional default parameters
- `JSONDeserializer`, `JSONSerializer`, `JSONStringify`, `JSONCoStringify`: add coverage of `Opt`
- `JSONSerializer`, `JSONStringify`, `JSONCoStringify`: change error message on circular reference
- `pom.xml`: updated Kotlin and dependency versions

## [4.2] - 2023-01-08
### Changed
- `pom.xml`: updated multiple dependency versions

## [4.1] - 2023-01-03
### Changed
- `JSONDeserializer`: minor improvements to error messages
- `pom.xml`: updated multiple dependency versions

## [4.0] - 2022-11-27
### Changed
- `pom.xml`: updated versions of `kjson-core` and `kjson-pointer` (potential breaking change)

## [3.10] - 2022-11-20
### Changed
- `pom.xml`: updated multiple dependency versions

## [3.9] - 2022-11-13
### Changed
- `pom.xml`: updated versions of `kjson-pointer` (yet again) and others

## [3.8] - 2022-10-16
### Changed
- `pom.xml`: updated version of `kjson-pointer` again

## [3.7] - 2022-10-11
### Changed
- `pom.xml`: updated version of `kjson-pointer`

## [3.6] - 2022--10-09
### Changed
- tests: added tests for value classes
- `JSONConfig`: added `copy` function
- `pom.xml`: updated versions of `kjson-core` and `kjson-pointer`

## [3.5] - 2022-09-19
### Changed
- `pom.xml`: updated version of `kjson-core`
- `JSONConfig`: simplified recent changes to `fromJSONPolymorphic`

## [3.4] - 2022-09-15
### Changed
- `JSONConfig`, `JSONDeserializer`: added `fromJSONObject`, `fromJSONArray`; extended `fromJSONString`
- `JSONDeserializer`, `JSONSerializerFunctions`, `JSONKotlinException`: improved companion object `fromJSON`
- `JSONConfig`: changed `fromJSONPolymorphic` to allow non-primitive discriminator values

## [3.3] - 2022-09-04
### Changed
- `JSONFun`: added versions of `parseJSON` that use `Reader` as receiver
- `JSONConfig`, `JSONFun`: allow use of `ParseOptions` for lenient parsing

## [3.2] - 2022-07-05
### Changed
- `JSONConfig`: added use of environment variables to initialise default values
- `JSONDeserializer`: improved nullability handling
- `pom.xml`: bumped dependency version

## [3.1] - 2022-06-23
### Changed
- `pom.xml`: bumped dependency version
- `JSONDeserializer`: added `deserializeNonNullable` function
- `JSONFun`: added `fromJSONValue` and `fromJSONValueNullable` (extension functions on `JSONValue`)

## [3.0] - 2022-06-08
### Added
- `JSONCoStringify`: non-blocking output functions
### Changed
- `JSONConfig`, `JSONFun`, `JSONDeserializer`, `JSONSerializer`, `JSONStringify`: changed `toJSON` and `fromJSON`
  functions to be extension functions on `JSONConfig`
- `JSONFun`: added non-blocking extension functions
- `JSONConfig`: changed `fromJSONPolymorphic` to allow `Any` discriminator values
- `pom.xml`: bumped dependency versions, added coroutine functions as optional dependency

## [2.5] - 2022-05-29
### Changed
- `JSONDeserializer`: fixed bug in deserializing some collection types
- `pom.xml`: bumped dependency versions

## [2.4] - 2022-05-25
### Changed
- `JSONSerializer`, `JSONStringify`: fixed handling of `IntArray`, `LongArray` _etc._
- `JSONDeserializerFunctions`: minor optimisations

## [2.3] - 2022-05-04
### Changed
- `pom.xml`: bumped dependency versions
- `JSONSerializerFunctions`: take advantage of new functions

## [2.2] - 2022-04-18
### Changed
- `JSONSerializer`, `JSONSerializerFunctions`, `JSONStringify`: optimised output of standard classes
- `pom.xml`: bumped dependency versions

## [2.1] - 2022-02-11
### Changed
- `JSONSerializerFunctions`: fixed bug in serialization of some internal Kotlin classes

## [2.0] - 2022-01-31
### Changed
- `JSONDeserializer`, `JSONSerializer`, `JSONSerializerFunctions`, `JSONStringify`: added unsigned integer types
- `JSONConfig`, `JSONDeserializer`, `JSONFun`: minor code reformatting
- `JSONDeserializer`: clarified deserialization into class with constructor taking `Number` _etc._
- tests: added tests and split into multiple files

## [1.7] - 2022-01-22
### Changed
- `pom.xml`: updated Kotlin version to 1.6.10
- `JSONDeserializer`, `JSONSerializer`, `JSONSerializerFunctions`, `JSONStringify`: added `kotlin.time.Duration`

## [1.6] - 2021-11-11
### Changed
- `JSONTypeRef`: added `createRef` function

## [1.5] - 2021-10-13
### Changed
- `pom.xml`: bumped dependency versions

## [1.4] - 2021-10-13
### Changed
- `JSONConfig`: added `configurator` to constructor
- `JSONConfig`: improved polymorphic deserialization

## [1.3] - 2021-08-30
### Changed
- `JSONDeserializer`: improved type determination for parameterised types with upper bound

## [1.2] - 2021-08-29
### Changed
- `pom.xml`: bumped dependency versions, switched to `int-output` library

## [1.1] - 2021-08-25
### Changed
- `JSONSerializer`, `JSONDeserializer`, `JSONStringify`, `JSONConfig`: improved handling of sealed classes and
  polymorphic deserialization
- `pom.xml`: bumped dependency versions

## [1.0] - 2021-08-22
### Changed
- `pom.xml`: bumped version to 1.0
- several files: tidy up, improve comments

## [0.2] - 2021-08-22
### Changed
- all files: major reorganisation - many files moved to other projects, other files brought in

## [0.1] - 2021-07-27
### Added
- all files: initial versions (work in progress)
