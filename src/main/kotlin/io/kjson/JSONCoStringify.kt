/*
 * @(#) JSONCoStringify.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2022, 2023 Peter Wall
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

import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.time.Duration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelIterator
import kotlinx.coroutines.flow.Flow

import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Year
import java.time.YearMonth
import java.util.BitSet
import java.util.Calendar
import java.util.Date
import java.util.Enumeration
import java.util.UUID
import java.util.stream.BaseStream

import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.JSONSerializerFunctions.findSealedClass
import io.kjson.JSONSerializerFunctions.findToJSON
import io.kjson.JSONSerializerFunctions.isToStringClass
import io.kjson.annotation.JSONDiscriminator
import io.kjson.annotation.JSONIdentifier
import io.kjson.optional.Opt
import net.pwall.json.JSONCoFunctions.outputChar
import net.pwall.json.JSONCoFunctions.outputString
import net.pwall.util.CoDateOutput.outputCalendar
import net.pwall.util.CoDateOutput.outputDate
import net.pwall.util.CoDateOutput.outputInstant
import net.pwall.util.CoDateOutput.outputLocalDate
import net.pwall.util.CoDateOutput.outputLocalDateTime
import net.pwall.util.CoDateOutput.outputLocalTime
import net.pwall.util.CoDateOutput.outputMonthDay
import net.pwall.util.CoDateOutput.outputOffsetDateTime
import net.pwall.util.CoDateOutput.outputOffsetTime
import net.pwall.util.CoDateOutput.outputYear
import net.pwall.util.CoDateOutput.outputYearMonth
import net.pwall.util.CoIntOutput.output4HexLC
import net.pwall.util.CoIntOutput.output8HexLC
import net.pwall.util.CoIntOutput.outputInt
import net.pwall.util.CoIntOutput.outputLong
import net.pwall.util.CoIntOutput.outputUnsignedInt
import net.pwall.util.CoIntOutput.outputUnsignedLong
import net.pwall.util.CoOutput
import net.pwall.util.output

/**
 * Reflection-based non-blocking JSON serialization for Kotlin - serialize direct to a non-blocking function.
 *
 * @author  Peter Wall
 */
object JSONCoStringify {

    /**
     * Stringify an object to JSON, using a non-blocking output function.  The output of the serialization process will
     * be supplied to the output function a character at a time.
     *
     * @param   obj     the object to be converted to JSON (`null` will be converted to `"null"`)
     * @param   config  an optional [JSONConfig] to customise the conversion
     * @param   out     the output function (`(char) -> Unit`)
     */
    suspend fun coStringify(
        obj: Any?,
        config: JSONConfig = JSONConfig.defaultConfig,
        out: CoOutput,
    ) {
        when (obj) {
            null -> out.output("null")
            else -> out.outputJSON(obj, config)
        }
    }

    /**
     * Stringify an object to JSON, as an extension function to a non-blocking output function.  The output of the
     * serialization process will be supplied to the output function a character at a time.
     *
     * @receiver        the output function (`(char) -> Unit`)
     * @param   obj     the object to be converted to JSON (`null` will be converted to `"null"`)
     * @param   config  an optional [JSONConfig] to customise the conversion
     */
    suspend fun CoOutput.outputJSON(
        obj: Any?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ) {
        outputJSONInternal(obj, config, mutableSetOf())
    }

