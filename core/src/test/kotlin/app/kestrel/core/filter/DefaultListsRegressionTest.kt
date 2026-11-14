package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Regression guard on the REAL bundled snapshot (app/src/main/assets). Skipped when the snapshot is absent.
 * "Must not block" is the important half: a false positive here is a broken website for the user.
 */
class DefaultListsRegressionTest {
    private lateinit var fs: FilterSet
    private lateinit var psl: Psl

    @Before
    fun load() {
        val kfs = File("../app/src/main/assets/filters/default.kfs")
        val pslFile = File("../app/src/main/assets/psl.txt")
        assumeTrue("snapshot not generated", kfs.isFile && pslFile.isFile)
        fs = FilterSetCodec.read(kfs.readBytes())
        psl = Psl.parse(pslFile.readLines().asSequence())
    }

    private fun verdict(url: String, page: String, type: Int) = fs.check(url, page, type, psl)

    @Test
    fun blocksWellKnownAdAndTrackerRequests() {
        val s = RequestType.SCRIPT
        val cases = listOf(
            Triple("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js", "news.example", s),
            Triple("https://securepubads.g.doubleclick.net/tag/js/gpt.js", "news.example", s),
            Triple("https://www.google-analytics.com/analytics.js", "news.example", s),
            Triple("https://www.google-analytics.com/g/collect?v=2&tid=G-1", "news.example", RequestType.XHR),
            Triple("https://connect.facebook.net/en_US/fbevents.js", "shop.example", s),
            Triple("https://www.facebook.com/tr/?id=1&ev=PageView", "shop.example", RequestType.IMAGE),
            Triple("https://static.criteo.net/js/ld/publishertag.js", "news.example", s),
            Triple("https://cdn.taboola.com/libtrc/loader.js", "news.example", s),
            Triple("https://widgets.outbrain.com/outbrain.js", "news.example", s),
            Triple("https://c.amazon-adsystem.com/aax2/apstag.js", "news.example", s),
            Triple("https://ib.adnxs.com/ut/v3", "news.example", RequestType.XHR),
            Triple("https://sb.scorecardresearch.com/beacon.js", "news.example", s),
            Triple("https://static.hotjar.com/c/hotjar-1.js", "shop.example", s),
            Triple("https://bat.bing.com/bat.js", "shop.example", s),
            Triple("https://px.ads.linkedin.com/collect?pid=1", "shop.example", RequestType.IMAGE),
            Triple("https://analytics.tiktok.com/i18n/pixel/events.js", "shop.example", s),
        )
        val missed = cases.filter { (u, p, t) -> !Verdict.isBlocked(verdict(u, p, t)) }.map { it.first }
        assertTrue("real lists failed to block: $missed", missed.isEmpty())
    }

    /** AA3-3b: the bundled snapshot carries the lists' `$removeparam` rules, and they leave every address a page needs alone. */
    @Test
    fun theSnapshotStripsKnownTrackingParametersFromNavigationsAndNothingElse() {
        assertTrue("query rules in the snapshot: ${fs.queryRuleCount}", fs.queryRuleCount in 1800..6000)
        assertEquals("https://example.com/p?a=1", fs.stripQuery("https://example.com/p?gclid=Cj0&a=1&fbclid=IwAR&msclkid=9"))
        assertEquals("https://youtu.be/abc", fs.stripQuery("https://youtu.be/abc?is=ZZ"))
        val needed = listOf(
            "https://www.google.com/search?q=hello&hl=en", "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL1&t=30", "https://duckduckgo.com/?q=hello",
            "https://en.wikipedia.org/w/index.php?title=Web_browser&action=edit", "https://github.com/search?q=kestrel&type=repositories", "https://www.amazon.com/s?k=cables",
            "https://www.reddit.com/r/android/comments/abc/title/?sort=new", "https://mail.proton.me/u/0/inbox?page=2", "https://accounts.google.com/o/oauth2/v2/auth?client_id=1&state=2&response_type=code",
            "https://www.bbc.co.uk/search?q=news&page=2", "https://news.ycombinator.com/item?id=1", "https://www.nytimes.com/search?query=a&sort=best",
        )
        val changed = needed.filter { fs.stripQuery(it) != null }
        assertTrue("addresses a page needs were changed: $changed", changed.isEmpty())
    }

