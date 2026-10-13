package tr.com.covolt.aramaengelleyici

import android.content.Context

/** Her testten önce cihazdaki ayar ve geçmiş dosyalarını sıfırlar. */
fun Context.clearAppPrefs() {
    listOf("settings", "history").forEach {
        getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
    }
}