    private suspend fun CoOutput.outputJSONInternal(
        obj: Any?,
        config: JSONConfig,
        references: MutableSet<Any>,
    ) {

        if (obj == null) {
            output("null")
            return
        }

        config.findToJSONMapping(obj::class)?.let {
            outputJSONInternal(config.it(obj), config, references)
            return
        }

        when (obj) {
            is JSONValue -> obj.coOutput(this)
            is CharSequence -> outputString(obj, config.stringifyNonASCII)
            is Char -> outputQuoted { outputChar(obj, config.stringifyNonASCII) }
            is CharArray -> outputQuoted {
                for (i in obj.indices)
                    outputChar(obj[i], config.stringifyNonASCII)
            }
            is Number -> outputNumber(obj, config, references)
            is UInt -> outputUnsignedInt(obj.toInt())
            is ULong -> outputUnsignedLong(obj.toLong())
            is UShort -> outputInt(obj.toInt())
            is UByte -> outputInt(obj.toInt())
            is Boolean -> output(obj.toString())
            is Array<*> -> outputArray(obj, config, references)
            is Pair<*, *> -> outputPair(obj, config, references)
            is Triple<*, *, *> -> outputTriple(obj, config, references)
            is IntArray -> outputTypedArray(obj.size) { outputInt(obj[it]) }
            is LongArray -> outputTypedArray(obj.size) { outputLong(obj[it]) }
            is ByteArray -> outputTypedArray(obj.size) { outputInt(obj[it].toInt()) }
            is ShortArray -> outputTypedArray(obj.size) { outputInt(obj[it].toInt()) }
            is FloatArray -> outputTypedArray(obj.size) { output(obj[it].toString()) }
            is DoubleArray -> outputTypedArray(obj.size) { output(obj[it].toString()) }
            is BooleanArray -> outputTypedArray(obj.size) { output(obj[it].toString()) }
            else -> outputObject(obj, config, references)
        }

    }

    private suspend fun CoOutput.outputNumber(
        number: Number,
        config: JSONConfig,
        references: MutableSet<Any>,
    ) {
        when (number) {
            is Int -> outputInt(number)
            is Long -> outputLong(number)
            is Short, is Byte -> outputInt(number.toInt())
            is Float, is Double -> output(number.toString())
            is BigInteger -> {
                if (config.bigIntegerString)
                    outputString(number.toString(), false)
                else
                    output(number.toString())
            }
            is BigDecimal -> {
                if (config.bigDecimalString)
                    outputString(number.toString(), false)
                else
                    output(number.toString())
            }
            else -> outputObject(number, config, references)
        }
    }

    private suspend fun CoOutput.outputArray(
        array: Array<*>,
        config: JSONConfig,
        references: MutableSet<Any>,
    ) {
        if (array.isArrayOf<Char>()) {
            outputQuoted {
                for (i in array.indices)
                    outputChar(array[i] as Char, config.stringifyNonASCII)
            }
        }
        else {
            outputTypedArray(array.size) {
                outputJSONInternal(array[it], config, references)
            }
        }
    }

    private suspend fun CoOutput.outputTypedArray(
        size: Int,
        itemFunction: suspend (Int) -> Unit,
    ) {
        output('[')
        if (size > 0) {
            for (i in 0 until size) {
                if (i > 0)
                    output(',')
                itemFunction(i)
            }
        }
        output(']')
    }

    private suspend fun CoOutput.outputPair(
        pair: Pair<*, *>,
        config: JSONConfig,
        references: MutableSet<Any>,
    ) {
        output('[')
        outputJSONInternal(pair.first, config, references)
        output(',')
        outputJSONInternal(pair.second, config, references)
        output(']')
    }

    private suspend fun CoOutput.outputTriple(
        triple: Triple<*, *, *>,
        config: JSONConfig,
        references: MutableSet<Any>,
    ) {
        output('[')
        outputJSONInternal(triple.first, config, references)
        output(',')
        outputJSONInternal(triple.second, config, references)
        output(',')
        outputJSONInternal(triple.third, config, references)
        output(']')
    }

