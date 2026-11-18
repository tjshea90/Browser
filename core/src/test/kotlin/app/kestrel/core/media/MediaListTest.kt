package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaListTest {
    private fun file(url: String, len: Long, mime: String = "video/mp4") = MediaCandidate(url, MediaKind.FILE, mime, len, "https://site.test/watch")
    private fun hls(url: String) = MediaCandidate(url, MediaKind.HLS, "application/vnd.apple.mpegurl", -1, "https://site.test/watch")

    @Test fun aRepeatedSightingKeepsTheFirstTimeAndLearnsTheLastWithoutChangingTheList() {
        // ANS55: a player that moves on to another video asks for that video's master list last; the same master asked for again is the newest news
        val l = MediaList()
        fun seenAt(url: String, at: Long) = MediaCandidate(url, MediaKind.HLS, "application/vnd.apple.mpegurl", -1, "https://site.test/watch", "", at)
        assertTrue(l.add(seenAt("https://c.test/a/master.m3u8", 100)))
        assertTrue(l.add(seenAt("https://c.test/b/master.m3u8", 200)))
        assertFalse("nothing in the list changed", l.add(seenAt("https://c.test/a/master.m3u8", 300)))
        val a = l.usable().first { it.url.contains("/a/") }
        assertEquals(100L, a.seenAt); assertEquals(300L, a.lastSeenAt)
        assertEquals(200L, l.usable().first { it.url.contains("/b/") }.lastSeenAt)
        assertFalse("not read as a master until the early look says so", a.master)
    }

    @Test fun theSameUrlIsOneEntryAndLearnsItsSize() {
        val l = MediaList()
        assertTrue(l.add(file("https://c.test/a.mp4", -1)))
        assertFalse(l.add(file("https://c.test/a.mp4", -1)))            // a second Range request: nothing new
        assertTrue(l.add(file("https://c.test/a.mp4", 5_000_000)))      // now the total is known
        assertFalse(l.add(file("https://c.test/a.mp4#t=5", 5_000_000))) // fragments do not make a new entry
        assertEquals(1, l.size)
        assertEquals(5_000_000L, l.usable()[0].length)
    }

    @Test fun tinyFilesAreLeftOutAndSmallOnesNextToAStreamToo() {
        val l = MediaList()
        l.add(file("https://c.test/click.mp3", 20_000, "audio/mpeg"))
        l.add(file("https://c.test/ad.mp4", 2_000_000))
        l.add(file("https://c.test/main.mp4", 80_000_000))
        assertEquals(listOf("https://c.test/ad.mp4", "https://c.test/main.mp4").sorted(), l.usable().map { it.url }.sorted())
        l.add(hls("https://c.test/master.m3u8"))
        assertEquals(listOf("https://c.test/master.m3u8", "https://c.test/main.mp4"), l.usable().map { it.url }) // the ad clip is gone, the stream comes first
    }

    @Test fun biggestPictureFirstAudioLast() {
        val l = MediaList()
        l.add(file("https://c.test/clip_480p.mp4", 10_000_000))
        l.add(file("https://c.test/clip_1080p.mp4", 40_000_000))
        l.add(file("https://c.test/clip_720p.mp4", 20_000_000))
        l.add(file("https://c.test/clip.m4a", 3_000_000, "audio/mp4"))
        assertEquals(listOf("clip_1080p.mp4", "clip_720p.mp4", "clip_480p.mp4", "clip.m4a"), l.usable().map { it.url.substringAfterLast('/') })
    }

    @Test fun thePagesOwnInfoNamesTheResolution() {
        val l = MediaList()
        l.add(file("https://c.test/v/abc123", 10_000_000))
        assertEquals(0, l.usable()[0].knownHeight)
        assertTrue(l.applyInfo("https://c.test/v/abc123", 1280, 720, 61_000, null))
        val c = l.usable()[0]
        assertEquals(720, c.knownHeight); assertEquals(61_000L, c.durationMs)
        assertFalse(l.applyInfo("https://c.test/v/abc123", 1280, 720, 61_000, null))        // nothing changed
        assertFalse(l.applyInfo("https://c.test/other", 640, 360, 1, null))                  // an unknown URL
        assertTrue(l.applyInfo("https://c.test/v/abc123", 0, 0, 0, "HD 1080"))
        assertEquals(720, c.knownHeight)                                                      // the player's real size wins over the label
    }

    @Test fun labelsGiveAResolution() {
        assertEquals(720, MediaCandidate.heightOfLabel("720p"))
        assertEquals(1080, MediaCandidate.heightOfLabel("HD 1080"))
        assertEquals(0, MediaCandidate.heightOfLabel("HD"))
        assertEquals(0, MediaCandidate.heightOfLabel(null))
        assertEquals(0, MediaCandidate.heightOfLabel("99999"))
    }

    @Test fun theListIsCapped() {
        val l = MediaList(max = 5)
        for (i in 1..9) l.add(file("https://c.test/v$i.mp4", 9_000_000))
        assertEquals(5, l.size)
        assertEquals(listOf("v9.mp4", "v8.mp4", "v7.mp4", "v6.mp4", "v5.mp4"), l.usable().map { it.url.substringAfterLast('/') }) // the oldest went, newest first
    }

    @Test fun wordsNextToTheButton() {
        assertEquals("1080p", MediaLabels.quality(1080))
        assertEquals("4K (2160p)", MediaLabels.quality(2160))
        assertEquals("", MediaLabels.quality(0))
        assertEquals("size unknown", MediaLabels.size(-1))
        assertEquals("0:48", MediaLabels.duration(48_000))
        assertEquals("1:02:03", MediaLabels.duration(3_723_000))
        assertEquals("", MediaLabels.duration(0))
        assertEquals("MP4", MediaLabels.format(file("https://c.test/a.mp4", 1)))
        assertEquals("HLS stream", MediaLabels.format(hls("https://c.test/a.m3u8")))
        assertEquals("My clip - 720p", MediaLabels.fileStem("  My   clip ", "site.test", "720p"))
        assertEquals("site.test", MediaLabels.fileStem("", "site.test", ""))
    }

    @Test fun usableCountAlwaysAgreesWithTheList() {
        val l = MediaList()
        assertEquals(0, l.usableCount())
        l.add(file("https://c.test/tiny.mp4", 5_000)) // under 100 KB: not offered
        assertEquals(l.usable().size, l.usableCount())
        l.add(file("https://c.test/main.mp4", 5_000_000))
        assertEquals(1, l.usableCount())
        l.add(file("https://c.test/unknown.mp4", -1))
        assertEquals(l.usable().size, l.usableCount())
        assertEquals(2, l.usableCount())
        l.add(file("https://c.test/unknown.mp4", 9_000_000)) // the size became known: the same entry, a better size
        assertEquals(l.usable().size, l.usableCount())
        l.add(hls("https://c.test/master.m3u8")) // a stream arrives: the 5 MB clip stays (under 4 MB would be dropped next to a stream)
        assertEquals(l.usable().size, l.usableCount())
        l.add(file("https://c.test/clip.mp4", 3_000_000)) // under 4 MB next to a stream: an ad or preview
        assertEquals(l.usable().size, l.usableCount())
        l.clear()
        assertEquals(0, l.usableCount())
    }

    @Test fun labelHeightStillParses() {
        assertEquals(720, MediaCandidate.heightOfLabel("HD 720"))
        assertEquals(1080, MediaCandidate.heightOfLabel("1080p"))
        assertEquals(0, MediaCandidate.heightOfLabel("fast"))
        assertEquals(0, MediaCandidate.heightOfLabel(null))
    }

    // ---- RX3-V1: what the page's own element says it is for. A decoy (hover preview, hero loop, ad) stays in the list for "Other files" but is not counted for the dock badge.

    private fun played(l: MediaList, c: MediaCandidate, role: VideoRole?, durMs: Long = 25_000, h: Int = 720) { l.add(c); l.applyInfo(c.url, h * 16 / 9, h, durMs, null, role) }

    @Test fun aHoverPreviewOrHeroLoopThatReportedItselfIsNotCountedEvenWhenSharpAndLong() {
        val l = MediaList()
        played(l, file("https://c.test/hero.mp4", 6_000_000), VideoRole.DECOR, durMs = 90_000, h = 1080)        // long and sharp: the old exemptions would have counted it
        assertEquals(0, l.usableCount())
        played(l, file("https://c.test/film.mp4", 900_000_000), VideoRole.PLAYER, durMs = 5_400_000, h = 1080)
        assertEquals(1, l.usableCount())
        assertEquals(2, l.size)                                                                                   // nothing is deleted
        assertEquals(listOf("https://c.test/film.mp4", "https://c.test/hero.mp4"), l.usable().map { it.url }.sortedBy { !it.contains("film") }) // the sheet still has it, to tuck under "Other files"
    }

    @Test fun aPreRollAdFlaggedByThePageIsNotCounted() {
        val l = MediaList()
        played(l, file("https://ads.test/pre.mp4", 9_000_000), VideoRole.AD, durMs = 30_000)
        assertEquals(0, l.usableCount())
    }

    @Test fun aDecoyDoesNotGetTheRealVideoBonusButOneWithoutFactsDoes() {
        val l = MediaList()
        played(l, file("https://c.test/loop.mp4", 6_000_000), VideoRole.DECOR)
        played(l, file("https://c.test/old.mp4", 6_000_000), null)                                              // an old extension / a report without facts: the old rule (a player reported it)
        assertEquals(1, l.usableCount())
        assertTrue(l.find("https://c.test/old.mp4")!!.touched); assertFalse(l.find("https://c.test/loop.mp4")!!.touched)
    }

    @Test fun theOwnerShowingInterestCountsADecoyAgain() {
        val l = MediaList()
        played(l, file("https://c.test/loop.mp4", 6_000_000), VideoRole.DECOR)
        assertEquals(0, l.usableCount())
        assertTrue(l.touch("https://c.test/loop.mp4"))                                                           // long-pressed, opened or downloaded
        assertEquals(1, l.usableCount())
    }

    @Test fun theLatestReportWinsSoAPreviewThatTurnsIntoAPlayerCounts() {
        val l = MediaList()
        played(l, file("https://c.test/clip.mp4", 6_000_000), VideoRole.DECOR)
        assertEquals(0, l.usableCount())
        l.applyInfo("https://c.test/clip.mp4", 1280, 720, 25_000, null, VideoRole.PLAYER)                        // unmuted, controls shown: now it is a player
        assertEquals(1, l.usableCount())
    }

    @Test fun aStreamIsAlsoNotCountedWhenItIsDecoration() {
        val l = MediaList()
        l.add(hls("https://c.test/hero.m3u8")); l.applyInfo("https://c.test/hero.m3u8", 1280, 720, 40_000, null, VideoRole.DECOR)
        assertEquals(0, l.usableCount())
        assertEquals(1, l.usable().size)
    }

    @Test fun aSmallDecoyNextToAStreamIsLeftOutOfTheSheetToo() {
        val l = MediaList()
        l.add(hls("https://c.test/master.m3u8"))
        played(l, file("https://c.test/loop.mp4", 2_000_000), VideoRole.DECOR)                                   // the old rule hides small clips next to a stream; "long or sharp" no longer rescues a decoy
        assertEquals(listOf("https://c.test/master.m3u8"), l.usable().map { it.url })
    }

    @Test fun whatTheElementSaidBeforeTheFileWasSeenIsKeptForIt() {            // the page reports on loadedmetadata, the extension on the response: either can come first (seen on the emulator)
        val l = MediaList()
        assertFalse(l.applyInfo("https://c.test/hero.mp4", 1280, 720, 90_000, "HD", VideoRole.DECOR))   // nothing to apply it to yet
        assertFalse(l.applyInfo("https://c.test/film.mp4", 1280, 720, 5_400_000, null, VideoRole.PLAYER))
        l.add(file("https://c.test/hero.mp4", 6_000_000)); l.add(file("https://c.test/film.mp4", 900_000_000))
        assertEquals(1, l.usableCount())
        assertEquals(VideoRole.DECOR, l.find("https://c.test/hero.mp4")!!.role); assertEquals("HD", l.find("https://c.test/hero.mp4")!!.label)
        assertTrue(l.find("https://c.test/film.mp4")!!.touched); assertEquals(720, l.find("https://c.test/film.mp4")!!.height)
    }

    @Test fun keptInfoIsBoundedAndForgottenWithTheList() {
        val l = MediaList()
        for (i in 0 until 100) l.applyInfo("https://c.test/x$i.mp4", 0, 0, 1000, null, VideoRole.DECOR)
        l.add(file("https://c.test/x0.mp4", 6_000_000)); l.add(file("https://c.test/x99.mp4", 6_000_000))
        assertEquals(null, l.find("https://c.test/x0.mp4")!!.role)                  // the oldest were dropped: at most a few dozen are kept
        assertEquals(VideoRole.DECOR, l.find("https://c.test/x99.mp4")!!.role)
        l.applyInfo("https://c.test/y.mp4", 0, 0, 1000, null, VideoRole.DECOR); l.clear()
        l.add(file("https://c.test/y.mp4", 6_000_000))
        assertEquals(null, l.find("https://c.test/y.mp4")!!.role)                   // a new page: what was said about the old page's files is gone
    }

    // ---- RX3-V2: verify before counting. An address found only in the page's text (a link to teaser.mp4, a string in a script, an og tag) was never requested: nobody has seen what it is.

    private fun markup(url: String, len: Long = -1, kind: MediaKind = MediaKind.FILE) = MediaCandidate(url, kind, "", len, "https://site.test/watch").also { it.fromMarkup = true }

    @Test fun anAddressFoundOnlyInThePageTextIsNotCountedUntilItsLengthIsKnownToBeBig() {
        val l = MediaList()
        l.add(markup("https://c.test/teaser.mp4"))
        assertEquals(0, l.usableCount())                                   // the old rule counted unknown length as usable: a page with a link to an animated preview showed the badge
        assertEquals(1, l.usable().size)                                   // still offered under "Other files"
        assertTrue(l.setLength("https://c.test/teaser.mp4", 30_000))       // a one-byte Range request said: 30 KB
        assertEquals(0, l.usableCount())
        assertTrue(l.usable().isEmpty())                                   // under 100 KB it is not even offered
        l.add(markup("https://c.test/film.mp4"))
        assertTrue(l.setLength("https://c.test/film.mp4", 700_000_000))
        assertEquals(1, l.usableCount())
    }

    @Test fun aFileSeenOnTheWireStillCountsWhenItsLengthIsUnknown() {
        val l = MediaList()
        l.add(file("https://c.test/chunked.mp4", -1))                      // a response with a media type and no length (chunked): it was really requested
        assertEquals(1, l.usableCount())
    }

    @Test fun thePlayerOrTheOwnerMakesAMarkupAddressReal() {
        val l = MediaList()
        l.add(markup("https://c.test/a.mp4")); l.add(markup("https://c.test/b.mp4"))
        assertEquals(0, l.usableCount())
        l.applyInfo("https://c.test/a.mp4", 1280, 720, 300_000, null, VideoRole.PLAYER)     // an element that plays it reported it
        assertEquals(1, l.usableCount())
        assertTrue(l.touch("https://c.test/b.mp4"))                                         // the owner long-pressed it
        assertEquals(2, l.usableCount())
    }

    @Test fun aStreamFoundOnlyInTheTextWaitsForProofToo() {
        val l = MediaList()
        l.add(markup("https://c.test/master.m3u8", kind = MediaKind.HLS))
        assertEquals(0, l.usableCount())
        l.add(hls("https://c.test/other.m3u8"))                            // one that a player really asked for counts at once
        assertEquals(1, l.usableCount())
    }

    @Test fun unverifiedAddressesAreListedForTheIdleCheckOnlyWhenNothingElseIsReal() {
        val l = MediaList()
        l.add(markup("https://c.test/a.mp4")); l.add(markup("https://c.test/b.mp4")); l.add(markup("https://c.test/c.mp4")); l.add(markup("https://c.test/d.mp4"))
        assertEquals(listOf("https://c.test/d.mp4", "https://c.test/c.mp4", "https://c.test/b.mp4"), l.unverifiedToCheck(3).map { it.url })   // newest first, at most three
        l.add(file("https://c.test/real.mp4", 80_000_000))
        assertTrue(l.unverifiedToCheck(3).isEmpty())                       // a page that has a verified video needs no checking
    }

    // ---- RX3-V5: the early look at a stream's playlist says whether it can be saved and how long it is

    @Test fun aLiveOrProtectedOrVeryShortStreamIsNotCountedOnceItsPlaylistWasRead() {
        val l = MediaList()
        l.add(hls("https://c.test/live.m3u8")); l.add(hls("https://c.test/drm.m3u8")); l.add(hls("https://c.test/ad.m3u8")); l.add(hls("https://c.test/film.m3u8"))
        assertEquals(4, l.usableCount())                                                                          // nobody has looked yet: as before
        assertTrue(l.applyStream("https://c.test/live.m3u8", live = true, drm = false, durationMs = 0, adMs = 0, breaks = 0))
        l.applyStream("https://c.test/drm.m3u8", live = false, drm = true, durationMs = 3_600_000, adMs = 0, breaks = 0)
        l.applyStream("https://c.test/ad.m3u8", live = false, drm = false, durationMs = 15_000, adMs = 0, breaks = 0)
        l.applyStream("https://c.test/film.m3u8", live = false, drm = false, durationMs = 5_400_000, adMs = 90_000, breaks = 2)
        assertEquals(listOf("https://c.test/film.m3u8"), l.usable().filter { it.counts() }.map { it.url })
        assertEquals(1, l.usableCount())
        assertEquals(90_000L, l.find("https://c.test/film.m3u8")!!.adMs); assertEquals(2, l.find("https://c.test/film.m3u8")!!.adBreaks)
        assertEquals(4, l.size)                                                                                   // nothing is deleted: the sheet still lists them and says why they cannot be saved
    }

    @Test fun aStreamWhosePlaylistWasReadAndIsALongRecordingStillCountsAndAnUnreadOneKeepsCounting() {
        val l = MediaList()
        l.add(hls("https://c.test/a.m3u8")); l.add(hls("https://c.test/b.m3u8"))
        l.applyStream("https://c.test/a.m3u8", live = false, drm = false, durationMs = 20_000, adMs = 0, breaks = 0)    // exactly 20 s: not under the limit
        assertEquals(2, l.usableCount())
        assertFalse(l.applyStream("https://c.test/unknown.m3u8", live = true, drm = false, durationMs = 0, adMs = 0, breaks = 0)) // a stream the list does not hold changes nothing
    }

    @Test fun streamsToLookAtAreTheNewestUnreadOnesAtMostThree() {
        val l = MediaList()
        for (i in 1..5) l.add(hls("https://c.test/s$i.m3u8"))
        assertEquals(listOf("s5", "s4", "s3"), l.streamsToLook(3).map { it.url.substringAfterLast('/').substringBefore('.') })
        l.applyStream("https://c.test/s5.m3u8", live = false, drm = false, durationMs = 60_000, adMs = 0, breaks = 0)
        assertEquals(listOf("s4", "s3", "s2"), l.streamsToLook(3).map { it.url.substringAfterLast('/').substringBefore('.') })      // a looked-at one is not asked again
        l.add(file("https://c.test/film.mp4", 80_000_000))
        assertEquals(3, l.streamsToLook(3).size)                                                                  // files are never looked at here
    }

    // ---- RX3-V3: which element actually plays

    private fun playedInfo(l: MediaList, url: String, element: Int, durMs: Long, area: Long = 120_000, role: VideoRole? = VideoRole.PLAYER) = l.applyInfo(url, 1280, 720, durMs, null, role, played = true, element = element, areaPx = area)

    @Test fun thePlayedFileWithTheLargestPictureThenTheLongestLengthIsThePrimaryOne() {
        val l = MediaList()
        assertEquals(null, l.primary())
        l.add(file("https://c.test/small.mp4", 20_000_000)); l.add(file("https://c.test/big.mp4", 20_000_000)); l.add(file("https://c.test/long.mp4", 20_000_000)); l.add(file("https://c.test/unplayed.mp4", 900_000_000))
        playedInfo(l, "https://c.test/small.mp4", 1, 600_000, area = 20_000)
        playedInfo(l, "https://c.test/big.mp4", 2, 100_000, area = 150_000)
        assertEquals("https://c.test/big.mp4", l.primary()!!.url)                                              // the biggest picture wins, however short
        playedInfo(l, "https://c.test/long.mp4", 3, 900_000, area = 150_000)
        assertEquals("equal pictures: the longer file", "https://c.test/long.mp4", l.primary()!!.url)
        assertTrue("a file nobody played is never primary", l.find("https://c.test/unplayed.mp4")!!.played.not())
    }

    @Test fun aPreRollThatPlayedOnTheSameElementBeforeAFilmThreeTimesLongerIsAnAd() {
        val l = MediaList()
        l.add(file("https://ads.test/pre.mp4", 8_000_000)); l.add(file("https://c.test/film.mp4", 300_000_000))
        playedInfo(l, "https://ads.test/pre.mp4", 1, 15_000)
        assertEquals("until the film plays the clip is just a played file, like the film's own file", 2, l.usableCount())
        playedInfo(l, "https://c.test/film.mp4", 1, 300_000)                                                  // the SAME element, later, a file 20 times longer
        assertEquals(VideoRole.AD, l.find("https://ads.test/pre.mp4")!!.role)
        assertEquals(1, l.usableCount()); assertEquals("https://c.test/film.mp4", l.primary()!!.url)
    }

    @Test fun theDurationJumpNeedsTheSameElementAnEarlierFileUnderNinetySecondsAndThreeTimesTheLength() {
        fun run(first: Long, second: Long, sameElement: Boolean = true): VideoRole? {
            val l = MediaList(); l.add(file("https://c.test/a.mp4", 20_000_000)); l.add(file("https://c.test/b.mp4", 20_000_000))
            playedInfo(l, "https://c.test/a.mp4", 1, first); playedInfo(l, "https://c.test/b.mp4", if (sameElement) 1 else 2, second)
            return l.find("https://c.test/a.mp4")!!.role
        }
        assertEquals(VideoRole.AD, run(30_000, 90_000))
        assertEquals("less than three times", VideoRole.PLAYER, run(60_000, 150_000))
        assertEquals("the earlier file is 90 s or more: two parts of one film", VideoRole.PLAYER, run(120_000, 600_000))
        assertEquals("another element: two different videos", VideoRole.PLAYER, run(15_000, 600_000, sameElement = false))
    }

    @Test fun theOwnerShowingInterestInThePreRollKeepsItEvenAfterTheJump() {
        val l = MediaList()
        l.add(file("https://ads.test/pre.mp4", 8_000_000)); l.add(file("https://c.test/film.mp4", 300_000_000))
        playedInfo(l, "https://ads.test/pre.mp4", 1, 15_000)
        l.touch("https://ads.test/pre.mp4")
        playedInfo(l, "https://c.test/film.mp4", 1, 300_000)
        assertEquals(2, l.usableCount())
    }

    @Test fun aDecoyThatPlaysIsStillADecoyAndNeverPrimary() {
        val l = MediaList()
        l.add(file("https://c.test/hero.mp4", 6_000_000))
        playedInfo(l, "https://c.test/hero.mp4", 1, 90_000, role = VideoRole.DECOR)
        assertEquals(null, l.primary()); assertEquals(0, l.usableCount())
    }

    @Test fun whatTheElementSaidBeforeTheFileWasSeenKeepsItsPlayedFlag() {
        val l = MediaList()
        playedInfo(l, "https://c.test/film.mp4", 4, 300_000, area = 90_000)                                    // the report comes before the extension reported the file
        l.add(file("https://c.test/film.mp4", 300_000_000))
        val c = l.find("https://c.test/film.mp4")!!
        assertTrue(c.played); assertEquals(4, c.element); assertEquals("https://c.test/film.mp4", l.primary()!!.url)
    }

    @Test fun aFullListEvictsDecoysBeforeAnythingElse() {
        val l = MediaList(max = 4)
        played(l, file("https://c.test/film.mp4", 80_000_000), VideoRole.PLAYER, durMs = 3_600_000)
        played(l, file("https://c.test/loop1.mp4", 6_000_000), VideoRole.DECOR)
        played(l, file("https://c.test/loop2.mp4", 6_000_000), VideoRole.DECOR)
        l.add(file("https://c.test/plain.mp4", 8_000_000))
        l.add(file("https://c.test/new.mp4", 9_000_000))                                                         // one too many: a decoy goes, not the film and not the plain file
        val urls = l.keys()
        assertTrue(urls.contains("https://c.test/film.mp4")); assertTrue(urls.contains("https://c.test/plain.mp4")); assertTrue(urls.contains("https://c.test/new.mp4"))
        assertEquals(1, urls.count { it.contains("loop") })
    }
}

