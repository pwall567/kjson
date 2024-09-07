/*
 * @(#) Serializer.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2024 Peter Wall
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

package io.kjson.serialize

import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.staticProperties
import kotlin.time.Duration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow

import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
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
import java.util.Enumeration
import java.util.LinkedHashMap
import java.util.LinkedList
import java.util.Locale
import java.util.UUID
import java.util.stream.BaseStream
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream

import io.kjson.JSON.appendTo
import io.kjson.JSON.coOutputTo
import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONConfig
import io.kjson.JSONDecimal
import io.kjson.JSONDeserializer
import io.kjson.JSONDeserializer.anyQType
import io.kjson.JSONDeserializer.applyTypeParameters
import io.kjson.JSONDeserializerFunctions.displayName
import io.kjson.JSONInt
import io.kjson.JSONKotlinException
import io.kjson.JSONLong
import io.kjson.JSONObject
import io.kjson.JSONSerializerFunctions.findToJSON
import io.kjson.JSONString
import io.kjson.JSONValue
import io.kjson.optional.Opt
import io.kjson.toKType
import io.kjson.util.findSealedClassDiscriminator
import io.kjson.util.getJavaClassHierarchy
import io.kjson.util.isKotlinClass
import io.kjson.util.isStaticOrTransient
import net.pwall.util.CoOutput
import net.pwall.util.output

sealed interface Serializer<in T : Any> {

    fun serialize(value: T, config: JSONConfig, references: MutableList<Any>): JSONValue?

    fun appendTo(a: Appendable, value: T, config: JSONConfig, references: MutableList<Any>) {
        serialize(value, config, references).appendTo(a)
    }

    suspend fun output(out: CoOutput, value: T, config: JSONConfig, references: MutableList<Any>) {
        serialize(value, config, references).coOutputTo(out)
    }

    fun serializeNested(
        value: T?,
        config: JSONConfig,
        references: MutableList<Any>,
        nestedName: String,
    ): JSONValue? = when (value) {
        null -> null
        in references -> throw JSONKotlinException("Circular reference to ${value::class.simpleName}", nestedName)
        else -> try {
            references.add(value)
            serialize(value, config, references)
        }
        catch (e: JSONKotlinException) {
            throw e.nested(nestedName)
        }
        finally {
            references.removeLast()
        }
    }

    fun appendNestedTo(
        a: Appendable,
        value: T?,
        config: JSONConfig,
        references: MutableList<Any>,
        nestedName: String,
    ) {
        when (value) {
            null -> a.append("null")
            in references -> throw JSONKotlinException("Circular reference to ${value::class.simpleName}", nestedName)
            else -> try {
                references.add(value)
                appendTo(a, value, config, references)
            }
            catch (e: JSONKotlinException) {
                throw e.nested(nestedName)
            }
            finally {
                references.removeLast()
            }
        }
    }

    suspend fun outputNestedTo(
        out: CoOutput,
        value: T?,
        config: JSONConfig,
        references: MutableList<Any>,
        nestedName: String,
    ) {
        when (value) {
            null -> out.output("null")
            in references -> throw JSONKotlinException("Circular reference to ${value::class.simpleName}", nestedName)
            else -> try {
                references.add(value)
                output(out, value, config, references)
            }
            catch (e: JSONKotlinException) {
                throw e.nested(nestedName)
            }
            finally {
                references.removeLast()
            }
        }
    }

    companion object {

        val initialEntries = listOf(
            Any::class to AnySerializer,
            CharSequence::class to CharSequenceSerializer,
            String::class to CharSequenceSerializer,
            Boolean::class to BooleanSerializer,
            UUID::class to UUIDSerializer,
            BigInteger::class to BigIntegerSerializer,
            BigDecimal::class to BigDecimalSerializer,
            OffsetDateTime::class to OffsetDateTimeSerializer,
            LocalDate::class to LocalDateSerializer,
            Instant::class to InstantSerializer,
            OffsetTime::class to OffsetTimeSerializer,
            LocalDateTime::class to LocalDateTimeSerializer,
            LocalTime::class to LocalTimeSerializer,
            Year::class to YearSerializer,
            YearMonth::class to YearMonthSerializer,
            MonthDay::class to MonthDaySerializer,
            Duration::class to DurationSerializer,
            Char::class to CharSerializer,
            CharArray::class to CharArraySerializer,
            IntArray::class to IntArraySerializer,
            LongArray::class to LongArraySerializer,
            ByteArray::class to ByteArraySerializer,
            ShortArray::class to ShortArraySerializer,
            FloatArray::class to FloatArraySerializer,
            DoubleArray::class to DoubleArraySerializer,
            BooleanArray::class to BooleanArraySerializer,
            UInt::class to UIntSerializer,
            UShort::class to UShortSerializer,
            UByte::class to UByteSerializer,
            ULong::class to ULongSerializer,
            Int::class to IntSerializer,
            Short::class to ShortSerializer,
            Byte::class to ByteSerializer,
            Long::class to LongSerializer,
            Float::class to FloatingPointSerializer,
            Double::class to FloatingPointSerializer,
            java.sql.Date::class to ToStringSerializer,
            java.sql.Time::class to ToStringSerializer,
            java.sql.Timestamp::class to ToStringSerializer,
            ZonedDateTime::class to ToStringSerializer,
            java.time.Duration::class to ToStringSerializer,
            Period::class to ToStringSerializer,
            URI::class to ToStringSerializer,
            URL::class to ToStringSerializer,
            BitSet::class to BitSetSerializer,
            Date::class to DateSerializer,
            IntStream::class to IntStreamSerializer,
            LongStream::class to LongStreamSerializer,
            DoubleStream::class to DoubleStreamSerializer,
            JSONString::class to JSONValueSerializer,
            JSONInt::class to JSONValueSerializer,
            JSONLong::class to JSONValueSerializer,
            JSONDecimal::class to JSONValueSerializer,
            JSONBoolean::class to JSONValueSerializer,
            JSONArray::class to JSONValueSerializer,
            JSONObject::class to JSONValueSerializer,
        )

        /**
         * Find the appropriate [Serializer], given a [KType].
         */
        internal fun findSerializer(
            kType: KType,
            config: JSONConfig,
        ) : Serializer<Any> {
            config.findSerializer(kType)?.let { return it }
            config.addSerializer(kType, AnySerializer) // guard against circular references
            val result = createSerializer(kType, config)
            config.addSerializer(kType, result)
            @Suppress("unchecked_cast")
            return result as Serializer<Any>
        }

        /**
         * Find the appropriate [Serializer], given a [KClass], but no type parameters.
         */
        internal fun <TT : Any> findSerializer(
            kClass: KClass<TT>,
            config: JSONConfig,
        ) : Serializer<TT> {
            config.findSerializer(kClass)?.let { return it }
            config.addSerializer(kClass, AnySerializer) // guard against circular references
            val result = createSerializer(kClass, config)
            config.addSerializer(kClass, result)
            return result
        }

        /**
         * Create a [Serializer], given a [KClass], but no type parameters.
         */
        @Suppress("unchecked_cast")
        private fun <TT : Any> createSerializer(
            kClass: KClass<TT>,
            config: JSONConfig,
        ) : Serializer<TT> {

            // does it have a toJSON mapping defined in the config?

            config.findToJSONMapping(kClass)?.let { mapping ->
                return ConfigToJSONMappingSerializer(mapping, kClass.displayName())
            }

            // Does it have an in-class toJSON?

            kClass.findToJSON()?.let { mapping ->
                return InClassToJSONSerializer(mapping, kClass.displayName())
            }

            // is it an array of primitive type?

            if (kClass.java.isArray) {
                val arrayType = kClass.java.componentType
                if (arrayType.isPrimitive) {
                    return when (arrayType) {
                        Integer.TYPE -> ArraySerializer(Int::class, IntSerializer)
                        java.lang.Long.TYPE -> ArraySerializer(Long::class, LongSerializer)
                        java.lang.Short.TYPE -> ArraySerializer(Short::class, ShortSerializer)
                        java.lang.Byte.TYPE -> ArraySerializer(Byte::class, ByteSerializer)
                        Character.TYPE -> ArraySerializer(Char::class, CharSerializer)
                        java.lang.Float.TYPE -> ArraySerializer(Float::class, FloatingPointSerializer)
                        java.lang.Double.TYPE -> ArraySerializer(Double::class, FloatingPointSerializer)
                        java.lang.Boolean.TYPE -> ArraySerializer(Boolean::class, BooleanSerializer)
                        else -> throw JSONKotlinException("Can't serialize $kClass")
                    } as Serializer<TT>
                }
                else {
                    val itemType = arrayType.kotlin
                    return ArraySerializer(
                        itemClass = itemType,
                        itemSerializer = findSerializer(itemType as KClass<Any>, config),
                    ) as Serializer<TT>
                }
            }

            // is it a CharSequence?

            if (kClass.isSubclassOf(CharSequence::class))
                return CharSequenceSerializer as Serializer<TT>

            // is it an enum?

            if (kClass.isSubclassOf(Enum::class))
                return ToStringSerializer

            // is it a Calendar?

            if (kClass.isSubclassOf(Calendar::class))
                return CalendarSerializer as Serializer<TT>

            return createSerializer(kClass.starProjectedType, kClass, config)
        }

        /**
         * Create a [Serializer], given a [KType].
         */
        private fun createSerializer(
            kType: KType,
            config: JSONConfig,
        ) : Serializer<*> {

            // does it have a toJSON mapping defined in the config?

            config.findToJSONMapping(kType)?.let { mapping ->
                return ConfigToJSONMappingSerializer(mapping, kType.displayName())
            }

            // Does it have an in-class toJSON?

            val kClass = kType.classifier as? KClass<*> ?: throw JSONKotlinException("Can't serialize $kType")
            kClass.findToJSON()?.let { mapping ->
                return InClassToJSONSerializer(mapping, kType.displayName())
            }

            // is it an array of primitive type?

            if (kClass.java.isArray) {
                val arrayType = kClass.java.componentType
                if (arrayType.isPrimitive) {
                    return when (arrayType) {
                        Integer.TYPE -> ArraySerializer(Int::class, IntSerializer)
                        java.lang.Long.TYPE -> ArraySerializer(Long::class, LongSerializer)
                        java.lang.Short.TYPE -> ArraySerializer(Short::class, ShortSerializer)
                        java.lang.Byte.TYPE -> ArraySerializer(Byte::class, ByteSerializer)
                        Character.TYPE -> ArraySerializer(Char::class, CharSerializer)
                        java.lang.Float.TYPE -> ArraySerializer(Float::class, FloatingPointSerializer)
                        java.lang.Double.TYPE -> ArraySerializer(Double::class, FloatingPointSerializer)
                        java.lang.Boolean.TYPE -> ArraySerializer(Boolean::class, BooleanSerializer)
                        else -> throw JSONKotlinException("Can't serialize $kType")
                    }
                }
                else {
                    val itemType = JSONDeserializer.getTypeParam(kType.arguments).applyTypeParameters(kType)
                    return ArraySerializer(
                        itemClass = itemType.classifier.asKClassAny(),
                        itemSerializer = findSerializer(itemType, config),
                    )
                }
            }

            // is it a CharSequence?

            if (kClass.isSubclassOf(CharSequence::class))
                return CharSequenceSerializer

            // is it an enum?

            if (kClass.isSubclassOf(Enum::class))
                return ToStringSerializer

            // is it a Calendar?

            if (kClass.isSubclassOf(Calendar::class))
                return CalendarSerializer

            return createSerializer(kType, kClass, config)
        }

        /**
         * Create a [Serializer], given both a [KType] and a [KClass] (shared functionality for both of the preceding
         * functions.
         */
        @Suppress("unchecked_cast")
        private fun <TT : Any> createSerializer(
            kType: KType,
            kClass: KClass<TT>,
            config: JSONConfig,
        ) : Serializer<TT> {

            // is it an Iterable (List etc.)

            if (kClass == List::class || kClass == MutableList::class || kClass == ArrayList::class ||
                    kClass == LinkedList::class) {
                val itemType = JSONDeserializer.getTypeParam(kType.arguments).applyTypeParameters(kType)
                return IterableSerializer(
                    itemSerializer = findSerializer(itemType, config),
                ) as Serializer<TT>
            }

            if (kClass.isSubclassOf(Iterable::class)) {
                // derive type information from "iterator" operation
                val iteratorFunction = kClass.functions.find { it.name == "iterator" && it.parameters.size == 2 &&
                        it.parameters[0].kind == KParameter.Kind.INSTANCE &&
                        it.parameters[1].type.classifier == Int::class }
                val iteratorType = (iteratorFunction?.returnType ?: anyQType).applyTypeParameters(kType)
                val nextFunction = (iteratorType.classifier as? KClass<*>)?.functions?.find { it.name == "next" &&
                        it.parameters.size == 1 && it.parameters[0].kind == KParameter.Kind.INSTANCE }
                val itemType = (nextFunction?.returnType ?: anyQType).applyTypeParameters(iteratorType)
                return IterableSerializer(
                    itemSerializer = findSerializer(itemType, config),
                ) as Serializer<TT>
            }

            // is it a Map?

            if (kClass == Map::class || kClass == MutableMap::class || kClass == LinkedHashMap::class ||
                    kClass == HashMap::class) {
                // use simpler form of key and value type determination
                val keyType = JSONDeserializer.getTypeParam(kType.arguments, 0).applyTypeParameters(kType)
                val keySerializer = findSerializer(keyType, config)
                val valueType = JSONDeserializer.getTypeParam(kType.arguments, 1).applyTypeParameters(kType)
                val valueSerializer = findSerializer(valueType, config)
                return createMapSerializer(keyType, keySerializer, valueSerializer) as Serializer<TT>
            }

            if (kClass.isSubclassOf(Map::class)) {
                // derive type information from "get" operation
                val getFunction = kClass.functions.find { it.name == "get" && it.parameters.size == 2 &&
                        it.parameters.first().kind == KParameter.Kind.INSTANCE }
                val keyType = (getFunction?.parameters?.get(1)?.type ?: anyQType).applyTypeParameters(kType)
                val keySerializer = findSerializer(keyType, config)
                val valueType = (getFunction?.returnType ?: anyQType).applyTypeParameters(kType)
                val valueSerializer = findSerializer(valueType, config)
                return createMapSerializer(keyType, keySerializer, valueSerializer) as Serializer<TT>
            }

            // is it a Sequence?

            if (kClass.isSubclassOf(Sequence::class)) {
                val itemType = JSONDeserializer.getTypeParam(kType.arguments).applyTypeParameters(kType)
                return SequenceSerializer(
                    itemSerializer = findSerializer(itemType, config),
                ) as Serializer<TT>
            }

            // is it a Flow?

            if (kClass.isSubclassOf(Flow::class)) {
                val itemType = JSONDeserializer.getTypeParam(kType.arguments).applyTypeParameters(kType)
                return FlowSerializer(
                    itemSerializer = findSerializer(itemType, config),
                ) as Serializer<TT>
            }

            // is it a Channel?

            if (kClass.isSubclassOf(Channel::class)) {
                val itemType = JSONDeserializer.getTypeParam(kType.arguments).applyTypeParameters(kType)
                return ChannelSerializer(
                    itemSerializer = findSerializer(itemType, config),
                ) as Serializer<TT>
            }

            // is it a BaseStream?

            if (kClass.isSubclassOf(BaseStream::class)) {
                val itemType = JSONDeserializer.getTypeParam(kType.arguments).applyTypeParameters(kType)
                return BaseStreamSerializer(
                    itemSerializer = findSerializer(itemType, config),
                ) as Serializer<TT>
            }

            // is it an Enumeration?

            if (kClass.isSubclassOf(Enumeration::class)) {
                val itemType = JSONDeserializer.getTypeParam(kType.arguments).applyTypeParameters(kType)
                return EnumerationSerializer(
                    itemSerializer = findSerializer(itemType, config),
                ) as Serializer<TT>
            }

            // is it an Iterator?

            if (kClass.isSubclassOf(Iterator::class)) {
                val itemType = JSONDeserializer.getTypeParam(kType.arguments).applyTypeParameters(kType)
                return IteratorSerializer(
                    itemSerializer = findSerializer(itemType, config),
                ) as Serializer<TT>
            }

            // is it a Pair?

            if (kClass == Pair::class) {
                val firstType = JSONDeserializer.getTypeParam(kType.arguments, 0).applyTypeParameters(kType)
                val secondType = JSONDeserializer.getTypeParam(kType.arguments, 1).applyTypeParameters(kType)
                return PairSerializer(
                    firstSerializer = findSerializer(firstType, config),
                    secondSerializer = findSerializer(secondType, config),
                ) as Serializer<TT>
            }

            // is it a Triple?

            if (kClass == Triple::class) {
                val firstType = JSONDeserializer.getTypeParam(kType.arguments, 0).applyTypeParameters(kType)
                val secondType = JSONDeserializer.getTypeParam(kType.arguments, 1).applyTypeParameters(kType)
                val thirdType = JSONDeserializer.getTypeParam(kType.arguments, 2).applyTypeParameters(kType)
                return TripleSerializer(
                    firstSerializer = findSerializer(firstType, config),
                    secondSerializer = findSerializer(secondType, config),
                    thirdSerializer = findSerializer(thirdType, config),
                ) as Serializer<TT>
            }

            // is it an Opt?

            if (kClass == Opt::class) {
                val optType = JSONDeserializer.getTypeParam(kType.arguments).applyTypeParameters(kType)
                return OptSerializer(
                    optSerializer = findSerializer(optType, config),
                ) as Serializer<TT>
            }

            // serialize as an object

            return if (kClass.isKotlinClass())
                createObjectSerializer(kType, kClass, config)
            else
                createJavaObjectSerializer(kClass, config)
        }

        @Suppress("unchecked_cast")
        private fun KClassifier?.asKClassAny(): KClass<Any> = (this as? KClass<Any>) ?: Any::class

        private fun createMapSerializer(
            keyType: KType,
            keySerializer: Serializer<Any>,
            valueSerializer: Serializer<Any>,
        ): Serializer<*> {
            return when {
                !keyType.isMarkedNullable && keySerializer is StringSerializer -> MapSerializer(
                    keyClass = keyType.classifier.asKClassAny(),
                    keySerializer = keySerializer,
                    valueSerializer = valueSerializer,
                )
                !keyType.isMarkedNullable && keySerializer == AnySerializer -> MapSerializer(
                    keyClass = keyType.classifier.asKClassAny(),
                    keySerializer = ToStringUnsafeSerializer,
                    valueSerializer = valueSerializer,
                )
                else -> PseudoMapSerializer(
                    keySerializer = keySerializer,
                    valueSerializer = valueSerializer,
                )
            }
        }

        @Suppress("unchecked_cast")
        internal fun <T : Any> createObjectSerializer(
            kType: KType,
            kClass: KClass<T>,
            config: JSONConfig,
        ): Serializer<T> {
            val sealedClassDiscriminator = kClass.findSealedClassDiscriminator(config)
            val statics: Collection<KProperty<*>> = kClass.staticProperties
            val includeAll = config.includeNullFields(kClass)
            val propertyDescriptors = mutableListOf<ObjectSerializer.PropertyDescriptor<Any>>()
            if (kClass.isData && kClass.constructors.isNotEmpty()) {
                // data classes will be a frequent use of serialization, so optimise for them
                val constructor = kClass.constructors.first()
                for (parameter in constructor.parameters) {
                    val member = kClass.members.find { it.name == parameter.name }
                    if (member is KProperty<*> && member.visibility == KVisibility.PUBLIC) {
                        val annotations = parameter.annotations
                        if (!config.hasIgnoreAnnotation(annotations)) {
                            val name = config.findNameFromAnnotation(annotations) ?: member.name
                            if (sealedClassDiscriminator == null || name != sealedClassDiscriminator.name) {
                                val getter = member.getter
                                val propertyType = getter.returnType.applyTypeParameters(kType).let {
                                    if (it.classifier is KClass<*>) it else anyQType
                                }
                                propertyDescriptors.add(
                                    ObjectSerializer.KotlinPropertyDescriptor(
                                        name = name,
                                        kClass = propertyType.classifier as KClass<Any>,
                                        serializer = findSerializer(propertyType, config),
                                        includeIfNull = includeAll || config.hasIncludeIfNullAnnotation(annotations),
                                        getter = getter,
                                    )
                                )
                            }
                        }
                    }
                }
                for (member in kClass.members) {
                    if (member is KProperty<*> && member.visibility == KVisibility.PUBLIC &&
                            !statics.contains(member) && !constructor.parameters.any { it.name == member.name }) {
                        val annotations = member.annotations
                        if (!config.hasIgnoreAnnotation(annotations)) {
                            val name = config.findNameFromAnnotation(annotations) ?: member.name
                            val getter = member.getter
                            val propertyType = getter.returnType.applyTypeParameters(kType).let {
                                if (it.classifier is KClass<*>) it else anyQType
                            }
                            propertyDescriptors.add(
                                ObjectSerializer.KotlinPropertyDescriptor(
                                    name = name,
                                    kClass = propertyType.classifier as KClass<Any>,
                                    serializer = findSerializer(propertyType, config),
                                    includeIfNull = includeAll || config.hasIncludeIfNullAnnotation(annotations),
                                    getter = getter,
                                )
                            )
                        }
                    }
                }
            }
            else {
                for (member in kClass.members) {
                    if (member is KProperty<*> && member.visibility == KVisibility.PUBLIC &&
                            !statics.contains(member)) {
                        val annotations = member.annotations
                        if (!config.hasIgnoreAnnotation(annotations)) {
                            val name = config.findNameFromAnnotation(annotations) ?: member.name
                            val getter = member.getter
                            val propertyType = getter.returnType.applyTypeParameters(kType).let {
                                if (it.classifier is KClass<*>) it else anyQType
                            }
                            propertyDescriptors.add(
                                ObjectSerializer.KotlinPropertyDescriptor(
                                    name = name,
                                    kClass = propertyType.classifier as KClass<Any>,
                                    serializer = findSerializer(propertyType, config),
                                    includeIfNull = includeAll || config.hasIncludeIfNullAnnotation(annotations),
                                    getter = getter,
                                )
                            )
                        }
                    }
                }
            }
            return ObjectSerializer(
                kClass = kClass,
                sealedClassDiscriminator = sealedClassDiscriminator,
                propertyDescriptors = propertyDescriptors,
            )
        }

        @Suppress("unchecked_cast")
        private fun <T : Any> createJavaObjectSerializer(
            kClass: KClass<T>,
            config: JSONConfig,
        ): Serializer<T> {
            val jClass = kClass.java
            val propertyDescriptors = mutableListOf<ObjectSerializer.PropertyDescriptor<Any>>()
            val classHierarchy = getJavaClassHierarchy(jClass)
            val includeAll = config.includeNullFields(kClass)
            while (classHierarchy.isNotEmpty()) {
                val methods = classHierarchy.removeFirst().declaredMethods
                for (method in methods) {
                    if (!method.isStaticOrTransient() && method.parameters.isEmpty()) {
                        val annotations = method.annotations.asList()
                        if (!config.hasIgnoreAnnotation(annotations)) {
                            val methodName = config.findNameFromAnnotation(annotations) ?: method.name
                            if (methodName.length > 3 && methodName.startsWith("get") && methodName[3] in 'A'..'Z') {
                                val name = methodName[3].lowercase(Locale.US) + methodName.substring(4)
                                val propertyClass = method.returnType
                                val propertyType = propertyClass.kotlin.starProjectedType // TODO revisit this?
                                propertyDescriptors.removeIf { it.name == name }
                                propertyDescriptors.add(
                                    ObjectSerializer.JavaPropertyDescriptor(
                                        name = name,
                                        kClass = kClass as KClass<Any>,
                                        serializer = findSerializer(propertyType, config),
                                        includeIfNull = includeAll || config.hasIncludeIfNullAnnotation(annotations),
                                        getter = method,
                                    )
                                )
                            }
                            else if (methodName.length > 2 && methodName.startsWith("is") &&
                                    methodName[2] in 'A'..'Z' && method.returnType == java.lang.Boolean.TYPE) {
                                val name = methodName[2].lowercase(Locale.US) + methodName.substring(3)
                                val propertyClass = java.lang.Boolean.TYPE
                                val propertyType = propertyClass.toKType(nullable = true)
                                propertyDescriptors.removeIf { it.name == name }
                                propertyDescriptors.add(
                                    ObjectSerializer.JavaPropertyDescriptor(
                                        name = name,
                                        kClass = kClass as KClass<Any>,
                                        serializer = findSerializer(propertyType, config),
                                        includeIfNull = includeAll || config.hasIncludeIfNullAnnotation(annotations),
                                        getter = method,
                                    )
                                )
                            }
                        }
                    }
                }
            }
            return ObjectSerializer(
                kClass = kClass,
                sealedClassDiscriminator = null,
                propertyDescriptors = propertyDescriptors,
            )
        }

    }

}