    private suspend fun CoOutput.outputObject(
        obj: Any,
        config: JSONConfig,
        references: MutableSet<Any>,
    ) {
        val objClass = obj::class
        if (objClass.isToStringClass() || obj is Enum<*>) {
            outputString(obj.toString(), config.stringifyNonASCII)
            return
        }
        objClass.findToJSON()?.let {
            try {
                outputJSONInternal(it.call(obj), config, references)
                return
            }
            catch (e: Exception) {
                fatal("Error in custom toJSON - ${objClass.simpleName}", e)
            }
        }
        when (obj) {
            is Iterable<*> -> outputIterator(obj.iterator(), config, references)
            is Iterator<*> -> outputIterator(obj, config, references)
            is Sequence<*> -> outputIterator(obj.iterator(), config, references)
            is Channel<*> -> outputChannel(obj.iterator(), config, references)
            is Flow<*> -> outputFlow(obj, config, references)
            is Enumeration<*> -> outputIterator(obj.iterator(), config, references)
            is BaseStream<*, *> -> outputIterator(obj.iterator(), config, references)
            is Map<*, *> -> outputMap(obj, config, references)
            is BitSet -> outputBitSet(obj)
            is Calendar -> outputQuoted { outputCalendar(obj) }
            is Date -> outputQuoted { outputDate(obj) }
            is Instant -> outputQuoted { outputInstant(obj) }
            is LocalDate -> outputQuoted { outputLocalDate(obj) }
            is LocalDateTime -> outputQuoted { outputLocalDateTime(obj) }
            is LocalTime -> outputQuoted { outputLocalTime(obj) }
            is OffsetTime -> outputQuoted { outputOffsetTime(obj) }
            is OffsetDateTime -> outputQuoted { outputOffsetDateTime(obj) }
            is Year -> outputQuoted { outputYear(obj) }
            is YearMonth -> outputQuoted { outputYearMonth(obj) }
            is MonthDay -> outputQuoted { outputMonthDay(obj) }
            is Duration -> outputQuoted { output(obj.toIsoString()) }
            is UUID -> outputQuoted { outputUUID(obj) }
            is Opt<*> -> outputJSONInternal(obj.orNull, config, references)
            else -> {
                references.add(obj)
                try {
                    output('{')
                    var continuation = false
                    val skipName = objClass.findSealedClass()?.let {
                        val discriminatorName = it.findAnnotation<JSONDiscriminator>()?.id ?:
                                config.sealedClassDiscriminator
                        outputString(discriminatorName, config.stringifyNonASCII)
                        output(':')
                        outputString(objClass.findAnnotation<JSONIdentifier>()?.id ?: objClass.simpleName ?: "null",
                                config.stringifyNonASCII)
                        continuation = true
                        discriminatorName
                    }
                    val includeAll = config.hasIncludeAllPropertiesAnnotation(objClass.annotations)
                    val statics: Collection<KProperty<*>> = objClass.staticProperties
                    if (objClass.isData && objClass.constructors.isNotEmpty()) {
                        // data classes will be a frequent use of serialization, so optimise for them
                        val constructor = objClass.constructors.first()
                        for (parameter in constructor.parameters) {
                            val member = objClass.members.find { it.name == parameter.name }
                            if (member is KProperty<*>)
                                continuation = outputUsingGetter(member, parameter.annotations, obj, config, references,
                                        includeAll, skipName, continuation)
                        }
                        // now check whether there are any more properties not in constructor
                        for (member in objClass.members) {
                            if (member is KProperty<*> && !statics.contains(member) &&
                                    !constructor.parameters.any { it.name == member.name })
                                continuation = outputUsingGetter(member, member.annotations, obj, config, references,
                                        includeAll, skipName, continuation)
                        }
                    }
                    else {
                        for (member in objClass.members) {
                            if (member is KProperty<*> && !statics.contains(member)) {
                                val combinedAnnotations = ArrayList(member.annotations)
                                objClass.constructors.firstOrNull()?.parameters?.find { it.name == member.name }?.let {
                                    combinedAnnotations.addAll(it.annotations)
                                }
                                continuation = outputUsingGetter(member, combinedAnnotations, obj, config, references,
                                        includeAll, skipName, continuation)
                            }
                        }
                    }
                    output('}')
                }
                finally {
                    references.remove(obj)
                }
            }
        }
    }

