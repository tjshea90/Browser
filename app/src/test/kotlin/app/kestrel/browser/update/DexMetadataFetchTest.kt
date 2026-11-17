package app.kestrel.browser.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import app.kestrel.core.update.UpdateOffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

/**
 * LAB6 (compile at install): the updater's dex-metadata handling on the JVM. A local server plays GitHub's asset download; the REAL file AGP writes next to the release build is the fixture. Proves: fetched once,
 * checked (zip with primary.prof, the release's own digest), kept for the install, never fetched again; every way the file can be wrong or unwanted ends in "no file" (the install is then what it always was); and the
 * one retry without the file after the installer refuses it, remembered for two weeks, never twice in a row, never for another failure.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DexMetadataFetchTest {
    private lateinit var server: ServerSocket
    @Volatile private var running = true
    @Volatile private var status = 200
    @Volatile private var body: ByteArray = ByteArray(0)
    private val hits = AtomicInteger()
    private val real: ByteArray by lazy { DexMetadataFetchTest::class.java.getResourceAsStream("/update/agp-baseline-1.dm")!!.readBytes() }

    @Before fun start() {
        body = real
        server = ServerSocket(0, 20, java.net.InetAddress.getByName("127.0.0.1"))
        Thread({
            while (running) {
                val sock = try { server.accept() } catch (e: Exception) { break }
                Thread({ try { serve(sock) } catch (e: Exception) { /* a test client hung up */ } }, "fake-asset-conn").start()
            }
        }, "fake-asset-server").apply { isDaemon = true }.start()
        val c = ctx()
        File(c.filesDir, "updates").deleteRecursively()
        c.getSharedPreferences("kestrel_update", Context.MODE_PRIVATE).edit().clear().commit()
        UpdateManager.setStateForTest(UpdateManager.State(UpdateManager.Phase.IDLE))
    }

    @After fun stop() { running = false; try { server.close() } catch (e: Exception) { /* closed */ } }

    private fun serve(sock: Socket) {
        sock.use { s ->
            val input = s.getInputStream().bufferedReader()
            var line = input.readLine()
            while (line != null && line.isNotEmpty()) line = input.readLine()
            hits.incrementAndGet()
            val b = body
            val out = s.getOutputStream()
            out.write("HTTP/1.1 $status X\r\nContent-Length: ${b.size}\r\nConnection: close\r\n\r\n".toByteArray())
            out.write(b); out.flush()
        }
    }

    private fun ctx(): Context = org.robolectric.RuntimeEnvironment.getApplication()
    private fun sha(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
    private fun sp(c: Context) = c.getSharedPreferences("kestrel_update", Context.MODE_PRIVATE)
    private val name = "kestrel-g-0.3.0-alpha65.dm"

    private fun offer(digest: String? = sha(real), dmName: String? = name, dmUrl: String? = "http://127.0.0.1:${server.localPort}/dm", dmSize: Long = real.size.toLong()) =
        UpdateOffer("kestrel-g-v0.3.0-alpha65", "0.3.0-alpha65", 166, "kestrel-g-0.3.0-alpha65.apk", "http://127.0.0.1/apk", 95_000_000, null, "n", "p", dmName, dmUrl, dmSize, digest)

    @Test fun fetchedOnceCheckedAndKeptForTheInstall() {
        val c = ctx()
        val f = UpdateManager.ensureDm(c, offer())
        assertNotNull("the file", f)
        assertEquals(name, f!!.name)
        assertEquals(sha(real), sha(f.readBytes()))
        assertEquals(1, hits.get())
        running = false; server.close()                                 // the network is gone: the kept copy is used
        assertEquals(f.path, UpdateManager.ensureDm(c, offer())!!.path)
        assertEquals("never fetched twice", 1, hits.get())
        assertFalse("no half-written file is left", File(f.path + ".part").exists())
    }

    @Test fun aWrongChecksumIsNotUsedAndNothingIsKept() {
        val c = ctx()
        assertNull(UpdateManager.ensureDm(c, offer(digest = "0".repeat(64))))
        assertFalse(File(c.filesDir, "updates/$name").exists())
    }

    @Test fun aFileThatIsNotTheProfileZipIsNotUsed() {
        body = "<html><body>Not Found</body></html> padding padding padding padding".toByteArray()
        assertNull(UpdateManager.ensureDm(ctx(), offer(digest = null)))
        assertEquals(1, hits.get())
    }

    @Test fun aMissingFileIsNotAnError() {
        status = 404; body = "nope".toByteArray()
        assertNull(UpdateManager.ensureDm(ctx(), offer()))
    }

    @Test fun aKeptFileThatWentBadIsReplacedNotTrusted() {
        val c = ctx()
        File(c.filesDir, "updates").mkdirs()
        File(c.filesDir, "updates/$name").writeBytes(ByteArray(64) { 9 })
        val f = UpdateManager.ensureDm(c, offer())
        assertNotNull(f)
        assertEquals(sha(real), sha(f!!.readBytes()))
        assertEquals(1, hits.get())
    }

    @Test fun aReleaseWithoutTheFileAsksNobody() {
        assertNull(UpdateManager.ensureDm(ctx(), offer(dmName = null, dmUrl = null)))
        assertEquals(0, hits.get())
    }

    @Test fun anOversizedAnnouncementIsNotFetched() {
        assertNull(UpdateManager.ensureDm(ctx(), offer(dmSize = 50_000_000)))
        assertEquals(0, hits.get())
    }

    @Test fun aRefusalOfTheLastTwoWeeksKeepsTheFileOut() {
        val c = ctx()
        sp(c).edit().putLong("dm_refused_at", System.currentTimeMillis() - 3600_000L).commit()
        assertNull(UpdateManager.ensureDm(c, offer()))
        assertEquals(0, hits.get())
        sp(c).edit().putLong("dm_refused_at", System.currentTimeMillis() - 15L * 24 * 3600_000L).commit()
        assertNotNull("after two weeks it is tried again", UpdateManager.ensureDm(c, offer()))
    }

    @Test @Config(sdk = [31]) fun otherAndroidVersionsGetNoFile() {
        assertNull(UpdateManager.ensureDm(ctx(), offer()))
        assertEquals(0, hits.get())
    }

    @Test fun theRetryHappensOnceAndRemembersTheRefusal() {
        val c = ctx()
        assertFalse("no file was attached to the failed install: not ours", UpdateManager.installRetryWithoutDm(c))
        sp(c).edit().putBoolean("install_dm", true).commit()
        assertTrue(UpdateManager.installRetryWithoutDm(c))
        assertFalse(sp(c).getBoolean("install_dm", true))
        assertTrue(sp(c).getLong("dm_refused_at", 0L) > 0L)
        assertFalse("a second failure is a failure, not another retry", UpdateManager.installRetryWithoutDm(c))
    }

    private fun result(status: Int, message: String?): Intent = Intent().putExtra(PackageInstaller.EXTRA_STATUS, status).also { if (message != null) it.putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, message) }

    @Test fun theReceiverRetriesWithoutTheFileOnlyWhenTheInstallerRefusedTheFile() {
        val c = ctx()
        sp(c).edit().putBoolean("install_dm", true).commit()
        InstallResultReceiver().onReceive(c, result(PackageInstaller.STATUS_FAILURE_INVALID, "INSTALL_FAILED_BAD_DEX_METADATA: Failed to verify dex metadata"))
        assertFalse(sp(c).getBoolean("install_dm", true))
        assertTrue(sp(c).getLong("dm_refused_at", 0L) > 0L)
        assertTrue("the retry is not a failure shown to the person", UpdateManager.state.phase != UpdateManager.Phase.ERROR)
    }

    @Test fun anyOtherFailureIsReportedAsItAlwaysWas() {
        val c = ctx()
        sp(c).edit().putBoolean("install_dm", true).commit()
        InstallResultReceiver().onReceive(c, result(PackageInstaller.STATUS_FAILURE_INVALID, "INSTALL_FAILED_INVALID_APK: not an app"))
        assertTrue("the file was not blamed", sp(c).getBoolean("install_dm", false))
        assertEquals(0L, sp(c).getLong("dm_refused_at", 0L))
        assertEquals(UpdateManager.Phase.ERROR, UpdateManager.state.phase)
    }

    @Test fun aCancelByThePersonNeverTriggersTheRetry() {
        val c = ctx()
        sp(c).edit().putBoolean("install_dm", true).commit()
        InstallResultReceiver().onReceive(c, result(PackageInstaller.STATUS_FAILURE_ABORTED, "INSTALL_FAILED_BAD_DEX_METADATA"))
        assertTrue(sp(c).getBoolean("install_dm", false))
        assertEquals(0L, sp(c).getLong("dm_refused_at", 0L))
    }
}
