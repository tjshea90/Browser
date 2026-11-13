package app.kestrel.browser.download

import app.kestrel.core.download.DlState
import app.kestrel.core.media.StreamRec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The browser's own file form of a stream download record (EY-8): what recovering a refused link needs survives a restart, and a record written by an older build still loads. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class StreamStoreFilesTest {
    @get:Rule val tmp = TemporaryFolder()

    private val player = mapOf("Referer" to "https://player.test/", "Origin" to "https://player.test")
    private val site = mapOf("Referer" to "https://site.test/")

    @Test fun theRecoveryDataSurvivesASaveAndALoad() {
        val store = StreamStoreFiles(tmp.newFolder("streams"), tmp.newFolder("private"))
        val r = StreamRec(1_000_000_001L)
        r.state = DlState.PAUSED_SYSTEM; r.url = "https://cdn.test/v/720.m3u8"; r.headers = player
        r.masterUrl = "https://cdn.test/v/master.m3u8"; r.frame = "https://player.test/embed/7"
        r.headerSets = listOf(player, site, emptyMap())
        r.fp = "fp-1"; r.madeAt = 1_700_000_000_000L; r.expiresAt = 1_700_000_600_000L
        store.save(r)
        val back = store.loadAll().single()
        assertEquals("https://cdn.test/v/master.m3u8", back.masterUrl)
        assertEquals("https://player.test/embed/7", back.frame)
        assertEquals(listOf(player, site, emptyMap<String, String>()), back.headerSets)
        assertEquals("fp-1", back.fp)
        assertEquals(1_700_000_000_000L, back.madeAt); assertEquals(1_700_000_600_000L, back.expiresAt)
        assertEquals(player, back.headers)
    }

    @Test fun aRecordWrittenBeforeThoseFieldsExistedStillLoads() {
        val root = tmp.newFolder("streams")
        File(root, "1000000002.json").writeText("""{"id":1000000002,"state":${DlState.PAUSED_USER.code},"url":"https://cdn.test/v/a.m3u8","headers":{"Referer":"https://site.test/"}}""")
        val back = StreamStoreFiles(root, tmp.newFolder("private")).loadAll().single()
        assertEquals("https://cdn.test/v/a.m3u8", back.url)
        assertNull(back.masterUrl); assertNull(back.frame); assertNull(back.fp)
        assertTrue(back.headerSets.isEmpty())
        assertEquals(0L, back.madeAt); assertEquals(0L, back.expiresAt)
        assertEquals(site, back.headers)
    }
}