class MediaListFramesTest {
    private fun hls(url: String, frame: String = "") = MediaCandidate(url, MediaKind.HLS, "application/vnd.apple.mpegurl", -1, "https://site.test/watch", frame)

    /** An address found only in a page's text: nobody has seen it, it is not counted, so nothing about it is captured. */
    private fun doc(url: String, frame: String = "") = MediaCandidate(url, MediaKind.FILE, "", -1, "https://site.test/watch", frame).also { it.fromMarkup = true }

    @Test fun whatAGoneDocumentOfferedAndWasNeverOfferedToThePersonIsDroppedAndNothingElse() {
        val l = MediaList()
        l.add(doc("https://cdn.test/one.mp4", "https://player-one.test/embed"))     // server one
        l.add(doc("https://cdn.test/side.mp4", "https://sidebar.test/s"))           // another frame, still there
        l.add(doc("https://cdn.test/page.mp4"))                                     // the page's own (frame "")
        assertEquals(listOf("https://cdn.test/one.mp4"), l.dropFrames(listOf("https://player-one.test/embed")))
        assertEquals(setOf("https://cdn.test/side.mp4", "https://cdn.test/page.mp4"), l.keys().toSet())
        assertTrue(l.dropFrames(listOf("https://player-one.test/embed")).isEmpty())  // already gone
        assertTrue(l.dropFrames(listOf("")).isEmpty())                                 // the page itself is never dropped by a frame
        assertEquals(2, l.size)
    }

