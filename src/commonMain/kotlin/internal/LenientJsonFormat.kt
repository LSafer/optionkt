package net.lsafer.optionkt.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import net.lsafer.optionkt.InternalOptionktApi

@OptIn(ExperimentalSerializationApi::class)
@InternalOptionktApi
val LenientJsonFormat = Json {
    isLenient = true
    allowSpecialFloatingPointValues = true
    allowTrailingComma = true
    coerceInputValues = true
    ignoreUnknownKeys = true
    decodeEnumsCaseInsensitive = true
}
