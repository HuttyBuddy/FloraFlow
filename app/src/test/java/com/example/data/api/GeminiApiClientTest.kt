package com.example.data.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.anyOrNull
import java.io.IOException

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

    @Test
    fun getGardeningAdvice_networkError_returnsErrorMessage() = runBlocking {
        // Arrange
        val originalService = GeminiApiClient.service
        val mockService = mock<GeminiApiService>()
        whenever(mockService.generateContent(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenAnswer {
            throw IOException("Network error")
        }

        try {
            GeminiApiClient.service = mockService

            // Act
            val result = GeminiApiClient.getGardeningAdvice(
                prompt = "How to grow roses?",
                apiKey = "test_key"
            )

            // Assert
            assertEquals("Network Error: Please check your internet connection and try again.", result)
            assertTrue(GeminiApiClient.isAiError(result))
        } finally {
            // Restore
            GeminiApiClient.service = originalService
        }
    }
}