    /**
     * AW1b/e: a scriptlet runs INSIDE the page. For the sites people log in to, search, read the reference of, write code on or mail from, the bundled lists must give none at all
     * (a new list that adds one has to be looked at by a human before it reaches the tablet), and no site gets a pile of them.
     */
    @Test
    fun noScriptletRunsOnTheSitesPeopleDependOn() {
        val untouchable = listOf(
            "www.google.com", "accounts.google.com", "mail.google.com", "duckduckgo.com", "search.brave.com", "www.bing.com", "en.wikipedia.org", "github.com", "gist.github.com", "stackoverflow.com",
            "developer.android.com", "developer.mozilla.org", "www.paypal.com", "www.amazon.com", "mail.proton.me", "account.proton.me", "claude.ai", "chatgpt.com", "www.bbc.com", "news.ycombinator.com",
            "www.wikipedia.org", "play.google.com", "www.apple.com", "www.microsoft.com", "login.live.com", "outlook.live.com", "docs.google.com", "drive.google.com", "web.whatsapp.com", "www.linkedin.com",
        )
        val touched = untouchable.map { it to fs.scriptletsFor(it).map { c -> ScriptletRules.decode(c)!!.let { d -> d.name + "(" + d.args.joinToString(",") + ")" } } }.filter { it.second.isNotEmpty() }
        assertTrue("scriptlets for sites that must stay untouched: $touched", touched.isEmpty())
        // whatever a host gets, it is a handful, never a pile (the extension also caps a payload at ${ScriptletRules.MAX_CALLS} calls)
        val big = listOf("www.youtube.com", "www.facebook.com", "www.reddit.com", "www.twitch.tv", "twitter.com", "x.com", "www.nytimes.com", "www.instagram.com", "www.tiktok.com")
            .map { it to fs.scriptletsFor(it).size }.filter { it.second > 25 }
        assertTrue("a pile of scriptlets: $big", big.isEmpty())
    }

    /** AW1c: the stand-in a verdict names is one we have, and a document is never answered with one. */
    @Test
    fun standInsNameKnownResourcesAndNeverAnswerADocument() {
        val sample = listOf("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js", "https://www.googletagmanager.com/gtm.js?id=GTM-1", "https://www.google-analytics.com/analytics.js",
            "https://imasdk.googleapis.com/js/sdkloader/ima3.js", "https://securepubads.g.doubleclick.net/tag/js/gpt.js", "https://c.amazon-adsystem.com/aax2/apstag.js")
        var named = 0
        for (u in sample) for (t in listOf(RequestType.SCRIPT, RequestType.IMAGE, RequestType.XHR, RequestType.SUBDOCUMENT, RequestType.MEDIA, RequestType.OTHER)) {
            val id = Verdict.redirect(verdict(u, "news.example", t))
            assertTrue("$u type $t names resource $id", id in 0 until RedirectResources.FILES.size)
            if (id != 0) named++
        }
        val doc = Verdict.redirect(verdict("https://pagead2.googlesyndication.com/", "", RequestType.DOCUMENT))
        assertEquals("a document is never answered with a stand-in", 0, doc and 0)
        assertTrue("the real lists name some stand-ins for well-known ad files ($named)", named >= 1)
    }

