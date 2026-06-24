package com.example.ui.screens.restoration

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class SoundscapeServiceTest {

    @Test
    fun testMediaPlayerCreateException() {
        // Since MediaPlayer.create swallowing IOExceptions is a native Android behavior,
        // and we cannot use mockk due to lack of dependency, we can test the catch block
        // by passing an invalid Context that causes create() to throw a NullPointerException
        // internally or by setting up a ShadowMediaPlayer exception.
        // Actually, ShadowMediaPlayer.addException on DataSource with RuntimeException will throw RuntimeException during create.

        val context = RuntimeEnvironment.getApplication()
        val afd = context.resources.openRawResourceFd(com.example.R.raw.wind_chime)

        ShadowMediaPlayer.addException(
            DataSource.toDataSource(afd.fileDescriptor, afd.startOffset, afd.length),
            java.lang.RuntimeException("Mocked MediaPlayer exception")
        )

        val controller = Robolectric.buildService(SoundscapeService::class.java)
        val service = controller.create().get()

        val intent = Intent(service, SoundscapeService::class.java).apply { action = "ACTION_PLAY" }
        service.onStartCommand(intent, 0, 1)

        // This confirms that despite the exception, the code didn't crash and the state is preserved
        assertTrue("Service should start even if media player fails", service.isPlaying())
        assertEquals(0.5f, service.getAmbientVolume())
    }
}
