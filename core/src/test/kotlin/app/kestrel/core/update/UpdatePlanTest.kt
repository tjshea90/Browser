package app.kestrel.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePlanTest {
    private val xml = """<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>org.mozilla.geckoview</groupId>
  <artifactId>geckoview-armeabi-v7a</artifactId>
  <versioning>
    <latest>161.0.20261110090000</latest>
    <release>161.0.20261110090000</release>
    <versions>
      <version>159.0.20261001120000</version>
      <version>160.0.20261104181552</version>
      <version>160.0.1.20261107101010</version>
      <version>161.0.20261110090000</version>
    </versions>
    <lastUpdated>20261110090500</lastUpdated>
  </versioning>
</metadata>"""

    @Test fun theNewestReleaseIsTheDeclaredOne() {
        assertEquals("161.0.20261110090000", EngineWatch.newest(xml))
    }

    @Test fun whenNothingIsDeclaredTheHighestVersionWins() {
        val x = "<versions><version>159.0.20260901000000</version><version>161.0.20261110090000</version><version>160.0.20261104181552</version></versions>"
        assertEquals("161.0.20261110090000", EngineWatch.newest(x))
    }

    @Test fun aStaleReleaseTagDoesNotHideAHigherListedVersion() {
        val x = "<release>160.0.20261104181552</release><versions><version>160.0.20261104181552</version><version>161.0.20261110090000</version></versions>"
        assertEquals("161.0.20261110090000", EngineWatch.newest(x))
    }

    @Test fun garbageNamesNoVersion() {
        assertNull(EngineWatch.newest(""))
        assertNull(EngineWatch.newest("<html>blocked by a captive portal</html>"))
        assertNull(EngineWatch.newest("<version>abc</version>"))
    }

    @Test fun newerMeansAHigherMajorOrMinorNotARebuild() {
        assertTrue(EngineWatch.isNewer("160.0.20261104181552", "161.0.20261110090000"))
        assertTrue(EngineWatch.isNewer("160.0", "160.1.20261107101010"))
        assertFalse("a rebuild of the same release is not news", EngineWatch.isNewer("160.0.20261104181552", "160.0.20261109000000"))
        assertFalse(EngineWatch.isNewer("161.0", "160.0.20261104181552"))
        assertFalse(EngineWatch.isNewer(null, "161.0.1"))
        assertFalse(EngineWatch.isNewer("160.0", ""))
        assertFalse(EngineWatch.isNewer("x", "161.0.1"))
    }

    @Test fun labelsAreWhatPeopleSay() {
        assertEquals("161", EngineWatch.label("161.0.20261110090000"))
        assertEquals("160.1", EngineWatch.label("160.1.20261107101010"))
    }

    private val news = UpdatePlan.AppNews("kestrel-g-v0.3.0-alpha16", "0.3.0-alpha16", "92 MB", false)

    @Test fun nothingNewIsNoDialog() {
        assertNull(UpdatePlan.build("Kestrel G", "0.3.0-alpha15", null, "160.0", "160.0.20261104181552", "Ad-blocking lists refreshed today", null))
    }

    @Test fun anAppUpdateOffersTheDownload() {
        val n = UpdatePlan.build("Kestrel G", "0.3.0-alpha15", news, "160.0", "160.0.20261104181552", null, null)!!
        assertEquals("Update available", n.title)
        assertEquals(UpdatePlan.Action.DOWNLOAD, n.action)
        assertEquals(listOf("Kestrel G 0.3.0-alpha16 (92 MB) - you have 0.3.0-alpha15."), n.lines)
    }

    @Test fun aDownloadedUpdateOffersTheInstall() {
        val n = UpdatePlan.build("Kestrel G", "0.3.0-alpha15", UpdatePlan.AppNews("t", "0.3.0-alpha16", "92 MB", true), null, null, null, null)!!
        assertEquals("Update ready to install", n.title)
        assertEquals(UpdatePlan.Action.INSTALL, n.action)
        assertFalse("the size of a finished download is no news", n.lines[0].contains("92 MB"))
    }

    @Test fun aNewerEngineAloneIsOnlyInformation() {
        val n = UpdatePlan.build("Kestrel G", "0.3.0-alpha15", null, "160.0", "161.0.20261110090000", null, null)!!
        assertEquals("Newer browser engine available", n.title)
        assertEquals(UpdatePlan.Action.NONE, n.action)
        assertTrue(n.lines[0].contains("Firefox 161 is out (this app has 160)"))
        assertTrue(n.lines[0].contains("will move to it in a coming update"))
    }

    @Test fun appAndEngineAndListsShareOneDialog() {
        val n = UpdatePlan.build("Kestrel G", "0.3.0-alpha15", news, "160.0", "161.0.20261110090000", "Ad-blocking lists: refreshed today (99,120 rules).", null)!!
        assertEquals(3, n.lines.size)
        assertEquals(UpdatePlan.Action.DOWNLOAD, n.action)
        assertFalse("with an app update the engine line makes no promise", n.lines[1].contains("coming update"))
        assertTrue(n.lines[2].startsWith("Ad-blocking lists"))
    }

    @Test fun theSameNewsIsShownOnce() {
        val first = UpdatePlan.build("Kestrel G", "0.3.0-alpha15", news, "160.0", "161.0.2", null, null)!!
        assertNull(UpdatePlan.build("Kestrel G", "0.3.0-alpha15", news, "160.0", "161.0.2", null, first.key))
        // a later check finds the file downloaded: that is new news
        assertNotNull(UpdatePlan.build("Kestrel G", "0.3.0-alpha15", UpdatePlan.AppNews(news.tag, news.versionName, news.sizeText, true), "160.0", "161.0.2", null, first.key))
        // and a newer release is new news too
        assertNotNull(UpdatePlan.build("Kestrel G", "0.3.0-alpha15", UpdatePlan.AppNews("kestrel-g-v0.3.0-alpha17", "0.3.0-alpha17", "92 MB", false), "160.0", "161.0.2", null, first.key))
    }
}
