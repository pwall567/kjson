/*
 * @(#) JavaMultiConstructor.kt
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

package io.kjson.testclasses;

import java.beans.ConstructorProperties;

public class JavaMultiConstructor {

    private final String type;
    private final String name;
    private final int number;

    @SuppressWarnings("unused")
    @ConstructorProperties({"name", "number"})
    public JavaMultiConstructor(String name, int number) {
        type = "A";
        this.name = name;
        this.number = number;
    }

    @SuppressWarnings("unused")
    public JavaMultiConstructor(String name) {
        type = "B";
        this.name = name;
        number = 8;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }

}
