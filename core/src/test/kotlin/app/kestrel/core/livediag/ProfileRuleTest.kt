package app.kestrel.core.livediag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRuleTest {
    private fun why(privateTab: Boolean = false, privateOpen: Boolean = false, url: String = "https://claude.ai/chat/abc", diag: Boolean = true, token: Boolean = true, rec: Boolean = false) =
        ProfileRule.whyNot(privateTab, privateOpen, url, diag, token, rec)

    @Test fun anOrdinaryPageOfANormalTabMayBeRecordedWhenTheOwnersSwitchAndTokenAreThere() {
        assertEquals("", why())
        assertEquals("", why(url = "http://example.org/"))
    }

    @Test fun neverWithoutTheSwitchAndTokenNeverInOrNextToAPrivateTabNeverTwiceAtOnce() {
        assertTrue(why(diag = false).startsWith("Turn on Live diagnostics")); assertTrue(why(token = false).startsWith("Turn on Live diagnostics"))
        assertTrue(why(privateTab = true).startsWith("Not in a private tab"))
        assertTrue("a private tab elsewhere is in the same page process", why(privateOpen = true).startsWith("Close the private tabs first"))
        assertTrue(why(rec = true).startsWith("A recording is already running"))
    }

    @Test fun neverForSensitiveLocalOrNonWebPages() {
        for (u in listOf("https://www.chase.com/", "https://mail.example.org/inbox", "https://accounts.example.org/login", "https://example.org/checkout/pay", "https://portal.agency.gov/"))
            assertTrue(u, why(url = u).startsWith("Not for sign-in, bank, mail"))
        for (u in listOf("http://127.0.0.1:8002/x", "http://192.168.1.1/", "http://localhost:3000/", "https://nas.local/", "http://router/")) assertTrue(u, why(url = u).startsWith("Not for a page on this tablet"))
        assertTrue(why(url = "about:blank").startsWith("Open the page"))
        assertTrue(why(url = "").startsWith("Open the page")); assertTrue(why(url = "file:///sdcard/a.html").startsWith("Open the page"))
    }

    @Test fun theNameIsNewEachTimeAndTheCompanionSitsNextToIt() {
        val p = ProfileRule.path("2026-11-27T05:17:42Z", "852b0843cb06")
        assertEquals("profiles/2026-11-27/051742-852b0843cb06.json.gz", p)
        assertEquals("profiles/2026-11-27/051742-852b0843cb06-2.json.gz", ProfileRule.path("2026-11-27T05:17:42Z", "852b0843cb06", 1))
        assertEquals("profiles/2026-11-27/051742-852b0843cb06.meta.json", ProfileRule.metaPath(p))
        assertEquals("a hostile install id cannot climb out of the folder", "profiles/undated/000000-evilabc.json.gz", ProfileRule.path("", "../evil/abc"))
    }

    @Test fun theRecordingsAreBoundedAndSampleOnlyScriptsAndProcessorTime() {
        assertTrue(ProfileRule.LOAD_MAX_MS > ProfileRule.NOW_MS && ProfileRule.LOAD_TAIL_MS < ProfileRule.LOAD_MAX_MS)
        assertTrue("the memory release comes before the end of a load recording and after its first seconds", ProfileRule.RELEASE_BEFORE_END_MS in 1_000L..(ProfileRule.LOAD_MAX_MS / 2))
        assertEquals(listOf("GeckoMain"), ProfileRule.THREADS.toList())
        assertEquals(listOf("js", "cpu"), ProfileRule.FEATURES.toList())
        assertTrue("no wildcard thread filter: only the named threads are sampled", ProfileRule.THREADS.none { it.contains("*") } && "GeckoMain" in ProfileRule.THREADS)
    }

    private fun gz(text: String): ByteArray = java.io.ByteArrayOutputStream().also { o -> java.util.zip.GZIPOutputStream(o).use { it.write(text.toByteArray(Charsets.US_ASCII)) } }.toByteArray()

    @Test fun aRecordingThatSaysTheChildProcessFailedIsRecognisedWhereverTheSentenceSits() {
        val sentence = "Could not generate profile from pid 13909, failure: OOM in ChunkedJSONWriteFunc::AllocChunk allocating new chunk"
        assertTrue("at the start", ProfileRule.pageProcessMissing(gz(sentence + " " + "x".repeat(1000))))
        assertTrue("at the very end", ProfileRule.pageProcessMissing(gz("y".repeat(5000) + sentence)))
        for (pad in listOf(65_500, 65_520, 65_536, 65_545, 131_040, 200_003)) assertTrue("across a 64 KB block boundary (pad $pad)", ProfileRule.pageProcessMissing(gz("z".repeat(pad) + sentence + "tail")))
        assertFalse("a clean recording", ProfileRule.pageProcessMissing(gz("{\"meta\":{},\"profileGatheringLog\":{\"events\":[\"Got profile from pid 1, with size: 231935\"]}}" + "q".repeat(300_000))))
        assertFalse("not gzip: unknown, not missing", ProfileRule.pageProcessMissing("plain text, not a gzip file".toByteArray()))
        assertFalse("empty", ProfileRule.pageProcessMissing(ByteArray(0)))
    }
}
