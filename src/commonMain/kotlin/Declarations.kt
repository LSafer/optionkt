package net.lsafer.optionkt

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
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
    return options.asList().mergeOptionSource()
}

fun List<Map<String, String?>>.mergeOptionSource(): Map<String, String> {
    return buildMap { for (m in this@mergeOptionSource) for ((k, v) in m) if (v != null) put(k, v) }
}
