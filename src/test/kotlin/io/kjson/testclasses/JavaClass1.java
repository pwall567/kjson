/*
 * @(#) JavaClass1.java
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

package io.kjson.testclasses;

public class JavaClass1 {

    @SuppressWarnings("unused")
    public static final String description = "Test java class";

    private int field1;
    private String field2;

    public JavaClass1(int field1, String field2) {
        this.field1 = field1;
        this.field2 = field2;
    }

    /**
     * No argument constructor required for deserialization of Java classes.
     */
    @SuppressWarnings("unused")
    public JavaClass1() {
        this(0, null);
    }

    @SuppressWarnings("unused")
    public int getField1() {
        return field1;
    }

    @SuppressWarnings("unused")
    public void setField1(int field1) {
        this.field1 = field1;
    }

    @SuppressWarnings("unused")
    public String getField2() {
        return field2;
    }

    @SuppressWarnings("unused")
    public void setField2(String field2) {
        this.field2 = field2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof JavaClass1))
            return false;
        JavaClass1 obj1 = (JavaClass1)obj;
        return field1 == obj1.field1 && field2.equals(obj1.field2);
    }

    @Override
    public int hashCode() {
        return field1 ^ field2.hashCode();
    }

}
