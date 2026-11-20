package app.kestrel.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Security rules that live in Android code the JVM tests cannot run (the Gecko module has no unit-test source set): each one is pinned by reading the source, so a later edit that quietly undoes a fix fails here.
 * RVW3 (research/REVIEW_2026-11-21_SECURITY.md): SC1 (a page pointing Kestrel's own player at the owner's network) and SC7 (private-tab hosts in the Diagnostics that get pasted into a public repository).
 */
class SecurityRulesTest {
    private fun source(path: String): String {
        val f = listOf(path, "../$path").map { File(it) }.firstOrNull { it.exists() } ?: throw IllegalStateException("$path not found (run from the repository root or core/)")
        return f.readText()
    }

    // SC1: the film's address is the page's word and the player is the APP (the engine's local-network blocking does not apply to it)
    @Test fun kestrelsPlayerOpensOnlyHttpAddressesAndNeverOnTheWordOfAPageOnTheLocalNetwork() {
        val factory = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/PlayerFactory.kt")
        val code = factory.lines().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")       // (comments may name what was removed)
        assertFalse("the player must not use DefaultDataSource (it opens file:, content:, asset: and rawresource: addresses a page's playlist can name)", code.contains("DefaultDataSource"))
        assertTrue("the sources are fed by the HTTP data source", code.contains("val data: DataSource.Factory = http"))
        val flow = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/VideoFlow.kt")
        val start = flow.indexOf("private fun start(tab: Tab, why: WebHandOver.Why")
        assertTrue("VideoFlow.start exists", start >= 0)
        val body = flow.substring(start, flow.indexOf("NativePlayerActivity.launch(a, d.url", start))
        assertTrue("a hand-over is refused for a local address named by a public page, before anything starts: ${body.take(200)}", body.contains("WebHandOver.hostRefused(tab.url, d.url)"))
        assertTrue("and that check comes before the player or the inline player is started", body.indexOf("WebHandOver.hostRefused(") < body.indexOf("a.inline.start("))
    }

    // SC7: S6 fixed four sites; these seven still named the host of a private tab. Every Health line of the two files that names a host or cuts a URL must say "private tab" for a private tab.
    @Test fun noHealthLineNamesTheHostOfAPrivateTab() {
        for (file in listOf("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt", "geckoapp/src/main/kotlin/app/kestrel/browser/web/TabDelegates.kt")) {
            val bad = source(file).lines().withIndex().filter { (_, l) ->
                l.contains("Health.log(") && (l.contains("hostOf(") || l.contains("url.take(")) &&
                    !l.contains("isPrivateTab") && !l.contains("Health.where(") &&
                    !l.contains("Health.log(\"lab\"") &&            // the debug lab commands need FLAG_DEBUGGABLE
                    !l.contains("\"early-cover\"")                    // a tab restored from the saved session: private tabs are never saved
            }.map { (i, l) -> "$file:${i + 1}: ${l.trim().take(140)}" }
            assertTrue("a Health line names the host of a private tab:\n" + bad.joinToString("\n"), bad.isEmpty())
        }
    }
}
