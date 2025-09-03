package net.lsafer.optionkt.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import net.lsafer.optionkt.InternalOptionktApi
import net.lsafer.optionkt.OptionDoc
import net.lsafer.optionkt.OptionRef
import java.security.MessageDigest
import java.util.*

internal data class SchemaResult(
    val type: JsonObjectBuilder.() -> Unit,
    val serialName: String? = null,
    val additionalTypes: Map<String, JsonObjectBuilder.() -> Unit> = emptyMap(),
    val immediateProperties: Map<String, JsonObjectBuilder.() -> Unit> = emptyMap(),
)

internal fun SchemaResult.toSchemaObject(): JsonObject {
    return buildJsonObject {
        type.invoke(this)

        putJsonObject($$"$defs") {
            additionalTypes.forEach {
                putJsonObject(it.key) {
                    it.value.invoke(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal fun calculateSchema(descriptor: SerialDescriptor): SchemaResult {
    if (descriptor.isNullable) {
        val subDescriptor = descriptor.nonNullOriginal
        val subSchema = calculateSchema(subDescriptor)

        return SchemaResult(
            type = {
                putJsonArray("oneOf") {
                    addJsonObject {
                        subSchema.type(this)
                    }
                    addJsonObject {
                        put("type", "null")
                    }
                }
            },
            additionalTypes = subSchema.additionalTypes,
            immediateProperties = subSchema.immediateProperties,
        )
    }

    if (descriptor.serialName == "kotlinx.serialization.json.JsonObject") return SchemaResult({ put("type", "object") })
    if (descriptor.serialName == "kotlinx.serialization.json.JsonArray") return SchemaResult({ put("type", "array") })
    if (descriptor.kind == PrimitiveKind.BOOLEAN) return SchemaResult({ put("type", "boolean") })
    if (descriptor.kind == PrimitiveKind.CHAR) return SchemaResult({ put("type", "string") })
    if (descriptor.kind == PrimitiveKind.STRING) return SchemaResult({ put("type", "string") })
    if (descriptor.kind == PrimitiveKind.BYTE) return SchemaResult({ put("type", "number") })
    if (descriptor.kind == PrimitiveKind.SHORT) return SchemaResult({ put("type", "number") })
    if (descriptor.kind == PrimitiveKind.INT) return SchemaResult({ put("type", "number") })
    if (descriptor.kind == PrimitiveKind.LONG) return SchemaResult({ put("type", "number") })
    if (descriptor.kind == PrimitiveKind.FLOAT) return SchemaResult({ put("type", "number") })
    if (descriptor.kind == PrimitiveKind.DOUBLE) return SchemaResult({ put("type", "number") })

    if (descriptor.kind == StructureKind.MAP) {
        val subDescriptor = descriptor.getElementDescriptor(1)
        val subSchema = calculateSchema(subDescriptor)

        return SchemaResult(
            type = {
                put("type", "object")
                putJsonObject("additionalProperties") {
                    subSchema.type(this)
                }
            },
            additionalTypes = subSchema.additionalTypes,
        )
    }

    if (descriptor.kind == StructureKind.LIST) {
        val subDescriptor = descriptor.getElementDescriptor(0)
        val subSchema = calculateSchema(subDescriptor)

        return SchemaResult(
            type = {
                put("type", "array")
                putJsonObject("items") {
                    subSchema.type(this)
                }
            },
            additionalTypes = subSchema.additionalTypes,
        )
    }

    if (descriptor.kind == SerialKind.ENUM) {
        data class EnumSubInfo(val serialName: String)

        val optionRef = descriptor.obtainOptionRef()

        val simpleClassName by lazy { descriptor.simpleClassNameOrNull() }
        val md5Id by lazy { descriptor.md5Id() }

        val serialName = descriptor.serialName
        val defRef = optionRef?.value ?: simpleClassName ?: md5Id

        val subInfoList = List(descriptor.elementsCount) { i ->
            val subSerialName = descriptor.getElementName(i)

            EnumSubInfo(serialName = subSerialName)
        }

        val additionalType: JsonObjectBuilder.() -> Unit = {
            put("type", "string")
            putJsonArray("enum") {
                subInfoList.forEach { subInfo ->
                    add(subInfo.serialName)
                }
            }
        }

        return SchemaResult(
            type = { put($$"$ref", $$"#/$defs/$$defRef") },
            serialName = serialName,
            additionalTypes = buildMap {
                put(defRef, additionalType)
            },
        )
    }

    if (descriptor.kind == StructureKind.OBJECT) {
        val optionRef = descriptor.obtainOptionRef()
        val optionDocs = descriptor.obtainOptionDocList()

        val simpleClassName by lazy { descriptor.simpleClassNameOrNull() }
        val md5Id by lazy { descriptor.md5Id() }

        val serialName = descriptor.serialName
        val title = optionRef?.value ?: simpleClassName ?: serialName
        val defRef = optionRef?.value ?: simpleClassName ?: md5Id

        val additionalType: JsonObjectBuilder.() -> Unit = {
            put("type", "object")
            put("title", title)
            put("additionalProperties", false)
            putAll(optionDocs)
        }

        return SchemaResult(
            type = { put($$"$ref", $$"#/$defs/$$defRef") },
            serialName = serialName,
            additionalTypes = buildMap {
                put(defRef, additionalType)
            },
        )
    }

    if (descriptor.kind == PolymorphicKind.SEALED) {
        data class SealedSubInfo(val schema: SchemaResult)

        val optionRef = descriptor.obtainOptionRef()
        val optionDocs = descriptor.obtainOptionDocList()
        val jsonClassDiscriminator = descriptor.obtainJsonClassDiscriminator()

        val simpleClassName by lazy { descriptor.simpleClassNameOrNull() }
        val md5Id by lazy { descriptor.md5Id() }

        val serialName = descriptor.serialName
        val title = optionRef?.value ?: simpleClassName ?: serialName
        val defRef = optionRef?.value ?: simpleClassName ?: md5Id
        val discriminator = jsonClassDiscriminator?.discriminator ?: "type"

        val subInfoList = descriptor
            .getElementDescriptor(1)
            .elementDescriptors
            .map { subDescriptor ->
                val subSchema = calculateSchema(subDescriptor)

                SealedSubInfo(schema = subSchema)
            }

        val additionalType: JsonObjectBuilder.() -> Unit = {
            put("title", title)
            putAll(optionDocs)

            putJsonArray("oneOf") {
                subInfoList.forEach { subInfo ->
                    addJsonObject {
                        if (subInfo.schema.serialName == null) {
                            subInfo.schema.type(this)
                        } else {
                            putJsonArray("allOf") {
                                addJsonObject {
                                    subInfo.schema.type(this)
                                }
                                addJsonObject {
                                    putJsonArray("required") {
                                        add(discriminator)
                                    }
                                    putJsonObject("properties") {
                                        putJsonObject(discriminator) {
                                            put("const", subInfo.schema.serialName)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return SchemaResult(
            type = { put($$"$ref", $$"#/$defs/$$defRef") },
            serialName = serialName,
            additionalTypes = buildMap {
                put(defRef, additionalType)
                subInfoList.forEach { subInfo ->
                    putAll(subInfo.schema.additionalTypes)
                }
            }
        )
    }

    if (descriptor.kind == StructureKind.CLASS && descriptor.isInline) {
        val optionRef = descriptor.obtainOptionRef()
        val optionDocs = descriptor.obtainOptionDocList()

        val simpleClassName by lazy { descriptor.simpleClassNameOrNull() }
        val md5Id by lazy { descriptor.md5Id() }

        val serialName = descriptor.serialName
        val title = optionRef?.value ?: simpleClassName ?: serialName
        val defRef = optionRef?.value ?: simpleClassName ?: md5Id

        val subDescriptor = descriptor.getElementDescriptor(0)
        val subSchema = calculateSchema(subDescriptor)

        val additionalType: JsonObjectBuilder.() -> Unit = {
            subSchema.type(this)
            put("title", title)
            putAll(optionDocs)
        }

        return SchemaResult(
            type = { put($$"$ref", $$"#/$defs/$$defRef") },
            serialName = serialName,
            additionalTypes = buildMap {
                put(defRef, additionalType)
                putAll(subSchema.additionalTypes)
            },
            immediateProperties = subSchema.immediateProperties,
        )
    }

    if (descriptor.kind == StructureKind.CLASS) {
        data class DataSubInfo(
            val serialName: String,
            val isOptional: Boolean,
            val type: JsonObjectBuilder.() -> Unit,
            val additionalTypes: Map<String, JsonObjectBuilder.() -> Unit>,
            val immediateProperties: Map<String, JsonObjectBuilder.() -> Unit>,
        )

        val optionRef = descriptor.obtainOptionRef()
        val optionDocs = descriptor.obtainOptionDocList()

        val simpleClassName by lazy { descriptor.simpleClassNameOrNull() }
        val md5Id by lazy { descriptor.md5Id() }

        val serialName = descriptor.serialName
        val title = optionRef?.value ?: simpleClassName ?: serialName
        val defRef = optionRef?.value ?: simpleClassName ?: md5Id

        val subInfoList = List(descriptor.elementsCount) { i ->
            val subOptionDocs = descriptor.obtainElementOptionDocList(i)

            val subSerialName = descriptor.getElementName(i)
            val subDescriptor = descriptor.getElementDescriptor(i)
            val subIsOptional = descriptor.isElementOptional(i)
            val subSchema = calculateSchema(subDescriptor)

            DataSubInfo(
                serialName = subSerialName,
                isOptional = subIsOptional,
                type = {
                    subSchema.type.invoke(this)
                    putAll(subOptionDocs)
                },
                additionalTypes = subSchema.additionalTypes,
                immediateProperties = subSchema.immediateProperties
                    .mapKeys { "${subSerialName}.${it.key}" },
            )
        }

        val additionalType: JsonObjectBuilder.() -> Unit = {
            put("type", "object")
            put("title", title)
            put("additionalProperties", false)
            putAll(optionDocs)

            putJsonObject("properties") {
                subInfoList.forEach { subInfo ->
                    putJsonObject(subInfo.serialName) {
                        subInfo.type(this)
                    }
                    subInfo.immediateProperties.map { (subSubSerialName, subSubType) ->
                        putJsonObject(subSubSerialName) {
                            subSubType()
                            put("x-is-dot-sc", true)
                        }
                    }
                }
            }
            putJsonArray("required") {
                subInfoList.forEach { subInfo ->
                    if (!subInfo.isOptional)
                        add(subInfo.serialName)
                }
            }
        }

        return SchemaResult(
            type = { put($$"$ref", $$"#/$defs/$$defRef") },
            serialName = serialName,
            additionalTypes = buildMap {
                put(defRef, additionalType)
                subInfoList.forEach { subInfo ->
                    putAll(subInfo.additionalTypes)
                }
            },
            immediateProperties = buildMap {
                subInfoList.forEach { subInfo ->
                    put(subInfo.serialName, subInfo.type)
                    putAll(subInfo.immediateProperties)
                }
            }
        )
    }

    // Unhandled: PolymorphicKind.OPEN, SerialKind.CONTEXTUAL
    error("Couldn't build schema for type: ${descriptor.serialName}")
}

@OptIn(InternalOptionktApi::class)
private fun JsonObjectBuilder.putAll(docs: List<OptionDoc>) {
    docs.forEach { doc ->
        LenientJsonFormat
            .decodeFromString<JsonObject>(doc.value)
            .forEach { put(it.key, it.value) }
    }
}

private fun SerialDescriptor.obtainOptionRef() = annotations.find { it is OptionRef } as OptionRef?
private fun SerialDescriptor.obtainOptionDocList() = annotations.filterIsInstance<OptionDoc>()
private fun SerialDescriptor.obtainElementOptionDocList(i: Int) = getElementAnnotations(i).filterIsInstance<OptionDoc>()

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.obtainJsonClassDiscriminator() =
    annotations.find { it is JsonClassDiscriminator } as JsonClassDiscriminator?

private fun SerialDescriptor.simpleClassNameOrNull(): String? {
    try {
        Class.forName(serialName)
        return serialName.substringAfterLast('.')
    } catch (_: ClassNotFoundException) {
        val builder = StringBuilder(serialName)

        (serialName.lastIndex downTo 1).forEach { i ->
            if (builder[i] == '.') {
                builder[i] = '$'

                try {
                    Class.forName(builder.toString())
                    ((i - 1) downTo 1).forEach { j ->
                        if (serialName[j] == '.')
                            return serialName.substring(j + 1)
                    }
                    return serialName
                } catch (_: ClassNotFoundException) {
                }
            }
        }
    }
    return null
}

private fun SerialDescriptor.md5Id(): String {
    val bytes = MessageDigest.getInstance("MD5").digest("${hashCode()}${toString()}".toByteArray())
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
