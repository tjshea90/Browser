package app.kestrel.core.courier

import app.kestrel.core.download.FakeServer
import app.kestrel.core.download.testBytes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList

/** The tablet's upkeep of the phone's page-load proxy, against a real phone host (real server, real proxy) on loopback. */
class ProxyLinkTest {
    private val root: File = Files.createTempDirectory("proxylink").toFile()
    private val cleanups = CopyOnWriteArrayList<() -> Unit>()
    private val loop = InetAddress.getByName("127.0.0.1")
    @After fun tearDown() { for (c in cleanups.reversed()) try { c() } catch (_: Exception) {}; root.deleteRecursively() }

    private fun phone(withProxy: Boolean = true) = Phone(FakeServer(testBytes(10)), root, cleanups, withProxy = withProxy)
    private fun client(p: Phone, key: ByteArray = TEST_KEY) = CourierClient("127.0.0.1", p.port, key)

    private fun await(what: String, ms: Long = 10_000, cond: () -> Boolean) { val end = System.currentTimeMillis() + ms; while (!cond()) { if (System.currentTimeMillis() > end) throw AssertionError("timed out: $what"); Thread.sleep(10) } }

    /** True when a CONNECT through the phone's proxy gets a "200 Connection Established" (to a site that exists), false when the proxy resets the stranger. */
    private fun proxyAnswers(port: Int, site: Int): Boolean {
        val s = Socket(loop, port); s.soTimeout = 3000
        return try {
            s.getOutputStream().write("CONNECT 127.0.0.1:$site HTTP/1.1\r\nHost: x\r\n\r\n".toByteArray())
            val b = ByteArray(64); val n = s.getInputStream().read(b)
            n > 0 && String(b, 0, n).startsWith("HTTP/1.1 200")
        } catch (e: IOException) { false } finally { s.close() }
    }

    private fun site(): Int { val ss = ServerSocket(0, 10, loop); cleanups.add { ss.close() }; Thread({ while (true) { val c = try { ss.accept() } catch (e: IOException) { break }; Thread { try { c.getInputStream().read() } catch (_: IOException) {} ; c.close() }.also { it.isDaemon = true }.start() } }).also { it.isDaemon = true }.start(); return ss.localPort }

    @Test fun theProxyOpensForTheTabletAndStaysShutForStrangers() {
        val ph = phone(); val web = site()
        assertFalse("nobody is armed yet", proxyAnswers(ph.proxyPort, web))
        val states = CopyOnWriteArrayList<ProxyLink.State>()
        val link = ProxyLink({ client(ph) }, { true }, { states.add(it) })
        link.start(); cleanups.add { link.stop() }
        await("active") { link.state is ProxyLink.State.Active }
        val a = link.state as ProxyLink.State.Active
        assertEquals("127.0.0.1", a.host); assertEquals(ph.proxyPort, a.port)
        assertTrue("armed: a CONNECT works", proxyAnswers(ph.proxyPort, web))
        assertEquals(1, states.size)
    }

    @Test fun switchingItOffClosesThePhoneAtOnce() {
        val ph = phone(); val web = site()
        var on = true
        val link = ProxyLink({ client(ph) }, { on }, { })
        link.start(); cleanups.add { link.stop() }
        await("active") { link.state is ProxyLink.State.Active }
        assertTrue(proxyAnswers(ph.proxyPort, web))
        on = false; link.refresh()
        await("off") { link.state === ProxyLink.State.Off }
        await("the phone closed its door") { !proxyAnswers(ph.proxyPort, web) }
    }

    @Test fun aPhoneThatStopsAnsweringIsReportedAndFoundAgain() {
        val ph = phone()
        var cl: CourierClient? = client(ph)
        val link = ProxyLink({ cl }, { true }, { }, retryMs = 50, maxRetryMs = 100)
        link.start(); cleanups.add { link.stop() }
        await("active") { link.state is ProxyLink.State.Active }
        // the phone vanishes (hotspot off): the tablet points at an address where nothing listens
        val dead = ServerSocket(0, 1, loop).use { it.localPort }
        cl = CourierClient("127.0.0.1", dead, TEST_KEY)
        link.refresh()
        await("unavailable") { link.state is ProxyLink.State.Unavailable }
        assertTrue((link.state as ProxyLink.State.Unavailable).why, (link.state as ProxyLink.State.Unavailable).why.contains("does not answer"))
        cl = client(ph); link.refresh()
        await("active again") { link.state is ProxyLink.State.Active }
    }

    @Test fun aCourierWithoutAProxyIsTooOldAndSaysSo() {
        val ph = phone(withProxy = false)
        val link = ProxyLink({ client(ph) }, { true }, { }, retryMs = 5000)
        link.start(); cleanups.add { link.stop() }
        await("unavailable") { link.state is ProxyLink.State.Unavailable }
        assertTrue((link.state as ProxyLink.State.Unavailable).why, (link.state as ProxyLink.State.Unavailable).why.contains("update"))
    }

    @Test fun aWrongPairingCodeIsReportedAsThat() {
        val ph = phone()
        val wrong = client(ph, ByteArray(32) { 7 })
        val link = ProxyLink({ wrong }, { true }, { }, retryMs = 5000)
        link.start(); cleanups.add { link.stop() }
        await("unavailable") { link.state is ProxyLink.State.Unavailable }
        assertTrue((link.state as ProxyLink.State.Unavailable).why, (link.state as ProxyLink.State.Unavailable).why.contains("pairing"))
    }

    @Test fun noPairingMeansDirectLoading() {
        val link = ProxyLink({ null }, { true }, { }, retryMs = 5000)
        link.start(); cleanups.add { link.stop() }
        await("unavailable") { link.state is ProxyLink.State.Unavailable }
        assertTrue((link.state as ProxyLink.State.Unavailable).why.contains("not paired"))
    }

    @Test fun theStatusReportsWhatTheProxyDid() {
        val ph = phone(); val web = site()
        val c = client(ph)
        val arm = c.armProxy(120)
        assertEquals(ph.proxyPort, arm.port)
        assertTrue(proxyAnswers(ph.proxyPort, web))
        val st = c.status()
        assertNotNull("the status has a proxy block", st.proxy)
        assertEquals(ph.proxyPort, st.proxy!!.port); assertEquals(1, st.proxy!!.armed); assertEquals(1L, st.proxy!!.tunnels)
    }

    @Test fun theRenewalKeepsItOpenPastTheFirstArming() {
        val ph = phone(); val web = site()
        val link = ProxyLink({ client(ph) }, { true }, { }, rearmEveryMs = 100)
        link.start(); cleanups.add { link.stop() }
        await("active") { link.state is ProxyLink.State.Active }
        Thread.sleep(500)                                                           // several renewals later
        assertTrue(proxyAnswers(ph.proxyPort, web))
        assertTrue("still one tablet armed, not one per renewal", client(ph).status().proxy!!.armed == 1)
    }
}
