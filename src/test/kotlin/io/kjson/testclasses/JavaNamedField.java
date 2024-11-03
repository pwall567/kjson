/*
 * @(#) JavaNamedField.kt
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

import io.kjson.annotation.JSONIgnore;
import io.kjson.annotation.JSONName;

public class JavaNamedField {

    private String field1;
    private int field2;

    public String getField1() {
        return field1;
    }

    public int getField2() {
        return field2;
    }

    @JSONName(name = "alpha")
    public void setField1(String field1) {
        this.field1 = field1;
    }

    @JSONIgnore
    public void setField2(int field2) {
        this.field2 = field2;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof JavaNamedField))
            return false;
        JavaNamedField otherJNF = (JavaNamedField)obj;
        return field1.equals(otherJNF.field1) && field2 == otherJNF.field2;
    }

    @Override
    public int hashCode() {
        return field1.hashCode() + field2;
    }

}
