package tr.com.covolt.aramaengelleyici.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import tr.com.covolt.aramaengelleyici.core.RuleConfig

enum class BlockAction { REJECT, SILENCE }

/**
 * Tüm ayarlar yalnızca cihazda, SharedPreferences içinde tutulur.
 * Arama filtresi servisi her aramada buradan okur; okuma senkron ve hızlıdır.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var isPro: Boolean
        get() = prefs.getBoolean(K_PRO, false)
        set(value) = prefs.edit { putBoolean(K_PRO, value) }

    var block0850: Boolean
        get() = prefs.getBoolean(K_0850, true)
        set(value) = prefs.edit { putBoolean(K_0850, value) }

    var block444: Boolean
        get() = prefs.getBoolean(K_444, false)
        set(value) = prefs.edit { putBoolean(K_444, value) }

    var blockHidden: Boolean
        get() = prefs.getBoolean(K_HIDDEN, true)
        set(value) = prefs.edit { putBoolean(K_HIDDEN, value) }

    var blockInternational: Boolean
        get() = prefs.getBoolean(K_INTL, false)
        set(value) = prefs.edit { putBoolean(K_INTL, value) }

    var blockAllUnknown: Boolean
        get() = prefs.getBoolean(K_UNKNOWN, false)
        set(value) = prefs.edit { putBoolean(K_UNKNOWN, value) }

    var nightModeEnabled: Boolean
        get() = prefs.getBoolean(K_NIGHT, false)
        set(value) = prefs.edit { putBoolean(K_NIGHT, value) }

    var nightStartMinute: Int
        get() = prefs.getInt(K_NIGHT_START, 23 * 60)
        set(value) = prefs.edit { putInt(K_NIGHT_START, value) }

    var nightEndMinute: Int
        get() = prefs.getInt(K_NIGHT_END, 8 * 60)
        set(value) = prefs.edit { putInt(K_NIGHT_END, value) }

    var pausedUntilMillis: Long
        get() = prefs.getLong(K_PAUSED, 0L)
        set(value) = prefs.edit { putLong(K_PAUSED, value) }

    var action: BlockAction
        get() = runCatching { BlockAction.valueOf(prefs.getString(K_ACTION, null) ?: "") }
            .getOrDefault(BlockAction.REJECT)
        set(value) = prefs.edit { putString(K_ACTION, value.name) }

    var blacklist: Set<String>
        get() = readSet(K_BLACK)
        set(value) = prefs.edit { putStringSet(K_BLACK, HashSet(value)) }

    var whitelist: Set<String>
        get() = readSet(K_WHITE)
        set(value) = prefs.edit { putStringSet(K_WHITE, HashSet(value)) }

    var customPrefixes: Set<String>
        get() = readSet(K_PREFIX)
        set(value) = prefs.edit { putStringSet(K_PREFIX, HashSet(value)) }

    fun toConfig(): RuleConfig = RuleConfig(
        isPro = isPro,
        block0850 = block0850,
        block444 = block444,
        blockHidden = blockHidden,
        blockInternational = blockInternational,
        blockAllUnknown = blockAllUnknown,
        nightModeEnabled = nightModeEnabled,
        nightStartMinute = nightStartMinute,
        nightEndMinute = nightEndMinute,
        pausedUntilMillis = pausedUntilMillis,
        blacklist = blacklist,
        whitelist = whitelist,
        customPrefixes = customPrefixes,
    )

    // getStringSet'in döndürdüğü örnek değiştirilmemeli; kopyasını veriyoruz.
    private fun readSet(key: String): Set<String> =
        prefs.getStringSet(key, null)?.toSet() ?: emptySet()

    private companion object {
        const val FILE = "settings"
        const val K_PRO = "is_pro"
        const val K_0850 = "block_0850"
        const val K_444 = "block_444"
        const val K_HIDDEN = "block_hidden"
        const val K_INTL = "block_international"
        const val K_UNKNOWN = "block_all_unknown"
        const val K_NIGHT = "night_mode"
        const val K_NIGHT_START = "night_start"
        const val K_NIGHT_END = "night_end"
        const val K_PAUSED = "paused_until"
        const val K_ACTION = "action"
        const val K_BLACK = "blacklist"
        const val K_WHITE = "whitelist"
        const val K_PREFIX = "custom_prefixes"
    }
}
