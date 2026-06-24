package com.example.ui.screens.restoration

import android.media.MediaPlayer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SoundscapeServiceTest {

    private lateinit var service: SoundscapeService

    @Before
    fun setup() {
        service = Robolectric.buildService(SoundscapeService::class.java).create().get()
    }

    @After
    fun teardown() {
        // Reset ShadowMediaPlayer to avoid leaking state into other tests
        ShadowMediaPlayer.setCreateListener(null)
    }

    @Test
    fun `test MediaPlayer create exception in playAmbientLoop`() {
        // Setup mock so MediaPlayer.create fails and returns null or throws
        ShadowMediaPlayer.setCreateListener { player, shadow ->
            throw RuntimeException("Simulated MediaPlayer exception")
        }

        // Action
        service.startSoundscape()

        // Verify: Start should handle exception and complete
        assertTrue(service.isPlaying())
        // Should catch the exception in playAmbientLoop and not crash.
    }
}
