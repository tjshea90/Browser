package app.kestrel.core.livediag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LD2b: each rule speaks only with evidence, names the real Settings row, and says its numbers. */
class AdvisorTest {
    private fun site(n: String, loads: Int, failed: Int = 0, median: Long = 3000) = SiteStat(n, loads, failed, median, median * 2, 800)
    private fun adv(s: Map<String, String> = emptyMap(), k: Map<String, Long> = emptyMap(), sites: List<SiteStat> = emptyList(), f: Map<String, Long> = emptyMap()) = Advisor.advise(s, k, sites, f)

    @Test fun aQuietTabletGetsNoAdvice() {
        assertEquals(emptyList<Advice>(), adv(f = mapOf("ramAvailMb" to 600L, "storageFreeMb" to 4000L)))
    }

    @Test fun engineKillsSuggestFewerTabsAndThenSayCloseTabs() {
        val a = adv(k = mapOf("gecko onKill" to 2L)).single()
        assertEquals("tabs_kept", a.key); assertEquals(1, a.value)
        assertTrue(a.text(), a.text().startsWith("Settings > Appearance > Tabs kept ready: change Automatic to Few (2)"))
        assertTrue(a.why, a.why.contains("2 times"))
        val b = adv(s = mapOf("tabs_kept" to "1"), k = mapOf("gecko onKill" to 3L, "gecko onCrash" to 1L)).single()
        assertNull(b.key); assertTrue(b.change, b.change.contains("close the tabs"))
        assertTrue("one kill is not a pattern", adv(k = mapOf("gecko onKill" to 1L)).isEmpty())
    }

    @Test fun manyHibernationsWithFreeMemoryAndNoKillsSuggestAutomatic() {
        val a = adv(s = mapOf("tabs_kept" to "1"), k = mapOf("tabs hibernate" to 14L), f = mapOf("ramAvailMb" to 520L)).single()
        assertEquals("tabs_kept", a.key); assertEquals(0, a.value)
        assertTrue(adv(s = mapOf("tabs_kept" to "1"), k = mapOf("tabs hibernate" to 14L), f = mapOf("ramAvailMb" to 200L)).isEmpty())
    }

    @Test fun slowSitesSuggestThePhoneWhenPairedAndPairingWhenNot() {
        val sites = listOf(site("a.example.org", 5, median = 15_000), site("b.example.org", 4, median = 13_000), site("c.example.org", 9))
        val paired = adv(sites = sites, f = mapOf("phonePaired" to 1L)).single()
        assertEquals("pages_via_phone", paired.key); assertEquals(true, paired.value); assertTrue(paired.why, paired.why.contains("a.example.org"))
        val unpaired = adv(sites = sites).single()
        assertNull(unpaired.key); assertTrue(unpaired.change, unpaired.change.contains("pair Kestrel Courier"))
        assertTrue("already on: nothing to suggest", adv(s = mapOf("pages_via_phone" to "true"), sites = sites, f = mapOf("phonePaired" to 1L)).isEmpty())
        assertTrue("one slow site is not enough", adv(sites = listOf(site("a.example.org", 5, median = 15_000)), f = mapOf("phonePaired" to 1L)).isEmpty())
    }

    @Test fun aQuarterOfLoadsFailingSuggestsHttp3OffAndOneBrokenSiteSuggestsItsShields() {
        val a = adv(sites = listOf(site("a.example.org", 8, failed = 3), site("b.example.org", 8, failed = 1)))
        val h3 = a.first { it.id == "h3-off" }
        assertEquals("net_h3", h3.key); assertEquals(false, h3.value)
        val one = adv(sites = listOf(site("bad.example.org", 4, failed = 3), site("ok.example.org", 20))).single()
        assertNull(one.key); assertEquals("Shields for bad.example.org", one.setting)
        assertTrue("a sensitive bucket is never named", adv(sites = listOf(site("<sensitive-site>", 5, failed = 5))).isEmpty())
    }

    @Test fun slowScreenUpdatesSuggestACalmerGlow() {
        val a = adv(f = mapOf("slowFrames" to 60L, "worstFrameMs" to 250L)).single()
        assertEquals("neon", a.key); assertEquals(1, a.value)
        assertEquals(0, adv(s = mapOf("neon" to "1"), f = mapOf("worstFrameMs" to 700L)).single().value)
    }

    @Test fun droppedFramesAndStallsSuggestThirtyFramesAndASmallerPicture() {
        val d = adv(f = mapOf("framesShown" to 5000L, "framesDropped" to 400L)).single()
        assertEquals("vid_fps30", d.key); assertEquals(true, d.value); assertTrue(d.why, d.why.contains("7 %"))
        val st = adv(f = mapOf("plays" to 2L, "playStalls" to 4L, "playStalledMs" to 20_000L, "linkKbps" to 3000L)).single()
        assertEquals("video_cap", st.key); assertEquals(2, st.value); assertTrue(st.why, st.why.contains("3.0 Mbit/s"))
        assertTrue("too few frames to judge", adv(f = mapOf("framesShown" to 100L, "framesDropped" to 50L)).isEmpty())
    }

    @Test fun backOffWithWaitingPicturesSuggestsInstantBack() {
        val a = adv(s = mapOf("instant_back" to "false"), k = mapOf("tabs back-picture" to 7L)).single()
        assertEquals("instant_back", a.key); assertEquals(true, a.value)
        assertTrue(adv(k = mapOf("tabs back-picture" to 7L)).isEmpty())
    }

    @Test fun housekeepingAdviceNamesTheButtons() {
        val a = adv(f = mapOf("storageFreeMb" to 300L, "filterAgeDays" to 12L, "liveFailsInRow" to 3L))
        assertEquals(setOf("storage-low", "filters-old", "diag-token"), a.map { it.id }.toSet())
        assertTrue(a.all { it.key == null })
    }

    @Test fun theMostSeriousComeFirstAndThereAreNeverMoreThanEight() {
        val many = (1..12).map { site("s$it.example.org", 4, failed = 3) }
        val a = adv(k = mapOf("gecko onKill" to 5L), sites = many)
        assertTrue(a.size <= 8); assertEquals(3, a.first().severity)
        assertEquals(a.map { it.severity }.sortedDescending(), a.map { it.severity })
    }

    @Test fun onlyTheNamedSettingsCanBeChangedByTapAndWithinRange() {
        assertTrue(Advisor.canApply("tabs_kept", 1)); assertTrue(Advisor.canApply("net_h3", false))
        assertFalse(Advisor.canApply("tabs_kept", 7)); assertFalse(Advisor.canApply("net_h3", 1))
        assertFalse("not a whitelisted key", Advisor.canApply("phone_code", "x")); assertFalse(Advisor.canApply("sh_ads", false))
        // every rule that offers a tap names an allowed key
        val all = adv(s = mapOf("tabs_kept" to "2", "instant_back" to "false"), k = mapOf("gecko onKill" to 5L, "tabs back-picture" to 9L), sites = (1..3).map { site("s$it.example.org", 8, failed = 3, median = 14_000) },
            f = mapOf("phonePaired" to 1L, "slowFrames" to 80L, "framesShown" to 5000L, "framesDropped" to 500L, "plays" to 1L, "playStalls" to 5L))
        for (x in all) if (x.key != null) assertTrue(x.id, Advisor.canApply(x.key, x.value))
    }
}
