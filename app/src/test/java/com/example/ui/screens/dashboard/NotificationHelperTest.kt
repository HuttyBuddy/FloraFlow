package com.example.ui.screens.dashboard

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertTrue
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Implementation
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [NotificationHelperTest.ThrowingShadowNotificationManager::class])
class NotificationHelperTest {

    @Implements(NotificationManager::class)
    class ThrowingShadowNotificationManager : ShadowNotificationManager() {
        companion object {
            var wasCalled = false
        }

        @Implementation
        override fun notify(tag: String?, id: Int, notification: Notification?) {
            wasCalled = true
            throw SecurityException("Mock Security Exception for Testing")
        }
    }

    @Test
    fun sendCareReminder_catchesSecurityException() {
        ThrowingShadowNotificationManager.wasCalled = false
        val context = ApplicationProvider.getApplicationContext<Context>()

        var exceptionBubbledUp = false
        try {
            NotificationHelper.sendCareReminder(
                context = context,
                title = "Test Title",
                message = "Test Message"
            )
        } catch (e: SecurityException) {
            exceptionBubbledUp = true
        }

        assertTrue("The mock method was never called! The test is invalid.", ThrowingShadowNotificationManager.wasCalled)
        assertTrue("SecurityException should be caught within sendCareReminder, but it bubbled up", !exceptionBubbledUp)
    }
}
