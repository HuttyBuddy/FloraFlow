package com.example

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class MainActivityTest {
    @Test
    fun `onCreate handles WorkManager initialization exception`() {
        ShadowLog.stream = System.out

        // Launch MainActivity using Robolectric
        // WorkManager is not initialized in Robolectric tests by default,
        // so WorkManager.getInstance(applicationContext) will throw an exception.
        // We verify that the catch block catches it and logs the error.
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()

        val logs = ShadowLog.getLogsForTag("MainActivity")
        val hasErrorLog = logs.any { log ->
            log.type == Log.ERROR && log.msg.contains("Failed to schedule CareSyncWorker")
        }

        assertTrue("Expected error log for WorkManager initialization failure", hasErrorLog)
    }
}
