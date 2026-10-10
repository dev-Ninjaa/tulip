package com.example.tulip.data.ai.ollama

import com.example.tulip.data.ai.AiProviderType
import com.example.tulip.data.ai.AiResult
import com.example.tulip.data.ai.NatureAiService
import com.example.tulip.data.ai.OllamaConnectionState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.URI
import java.util.concurrent.TimeUnit

class OllamaAiService : NatureAiService {

    override val providerType: AiProviderType = AiProviderType.OLLAMA

    var serverUrl: String = "http://192.168.1.100:11434"
    var modelId: String = "gemma3:4b"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        // Gemma 3 4B on consumer hardware may take longer for multimodal inference
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    companion object {
        fun normalizeAndValidateUrl(raw: String): Result<String> {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("Ollama server URL cannot be empty."))
            }

            val candidate = if (!trimmed.contains("://")) {
                "http://$trimmed"
            } else {
                trimmed
            }

            return try {
                val uri = URI.create(candidate)
                val scheme = uri.scheme?.lowercase()
                if (scheme != "http" && scheme != "https") {
                    return Result.failure(IllegalArgumentException("Only http:// and https:// URLs are supported."))
                }
                if (uri.host.isNullOrBlank()) {
                    return Result.failure(IllegalArgumentException("Invalid server address or host."))
                }
                val port = if (uri.port != -1) ":${uri.port}" else ""
                val normalized = "${scheme}://${uri.host}$port"
                val trailingSlashNormalized = if (normalized.endsWith("/")) normalized else "$normalized/"
                Result.success(trailingSlashNormalized)
            } catch (e: Exception) {
                Result.failure(IllegalArgumentException("Invalid URL format: ${e.message}"))
            }
        }
    }

    private fun getRetrofit(): Retrofit? {
        val validated = normalizeAndValidateUrl(serverUrl).getOrNull() ?: return null
        return Retrofit.Builder()
            .baseUrl(validated)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    suspend fun testConnection(): OllamaConnectionState = withContext(Dispatchers.IO) {
        val validatedUrlResult = normalizeAndValidateUrl(serverUrl)
        if (validatedUrlResult.isFailure) {
            return@withContext OllamaConnectionState.Error(
                validatedUrlResult.exceptionOrNull()?.message ?: "Invalid server URL."
            )
        }

        val retrofit = getRetrofit() ?: return@withContext OllamaConnectionState.Error("Failed to initialize HTTP client for $serverUrl")
        val api = retrofit.create(OllamaApiService::class.java)

        try {
            val response = api.getTags()
            if (!response.isSuccessful) {
                val code = response.code()
                return@withContext OllamaConnectionState.Error("Ollama server returned HTTP $code. Check server address and port.")
            }

            val body = response.body()
            val installedModels = body?.models?.map { it.name } ?: emptyList()
            
            // Model matching: Ollama tags may be e.g. "gemma3:4b" or "gemma3:4b-it" or "gemma3:latest"
            val target = modelId.trim()
            val matched = installedModels.any { name ->
                name.equals(target, ignoreCase = true) ||
                name.startsWith("$target:", ignoreCase = true) ||
                (target.contains(":") && name.substringBefore(":") == target.substringBefore(":"))
            }

            if (matched) {
                OllamaConnectionState.Connected(target, installedModels)
            } else {
                OllamaConnectionState.ModelMissing(target, installedModels)
            }
        } catch (e: java.net.ConnectException) {
            OllamaConnectionState.Error("Could not connect to Ollama at $serverUrl. Ensure Ollama is running and OLLAMA_HOST=0.0.0.0 is set.")
        } catch (e: java.net.SocketTimeoutException) {
            OllamaConnectionState.Error("Connection timed out reaching $serverUrl. Check your Wi-Fi network and firewall.")
        } catch (e: java.net.UnknownHostException) {
            OllamaConnectionState.Error("Host not found ($serverUrl). If using laptop, use LAN IP (e.g. 192.168.x.x) rather than localhost.")
        } catch (e: Exception) {
            OllamaConnectionState.Error("Connection test failed: ${e.localizedMessage ?: "Unknown network failure"}")
        }
    }

    override suspend fun fixAndTranslateToEnglish(rawText: String): AiResult<String> = withContext(Dispatchers.IO) {
        if (rawText.isBlank()) {
            return@withContext AiResult.Error("No text provided to fix or translate.")
        }

        val retrofit = getRetrofit()
            ?: return@withContext AiResult.Error("Ollama server URL is not properly configured. Please configure in Settings.")

        val api = retrofit.create(OllamaApiService::class.java)

        val systemPrompt = """
            You are Tulip's personal field notebook editor and multilingual translator running Gemma 3 4B.
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

        val request = OllamaChatRequest(
            model = modelId.trim(),
            messages = listOf(
                OllamaChatMessage(role = "system", content = systemPrompt),
                OllamaChatMessage(role = "user", content = rawText)
            ),
            stream = false,
            options = mapOf(
                "temperature" to 0.2,
                "top_p" to 0.95
            )
        )

        try {
            val response = api.chat(request)
            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.message?.content?.trim()
                if (!text.isNullOrEmpty()) {
                    AiResult.Success(text)
                } else {
                    AiResult.Error("No text returned by Gemma 3 model. Ensure Ollama server has sufficient memory.")
                }
            } else {
                val code = response.code()
                val err = response.errorBody()?.string() ?: ""
                if (code == 404 || err.contains("model not found", ignoreCase = true)) {
                    AiResult.Error("Model '$modelId' not found on Ollama server. Run 'ollama pull $modelId' on your host machine.")
                } else {
                    AiResult.Error("Ollama request failed (HTTP $code): ${err.take(120)}")
                }
            }
        } catch (e: java.net.ConnectException) {
            AiResult.Error("Cannot connect to local Ollama server at $serverUrl. Ensure your laptop is on the same Wi-Fi.", isNetworkOrOffline = true)
        } catch (e: java.net.SocketTimeoutException) {
            AiResult.Error("Inference timed out. Gemma 3 4B may be taking too long to generate on the host machine.", isNetworkOrOffline = true)
        } catch (e: Exception) {
            AiResult.Error("Ollama network error: ${e.localizedMessage ?: "Unknown failure"}", isNetworkOrOffline = true)
        }
    }

    override suspend fun askAiAboutImage(
        mimeType: String,
        base64Data: String,
        userQuestion: String
    ): AiResult<String> = withContext(Dispatchers.IO) {
        if (userQuestion.isBlank()) {
            return@withContext AiResult.Error("Please enter a question about this photograph.")
        }

        val retrofit = getRetrofit()
            ?: return@withContext AiResult.Error("Ollama server URL is not configured. Please configure in Settings.")

        val api = retrofit.create(OllamaApiService::class.java)

        val systemPrompt = """
            You are Tulip's expert botanical, wildlife, and natural history field assistant running Gemma 3 4B multimodal.
            You are assisting a nature explorer or camper observing their surroundings.
            Strict guidelines:
            1. Answer the user's specific question directly based on the provided photograph.
            2. Distinguish clearly between a likely identification and a verified certainty.
            3. If the image lacks sufficient detail or angle for reliable identification, state the uncertainty honestly and suggest what details to photograph next (e.g. leaf arrangement, stem cross-section, flower underside, bark texture).
            4. CRITICAL SAFETY: Never tell users that an unidentified plant, mushroom, berry, or creature is safe to eat, ingest, or handle. Always include caution for foraged items.
            5. Do not invent precise GPS coordinates or make false authoritative claims.
            6. Keep the tone calm, observant, educational, and respectful of natural ecosystems.
        """.trimIndent()

        // Clean Base64 for Ollama (remove any data:image/jpeg;base64, prefix if present)
        val cleanBase64 = if (base64Data.contains(",")) {
            base64Data.substringAfter(",")
        } else {
            base64Data
        }

        val request = OllamaChatRequest(
            model = modelId.trim(),
            messages = listOf(
                OllamaChatMessage(role = "system", content = systemPrompt),
                OllamaChatMessage(
                    role = "user",
                    content = userQuestion,
                    images = listOf(cleanBase64)
                )
            ),
            stream = false,
            options = mapOf(
                "temperature" to 0.3,
                "top_p" to 0.95
            )
        )

        try {
            val response = api.chat(request)
            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.message?.content?.trim()
                if (!text.isNullOrEmpty()) {
                    AiResult.Success(text)
                } else {
                    AiResult.Error("No response generated by Gemma 3 4B. Check photograph clarity or server resources.")
                }
            } else {
                val code = response.code()
                val err = response.errorBody()?.string() ?: ""
                if (code == 404 || err.contains("model not found", ignoreCase = true)) {
                    AiResult.Error("Model '$modelId' not found on Ollama server. Run 'ollama pull $modelId' on your host machine.")
                } else {
                    AiResult.Error("Ollama multimodal request failed (HTTP $code): ${err.take(120)}")
                }
            }
        } catch (e: java.net.ConnectException) {
            AiResult.Error("Cannot connect to local Ollama server at $serverUrl. Check connection and firewall.", isNetworkOrOffline = true)
        } catch (e: java.net.SocketTimeoutException) {
            AiResult.Error("Gemma 3 4B image analysis timed out. Vision inference can be compute-intensive on the host.", isNetworkOrOffline = true)
        } catch (e: Exception) {
            AiResult.Error("Ollama network error: ${e.localizedMessage ?: "Unknown failure"}", isNetworkOrOffline = true)
        }
    }
}
