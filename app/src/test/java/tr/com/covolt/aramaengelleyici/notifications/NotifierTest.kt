package tr.com.covolt.aramaengelleyici.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import tr.com.covolt.aramaengelleyici.ui.shiftMinute

@RunWith(AndroidJUnit4::class)
class NotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun weeklyText() {
        assertNull(Notifier.weeklySummaryText(0))
        assertEquals("Bu hafta 1 istenmeyen aramayı senin yerine engelledik.", Notifier.weeklySummaryText(1))
        assertEquals("Bu hafta 17 istenmeyen aramayı senin yerine engelledik.", Notifier.weeklySummaryText(17))
    }

    @Test
    fun weeklySummaryNotPostedWithoutPermission() {
        val manager = context.getSystemService(NotificationManager::class.java)
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Notifier.showWeeklySummary(context, 5)
        assertEquals(0, shadowOf(manager).allNotifications.size)

        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Notifier.showWeeklySummary(context, 5)
        assertEquals(1, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun channelsAreCreated() {
        Notifier.ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val ids = manager.notificationChannels.map { it.id }
        assertTrue(Notifier.CHANNEL_BLOCKED in ids)
        assertTrue(Notifier.CHANNEL_SUMMARY in ids)
    }

    @Test
    fun timeStepperWrapsAroundMidnight() {
        assertEquals(0, shiftMinute(23 * 60 + 30, 30))
        assertEquals(23 * 60 + 30, shiftMinute(0, -30))
        assertEquals(8 * 60 + 30, shiftMinute(8 * 60, 30))
    }
}
