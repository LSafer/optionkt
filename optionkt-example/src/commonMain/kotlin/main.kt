package com.example.optionkt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import net.lsafer.optionkt.*
import net.lsafer.optionkt.internal.LenientJsonFormat

@Serializable
data class MyOptions(
    @OptionDoc("""{description: "Set to true to enable xyz", default: false}""")
    val enable_xyz: Boolean = false,
    val nested: MyNestedOptions = MyNestedOptions.OptionB,
    val otherNested: MyOtherNestedOptions = MyOtherNestedOptions(),
)

@Serializable
sealed interface MyNestedOptions {
    @Serializable
    @SerialName("option-a")
    data class OptionA(
        val amount: Int,
    ) : MyNestedOptions

    @Serializable
    @SerialName("option-b")
    data object OptionB : MyNestedOptions
}

@Serializable
data class MyOtherNestedOptions(
    val ignore_abc: Boolean = true,
)

@OptIn(InternalOptionktApi::class)
fun main() {
    val Json = LenientJsonFormat

    // generate schema
    val optionsSchema = createOptionSchemaObject<MyOptions>()

    // collect raw sources
    val src0 = "{opt: 1}".decodeJsonOptionSource()
    val src1 = "opt: 1".decodeYamlOptionSource()
    val obj2 = buildJsonObject { put("opt", 1) }
    val src2 = flattenOptionSource(obj2)
    val src3 = mutableMapOf<String, String?>("opt" to "1")

    // compile manual
    run {
        val mergedSrc: Map<String, String> = mergeOptionSource(src0, src1, src2, src3)
        val resultObj: JsonObject = unflattenOptionSource(mergedSrc)
        val options = Json.decodeFromJsonElement<MyOptions>(resultObj)
    }

    // compile shortcut
    run {
        val options = compileOptionSource<MyOptions>(src0, src1, src2, src3, format = Json)
    }
}
