package tr.com.covolt.aramaengelleyici.screening

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

/** Kullanıcının bu uygulamayı "arama filtresi ve spam" uygulaması seçip seçmediğini yönetir. */
object ScreeningRole {

    private fun roleManager(context: Context): RoleManager? =
        context.getSystemService(RoleManager::class.java)

    /** Telefon bu özelliği destekliyor mu? (Bazı üretici yazılımları desteklemeyebilir.) */
    fun isAvailable(context: Context): Boolean =
        roleManager(context)?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true

    fun isHeld(context: Context): Boolean =
        roleManager(context)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

    fun requestIntent(context: Context): Intent? =
        roleManager(context)?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
}