    // ---- VS2 (owner 2026-11-18, doraby + vidsrc / 111movies): "capture the video source when it is found so it doesn't disappear from the list"
    @Test fun aStreamThatWasOfferedStaysWhenItsPlayerFrameGoesAwayAndComesLastInTheList() {
        val l = MediaList()
        l.add(hls("https://cdn.test/one.m3u8", "https://player-one.test/embed"))
        assertEquals(1, l.usableCount())                                              // offered = captured
        assertTrue("nothing is removed", l.dropFrames(listOf("https://player-one.test/embed")).isEmpty())
        assertEquals(1, l.usableCount())
        assertTrue(l.find("https://cdn.test/one.m3u8")!!.orphan)
        l.add(hls("https://cdn.test/page.m3u8"))                                       // the page's own stream arrives too
        assertEquals("the live one first, the kept one last", listOf("https://cdn.test/page.m3u8", "https://cdn.test/one.m3u8"), l.usable().map { it.url })
    }

    @Test fun aKeptStreamIsReplacedWhenALivePlayerOffersAFilmOfItsOwnButNotByASmallClip() {
        val l = MediaList()
        l.add(hls("https://cdn.test/one.m3u8", "https://player-one.test/embed")); l.usableCount()
        l.dropFrames(listOf("https://player-one.test/embed"))
        l.add(MediaCandidate("https://ads.test/pre.mp4", MediaKind.FILE, "video/mp4", 600_000, "https://site.test/watch", "https://player-two.test/e"))
        assertNotNull("a small clip does not replace it", l.find("https://cdn.test/one.m3u8"))
        l.add(hls("https://cdn.test/two.m3u8", "https://player-two.test/e"))
        assertNull("server two's film replaces the kept one", l.find("https://cdn.test/one.m3u8"))
        assertNotNull(l.find("https://cdn.test/two.m3u8"))
    }

