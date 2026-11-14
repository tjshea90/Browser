package app.kestrel.core.filter

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Throwaway probe (AX2): what do the bundled lists do on www.ally.com? Not committed. */
class ZzAllyProbeTest {
    @Test fun probe() {
        val kfs = File("../app/src/main/assets/filters/default.kfs"); val pslFile = File("../app/src/main/assets/psl.txt")
        assumeTrue(kfs.isFile && pslFile.isFile)
        val fs = FilterSetCodec.read(kfs.readBytes()); val psl = Psl.parse(pslFile.readLines().asSequence())
        val out = StringBuilder()
        for (h in listOf("www.ally.com", "ally.com", "secure.ally.com")) {
            out.append("HOST $h\n")
            for (c in fs.scriptletsFor(h)) { val d = ScriptletRules.decode(c)!!; out.append("  scriptlet ${d.name}(${d.args.joinToString(" | ")})\n") }
            out.append("  payload=${fs.scriptletPayload(h).length} chars; css=${fs.cosmeticCss(h, true, 1200, tokenMode = true, scriptlets = true).length} chars; procedural=${fs.proceduralFor(h).size}; allowed=${fs.isPageAllowed(h)}\n")
        }
        // the third parties an ordinary bank help page loads, judged as the extension would (script / xhr / image / frame / other), page = www.ally.com
        val urls = listOf(
            "https://assets.adobedtm.com/launch-EN123.min.js", "https://tags.tiqcdn.com/utag/ally/main/prod/utag.js", "https://www.googletagmanager.com/gtm.js?id=GTM-ABC",
            "https://www.google-analytics.com/analytics.js", "https://js-agent.newrelic.com/nr-loader-spa-1.1.js", "https://bam.nr-data.net/events/1/abc", "https://cdn.cookielaw.org/scripttemplates/otSDKStub.js",
            "https://consent.trustarc.com/notice?domain=ally.com", "https://nexus.ensighten.com/ally/prod/Bootstrap.js", "https://connect.facebook.net/en_US/fbevents.js", "https://static.hotjar.com/c/hotjar-1.js",
            "https://js.adsrvr.org/up_loader.1.1.0.js", "https://cdn.optimizely.com/js/123.js", "https://www.gstatic.com/recaptcha/releases/x/recaptcha__en.js", "https://ruxitagentjs.com/x.js",
            "https://ally.demdex.net/dest5.html", "https://dpm.demdex.net/id?d_visid_ver=5.2.0", "https://sstats.ally.com/b/ss/allyprod/1/JS-2.22.0/s123", "https://survey.medallia.com/x.js",
            "https://nexus.ally.com/x", "https://www.ally.com/etc/clientlibs/x.js",
        )
        val types = listOf("script" to RequestType.SCRIPT, "xhr" to RequestType.XHR, "image" to RequestType.IMAGE, "frame" to RequestType.SUBDOCUMENT, "other" to RequestType.OTHER, "ping" to RequestType.PING)
        for (u in urls) {
            val row = types.map { (n, t) -> val v = fs.check(u, "www.ally.com", t, psl); if (Verdict.isBlocked(v)) n + (if (Verdict.redirect(v) != 0) "->" + RedirectResources.FILES[Verdict.redirect(v)] else ":block") else null }.filterNotNull()
            out.append(u.take(70).padEnd(72)).append(row.joinToString(" ")).append('\n')
        }
        File(System.getProperty("java.io.tmpdir"), "ally-probe.txt").writeText(out.toString())
    }
}
