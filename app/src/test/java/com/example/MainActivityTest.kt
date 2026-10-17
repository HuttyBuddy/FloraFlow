package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class MainActivityTest {

    @Test
    fun testOnCreate_workManagerThrowsException_isCaughtAndLogged() {
        ShadowLog.setupLogging()
        // Without initializing WorkManager in Robolectric, it throws an exception.
        // Let's see if MainActivity catches it and logs the error.
        ActivityScenario.launch(MainActivity::class.java).use {
            val logs = ShadowLog.getLogsForTag("MainActivity")
            val hasErrorLog = logs.any { log ->
                log.msg.contains("Failed to schedule CareSyncWorker")
            }
            assertTrue("Expected error log not found", hasErrorLog)
        }
    }
}