    @Test fun aKeptStreamLivesAgainWhenThePlayerAsksForItAgainAndTheOwnersInterestKeepsItForGood() {
        val l = MediaList()
        l.add(hls("https://cdn.test/one.m3u8", "https://player-one.test/embed")); l.usableCount()
        l.dropFrames(listOf("https://player-one.test/embed"))
        assertTrue(l.add(hls("https://cdn.test/one.m3u8", "https://player-one-again.test/embed")))      // the reloaded frame asks again
        assertFalse(l.find("https://cdn.test/one.m3u8")!!.orphan)
        assertEquals(1, l.size)
        // what the owner touched is never replaced
        l.dropFrames(listOf("https://player-one-again.test/embed"))
        l.touch("https://cdn.test/one.m3u8")
        l.add(hls("https://cdn.test/two.m3u8", "https://player-two.test/e"))
        assertNotNull(l.find("https://cdn.test/one.m3u8"))
    }

    @Test fun aCapturedStreamIsNotHiddenLaterAsAClipOrAnAdvertisement() {
        val l = MediaList()
        val a = hls("https://cdn.test/a.m3u8"); l.add(a)
        l.applyStream(a.url, live = false, drm = false, durationMs = 40_000, adMs = 0, breaks = 0)          // a 40 s stream, readable
        assertEquals(1, l.usableCount())                                                                      // offered: captured
        val b = hls("https://cdn.test/b.m3u8"); l.add(b)
        l.applyStream(b.url, live = false, drm = false, durationMs = 1_200_000, adMs = 0, breaks = 0)       // the film (20 min) arrives: a is "a clip next to a film"
        assertEquals("both stay counted", 2, l.usableCount())
        a.role = VideoRole.AD
        assertTrue("an advertisement label that arrives later does not take it out", a.counts())
        // a stream that cannot be saved is never kept
        val live = hls("https://cdn.test/live.m3u8"); l.add(live); l.usableCount()
        l.applyStream(live.url, live = true, drm = false, durationMs = 0, adMs = 0, breaks = 0)
        assertFalse(live.counts())
    }

