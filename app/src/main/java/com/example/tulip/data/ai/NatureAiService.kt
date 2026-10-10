package com.example.tulip.data.ai

enum class AiProviderType(val displayName: String) {
    GEMINI("Gemini (Cloud)"),
    OLLAMA("Local Ollama (Gemma 3 4B)")
}

sealed class OllamaConnectionState {
    data object Idle : OllamaConnectionState()
    data object Testing : OllamaConnectionState()
    data class Connected(val modelName: String, val allModels: List<String>) : OllamaConnectionState()
    data class ModelMissing(val requestedModel: String, val installedModels: List<String>) : OllamaConnectionState()
    data class Error(val message: String) : OllamaConnectionState()
}

interface NatureAiService {
    val providerType: AiProviderType

    suspend fun fixAndTranslateToEnglish(rawText: String): AiResult<String>

    suspend fun askAiAboutImage(
        mimeType: String,
        base64Data: String,
        userQuestion: String
    ): AiResult<String>
}
