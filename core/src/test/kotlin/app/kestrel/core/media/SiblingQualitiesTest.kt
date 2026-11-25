package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** PERF5b (owner 2026-11-22): pornhub gives one single-picture list per quality; they are the same film in other qualities, and only those. */
class SiblingQualitiesTest {
    private fun k(u: String, ms: Long, vararg s: Pair<Int, Int>, seen: Long = 0L) = SiblingQualities.Known(u, ms, s.toList(), seen)

    @Test fun threeSinglePictureListsOfTheSameLengthAreOneFilmInThreeQualitiesBiggestFirst() {
        val p720 = k("https://c.example/720.m3u8", 1_139_000, 1280 to 720, seen = 3)
        val alts = SiblingQualities.alternates(p720, listOf(p720, k("https://c.example/240.m3u8", 1_139_400, 426 to 240, seen = 1), k("https://c.example/480.m3u8", 1_138_200, 854 to 480, seen = 2)))
        assertEquals(listOf(720, 480, 240), alts.map { it.h })
        assertEquals("https://c.example/480.m3u8", alts[1].url)
    }

    @Test fun aListOfAnotherLengthOrWithSeveralPicturesOrTheSameSizeIsNoSibling() {
        val c = k("https://c.example/720.m3u8", 1_139_000, 1280 to 720)
        val alts = SiblingQualities.alternates(c, listOf(
            k("https://c.example/ad.m3u8", 30_000, 640 to 360),                                  // an advertisement: another length
            k("https://c.example/abr.m3u8", 1_139_000, 1280 to 720, 854 to 480),                  // an ABR list chooses by itself
            k("https://c.example/same.m3u8", 1_139_000, 1280 to 720),                            // the same size again
            k("https://c.example/360.m3u8", 1_139_500, 640 to 360)))
        assertEquals(listOf(720, 360), alts.map { it.h })
    }

    @Test fun nothingToChooseFromGivesNoList() {
        val c = k("https://c.example/720.m3u8", 1_139_000, 1280 to 720)
        assertEquals(0, SiblingQualities.alternates(c, listOf(c)).size)
        assertEquals("a film with several pictures is its own ABR list", 0, SiblingQualities.alternates(k("https://c.example/m.m3u8", 1_139_000, 1280 to 720, 854 to 480), listOf(c)).size)
        assertEquals("an unknown length is never matched", 0, SiblingQualities.alternates(k("https://c.example/u.m3u8", 0, 1280 to 720), listOf(k("https://c.example/v.m3u8", 0, 640 to 360))).size)
    }

    @Test fun theStartingQualityIsTheBiggestThatFitsTheSmoothSizeElseTheSmallest() {
        val alts = listOf(SiblingQualities.Alt("u1080", 1920, 1080), SiblingQualities.Alt("u720", 1280, 720), SiblingQualities.Alt("u480", 854, 480), SiblingQualities.Alt("u240", 426, 240))
        assertEquals("the full-screen screen starts at 720p", "u720", SiblingQualities.startFor(alts, 720)?.url)
        assertEquals("a small box starts at 480p", "u480", SiblingQualities.startFor(alts, 480)?.url)
        assertEquals("a remembered pick of 1080p is honoured", "u1080", SiblingQualities.startFor(alts, 1080)?.url)
        assertEquals("nothing fits: the smallest", "u240", SiblingQualities.startFor(alts.filter { it.h >= 480 }.map { SiblingQualities.Alt(it.url, it.w, it.h) } + SiblingQualities.Alt("u240", 426, 240), 100)?.url)
        assertEquals("a portrait picture is judged by its shorter side", "p", SiblingQualities.startFor(listOf(SiblingQualities.Alt("p", 720, 1280), SiblingQualities.Alt("q", 1080, 1920)), 720)?.url)
        assertEquals("one list or none: nothing to choose", null, SiblingQualities.startFor(alts.take(1), 720))
        assertEquals(null, SiblingQualities.startFor(emptyList(), 720))
    }

    @Test fun theLinesRoundTripAndAHostileLineIsDropped() {
        val alts = listOf(SiblingQualities.Alt("https://c.example/a.m3u8?x=1|2", 1280, 720), SiblingQualities.Alt("https://c.example/b.m3u8", 426, 240))
        val back = SiblingQualities.decode(SiblingQualities.encode(alts))
        assertEquals(listOf(720, 240), back.map { it.h }); assertEquals("https://c.example/a.m3u8?x=1|2", back[0].url)
        val bad = SiblingQualities.decode("0|720|https://x.example/a\n1280|720|file:///data/x\n1280|720|javascript:alert(1)\nxx|1|https://x.example/\n640|360|https://ok.example/z.m3u8")
        assertEquals(1, bad.size); assertTrue(bad[0].url.startsWith("https://ok.example"))
    }
}
