package net.lsafer.optionkt.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.json.*
import net.lsafer.optionkt.OptionDoc
import kotlin.enums.EnumEntries
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

@PublishedApi
internal data class SchemaResult(
    val type: JsonObjectBuilder.() -> Unit,
    val additionalTypes: Map<String, JsonObjectBuilder.() -> Unit> = emptyMap(),
    val immediateProperties: Map<String, JsonObjectBuilder.() -> Unit> = emptyMap(),
)

@PublishedApi
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

@PublishedApi
internal fun calculateSchema(kType: KType): SchemaResult {
    val kClass = kType.jvmErasure

    if (kClass == JsonObject::class) return SchemaResult({ put("type", "object") })
    if (kClass == JsonArray::class) return SchemaResult({ put("type", "array") })
    if (kClass == String::class) return SchemaResult({ put("type", "string") })
    if (kClass == Regex::class) return SchemaResult({ put("type", "string") })
    if (kClass == Boolean::class) return SchemaResult({ put("type", "boolean") })
    if (kClass.isSubclassOf(Number::class)) return SchemaResult({ put("type", "number") })

    if (kClass.isSubclassOf(Map::class)) {
        val subKType = kType.arguments[1].type!!
        val subSchema = calculateSchema(subKType)

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

    if (kClass.isSubclassOf(List::class) || kClass.isSubclassOf(Set::class)) {
        val subKType = kType.arguments[0].type!!
        val subSchema = calculateSchema(subKType)

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

    if (kClass.isSubclassOf(Enum::class)) {
        data class EnumSubInfo(val name: String)

        val subInfoList = kClass.enumEntries().map { subEnum ->
            val subSerialName = subEnum.findAnnotation<SerialName>()
            val subName = subSerialName?.value ?: subEnum.name

            EnumSubInfo(name = subName)
        }

        return SchemaResult({
            put("type", "string")
            putJsonArray("enum") {
                subInfoList.forEach { subInfo ->
                    add(subInfo.name)
                }
            }
        })
    }

    if (kClass.isValue) {
        val subKType = kClass.primaryConstructor!!.parameters[0].type
        val subSchema = calculateSchema(subKType)

        return subSchema
    }

    if (kClass.objectInstance != null) {
        val serialName = kClass.findAnnotation<SerialName>()
        val optionDocs = kClass.findAnnotations<OptionDoc>()
        val ref = kClass.identifyingName()
        val discriminator = serialName?.value

        return SchemaResult({
            put("type", "object")
            put("title", ref)
            put("additionalProperties", false)
            putAll(optionDocs)

            putJsonObject("properties") {
                if (discriminator != null) {
                    putJsonObject("type") {
                        put("const", discriminator)
                    }
                }
            }
        })
    }

    if (kClass.isSealed) {
        data class SealedSubInfo(val schema: SchemaResult)

        val optionDocs = kClass.findAnnotations<OptionDoc>()
        val ref = kClass.identifyingName()

        val subInfoList = kClass.sealedSubclasses.map { subKClass ->
            val subSchema = calculateSchema(subKClass.starProjectedType)

            SealedSubInfo(schema = subSchema)
        }

        val additionalType: JsonObjectBuilder.() -> Unit = {
            put("title", ref)
            putAll(optionDocs)

            putJsonArray("oneOf") {
                subInfoList.forEach { subInfo ->
                    addJsonObject {
                        subInfo.schema.type(this)
                    }
                }
            }
        }

        return SchemaResult(
            type = {
                put($$"$ref", $$"#/$defs/$$ref")
            },
            additionalTypes = buildMap {
                put(ref, additionalType)

                subInfoList.forEach { subInfo ->
                    putAll(subInfo.schema.additionalTypes)
                }
            }
        )
    }

    if (kClass.isData) {
        data class DataSubInfo(
            val name: String,
            val isOptional: Boolean,
            val type: JsonObjectBuilder.() -> Unit,
            val additionalTypes: Map<String, JsonObjectBuilder.() -> Unit>,
            val immediateProperties: Map<String, JsonObjectBuilder.() -> Unit>,
        )

        val serialName = kClass.findAnnotation<SerialName>()
        val optionDocs = kClass.findAnnotations<OptionDoc>()
        val discriminator = serialName?.value
        val ref = kClass.identifyingName()

        val subInfoList = kClass.primaryConstructor!!.parameters.map { subKParameter ->
            val subSerialName = subKParameter.findAnnotation<SerialName>()
            val subOptionDocs = subKParameter.findAnnotations<OptionDoc>()
            val subName = subSerialName?.value ?: subKParameter.name!!
            val subIsOptional = subKParameter.isOptional
            val subKType = subKParameter.type
            val subSchema = calculateSchema(subKType)

            DataSubInfo(
                name = subName,
                isOptional = subIsOptional,
                type = {
                    put("title", subName)
                    subSchema.type.invoke(this)
                    putAll(subOptionDocs)
                },
                additionalTypes = subSchema.additionalTypes,
                immediateProperties = subSchema.immediateProperties
                    .mapKeys { "${subName}.${it.key}" },
            )
        }

        val additionalType: JsonObjectBuilder.() -> Unit = {
            put("type", "object")
            put("title", ref)
            put("additionalProperties", false)
            putAll(optionDocs)

            putJsonObject("properties") {
                if (discriminator != null) {
                    putJsonObject("type") {
                        put("const", discriminator)
                    }
                }

                subInfoList.forEach { subInfo ->
                    putJsonObject(subInfo.name) {
                        subInfo.type(this)
                    }
                    subInfo.immediateProperties.map { (name, type) ->
                        putJsonObject(name) {
                            type()
                            put("x-is-dot-sc", true)
                        }
                    }
                }
            }
            putJsonArray("required") {
                subInfoList.forEach { subInfo ->
                    if (!subInfo.isOptional)
                        add(subInfo.name)
                }
            }
        }

        return SchemaResult(
            type = {
                put($$"$ref", $$"#/$defs/$$ref")
            },
            additionalTypes = buildMap {
                put(ref, additionalType)
                subInfoList.forEach { subInfo ->
                    putAll(subInfo.additionalTypes)
                }
            },
            immediateProperties = buildMap {
                subInfoList.forEach { subInfo ->
                    put(subInfo.name, subInfo.type)
                    putAll(subInfo.immediateProperties)
                }
            }
        )
    }

    error("Couldn't build schema for type: $kType")
}

private fun KClass<*>.identifyingName(): String {
    return jvmName.substringAfterLast('.').replace('$', '.')
}

private fun KClass<*>.enumEntries(): EnumEntries<*> {
    require(isSubclassOf(Enum::class)) { "Class is not an enum class: $this" }
    return staticProperties.single { it.name == "entries" }
        .javaGetter!!.invoke(null) as EnumEntries<*>
}

private inline fun <reified T : Annotation> Enum<*>.findAnnotation(): T? {
    return this.declaringJavaClass.getField(this.name).getAnnotation(T::class.java)
}

private fun JsonObjectBuilder.putAll(docs: List<OptionDoc>) {
    docs.forEach { doc ->
        LenientJsonFormat
            .decodeFromString<JsonObject>(doc.value)
            .forEach { put(it.key, it.value) }
    }
}
