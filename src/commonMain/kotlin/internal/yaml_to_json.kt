package net.lsafer.optionkt.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import net.mamoe.yamlkt.*

private val NUMBER_REGEXP = Regex("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")

internal fun YamlMap.toJsonObject(): JsonObject {
    return JsonObject(entries.associate {
        it.key.toString() to it.value.toJsonElement()
    })
}

internal fun YamlList.toJsonArray(): JsonArray {
    return JsonArray(map { it.toJsonElement() })
}

internal fun YamlElement.toJsonElement(): JsonElement {
    return when (this) {
        is YamlNull -> JsonNull
        is YamlLiteral -> toJsonPrimitive()
        is YamlMap -> toJsonObject()
        is YamlList -> toJsonArray()
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal fun YamlLiteral.toJsonPrimitive(): JsonPrimitive {
    return when {
        content == "null" -> JsonNull
        content == "true" -> JsonPrimitive(true)
        content == "false" -> JsonPrimitive(false)
        content matches NUMBER_REGEXP -> JsonUnquotedLiteral(content)
        else -> JsonPrimitive(content)
    }
}