    @Test
    fun neverBlocksPagesOrEssentialInfrastructure() {
        val doc = RequestType.DOCUMENT
        val pages = listOf(
            "https://en.wikipedia.org/wiki/Web_browser", "https://github.com/", "https://www.google.com/search?q=hello", "https://www.bbc.com/news",
            "https://www.reddit.com/r/android/", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "https://www.amazon.com/", "https://news.ycombinator.com/",
            "https://example.com/", "https://search.brave.com/search?q=hello", "https://duckduckgo.com/?q=hello", "https://www.nytimes.com/",
            "https://stackoverflow.com/questions/1", "https://developer.android.com/", "https://www.paypal.com/signin", "https://accounts.google.com/signin",
        )
        val blockedPages = pages.filter { Verdict.isBlocked(verdict(it, "", doc)) }
        assertTrue("main-frame navigations blocked: $blockedPages", blockedPages.isEmpty())

        val s = RequestType.SCRIPT
        val sub = listOf(
            Triple("https://fonts.googleapis.com/css2?family=Roboto", "news.example", RequestType.STYLESHEET),
            Triple("https://fonts.gstatic.com/s/roboto/v30/x.woff2", "news.example", RequestType.FONT),
            Triple("https://cdn.jsdelivr.net/npm/jquery@3/dist/jquery.min.js", "news.example", s),
            Triple("https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js", "news.example", s),
            Triple("https://code.jquery.com/jquery-3.6.0.min.js", "news.example", s),
            Triple("https://unpkg.com/react@18/umd/react.production.min.js", "news.example", s),
            Triple("https://cdnjs.cloudflare.com/ajax/libs/lodash.js/4.17.21/lodash.min.js", "news.example", s),
            Triple("https://www.google.com/recaptcha/api.js", "login.example", s),
            Triple("https://www.gstatic.com/recaptcha/releases/x/recaptcha__en.js", "login.example", s),
            Triple("https://challenges.cloudflare.com/turnstile/v0/api.js", "login.example", s),
            Triple("https://js.stripe.com/v3/", "shop.example", s),
            Triple("https://www.paypal.com/sdk/js?client-id=x", "shop.example", s),
            Triple("https://maps.googleapis.com/maps/api/js?key=x", "shop.example", s),
            Triple("https://i.ytimg.com/vi/abc/hqdefault.jpg", "www.youtube.com", RequestType.IMAGE),
            Triple("https://static.licdn.com/sc/h/x.js", "www.linkedin.com", s),
            Triple("https://cdn.shopify.com/s/files/1/x/theme.js", "shop.example", s),
            Triple("https://upload.wikimedia.org/wikipedia/commons/a/a9/x.png", "en.wikipedia.org", RequestType.IMAGE),
            Triple("https://avatars.githubusercontent.com/u/1?v=4", "github.com", RequestType.IMAGE),
            // Google Search is now the default search: its results page must keep working (shapes taken from a real mobile results page).
            Triple("https://www.google.com/xjs/_/js/k=xjs.s.en_US.abc/am=AAAA/d=1/ed=1/dg=0/ujg=1/rs=ACT90o/m=sy1,syn,sy2", "www.google.com", s),
            Triple("https://www.gstatic.com/og/_/js/k=og.qtm.en_US.abc/rt=j/m=q_dnp,qmd,qcwid,qapid/exm=qaaw/d=1/ed=1/rs=AA2YrT", "www.google.com", s),
            Triple("https://ssl.gstatic.com/gb/images/bar/al-icon.png", "www.google.com", RequestType.IMAGE),
            Triple("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTx", "www.google.com", RequestType.IMAGE),
            Triple("https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png", "www.google.com", RequestType.IMAGE),
            Triple("https://www.google.com/textinputassistant/tia.png", "www.google.com", RequestType.IMAGE),
            Triple("https://fonts.gstatic.com/s/googlesans/v58/4UaGrENHsxJlGDuGo1OIlL3Owps.woff2", "www.google.com", RequestType.FONT),
            Triple("https://apis.google.com/_/scs/abc-static/_/js/k=gapi.gapi.en.xyz/m=gapi_iframes", "www.google.com", s),
            Triple("https://www.google.com/complete/search?q=hel&client=gws-wiz", "www.google.com", RequestType.XHR),
            Triple("https://consent.google.com/ml?continue=https://www.google.com/search?q=x", "www.google.com", RequestType.SUBDOCUMENT),
        )
        val blocked = sub.filter { (u, p, t) -> Verdict.isBlocked(verdict(u, p, t)) }.map { it.first }
        assertTrue("essential resources blocked (would break sites): $blocked", blocked.isEmpty())
    }

