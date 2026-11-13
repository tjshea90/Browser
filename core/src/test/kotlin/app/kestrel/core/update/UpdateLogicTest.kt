package app.kestrel.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateLogicTest {
    private val sha = "ecd42ae8d8e0885e77b56346e775ee7764a4cdabc4115611d3174ddbe57288d9"

    private fun body(code: Int, note: String = "Typing-freeze fixes.", name: String = "Kestrel G 0.2.0-alpha$code") =
        "$note\n\n**$name** (versionCode $code). Kestrel G on Firefox's engine.\n\n- Install: open this page.\n- APK SHA-256: `$sha`\n"

    private fun rel(tag: String, code: Int, assets: List<ReleaseAsset> = listOf(ReleaseAsset("$tag.apk", "https://example.invalid/$tag.apk", 92_000_000, null)),
                    draft: Boolean = false, body: String = body(code)) =
        Release(tag, tag, body, true, draft, "https://example.invalid/r/$tag", assets)

    @Test fun parsesTheVersionCodeFromTheGeneratedBody() {
        assertEquals(102L, UpdateLogic.versionCode(body(102)))
        assertNull(UpdateLogic.versionCode("No change note was given."))
    }

    @Test fun picksTheBodyChecksumAndFallsBackToGithubsDigest() {
        assertEquals(sha, UpdateLogic.sha256(body(5), null))
        val a = ReleaseAsset("x.apk", "u", 1, "sha256:" + sha.uppercase())
        assertEquals(sha, UpdateLogic.sha256("nothing here", a))
        assertNull(UpdateLogic.sha256("nothing here", ReleaseAsset("x.apk", "u", 1, "md5:abcd")))
        assertNull(UpdateLogic.sha256("nothing here", ReleaseAsset("x.apk", "u", 1, "sha256:short")))
    }

    @Test fun notesAreTheHumanPartOnly() {
        assertEquals("Typing-freeze fixes.", UpdateLogic.notes(body(3)))
        assertEquals("", UpdateLogic.notes("**Kestrel G 1** (versionCode 1). about"))
        assertEquals("plain text with no generated paragraph", UpdateLogic.notes("plain text with no generated paragraph"))
    }

    @Test fun offersTheNewestReleaseOfThisAppOnly() {
        val list = listOf(
            rel("kestrel-g-v0.2.0-alpha3", 102),
            rel("kestrel-g-v0.2.0-alpha4", 103),
            rel("kestrel-v0.1.1", 200), // the other app: a higher code must never be offered to this one
            rel("kestrel-g-v0.2.0-alpha2", 101),
        )
        val o = UpdateLogic.choose(list, "kestrel-g-v", 102)!!
        assertEquals("kestrel-g-v0.2.0-alpha4", o.tag)
        assertEquals("0.2.0-alpha4", o.versionName)
        assertEquals(103L, o.versionCode)
        assertEquals(sha, o.sha256)
        assertEquals("Typing-freeze fixes.", o.notes)
    }

    @Test fun nothingWhenUpToDate() {
        assertNull(UpdateLogic.choose(listOf(rel("kestrel-g-v0.2.0-alpha3", 102)), "kestrel-g-v", 102))
        assertNull(UpdateLogic.choose(listOf(rel("kestrel-g-v0.2.0-alpha3", 102)), "kestrel-g-v", 150)) // a newer local build (debug) is not "downgraded"
        assertNull(UpdateLogic.choose(emptyList(), "kestrel-g-v", 1))
    }

    @Test fun ignoresDraftsReleasesWithoutAnApkAndWithoutACode() {
        assertNull(UpdateLogic.choose(listOf(rel("kestrel-g-v9", 900, draft = true)), "kestrel-g-v", 1))
        assertNull(UpdateLogic.choose(listOf(rel("kestrel-g-v9", 900, assets = listOf(ReleaseAsset("notes.txt", "u", 1, null)))), "kestrel-g-v", 1))
        assertNull(UpdateLogic.choose(listOf(rel("kestrel-g-v9", 900, body = "hand-written release, no code")), "kestrel-g-v", 1))
    }

    @Test fun webAndGeckoPrefixesNeverCollide() {
        val list = listOf(rel("kestrel-g-v0.2.0-alpha4", 103), rel("kestrel-v0.1.2", 3))
        assertEquals("kestrel-v0.1.2", UpdateLogic.choose(list, "kestrel-v", 2)!!.tag)
        assertEquals("kestrel-g-v0.2.0-alpha4", UpdateLogic.choose(list, "kestrel-g-v", 2)!!.tag)
    }

    @Test fun formatsSizes() {
        assertEquals("92.0 MB", UpdateLogic.formatSize(96_468_992))
        assertEquals("512 KB", UpdateLogic.formatSize(524_288))
        assertEquals("unknown size", UpdateLogic.formatSize(-1))
    }

    @Test fun resumeOnlyWhenTheServerHonoursTheOffset() {
        assertTrue(UpdateLogic.canResume(1000, 206, 1000))
        assertFalse(UpdateLogic.canResume(1000, 200, null)) // server ignored Range: restart
        assertFalse(UpdateLogic.canResume(1000, 206, 0))
        assertFalse(UpdateLogic.canResume(0, 206, 0))
    }

    private val mib = 1L shl 20
    private fun m(x: Double) = (x * mib).toLong()

    @Test fun lowReserveIsTheSmallerOfFivePercentAndHalfAGigabyte() {
        // Android 10 StorageManager.getStorageLowBytes, measured on the emulator (research/ADB_FOR_THE_OWNER.md): 6,094,400 KiB data partition
        val total = 6_094_400L * 1024
        assertEquals(312_033_280L, UpdateLogic.lowReserve(total))                       // 5 % = 297.6 MiB
        assertEquals(124_813_312L, UpdateLogic.lowReserve(total, percent = 2))          // 2 % = 119.0 MiB (the install bar moved down by the same 178.6 MiB)
        assertEquals(100 * mib, UpdateLogic.lowReserve(total, maxBytes = 100 * mib))    // the cap
        assertEquals(500 * mib, UpdateLogic.lowReserve(total, percent = 20))            // 20 % would be 1190 MiB: the default 500 MiB cap binds (measured: 502 fails / 505 passes for a 2 MB app)
        assertEquals(500 * mib, UpdateLogic.lowReserve(m(27.75 * 1024)))                // the owner's 27.75 GB tablet: 5 % = 1.39 GB, capped to 500 MiB (NOT a tenth = 2.8 GB)
        assertEquals(0L, UpdateLogic.lowReserve(0L)); assertEquals(0L, UpdateLogic.lowReserve(total, percent = 0)); assertEquals(500 * mib, UpdateLogic.lowReserve(total * 100, percent = 500))
    }

    @Test fun storagePlanForTheGeckoApk() {
        // an 11 GB data partition: 5 % = 550 MiB, capped to 500 MiB; 92 MB APK, 132 MB of libraries to unpack, 32 MB margin
        val reserve = UpdateLogic.lowReserve(11_000L * mib)
        assertEquals(500 * mib, reserve)
        val plan = UpdateLogic.storagePlan(92 * mib, 132 * mib, freeBytes = 1800 * mib, reserveBytes = reserve)
        assertEquals((500 + 2 * (92 + 132) + 32) * mib, plan.needKeep)    // 980
        assertEquals((500 + 2 * (92 + 132) + 32 - 92) * mib, plan.needDrop) // 888: our own copy deleted once the installer holds its own
        assertEquals((2 * (92 + 132) + 32) * mib, plan.installTakes)       // 480
        assertTrue(plan.fitsKeeping); assertFalse(plan.fitsDropping); assertEquals(0L, plan.shortBy)
        val tight = UpdateLogic.storagePlan(92 * mib, 132 * mib, freeBytes = 900 * mib, reserveBytes = reserve) // enough only without our own copy
        assertFalse(tight.fitsKeeping); assertTrue(tight.fitsDropping); assertEquals(0L, tight.shortBy)
        val short = UpdateLogic.storagePlan(92 * mib, 132 * mib, freeBytes = 800 * mib, reserveBytes = reserve)
        assertFalse(short.fitsKeeping); assertFalse(short.fitsDropping); assertEquals(88 * mib, short.shortBy)
        assertEquals(80 * mib, UpdateLogic.roundUpTenMb(75 * mib)); assertEquals(0L, UpdateLogic.roundUpTenMb(0)); assertEquals(10 * mib, UpdateLogic.roundUpTenMb(1))
    }

    @Test fun storagePlanMatchesWhatTheEmulatorMeasured() {
        // Android 10 emulator, 6,094,400 KiB data partition, `adb install --no-incremental` of a fresh app (no copy of ours on the disk), the smallest free space BEFORE the install that succeeded (bisection):
        // small WebView APK (2.3 MiB, no libraries): 301 fails / 304 passes (reserve 297.6 MiB); 2 %: 122 fails / 124 passes (119.0); 100 MiB cap: 104 fails / 106 passes; 20 %: 502 fails / 505 passes (500 MiB cap).
        // Gecko release APK (95.5 MiB + 187.4 MiB of libraries): 861 fails / 868 passes (297.6); with 2 %: 676 fails / 685 passes (119.0); with the 100 MiB cap: passes at 760.  Line = reserve + 2 x (APK + libraries).
        val total = 6_094_400L * 1024
        fun fits(apkMiB: Double, libMiB: Double, freeMiB: Long, reserve: Long) =
            UpdateLogic.storagePlan(m(apkMiB), m(libMiB), freeBytes = freeMiB * mib, reserveBytes = reserve, marginBytes = 0).fitsKeeping
        val r5 = UpdateLogic.lowReserve(total); val r2 = UpdateLogic.lowReserve(total, percent = 2); val r100 = UpdateLogic.lowReserve(total, maxBytes = 100 * mib); val r20 = UpdateLogic.lowReserve(total, percent = 20)
        assertTrue(fits(2.3, 0.0, 304, r5)); assertFalse(fits(2.3, 0.0, 301, r5))
        assertTrue(fits(2.3, 0.0, 124, r2)); assertFalse(fits(2.3, 0.0, 122, r2))
        assertTrue(fits(2.3, 0.0, 106, r100)); assertFalse(fits(2.3, 0.0, 104, r100))
        assertTrue(fits(2.3, 0.0, 505, r20)); assertFalse(fits(2.3, 0.0, 502, r20))
        assertTrue(fits(95.5, 187.4, 868, r5)); assertFalse(fits(95.5, 187.4, 861, r5))
        assertTrue(fits(95.5, 187.4, 690, r2)); assertFalse(fits(95.5, 187.4, 676, r2)) // measured passes at 685; the line says 684.8 (rounded inputs): 690 keeps a margin
        assertTrue(fits(95.5, 187.4, 760, r100))
    }

    @Test fun theOwnersTabletAlreadyHasRoomForTheUpdate() {
        // alpha27 Diagnostics from the owner's tablet: 1.62 GB free of 27.75 GB; this app: package 94.4 + libraries 131.7 + data 62.3 + cache 228.1 MB; alpha30 is a 94.4 MiB APK.
        // The reserve there is 500 MiB (the cap), not a tenth (2.8 GB): alpha28-alpha30 said "needs about 3.1 GB, free 1.4 GB more" and sent the owner to delete things for nothing.
        val reserve = UpdateLogic.lowReserve(m(27.75 * 1024))
        val plan = UpdateLogic.storagePlan(m(94.4), m(131.7), freeBytes = m(1.62 * 1024), reserveBytes = reserve)
        assertTrue("needs about 0.96 GiB: ${plan.needKeep / mib} MiB", plan.needKeep in m(970.0)..m(1000.0))
        assertTrue(plan.fitsKeeping); assertEquals(0L, plan.shortBy)
        // and with the old picture (a tenth) it would have said the opposite
        val old = UpdateLogic.storagePlan(m(94.4), m(131.7), freeBytes = m(1.62 * 1024), reserveBytes = m(2.775 * 1024), marginBytes = 48 * mib)
        assertFalse(old.fitsKeeping)
    }

    @Test fun theOtherWayHelpsOnlyWhenTheUninstallFreesEnough() {
        // a tablet that is genuinely tight: 700 MiB free, a 92 MB APK with 132 MB of libraries, reserve 500 MiB -> in place needs 980 (kept) / 888 (dropped)
        val plan = UpdateLogic.storagePlan(92 * mib, 132 * mib, freeBytes = 700 * mib, reserveBytes = 500 * mib)
        assertFalse(plan.fitsKeeping); assertFalse(plan.fitsDropping); assertEquals(188 * mib, plan.shortBy)
        val other = UpdateLogic.otherWayPlan(plan, ownBytes = m(94.4 + 131.7 + 62.3 + 228.1), apkBytes = 92 * mib, fileKept = true) // the uninstall hands back about 0.5 GB
        assertTrue("about 1.1 GB would be free after the uninstall: ${other.freeAfter / mib} MiB", other.freeAfter in m(1100.0)..m(1200.0))
        assertTrue(other.fits); assertEquals(0L, other.shortBy)
    }

    @Test fun theOtherWayFitsWhenTheUninstallFreesEnoughAndCountsADownloadThatIsStillToCome() {
        val plan = UpdateLogic.storagePlan(92 * mib, 132 * mib, freeBytes = 1000 * mib, reserveBytes = 1100 * mib)       // needKeep 1100 + 448 + 32 = 1580
        val big = UpdateLogic.otherWayPlan(plan, ownBytes = 700 * mib, apkBytes = 92 * mib, fileKept = true)            // 1000 + 700 - 92 = 1608 >= 1580
        assertTrue(big.fits); assertEquals(0L, big.shortBy); assertEquals(1608 * mib, big.freeAfter)
        val small = UpdateLogic.otherWayPlan(plan, ownBytes = 400 * mib, apkBytes = 92 * mib, fileKept = true)          // 1308: 272 MB short
        assertFalse(small.fits); assertEquals(272 * mib, small.shortBy)
        val notYet = UpdateLogic.otherWayPlan(plan, ownBytes = 400 * mib, apkBytes = 92 * mib, fileKept = false)         // the download is on the disk by then and leaves with the app: same as kept
        assertEquals(1400 * mib, notYet.freeAfter)
        assertEquals(0L, UpdateLogic.otherWayPlan(UpdateLogic.storagePlan(92 * mib, 0, 0, 0), 0, 92 * mib, true).freeAfter) // never negative
    }

    @Test fun storagePlanWithoutNativeLibraries() { // the WebView build: a 2 MB app on a small partition
        val plan = UpdateLogic.storagePlan(2 * mib, 0, freeBytes = 400 * mib, reserveBytes = 300 * mib)
        assertTrue(plan.fitsKeeping); assertEquals(0L, plan.shortBy)
    }

    // ------------------------------------------------------------------ owner report X1: "Alpha9 says it is up to date"
    // GitHub's list of this repository's releases is sorted by tag NAME, descending - not by date and not by version. This is the order it really answered with (2026-11-12), 13 releases:
    private val githubOrder = listOf(
        "kestrel-v0.1.1", "kestrel-g-v0.2.0-alpha9", "kestrel-g-v0.2.0-alpha8", "kestrel-g-v0.2.0-alpha7", "kestrel-g-v0.2.0-alpha6", "kestrel-g-v0.2.0-alpha5", "kestrel-g-v0.2.0-alpha4",
        "kestrel-g-v0.2.0-alpha3", "kestrel-g-v0.2.0-alpha2", "kestrel-g-v0.2.0-alpha13", "kestrel-g-v0.2.0-alpha12", "kestrel-g-v0.2.0-alpha11", "kestrel-g-v0.2.0-alpha10",
    )
    private fun codeOf(tag: String): Int = when {
        tag == "kestrel-v0.1.1" -> 2
        tag.startsWith("kestrel-g-v0.2.0-alpha") -> 99 + tag.removePrefix("kestrel-g-v0.2.0-alpha").toInt() // alpha2 = 101 ... alpha13 = 112 (the real codes)
        else -> 113
    }
    private fun releasesIn(order: List<String>) = order.map { rel(it, codeOf(it)) }

    @Test fun theOldUpdaterReadOnlyTheFirstEightEntriesAndSoNeverSawAlpha10OrLater() {
        val firstEight = releasesIn(githubOrder).take(8)
        assertEquals(listOf("kestrel-v0.1.1", "kestrel-g-v0.2.0-alpha9", "kestrel-g-v0.2.0-alpha8", "kestrel-g-v0.2.0-alpha7", "kestrel-g-v0.2.0-alpha6", "kestrel-g-v0.2.0-alpha5", "kestrel-g-v0.2.0-alpha4", "kestrel-g-v0.2.0-alpha3"), firstEight.map { it.tag })
        // an installed alpha9 (code 108), or alpha11 (110), or alpha13 (112): "up to date" although alpha13 exists - the reported bug
        assertNull(UpdateLogic.choose(firstEight, "kestrel-g-v", 108))
        assertNull(UpdateLogic.choose(firstEight, "kestrel-g-v", 110))
        assertNull(UpdateLogic.choose(firstEight, "kestrel-g-v", 112))
    }

    @Test fun naturalTagOrderPutsAlpha10AboveAlpha9AndTheNewestFirstWhateverOrderGithubUses() {
        assertTrue(UpdateLogic.compareTags("kestrel-g-v0.2.0-alpha10", "kestrel-g-v0.2.0-alpha9") > 0)
        assertTrue(UpdateLogic.compareTags("kestrel-g-v0.2.0-alpha13", "kestrel-g-v0.2.0-alpha2") > 0)
        assertTrue(UpdateLogic.compareTags("kestrel-g-v0.3.0-alpha14", "kestrel-g-v0.2.0-alpha13") > 0)
        assertTrue(UpdateLogic.compareTags("kestrel-g-v0.2.0-alpha09", "kestrel-g-v0.2.0-alpha9") == 0)
        assertEquals(0, UpdateLogic.compareTags("kestrel-g-v0.2.0-alpha13", "kestrel-g-v0.2.0-alpha13"))
        for (shuffled in listOf(githubOrder, githubOrder.reversed(), githubOrder.sorted(), githubOrder.shuffled(java.util.Random(5)))) {
            assertEquals(
                listOf("kestrel-g-v0.2.0-alpha13", "kestrel-g-v0.2.0-alpha12", "kestrel-g-v0.2.0-alpha11", "kestrel-g-v0.2.0-alpha10"),
                UpdateLogic.newestTags(shuffled, "kestrel-g-v", 4),
            )
        }
        assertEquals(listOf("kestrel-v0.1.1"), UpdateLogic.newestTags(githubOrder, "kestrel-v", 4)) // the other app's tags are not mixed in
    }

    @Test fun theNewReadingFindsTheNewestReleaseForEveryInstalledVersion() {
        val tags = UpdateLogic.newestTags(githubOrder, "kestrel-g-v", 4)
        val fetched = releasesIn(tags)
        for (installed in listOf(101L, 108L, 109L, 110L, 111L)) assertEquals("installed $installed", "kestrel-g-v0.2.0-alpha13", UpdateLogic.choose(fetched, "kestrel-g-v", installed)!!.tag)
        assertNull(UpdateLogic.choose(fetched, "kestrel-g-v", 112))
    }

    @Test fun readsTheTagNamesOfAMatchingRefsAnswer() {
        val json = """[{"ref":"refs/tags/kestrel-g-v0.2.0-alpha9","node_id":"x","url":"u","object":{"sha":"a","type":"commit","url":"u"}},
            {"ref":"refs/tags/kestrel-g-v0.2.0-alpha10","node_id":"y","url":"u","object":{"sha":"b","type":"commit","url":"u"}}]"""
        assertEquals(listOf("kestrel-g-v0.2.0-alpha9", "kestrel-g-v0.2.0-alpha10"), UpdateLogic.tagsFromRefs(json))
        assertEquals(emptyList<String>(), UpdateLogic.tagsFromRefs("[]"))
        assertEquals(emptyList<String>(), UpdateLogic.tagsFromRefs("""{"message":"Not Found"}"""))
    }

    @Test fun aReleaseNamedZeroPointThreeSortsInsideTheOldEightEntryWindowSoInstalledAlpha9To13CanStillSeeIt() {
        // the rescue for versions that have the bug: GitHub lists by tag name, "0.3.0" sorts above every "0.2.0-alphaN"
        val withNew = (githubOrder + "kestrel-g-v0.3.0-alpha14").sortedDescending()
        val window = releasesIn(withNew).take(8)
        assertTrue(window.any { it.tag == "kestrel-g-v0.3.0-alpha14" })
        for (installed in listOf(108L, 110L, 112L)) assertEquals("kestrel-g-v0.3.0-alpha14", UpdateLogic.choose(window, "kestrel-g-v", installed)!!.tag)
        // and the following build keeps sorting above it (the digits stay equally long until alpha99)
        assertTrue(("kestrel-g-v0.3.0-alpha15") > ("kestrel-g-v0.3.0-alpha14"))
    }
}
