package tr.com.covolt.aramaengelleyici.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import tr.com.covolt.aramaengelleyici.data.HistoryStore
import tr.com.covolt.aramaengelleyici.data.SettingsStore
import java.util.concurrent.TimeUnit

/** Haftada bir, son 7 günde engellenen arama sayısını bildirim olarak gösterir. */
class WeeklySummaryWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        val context = applicationContext
        if (!SettingsStore(context).weeklySummary) return Result.success()
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val count = HistoryStore(context).countSince(since)
        Notifier.showWeeklySummary(context, count)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "haftalik_ozet"

        /** Ayar açıksa haftalık işi kurar (zaten kuruluysa dokunmaz), kapalıysa iptal eder. */
        fun sync(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (enabled) {
                val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
                    .setInitialDelay(7, TimeUnit.DAYS)
                    .build()
                workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            } else {
                workManager.cancelUniqueWork(WORK_NAME)
            }
        }
    }
}
