package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RX3-V5 (research/REVIEW-EXTERNAL-3.md part V, research/FINDER_LAB.md section 3): which segments of a stream playlist are advertising, found BEFORE anything is downloaded. */
class AdRunsTest {
    private var seq = 0L
    private fun seg(url: String, ms: Long = 6_000, disc: Boolean = false, ad: Boolean = false) = Hls.Seg(url, ms, -1, -1, -1, seq++, disc, ad)
    private fun media(segs: List<Hls.Seg>) = Hls.Media(segs, emptyList(), null, true, 6_000, 0, null)
    private fun film(n: Int, host: String = "cdn.example.com", dir: String = "/v/abc/720p", startDisc: Boolean = false) =
        (0 until n).map { seg("https://$host$dir/seg$it.ts", disc = startDisc && it == 0) }
    private fun ad(n: Int, host: String, dir: String = "/ads/xyz", markers: Boolean = false) =
        (0 until n).map { seg("https://$host$dir/a$it.ts", disc = it == 0, ad = markers) }

    @Test fun aPlaylistWithNothingSpecialHasNoAdvertising() {
        assertTrue(AdRuns.find(media(film(100))).ranges.isEmpty())
    }

    @Test fun aMarkedBreakInTheMiddleIsFoundWithoutAnyHostOrDiscontinuity() {
        val segs = film(50) + (0 until 5).map { seg("https://cdn.example.com/v/abc/720p/ad$it.ts", ad = true) } + film(50)
        val r = AdRuns.find(media(segs))
        assertEquals(listOf(50..54), r.ranges); assertEquals(30_000L, r.skippedMs); assertEquals(1, r.breaks)
    }

    @Test fun aRunFromAnotherSiteBetweenDiscontinuitiesIsAdvertising() {
        val segs = film(60) + ad(5, "ads.adserver.net") + film(60, startDisc = true)
        val r = AdRuns.find(media(segs))
        assertEquals(listOf(60..64), r.ranges)
    }

    @Test fun aPreRollFromAnotherSiteIsFoundToo() {
        val segs = ad(4, "ads.adserver.net") + film(100, startDisc = true)
        assertEquals(listOf(0..3), AdRuns.find(media(segs)).ranges)
    }

    @Test fun anotherDirectoryOnTheSameSiteIsAnotherSourceWhenTheRunIsShort() {
        val segs = film(60) + (0 until 5).map { seg("https://cdn.example.com/promo/x7/p$it.ts", disc = it == 0) } + film(60, startDisc = true)
        assertEquals(listOf(60..64), AdRuns.find(media(segs)).ranges)
    }

    @Test fun aSecondHostOfTheSameRegistrableDomainIsTheSameSource() {
        val segs = film(50, host = "cdn1.example.com") + film(5, host = "cdn2.example.com", startDisc = true) + film(50, host = "cdn1.example.com", startDisc = true)
        assertTrue(AdRuns.find(media(segs)).ranges.isEmpty())
    }

    @Test fun aTwoPartFilmWhereNoRunHoldsMostOfTheTimeStaysWhole() {
        val segs = film(60, host = "cdn.example.com") + film(50, host = "other.example.org", startDisc = true)
        assertTrue(AdRuns.find(media(segs)).ranges.isEmpty())                       // 55 % / 45 %: nothing dominates
    }

    @Test fun aLongForeignRunIsNeverCalledAdvertising() {
        val segs = film(100) + ad(30, "other.example.org").map { seg(it.url, 6_000, it.discontinuity) }       // 180 s from another site: longer than any ad
        assertTrue(AdRuns.find(media(segs)).ranges.isEmpty())
    }

    @Test fun theLongestRunIsNeverMarkedAndAShortFilmDoesNotLoseItsHalf() {
        val segs = ad(5, "ads.adserver.net") + film(8, startDisc = true)          // 30 s foreign + 48 s film: the film is not 3 times the foreign run
        assertTrue(AdRuns.find(media(segs)).ranges.isEmpty())
    }

    @Test fun markersThatCoverMoreThanHalfOfThePlaylistAreTakenForAMistake() {
        val segs = (0 until 30).map { seg("https://cdn.example.com/v/s$it.ts", ad = it >= 5) }
        assertTrue(AdRuns.find(media(segs)).ranges.isEmpty())
    }

    @Test fun markersAndAForeignRunTogetherCountEachSegmentOnce() {
        val segs = film(60) + ad(5, "ads.adserver.net", markers = true) + film(60, startDisc = true)
        val r = AdRuns.find(media(segs))
        assertEquals(listOf(60..64), r.ranges); assertEquals(30_000L, r.skippedMs)
    }

    @Test fun twoBreaksAreTwoRangesAndTheNoteCountsThem() {
        val segs = film(40) + ad(3, "ads.adserver.net") + film(40, startDisc = true) + ad(2, "ads.adserver.net") + film(40, startDisc = true)
        val r = AdRuns.find(media(segs))
        assertEquals(listOf(40..42, 83..84), r.ranges); assertEquals(2, r.breaks); assertEquals(30_000L, r.skippedMs)
        assertEquals("Left out 0:30 of advertising (2 breaks)", AdRuns.note(r))
        assertEquals("Left out 1:30 of advertising (1 break)", AdRuns.note(AdRuns.Result(listOf(0..14), 90_000L)))
        assertEquals(null, AdRuns.note(AdRuns.NONE))
    }

    @Test fun neverEverythingAndNeverAnEmptyOrTinyPlaylist() {
        assertTrue(AdRuns.find(media(emptyList())).ranges.isEmpty())
        assertTrue(AdRuns.find(media(film(1))).ranges.isEmpty())
        assertTrue(AdRuns.find(media(ad(10, "ads.adserver.net", markers = true))).ranges.isEmpty())   // all of it marked: no film left, so the marks are not believed
    }

    @Test fun theDownloadSeesTheFirstSegmentAfterABreakAsANewRun() {
        val segs = film(50) + (0 until 5).map { seg("https://cdn.example.com/v/abc/720p/ad$it.ts", ad = true) } + film(50)
        val m = media(segs)
        val eff = AdRuns.withBreaks(m, AdRuns.find(m))
        assertTrue(eff.segments[55].discontinuity)                                    // the writer starts a new run where the film comes back
        assertTrue(!eff.segments[56].discontinuity)
        assertEquals(m.segments.size, eff.segments.size)
        assertEquals(m.durationMs, eff.durationMs)
    }
}
