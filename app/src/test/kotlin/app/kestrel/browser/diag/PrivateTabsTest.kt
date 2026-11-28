package app.kestrel.browser.diag

import android.content.Context
import app.kestrel.browser.Io
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * PV1 (owner 2026-11-21: "Confirm that if I'm using private tabs that no data or information is ever saved at any time or sent anywhere from private tabs. If this is not the case, fix it").
 * Every test uses a made-up site name ("canary") that is easy to search for: if it shows up in the event ring, the live diagnostics' journal, the crash note or the engine's crash reports while a private tab
 * exists, the test fails. The audit table is research/PRIVATE_TABS.md.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PrivateTabsTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private var now = 1_000_000L
    private val canary = "canary-private-site.example.com"

    @Before fun start() {
        PrivateGuard.resetForTest(); PrivateGuard.clock = { now }
        LiveDiag.resetForTest(); ctx.filesDir.deleteRecursively(); ctx.filesDir.mkdirs()
        ctx.getSharedPreferences("livediag", Context.MODE_PRIVATE).edit().clear().commit()
        LiveDiag.init(ctx); ioDone()
    }
    @After fun end() { ioDone(); PrivateGuard.resetForTest(); LiveDiag.resetForTest() }

    private fun ioDone() { val l = CountDownLatch(1); Io.exec { l.countDown() }; assertTrue(l.await(10, TimeUnit.SECONDS)) }
    private fun gecko(rel: String) = File("../geckoapp/src/main/kotlin/app/kestrel/browser/$rel").readText()
    private fun app(rel: String) = File("src/main/kotlin/app/kestrel/browser/$rel").readText()
    private fun ring() = Health.ring.last(200).joinToString("\n")

    @Test fun noSiteNameReachesTheRingOrTheLiveDiagnosticsWhileAPrivateTabExists() {
        PrivateGuard.setOpen(true)
        Health.log("cosmetic", "css", "$canary top=false len=10 tokens=true scriptlets=false")
        Health.log("media", "maybe", "r9---sn-canary.googlevideo.com type=text/plain len=5000 kind=xmlhttprequest")
        Health.log("page", "loaded", "host=$canary paint=1ms done=2ms blocked=0 visit=1 ok=true")
        Health.log("gecko", "onKill", "https://$canary/secret/path?token=1")
        Health.log("page", "timing", "host=$canary ttfb=100 slow=cdn.$canary:js:900 | shields=up net=fast-wifi")
        val r = ring()
        assertFalse(r, r.contains("canary"))
        assertTrue("the line still says what happened: $r", r.contains("cosmetic css <site> top=false len=10"))
        val j = LiveDiag.journalForTest().joinToString("\n")
        assertFalse(j, j.contains("canary"))
        assertTrue("the journal kept the events (masked): $j", j.contains("cosmetic css"))
        assertTrue(Health.counter("private.masked") >= 5)
    }

    @Test fun theNamesComeBackTwoMinutesAfterTheLastPrivateTabWentAway() {
        PrivateGuard.setOpen(true)
        now += 5_000; PrivateGuard.setOpen(false)
        now += 119_000
        Health.log("cosmetic", "css", "during-window.example.org top=true len=1")
        now += 2_000
        Health.log("cosmetic", "css", "after-window.example.org top=true len=1")
        val r = ring()
        assertFalse("a late event of the closed private tab is still masked: $r", r.contains("during-window.example.org"))
        assertTrue("later events are about the regular tabs again: $r", r.contains("after-window.example.org"))
    }

    @Test fun theLiveDiagnosticsSendNothingWhileAPrivateTabExists() {
        assertTrue(LiveDiag.saveToken("github_pat_" + "A".repeat(40)))
        PrivateGuard.setOpen(true)
        var said = ""
        LiveDiag.sendNow({ "snapshot text must not even be built" }) { said = it }
        assertTrue(said, said.contains("private tab"))
        assertTrue(Health.counter("private.holds") >= 1)
        assertEquals("nothing was queued", false, LiveDiag.queuedForTest().also { })
    }

    @Test fun theDiagnosticsLineSaysWhetherTheSafetyNetIsOnAndCarriesNothingPrivate() {
        assertTrue(PrivateGuard.line(), PrivateGuard.line().contains("the safety net is idle"))
        PrivateGuard.setOpen(true)
        Health.log("cosmetic", "css", "$canary top=true len=1")
        assertTrue(PrivateGuard.line(), PrivateGuard.line().contains("the safety net is ON"))
        assertFalse(PrivateGuard.line(), PrivateGuard.line().contains("canary"))
        PrivateGuard.setOpen(false); now += 30_000
        assertTrue(PrivateGuard.line(), PrivateGuard.line().contains("still ON for 90 s"))
        now += 100_000
        assertTrue(PrivateGuard.line().contains("idle"))
    }

    @Test fun theCrashNoteNamesNoSiteWhileAPrivateTabExists() {
        PrivateGuard.setOpen(true)
        Health.log("gecko", "page-start", "https://$canary/watch?v=1")
        CrashNote.write(ctx, Thread.currentThread(), RuntimeException("could not load https://$canary/a?b=c", IllegalStateException("cause for $canary")))
        val note = CrashNote.previous(ctx)
        assertNotNull(note)
        assertFalse(note!!, note.contains("canary"))
        assertTrue(note, note.contains("java.lang.RuntimeException: could not load <site>"))
    }

    @Test fun theEnginesCrashReportsLoseTheirMemoryDumpAndTheAddressOfThePageButKeepWhatDiagnosticsReads() {
        val dir = File(ctx.filesDir, "mozilla/abc.default/minidumps").also { it.mkdirs() }
        val old = System.currentTimeMillis() - 60_000L
        File(dir, "a.dmp").also { it.writeText("MDMP some page text $canary"); it.setLastModified(old) }
        File(dir, "a.extra").also { it.writeText("""{"ProcessType":"tab","MozCrashReason":"MOZ_CRASH(oom)","URL":"https://$canary/secret","Notes":"n","CrashTime":"1700000000","StackTraces":"x"}"""); it.setLastModified(old) }
        File(dir, "b.extra").also { it.writeText("ProcessType=tab\nURL=https://$canary/other\nMozCrashReason=abort\nCrashTime=1700000001\n"); it.setLastModified(old) }
        File(dir, "fresh.dmp").also { it.writeText("being written right now"); it.setLastModified(System.currentTimeMillis()) }
        val n = CrashNote.scrubEngineReports(ctx)
        assertTrue("changed $n", n >= 3)
        assertFalse("the dump is gone", File(dir, "a.dmp").exists())
        assertTrue("a report being written right now is left alone", File(dir, "fresh.dmp").exists())
        for (f in listOf("a.extra", "b.extra")) {
            val t = File(dir, f).readText()
            assertFalse(f + ": " + t, t.contains("canary")); assertFalse(t, t.contains("URL")); assertTrue(f + ": " + t, t.contains("ProcessType"))
            assertTrue("its time is kept (LastRun judges by it)", Math.abs(File(dir, f).lastModified() - old) < 2_000L)
        }
        val lines = CrashNote.engineLines(ctx, System.currentTimeMillis()).joinToString("\n")
        assertTrue("Diagnostics still reads the reason: $lines", lines.contains("MozCrashReason=MOZ_CRASH(oom)") && lines.contains("ProcessType=tab"))
        assertEquals("a second run finds nothing more to change", 0, CrashNote.scrubEngineReports(ctx))
    }

    // ---- the rules, pinned in the source (a later edit that opens a hole fails here)

    @Test fun everyEventTakesTheSafetyNetOnItsWayIntoTheRing() {
        val h = app("diag/Health.kt")
        assertTrue(h.contains("append(PrivateGuard.text(detail))"))
        assertTrue("the ring takes the masked line, and the live diagnostics read the same line", h.indexOf("PrivateGuard.text(detail)") < h.indexOf("ring.add(line)") && h.indexOf("ring.add(line)") < h.indexOf("LiveDiag.observe(layer, event, line)"))
    }

    @Test fun thePrivateWindowOpensWithTheTabAndEveryRemovalChecksIt() {
        val b = gecko("BrowserActivity.kt")
        assertTrue(b.contains("if (privateTab) PrivateGuard.setOpen(true)"))
        assertTrue(b.substringAfter("private fun syncPrivateAnchor()").substringBefore("if (need && privateAnchor == null)").contains("PrivateGuard.setOpen(need)"))
        assertTrue("every place that removes tabs asks the anchor (and with it the window) again", b.substringAfter("private fun doCloseTab(").substringBefore("if (tab === restoringTab)").contains("syncPrivateAnchor()"))
        assertTrue(b.substringAfter("private fun liveDiagSnapshot()").substringBefore("val inPrivate").contains("if (PrivateGuard.active()) return \"\""))
    }

    @Test fun noDiagnosticLineNamesTheFrontPrivatePageOrCountsPrivateTabs() {
        val b = gecko("BrowserActivity.kt")
        assertFalse("the Engine line used to print the front page's address whatever the tab", b.contains("; current \${current?.url?.take(60)"))
        assertTrue(b.contains("current \${if (current?.isPrivateTab == true) \"a private tab\""))
        assertTrue(b.contains("if (tab.isPrivateTab) \"private tab\" else (tab.topHost ?: \"?\")"))
        assertTrue(b.contains("m[\"tabs\"] = tabs.count { !it.isPrivateTab }"))
        assertFalse("no 'tabs=' count that includes private tabs", b.contains("\" tabs=\" + tabs.size"))
    }

    @Test fun theLiveDiagnosticsLookAtTheWindowBeforeTheySendAndJustBeforeTheBytesGo() {
        val s = app("diag/LiveDiag.kt")
        val req = s.substringAfter("private fun request(").substringBefore("if (!uploading.compareAndSet")
        assertTrue(req.contains("if (PrivateGuard.active())"))
        assertTrue("the hold comes before the snapshot is built", req.indexOf("PrivateGuard.active()") < req.indexOf("DiagSchedule.State("))
        val up = s.substringAfter("private fun upload(")
        assertEquals("checked at the start of an upload and again before the bytes", 2, Regex("PrivateGuard\\.active\\(\\)").findAll(up).count())
    }

    @Test fun theKeyboardLearnsNothingFromAPrivateTabNotEvenWhereTheThumbLands() {
        val m = app("ui/MiniNav.kt")
        val bad = m.lines().filter { it.contains(".accepted(") && !it.contains("learnAllowed()") && !it.trimStart().startsWith("*") && !it.trimStart().startsWith("//") }
        assertEquals(bad.joinToString("\n"), emptyList<String>(), bad)
        assertTrue(app("ui/Panels.kt").contains("IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING"))
    }

    @Test fun theEnginesAddressChecksArePausedWhileAPrivateTabExistsAndComeBackAfterwards() {
        val b = gecko("BrowserActivity.kt")
        assertTrue(b.substringAfter("private fun syncPrivateAnchor()").substringBefore("if (need && privateAnchor == null)").contains("GeckoEngine.pauseSafeBrowsing(need)"))
        val e = gecko("GeckoEngine.kt")
        val f = e.substringAfter("fun pauseSafeBrowsing(on: Boolean)").substringBefore("/** The profile now in force")
        assertTrue(f.contains("setGeckoPref(\"browser.safebrowsing.malware.enabled\", false") && f.contains("setGeckoPref(\"browser.safebrowsing.phishing.enabled\", false"))
        assertTrue(f.contains("clearGeckoUserPref(\"browser.safebrowsing.malware.enabled\")") && f.contains("clearGeckoUserPref(\"browser.safebrowsing.phishing.enabled\")"))
        assertTrue("it is a USER-branch override, so clearing it brings the engine's own default back", f.contains("PREF_BRANCH_USER"))
    }

    @Test fun crashReportsAreScrubbedAtStartAndAfterEveryEngineCrashOrKill() {
        assertTrue(gecko("GeckoApp.kt").contains("CrashNote.scrubEngineReports(this)"))
        val t = gecko("web/TabDelegates.kt")
        assertEquals(2, Regex("scrubCrashReports\\(\\) \\}").findAll(t).count())
    }

    /** VD1 (owner 2026-11-21, per-site video lines): the new `video gate|codec-no|error` lines name a site; with a private tab open the safety net masks it, and the handler drops a private, unknown or sensitive page before anything is kept. */
    @Test fun theVideoLinesNameNoSiteWhileAPrivateTabExistsAndTheirHandlerDropsPrivateAndSensitivePages() {
        PrivateGuard.setOpen(true)
        Health.log("video", "gate", "$canary held=1 kept=6 refused=14 opened=0 frame=top rule=on")
        Health.log("video", "codec-no", "$canary size=0 mode=0 fps=4 last=avc_60fps")
        Health.log("video", "error", "$canary code=4 net=3 ready=0 src=blob frame=top el=v")
        val r = ring()
        assertFalse(r, r.contains("canary"))
        assertTrue("the lines still say what happened: $r", r.contains("video gate <site> held=1 kept=6 refused=14"))
        val j = LiveDiag.journalForTest().joinToString("\n")
        assertFalse(j, j.contains("canary"))
        assertTrue("the journal keeps the video problems (masked): $j", j.contains("video gate") && j.contains("video error"))
        val b = gecko("BrowserActivity.kt")
        val h = b.substringAfter("private fun onVideoSite(").substringBefore("private fun liveDiagExtraFacts()")
        assertTrue("an unknown or private page is dropped before the post", h.contains("if (page.isEmpty() || pageIsPrivate(page)) return"))
        assertTrue("the tab's own kind and a sensitive site are checked again on the main thread", h.contains("tab.isPrivateTab || app.kestrel.core.livediag.SensitiveSites.isSensitive(host)"))
        assertTrue("the host is the TAB's, not the script's", h.contains("tab.topHost") && !h.contains("m.optString(\"h\")"))
        val sb = gecko("web/ShieldsBridge.kt")
        for (k in listOf("vgh" to "videoGate", "vch" to "videoCodec", "verr" to "videoError")) assertTrue(k.first, sb.contains("\"${k.first}\" -> { ${k.second}(m); \"\" }"))
    }

    /** IMG1: the facts about a picked file are looked at only for a normal tab, with no private tab anywhere, and carry no file name. */
    @Test fun thePickedFileFactsAreTakenOnlyOutsideAPrivateTabAndNameNoFile() {
        val b = gecko("BrowserActivity.kt")
        assertTrue(b.contains("if (given.isNotEmpty() && current?.isPrivateTab != true && !PrivateGuard.active())"))
        val f = b.substringAfter("private fun pickedFacts(").substringBefore("internal fun requestAndroidPermissions(")
        assertTrue("only the size and the type are read from the provider, the display name only for its extension", f.contains("OpenableColumns.DISPLAY_NAME") && f.contains("lastIndexOf('.')"))
        assertFalse("the name itself never goes into a line", Regex("Health\\.log\\([^\\n]*(getString|displayName)").containsMatchIn(f))
        assertTrue(f.contains("FilePickFacts.logLine(f)"))
    }

    /** PERF3c: the typing probe counts only numbers, and only outside a private tab. */
    @Test fun theTypingProbeKeepsOnlyNumbersAndSkipsPrivateTabs() {
        val b = gecko("BrowserActivity.kt")
        val w = b.substringAfter("private val typingWatcher").substringBefore("private fun flushAddressTyping()")
        assertTrue("the address-bar probe asks the tab kind first", w.contains("if (current?.isPrivateTab == true) return"))
        assertFalse("it never reads the typed text", w.contains("s.toString()") || w.contains("s?.toString()") || w.contains("s.length") && w.contains("Health.log"))
        val h = b.substringAfter("private fun onTypingWait(").substringBefore("private var typingModeSince")
        assertTrue(h.contains("pageIsPrivate(page)") && h.contains("PrivateGuard.active()") && h.contains("tab.isPrivateTab"))
        assertFalse("no address or title in the line", h.contains("tab.url") || h.contains("tab.title") || h.contains("topHost"))
    }

    /** PERF10c: what a page received when a file input changed is logged for a normal tab only, never while a private tab exists, and carries no name, path or title. */
    @Test fun theAttachProbeIsToldForNormalTabsOnlyAndNamesNoFile() {
        val b = gecko("BrowserActivity.kt")
        val h = b.substringAfter("private fun onFileCheck(").substringBefore("private fun onTypingWait(")
        assertTrue(h.contains("pageIsPrivate(page)") && h.contains("PrivateGuard.active()") && h.contains("tab.isPrivateTab"))
        assertTrue("it logs the pieces FilePickFacts builds", h.contains("FilePickFacts.pageLine(") && h.contains("FilePickFacts.pageVerdict("))
        assertFalse("no address or title in the line", h.contains("tab.url") || h.contains("tab.title"))
        val pt = File("../geckoapp/src/main/assets/kestrel-shields/pagetime.js").readText()
        assertFalse("the page script never reads a file name or path", pt.contains("f.name") || pt.contains("fl[i].name") || pt.contains("webkitRelativePath"))
    }
}

