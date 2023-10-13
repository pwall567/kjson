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

import io.kjson.JSONSerializerFunctions.findSealedClass
import io.kjson.JSONSerializerFunctions.findToJSON
import io.kjson.JSONSerializerFunctions.getCombinedAnnotations
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
import net.pwall.util.CoOutputFlushable
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
     * @receiver        the output function (`(Char) -> Unit`)
     * @param   obj     the object to be converted to JSON (`null` will be converted to `"null"`)
     * @param   config  an optional [JSONConfig] to customise the conversion
     */
    suspend fun CoOutput.outputJSON(
        obj: Any?,
        config: JSONConfig = JSONConfig.defaultConfig,
    ) {
        outputJSONInternal(obj, JSONContext(config), mutableListOf())
    }

    private suspend fun CoOutput.outputJSONInternal(
        obj: Any?,
        context: JSONContext,
        references: MutableList<Any>,
    ) {

        if (obj == null) {
            output("null")
            return
        }

        if (obj is JSONValue) {
            obj.coOutput(this)
            return
        }

        val config = context.config
        config.findToJSONMapping(obj::class)?.let {
            outputJSONInternal(context.it(obj), context, references)
            return
        }

        if (obj is Enum<*> || obj::class.isToStringClass()) {
            outputString(obj.toString(), config.stringifyNonASCII)
            return
        }

        when (obj) {
            is CharSequence -> outputString(obj, config.stringifyNonASCII)
            is CharArray -> outputQuoted {
                for (i in obj.indices)
                    outputChar(obj[i], config.stringifyNonASCII)
            }
            is Char -> outputQuoted { outputChar(obj, config.stringifyNonASCII) }
            is Number -> outputNumber(obj, context, references)
            is Boolean -> output(obj.toString())
            is UInt -> outputUnsignedInt(obj.toInt())
            is UShort -> outputInt(obj.toInt())
            is UByte -> outputInt(obj.toInt())
            is ULong -> outputUnsignedLong(obj.toLong())
            is BitSet -> outputBitSet(obj)
            is Calendar -> outputQuoted { outputCalendar(obj) }
            is Date -> outputQuoted { outputDate(obj) }
            is Duration -> outputQuoted { output(obj.toIsoString()) }
            is Instant -> outputQuoted { outputInstant(obj) }
            is OffsetDateTime -> outputQuoted { outputOffsetDateTime(obj) }
            is OffsetTime -> outputQuoted { outputOffsetTime(obj) }
            is LocalDateTime -> outputQuoted { outputLocalDateTime(obj) }
            is LocalDate -> outputQuoted { outputLocalDate(obj) }
            is LocalTime -> outputQuoted { outputLocalTime(obj) }
            is Year -> outputQuoted { outputYear(obj) }
            is YearMonth -> outputQuoted { outputYearMonth(obj) }
            is MonthDay -> outputQuoted { outputMonthDay(obj) }
            is UUID -> outputQuoted { outputUUID(obj) }
            is IntArray -> outputTypedArray(obj.size) { outputInt(obj[it]) }
            is LongArray -> outputTypedArray(obj.size) { outputLong(obj[it]) }
            is ByteArray -> outputTypedArray(obj.size) { outputInt(obj[it].toInt()) }
            is ShortArray -> outputTypedArray(obj.size) { outputInt(obj[it].toInt()) }
            is FloatArray -> outputTypedArray(obj.size) { output(obj[it].toString()) }
            is DoubleArray -> outputTypedArray(obj.size) { output(obj[it].toString()) }
            is BooleanArray -> outputTypedArray(obj.size) { output(obj[it].toString()) }
            else -> outputObject(obj, context, references)
        }

    }

    private suspend fun CoOutput.outputNumber(
        number: Number,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        when (number) {
            is Int -> outputInt(number)
            is Long -> outputLong(number)
            is Short, is Byte -> outputInt(number.toInt())
            is Float, is Double -> output(number.toString())
            is BigInteger -> {
                if (context.config.bigIntegerString)
                    outputString(number.toString(), false)
                else
                    output(number.toString())
            }
            is BigDecimal -> {
                if (context.config.bigDecimalString)
                    outputString(number.toString(), false)
                else
                    output(number.toString())
            }
            else -> outputObject(number, context, references)
        }
    }

    private suspend fun CoOutput.outputObject(
        obj: Any,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        if (obj in references)
            context.fatal("Circular reference to ${obj::class.simpleName}")
        references.add(obj)
        try {
            val objClass = obj::class
            objClass.findToJSON()?.let {
                try {
                    outputJSONInternal(it.call(obj), context, references)
                    return
                }
                catch (e: JSONException) {
                    throw e
                }
                catch (e: Exception) {
                    context.fatal("Error in custom toJSON - ${objClass.simpleName}", e)
                }
            }
            when (obj) {
                is Array<*> -> outputArray(obj, context, references)
                is Pair<*, *> -> outputPair(obj, context, references)
                is Triple<*, *, *> -> outputTriple(obj, context, references)
                is Iterable<*> -> outputIterator(obj.iterator(), context, references)
                is Iterator<*> -> outputIterator(obj, context, references)
                is Sequence<*> -> outputIterator(obj.iterator(), context, references)
                is Channel<*> -> outputChannel(obj.iterator(), context, references)
                is Flow<*> -> outputFlow(obj, context, references)
                is Enumeration<*> -> outputIterator(obj.iterator(), context, references)
                is BaseStream<*, *> -> outputIterator(obj.iterator(), context, references)
                is Map<*, *> -> outputMap(obj, context, references)
                is Opt<*> -> outputJSONInternal(obj.orNull, context, references)
                else -> {
                    val config = context.config
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
                    val includeAll = config.includeNullFields(objClass)
                    val statics: Collection<KProperty<*>> = objClass.staticProperties
                    if (objClass.isData && objClass.constructors.isNotEmpty()) {
                        // data classes will be a frequent use of serialization, so optimise for them
                        val constructor = objClass.constructors.first()
                        for (parameter in constructor.parameters) {
                            val member = objClass.members.find { it.name == parameter.name }
                            if (member is KProperty<*>)
                                continuation = outputUsingGetter(
                                    member = member,
                                    annotations = parameter.annotations,
                                    obj = obj,
                                    context = context,
                                    references = references,
                                    includeAll = includeAll,
                                    skipName = skipName,
                                    continuation = continuation,
                                )
                        }
                        // now check whether there are any more properties not in constructor
                        for (member in objClass.members) {
                            if (member is KProperty<*> && !statics.contains(member) &&
                                    !constructor.parameters.any { it.name == member.name })
                                continuation = outputUsingGetter(
                                    member = member,
                                    annotations = member.annotations,
                                    obj = obj,
                                    context = context,
                                    references = references,
                                    includeAll = includeAll,
                                    skipName = skipName,
                                    continuation = continuation,
                                )
                        }
                    }
                    else {
                        for (member in objClass.members) {
                            if (member is KProperty<*> && !statics.contains(member))
                                continuation = outputUsingGetter(
                                    member = member,
                                    annotations = member.getCombinedAnnotations(objClass),
                                    obj = obj,
                                    context = context,
                                    references = references,
                                    includeAll = includeAll,
                                    skipName = skipName,
                                    continuation = continuation,
                                )
                        }
                    }
                    output('}')
                }
            }
        }
        finally {
            references.remove(obj)
        }
    }

    private suspend fun CoOutput.outputUsingGetter(
        member: KProperty<*>,
        annotations: List<Annotation>?,
        obj: Any,
        context: JSONContext,
        references: MutableList<Any>,
        includeAll: Boolean,
        skipName: String?,
        continuation: Boolean,
    ): Boolean {
        val config = context.config
        if (!config.hasIgnoreAnnotation(annotations)) {
            val name = config.findNameFromAnnotation(annotations) ?: member.name
            if (name != skipName) {
                val wasAccessible = member.isAccessible
                member.isAccessible = true
                try {
                    val v = member.getter.call(obj)
                    if (v != null || includeAll || config.hasIncludeIfNullAnnotation(annotations)) {
                        if (v is Opt<*>) {
                            if (v.isSet) {
                                outputObjectValue(name, v.value, context, references, continuation)
                                return true
                            }
                        }
                        else {
                            outputObjectValue(name, v, context, references, continuation)
                            return true
                        }
                    }
                }
                catch (e: JSONException) {
                    throw e
                }
                catch (e: Exception) {
                    context.fatal("Error getting property ${member.name} from ${obj::class.simpleName}", e)
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
        context: JSONContext,
        references: MutableList<Any>,
        continuation: Boolean,
    ) {
        if (continuation)
            output(',')
        outputString(name, context.config.stringifyNonASCII)
        output(':')
        outputJSONInternal(value, context.child(name), references)
    }

    private suspend fun CoOutput.outputArray(
        array: Array<*>,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        if (array.isArrayOf<Char>()) {
            outputQuoted {
                for (i in array.indices)
                    outputChar(array[i] as Char, context.config.stringifyNonASCII)
            }
        }
        else {
            outputTypedArray(array.size) {
                outputJSONInternal(array[it], context.child(it), references)
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
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        output('[')
        outputJSONInternal(pair.first, context.child(0), references)
        output(',')
        outputJSONInternal(pair.second, context.child(1), references)
        output(']')
    }

    private suspend fun CoOutput.outputTriple(
        triple: Triple<*, *, *>,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        output('[')
        outputJSONInternal(triple.first, context.child(0), references)
        output(',')
        outputJSONInternal(triple.second, context.child(1), references)
        output(',')
        outputJSONInternal(triple.third, context.child(2), references)
        output(']')
    }

    private suspend fun CoOutput.outputIterator(
        iterator: Iterator<*>,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        output('[')
        if (iterator.hasNext()) {
            var index = 0
            while (true) {
                outputJSONInternal(iterator.next(), context.child(index++), references)
                if (!iterator.hasNext())
                    break
                output(',')
            }
        }
        output(']')
    }

    private suspend fun CoOutput.outputMap(
        map: Map<*, *>,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        output('{')
        map.entries.iterator().let {
            if (it.hasNext()) {
                while (true) {
                    val (key, value) = it.next()
                    val keyString = key.toString()
                    outputString(keyString, context.config.stringifyNonASCII)
                    output(':')
                    outputJSONInternal(value, context.child(keyString), references)
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
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        output('[')
        if (iterator.hasNext()) {
            var index = 0
            while (true) {
                outputJSONInternal(iterator.next(), context.child(index++), references)
                if (this is CoOutputFlushable)
                    flush()
                if (!iterator.hasNext())
                    break
                output(',')
            }
        }
        output(']')
    }

    private suspend fun CoOutput.outputFlow(
        flow: Flow<*>,
        context: JSONContext,
        references: MutableList<Any>,
    ) {
        output('[')
        var index = 0
        var continuation = false
        flow.collect {
            if (continuation)
                output(',')
            outputJSONInternal(it, context.child(index++), references)
            if (this is CoOutputFlushable)
                flush()
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
