package app.kestrel.core.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** IMG1 (owner 2026-11-21): the facts about a picked file say whether the file or the page is to blame, and carry no name. */
class FilePickFactsTest {
    private fun p(type: String = "image/png", bytes: Long = 830_000, w: Int = 600, h: Int = 973, ext: Boolean = true, free: Long = 300) = FilePickFacts.Picked(type, bytes, w, h, ext, free)

    @Test fun anOrdinaryScreenshotIsDescribedAsOneAndBlamesNoOne() {
        assertEquals("type=image/png kb=811 size=600x973 decodes=1 ext=1 freeMb=300", FilePickFacts.logLine(p()))
        assertTrue(FilePickFacts.verdict(p()), FilePickFacts.verdict(p()).startsWith("an ordinary picture (image/png, 600x973, 811 KB)"))
    }

    @Test fun anEmptyFileAFullCacheAndAPictureWithoutAReadableHeaderAreEachNamed() {
        assertTrue(FilePickFacts.verdict(p(bytes = 0, w = 0, h = 0)).contains("EMPTY file"))
        assertTrue(FilePickFacts.verdict(p(free = 12)).contains("only 12 MB free"))
        assertTrue(FilePickFacts.verdict(p(w = 0, h = 0)).contains("cannot read this picture's header"))
        assertTrue(FilePickFacts.verdict(p(ext = false)).contains("no file extension"))
    }

    @Test fun theLineCarriesOnlyNumbersAndATypeNeverAPathOrAName() {
        val l = FilePickFacts.logLine(p(type = "image/png; name=/storage/emulated/0/Pictures/Secret Name.png"))
        assertEquals("type=odd kb=811 size=600x973 decodes=1 ext=1 freeMb=300", l)
        assertFalse(l, l.contains("Secret") || l.contains("Pictures"))
        assertTrue(FilePickFacts.logLine(p(type = "")).startsWith("type=none "))
        assertTrue(FilePickFacts.logLine(p(type = "image/svg+xml")).startsWith("type=image/svg+xml "))
        assertTrue(FilePickFacts.logLine(p(bytes = -1)).contains("kb=-1"))
    }

    private fun g(type: String = "image/png", kb: Long = 119, mg: String = "png", dec: Int = 1, w: Int = 600, h: Int = 1024, px: Int = 1, enc: Int = 1, ek: Long = 40, ms: Long = 55) = FilePickFacts.PageGot(type, kb, mg, dec, w, h, px, enc, ek, ms)

    /** PERF10c (owner 2026-11-22: "I can't attach photos to claude.ai"): what the PAGE received says which side is at fault. */
    @Test fun whatThePageReceivedNamesTheSideAtFault() {
        assertEquals("host=claude.ai n=1 type=image/png kb=119 bytes=png decode=ok 600x1024 canvas=ok reencode=ok(40kb) 55ms", FilePickFacts.pageLine("claude.ai", 1, listOf(g())))
        assertTrue(FilePickFacts.pageVerdict(g()).contains("claude.ai or its server refused it"))
        assertTrue(FilePickFacts.pageVerdict(g(kb = 0, dec = 0, px = 0, enc = 0)).contains("EMPTY"))
        assertTrue(FilePickFacts.pageVerdict(g(dec = 2, w = 0, h = 0, px = 0, enc = 0)).contains("could NOT decode"))
        assertTrue(FilePickFacts.pageVerdict(g(px = 4, enc = 0)).contains("REFUSED"))
        assertTrue(FilePickFacts.pageVerdict(g(px = 3)).contains("one flat colour"))
        assertTrue(FilePickFacts.pageVerdict(g(px = 2)).contains("all transparent"))
        assertTrue(FilePickFacts.pageVerdict(g(enc = 2, ek = 0)).contains("could not re-encode"))
        assertTrue(FilePickFacts.pageVerdict(g(type = "image/heic", mg = "heic", dec = 2)).contains("heic picture"))
        assertTrue(FilePickFacts.pageVerdict(g(type = "application/pdf", mg = "pdf", dec = 0, px = 0, enc = 0, w = 0, h = 0)).contains("not a picture"))
        assertTrue(FilePickFacts.pageVerdict(g(dec = 3, px = 0, enc = 0)).contains("took over 6 s"))
    }

    @Test fun thePageLineCarriesOnlyFixedWordsAndNumbers() {
        val l = FilePickFacts.pageLine("claude.ai", 5, listOf(g(type = "image/png; name=/storage/Secret Name.png", mg = "Secret"), g(), g(), g()))
        assertFalse(l, l.contains("Secret") || l.contains("storage"))
        assertTrue(l, l.contains("type=odd") && l.contains("bytes=other") && l.startsWith("host=claude.ai n=5 "))
        assertEquals("at most 3 files are told", 3, l.split(" ; ").size)
    }
}
