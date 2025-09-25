package com.example.aishield

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object HuggingFaceClient {
    // If you test direct HF put token here (for testing only).
    private const val API_KEY = "hf_your_api_key_here" // or keep empty when using proxy
    // If you use direct HF:
    private const val HF_URL = "https://api-inference.huggingface.co/models/google/gemma-2b-it"
    // If you use local proxy, set PROXY_URL instead, e.g. "http://10.0.2.2:3000/chat"
    private const val PROXY_URL = "" // set to "" to call HF_URL directly

    private val client = OkHttpClient()

    fun sendMessage(message: String, callback: (String) -> Unit) {
        val url = if (PROXY_URL.isNullOrBlank()) HF_URL else PROXY_URL
        val json = JSONObject().put("inputs", message)
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val builder = Request.Builder()
            .url(url)
            .post(requestBody)

        if (PROXY_URL.isNullOrBlank()) {
            // direct call to HF
            builder.header("Authorization", "Bearer $API_KEY")
        }

        val request = builder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HuggingFaceClient", "network failure: ${e.message}")
                callback("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val contentType = response.header("Content-Type") ?: ""
                val body = response.body?.string() ?: ""

                Log.d("HuggingFaceClient", "HTTP $code Content-Type:$contentType")
                Log.d("HuggingFaceClient", "Body (first 2000 chars): ${body.take(2000)}")

                // Non-200
                if (code != 200) {
                    // Try JSON { "error": "..." }
                    try {
                        val obj = JSONObject(body)
                        val err = when {
                            obj.has("error") -> obj.getString("error")
                            obj.has("message") -> obj.getString("message")
                            else -> obj.toString()
                        }
                        callback("API Error (status $code): $err")
                        return
                    } catch (_: Exception) { /* not JSON */ }

                    // HTML or unknown text returned
                    if (contentType.contains("text/html") || body.trimStart().startsWith("<")) {
                        callback("API returned HTML (status $code). Check token/model; see logs for full body.")
                    } else {
                        callback("API returned status $code. Body: ${body.take(2000)}")
                    }
                    return
                }

                // 200 OK — expect JSON array OR object
                try {
                    if (contentType.contains("application/json") || body.trimStart().startsWith("{") || body.trimStart().startsWith("[")) {
                        // Try array with generated_text
                        try {
                            val arr = JSONArray(body)
                            val first = arr.getJSONObject(0)
                            val text = when {
                                first.has("generated_text") -> first.getString("generated_text")
                                first.has("translation_text") -> first.getString("translation_text")
                                else -> first.toString()
                            }
                            callback(text)
                            return
                        } catch (_: Exception) {
                            // Try object with error or message
                            val obj = JSONObject(body)
                            if (obj.has("error")) {
                                callback("API Error: ${obj.getString("error")}")
                                return
                            }
                            // fallback
                            callback(obj.toString())
                            return
                        }
                    } else if (body.trimStart().startsWith("<")) {
                        callback("Invalid response: HTML returned (see logs).")
                    } else {
                        callback("Unexpected response: ${body.take(2000)}")
                    }
                } catch (e: Exception) {
                    Log.e("HuggingFaceClient", "parse error: ${e.message}")
                    callback("Error parsing response: ${e.message}\nBody snippet: ${body.take(800)}")
                }
            }
        })
    }
}
