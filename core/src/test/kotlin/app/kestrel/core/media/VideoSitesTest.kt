package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** VD1 (owner 2026-11-21): which SITE had its videos held back, refused or failed: the part the summed counters could not say. */
class VideoSitesTest {
    @Test fun aSiteThatWasHeldBackIsNamedWithTheNumbersOfTheRuleThatDidIt() {
        val v = VideoSites()
        assertTrue(v.isEmpty()); assertTrue(v.line(), v.line().contains("no site had a video held back"))
        v.gate("www.instagram.example", 0, 0, 6, 14); v.gate("www.instagram.example", 2, 1, 0, 3)
        assertFalse(v.isEmpty())
        val l = v.line()
        assertTrue(l, l.contains("www.instagram.example: press-to-play rule held 2 (markup), kept 6 script players from fetching, refused 17 plays, opened by a press 1"))
    }

    @Test fun theFormatAnswersAndTheErrorsAreSaidPerSiteAndTheMostAffectedComesFirst() {
        val v = VideoSites()
        v.gate("quiet.example", 1, 0, 0, 0)
        v.codec("sixty.example", 0, 0, 4, "avc 60fps"); v.error("sixty.example", 4, "blob"); v.error("sixty.example", 4, "blob"); v.error("sixty.example", 2, "http")
        val l = v.line()
        assertTrue(l, l.indexOf("sixty.example") < l.indexOf("quiet.example"))
        assertTrue(l, l.contains("format answered no: size 0, H.264-only 0, 30 fps 4 (last avc 60fps)"))
        assertTrue(l, l.contains("errors code 4 blob x2, code 2 http x1"))
    }

    @Test fun theTableIsBoundedAndDropsTheSiteTouchedLongestAgo() {
        val v = VideoSites(max = 3)
        for (h in listOf("a.example", "b.example", "c.example")) v.gate(h, 1, 0, 0, 0)
        v.gate("a.example", 1, 0, 0, 0)                 // a is fresh again
        v.gate("d.example", 1, 0, 0, 0)                 // b goes
        val l = v.line()
        assertFalse(l, l.contains("b.example")); assertTrue(l, l.contains("a.example") && l.contains("c.example") && l.contains("d.example"))
    }

    @Test fun aSiteOnlyOpenedByAPressIsNotAProblem() {
        val v = VideoSites(); v.gate("fine.example", 0, 3, 0, 0)
        assertTrue(v.line(), v.line().contains("no site had a video held back")); assertFalse(v.isEmpty())
    }

    @Test fun anElementStuckLoadingWithNoErrorIsSaidInWords() {
        val v = VideoSites()
        v.error("spin.example", 99, "blob"); v.error("spin.example", 99, "blob")
        val l = v.line()
        assertTrue(l, l.contains("spin.example: errors stuck loading (no error, no data) blob x2"))
    }
}
