package app.kestrel.core.courier

import app.kestrel.core.download.Clock
import app.kestrel.core.download.FetchException
import app.kestrel.core.download.FetchRequest
import app.kestrel.core.download.Fetcher
import java.io.IOException
import java.util.Locale

/** What "Test the phone link" found: three speeds (bytes per second, 0 = could not be measured) and one plain sentence about whether using the phone helps. */
class LinkReport(val lanBps: Long, val phoneNetBps: Long, val tabletNetBps: Long, val verdict: String, val problems: List<String>)

/**
 * Measures the three speeds that decide whether downloading on the phone is worth it (research/PHONE_OFFLOAD.md section 2): the link between the tablet and the phone,
 * the phone's own internet connection, and the tablet's own internet connection, the last two on the same address. Without these numbers nobody can promise a speed-up.
 */
object LinkTest {
    fun run(client: CourierClient, tablet: Fetcher, url: String, lanBytes: Long = 16L shl 20, netBytes: Long = 12L shl 20, clock: Clock = Clock.System): LinkReport {
        val problems = ArrayList<String>()
        var lan = 0L; var phone = 0L; var mine = 0L
        try { lan = client.speedtest(lanBytes).bytesPerSec } catch (e: CourierException) { problems.add("link: " + describe(e)) }
        try {
            val r = client.probe(url, netBytes)
            if (r.ok) phone = r.bps else problems.add("phone internet: " + (r.error ?: "answer ${r.status}"))
        } catch (e: CourierException) { problems.add("phone internet: " + describe(e)) }
        try { mine = tabletSpeed(tablet, url, netBytes, clock) } catch (e: FetchException) { problems.add("tablet internet: ${e.kind.name.lowercase()}") } catch (e: IOException) { problems.add("tablet internet: ${e.message}") }
        return LinkReport(lan, phone, mine, verdict(lan, phone, mine), problems)
    }

    private fun describe(e: CourierException): String = when (e.kind) {
        CourierException.Kind.UNREACHABLE -> "no answer from the phone"
        CourierException.Kind.TIMEOUT -> "the phone answered too slowly"
        CourierException.Kind.AUTH, CourierException.Kind.IMPOSTOR -> "the pairing code does not match"
        else -> e.kind.name.lowercase()
    }

    private fun tabletSpeed(f: Fetcher, url: String, bytes: Long, clock: Clock): Long {
        val t0 = clock.now()
        var n = 0L
        f.open(FetchRequest(url, 0, bytes - 1, null, emptyMap(), connectMs = 8_000, readMs = 10_000)).use { r ->
            if (r.status !in 200..299) throw FetchException(FetchException.Kind.PROTOCOL, "answer ${r.status}")
            val i = r.body ?: return 0L
            val buf = ByteArray(64 * 1024)
            while (n < bytes && clock.now() - t0 < 20_000) { val k = i.read(buf); if (k < 0) break; n += k }
        }
        return n * 1000 / maxOf(1L, clock.now() - t0)
    }

    fun verdict(lan: Long, phone: Long, tablet: Long): String {
        fun mb(b: Long) = String.format(Locale.US, "%.1f MB/s", b / 1_000_000.0)
        if (phone <= 0 && tablet <= 0) return "The test could not reach the internet from either device."
        if (phone <= 0) return "Your phone could not run the internet test, so the phone cannot be judged."
        if (lan <= 0) return "The link between the tablet and the phone could not be measured, so the phone cannot be judged."
        if (tablet <= 0) return "The tablet's own internet test failed while the phone's worked (${mb(phone)}): the phone is the better choice."
        val ratio = phone.toDouble() / tablet
        val effective = minOf(phone, lan)
        return when {
            ratio < 1.3 -> "No big gain expected: the tablet's own internet (${mb(tablet)}) is nearly as fast as the phone's (${mb(phone)}). The phone would still take the work off the tablet's processor."
            effective < tablet * 1.2 -> "Not worth it for speed: the phone's internet is faster (${mb(phone)} against ${mb(tablet)}), but the wifi link between phone and tablet (${mb(lan)}) is the limit."
            lan >= phone * 0.8 -> "Worth it: the phone downloads about ${String.format(Locale.US, "%.1f", ratio)} times faster (${mb(phone)} against ${mb(tablet)}) and the link between them (${mb(lan)}) keeps up."
            else -> "Partly worth it: the phone is faster (${mb(phone)} against ${mb(tablet)}), but the link between phone and tablet (${mb(lan)}) limits the copy, so expect about ${mb(effective)}."
        }
    }
}
