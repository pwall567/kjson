package io.kjson

import net.pwall.util.ImmutableList

class JSONArray internal constructor(array: Array<JSONValue?>, size: Int) : JSONValue,
        ImmutableList<JSONValue?>(array, size) {

    override fun appendToJSON(a: Appendable) {
        a.append('[')
        if (isNotEmpty()) {
            val iterator = iterator()
            while (true) {
                iterator.next().appendToJSON(a)
                if (!iterator.hasNext())
                    break
                a.append(',')
            }
        }
        a.append(']')
    }

}
