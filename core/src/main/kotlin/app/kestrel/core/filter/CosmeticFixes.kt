package app.kestrel.core.filter

/**
 * PERF2a (owner 2026-11-21): on pornhub.com the loading circle over a video was cut off at the bottom with Shields on and whole with Shields off; turning off ONLY "Hide ad spaces & cookie banners" made it whole (the owner's own test),
 * so one of the site's element-hiding rules from the public lists changes the player's size. The bundled lists give pornhub.com, among about sixty hiding rules, two that change GEOMETRY and hide no advertisement: `div.container`
 * with `margin-top:0px !important` and `.video-wrapper > #player ~ .hd.clear` (the strip after the player). Those two are dropped for that site only; every other rule stays (the page report of alpha88 names the rules that matched on a
 * page, so the next one is found the same way). A short table of exact selectors per site, never a pattern: a list that changes cannot make it drop more.
 */
object CosmeticFixes {
    private val DROP: Map<String, Set<String>> = mapOf(
        "pornhub.com" to setOf("div.container", ".video-wrapper > #player ~ .hd.clear"),
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
