/*
 * @(#) reflectionUtil.kt
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

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

import java.lang.reflect.Member
import java.lang.reflect.Modifier

import io.kjson.JSONConfig
import io.kjson.annotation.JSONDiscriminator
import io.kjson.annotation.JSONIdentifier

fun KClass<*>.isKotlinClass(): Boolean = java.getAnnotation(Metadata::class.java)?.kind == 1

fun KClass<*>.findSealedClass(): KClass<*>? {
    for (supertype in supertypes) {
        (supertype.classifier as? KClass<*>)?.let {
            if (it.isSealed)
                return it
            if (it != Any::class)
                it.findSealedClass()?.let { c -> return c }
        }
    }
    return null
}

fun KClass<*>.discriminatorName(config: JSONConfig): String =
    findAnnotation<JSONDiscriminator>()?.id ?: config.sealedClassDiscriminator

fun KClass<*>.discriminatorValue(): String = findAnnotation<JSONIdentifier>()?.id ?: simpleName ?: "null"

fun KClass<*>.findSealedClassDiscriminator(config: JSONConfig): NameValuePair? = findSealedClass()?.let {
    NameValuePair(it.discriminatorName(config), discriminatorValue())
}

fun KCallable<*>.getCombinedAnnotations(objClass: KClass<*>): List<Annotation> =
    annotations.combinedWith(objClass.findConstructorParameter(name)?.annotations)

fun KClass<*>.findConstructorParameter(name: String): KParameter? =
    constructors.firstOrNull()?.parameters?.find { it.name == name }

fun <T> List<T>.combinedWith(otherList: List<T>?): List<T> = when {
    otherList.isNullOrEmpty() -> this
    this.isEmpty() -> otherList
    else -> this + otherList
}

fun getJavaClassHierarchy(jClass: Class<*>): MutableList<Class<*>> {
    val result = mutableListOf<Class<*>>()
    var currentClass = jClass
    while (true) {
        result.add(0, currentClass)
        currentClass.superclass?.takeIf { it != java.lang.Object::class.java }?.let { currentClass = it } ?: break
    }
    return result
}

fun Member.isPublic(): Boolean {
    return Modifier.isPublic(modifiers)
}

fun Member.isStatic(): Boolean {
    return Modifier.isStatic(modifiers)
}

fun Member.isTransient(): Boolean {
    return Modifier.isTransient(modifiers)
}

fun Member.isStaticOrTransient(): Boolean {
    return isStatic() || isTransient()
}