    @Test fun theSamePageAgainKeepsWhatWasCapturedAnotherPageForgetsEverything() {
        val l = MediaList()
        val a = MediaCandidate("https://cdn.test/a.m3u8", MediaKind.HLS, "", -1, "https://doraby.com/ozark/", "https://vidsrc.test/embed/1")
        l.add(a); l.add(doc("https://cdn.test/never-offered.mp4")); l.usableCount()
        assertEquals(1, l.onNewPage("https://doraby.com/ozark/"))                       // a reload: the never-offered one goes, the captured one stays
        assertEquals(listOf("https://cdn.test/a.m3u8"), l.keys())
        assertTrue(a.orphan); assertFalse(a.played)
        assertEquals("the www. form, a fragment and a trailing slash are the same page", 0, l.onNewPage("https://www.doraby.com/ozark#player"))
        assertEquals(1, l.onNewPage("https://doraby.com/ozark/?season=1&episode=2"))     // another film of the same site: forgotten
        assertTrue(l.isEmpty())
        val l2 = MediaList(); l2.add(MediaCandidate("https://cdn.test/a.m3u8", MediaKind.HLS, "", -1, "https://doraby.com/ozark/")); l2.usableCount()
        assertEquals(1, l2.onNewPage("https://other.test/ozark/")); assertTrue(l2.isEmpty())
        val l3 = MediaList(); l3.add(MediaCandidate("https://cdn.test/a.m3u8", MediaKind.HLS, "", -1, "https://doraby.com/ozark/")); l3.usableCount()
        assertEquals(1, l3.onNewPage("about:blank")); assertTrue(l3.isEmpty())
    }

