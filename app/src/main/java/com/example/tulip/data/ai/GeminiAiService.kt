package com.example.tulip.data.ai

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed class AiResult<out T> {
    data class Success<T>(val data: T) : AiResult<T>()
    data class Error(val message: String, val isNetworkOrOffline: Boolean = false, val isQuota: Boolean = false) : AiResult<Nothing>()
}

class GeminiAiService : NatureAiService {

    override val providerType: AiProviderType = AiProviderType.GEMINI

    // Configurable model identifier with validated default
    // As per prompt & Gemini API skill: supports gemini-3.5-flash / gemini-flash-latest
    var activeModel: String = "gemini-3.5-flash"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Fix & Translate to English:
     * - Case A: English -> Correct grammar, spelling, punctuation, capitalization, transcription artifacts.
     *   Preserve personal voice, emotional nuance, tone, and level of detail. Do not invent facts or corporate prose.
     * - Case B: Other languages -> Identify language, translate naturally to English while correcting grammar.
     * - Case C: Mixed language -> Translate non-English while preserving proper names, cultural phrases, and local terminology.
     */
    override suspend fun fixAndTranslateToEnglish(rawText: String): AiResult<String> = withContext(Dispatchers.IO) {
        if (rawText.isBlank()) {
            return@withContext AiResult.Error("No text provided to fix or translate.")
        }

        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AiResult.Error(
                "Gemini API key is not configured. Please add your GEMINI_API_KEY to AI Studio Secrets or .env file."
            )
        }

        val systemPrompt = """
            You are Tulip's personal field notebook editor and multilingual translator.
            Task: Fix grammar/spelling or translate natural observations into natural, flowing English.
            Rules:
            1. If the input is in English:
               - Fix spelling, punctuation, capitalization, and minor grammatical or transcription artifacts.
               - Keep the exact personal voice, tone, and details.
               - Do NOT make it corporate, overly academic, or artificially florid.
               - If it is already correct, return it unchanged.
            2. If the input is in another language:
               - Translate it into natural, idiomatic English while correcting any grammatical issues.
            3. If the input is mixed language:
               - Translate non-English parts naturally into English, but preserve local plant/species names, place names, and culturally specific expressions.
            4. Absolute fidelity: Never invent facts, events, emotions, locations, or extra observations that the user did not write.
            5. Return ONLY the final corrected/translated English text. No introductory remarks, conversational greetings, quotes, or markdown explanations.
        """.trimIndent()

        val request = GeminiRequest(
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = rawText))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.2f, // low temperature for high fidelity
                topP = 0.95f,
                maxOutputTokens = 1024
            )
        )

        executeRequest(request)
    }

    /**
     * Ask AI:
     * Multimodal question answering on natural observations.
     * Provides honest, uncertainty-aware identification. Never claims wild items are safe to eat or handle.
     */
    override suspend fun askAiAboutImage(
        mimeType: String,
        base64Data: String,
        userQuestion: String
    ): AiResult<String> = withContext(Dispatchers.IO) {
        if (userQuestion.isBlank()) {
            return@withContext AiResult.Error("Please enter a question about this photograph.")
        }

        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AiResult.Error(
                "Gemini API key is not configured. Please add your GEMINI_API_KEY to AI Studio Secrets or .env file."
            )
        }

        val systemPrompt = """
            You are Tulip's expert botanical, wildlife, and natural history field assistant.
            You are assisting a nature explorer or camper observing their surroundings.
            Strict guidelines:
            1. Answer the user's specific question directly based on the provided photograph.
            2. Distinguish clearly between a likely identification and a verified certainty.
            3. If the image lacks sufficient detail or angle for reliable identification, state the uncertainty honestly and suggest what details to photograph next (e.g. leaf arrangement, stem cross-section, flower underside, bark texture).
            4. CRITICAL SAFETY: Never tell users that an unidentified plant, mushroom, berry, or creature is safe to eat, ingest, or handle. Always include caution for foraged items.
            5. Do not invent precise GPS coordinates or make false authoritative claims.
            6. Keep the tone calm, observant, educational, and respectful of natural ecosystems.
        """.trimIndent()

        val request = GeminiRequest(
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = base64Data)),
                        GeminiPart(text = userQuestion)
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.3f,
                topP = 0.95f,
                maxOutputTokens = 1024
            )
        )

        executeRequest(request)
    }

    private suspend fun executeRequest(request: GeminiRequest): AiResult<String> {
        val apiKey = getApiKey()
        return try {
            val response = apiService.generateContent(activeModel, apiKey, request)
            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!text.isNullOrEmpty()) {
                    AiResult.Success(text)
                } else {
                    AiResult.Error("No response generated by the model. Check input text or photograph clarity.")
                }
            } else {
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                when (errorCode) {
                    429 -> AiResult.Error("Rate limit reached. Please wait a moment before trying again.", isQuota = true)
                    403 -> AiResult.Error("API key unauthorized or quota exhausted. Please check your Gemini API key in settings or secrets.", isQuota = true)
                    404 -> AiResult.Error("The configured model ($activeModel) was not found or is unavailable. Please verify model ID in Settings.")
                    500, 503 -> AiResult.Error("Gemini service is temporarily unavailable. Please retry in a few moments.")
                    else -> AiResult.Error("AI request failed (HTTP $errorCode): ${errorBody.take(120)}")
                }
            }
        } catch (e: java.net.UnknownHostException) {
            AiResult.Error("No internet connection. Tulip journal entries are safely saved locally on device.", isNetworkOrOffline = true)
        } catch (e: java.net.SocketTimeoutException) {
            AiResult.Error("The request timed out. Please check your network connection and retry.", isNetworkOrOffline = true)
        } catch (e: Exception) {
            AiResult.Error("Network error: ${e.localizedMessage ?: "Unknown failure"}", isNetworkOrOffline = true)
        }
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }
}
