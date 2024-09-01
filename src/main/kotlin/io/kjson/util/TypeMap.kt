/*
 * @(#) TypeMap.kt
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

package io.kjson.util

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

import java.util.HashMap

class TypeMap<T>(
    private val initialEntries: List<Pair<KClass<*>, T>>,
) {

    internal val classMap = HashMap<KClass<*>, ClassEntry<T>>(initialEntries.size + 20)

    operator fun get(kClass: KClass<*>): T? {
        fillInitialIfEmpty()
        return classMap[kClass]?.getTypeHandler() // should we check that class has no parameters?
    }

    operator fun get(kType: KType): T? {
        fillInitialIfEmpty()
        val kClass = kType.classifier as? KClass<*> ?: return null // throw exception?
        return classMap[kClass]?.getTypeHandler(kType)
    }

    operator fun set(kType: KType, typeHandler: T): T? {
        fillInitialIfEmpty()
        val kClass = kType.classifier as? KClass<*> ?: return null // throw exception?
        val arguments = kType.arguments
        val classEntry = classMap[kClass]
        if (classEntry == null) {
            classMap[kClass] = if (arguments.isEmpty())
                SimpleClassEntry(typeHandler)
            else
                ParameterizedClassEntry(arguments.size, mutableListOf((typeHandler) to arguments))
            return null
        }
        val previous = classEntry.getTypeHandler(kType)
        classMap[kClass] = classEntry.setTypeHandler(typeHandler, kType)
        return previous
    }

    operator fun set(kClass: KClass<*>, typeHandler: T): T? {
        fillInitialIfEmpty()
        val classEntry = classMap[kClass]
        if (classEntry == null) {
            classMap[kClass] = SimpleClassEntry(typeHandler)
            return null
        }
        val previous = classEntry.getTypeHandler()
        classMap[kClass] = classEntry.setTypeHandler(typeHandler)
        return previous
    }

    fun remove(kClass: KClass<*>) {
        fillInitialIfEmpty()
        classMap.remove(kClass)
    }

    fun clear() {
        classMap.clear()
    }

    private fun fillInitialIfEmpty() {
        if (classMap.isEmpty()) {
            for (entry in initialEntries)
                classMap[entry.first] = SimpleClassEntry(entry.second)
        }
    }

    sealed interface ClassEntry<T> {
        fun getTypeHandler(): T?
        fun getTypeHandler(kType: KType): T?
        fun setTypeHandler(typeHandler: T): ClassEntry<T>
        fun setTypeHandler(typeHandler: T, kType: KType): ClassEntry<T>
    }

    data class SimpleClassEntry<T>(
        val handler: T
    ) : ClassEntry<T> {

        override fun getTypeHandler(): T = handler

        override fun getTypeHandler(kType: KType): T? {
            if (kType.arguments.isNotEmpty())
                return null // throw exception?
            return handler
        }

        override fun setTypeHandler(typeHandler: T): SimpleClassEntry<T> {
            return SimpleClassEntry(typeHandler)
        }

        override fun setTypeHandler(typeHandler: T, kType: KType): ClassEntry<T> {
            if (kType.arguments.isNotEmpty())
                return this // throw exception?
            return SimpleClassEntry(typeHandler)
        }

    }

    data class ParameterizedClassEntry<T>(
        val numParams: Int,
        val candidates: MutableList<Pair<T, List<KTypeProjection>>>,
    ) : ClassEntry<T> {

        override fun getTypeHandler(): T? = null

        override fun getTypeHandler(kType: KType): T? {
            val arguments = kType.arguments
            if (arguments.size != numParams)
                return null // throw exception?
            for (candidate in candidates) {
                if (arguments sameAs candidate.second)
                    return candidate.first
            }
            return null
        }

        override fun setTypeHandler(typeHandler: T): ClassEntry<T> = this // throw exception?

        override fun setTypeHandler(typeHandler: T, kType: KType): ClassEntry<T> {
            val arguments = kType.arguments
            if (arguments.size != numParams)
                return this // throw exception?
            for (i in candidates.indices) {
                if (arguments sameAs candidates[i].second) {
                    candidates[i] = typeHandler to arguments
                    return this
                }
            }
            candidates.add(typeHandler to arguments)
            return this
        }
    }

    companion object {

        private infix fun KType.sameAs(other: KType): Boolean = classifier == other.classifier &&
                isMarkedNullable == other.isMarkedNullable && arguments sameAs other.arguments

        infix fun List<KTypeProjection>.sameAs(other: List<KTypeProjection>): Boolean {
            if (size != other.size)
                return false
            for (i in indices) {
                val thisItemType = this[i].type
                val otherItemType = other[i].type
                if (thisItemType == null || otherItemType == null)
                    return false
                if (!(thisItemType sameAs otherItemType))
                    return false
            }
            return true
        }

    }

}
