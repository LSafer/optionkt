package net.lsafer.optionkt

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.lsafer.optionkt.internal.LenientJsonFormat
import net.lsafer.optionkt.internal.flatten
import net.lsafer.optionkt.internal.unflatten
import kotlin.jvm.JvmName

@OptIn(InternalOptionktApi::class)
@JvmName("flattenOptionSource_old")
@Deprecated("Use the extension function instead.", ReplaceWith("options.flattenOptionSource()"))
fun flattenOptionSource(options: JsonObject) = options.flattenOptionSource()

@OptIn(InternalOptionktApi::class)
fun JsonObject.flattenOptionSource(): Map<String, String> {
    return buildMap { flatten(this@flattenOptionSource, out = this, path = "") }
}

@OptIn(InternalOptionktApi::class)
@JvmName("unflattenOptionSource_old")
@Deprecated("Use the extension function instead.", ReplaceWith("options.unflattenOptionSource()"))
fun unflattenOptionSource(options: Map<String, String>) = options.unflattenOptionSource()

@OptIn(InternalOptionktApi::class)
fun Map<String, String>.unflattenOptionSource(): JsonObject {
    return unflatten(this, path = "")
}

@OptIn(InternalOptionktApi::class)
fun String.decodeJsonOptionSource(): Map<String, String> {
    val jsonObject = LenientJsonFormat.decodeFromString<JsonObject>(this)
    return jsonObject.flattenOptionSource()
}

@OptIn(InternalOptionktApi::class)
inline fun <reified T> compileOptionSource(
    vararg options: Map<String, String?>,
    format: Json = LenientJsonFormat,
): T {
    return options.asList().compileOptionSource(format)
}

@OptIn(InternalOptionktApi::class)
inline fun <reified T> List<Map<String, String?>>.compileOptionSource(
    format: Json = LenientJsonFormat
): T {
    val jsonObject = mergeOptionSource().unflattenOptionSource()
    return format.decodeFromString(jsonObject.toString())
}

@OptIn(InternalOptionktApi::class)
inline fun <reified T> Map<String, String?>.compileOptionSource(
    format: Json = LenientJsonFormat
): T {
    val jsonObject = mergeOptionSource(this).unflattenOptionSource()
    return format.decodeFromString(jsonObject.toString())
}
