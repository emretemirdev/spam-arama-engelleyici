package tr.com.covolt.aramaengelleyici.core

/** Engelleme kurallarının o anki hali. Saf veri: test edilebilir, Android'e bağlı değil. */
data class RuleConfig(
    val isPro: Boolean = false,
    val block0850: Boolean = true,
    val block444: Boolean = false,
    val blockHidden: Boolean = true,
    val blockInternational: Boolean = false,
    val blockAllUnknown: Boolean = false,
    val nightModeEnabled: Boolean = false,
    val nightStartMinute: Int = 23 * 60,
    val nightEndMinute: Int = 8 * 60,
    val pausedUntilMillis: Long = 0L,
    val blacklist: Set<String> = emptySet(),
    val whitelist: Set<String> = emptySet(),
    val customPrefixes: Set<String> = emptySet(),
)

enum class BlockReason(val label: String) {
    BLACKLIST("Kara liste"),
    HIDDEN("Gizli numara"),
    N0850("0850"),
    N444("444"),
    INTERNATIONAL("Yurt dışı"),
    CUSTOM_PREFIX("Özel önek"),
    NIGHT("Gece modu"),
    UNKNOWN("Rehber dışı"),
}

sealed interface Decision {
    data object Allow : Decision
    data class Block(val reason: BlockReason) : Decision
}

object RuleEngine {

    /** Ücretsiz sürümde kara listeye eklenebilecek en fazla numara. */
    const val FREE_BLACKLIST_LIMIT = 5

    /**
     * Rehberde OLMAYAN bir numaradan gelen aramaya ne yapılacağına karar verir.
     * (Android rehberdeki numaraları arama filtresine hiç göndermez.)
     *
     * Sıra: beyaz liste > gizli numara > kara liste > "kargo bekliyorum" duraklatması >
     * 0850 > 444 > yurt dışı > özel önek > gece modu > rehber dışı herkes.
     */
    fun decide(
        rawNumber: String?,
        isHidden: Boolean,
        config: RuleConfig,
        nowMillis: Long,
        minuteOfDay: Int,
    ): Decision {
        val parsed = if (isHidden) null else PhoneNumbers.parse(rawNumber)

        if (parsed == null) {
            return if (config.blockHidden) Decision.Block(BlockReason.HIDDEN) else Decision.Allow
        }

        val number = parsed.normalized
        if (number in config.whitelist) return Decision.Allow
        if (number in config.blacklist) return Decision.Block(BlockReason.BLACKLIST)

        // "Kargo bekliyorum": otomatik kurallar geçici olarak kapalı, listeler çalışmaya devam eder.
        if (nowMillis < config.pausedUntilMillis) return Decision.Allow

        if (config.block0850 && PhoneNumbers.is0850(number)) {
            return Decision.Block(BlockReason.N0850)
        }

        if (!config.isPro) return Decision.Allow

        if (config.block444 && PhoneNumbers.is444(number)) {
            return Decision.Block(BlockReason.N444)
        }
        if (config.blockInternational && parsed.isInternational) {
            return Decision.Block(BlockReason.INTERNATIONAL)
        }
        if (config.customPrefixes.any { it.isNotBlank() && number.startsWith(it) }) {
            return Decision.Block(BlockReason.CUSTOM_PREFIX)
        }
        if (config.nightModeEnabled &&
            isInWindow(minuteOfDay, config.nightStartMinute, config.nightEndMinute)
        ) {
            return Decision.Block(BlockReason.NIGHT)
        }
        if (config.blockAllUnknown) {
            return Decision.Block(BlockReason.UNKNOWN)
        }
        return Decision.Allow
    }

    /** [start, end) aralığında mı? Gece yarısını geçen aralıkları (23:00-08:00) da destekler. */
    fun isInWindow(minuteOfDay: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> minuteOfDay in start until end
        else -> minuteOfDay >= start || minuteOfDay < end
    }
}
