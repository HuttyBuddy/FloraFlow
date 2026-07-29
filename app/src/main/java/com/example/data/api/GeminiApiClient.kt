package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response Data Classes styled with Moshi ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String // base64 encoded image string
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxOutputTokens: Int? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

// --- Retrofit Service ---

interface GeminiApiService {
    @POST
    suspend fun generateContent(
        @Url url: String,
        @Query("key") apiKey: String?,
        @Header("X-App-Secret") appSecret: String?,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain: Interceptor.Chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Cache-Control", "no-store, no-cache")
                .header("User-Agent", "FloraFlow-Secure-Client/1.0")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .build()

    // Moshi Instance — relies solely on generated adapters (@JsonClass(generateAdapter = true))
    // for every request/response type here, which is R8/minification-safe. Do not add the
    // reflection-based KotlinJsonAdapterFactory back; it breaks under ProGuard/R8 shrinking.
    private val moshi = Moshi.Builder()
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun getGardeningAdvice(
        prompt: String,
        chatHistory: List<Content> = emptyList(),
        systemInstruction: String? = null,
        imageBytesBase64: String? = null,
        imageMimeType: String? = "image/jpeg",
        apiKey: String = BuildConfig.GEMINI_API_KEY
    ): String = withContext(Dispatchers.IO) {
        // Normalize the configured proxy value so it works whether the .env
        // entry is host-only ("my-proxy.run.app") or was pasted with a scheme
        // and/or trailing slash ("https://my-proxy.run.app/") — either form
        // must not produce a malformed "https://https://..." URL.
        val rawProxyValue = try { BuildConfig.GEMINI_PROXY_URL } catch (_: Exception) { "" }
        val proxyHost = rawProxyValue
            .trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
        val isProxyActive = proxyHost.isNotEmpty() && proxyHost != "none" && proxyHost != "YOUR_PROXY_URL"

        val endpointUrl = if (isProxyActive) {
            "https://$proxyHost/v1beta/models/gemini-3.5-flash:generateContent"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
        }

        if (!isProxyActive && (apiKey == "DEMO_MODE" || apiKey == "OFFLINE_MODE")) {
            return@withContext getOfflineBotanicalAdvice(prompt)
        }

        val requestKey = if (isProxyActive) {
            null
        } else {
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext if (com.example.BuildConfig.DEBUG) {
                    "Developer Error: GEMINI_API_KEY is not configured. Please add your key to the .env file."
                } else {
                    "AI features are temporarily unavailable. Please try again later."
                }
            }
            apiKey
        }

        val appSecret = if (isProxyActive) {
            try { BuildConfig.APP_SHARED_SECRET.takeIf { it.isNotBlank() && it != "YOUR_APP_SHARED_SECRET" } } catch (_: Exception) { null }
        } else {
            null
        }

        val formattedContents = mutableListOf<Content>()
        if (chatHistory.isNotEmpty()) {
            formattedContents.addAll(chatHistory)
        }

        val userParts = mutableListOf<Part>()
        if (imageBytesBase64 != null) {
            userParts.add(Part(inlineData = InlineData(mimeType = imageMimeType ?: "image/jpeg", data = imageBytesBase64)))
        }
        userParts.add(Part(text = prompt))

        // Add the current prompt (and optional image)
        formattedContents.add(Content(role = "user", parts = userParts))

        val request = GenerateContentRequest(
            contents = formattedContents,
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = systemInstruction?.let {
                Content(parts = listOf(Part(text = it)))
            }
        )

        try {
            val response = service.generateContent(endpointUrl, requestKey, appSecret, request)
            val candidate = response.candidates?.firstOrNull()
            if (candidate != null && candidate.finishReason != null) {
                if (candidate.finishReason.contains("SAFETY", ignoreCase = true)) {
                    return@withContext "Content was flagged for safety. Please rephrase your request to be more garden-appropriate."
                }
            }
            candidate?.content?.parts?.firstOrNull()?.text 
                ?: "No advice received. Please try again."
        } catch (e: java.io.IOException) {
            "Network Error: Please check your internet connection and try again."
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                401 -> "Authentication Error: Your Gemini API key might be invalid."
                403 -> "Permission Denied: Ensure your API key has the correct permissions."
                429 -> "Too Many Requests: You're sending requests too quickly."
                else -> "API Error: The server returned an error (${e.code()})."
            }
        } catch (e: Exception) {
            "Botanical Sync Error: An unexpected error occurred. This might be a persistent issue with the AI service."
        }
    }

    fun isAiError(response: String): Boolean {
        return response.startsWith("Network Error:") ||
               response.startsWith("Authentication Error:") ||
               response.startsWith("Permission Denied:") ||
               response.startsWith("Too Many Requests:") ||
               response.startsWith("API Error:") ||
               response.startsWith("Botanical Sync Error:") ||
               response.contains("API Key is missing") ||
               response.startsWith("Developer Error:") ||
               response.startsWith("AI features are temporarily") ||
               response.startsWith("Content was flagged for safety.")
    }

    private fun getOfflineBotanicalAdvice(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("yellow") || lower.contains("speckled") || lower.contains("pest") || lower.contains("cure") -> {
                "🌱 **Dr. Julian's Diagnostic Analysis:**\n\n" +
                "Speckled, yellowing leaves on young plants typically indicate early-stage **spider mites** or **thrips** feeding on plant cell sap, or a soil nutrient lockup.\n\n" +
                "**Organic Therapeutic Cures:**\n" +
                "1. 🧴 **Cold-Pressed Neem Oil Spray:** Mix 1 tsp organic Neem Oil with 1/2 tsp gentle Castile soap in 1L warm water. Spray undersides of leaves weekly.\n" +
                "2. 🧽 **Insecticidal Soap Wipe:** Gently clean foliage with mild soapy water to remove physical pest eggs.\n" +
                "3. 🪴 **Resilient Replacements:** Consider isolating infected plants and introducing sturdy species like **Snake Plant** or **ZZ Plant** while leaves recover."
            }
            lower.contains("monstera") || lower.contains("light") || lower.contains("window") -> {
                "🌿 **Lighting & Microclimate Guidance:**\n\n" +
                "**Monstera Deliciosa** thrives best in bright, indirect daylight (about 200–400 foot-candles). Avoid harsh direct afternoon sun which can scorch fenestrated leaves.\n\n" +
                "**Styling Tip:** Pair near a **Peace Lily** or **Pothos** for layered canopy elevation and humidity retention!"
            }
            lower.contains("companion") || lower.contains("match") || lower.contains("pair") -> {
                "🌸 **Biophilic Companion Match:**\n\n" +
                "Great choices for indoor biophilic harmony include pairing **Monstera Deliciosa** with low-tier **Pothos** trailing vines and a vertical **Fiddle Leaf Fig**. These create a multi-layer air-purifying indoor sanctuary!"
            }
            else -> {
                "🪴 **Dr. Julian's Biophilic Advice:**\n\n" +
                "Indoor plants thrive when light, airflow, and soil moisture are in rhythm. For optimal sanctuary growth, balance humidity around 50% and group complementary species like **Peace Lily**, **Snake Plant**, and **Pothos**!"
            }
        }
    }
}