    @Test fun pageIdentityIsHostPathAndQueryWithoutTheFragment() {
        assertEquals("doraby.com/ozark", PageIdentity.of("https://www.Doraby.com/Ozark/#x"))
        assertEquals("doraby.com/ozark?s=1", PageIdentity.of("http://doraby.com/ozark/?s=1"))
        assertEquals("doraby.com", PageIdentity.of("https://doraby.com/"))
        assertEquals("", PageIdentity.of("about:blank")); assertEquals("", PageIdentity.of("file:///x")); assertEquals("", PageIdentity.of(""))
        assertTrue(PageIdentity.same("https://a.test/x/", "https://www.a.test/x#top")); assertFalse(PageIdentity.same("https://a.test/x", "https://a.test/y")); assertFalse(PageIdentity.same("", ""))
    }

    // ---- EY-17 (research/REVIEW-EXTERNAL-2.md): rank, not age
    @Test fun aRealVideoIsNotPushedOutBySixtyPreviews() {
        val l = MediaList()
        val big = MediaCandidate("https://cdn.test/movie.mp4", MediaKind.FILE, "video/mp4", 300L * 1024 * 1024, "https://site.test/watch")
        l.add(big)                                                                    // the real video came FIRST (the oldest entry)
        for (i in 1..60) l.add(MediaCandidate("https://ads.test/p$i.mp4", MediaKind.FILE, "video/mp4", 200L * 1024, "https://site.test/watch")) // then sixty previews / ads
        assertEquals(48, l.size)
        assertNotNull("the oldest, biggest entry stays", l.find("https://cdn.test/movie.mp4"))
        assertNotNull("and so does the newest preview", l.find("https://ads.test/p60.mp4"))
        // a real one that arrives LAST is never the one dropped
        val late = MediaCandidate("https://cdn.test/late.mp4", MediaKind.FILE, "video/mp4", 120L * 1024 * 1024, "https://site.test/watch")
        l.add(late)
        assertEquals(48, l.size); assertNotNull(l.find("https://cdn.test/late.mp4")); assertNotNull(l.find("https://cdn.test/movie.mp4"))
    }

