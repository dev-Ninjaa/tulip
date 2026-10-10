package com.example.tulip.data.ai.ollama

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OllamaApiService {
    @GET("api/tags")
    suspend fun getTags(): Response<OllamaTagsResponse>

    @POST("api/chat")
    suspend fun chat(
        @Body request: OllamaChatRequest
    ): Response<OllamaChatResponse>
}
