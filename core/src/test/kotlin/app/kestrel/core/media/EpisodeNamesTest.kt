package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AL6 (owner message 26): "Ozark season 2 Episode 5" is saved as "Ozark S02E05", not as the page's whole title. */
class EpisodeNamesTest {
    private fun page(title: String, url: String = "https://doraby.com/watch", host: String = "doraby.com") = EpisodeNames.fromPage(title, url, host)

    @Test fun theOwnersExample() {
        assertEquals("Ozark S02E05", page("Watch Ozark Season 2 Episode 5 Online Free | Doraby"))
        assertEquals("Ozark S02E05", page("Ozark S02E05 - Doraby"))
        assertEquals("Ozark S02E05", page("Ozark - Season 2 - Episode 5 - Free Streaming"))
        assertEquals("Ozark S02E05", page("Ozark (TV Series 2017–2022) Season 2 Episode 5"))
        assertEquals("Ozark S02E05", page("Ozark Season 2, Episode 5: The Badlands | Doraby"))
        assertEquals("Ozark S02E05", page("Ozark 2x05 - The Badlands"))
        assertEquals("Ozark S02E05", page("Ozark - Episode 5 (Season 2)"))
        assertEquals("Ozark S02E05", page("Doraby - Ozark S2 E5"))
        assertEquals("Ozark S02E05", page("Ozark s02.e05 720p"))
    }

    @Test fun twoDigitSeasonsAndThreeDigitEpisodes() {
        assertEquals("One Piece S21E1000", page("One Piece Season 21 Episode 1000"))
        assertEquals("Friends S10E18", page("Friends S10E18 The Last One"))
        assertEquals("Naruto Shippuden E220", page("Naruto Shippuden Episode 220 English Subbed"))
    }

    @Test fun theAddressNamesTheEpisodeWhenTheTitleDoesNot() {
        assertEquals("Ozark S02E05", page("Ozark - Doraby", "https://doraby.com/tv/ozark/season-2/episode-5"))
        assertEquals("Ozark S02E05", page("Ozark | Watch Online", "https://site.example/watch/ozark-s2-e5"))
        assertEquals("Ozark S02E05", page("", "https://site.example/ozark-season-2-episode-5-online"))
        assertEquals("Breaking Bad S05E14", page("", "https://site.example/series/breaking-bad/season/5/episode/14"))
    }

    @Test fun filmsAndListingsNameNoEpisode() {
        assertNull(page("Watch Dune: Part Two (2024) Full Movie Online Free - Doraby"))
        assertNull(page("Ozark Season 2 - all episodes"))
        assertNull(page("Big Buck Bunny 1920x1080 test"))
        assertNull(page("Intro to Web Design"))
        assertNull(page(""))
    }

    @Test fun anEpisodeLabelIsNeverTakenForTheShow() {
        assertEquals("Ozark S02E05", page("Episode 5 | Season 2", "https://doraby.com/tv/ozark/season-2/episode-5"))
        assertNull(page("Episode 5", "https://doraby.com/"))
    }

    @Test fun filmTitlesLoseTheSiteNoiseButKeepTheirYear() {
        assertEquals("Dune - Part Two (2024)", EpisodeNames.cleanTitle("Watch Dune: Part Two (2024) Full Movie Online Free - Doraby", "doraby.com"))
        assertEquals("Free Guy (2021)", EpisodeNames.cleanTitle("Free Guy (2021) - Watch Online | Doraby", "doraby.com"))
        assertEquals("The Lego Movie", EpisodeNames.cleanTitle("The Lego Movie", "doraby.com"))
        assertEquals("Born Free", EpisodeNames.cleanTitle("Born Free", "doraby.com"))
        assertEquals("Ozark", EpisodeNames.cleanTitle("Ozark (TV Series 2017–2022) - Watch Online", "doraby.com"))
        assertEquals("Great Movie", EpisodeNames.cleanTitle("Great Movie", "s.example"))
        assertEquals("", EpisodeNames.cleanTitle("Watch Online Free | Doraby", "doraby.com"))
        assertEquals("", EpisodeNames.cleanTitle("", "doraby.com"))
    }

    @Test fun releaseNamesBecomeReadable() {
        assertEquals("Ozark S02E05", EpisodeNames.fromRelease("Ozark.S02E05.1080p.WEB.H264-NTb"))
        assertEquals("The Office US S03E10", EpisodeNames.fromRelease("The.Office.US.S03E10.720p.HDTV.x264-CTU"))
        assertEquals("Ozark S02E05", EpisodeNames.fromRelease("Ozark - S02E05 - The Badlands"))
        assertEquals("Ozark S02E05", EpisodeNames.fromRelease("Ozark.2017.S02E05.1080p"))
        assertEquals("Dune Part Two (2024)", EpisodeNames.fromRelease("Dune.Part.Two.2024.1080p.WEB-DL.DDP5.1.Atmos.H.264-FLUX"))
        assertEquals("My Holiday", EpisodeNames.fromRelease("My_Holiday_1080p"))
    }