    @Test fun aStreamAndAPlayedOrTouchedEntryAreNeverEvicted() {
        val l = MediaList(max = 6)
        val stream = MediaCandidate("https://cdn.test/master.m3u8", MediaKind.HLS, "", -1, "https://site.test/watch")
        val played = MediaCandidate("https://cdn.test/clip.mp4", MediaKind.FILE, "video/mp4", 3L * 1024 * 1024, "https://site.test/watch")
        l.add(stream); l.add(played)
        l.applyInfo("https://cdn.test/clip.mp4", 640, 360, 30_000, null)              // the page's player reported it: touched
        for (i in 1..20) l.add(MediaCandidate("https://ads.test/p$i.mp4", MediaKind.FILE, "video/mp4", 150L * 1024, "https://site.test/watch"))
        assertEquals(6, l.size)
        assertNotNull(l.find("https://cdn.test/master.m3u8")); assertNotNull(l.find("https://cdn.test/clip.mp4"))
        assertTrue("the played 3 MB clip is shown next to the stream (not hidden as a preview)", l.usable().any { it.url == "https://cdn.test/clip.mp4" })
        // an untouched 3 MB file next to a stream IS still hidden
        val l2 = MediaList()
        l2.add(MediaCandidate("https://cdn.test/master.m3u8", MediaKind.HLS, "", -1, "https://site.test/watch"))
        l2.add(MediaCandidate("https://cdn.test/preview.mp4", MediaKind.FILE, "video/mp4", 3L * 1024 * 1024, "https://site.test/watch"))
        assertTrue(l2.usable().none { it.url == "https://cdn.test/preview.mp4" })
        // pressing it (or choosing it in the sheet) makes it a real one too
        l2.touch("https://cdn.test/preview.mp4")
        assertTrue(l2.usable().any { it.url == "https://cdn.test/preview.mp4" })
        assertFalse("touching an address the list does not hold changes nothing", l2.touch("https://cdn.test/nothing.mp4"))
        // sharp (480p+) or long (20 s+) files are shown too
        val sharp = MediaCandidate("https://cdn.test/sharp.mp4", MediaKind.FILE, "video/mp4", 3L * 1024 * 1024, "https://site.test/watch").also { it.height = 720 }
        val long = MediaCandidate("https://cdn.test/long.mp4", MediaKind.FILE, "video/mp4", 3L * 1024 * 1024, "https://site.test/watch").also { it.durationMs = 25_000 }
        l2.add(sharp); l2.add(long)
        assertTrue(l2.usable().any { it.url == "https://cdn.test/sharp.mp4" } && l2.usable().any { it.url == "https://cdn.test/long.mp4" })
    }

