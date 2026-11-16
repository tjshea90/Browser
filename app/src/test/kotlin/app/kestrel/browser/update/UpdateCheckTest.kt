package app.kestrel.browser.update

import android.content.Context
import app.kestrel.browser.Flavor
import app.kestrel.browser.data.Prefs
import java.net.ServerSocket
import java.net.Socket
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * The whole update check against a local server that behaves like GitHub does for this repository (owner report X1): the release LIST is sorted by tag name (alpha9 ... alpha2, alpha13 ... alpha10),
 * single releases and the tag list answer with ETags and 304. Proves the manager finds alpha13 from every older version, never needs more than headers on the second look, and recomputes against
 * the version installed NOW (an answer stored when an older version was installed must not make a newer one look outdated, or the reverse).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class UpdateCheckTest {
    private lateinit var server: ServerSocket
    @Volatile private var running = true
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

    /** A just-enough HTTP/1.1 server: one request per connection, ETag and If-None-Match like GitHub's. */
    private fun serve(sock: Socket) {
        sock.use {
            val input = it.getInputStream().bufferedReader(Charsets.ISO_8859_1)
            val requestLine = input.readLine() ?: return
            val headers = HashMap<String, String>()
            while (true) { val l = input.readLine() ?: break; if (l.isEmpty()) break; val c = l.indexOf(':'); if (c > 0) headers[l.substring(0, c).trim().lowercase()] = l.substring(c + 1).trim() }
            val target = requestLine.split(' ')[1]
            val path = target.substringBefore('?')
            requests.incrementAndGet(); paths.add(target)
            fun reply(code: Int, body: String?, etag: String? = null) {
                val bytes = body?.toByteArray() ?: ByteArray(0)
                val out = it.getOutputStream()
                val head = StringBuilder("HTTP/1.1 $code X\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\nContent-Type: application/json\r\n")
                if (etag != null) head.append("ETag: $etag\r\n")
                head.append("\r\n")
                out.write(head.toString().toByteArray()); out.write(bytes); out.flush()
            }
            val body: String? = when {
                path.startsWith("/git/matching-refs/tags/") -> {
                    val prefix = path.removePrefix("/git/matching-refs/tags/")
                    "[" + tagsInGithubOrder.filter { t -> t.startsWith(prefix) }.joinToString(",") { t -> """{"ref":"refs/tags/$t","node_id":"n","url":"u","object":{"sha":"s","type":"commit","url":"u"}}""" } + "]"
                }
                path.startsWith("/releases/tags/") -> path.removePrefix("/releases/tags/").takeIf { t -> t in tagsInGithubOrder }?.let { t -> releaseJson(t) }
                path == "/maven-metadata.xml" -> "<metadata><versioning><release>161.0.20261110090000</release><versions><version>160.0.20261104181552</version><version>161.0.20261110090000</version></versions></versioning></metadata>"
                path == "/releases" -> "[" + tagsInGithubOrder.take(8).joinToString(",") { t -> releaseJson(t) } + "]" // GitHub's order, and the old 8 entries
                else -> null
            }
            if (body == null) { reply(404, null); return }
            val etag = "\"" + Integer.toHexString((path + body).hashCode()) + "\""
            if (headers["if-none-match"] == etag) { notModified.incrementAndGet(); reply(304, null); return }
            reply(200, body, etag)
        }
    }

    @Before fun start() {
        server = ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"))
        Thread({ while (running) { val sock = try { server.accept() } catch (e: Exception) { break }; Thread({ try { serve(sock) } catch (e: Exception) { /* a test client hung up */ } }, "fake-github-conn").start() } }, "fake-github").apply { isDaemon = true }.start()
        UpdateManager.apiBase = "http://127.0.0.1:${server.localPort}"
        Flavor.updateTagPrefix = "kestrel-g-v"
    }

    @After fun stop() { running = false; server.close(); UpdateManager.apiBase = "https://api.github.com/repos/tjshea90/Browser"; Flavor.updateTagPrefix = "kestrel-v" }

    private fun ctx(installedCode: Long): Context {
        val c = org.robolectric.RuntimeEnvironment.getApplication()
        setInstalled(c, installedCode)
        Prefs.get(c).autoUpdateDownload = false // the check must not start a download here
        UpdateManager.init(c)
        return c
    }

    private fun setInstalled(c: Context, code: Long) {
        val sp = org.robolectric.Shadows.shadowOf(c.packageManager)
        val info = try { c.packageManager.getPackageInfo(c.packageName, 0) } catch (e: Exception) { android.content.pm.PackageInfo().also { it.packageName = c.packageName } }
        info.packageName = c.packageName
        info.longVersionCode = code
        sp.installPackage(info)
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

    @Test fun theEngineWatchLearnsTheNewestFirefoxAndCostsOnlyHeadersTheSecondTime() {
        Flavor.engineVersion = "160.0"; Flavor.engineMetaUrl = "http://127.0.0.1:${server.localPort}/maven-metadata.xml"
        try {
            val c = ctx(108L)
            assertNull(UpdateManager.engineNewest(c))
            checkNow(c)
            assertEquals("161.0.20261110090000", UpdateManager.engineNewest(c))
            assertTrue(UpdateManager.engineIsNewer(c))
            val nm = notModified.get()
            checkNow(c)
            assertTrue("the second look asked Mozilla conditionally and got a 304", notModified.get() > nm && paths.count { it == "/maven-metadata.xml" } == 2)
            assertEquals("161.0.20261110090000", UpdateManager.engineNewest(c))
            Flavor.engineVersion = "161.0"
            assertTrue("the same release is not news", !UpdateManager.engineIsNewer(c))
        } finally { Flavor.engineVersion = ""; Flavor.engineMetaUrl = "" }
    }

    @Test fun anEngineLookThatFailsNeverFailsTheAppCheck() {
        Flavor.engineVersion = "160.0"; Flavor.engineMetaUrl = "http://127.0.0.1:1/maven-metadata.xml" // nothing listens there
        try {
            val s = checkNow(ctx(108L))
            assertEquals(UpdateManager.Phase.AVAILABLE, s.phase)
        } finally { Flavor.engineVersion = ""; Flavor.engineMetaUrl = "" }
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
        setInstalled(c, 112L)
        val after = checkNow(c)
        assertEquals(UpdateManager.Phase.UP_TO_DATE, after.phase)
        assertNull(after.offer)
    }

    @Test fun upToDateWhenTheNewestIsInstalled() {
        assertEquals(UpdateManager.Phase.UP_TO_DATE, checkNow(ctx(112L)).phase)
    }
}
