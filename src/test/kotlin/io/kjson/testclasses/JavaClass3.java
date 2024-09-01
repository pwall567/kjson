/*
 * @(#) JavaClass3.java
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

public class JavaClass3 extends JavaClass1 {

    private boolean flag;

    public JavaClass3(int field1, String field2, boolean flag) {
        super(field1, field2);
        this.flag = flag;
    }

    @SuppressWarnings("unused")
    public boolean isFlag() {
        return flag;
    }

    @SuppressWarnings("unused")
    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof JavaClass3))
            return false;
        JavaClass3 obj3 = (JavaClass3)obj;
        return super.equals(obj3) && flag == obj3.flag;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ (flag ? 1 : 0);
    }

}
