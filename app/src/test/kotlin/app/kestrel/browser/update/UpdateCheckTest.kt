package app.kestrel.browser.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.kestrel.browser.Flavor
import app.kestrel.browser.data.Prefs
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

/**
 * The whole update check against a local server that behaves like GitHub does for this repository (owner report X1): the release LIST is sorted by tag name (alpha9 ... alpha2, alpha13 ... alpha10),
 * single releases and the tag list answer with ETags and 304. Proves the manager finds alpha13 from every older version, never needs more than headers on the second look, and recomputes against
 * the version installed NOW (an answer stored when an older version was installed must not make a newer one look outdated, or the reverse).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class UpdateCheckTest {
    private lateinit var server: HttpServer
    private val requests = AtomicInteger()
    private val notModified = AtomicInteger()
    private val paths = java.util.Collections.synchronizedList(ArrayList<String>())
    private val sha = "b47fdf4cb02d3a2f5fe0a0761d503a0e81abf636dce81260a81aa5a9b668e8ce"

    private val tagsInGithubOrder = listOf(
        "kestrel-v0.1.1", "kestrel-g-v0.2.0-alpha9", "kestrel-g-v0.2.0-alpha8", "kestrel-g-v0.2.0-alpha7", "kestrel-g-v0.2.0-alpha6", "kestrel-g-v0.2.0-alpha5", "kestrel-g-v0.2.0-alpha4",
        "kestrel-g-v0.2.0-alpha3", "kestrel-g-v0.2.0-alpha2", "kestrel-g-v0.2.0-alpha13", "kestrel-g-v0.2.0-alpha12", "kestrel-g-v0.2.0-alpha11", "kestrel-g-v0.2.0-alpha10",
    )

    private fun codeOf(tag: String) = if (tag == "kestrel-v0.1.1") 2 else 99 + tag.removePrefix("kestrel-g-v0.2.0-alpha").toInt()

    private fun releaseJson(tag: String): String {
        val code = codeOf(tag)
        val name = tag.removePrefix("kestrel-g-v").removePrefix("kestrel-v")
        return """{"tag_name":"$tag","name":"$tag","draft":false,"prerelease":true,"html_url":"http://x/r/$tag",
            "body":"Note for $tag.\n\n**Kestrel G $name** (versionCode $code). About.\n\n- APK SHA-256: `$sha`\n",
            "assets":[{"name":"kestrel-g-$name.apk","size":92000000,"digest":null,"browser_download_url":"http://127.0.0.1/$tag.apk"}]}"""
    }

    private fun answer(ex: HttpExchange, body: String) {
        val path = ex.requestURI.toString()
        requests.incrementAndGet(); paths.add(path)
        val etag = "\"" + Integer.toHexString((path + body).hashCode()) + "\""
        if (ex.requestHeaders.getFirst("If-None-Match") == etag) { notModified.incrementAndGet(); ex.sendResponseHeaders(304, -1); ex.close(); return }
        val bytes = body.toByteArray()
        ex.responseHeaders.add("ETag", etag)
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(200, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }

    @Before fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex ->
            val path = ex.requestURI.path
            when {
                path.startsWith("/git/matching-refs/tags/") -> {
                    val prefix = path.removePrefix("/git/matching-refs/tags/")
                    answer(ex, "[" + tagsInGithubOrder.filter { it.startsWith(prefix) }.joinToString(",") { """{"ref":"refs/tags/$it","node_id":"n","url":"u","object":{"sha":"s","type":"commit","url":"u"}}""" } + "]")
                }
                path.startsWith("/releases/tags/") -> {
                    val tag = path.removePrefix("/releases/tags/")
                    if (tag in tagsInGithubOrder) answer(ex, releaseJson(tag)) else { requests.incrementAndGet(); ex.sendResponseHeaders(404, -1); ex.close() }
                }
                path == "/releases" -> answer(ex, "[" + tagsInGithubOrder.take(8).joinToString(",") { releaseJson(it) } + "]") // the list answers in GitHub's order, and the old 8 entries
                else -> { ex.sendResponseHeaders(404, -1); ex.close() }
            }
        }
        server.start()
        UpdateManager.apiBase = "http://127.0.0.1:${server.address.port}"
        Flavor.updateTagPrefix = "kestrel-g-v"
    }

    @After fun stop() { server.stop(0); UpdateManager.apiBase = "https://api.github.com/repos/tjshea90/Browser"; Flavor.updateTagPrefix = "kestrel-v" }

    private fun ctx(installedCode: Long): Context {
        val c = ApplicationProvider.getApplicationContext<Context>()
        val info = c.packageManager.getPackageInfo(c.packageName, 0)
        org.robolectric.Shadows.shadowOf(c.packageManager).also { it.getInternalMutablePackageInfo(c.packageName).longVersionCode = installedCode }
        Prefs.get(c).autoUpdateDownload = false // the check must not start a download here
        UpdateManager.init(c)
        assertNotNull(info)
        return c
    }

    private fun checkNow(c: Context): UpdateManager.State {
        UpdateManager.check(c, manual = true)
        val end = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < end) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            val p = UpdateManager.state.phase
            if (p != UpdateManager.Phase.CHECKING && p != UpdateManager.Phase.IDLE) return UpdateManager.state
            Thread.sleep(20)
        }
        throw AssertionError("the check did not finish: ${UpdateManager.state.phase}")
    }

    @Test fun findsAlpha13FromAnOlderVersionEvenThoughGithubListsItTenth() {
        val s = checkNow(ctx(108L)) // alpha9: the version that said "up to date"
        assertEquals(UpdateManager.Phase.AVAILABLE, s.phase)
        assertEquals("kestrel-g-v0.2.0-alpha13", s.offer!!.tag)
        assertEquals(112L, s.offer!!.versionCode)
        assertTrue("it asked for the tag list and single releases, not only the first page of the list: $paths", paths.any { it.startsWith("/git/matching-refs/tags/kestrel-g-v") } && paths.any { it == "/releases/tags/kestrel-g-v0.2.0-alpha13" })
    }

    @Test fun theSecondLookCostsOnlyHeadersAndStillDecidesAgainstTheVersionInstalledNow() {
        val c = ctx(108L)
        assertEquals(UpdateManager.Phase.AVAILABLE, checkNow(c).phase)
        val before = requests.get(); val nm = notModified.get()
        val again = checkNow(c)
        assertEquals(UpdateManager.Phase.AVAILABLE, again.phase)
        assertEquals("every request of the second look was conditional and answered 304", requests.get() - before, notModified.get() - nm)
        assertTrue(requests.get() - before >= 4)
        // the owner installs alpha13 by hand; the stored answers (headers-only, 304) must now read "up to date"
        org.robolectric.Shadows.shadowOf(c.packageManager).getInternalMutablePackageInfo(c.packageName).longVersionCode = 112L
        val after = checkNow(c)
        assertEquals(UpdateManager.Phase.UP_TO_DATE, after.phase)
        assertNull(after.offer)
    }

    @Test fun upToDateWhenTheNewestIsInstalled() {
        assertEquals(UpdateManager.Phase.UP_TO_DATE, checkNow(ctx(112L)).phase)
    }
}
