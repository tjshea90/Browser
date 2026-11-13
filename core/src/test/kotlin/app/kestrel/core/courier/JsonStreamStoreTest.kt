package app.kestrel.core.courier

import app.kestrel.core.download.DlState
import app.kestrel.core.media.StreamRec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The stream download record as a file: what the recovery of a refused link needs survives a restart (EY-8), and a record written before those fields existed still loads. */
class JsonStreamStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val player = mapOf("Referer" to "https://player.test/", "Origin" to "https://player.test")
    private val site = mapOf("Referer" to "https://site.test/")

    @Test fun theRecoveryDataSurvivesASaveAndALoad() {
        val dir = tmp.newFolder("streams")
        val r = StreamRec(1_000_000_001L)
        r.state = DlState.PAUSED_SYSTEM; r.url = "https://cdn.test/v/720.m3u8"; r.headers = player
        r.masterUrl = "https://cdn.test/v/master.m3u8"; r.frame = "https://player.test/embed/7"
        r.headerSets = listOf(player, site, emptyMap())
        r.fp = "fp-1"; r.madeAt = 1_700_000_000_000L; r.expiresAt = 1_700_000_600_000L
        JsonStreamStore(dir).save(r)
        val back = JsonStreamStore(dir).loadAll().single()
        assertEquals("https://cdn.test/v/master.m3u8", back.masterUrl)
        assertEquals("https://player.test/embed/7", back.frame)
        assertEquals(listOf(player, site, emptyMap<String, String>()), back.headerSets)
        assertEquals("fp-1", back.fp)
        assertEquals(1_700_000_000_000L, back.madeAt); assertEquals(1_700_000_600_000L, back.expiresAt)
        assertEquals(player, back.headers)
    }

    @Test fun aRecordFromBeforeThoseFieldsExistedLoadsWithoutThem() {
        val back = JsonStreamStore.decode(mapOf("id" to 1_000_000_002L, "state" to DlState.PAUSED_USER.code, "url" to "https://cdn.test/v/a.m3u8"))!!
        assertNull(back.masterUrl); assertNull(back.frame); assertNull(back.fp)
        assertTrue(back.headerSets.isEmpty())
        assertEquals(0L, back.madeAt); assertEquals(0L, back.expiresAt)
    }

    @Test fun theLadderIsCappedAndOddEntriesAreLeftOut() {
        val many = (1..12).map { mapOf("Referer" to "https://h$it.test/") }
        val m = LinkedHashMap<String, Any?>(JsonStreamStore.encode(StreamRec(1_000_000_003L).also { it.url = "https://cdn.test/v/b.m3u8"; it.state = DlState.QUEUED }))
        m["headerSets"] = many.map { LinkedHashMap<String, Any?>(it) } + listOf("not a map", 7L, mapOf("Referer" to 5L))
        val back = JsonStreamStore.decode(m)!!
        assertEquals("at most eight sets are kept", 8, back.headerSets.size)
        assertEquals(many.take(8), back.headerSets)
    }
}
