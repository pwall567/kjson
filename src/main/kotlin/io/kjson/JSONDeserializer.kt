/*
 * @(#) JSONDeserializer.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023, 2024 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.kjson

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.KVisibility
import kotlin.reflect.typeOf
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.time.Duration

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration as JavaDuration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.BitSet
import java.util.Calendar
import java.util.Date
import java.util.LinkedList
import java.util.UUID
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

import io.kjson.JSON.asStringOrNull
import io.kjson.JSONDeserializerFunctions.callWithSingle
import io.kjson.JSONDeserializerFunctions.classifierAsClass
import io.kjson.JSONDeserializerFunctions.createUUID
import io.kjson.JSONDeserializerFunctions.displayName
import io.kjson.JSONDeserializerFunctions.findFromJSONInvoker
import io.kjson.JSONDeserializerFunctions.findParameterName
import io.kjson.JSONDeserializerFunctions.findSingleParameterConstructor
import io.kjson.JSONDeserializerFunctions.hasNumberParameter
import io.kjson.JSONDeserializerFunctions.parseCalendar
import io.kjson.JSONDeserializerFunctions.toBigInteger
import io.kjson.JSONSerializerFunctions.isImpossible
import io.kjson.JSONSerializerFunctions.isUncachedClass
import io.kjson.annotation.JSONDiscriminator
import io.kjson.annotation.JSONIdentifier
import io.kjson.optional.Opt
import net.pwall.util.ImmutableMap
import net.pwall.util.ImmutableMapEntry
import net.pwall.util.ImmutableSet

/**
 * Reflection-based JSON deserialization for Kotlin.
 *
 * @author  Peter Wall
 */
object JSONDeserializer {

    private val anyQType = Any::class.createType(emptyList(), true)
    private val stringType = String::class.createType(emptyList(), false)

    /**
     * Deserialize a parsed [JSONValue] to the inferred [KType].
     *
     * @param   json        the parsed JSON, as a [JSONValue] (or `null`)
     * @param   config      an optional [JSONConfig]
     * @param   T           the target class
     * @return              the converted object
     */
    inline fun <reified T> deserialize(
        json: JSONValue?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ): T = deserialize(typeOf<T>(), json, config) as T

    /**
     * Deserialize a parsed [JSONValue] to a specified [KType].
     *
     * @param   resultType  the target type
     * @param   json        the parsed JSON, as a [JSONValue] (or `null`)
     * @param   config      an optional [JSONConfig]
     * @return              the converted object, of [resultType] (will be `null` only if [resultType] is nullable)
     */
    fun deserialize(
        resultType: KType,
        json: JSONValue?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ): Any? = deserialize(resultType, json, JSONContext(config))

    /**
     * Deserialize a parsed [JSONValue] to a specified [KType] (specifying [JSONContext]).
     *
     * @param   resultType  the target type
     * @param   json        the parsed JSON, as a [JSONValue] (or `null`)
     * @param   context     the [JSONContext]
     * @return              the converted object, of [resultType] (will be `null` only if [resultType] is nullable)
     */
    fun deserialize(
        resultType: KType,
        json: JSONValue?,
        context: JSONContext,
    ): Any? {
        context.config.findFromJSONMapping(resultType)?.let {
            return context.applyFromJSON(it, json, resultType)
        }
        val resultClass = resultType.classifierAsClass(resultType, context)
        if (json == null) {
            return when {
                resultClass == Opt::class -> Opt.UNSET
                resultType.isMarkedNullable -> null
                else -> context.fatal("Can't deserialize null as ${resultType.displayName()}")
            }
        }
        return deserialize(resultType, resultClass, resultType.arguments, json, context)
    }

