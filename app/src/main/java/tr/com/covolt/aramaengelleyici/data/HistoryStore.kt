package tr.com.covolt.aramaengelleyici.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import tr.com.covolt.aramaengelleyici.core.BlockReason

data class BlockedCall(
    val number: String,
    val reason: BlockReason,
    val timeMillis: Long,
)

/**
 * Engellenen aramaların geçmişi. Yalnızca cihazda tutulur, hiçbir yere gönderilmez.
 * Son [MAX_ENTRIES] kayıt saklanır; toplam sayaç ayrıca tutulur.
 */
class HistoryStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun add(call: BlockedCall) = synchronized(LOCK) {
        val updated = listOf(call) + all()
        val total = totalCount() + 1
        prefs.edit {
            putString(K_ENTRIES, encode(updated.take(MAX_ENTRIES)))
            putLong(K_TOTAL, total)
        }
    }

    /** En yeni kayıt en başta. */
    fun all(): List<BlockedCall> = synchronized(LOCK) {
        decode(prefs.getString(K_ENTRIES, null))
    }

    fun countSince(sinceMillis: Long): Int = all().count { it.timeMillis >= sinceMillis }

    fun totalCount(): Long = prefs.getLong(K_TOTAL, 0L)

    fun clear() = synchronized(LOCK) {
        prefs.edit { remove(K_ENTRIES) }
    }

    private fun encode(list: List<BlockedCall>): String {
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject()
                    .put("n", it.number)
                    .put("r", it.reason.name)
                    .put("t", it.timeMillis)
            )
        }
        return array.toString()
    }

    private fun decode(json: String?): List<BlockedCall> {
        if (json.isNullOrEmpty()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val o = array.getJSONObject(i)
                val reason = runCatching { BlockReason.valueOf(o.getString("r")) }.getOrNull()
                    ?: return@mapNotNull null
                BlockedCall(o.getString("n"), reason, o.getLong("t"))
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val FILE = "history"
        const val K_ENTRIES = "entries"
        const val K_TOTAL = "total"
        const val MAX_ENTRIES = 300
        val LOCK = Any()
    }
}
