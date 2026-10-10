package com.example.tulip.data.ai.ollama

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaChatMessage>,
    val stream: Boolean = false,
    val options: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class OllamaChatMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null // Base64 encoded images without data URI prefix
)

@JsonClass(generateAdapter = true)
data class OllamaChatResponse(
    val model: String? = null,
    val message: OllamaChatMessage? = null,
    val done: Boolean = true,
    @field:Json(name = "total_duration") val totalDuration: Long? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class OllamaTagsResponse(
    val models: List<OllamaModelInfo>? = null
)

@JsonClass(generateAdapter = true)
data class OllamaModelInfo(
    val name: String,
    val modified_at: String? = null,
    val size: Long? = null
)
