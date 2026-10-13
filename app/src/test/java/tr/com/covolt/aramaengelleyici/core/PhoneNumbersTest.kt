package tr.com.covolt.aramaengelleyici.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun turkishFormatsNormalizeToNational() {
        val expected = "08501234567"
        listOf(
            "+908501234567",
            "+90 850 123 45 67",
            "00908501234567",
            "908501234567",
            "08501234567",
            "8501234567",
            "0 (850) 123-45-67",
        ).forEach { raw ->
            val parsed = PhoneNumbers.parse(raw)!!
            assertEquals(raw, expected, parsed.normalized)
            assertFalse(raw, parsed.isInternational)
        }
    }

    @Test
    fun mobileNumber() {
        assertEquals("05321234567", PhoneNumbers.parse("+905321234567")!!.normalized)
    }

    @Test
    fun numbers444() {
        assertEquals("4441234", PhoneNumbers.parse("4441234")!!.normalized)
        assertEquals("4441234", PhoneNumbers.parse("04441234")!!.normalized)
        assertEquals("4441234", PhoneNumbers.parse("+904441234")!!.normalized)
        assertTrue(PhoneNumbers.is444("4441234"))
        assertTrue(PhoneNumbers.is444(PhoneNumbers.parse("+902124441234")!!.normalized))
        assertFalse(PhoneNumbers.is444("05321234567"))
    }

    @Test
    fun internationalNumbers() {
        val p1 = PhoneNumbers.parse("+447911123456")!!
        assertTrue(p1.isInternational)
        assertEquals("+447911123456", p1.normalized)

        val p2 = PhoneNumbers.parse("00447911123456")!!
        assertTrue(p2.isInternational)
        assertEquals("+447911123456", p2.normalized)
    }

    @Test
    fun emptyIsNull() {
        assertNull(PhoneNumbers.parse(null))
        assertNull(PhoneNumbers.parse(""))
        assertNull(PhoneNumbers.parse("  "))
        assertNull(PhoneNumbers.parse("abc"))
    }

    @Test
    fun prefixes() {
        assertEquals("0850", PhoneNumbers.normalizePrefix("0850"))
        assertEquals("0850", PhoneNumbers.normalizePrefix("850"))
        assertEquals("0850", PhoneNumbers.normalizePrefix("+90 850"))
        assertEquals("0212", PhoneNumbers.normalizePrefix("0212"))
        assertEquals("444", PhoneNumbers.normalizePrefix("444"))
        assertEquals("444", PhoneNumbers.normalizePrefix("0444"))
        assertEquals("+44", PhoneNumbers.normalizePrefix("+44"))
        assertEquals("+44", PhoneNumbers.normalizePrefix("0044"))
        assertNull(PhoneNumbers.normalizePrefix("0"))
        assertNull(PhoneNumbers.normalizePrefix("x"))
    }

    @Test
    fun pretty() {
        assertEquals("0850 123 45 67", PhoneNumbers.pretty("08501234567"))
        assertEquals("444 12 34", PhoneNumbers.pretty("4441234"))
        assertEquals("+447911123456", PhoneNumbers.pretty("+447911123456"))
    }
}
