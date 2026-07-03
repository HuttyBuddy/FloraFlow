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
            "Developer Error: GEMINI_API_KEY is not configured. Please add your key to the .env file.",
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
            "Developer Error: GEMINI_API_KEY is not configured. Please add your key to the .env file.",
            result
        )
    }
}