    /**
     * Owner message 22 (2026-11-13): "Doraby doesn't load movies at all, one site said incompatible browser or the browser can't access cloudflare". Doraby's player is a cross-site frame of one of
     * six embed hosts (the theme's `Servers` object); those players and Cloudflare's visitor check (Turnstile, `challenges.cloudflare.com`) must never be refused by OUR lists. (Firefox's own
     * Disconnect "Content" list names cloudflare.com and cdn.jsdelivr.net: that blocking was removed in alpha28, research/TABLET_DIAG_2026-11-13.md section 6.)
     */
    @Test
    fun theMoviePlayersOfDorabyAndCloudflaresVisitorCheckAreNotBlocked() {
        val sub = RequestType.SUBDOCUMENT; val s = RequestType.SCRIPT; val x = RequestType.XHR
        val cases = listOf(
            Triple("https://vidfast.vc/movie/1381071", "doraby.com", sub),
            Triple("https://www.vidy.st/movie/1381071", "doraby.com", sub),
            Triple("https://vidcore.net/movie/1381071", "doraby.com", sub),
            Triple("https://embed.vidrift.net/embed/movie/1381071?autoPlay=true", "doraby.com", sub),
            Triple("https://play.xpass.top/e/movie/1381071", "doraby.com", sub),
            Triple("https://furniturebeast.nl/embed/movie/tt34384661", "doraby.com", sub),
            Triple("https://challenges.cloudflare.com/cdn-cgi/challenge-platform/h/g/turnstile/if/ov2/av0/rcv/abc/0x4AAAAAAA/auto/fbE/new/normal/auto/", "vidfast.vc", sub),
            Triple("https://challenges.cloudflare.com/turnstile/v0/api.js?onload=onloadTurnstileCallback&render=explicit", "vidfast.vc", s),
            Triple("https://challenges.cloudflare.com/cdn-cgi/challenge-platform/h/b/orchestrate/chl_api/v1?ray=9a1b2c3d4e5f", "vidfast.vc", s),
            Triple("https://challenges.cloudflare.com/cdn-cgi/challenge-platform/h/g/flow/ov1/123:456:abc/9a1b/def", "vidfast.vc", x),
            Triple("https://vidfast.vc/cdn-cgi/challenge-platform/scripts/jsd/main.js", "vidfast.vc", s),
            Triple("https://vidfast.vc/cdn-cgi/challenge-platform/h/g/jsd/oneshot/7c7fee70a876/0.8:1:abc", "vidfast.vc", x),
            Triple("https://cdn.jsdelivr.net/npm/hls.js@1.5.7/dist/hls.min.js", "vidfast.vc", s),
            Triple("https://cdnjs.cloudflare.com/ajax/libs/hls.js/1.5.7/hls.min.js", "vidfast.vc", s),
            Triple("https://cdnjs.cloudflare.com/ajax/libs/video.js/8.10.0/video.min.js", "embed.vidrift.net", s),
        )
        val blocked = cases.filter { (u, p, t) -> Verdict.isBlocked(verdict(u, p, t)) }.map { it.first }
        assertTrue("a player or a Cloudflare check would be refused by our own lists: $blocked", blocked.isEmpty())
    }

    @Test
    fun essentialResourcesSurviveEvenWhenTheRequestTypeIsAmbiguous() {
        // shouldInterceptRequest cannot always tell the type (extension-less script URLs, fetch()): such requests must still pass.
        val essential = listOf(
            "https://js.stripe.com/v3/", "https://fonts.googleapis.com/css2?family=Roboto", "https://www.google.com/recaptcha/api.js",
            "https://cdn.jsdelivr.net/npm/jquery@3/dist/jquery.min.js", "https://maps.googleapis.com/maps/api/js?key=x",
            "https://www.paypal.com/sdk/js?client-id=x", "https://challenges.cloudflare.com/turnstile/v0/api.js",
            "https://api.github.com/repos/x/y", "https://accounts.google.com/gsi/client", "https://www.gstatic.com/firebasejs/9/firebase-app.js",
        )
        for (type in listOf(RequestType.UNKNOWN, RequestType.XHR or RequestType.OTHER, RequestType.OTHER)) {
            val blocked = essential.filter { Verdict.isBlocked(verdict(it, "news.example", type)) }
            assertTrue("blocked for type mask $type: $blocked", blocked.isEmpty())
        }
    }

    @Test
    fun firstPartyLoadsOfAdvertisedDomainsStillWorkWhereListsSayNothing() {
        // Visiting the tracker's own site must work (documents are exempt from ordinary rules).
        for (u in listOf("https://www.doubleclick.net/", "https://www.criteo.com/", "https://www.taboola.com/", "https://hotjar.com/")) {
            assertEquals(u, Verdict.NONE, verdict(u, "", RequestType.DOCUMENT) and Verdict.BLOCK)
        }
    }

    @Test
    fun snapshotLooksSane() {
        assertTrue("host rules ${fs.meta.hostRules}", fs.meta.hostRules > 50_000)
        assertTrue("pattern rules ${fs.meta.patternRules}", fs.meta.patternRules > 5_000)
        assertTrue("cosmetic ${fs.meta.cosmeticSite}", fs.meta.cosmeticSite > 5_000)
        val css = fs.cosmeticCss("www.example.com")
        assertTrue("generic css should be capped, was ${css.length} chars", css.length < 200_000)
    }
}