    private suspend fun CoOutput.outputUsingGetter(
        member: KProperty<*>,
        annotations: List<Annotation>?,
        obj: Any,
        config: JSONConfig,
        references: MutableSet<Any>,
        includeAll: Boolean,
        skipName: String?,
        continuation: Boolean,
    ): Boolean {
        if (!config.hasIgnoreAnnotation(annotations)) {
            val name = config.findNameFromAnnotation(annotations) ?: member.name
            if (name != skipName) {
                val wasAccessible = member.isAccessible
                member.isAccessible = true
                try {
                    val v = member.getter.call(obj)
                    if (v != null && v in references)
                        fatal("Circular reference: property ${member.name} in ${obj::class.simpleName}")
                    if (v != null || config.hasIncludeIfNullAnnotation(annotations) || config.includeNulls ||
                            includeAll) {
                        if (v is Opt<*>) {
                            if (v.isSet) {
                                outputObjectValue(name, v.value, config, references, continuation)
                                return true
                            }
                        }
                        else {
                            outputObjectValue(name, v, config, references, continuation)
                            return true
                        }
                    }
                }
                catch (e: JSONException) {
                    throw e
                }
                catch (e: Exception) {
                    fatal("Error getting property ${member.name} from ${obj::class.simpleName}", e)
                }
                finally {
                    member.isAccessible = wasAccessible
                }
            }
        }
        return continuation
    }

    private suspend fun CoOutput.outputObjectValue(
        name: String,
        value: Any?,
        config: JSONConfig,
        references: MutableSet<Any>,
        continuation: Boolean,
    ) {
        if (continuation)
            output(',')
        outputString(name, config.stringifyNonASCII)
        output(':')
        outputJSONInternal(value, config, references)
    }

    private suspend fun CoOutput.outputIterator(
        iterator: Iterator<*>,
        config: JSONConfig,
        references: MutableSet<Any>,
    ) {
        output('[')
        if (iterator.hasNext()) {
            while (true) {
                outputJSONInternal(iterator.next(), config, references)
                if (!iterator.hasNext())
                    break
                output(',')
            }
        }
        output(']')
    }

    private suspend fun CoOutput.outputMap(
        map: Map<*, *>,
        config: JSONConfig,
        references: MutableSet<Any>,
    ) {
        output('{')
        map.entries.iterator().let {
            if (it.hasNext()) {
                while (true) {
                    val (key, value) = it.next()
                    outputString(key.toString(), config.stringifyNonASCII)
                    output(':')
                    outputJSONInternal(value, config, references)
                    if (!it.hasNext())
                        break
                    output(',')
                }
            }
        }
        output('}')
    }

    private suspend fun CoOutput.outputUUID(uuid: UUID) {
        val highBits = uuid.mostSignificantBits
        output8HexLC((highBits shr 32).toInt())
        output('-')
        output4HexLC((highBits shr 16).toInt())
        output('-')
        output4HexLC(highBits.toInt())
        output('-')
        val lowBits = uuid.leastSignificantBits
        output4HexLC((lowBits shr 48).toInt())
        output('-')
        output4HexLC((lowBits shr 32).toInt())
        output8HexLC(lowBits.toInt())
    }

    private suspend fun CoOutput.outputBitSet(bitSet: BitSet) {
        output('[')
        var continuation = false
        for (i in 0 until bitSet.length()) {
            if (bitSet.get(i)) {
                if (continuation)
                    output(',')
                outputInt(i)
                continuation = true
            }
        }
        output(']')
    }

    private suspend fun CoOutput.outputChannel(
        iterator: ChannelIterator<*>,
        config: JSONConfig,
        references: MutableSet<Any>,
    ) {
        output('[')
        if (iterator.hasNext()) {
            while (true) {
                outputJSONInternal(iterator.next(), config, references)
                if (!iterator.hasNext())
                    break
                output(',')
            }
        }
        output(']')
    }

    private suspend fun CoOutput.outputFlow(
        flow: Flow<*>,
        config: JSONConfig,
        references: MutableSet<Any>,
    ) {
        output('[')
        var continuation = false
        flow.collect {
            if (continuation)
                output(',')
            outputJSONInternal(it, config, references)
            continuation = true
        }
        output(']')
    }

    private suspend inline fun CoOutput.outputQuoted(block: CoOutput.() -> Unit) {
        output('"')
        block()
        output('"')
    }

}
