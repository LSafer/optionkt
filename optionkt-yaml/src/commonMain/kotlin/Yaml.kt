package net.lsafer.optionkt

import net.lsafer.optionkt.internal.flatten
import net.lsafer.optionkt.internal.toJsonObject
import net.mamoe.yamlkt.Yaml
import net.mamoe.yamlkt.YamlMap
import kotlin.jvm.JvmName

@OptIn(InternalOptionktApi::class)
@JvmName("flattenOptionSource_old")
@Deprecated("Use the extension function instead.", ReplaceWith("options.flattenOptionSource()"))
fun flattenOptionSource(options: YamlMap) = options.flattenOptionSource()

@OptIn(InternalOptionktApi::class)
fun YamlMap.flattenOptionSource(): Map<String, String> {
    return buildMap { flatten(this@flattenOptionSource.toJsonObject(), out = this, path = "") }
}

fun String.decodeYamlOptionSource(): Map<String, String> {
    val yamlMap = Yaml.decodeYamlMapFromString(this)
    return yamlMap.flattenOptionSource()
}
