package com.example.data.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiApiClientTest {

    @Test
    fun getGardeningAdvice_emptyApiKey_returnsMissingKeyMessage() = runBlocking {
        val result = GeminiApiClient.getGardeningAdvice(
            prompt = "How to grow roses?",
            apiKey = ""
        )

        assertEquals(
            "API Key is missing. Please enter your GEMINI_API_KEY in the AI Studio Secrets panel.",
            result
        )
    }

    @Test
    fun getGardeningAdvice_placeholderApiKey_returnsMissingKeyMessage() = runBlocking {
        val result = GeminiApiClient.getGardeningAdvice(
            prompt = "How to grow roses?",
            apiKey = "MY_GEMINI_API_KEY"
        )

        assertEquals(
            "API Key is missing. Please enter your GEMINI_API_KEY in the AI Studio Secrets panel.",
            result
        )
    }
}
