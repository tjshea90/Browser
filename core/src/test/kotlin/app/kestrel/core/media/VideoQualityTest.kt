package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoQualityTest {
    private val policy = "Video: codecs offered to sites: automatic"

    @Test fun nothingYetSaysSoAndKeepsThePolicy() {
        val l = VideoQuality().line(policy)
        assertTrue(l, l.startsWith("Real video: nothing played yet") && l.endsWith(policy))
    }

    @Test fun lookAddUpIntoOneLine() {
        val q = VideoQuality()
        q.add("a.example", 1500, 30, 0, 60, 1280, 720)   // 2 %
        q.add("a.example", 1500, 150, 0, 60, 1280, 720)  // 10 %
        q.add("b.example", 750, 0, 0, 30, 640, 360)
        val l = q.line(policy)
        assertTrue(l, l.contains("3 looks at 2 site(s)"))
        assertTrue(l, l.contains("3 min watched"))     // 150 s = 2.5 min, rounded to the nearest minute
        assertTrue(l, l.contains("3750 frames, dropped 4.8 %")) // 180 of 3750
        assertTrue(l, l.contains("(worst: a.example 10 % at 1280x720)"))
        assertTrue(l, l.endsWith(policy))
    }

    @Test fun aShortLookIsNeverTheWorst() {
        val q = VideoQuality()
        q.add("short.example", 20, 20, 0, 1, 640, 360)   // 100 % of 20 frames means nothing
        assertTrue(q.line(policy), !q.line(policy).contains("worst:"))
    }

    @Test fun nonsenseNumbersAreIgnored() {
        val q = VideoQuality()
        q.add("", 100, 1, 0, 5, 1, 1)
        q.add("x.example", -1, 0, 0, 5, 1, 1)
        q.add("x.example", 100, 500, 0, 5, 1, 1)         // more dropped than shown
        q.add("x.example", 999_999, 0, 0, 5, 1, 1)
        assertTrue(q.line(policy).startsWith("Real video: nothing played yet"))
    }

    @Test fun onlyTheNewestTwentySitesAreKept() {
        val q = VideoQuality()
        for (i in 1..30) q.add("s$i.example", 100, 1, 0, 5, 640, 360)
        assertTrue(q.line(policy), q.line(policy).contains("at 20 site(s)"))
    }

    @Test fun corruptFramesAreMentionedOnlyWhenThereAre() {
        val q = VideoQuality()
        q.add("a.example", 300, 0, 0, 12, 640, 360)
        assertTrue(!q.line(policy).contains("corrupt"))
        q.add("a.example", 300, 0, 4, 12, 640, 360)
        assertTrue(q.line(policy).contains("corrupt 4"))
    }

    @Test fun clearForgetsEverything() {
        val q = VideoQuality(); q.add("a.example", 300, 3, 0, 12, 640, 360); q.clear()
        assertEquals(true, q.line(policy).startsWith("Real video: nothing played yet"))
    }
}
