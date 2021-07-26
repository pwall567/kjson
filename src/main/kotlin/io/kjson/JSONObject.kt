package io.kjson

import net.pwall.util.ImmutableMap

class JSONObject internal constructor(array: Array<MapEntry<String, JSONValue?>>, size: Int) : JSONValue,
        ImmutableMap<String, JSONValue?>(array, size) {

    override fun appendToJSON(a: Appendable) {
        a.append('{')
        if (isNotEmpty()) {
            val iterator = entries.iterator()
            while (true) {
                val entry = iterator.next()
                JSONString(entry.key).appendToJSON(a)
                a.append(':')
                entry.value.appendToJSON(a)
                if (!iterator.hasNext())
                    break
                a.append(',')
            }
        }
        a.append('}')
    }

}
