# Change Log

The format is based on [Keep a Changelog](http://keepachangelog.com/).

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
