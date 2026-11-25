package app.kestrel.core.media

/**
 * NM1 (owner message 2026-11-18: "i downloaded ozark season 1 episode 1, but it named it just ozark. Then when i went to download episode 2, it also named it just ozark ... deduce what each video should be named
 * based on different clues such as the site or link name or if i press episode 2 on the site"): what the PAGE says about WHICH video is on. Collected in the page by `kestrel-shields/names.js` (the top document
 * only), handed to [EpisodeNames.fromClues] together with the addresses of the player's frames. Every field is optional; 0 / "" = the page said nothing. The strings come from a page (untrusted): [clean] cuts them.
 */
class NameClues(
    /** The show's or film's own name as the page gives it (JSON-LD name, og:title, the heading), "" = none. */
    val show: String = "",
    /** The season / episode the page marks as the one in front (the ACTIVE item of its episode list), 0 = not told. */
    val season: Int = 0,
    val episode: Int = 0,
    /** That episode's own title ("Sugarwood"), "" = none. */
    val episodeTitle: String = "",
) {
    val isEmpty: Boolean get() = show.isEmpty() && season == 0 && episode == 0 && episodeTitle.isEmpty()
    override fun toString() = "show=\"$show\" S$season E$episode title=\"$episodeTitle\""

    companion object {
        const val MAX_TEXT = 80
        fun clean(s: String?): String = (s ?: "").replace(Regex("""[\u0000-\u001f<>]"""), " ").replace(Regex("""\s+"""), " ").trim().take(MAX_TEXT).trim()
        /** A season / episode number the page says: 1..199 for a season, 1..9999 for an episode, else 0 (nothing). */
        fun season(n: Int) = if (n in 1..199) n else 0
        fun episode(n: Int) = if (n in 1..9999) n else 0
    }
}
