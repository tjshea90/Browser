package app.kestrel.browser.diag

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** AX2: a crash note that survives the crash, and the guard that keeps the passkey (WebAuthn) API off. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class CrashNoteTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()

    @Before fun clean() { ctx.filesDir.deleteRecursively(); ctx.filesDir.mkdirs() }

    @Test fun anUncaughtExceptionLeavesANoteWithTheCauseTheThreadAndTheLastEvents() {
        Health.log("gecko", "page-start", "https://www.example.com/help/")
        val inner = IllegalStateException("inner reason")
        CrashNote.write(ctx, Thread.currentThread(), RuntimeException("boom on the page", inner))
        val note = CrashNote.previous(ctx)
        assertNotNull(note)
        assertTrue(note!!, note.startsWith("crash at "))
        assertTrue(note, note.contains("java.lang.RuntimeException: boom on the page"))
        assertTrue(note, note.contains("Caused by: java.lang.IllegalStateException: inner reason"))
        assertTrue(note, note.contains("at app.kestrel.browser.diag.CrashNoteTest"))
        assertTrue("the Health ring rides along: $note", note.contains("page-start https://www.example.com/help/"))
        assertTrue(note.length <= 14_000)
    }

    @Test fun theInstalledHandlerWritesTheNoteAndThenLetsThePreviousHandlerEndTheApp() {
        val before = Thread.getDefaultUncaughtExceptionHandler()
        var previousSaw: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, e -> previousSaw = e }
        try {
            CrashNote.install(ctx)
            CrashNote.install(ctx) // a second call does not stack another handler
            val h = Thread.getDefaultUncaughtExceptionHandler()!!
            h.uncaughtException(Thread.currentThread(), NoClassDefFoundError("com/google/android/gms/fido/fido2/api/common/Algorithm"))
            assertTrue("the original handler still runs: $previousSaw", previousSaw is NoClassDefFoundError)
            assertTrue(CrashNote.previous(ctx)!!.contains("NoClassDefFoundError"))
        } finally { Thread.setDefaultUncaughtExceptionHandler(before) }
    }

    @Test fun aNoteIsForgottenAfterAWeek() {
        CrashNote.write(ctx, Thread.currentThread(), RuntimeException("old"))
        val f = File(ctx.filesDir, "last-crash.txt")
        f.setLastModified(System.currentTimeMillis() - 8L * 86_400_000)
        assertNull(CrashNote.previous(ctx))
        assertTrue(CrashNote.reportLines(ctx).none { it.contains("EARLIER CRASH") })
    }

    @Test fun theReportSaysWhatTheEngineLeftOnDisk() {
        val dir = File(ctx.filesDir, "mozilla/prof.default/minidumps").also { it.mkdirs() }
        File(dir, "a1.extra").writeText("""{"ProcessType":"default","MozCrashReason":"MOZ_CRASH(test reason)","CrashTime":"1763137000","AvailablePhysicalMemory":"123456"}""")
        File(dir, "a1.dmp").writeBytes(ByteArray(10))
        val lines = CrashNote.reportLines(ctx)
        assertTrue(lines.toString(), lines.any { it.startsWith("Engine crash reports on disk: 1") })
        assertTrue(lines.toString(), lines.any { it.contains("MozCrashReason=MOZ_CRASH(test reason)") && it.contains("ProcessType=default") })
        assertFalse("no app crash was recorded", lines.any { it.contains("EARLIER CRASH") })
    }

    @Test fun nothingOnDiskMeansNoLines() {
        assertEquals(emptyList<String>(), CrashNote.reportLines(ctx))
    }

    /**
     * The engine's Android passkey code (org.mozilla.geckoview.WebAuthnTokenManager) loads Google Play services classes (com.google.android.gms.fido) that this app must not have and the tablet
     * does not have: a page that only asks "are passkeys available?" made the engine throw NoClassDefFoundError and the whole app closed (www.ally.com, reproduced on the emulator). The API stays OFF.
     */
    @Test fun thePasskeyApiStaysOffInTheEngineConfiguration() {
        val prefs = File("../geckoapp/src/main/assets/gecko-prefs.txt")
        assertTrue("run from the app module", prefs.isFile)
        val lines = prefs.readLines().map { it.trim() }.filter { !it.startsWith("#") }
        assertTrue("security.webauth.webauthn = false must stay in gecko-prefs.txt", lines.contains("security.webauth.webauthn = false"))
    }
}
