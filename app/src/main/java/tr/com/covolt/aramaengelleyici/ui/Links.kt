package tr.com.covolt.aramaengelleyici.ui

object Links {
    const val PACKAGE = "tr.com.covolt.aramaengelleyici"
    const val PLAY_STORE_WEB = "https://play.google.com/store/apps/details?id=$PACKAGE"
    const val PLAY_STORE_APP = "market://details?id=$PACKAGE"

    /** Ticari Elektronik İleti Yönetim Sistemi: vatandaşlar ticari arama/SMS iznini buradan kapatır. */
    const val IYS = "https://www.iys.org.tr"

    /**
     * YAPILACAK: Gizlilik politikasının yayınlandığı herkese açık adres (ör. şirket sitesi).
     * Play Console da aynı adresi ister. Metin: store/privacy-policy.html. Boş kalırsa satır gizlenir.
     */
    const val PRIVACY_POLICY = ""
}
