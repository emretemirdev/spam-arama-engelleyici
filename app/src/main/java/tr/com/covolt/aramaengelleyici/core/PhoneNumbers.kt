package tr.com.covolt.aramaengelleyici.core

/**
 * Gelen numarayı tek bir biçime çevirir.
 *
 * Türkiye numaraları "0" ile başlayan ulusal biçime ("08501234567", "05321234567"),
 * 444'lü numaralar başlarında 0 olmadan ("4441234") tutulur. Yurt dışı numaralar "+" ile
 * başlar ve [ParsedNumber.isInternational] true olur.
 */
data class ParsedNumber(
    val normalized: String,
    val isInternational: Boolean,
)

object PhoneNumbers {

    fun parse(raw: String?): ParsedNumber? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return null

        return when {
            hasPlus && digits.startsWith("90") -> turkish(digits.substring(2))
            hasPlus -> ParsedNumber("+$digits", isInternational = true)
            digits.startsWith("0090") -> turkish(digits.substring(4))
            digits.startsWith("00") -> ParsedNumber("+" + digits.substring(2), isInternational = true)
            digits.startsWith("90") && digits.length == 12 -> turkish(digits.substring(2))
            else -> turkish(digits)
        }
    }

    /** Ülke kodu çıkarılmış Türkiye numarasını ulusal biçime çevirir. */
    private fun turkish(nationalDigits: String): ParsedNumber {
        val n = nationalDigits.trimStart('0')
        val normalized = when {
            n.startsWith("444") && n.length <= 7 -> n
            n.length == 10 -> "0$n"
            else -> nationalDigits
        }
        return ParsedNumber(normalized, isInternational = false)
    }

    /** Kullanıcının listeye yazdığı numarayı karşılaştırılabilir biçime çevirir. */
    fun normalizeForList(raw: String): String? = parse(raw)?.normalized

    /** Kullanıcının yazdığı öneki ("0 850", "+90 212 444") karşılaştırılabilir biçime çevirir. */
    fun normalizePrefix(raw: String): String? {
        val trimmed = raw.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        val hasPlus = trimmed.startsWith("+")
        if (hasPlus && !digits.startsWith("90")) return "+$digits"
        if (!hasPlus && digits.startsWith("00") && !digits.startsWith("0090")) {
            val rest = digits.substring(2)
            return if (rest.isEmpty()) null else "+$rest"
        }
        val national = when {
            hasPlus -> digits.substring(2)
            digits.startsWith("0090") -> digits.substring(4)
            else -> digits
        }.trimStart('0')
        if (national.isEmpty()) return null
        return if (national.startsWith("444")) national else "0$national"
    }

    fun is0850(normalized: String): Boolean = normalized.startsWith("0850")

    /** "4441234" ya da alan koduyla gelen "02124441234" biçimindeki 444 numaraları. */
    fun is444(normalized: String): Boolean =
        normalized.startsWith("444") ||
            (normalized.length == 11 && normalized.startsWith("0") && normalized.substring(4, 7) == "444")

    /** Ekranda gösterim için: 08501234567 -> 0850 123 45 67 */
    fun pretty(normalized: String): String {
        if (normalized.length == 11 && normalized.startsWith("0")) {
            return "${normalized.substring(0, 4)} ${normalized.substring(4, 7)} " +
                "${normalized.substring(7, 9)} ${normalized.substring(9)}"
        }
        if (normalized.length == 7 && normalized.startsWith("444")) {
            return "444 ${normalized.substring(3, 5)} ${normalized.substring(5)}"
        }
        return normalized
    }
}
