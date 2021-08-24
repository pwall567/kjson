/*
 * @(#) TestSealedClasses.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021 Peter Wall
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

package io.kjson.testclasses

import io.kjson.annotation.JSONDiscriminator
import io.kjson.annotation.JSONIdentifier

// Note - these classes are copied from the Kotlin documentation:
// https://kotlinlang.org/docs/reference/sealed-classes.html

sealed class Expr

data class Const(val number: Double) : Expr()

@Suppress("unused")
data class Sum(val e1: Expr, val e2: Expr) : Expr()

@Suppress("unused")
object NotANumber : Expr()

@JSONDiscriminator("type")
sealed class Expr2

data class Const2(val number: Double) : Expr2()

@Suppress("unused")
data class Sum2(val e1: Expr2, val e2: Expr2) : Expr2()

@Suppress("unused")
object NotANumber2 : Expr2()

@JSONDiscriminator("type")
sealed class Expr3

@JSONIdentifier("CONST")
data class Const3(val number: Double) : Expr3()

@JSONIdentifier("SUM")
@Suppress("unused")
data class Sum3(val e1: Expr3, val e2: Expr3) : Expr3()

@JSONIdentifier("NAN")
@Suppress("unused")
object NotANumber3 : Expr3()

@JSONDiscriminator("type")
sealed class Party() {
    abstract val type: String
}

@JSONIdentifier("ORGANIZATION")
data class Organization(override val type: String, val id: Int, val name: String) : Party()

@JSONIdentifier("PERSON")
data class Person(override val type: String, val firstName: String, val lastName: String) : Party()