    @Test fun ordinaryNamesAreLeftAlone() {
        assertNull(EpisodeNames.fromRelease("Holiday_Clip"))
        assertNull(EpisodeNames.fromRelease("report.2023.final"))
        assertNull(EpisodeNames.fromRelease("Web Design Basics"))
        assertNull(EpisodeNames.fromRelease("invoice"))
        assertNull(EpisodeNames.fromRelease("2012.2009"))
    }

    @Test fun theNameIsAlwaysSafeForAFileSystem() {
        val n = page("Watch Dune: Part Two Season 1 Episode 2 Online Free | Doraby")!!
        assertEquals("Dune - Part Two S01E02", n)
        assert(n.none { it in "\\/:*?\"<>|" })
    }

    // ---- RVW1 N1 / N3 (research/REVIEW_2026-11-20_DOWNLOAD.md): a film or clip title keeps its two main pieces; a tab counter and a leading verb that belongs to the title

    @Test fun aTitleWithAnArtistAndASongKeepsBothAndDropsTheSite() {
        assertEquals("Rick Astley - Never Gonna Give You Up (Official Music Video)", EpisodeNames.cleanTitle("Rick Astley - Never Gonna Give You Up (Official Music Video) - YouTube", "www.youtube.com"))
        assertEquals("keeps two pieces when the host does not match the site name", "Rick Astley - Never Gonna Give You Up", EpisodeNames.cleanTitle("Rick Astley - Never Gonna Give You Up - Some Site", "example.org"))
        assertEquals("Mission - Impossible - Fallout (2018)", EpisodeNames.cleanTitle("Mission: Impossible - Fallout (2018) - Site Name", "site.example"))
        assertEquals("a complete film title (it ends with its year) stops the join", "Dune - Part Two (2024)", EpisodeNames.cleanTitle("Dune: Part Two (2024) - Action Movies - Doraby", "doraby.com"))
    }

    @Test fun aNotificationCounterInFrontOfATitleIsNotPartOfTheName() {
        assertEquals("Funny cat video", EpisodeNames.cleanTitle("(3) Funny cat video - YouTube", "youtube.com"))
        assertEquals("Inbox clip", EpisodeNames.cleanTitle("[12] Inbox clip", "site.example"))
        assertEquals("a year in brackets later in the title stays", "Free Guy (2021)", EpisodeNames.cleanTitle("(2) Free Guy (2021) - Watch Online | Doraby", "doraby.com"))
    }
    // ---- RVW5: titles measured on REAL pages with tools/sitelab/realnames.js (2026-11-20); the site's own name went into the file name when its host label is short or the tail is two words

    @Test fun theSiteNameIsDroppedWhenItsHostLabelIsShortOrTheTailIsTwoWords() {
        assertEquals("Sir Ken Robinson - Do schools kill creativity", EpisodeNames.cleanTitle("Sir Ken Robinson: Do schools kill creativity? | TED Talk", "www.ted.com"))
        assertEquals("Latest Videos", EpisodeNames.cleanTitle("Latest Videos | CNN", "www.cnn.com"))
        assertEquals("Tiny Desk", EpisodeNames.cleanTitle("Tiny Desk : NPR", "www.npr.org"))
        assertEquals("Vimeo Staff Picks", EpisodeNames.cleanTitle("Vimeo Staff Picks on Vimeo", "vimeo.com"))
        assertEquals("Big Buck Bunny", EpisodeNames.cleanTitle("Big Buck Bunny - Video Dailymotion", "www.dailymotion.com"))
        assertEquals("Big Buck Bunny 4K", EpisodeNames.cleanTitle("File:Big Buck Bunny 4K.webm - Wikimedia Commons", "commons.wikimedia.org"))
        assertEquals("Popeye for President - Paramount Pictures", EpisodeNames.cleanTitle("Popeye for President : Paramount Pictures : Free Download, Borrow, and Streaming : Internet Archive", "archive.org"))
    }

    @Test fun aTitleThatMerelyContainsAShortSiteLabelIsLeftAlone() {
        assertEquals("Ted Lasso - Trent Crimm", EpisodeNames.cleanTitle("Ted Lasso - Trent Crimm | Apple", "www.ted.com"))   // "Ted Lasso" is the FIRST piece (the title), never a site tail
        assertEquals("Rick Astley - Never Gonna Give You Up", EpisodeNames.cleanTitle("Rick Astley - Never Gonna Give You Up - YouTube", "www.youtube.com"))
        assertEquals("Artist - Cnn Parody", EpisodeNames.cleanTitle("Artist - Cnn Parody", "www.cnn.com"))                 // a short label stands only with filler words ("CNN Video"), so a real second piece stays
        assertEquals("Live on Vimeo Street", EpisodeNames.cleanTitle("Live on Vimeo Street", "vimeo.com"))                  // " on <label>" only at the very end
        assertEquals("Dune - Part Two (2024)", EpisodeNames.cleanTitle("Dune: Part Two (2024) | CNN", "www.cnn.com"))
    }
}