    /**
     * Deserialize a parsed [JSONValue] to a specified [KClass].
     *
     * @param   resultClass the target class
     * @param   json        the parsed JSON, as a [JSONValue] (or `null`)
     * @param   config      an optional [JSONConfig]
     * @param   T           the target class
     * @return              the converted object, of [resultClass] (may be `null`)
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> deserialize(
        resultClass: KClass<T>,
        json: JSONValue?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ): T? {
        config.findFromJSONMapping(resultClass)?.let {
            return JSONContext(config).applyFromJSON(it, json, resultClass) as T?
        }
        if (json == null)
            return if (resultClass == Opt::class) Opt.UNSET as T else null
        return deserialize(resultClass.starProjectedType, resultClass, emptyList(), json, JSONContext(config))
    }

    /**
     * Deserialize a parsed [JSONValue] to a specified [KClass], using a [JSONContext].
     *
     * @param   resultClass the target class
     * @param   json        the parsed JSON, as a [JSONValue] (or `null`)
     * @param   context     a [JSONContext]
     * @param   T           the target class
     * @return              the converted object, of [resultClass] (may be `null`)
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> deserialize(
        resultClass: KClass<T>,
        json: JSONValue?,
        context: JSONContext,
    ): T? {
        context.config.findFromJSONMapping(resultClass)?.let {
            return context.applyFromJSON(it, json, resultClass) as T?
        }
        if (json == null)
            return if (resultClass == Opt::class) Opt.UNSET as T else null
        return deserialize(resultClass.starProjectedType, resultClass, emptyList(), json, context)
    }

    /**
     * Deserialize a parsed [JSONValue] to a specified Java [Class].
     *
     * @param   javaClass   the target class
     * @param   json        the parsed JSON, as a [JSONValue] (or `null`)
     * @param   config      an optional [JSONConfig]
     * @return              the converted object
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> deserialize(
        javaClass: Class<T>,
        json: JSONValue?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ): T? = deserialize(javaClass.toKType(nullable = true), json, config)  as T?

    /**
     * Deserialize a parsed [JSONValue] to a specified Java [Type].
     *
     * @param   javaType    the target type
     * @param   json        the parsed JSON, as a [JSONValue] (or `null`)
     * @param   config      an optional [JSONConfig]
     * @return              the converted object
     */
    fun deserialize(
        javaType: Type,
        json: JSONValue?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ): Any? = deserialize(javaType.toKType(nullable = true), json, config)

    /**
     * Deserialize a parsed [JSONValue] to an unspecified([Any]) type.  Strings will be converted to `String`, numbers
     * to `Int`, `Long` or `BigDecimal`, booleans to `Boolean`, arrays to `ArrayList<Any?>` and objects to
     * `LinkedHashMap<String, Any?>` (an implementation of `Map` that preserves order).
     *
     * @param   json        the parsed JSON, as a [JSONValue] (or `null`)
     * @param   config      an optional [JSONConfig]
     * @return              the converted object
     */
    fun deserializeAny(
        json: JSONValue?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ): Any? = deserialize(anyQType, json, config)

    /**
     * Deserialize a parsed [JSONValue] to a specified [KType], where the result may not be `null` (specifying
     * [JSONContext]).
     *
     * @param   json        the parsed JSON, as a [JSONValue]
     * @param   context     a [JSONContext]
     * @param   T           the target type
     * @return              the converted object, of type [T] (will be `null` only if [T] is nullable)
     */
    private inline fun <reified T : Any> deserializeNonNull(
        json: JSONValue?,
        context: JSONContext,
    ): T = deserializeNonNull(typeOf<T>(), json, context)

