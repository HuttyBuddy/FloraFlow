package com.example.ui.screens.restoration

import android.media.MediaPlayer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SoundscapeServiceTest {

    @Test
    fun pauseSoundscape_mediaPlayerThrowsException_isCaught() {
        val service = Robolectric.buildService(SoundscapeService::class.java).create().get()

        // Set isPlaying to true using reflection, because the function returns early otherwise
        val isPlayingField = SoundscapeService::class.java.getDeclaredField("isPlaying")
        isPlayingField.isAccessible = true
        isPlayingField.set(service, true)

        // Mock MediaPlayer by subclassing and overriding pause()
        val mockMediaPlayer = object : MediaPlayer() {
            override fun pause() {
                throw IllegalStateException("Simulated MediaPlayer error")
            }
        }

        // Set mediaPlayer using reflection
        val mediaPlayerField = SoundscapeService::class.java.getDeclaredField("mediaPlayer")
        mediaPlayerField.isAccessible = true
        mediaPlayerField.set(service, mockMediaPlayer)

        // Call pauseSoundscape, which should catch the exception and not crash
        try {
            service.pauseSoundscape()
            // Verify that isPlaying was set to false
            assertFalse(isPlayingField.get(service) as Boolean)
        } catch (e: Exception) {
            fail("Exception should have been caught: ${e.message}")
        }
    }
}
