package com.cms.vacations

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule

class JsonSerializerFactory {

    val objectMapper = ObjectMapper()

    companion object {

        @JvmStatic
        fun jsonSerializer(): JsonSerializerFactory {
            val jsonSerializerFactory = JsonSerializerFactory()

            jsonSerializerFactory.objectMapper
                .registerModule(Jdk8Module())
                .registerModule(JavaTimeModule())
                .registerModule(kotlinModule())
                .setSerializationInclusion(JsonInclude.Include.ALWAYS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true)
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true)
                .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true)
                .setVisibility(
                    VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                )

            return jsonSerializerFactory
        }

        @JvmStatic
        fun jsonSerializerForApi(): JsonSerializerFactory {
            return jsonSerializer()
        }
    }

    fun withCustomModule(module: SimpleModule): JsonSerializerFactory {
        objectMapper.registerModule(module)
        return this
    }

    fun doNotFailOnUnknownProperties(): JsonSerializerFactory {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return this
    }

    fun ignoreAbsent(): JsonSerializerFactory {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
        return this
    }
}
