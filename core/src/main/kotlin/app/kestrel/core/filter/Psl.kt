package app.kestrel.core.filter

import java.net.IDN

/**
 * Public Suffix List logic: "which registrable domain (eTLD+1) is this host in?" Used to decide first- vs third-party.
 * Stored as sorted 64-bit hashes (about 80 KB for the full list) instead of a HashSet<String>.
 *
 * [FALLBACK] is a small hand-written subset good enough for tests and first run; the real list is loaded with [parse]
 * (bundled as an asset by the app).
 */
class Psl private constructor(
    private val rules: LongArray,
    private val wildcardParents: LongArray,
    private val exceptions: LongArray,
) {
    private class Entry(val host: String, val site: String?)

    private val cache = arrayOfNulls<Entry>(256)

    /**
     * Registrable domain of [host] ("a.b.example.co.uk" -> "example.co.uk"). Returns [host] itself for IP literals and
     * single-label hosts, and null when the host IS a public suffix ("co.uk").
     */
    fun siteOf(host: String): String? {
        val slot = (host.hashCode() * -0x61c88647 ushr 24) and 255
        val hit = cache[slot]
        if (hit != null && hit.host == host) return hit.site
        val computed = compute(host)
        cache[slot] = Entry(host, computed)
        return computed
    }

    /** The three sorted hash tables (rules, wildcard parents, exceptions): [ExtImage] hands them to the extension's JS engine, which must answer `siteOf` identically. */
    internal fun tables(): Array<LongArray> = arrayOf(rules, wildcardParents, exceptions)

    fun isSameSite(a: String, b: String): Boolean {
        if (a == b) return true
        val sa = siteOf(a) ?: return false
        val sb = siteOf(b) ?: return false
        return sa == sb
    }

    private fun compute(host: String): String? {
        if (host.isEmpty()) return null
        if (host.indexOf(':') >= 0 || app.kestrel.core.url.NetHosts.isIpv4(host)) return host
        val n = host.length
        // label start offsets
        var labels = 1
        for (c in host) if (c == '.') labels++
        if (labels == 1) return host
        val starts = IntArray(labels)
        var k = 1
        for (i in 0 until n) if (host[i] == '.') starts[k++] = i + 1

        var suffixLabels = 1 // default rule "*": the last label is the public suffix
        for (i in 0 until labels) {
            val h = Hash.h64(host, starts[i], n)
            if (Hash.firstIndexOf(exceptions, h) >= 0) { suffixLabels = labels - i - 1; break }
            if (Hash.firstIndexOf(rules, h) >= 0) { suffixLabels = labels - i; break }
            if (i + 1 < labels && Hash.firstIndexOf(wildcardParents, Hash.h64(host, starts[i + 1], n)) >= 0) {
                suffixLabels = labels - i; break
            }
        }
        if (suffixLabels >= labels) return null // host is itself a public suffix
        return host.substring(starts[labels - suffixLabels - 1])
    }

    companion object {
        @Volatile
        private var current: Psl? = null

        /** Process-wide list; the app replaces it with the full bundled list at startup. */
        @JvmStatic
        var default: Psl
            get() = current ?: FALLBACK
            set(value) { current = value }

        /** Parse publicsuffix.org `public_suffix_list.dat` text (both ICANN and private sections are used). */
        @JvmStatic
        fun parse(lines: Sequence<String>): Psl {
            val r = ArrayList<Long>(); val w = ArrayList<Long>(); val e = ArrayList<Long>()
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("//")) continue
                val token = line.split(' ', '\t')[0]
                // 9,950 of the 10,400 lines are plain ASCII, for which IDN.toASCII changes nothing: skipping its per-label machinery trims the start-up work of the background engine thread
                val ascii = if (token.all { it.code < 128 }) token.lowercase() else try { IDN.toASCII(token, IDN.ALLOW_UNASSIGNED).lowercase() } catch (ex: IllegalArgumentException) { continue }
                when {
                    ascii.startsWith("!") -> e.add(Hash.h64(ascii.substring(1)))
                    ascii.startsWith("*.") -> w.add(Hash.h64(ascii.substring(2)))
                    else -> r.add(Hash.h64(ascii))
                }
            }
            return Psl(sortedArray(r), sortedArray(w), sortedArray(e))
        }

        private fun sortedArray(l: List<Long>): LongArray {
            val a = LongArray(l.size) { l[it] }
            a.sort()
            return a
        }

        /** Multi-label suffixes people actually meet; single-label TLDs are covered by the default "*" rule. */
        private const val FALLBACK_TEXT = """
co.uk org.uk me.uk ac.uk gov.uk ltd.uk plc.uk net.uk sch.uk nhs.uk police.uk
co.jp ne.jp or.jp ac.jp go.jp ad.jp ed.jp gr.jp lg.jp
com.au net.au org.au edu.au gov.au asn.au id.au co.nz org.nz net.nz govt.nz ac.nz school.nz
co.za org.za net.za gov.za ac.za com.br net.br org.br gov.br edu.br com.cn net.cn org.cn gov.cn edu.cn
com.mx org.mx gob.mx edu.mx com.ar gob.ar com.co com.pe com.ve com.tr org.tr gov.tr edu.tr com.tw org.tw gov.tw edu.tw
com.hk org.hk gov.hk edu.hk com.sg org.sg gov.sg edu.sg com.my org.my gov.my edu.my
co.in net.in org.in ac.in gov.in firm.in gen.in ind.in co.kr or.kr go.kr ac.kr ne.kr co.id or.id go.id ac.id web.id
com.ph org.ph gov.ph edu.ph com.vn net.vn org.vn gov.vn edu.vn com.pk org.pk gov.pk edu.pk com.ng org.ng gov.ng edu.ng
co.il org.il ac.il gov.il com.ua org.ua gov.ua com.pl org.pl net.pl gov.pl com.es org.es com.pt org.pt com.gr org.gr
com.eg org.eg gov.eg com.sa org.sa gov.sa com.ar com.bd org.bd com.lk co.th or.th ac.th go.th in.th co.ke or.ke
github.io gitlab.io blogspot.com blogspot.co.uk appspot.com herokuapp.com vercel.app netlify.app pages.dev workers.dev
web.app firebaseapp.com cloudfront.net amazonaws.com s3.amazonaws.com azurewebsites.net wordpress.com tumblr.com
myshopify.com fly.dev onrender.com glitch.me repl.co surge.sh bitbucket.io readthedocs.io weebly.com wixsite.com
"""
        val FALLBACK: Psl by lazy {
            parse(FALLBACK_TEXT.split(' ', '\n', '\t').asSequence().filter { it.isNotBlank() })
        }
    }
}
