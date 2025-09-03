package net.lsafer.optionkt.internal

import kotlinx.serialization.json.*
import net.lsafer.optionkt.InternalOptionktApi

@InternalOptionktApi
fun flatten(map: JsonObject, out: MutableMap<String, String>, path: String) {
    val prefix = if (path.isEmpty()) "" else "$path."
    for ((name, value) in map) when (value) {
        is JsonObject -> flatten(value, out, "$prefix$name")
        is JsonPrimitive -> out["$prefix$name"] = Json.encodeToString(value)
        is JsonArray -> out["$prefix$name"] = Json.encodeToString(value)
    }
}

@InternalOptionktApi
fun unflatten(map: Map<String, String>, path: String): JsonObject {
    val prefix = if (path.isEmpty()) "" else "$path."
    val names = map.entries.asSequence()
        .filter { it.key.startsWith(prefix) }
        .groupBy {
            val i = it.key.indexOf('.', prefix.length)
            if (i == -1) it.key.substring(prefix.length)
            else it.key.substring(prefix.length, i)
        }

    return buildJsonObject {
        for ((name, items) in names) {
            val (lastPath, last) = items.last()
            val lastRelativeDotPeriodIndex = lastPath.indexOf('.', prefix.length)

            if (lastRelativeDotPeriodIndex == -1) {
                put(name, LenientJsonFormat.parseToJsonElement(last))
            } else {
                put(name, unflatten(map, "$prefix$name"))
            }
        }
    }
}
