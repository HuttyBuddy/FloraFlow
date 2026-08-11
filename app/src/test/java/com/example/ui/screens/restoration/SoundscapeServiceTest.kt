package com.example.ui.screens.restoration

import android.media.AudioTrack
import android.os.Build
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Answers
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLog
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class SoundscapeServiceTest {
    @Test
    fun testAudioTrackInitError() {
        ShadowLog.stream = System.out

        Mockito.mockConstruction(
            AudioTrack.Builder::class.java,
            Mockito.withSettings().defaultAnswer(Answers.RETURNS_SELF)
        ) { mock, context ->
            Mockito.`when`(mock.build()).thenThrow(UnsupportedOperationException("Simulated exception"))
        }.use {
            val controller = Robolectric.buildService(SoundscapeService::class.java)
            val service = controller.create().get()
            service.startSoundscape()

            val logs = ShadowLog.getLogsForTag("SoundscapeService")
            val hasError = logs.any { it.type == android.util.Log.ERROR && it.msg.contains("AudioTrack init failed") }
            assertTrue("Expected error log not found", hasError)
        }
    }

    @Test
    fun testAudioTrackWriteExceptionBreaksRenderLoop() {
        val mockAudioTrack = Mockito.mock(AudioTrack::class.java)

        Mockito.`when`(mockAudioTrack.write(
            Mockito.any(ShortArray::class.java),
            Mockito.anyInt(),
            Mockito.anyInt()
        )).thenThrow(RuntimeException("Simulated write error"))

        Mockito.mockConstruction(
            AudioTrack.Builder::class.java,
            Mockito.withSettings().defaultAnswer(Answers.RETURNS_SELF)
        ) { mock, context ->
            Mockito.`when`(mock.build()).thenReturn(mockAudioTrack)
        }.use {
            val controller = Robolectric.buildService(SoundscapeService::class.java)
            val service = controller.create().get()

            service.startSoundscape()

            Mockito.verify(mockAudioTrack, Mockito.timeout(2000).atLeastOnce()).write(
                Mockito.any(ShortArray::class.java),
                Mockito.anyInt(),
                Mockito.anyInt()
            )

            Mockito.verify(mockAudioTrack, Mockito.timeout(2000).atLeastOnce()).stop()
            Mockito.verify(mockAudioTrack, Mockito.timeout(2000).atLeastOnce()).release()
        }
    }
}
