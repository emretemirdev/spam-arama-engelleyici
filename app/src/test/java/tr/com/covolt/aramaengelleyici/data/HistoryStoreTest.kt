package tr.com.covolt.aramaengelleyici.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tr.com.covolt.aramaengelleyici.clearAppPrefs
import tr.com.covolt.aramaengelleyici.core.BlockReason

@RunWith(AndroidJUnit4::class)
class HistoryStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = context.clearAppPrefs()

    @Test
    fun newestFirstAndTotal() {
        val h = HistoryStore(context)
        h.add(BlockedCall("08500000001", BlockReason.N0850, 1_000))
        h.add(BlockedCall("Gizli numara", BlockReason.HIDDEN, 2_000))
        h.add(BlockedCall("+447911123456", BlockReason.INTERNATIONAL, 3_000))

        val all = HistoryStore(context).all()
        assertEquals(listOf(3_000L, 2_000L, 1_000L), all.map { it.timeMillis })
        assertEquals(BlockReason.INTERNATIONAL, all.first().reason)
        assertEquals(3L, h.totalCount())
    }

    @Test
    fun countSince() {
        val h = HistoryStore(context)
        h.add(BlockedCall("08500000001", BlockReason.N0850, 1_000))
        h.add(BlockedCall("08500000002", BlockReason.N0850, 5_000))
        h.add(BlockedCall("08500000003", BlockReason.N0850, 9_000))
        assertEquals(2, h.countSince(5_000))
        assertEquals(0, h.countSince(10_000))
    }

    @Test
    fun keepsOnlyLatestEntriesButCountsAll() {
        val h = HistoryStore(context)
        repeat(305) { i -> h.add(BlockedCall("0850000%04d".format(i), BlockReason.N0850, i.toLong())) }
        val all = h.all()
        assertEquals(300, all.size)
        assertEquals(304L, all.first().timeMillis)
        assertEquals(305L, h.totalCount())
    }

    @Test
    fun clearKeepsTotalCounter() {
        val h = HistoryStore(context)
        h.add(BlockedCall("08500000001", BlockReason.N0850, 1_000))
        h.clear()
        assertTrue(h.all().isEmpty())
        assertEquals(1L, h.totalCount())
    }

    @Test
    fun corruptedJsonIsIgnored() {
        context.getSharedPreferences("history", Context.MODE_PRIVATE)
            .edit().putString("entries", "{bozuk").commit()
        assertTrue(HistoryStore(context).all().isEmpty())
    }
}