    @Test fun theNewestSightingNamesTheDocumentThatAsks() {
        // an address found in server one's script (the document that holds it), asked for later by server two's player: it belongs to the second one now
        val l = MediaList()
        val first = MediaCandidate("https://ads.test/preroll.mp4", MediaKind.FILE, "", -1, "https://site.test/watch", "https://player-one.test/embed", 1)
        l.add(first)
        val second = MediaCandidate("https://ads.test/preroll.mp4", MediaKind.FILE, "video/mp4", 3_000_000, "https://site.test/watch", "https://player-two.test/e/9", 2)
        assertTrue(l.add(second))
        assertEquals(1, l.size)
        assertEquals("https://player-two.test/e/9", first.frame)
        assertEquals(3_000_000L, first.length)
        assertTrue("server one going away leaves it alone", l.dropFrames(listOf("https://player-one.test/embed")).isEmpty())
        assertEquals(listOf("https://ads.test/preroll.mp4"), l.dropFrames(listOf("https://player-two.test/e/9")))
        // a sighting that names no document never takes the frame away
        val l2 = MediaList()
        val a = MediaCandidate("https://cdn.test/a.m3u8", MediaKind.HLS, "", -1, "https://site.test/watch", "https://player-one.test/embed", 1)
        l2.add(a); l2.add(MediaCandidate("https://cdn.test/a.m3u8", MediaKind.HLS, "", -1, "https://site.test/watch", "", 2))
        assertEquals("https://player-one.test/embed", a.frame)
    }

    @Test fun theDownloadCountKeepsWhatWasOfferedUntilALiveFilmReplacesIt() {
        val l = MediaList()
        l.add(hls("https://cdn.test/one.m3u8", "https://player-one.test/embed"))
        assertEquals(1, l.usableCount())
        l.dropFrames(listOf("https://player-one.test/embed"))
        assertEquals("VS2: still counted after the frame went away", 1, l.usableCount())
        l.add(hls("https://cdn.test/two.m3u8", "https://player-two.test/e"))
        assertEquals(1, l.usableCount()); assertNull(l.find("https://cdn.test/one.m3u8"))
    }
}
