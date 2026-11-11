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

    @Test fun storagePlanForTheGeckoApk() {
        val mb = 1L shl 20
        // alpha numbers: 92 MB APK, 132 MB of libraries to unpack, 48 MB margin, and Android's reserve of 10 % of an 11 GB data partition
        val reserve = 1100 * mb
        val plan = UpdateLogic.storagePlan(92 * mb, 132 * mb, freeBytes = 1800 * mb, reserveBytes = reserve)
        assertEquals((1100 + 92 + 132 + 48 + 92) * mb, plan.needKeep)   // 1464
        assertEquals((1100 + 92 + 132 + 48) * mb, plan.needDrop)        // 1372
        assertEquals((2 * 92 + 132 + 48) * mb, plan.installTakes)       // 364
        assertTrue(plan.fitsKeeping); assertFalse(plan.fitsDropping); assertEquals(0L, plan.shortBy)
        val tight = UpdateLogic.storagePlan(92 * mb, 132 * mb, freeBytes = 1400 * mb, reserveBytes = reserve) // enough only without our own copy
        assertFalse(tight.fitsKeeping); assertTrue(tight.fitsDropping); assertEquals(0L, tight.shortBy)
        val short = UpdateLogic.storagePlan(92 * mb, 132 * mb, freeBytes = 900 * mb, reserveBytes = reserve) // the owner "made 900 MB": not enough on such a tablet
        assertFalse(short.fitsKeeping); assertFalse(short.fitsDropping); assertEquals(472 * mb, short.shortBy)
        assertEquals(80 * mb, UpdateLogic.roundUpTenMb(75 * mb)); assertEquals(0L, UpdateLogic.roundUpTenMb(0)); assertEquals(10 * mb, UpdateLogic.roundUpTenMb(1))
    }

    @Test fun storagePlanMatchesWhatTheEmulatorMeasured() {
        val mib = 1L shl 20
        // Android 10 emulator, 6,094,400 KiB data partition: reserve 624,066,560 B. `adb install` of a fresh app (no copy of ours on the disk), free space BEFORE the install:
        //   release APK 95.5 MiB + 187.4 MiB of libraries: passes at 1010, fails at 1000;  the same APK padded to 155.5 MiB: passes at 1130, fails at 1115 (the line rises 2 MiB per MiB of APK).
        val reserve = 624_066_560L
        fun fits(apkMiB: Double, freeMiB: Long) = UpdateLogic.storagePlan((apkMiB * mib).toLong(), (187.4 * mib).toLong(), freeBytes = freeMiB * mib, reserveBytes = reserve, marginBytes = 32 * mib).fitsKeeping
        assertTrue(fits(95.5, 1010)); assertFalse(fits(95.5, 1000))
        assertTrue(fits(155.5, 1130)); assertFalse(fits(155.5, 1115))
    }

    @Test fun storagePlanWithoutNativeLibraries() { // the WebView build: a 2 MB app on a small partition
        val mb = 1L shl 20
        val plan = UpdateLogic.storagePlan(2 * mb, 0, freeBytes = 400 * mb, reserveBytes = 300 * mb)
        assertTrue(plan.fitsKeeping); assertEquals(0L, plan.shortBy)
    }
}
