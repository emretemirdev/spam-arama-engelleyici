package tr.com.covolt.aramaengelleyici.screening

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

/** Kullanıcının bu uygulamayı "arama filtresi ve spam" uygulaması seçip seçmediğini yönetir. */
object ScreeningRole {

    private fun roleManager(context: Context): RoleManager? =
        context.getSystemService(RoleManager::class.java)

    /** Telefon bu özelliği destekliyor mu? (Bazı üretici yazılımları desteklemeyebilir.) */
    fun isAvailable(context: Context): Boolean = runCatching {
        roleManager(context)?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
    }.getOrDefault(false)

    fun isHeld(context: Context): Boolean = runCatching {
        roleManager(context)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    }.getOrDefault(false)

    fun requestIntent(context: Context): Intent? = runCatching {
        roleManager(context)?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
    }.getOrNull()
}
