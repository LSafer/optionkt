package net.lsafer.optionkt

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.lsafer.optionkt.internal.LenientJsonFormat
import net.lsafer.optionkt.internal.flatten
import net.lsafer.optionkt.internal.unflatten
import org.intellij.lang.annotations.Language

@RequiresOptIn
annotation class InternalOptionktApi

@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class OptionRef(val value: String)

@OptIn(ExperimentalSerializationApi::class)
@Repeatable
@SerialInfo
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class OptionDoc(
    @Language("JavaScript", prefix = "_(", suffix = ")")
    val value: String
)

fun mergeOptionSource(vararg options: Map<String, String?>): Map<String, String> {
    return buildMap { for (m in options) for ((k, v) in m) if (v != null) put(k, v) }
}

@InternalOptionktApi
fun flattenOptionSource(options: JsonObject): Map<String, String> {
    return buildMap { flatten(options, out = this, path = "") }
}

@InternalOptionktApi
fun unflattenOptionSource(options: Map<String, String>): JsonObject {
    return unflatten(options, path = "")
}

@InternalOptionktApi
fun String.decodeJsonOptionSource(): Map<String, String> {
    val jsonObject = LenientJsonFormat.decodeFromString<JsonObject>(this)
    return flattenOptionSource(jsonObject)
}

@InternalOptionktApi
inline fun <reified T> compileOptionSource(
    vararg options: Map<String, String?>,
    format: Json = LenientJsonFormat,
): T {
    val merged = mergeOptionSource(*options)
    val jsonObject = unflattenOptionSource(merged)
    return format.decodeFromString(jsonObject.toString())
}
