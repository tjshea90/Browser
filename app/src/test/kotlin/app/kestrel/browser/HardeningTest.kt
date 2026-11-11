package app.kestrel.browser

import android.content.Context
import app.kestrel.browser.web.ExternalIntents
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Random

/** Regression tests for findings of the independent security review (see docs/handoff/REVIEW-2026-11-11.md). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class HardeningTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()

    // ------------------------------------------------------------------ S1: a page must never be able to crash the browser through an intent: URL

    @Test
    fun malformedIntentUrlsAreRefusedNotThrown() {
        // Android's Intent.parseUri lets NumberFormatException (and friends) escape for these; it used to crash the UI thread.
        for (u in listOf(
            "intent:#Intent;i.x=zz;end", "intent:#Intent;l.x=99999999999999999999;end", "intent:#Intent;f.x=abc;end", "intent:#Intent;d.x=;end",
            "intent:#Intent;B.x=maybe;end", "intent:#Intent;b.x=1000;end", "intent:#Intent;c.x=zzz;end", "intent:#Intent;s.x=99999;end",
            "intent:#Intent;S.x=%zz;end", "intent:#Intent;i.=1;end", "intent:#Intent;launchFlags=0xZZ;end", "intent:#Intent;launchFlags=;end",
            "intent://x#Intent;scheme=http;i.a=1;f.b=zz;end", "intent:",
        )) {
            try { ExternalIntents.plan(ctx, u) } catch (e: Throwable) { throw AssertionError("plan() threw for $u: $e", e) }
        }
    }

    @Test
    fun randomIntentUrlsNeverThrow() {
        val rnd = Random(7)
        val parts = listOf("intent:", "#Intent", ";", "end", "i.", "l.", "f.", "d.", "B.", "b.", "c.", "s.", "S.", "x", "=", "zz", "1", "-9", "99999999999999999999",
            "action=android.intent.action.VIEW", "category=android.intent.category.BROWSABLE", "scheme=https", "package=a.b", "component=a.b/.C", "launchFlags=0x10000000",
            "launchFlags=zz", "SEL;", "//", "host", "/path", "%", "%zz", "browser_fallback_url=https%3A%2F%2Fexample.com", "S.browser_fallback_url=http%3A%2F%2Fevil")
        repeat(3000) {
            val sb = StringBuilder()
            repeat(1 + rnd.nextInt(14)) { sb.append(parts[rnd.nextInt(parts.size)]) }
            val u = sb.toString()
            try { ExternalIntents.plan(ctx, u) } catch (e: Throwable) { throw AssertionError("plan() threw for '$u': $e", e) }
        }
    }

    @Test
    fun blockedAndDangerousSchemesStillRefused() {
        for (u in listOf("javascript:alert(1)", "file:///sdcard/x", "content://x", "data:text/html,hi", "intent:#Intent;S.x=y;component=a.b/.C;end".replace("end", "scheme=file;end")))
            assertNull(u, if (u.startsWith("intent")) ExternalIntents.plan(ctx, u)?.takeIf { it.intent.component != null } else ExternalIntents.plan(ctx, u))
        assertTrue(ExternalIntents.plan(ctx, "intent://x#Intent;scheme=https;end") != null)
    }
}
