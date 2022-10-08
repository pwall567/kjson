# Change Log

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [Unreleased]
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
