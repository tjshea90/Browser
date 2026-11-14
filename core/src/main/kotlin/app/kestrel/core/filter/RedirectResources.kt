package app.kestrel.core.filter

/**
 * The neutered stand-ins a `$redirect=` rule can name (AW1c). A rule `||ads.example/tag.js$script,redirect=noopjs` does not only block the file: the page gets a harmless file of the same kind instead, so
 * scripts and players that wait for it (a video player waiting for an ad SDK, a page that calls a tracker's function) carry on instead of hanging or throwing. The files live in the extension
 * (geckoapp/.../kestrel-shields/redirects.js, our own code, answered as `data:` URLs; its table of ids is checked against [FILES] by web-tests/redirects.test.js through the fixture
 * `mini.redirects.tsv` that ExtImageTest writes).
 *
 * A rule names a resource by one of the names the lists use ([idOf]; a `:10` priority suffix is ignored). A `redirect=` rule whose resource we do not have is still a block rule (that is what it was
 * written for); a `redirect-rule=` rule whose resource we do not have is skipped (it only ever changes HOW something already blocked is answered).
 */
object RedirectResources {
    /** id -> the resource's name in the extension's table (redirects.js); index 0 = none. Never reorder, only append: the extension indexes its table with the same numbers and the rule words carry them. */
    val FILES: Array<String> = arrayOf(
        "", "noop.js", "noop.txt", "noop.html", "noop.css", "noop.json", "1x1.gif", "2x2.png", "3x2.png", "32x32.png",
        "noop-0.1s.mp3", "noop-1s.mp4", "google-ima.js", "googletagservices_gpt.js", "googletagmanager_gtm.js", "google-analytics_analytics.js",
        "googlesyndication_adsbygoogle.js", "noop-vast.xml", "fingerprint2.js", "nobab.js", "amazon_apstag.js", "chartbeat.js", "prebid-ads.js",
    )
    /** The most ids the rule words can carry (5 bits). */
    const val MAX_ID = 31

    private val NAMES: Map<String, Int> = HashMap<String, Int>().also { m ->
        fun add(id: Int, vararg names: String) { for (n in names) m[n] = id }
        add(1, "noopjs", "noop.js")
        add(2, "nooptext", "noop.txt", "empty")
        add(3, "noopframe", "noop.html")
        add(4, "noop.css", "noopcss")
        add(5, "noopjson")
        add(6, "1x1.gif", "1x1-transparent.gif")
        add(7, "2x2.png", "2x2-transparent.png")
        add(8, "3x2.png", "3x2-transparent.png")
        add(9, "32x32.png", "32x32-transparent.png")
        add(10, "noopmp3-0.1s", "noop-0.1s.mp3")
        add(11, "noopmp4-1s", "noop-1s.mp4")
        add(12, "google-ima.js")
        add(13, "googletagservices_gpt.js", "googletagservices.com/gpt.js")
        add(14, "googletagmanager_gtm.js", "googletagmanager.com/gtm.js")
        add(15, "google-analytics_analytics.js", "google-analytics.com/analytics.js", "google-analytics_ga.js", "google-analytics.com/ga.js")
        add(16, "googlesyndication_adsbygoogle.js", "googlesyndication.com/adsbygoogle.js")
        add(17, "noopvast-2.0", "noopvast-3.0", "noopvast-4.0")
        add(18, "fingerprint2.js")
        add(19, "nobab.js", "nobab2.js", "fuckadblock.js-3.2.0", "blockadblock.js")
        add(20, "amazon_apstag.js")
        add(21, "chartbeat.js")
        add(22, "prebid-ads.js")
    }

    /** The id of the resource a list calls [name] (case-insensitive, `:priority` ignored), 0 when we have none by that name. */
    fun idOf(name: String): Int {
        var n = name.trim().lowercase()
        val colon = n.lastIndexOf(':')
        if (colon > 0 && n.substring(colon + 1).all { it.isDigit() || it == '-' }) n = n.substring(0, colon)
        return NAMES[n] ?: 0
    }
}
