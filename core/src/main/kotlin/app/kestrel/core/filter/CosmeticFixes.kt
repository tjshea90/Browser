package app.kestrel.core.filter

/**
 * PERF2a (owner 2026-11-21): on pornhub.com the loading circle over a video was cut off at the bottom with Shields on and whole with Shields off; turning off ONLY "Hide ad spaces & cookie banners" made it whole (the owner's own test),
 * so one of the site's element-hiding rules from the public lists changes the player's size. (alpha90 upload of the watch page: `.watchpageAd` x3 hidden; alpha91 drops the whole group of rules around the player: the ad scripts and files stay blocked on the network, so an ad box left standing is empty.) The bundled lists give pornhub.com, among about sixty hiding rules, two that change GEOMETRY and hide no advertisement: `div.container`
 * with `margin-top:0px !important` and `.video-wrapper > #player ~ .hd.clear` (the strip after the player). Those two are dropped for that site only; every other rule stays (the page report of alpha88 names the rules that matched on a
 * page, so the next one is found the same way). A short table of exact selectors per site, never a pattern: a list that changes cannot make it drop more.
 */
object CosmeticFixes {
    private val DROP: Map<String, Set<String>> = mapOf(
        "pornhub.com" to setOf(
            "div.container", ".video-wrapper > #player ~ .hd.clear",                                  // alpha89: a top margin and the strip after the player
            ".watchpageAd", "#mobileContainer > div:not([id]):has(> .watchpageAd)", ".middleAdContainer", ".noBottom", ".ad-tabSplit",   // PERF4: the ad boxes around the player of a watch page (the owner's alpha90 upload: `.watchpageAd` x3 hidden on the watch page)
            ".mgp_overlayContainer", "div[class=\"video-wrapper\"] > .clear.hd:has(.adsbytrafficjunky)",   // the player's own overlay container and the ad row under it
        ),
    )

    /** The selector of a stored rule (a restyling rule keeps its declarations after [CosmeticSelectors.STYLE_SEP]). */
    private fun selectorOf(stored: String): String { val i = stored.indexOf(CosmeticSelectors.STYLE_SEP); return (if (i >= 0) stored.substring(0, i) else stored).trim() }

    fun drops(host: String, stored: String): Boolean {
        val h = host.lowercase().trim('.')
        val sel = selectorOf(stored)
        for ((site, list) in DROP) if ((h == site || h.endsWith(".$site")) && sel in list) return true
        return false
    }

    /** [rules] without the ones dropped for [host] (the same list when none is). */
    fun filter(host: String, rules: List<String>): List<String> = if (rules.none { drops(host, it) }) rules else rules.filter { !drops(host, it) }
}
