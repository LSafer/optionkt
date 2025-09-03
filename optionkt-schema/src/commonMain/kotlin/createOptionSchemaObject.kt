package net.lsafer.optionkt

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.json.JsonObject
import net.lsafer.optionkt.internal.calculateSchema
import net.lsafer.optionkt.internal.toSchemaObject

inline fun <reified T> createOptionSchemaObject(): JsonObject {
    return createOptionSchemaObject(serialDescriptor<T>())
}

fun createOptionSchemaObject(descriptor: SerialDescriptor): JsonObject {
    return calculateSchema(descriptor).toSchemaObject()
}
