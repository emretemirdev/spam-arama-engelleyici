package tr.com.covolt.aramaengelleyici.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import tr.com.covolt.aramaengelleyici.MainActivity
import tr.com.covolt.aramaengelleyici.R
import tr.com.covolt.aramaengelleyici.core.BlockReason
import tr.com.covolt.aramaengelleyici.core.PhoneNumbers

/** Uygulamanın gösterdiği iki bildirim: engellenen arama ve haftalık özet. */
object Notifier {

    const val CHANNEL_BLOCKED = "engellenen_aramalar"
    const val CHANNEL_SUMMARY = "haftalik_ozet"
    const val ID_BLOCKED = 1001
    const val ID_SUMMARY = 1002

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BLOCKED,
                "Engellenen aramalar",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Bir arama engellendiğinde sessiz bildirim" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SUMMARY,
                "Haftalık özet",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Haftada bir kaç arama engellendiğini gösterir" }
        )
    }

    /** Android 13+ bildirim izni verildi mi ve kullanıcı bildirimleri kapatmadı mı? */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showBlocked(context: Context, shownNumber: String, reason: BlockReason) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_BLOCKED)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("Arama engellendi")
            .setContentText("${PhoneNumbers.pretty(shownNumber)} · ${reason.label}")
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(context).notify(ID_BLOCKED, notification)
    }

    fun showWeeklySummary(context: Context, blockedThisWeek: Int) {
        val text = weeklySummaryText(blockedThisWeek) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("Haftalık özet")
            .setContentText(text)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_SUMMARY, notification)
    }

    /** Hiç engelleme yoksa bildirim gösterilmez (gürültü yapmasın). */
    fun weeklySummaryText(blockedThisWeek: Int): String? = when {
        blockedThisWeek <= 0 -> null
        blockedThisWeek == 1 -> "Bu hafta 1 istenmeyen aramayı senin yerine engelledik."
        else -> "Bu hafta $blockedThisWeek istenmeyen aramayı senin yerine engelledik."
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