    /**
     * Deserialize a parsed [JSONValue] to a specified [KType], where the result may not be `null` (specifying
     * [JSONContext]).
     *
     * @param   resultType  the target type
     * @param   json        the parsed JSON, as a [JSONValue]
     * @param   context     a [JSONContext]
     * @param   T           the target type
     * @return              the converted object, of type [T]
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> deserializeNonNull(
        resultType: KType,
        json: JSONValue?,
        context: JSONContext,
    ): T {
        context.config.findFromJSONMapping(resultType)?.let {
            return context.applyFromJSON(it, json, resultType) as T
        }
        if (json == null)
            context.fatal("Can't deserialize null as ${resultType.displayName()}")
        val resultClass = resultType.classifierAsClass(resultType, context)
        return deserialize(resultType, resultClass, emptyList(), json, context) as T
    }

    private fun JSONContext.applyFromJSON(
        fromJSONMapping: FromJSONMapping,
        json: JSONValue?,
        resultTypeOrClass: Any,
    ): Any? = try {
        fromJSONMapping(json)
    } catch (e: JSONException) {
        throw e
    } catch (e: Exception) {
        fatal("Error in custom fromJSON mapping of $resultTypeOrClass", e)
    }

    /**
     * Deserialize a parsed [JSONValue] to a parameterized [KClass], with the specified [KTypeProjection]s.
     *
     * @param   resultType  the target [KType]
     * @param   resultClass the target class
     * @param   types       the [KTypeProjection]s
     * @param   json        the parsed JSON, as a [JSONValue]
     * @param   context     a [JSONContext]
     * @param   T           the target class
     * @return              the converted object
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> deserialize(
        resultType: KType,
        resultClass: KClass<T>,
        types: List<KTypeProjection>,
        json: JSONValue,
        context: JSONContext,
    ): T {

        if (resultClass.isImpossible())
            context.fatal("Can't deserialize ${resultClass.simpleName}")

        // check for JSONValue

        if (resultClass.isSubclassOf(JSONValue::class) && resultClass.isSuperclassOf(json::class))
            return json as T

        // is it Opt?

        if (resultClass == Opt::class)
            return Opt.ofNullable(deserializeNested(resultType, getTypeParam(types), json, context)) as T

        // does the target class companion object have a "fromJSON()" method?

        if (!resultClass.isUncachedClass()) {
            try {
                resultClass.companionObject?.let { companionObject ->
                    try {
                        findFromJSONInvoker(resultClass, json::class, companionObject)?.let {
                            return it.invoke(json, context) as T
                        }
                    }
                    catch (e: InvocationTargetException) {
                        val cause = e.cause
                        if (cause is JSONException)
                            throw cause
                        context.fatal("Error in custom in-class fromJSON - ${resultClass.qualifiedName}", e.cause ?: e)
                    }
                    catch (e: Exception) {
                        context.fatal("Error in custom in-class fromJSON - ${resultClass.qualifiedName}", e)
                    }
                }
            }
            catch (e: JSONException) {
                throw e
            }
            catch (_: Exception) {} // some classes don't allow getting companion object (Kotlin bug?)
        }

        try {
            return when (json) {
                is JSONBoolean -> {
                    if (resultClass.isSuperclassOf(Boolean::class))
                        json.value as T
                    else
                        context.fatal("Can't deserialize $json as ${resultClass.displayName()}")
                }
                is JSONString -> deserializeString(resultClass, json.value, context)
                is JSONInt -> deserializeNumber(resultClass, json, context)
                is JSONLong -> deserializeNumber(resultClass, json, context)
                is JSONDecimal -> deserializeNumber(resultClass, json, context)
                is JSONArray -> deserializeArray(resultType, resultClass, types, json, context)
                is JSONObject -> deserializeObject(resultType, resultClass, types, json, context)
            }
        }
        catch (e: JSONException) {
            throw e
        }
        catch (ite: InvocationTargetException) {
            context.fatal("Error deserializing $resultType - ${ite.cause?.message ?: "InvocationTargetException"}",
                    ite.cause ?: ite)
        }
        catch (e: Exception) {
            context.fatal("Error deserializing $resultType - ${e.message ?: e::class.simpleName}", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> deserializeNumber(
        resultClass: KClass<T>,
        number: JSONNumber,
        context: JSONContext,
    ): T {
        try {
            when (resultClass) {
                Int::class -> if (number.isInt()) return number.toInt() as T
                Long::class -> if (number.isLong()) return number.toLong() as T
                Double::class -> return number.toDouble() as T
                Float::class -> return number.toFloat() as T
                Short::class -> if (number.isShort()) return number.toShort() as T
                Byte::class -> if (number.isByte()) return number.toByte() as T
                BigInteger::class -> if (number.isIntegral()) return number.toBigInteger() as T
                BigDecimal::class -> return number.toDecimal() as T
                UInt::class -> if (number.isUInt()) return number.toUInt() as T
                ULong::class -> if (number.isULong()) return number.toULong() as T
                UShort::class -> if (number.isUShort()) return number.toUShort() as T
                UByte::class -> if (number.isUByte()) return number.toUByte() as T
                Number::class -> return number.value as T
                Any::class -> return number.value as T
                else -> resultClass.constructors.singleOrNull { it.hasNumberParameter() }?.apply {
                    when (parameters[0].type.classifier) {
                        Int::class -> if (number.isInt()) return callWithSingle(number.toInt())
                        Long::class -> if (number.isLong()) return callWithSingle(number.toLong())
                        Short::class -> if (number.isShort()) return callWithSingle(number.toShort())
                        Byte::class -> if (number.isByte()) return callWithSingle(number.toByte())
                        UInt::class -> if (number.isUInt()) return callWithSingle(number.toUInt())
                        ULong::class -> if (number.isULong()) return callWithSingle(number.toULong())
                        UShort::class -> if (number.isUShort()) return callWithSingle(number.toUShort())
                        UByte::class -> if (number.isUByte()) return callWithSingle(number.toUByte())
                        Double::class -> return callWithSingle(number.toDouble())
                        Float::class -> return callWithSingle(number.toFloat())
                        BigInteger::class -> if (number.isIntegral()) return callWithSingle(number.toBigInteger())
                        BigDecimal::class -> return callWithSingle(number.toDecimal())
                    }
                }
            }
            context.fatal("Can't deserialize $number as ${resultClass.displayName()}")
        }
        catch (e: JSONException) {
            throw e
        }
        catch (e: Exception) {
            context.fatal("Error deserializing $number as ${resultClass.displayName()}", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> deserializeString(
        resultClass: KClass<T>,
        str: String,
        context: JSONContext,
    ): T {
        try {
            if (resultClass.isSuperclassOf(String::class))
                return str as T
            when (resultClass) {
                Char::class -> {
                    if (str.length != 1)
                        context.fatal("Character must be string of length 1")
                    return str[0] as T
                }
                CharArray::class -> return str.toCharArray() as T
                Array<Char>::class -> return Array(str.length) { i -> str[i] } as T
                java.sql.Date::class -> return java.sql.Date.valueOf(str) as T
                java.sql.Time::class -> return java.sql.Time.valueOf(str) as T
                java.sql.Timestamp::class -> return java.sql.Timestamp.valueOf(str) as T
                Calendar::class -> return parseCalendar(str) as T
                Date::class -> return parseCalendar(str).time as T
                Instant::class -> return Instant.parse(str) as T
                LocalDate::class -> return LocalDate.parse(str) as T
                LocalDateTime::class -> return LocalDateTime.parse(str) as T
                LocalTime::class -> return LocalTime.parse(str) as T
                OffsetTime::class -> return OffsetTime.parse(str) as T
                OffsetDateTime::class -> return OffsetDateTime.parse(str) as T
                ZonedDateTime::class -> return ZonedDateTime.parse(str) as T
                Year::class -> return Year.parse(str) as T
                YearMonth::class -> return YearMonth.parse(str) as T
                MonthDay::class -> return MonthDay.parse(str) as T
                JavaDuration::class -> return JavaDuration.parse(str) as T
                Period::class -> return Period.parse(str) as T
                Duration::class -> return Duration.parseIsoString(str) as T
                UUID::class -> return createUUID(str) as T
            }

            // is the target class an enum?

            if (resultClass.isSubclassOf(Enum::class))
                resultClass.staticFunctions.find { it.name == "valueOf" }?.apply { return call(str) as T }

            // does the target class have a public constructor that takes String? (e.g. StringBuilder, URL, ... )

            resultClass.findSingleParameterConstructor(String::class)?.apply {
                return callWithSingle(str)
            }

        }
        catch (e: JSONException) {
            throw e
        }
        catch (e: Exception) {
            context.fatal("Error deserializing \"$str\" as ${resultClass.displayName()}", e)
        }

        context.fatal("Can't deserialize \"$str\" as ${resultClass.displayName()}")
    }

    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
    private fun <T : Any> deserializeArray(
        resultType: KType,
        resultClass: KClass<T>,
        types: List<KTypeProjection>,
        json: JSONArray,
        context: JSONContext,
    ): T {
        return when (resultClass) {
            BooleanArray::class -> BooleanArray(json.size) { i ->
                deserializeNonNull<Boolean>(json[i], context.child(i))
            }
            ByteArray::class -> ByteArray(json.size) { i ->
                deserializeNonNull<Byte>(json[i], context.child(i))
            }
            CharArray::class -> CharArray(json.size) { i ->
                deserializeNonNull<Char>(json[i], context.child(i))
            }
            DoubleArray::class -> DoubleArray(json.size) { i ->
                deserializeNonNull<Double>(json[i], context.child(i))
            }
            FloatArray::class -> FloatArray(json.size) { i ->
                deserializeNonNull<Float>(json[i], context.child(i))
            }
            IntArray::class -> IntArray(json.size) { i ->
                deserializeNonNull<Int>(json[i], context.child(i))
            }
            LongArray::class -> LongArray(json.size) { i ->
                deserializeNonNull<Long>(json[i], context.child(i))
            }
            ShortArray::class -> ShortArray(json.size) { i ->
                deserializeNonNull<Short>(json[i], context.child(i))
            }

            Collection::class,
            MutableCollection::class,
            List::class,
            MutableList::class,
            Iterable::class,
            ArrayList::class -> ArrayList<Any?>(json.size).apply {
                fillFromJSON(resultType, json, getTypeParam(types), context)
            }

            Any::class -> ArrayList<Any?>(json.size).apply {
                fillFromJSON(resultType, json, anyQType, context)
            }

            LinkedList::class -> LinkedList<Any?>().apply {
                fillFromJSON(resultType, json, getTypeParam(types), context)
            }

            Stream::class -> ArrayList<Any?>(json.size).apply {
                fillFromJSON(resultType, json, getTypeParam(types), context)
            }.stream()

            IntStream::class -> {
                val intArray = IntArray(json.size) {
                        i -> deserializeNonNull<Int>(json[i], context.child(i))
                }
                IntStream.of(*intArray)
            }

            LongStream::class -> {
                val longArray = LongArray(json.size) {
                        i -> deserializeNonNull<Long>(json[i], context.child(i))
                }
                LongStream.of(*longArray)
            }

            DoubleStream::class -> {
                val doubleArray = DoubleArray(json.size) {
                        i -> deserializeNonNull<Double>(json[i], context.child(i))
                }
                DoubleStream.of(*doubleArray)
            }

            Set::class,
            MutableSet::class,
            LinkedHashSet::class -> LinkedHashSet<Any?>(json.size).apply {
                fillFromJSON(resultType, json, getTypeParam(types), context)
            }

            HashSet::class -> HashSet<Any?>(json.size).apply {
                fillFromJSON(resultType, json, getTypeParam(types), context)
            }

            Sequence::class -> json.mapIndexed { i, value ->
                deserializeNested(resultType, getTypeParam(types), value, context.child(i))
            }.asSequence()

            Pair::class -> {
                val result0 = deserializeNested(resultType, getTypeParam(types, 0), json[0], context.child("0"))
                val result1 = deserializeNested(resultType, getTypeParam(types, 1), json[1], context.child("1"))
                result0 to result1
            }

            Triple::class -> {
                val result0 = deserializeNested(resultType, getTypeParam(types, 0), json[0], context.child("0"))
                val result1 = deserializeNested(resultType, getTypeParam(types, 1), json[1], context.child("1"))
                val result2 = deserializeNested(resultType, getTypeParam(types, 2), json[2], context.child("2"))
                Triple(result0, result1, result2)
            }

            BitSet::class -> {
                BitSet().apply {
                    json.mapIndexed { i, value ->
                        if (value !is JSONInt)
                            context.child(i).fatal("Can't deserialize BitSet; array member not int")
                        set(value.value)
                    }
                }
            }

            else -> {
                if (resultClass.java.isArray) {
                    val type = getTypeParam(types)
                    val itemClass = type.classifier as? KClass<Any> ?: context.fatal("Can't determine array type")
                    newArray(itemClass, json.size).apply {
                        for (i in json.indices)
                            this[i] = deserializeNested(resultType, type, json[i], context.child(i))
                    }
                }
                else {

                    // If the target class has a constructor that takes a single List (or Set) parameter, create a
                    // List (Set) and invoke that constructor.  This should catch the less frequently used List (Set)
                    // classes.

                    resultClass.findSingleParameterConstructor(List::class)?.run {
                        val type = getTypeParam(parameters[0].type.arguments)
                        callWithSingle(ArrayList<Any?>(json.size).apply {
                            fillFromJSON(resultType, json, type, context)
                        })
                    } ?: resultClass.findSingleParameterConstructor(Set::class)?.run {
                        val type = getTypeParam(parameters[0].type.arguments)
                        callWithSingle(LinkedHashSet<Any?>(json.size).apply {
                            fillFromJSON(resultType, json, type, context)
                        })
                    } ?: context.fatal("Can't deserialize array as ${resultClass.displayName()}")
                }
            }
        } as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> newArray(
        itemClass: KClass<T>,
        size: Int,
    ): Array<T?> = java.lang.reflect.Array.newInstance(itemClass.java, size) as Array<T?>
    // there appears to be no way of creating an array of dynamic type in Kotlin
    // other than to use Java reflection

    private fun getTypeParam(types: List<KTypeProjection>, n: Int = 0): KType = types.getOrNull(n)?.type ?: anyQType

    private fun MutableCollection<Any?>.fillFromJSON(
        resultType: KType,
        json: JSONArray,
        type: KType,
        context: JSONContext,
    ) {
        // using for rather than map to avoid creation of intermediate List
        for (i in json.indices) {
            if (!add(deserializeNested(resultType, type, json[i], context.child(i))))
                context.child(i).fatal("Duplicate not allowed")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> deserializeObject(
        resultType: KType,
        resultClass: KClass<T>,
        types: List<KTypeProjection>,
        json: JSONObject,
        context: JSONContext,
    ): T {
        if (resultClass.isSubclassOf(Map::class)) {
            when (resultClass) {
                Map::class -> return deserializeImmutableMap(resultType, getTypeParam(types, 0), getTypeParam(types, 1),
                        json, context) as T
                HashMap::class -> return deserializeMap(resultType, HashMap(json.size), types, json, context) as T
                MutableMap::class,
                LinkedHashMap::class -> return deserializeMap(resultType, LinkedHashMap(json.size), types, json,
                        context) as T
            }
            // If the target class has a constructor that takes a single Map parameter, create a Map and invoke that
            // constructor.  This should catch the less frequently used Map classes.
            resultClass.findSingleParameterConstructor(Map::class)?.apply {
                val mapArgumentTypes = parameters[0].type.applyTypeParameters(resultType, context).arguments
                val keyType = getTypeParam(mapArgumentTypes, 0)
                val valueType = getTypeParam(mapArgumentTypes, 1)
                val argMap = if (keyType == stringType) {
                    // looks like a class delegating to a Map
                    val array: Array<ImmutableMapEntry<Any, Any?>> = ImmutableMap.createArray(json.size)
                    val entries = json.entries as ImmutableSet
                    for (index in 0 until entries.size) {
                        val entry = entries[index]
                        val key = entry.key
                        val memberType = findField(resultClass.members, key, context.config)?.returnType ?: valueType
                        val value = deserializeNested(resultType, memberType, entry.value, context.child(key))
                        array[index] = ImmutableMap.entry(key, value)
                    }
                    ImmutableMap(array)
                } else deserializeImmutableMap(
                    resultType = resultType,
                    keyType = keyType,
                    valueType = valueType,
                    json = json,
                    context = context,
                )
                return callWithSingle(argMap)
            }
        }

        val config = context.config
        if (resultClass.isSealed) {
            val discriminator = resultClass.findAnnotation<JSONDiscriminator>()?.id ?: config.sealedClassDiscriminator
            val subClassName = json[discriminator].asStringOrNull ?:
                    context.fatal("No discriminator for sealed class ${resultClass.displayName()}")
            val subClass = resultClass.sealedSubclasses.find {
                (it.findAnnotation<JSONIdentifier>()?.id ?: it.simpleName) == subClassName
            } ?: context.fatal("Can't find identifier $subClassName for sealed class ${resultClass.displayName()}")
            val nestedObject = if (findField(resultClass.members, discriminator, config) == null) {
                JSONObject.build {
                    for (property in json)
                        if (property.key != discriminator)
                            add(property)
                }
            } else
                json
            return deserializeObject(subClass.createType(types, nullable = resultType.isMarkedNullable), subClass,
                    types, nestedObject, context)
        }

        resultClass.objectInstance?.let { objectInstance ->
            return objectInstance.also { setRemainingFields(resultType, resultClass, it, json, context) }
        }

        if (resultClass == Any::class)
            return deserializeMap(resultType, LinkedHashMap(json.size), stringType, anyQType, json, context) as T

        val publicConstructors = resultClass.constructors.filter { it.visibility == KVisibility.PUBLIC }
        findBestConstructor(publicConstructors, json, config)?.apply {
            val args = mutableListOf<ImmutableMapEntry<KParameter, Any?>>()
            val properties = json.toMutableList()
            for (i in parameters.indices) {
                val parameter = parameters[i]
                val paramName = findParameterName(parameter, config) ?: "param$i"
                val propertyIndex = properties.indexOfFirst { it.key == paramName }
                if (!config.hasIgnoreAnnotation(parameter.annotations)) {
                    if (propertyIndex >= 0) {
                        val parameterValue = if (parameter.type.classifier == Opt::class) {
                            val value = deserializeNested(
                                enclosingType = resultType,
                                resultType = getTypeParam(parameter.type.arguments),
                                json = properties[propertyIndex].value,
                                context = context.child(paramName),
                            )
                            Opt.of(value)
                        }
                        else {
                            deserializeNested(
                                enclosingType = resultType,
                                resultType = parameter.type,
                                json = properties[propertyIndex].value,
                                context = context.child(paramName),
                            )
                        }
                        args.add(ImmutableMap.entry(parameter, parameterValue))
                    }
                    else {
                        if (!parameter.isOptional) {
                            val parameterValue = when {
                                parameter.type.classifier == Opt::class -> Opt.UNSET
                                parameter.type.isMarkedNullable -> null
                                else -> context.fatal("Can't create $resultClass - missing property $paramName")
                            }
                            args.add(ImmutableMap.entry(parameter, parameterValue))
                        }
                    }
                }
                if (propertyIndex >= 0)
                    properties.removeAt(propertyIndex)
            }
            return callBy(ImmutableMap.from(args)).also {
                if (properties.isNotEmpty())
                    setRemainingFields(resultType, resultClass, it, properties, context)
            }
        }
        // there is no matching constructor
        if (publicConstructors.size == 1) {
            val missing = resultClass.constructors.first().parameters.filter {
                !config.hasIgnoreAnnotation(it.annotations) && !it.isOptional && !it.type.isMarkedNullable
            }.map {
                findParameterName(it, config)
            }.filter {
                !json.containsKey(it)
            }
            context.fatal("Can't create ${resultClass.qualifiedName}; missing: ${missing.displayList()}")
        }
        val propMessage = when {
            json.isNotEmpty() -> json.keys.displayList()
            else -> "none"
        }
        context.fatal("Can't locate public constructor for ${resultClass.qualifiedName}; properties: $propMessage")
    }

    private fun Collection<Any?>.displayList(): String = joinToString(", ")

    private fun deserializeMap(
        resultType: KType,
        map: MutableMap<Any, Any?>,
        types: List<KTypeProjection>,
        json: JSONObject,
        context: JSONContext,
    ): MutableMap<Any, Any?> {
        val keyType = getTypeParam(types, 0)
        val valueType = getTypeParam(types, 1)
        return deserializeMap(resultType, map, keyType, valueType, json, context)
    }

    private fun deserializeMap(
        resultType: KType,
        map: MutableMap<Any, Any?>,
        keyType: KType,
        valueType: KType,
        json: JSONObject,
        context: JSONContext,
    ): MutableMap<Any, Any?> {
        for (property in json) {
            val child = context.child(property.key)
            val key = deserializeKey(property.key, keyType, resultType, child)
            map[key] = deserializeNested(resultType, valueType, property.value, child)
        }
        return map
    }

    private fun deserializeImmutableMap(
        resultType: KType,
        keyType: KType,
        valueType: KType,
        json: JSONObject,
        context: JSONContext,
    ): Map<Any, Any?> {
        val array: Array<ImmutableMapEntry<Any, Any?>> = ImmutableMap.createArray(json.size)
        for (index in json.indices) {
            val property = json[index]
            val child = context.child(property.key)
            val key = deserializeKey(property.key, keyType, resultType, child)
            val value = deserializeNested(resultType, valueType, property.value, child)
            array[index] = ImmutableMap.entry(key, value)
        }
        return ImmutableMap(array)
    }

    private fun deserializeKey(
        key: String,
        keyType: KType,
        resultType: KType,
        childContext: JSONContext
    ): Any = if (keyType == stringType)
        key
    else
        deserializeNested(
            enclosingType = resultType,
            resultType = keyType,
            json = JSONString(key),
            context = childContext,
        ) ?: childContext.fatal("Key can not be determined for Map")

    private fun <T : Any> setRemainingFields(
        resultType: KType,
        resultClass: KClass<T>,
        instance: T,
        properties: List<JSONObject.Property>,
        context: JSONContext,
    ) {
        val config = context.config
        for (property in properties) { // JSONObject fields not used in constructor
            val member = findField(resultClass.members, property.key, config)
            if (member != null) {
                if (!config.hasIgnoreAnnotation(member.annotations)) {
                    val value = deserializeNested(
                        enclosingType = resultType,
                        resultType = member.returnType,
                        json = property.value,
                        context = context.child(property.key),
                    )
                    if (member is KMutableProperty<*>) {
                        val wasAccessible = member.isAccessible
                        member.isAccessible = true
                        try {
                            member.setter.call(instance, value)
                        }
                        catch (e: Exception) {
                            context.fatal("Error setting property ${property.key} in ${resultClass.qualifiedName}", e)
                        }
                        finally {
                            member.isAccessible = wasAccessible
                        }
                    }
                    else {
                        if (member.getter.call(instance) != value)
                            context.fatal("Can't set property ${property.key} in ${resultClass.qualifiedName}")
                    }
                }
            }
            else {
                if (!(config.allowExtra || config.hasAllowExtraPropertiesAnnotation(resultClass.annotations)))
                    context.fatal("Can't find property ${property.key} in ${resultClass.qualifiedName}")
            }
        }
    }

    private fun findField(members: Collection<KCallable<*>>, name: String, config: JSONConfig): KProperty<*>? {
        for (member in members)
            if (member is KProperty<*> && (config.findNameFromAnnotation(member.annotations) ?: member.name) == name)
                return member
        return null
    }

    private fun <T : Any> findBestConstructor(
        constructors: Collection<KFunction<T>>,
        json: JSONObject,
        config: JSONConfig,
    ): KFunction<T>? {
        var result: KFunction<T>? = null
        var best = -1
        for (constructor in constructors) {
            val parameters = constructor.parameters
            if (parameters.any { findParameterName(it, config) == null || it.kind != KParameter.Kind.VALUE })
                continue
            val n = findMatchingParameters(parameters, json, config)
            if (n > best) {
                result = constructor
                best = n
            }
        }
        return result
    }

    private fun findMatchingParameters(parameters: List<KParameter>, json: JSONObject, config: JSONConfig): Int {
        var n = 0
        for (param in parameters) {
            if (json.containsKey(findParameterName(param, config)))
                n++
            else {
                if (!(param.isOptional || param.type.isMarkedNullable || param.type.classifier == Opt::class))
                    return -1
            }
        }
        return n
    }

    private fun deserializeNested(
        enclosingType: KType,
        resultType: KType,
        json: JSONValue?,
        context: JSONContext,
    ): Any? = deserialize(resultType.applyTypeParameters(enclosingType, context), json, context)

    private fun KType.applyTypeParameters(enclosingType: KType, context: JSONContext): KType {
        (classifier as? KTypeParameter)?.let { typeParameter ->
            val enclosingClass = enclosingType.classifierAsClass(this, context)
            val index = enclosingClass.typeParameters.indexOfFirst { it.name == typeParameter.name }
            return enclosingType.arguments.getOrNull(index)?.type ?:
            enclosingClass.typeParameters.getOrNull(index)?.upperBounds?.singleOrNull() ?:
                    context.fatal("Can't create ${displayName()} - no type information for ${typeParameter.name}")
        }

        if (arguments.isEmpty())
            return this

        return classifierAsClass(this, context).createType(arguments.map { (variance, type) ->
            if (variance == null || type == null)
                KTypeProjection.STAR
            else
                type.applyTypeParameters(enclosingType, context).let {
                    when (variance) {
                        KVariance.INVARIANT -> KTypeProjection.invariant(it)
                        KVariance.IN -> KTypeProjection.contravariant(it)
                        KVariance.OUT -> KTypeProjection.covariant(it)
                    }
                }
        }, isMarkedNullable, annotations)
    }

}
