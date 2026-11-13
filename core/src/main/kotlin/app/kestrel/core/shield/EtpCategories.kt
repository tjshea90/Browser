package app.kestrel.core.shield

/**
 * The categories of Firefox's own tracking protection (GeckoView `ContentBlocking.AntiTracking`; the numbers are the shipped constants, pinned by EtpCategoriesTest) and the choice Kestrel makes.
 *
 * GeckoView's STRICT (766) is AD + ANALYTIC + SOCIAL + CONTENT + TEST + CRYPTOMINING + FINGERPRINTING + STP + EMAIL. CONTENT is Mozilla's (Disconnect's) "Content" list: not trackers but the CDNs and video players
 * pages are built from: cdn.jsdelivr.net, unpkg.com, jwpcdn.com (JW Player), Brightcove, Kaltura, Mux, Bitmovin, Uplynk, Vimeo, Cloudflare, Shopify's CDN, pinimg.com, twimg.com ... Firefox itself only gets away
 * with blocking them in Strict mode because Mozilla's service sends it allow-lists for the sites that break; a GeckoView app does not get those. Until alpha28 Kestrel blocked them for everybody: doraby.com showed no
 * photos and no icons (its lazy-loader, slider and icon font come from jsDelivr) and video players stayed on their loading symbol (research/DORABY.md). Ads and trackers are Kestrel's own lists' job (EasyList,
 * EasyPrivacy, uBlock), so everything of STRICT stays except CONTENT.
 */
object EtpCategories {
    const val AD = 2
    const val ANALYTIC = 4
    const val SOCIAL = 8
    const val CONTENT = 16
    const val TEST = 32
    const val CRYPTOMINING = 64
    const val FINGERPRINTING = 128
    const val STP = 256
    const val EMAIL = 512
    const val STRICT = 766

    /** What Kestrel runs with: Strict without the CDN / player category. */
    const val KESTREL = STRICT and CONTENT.inv()

    /** "ads, content" for Diagnostics. */
    fun names(flags: Int): String {
        val out = ArrayList<String>(3)
        if (flags and AD != 0) out.add("ads")
        if (flags and ANALYTIC != 0) out.add("analytics")
        if (flags and SOCIAL != 0) out.add("social")
        if (flags and CONTENT != 0) out.add("content (CDN / player)")
        if (flags and TEST != 0) out.add("test")
        if (flags and CRYPTOMINING != 0) out.add("cryptominer")
        if (flags and FINGERPRINTING != 0) out.add("fingerprinter")
        if (flags and STP != 0) out.add("social tracker")
        if (flags and EMAIL != 0) out.add("email tracker")
        return if (out.isEmpty()) "other" else out.joinToString(", ")
    }
}
