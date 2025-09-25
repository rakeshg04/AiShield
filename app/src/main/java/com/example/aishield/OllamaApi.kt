package com.example.aishield

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class OllamaRequest(val model: String, val prompt: String)
data class OllamaResponse(val response: String)

interface OllamaApi {
    @Headers("Content-Type: application/json")
    @POST("api/generate")
    fun generate(@Body request: OllamaRequest): Call<OllamaResponse>
}
