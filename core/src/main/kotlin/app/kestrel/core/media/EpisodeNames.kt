package app.kestrel.core.media

import java.util.Locale

/**
 * Intuitive default names for videos (owner message 26, AL6: "if it can be figured from the site or video that the video on the page is Ozark season 2 Episode 5, auto rename the file ...
 * to something intuitive such as Ozark2.5 or OzarkS02E05"). The result is "Ozark S02E05" (the form media players and libraries understand), "Naruto Shippuden E220" when the site names no
 * season, and "Dune - Part Two (2024)" for a film. It reads, in order of trust: the file's own release-style name ("Ozark.S02E05.1080p.WEB.H264-GRP"), the page title ("Watch Ozark Season 2
 * Episode 5 Online Free | Site"), the page address ("/tv/ozark/season-2/episode-5", "ozark-s2-e5"). When nothing can be worked out it says so (null / "") and the caller keeps its old name.
 * Pure: EpisodeNamesTest. The owner can always edit the name in the sheet before the download starts.
 */
object EpisodeNames {
    private val OPTS = RegexOption.IGNORE_CASE

    private class Mark(val start: Int, val season: Int, val episode: Int)

    // S02E05, s2e5, S02 E05, S02.E05, S02-E05 (the digits must stand alone: "ABS2E5X" is not a marker)
    private val SXXEXX = Regex("""(?<![A-Za-z0-9])S(\d{1,2})[ ._-]?E(\d{1,4})(?![0-9])""", OPTS)
    // Season 2 Episode 5, Season 2, Episode 5, Season 2 - Ep. 5, Series 2 | Episode 5
    private val SEASON_EPISODE = Regex("""(?<![A-Za-z])(?:season|series)[ ._-]*(\d{1,2})(?![0-9])[^0-9A-Za-z]{0,8}(?:episode|ep)\b[ ._-]*(\d{1,4})(?![0-9])""", OPTS)
    // Episode 5 (Season 2), Ep 5 - Season 2
    private val EPISODE_SEASON = Regex("""(?<![A-Za-z])(?:episode|ep)\b[ ._-]*(\d{1,4})(?![0-9])[^0-9A-Za-z]{0,8}(?:season|series)[ ._-]*(\d{1,2})(?![0-9])""", OPTS)
    // 2x05 (never 1920x1080: the digits before the x stand alone and are one or two)
    private val NXM = Regex("""(?<![0-9A-Za-z])(\d{1,2})x(\d{2,3})(?![0-9A-Za-z])""", OPTS)
    // Episode 220 (no season: anime sites)
    private val EPISODE_ONLY = Regex("""(?<![A-Za-z])(?:episode|ep)\b[ ._-]*(\d{1,4})(?![0-9])""", OPTS)

    private fun mark(text: String, withEpisodeOnly: Boolean): Mark? {
        SXXEXX.find(text)?.let { return Mark(it.range.first, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        SEASON_EPISODE.find(text)?.let { return Mark(it.range.first, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        EPISODE_SEASON.find(text)?.let { return Mark(it.range.first, it.groupValues[2].toInt(), it.groupValues[1].toInt()) }
        NXM.find(text)?.let { return Mark(it.range.first, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        if (withEpisodeOnly) EPISODE_ONLY.find(text)?.let { return Mark(it.range.first, 0, it.groupValues[1].toInt()) }
        return null
    }

    private fun tag(m: Mark): String = (if (m.season > 0) "S%02d".format(Locale.ROOT, m.season) else "") + "E%02d".format(Locale.ROOT, m.episode)

    private val SEPARATOR = Regex("""\s+[-–—|:·•»«/]+\s+|\s*[|•»«]+\s*""")
    private val LEADING_NOISE = Regex("""^(?:watch(?:ing)?|stream(?:ing)?|download|play)\s+(?:(?:online|free|now|full|hd)\s+)*""", OPTS)
    private val SERIES_NOTE = Regex("""\s*\((?:tv\s+series|series|tv\s+show|tv|movie)[^)]*\)""", OPTS)
    private val PAREN_WITH_YEAR = Regex("""\s*[(\[][^)\]]*(?:19|20)\d{2}[^)\]]*[)\]]""")
    private val TRAILING_NOISE = listOf(
        "online free", "free online", "watch online", "watch free", "watch now", "free streaming", "streaming online", "online streaming", "full movie", "full episode", "full episodes",
        "full hd", "online hd", "hd online", "free download", "english subbed", "english dubbed", "english sub", "english dub", "eng sub", "online", "streaming", "subbed", "dubbed",
    )
    private val NOISE_WORDS = setOf(
        "watch", "watching", "stream", "streaming", "download", "play", "free", "online", "full", "hd", "movie", "movies", "film", "films", "tv", "show", "shows", "series", "episode", "episodes",
        "season", "seasons", "now", "new", "latest", "english", "subbed", "dubbed", "sub", "dub", "eng",
    )
    /** Words of an address that name a kind of page, not the show. */
    private val PATH_NOISE = setOf("tv", "watch", "series", "serie", "show", "shows", "movie", "movies", "film", "stream", "streaming", "play", "video", "episode", "episodes", "season", "seasons", "online", "free", "embed", "player", "details", "detail", "title", "titles", "media", "view")

    /** The name a page's title and address give: "Ozark S02E05", "Naruto Shippuden E220"; null when they name no episode (a film page, a listing, a page with no title). */
    fun fromPage(pageTitle: String, pageUrl: String, pageHost: String): String? {
        val title = pageTitle.replace(Regex("""\s+"""), " ").trim()
        mark(title, withEpisodeOnly = true)?.let { m ->
            val show = plausible(showBefore(title.substring(0, m.start), pageHost)).ifEmpty { showFromUrl(pageUrl, pageHost) }
            if (show.isNotEmpty()) return "$show ${tag(m)}"
        }
        val slug = slugOf(pageUrl)
        mark(slug, withEpisodeOnly = true)?.let { m ->
            val show = plausible(firstSegmentTitle(title, pageHost, dropYear = true)).ifEmpty { showFromUrl(pageUrl, pageHost) }
            if (show.isNotEmpty()) return "$show ${tag(m)}"
        }
        return null
    }

    // ---- NM1: the clues of the page's own player and episode list

    /** A season and an episode in the address of a player frame or of a media file: `/tv/69740/1/2`, `/embed/tv/69740/1-2`, `?season=1&episode=2`, `&s=1&e=2`, `/tv/ozark/season-1/episode-2`, `Ozark.S01E02`. */
    class UrlMark(val season: Int, val episode: Int)

    private val TV_PATH = Regex("""/(?:tv|tvshow|tvshows|series|serie|show|shows|episode)/[^/?#]+/(\d{1,3})(?:/|-|x)(\d{1,4})(?![0-9A-Za-z])""", OPTS)
    private val TV_PATH_DASH = Regex("""/(?:tv|tvshow|series|serie|show)/[^/?#]*?-(\d{1,3})-(\d{1,4})(?:[/?#]|$)""", OPTS)
    private val Q_SEASON = Regex("""[?&](?:season|season_number|seasonnumber|ss|s)=(\d{1,3})(?![0-9])""", OPTS)
    private val Q_EPISODE = Regex("""[?&](?:episode|episode_number|episodenumber|ep|epi|e)=(\d{1,4})(?![0-9])""", OPTS)

    fun urlMark(url: String): UrlMark? {
        if (url.isEmpty()) return null
        TV_PATH.find(url)?.let { return UrlMark(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        TV_PATH_DASH.find(url)?.let { return UrlMark(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        val qs = Q_SEASON.find(url)?.groupValues?.get(1)?.toIntOrNull()
        val qe = Q_EPISODE.find(url)?.groupValues?.get(1)?.toIntOrNull()
        if (qs != null && qe != null) return UrlMark(qs, qe)
        mark(slugOf(url), withEpisodeOnly = false)?.let { if (it.season > 0) return UrlMark(it.season, it.episode) }
        return null
    }

    /** The show or film name out of a page title ("Watch Ozark Online Free - Doraby" -> "Ozark", "Ozark (TV Series 2017-2022) - Doraby" -> "Ozark"); "" when it names nothing. */
    fun showFromTitle(pageTitle: String, pageHost: String): String =
        plausible(dropSeasonTail(firstSegmentTitle(pageTitle.replace(Regex("""\s+"""), " ").trim(), pageHost, dropYear = true)))

    /** "Ozark Season 2" -> "Ozark": a season label after the name is not part of the show's name. */
    private fun dropSeasonTail(s: String): String = s.replace(Regex("""\s*[-–—:,]?\s*(?:season|series)\s*\d+.*$""", OPTS), "").trim(' ', '-', '–', '—', ':', ',')

    private val SEASON_WORD = Regex("""(?<![A-Za-z])(?:season|series)[ ._-]*(\d{1,3})(?![0-9])""", OPTS)

    /**
     * The name the page's own clues give, nearest to the person's press first: the episode the page marks as the one in front (its active list item), else the season and episode in the address of the player's
     * frame or frames (every doraby server's embed address names them: /tv/<id>/<season>/<episode>) or of the file itself. The SHOW comes from the page's own heading / og:title / JSON-LD, else its title, else
     * its address. A season the clues give but the episode not ("Season 2" in the title, "Episode 4" in the list) is joined. Null when no episode can be made out (a film: the old rules decide).
     */
    fun fromClues(clues: NameClues?, frameUrls: List<String>, mediaUrl: String, pageTitle: String, pageUrl: String, pageHost: String): String? {
        var season = clues?.season ?: 0
        var episode = clues?.episode ?: 0
        if (episode == 0) {
            for (u in frameUrls + mediaUrl) { val m = urlMark(u) ?: continue; season = if (season > 0) season else m.season; episode = m.episode; break }
        }
        if (episode == 0) return null
        if (season == 0) season = SEASON_WORD.find(pageTitle)?.groupValues?.get(1)?.toIntOrNull() ?: SEASON_WORD.find(slugOf(pageUrl))?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val show = listOf(showFromTitle(clues?.show.orEmpty(), pageHost), showFromTitle(pageTitle, pageHost), showFromUrl(pageUrl, pageHost), titleCaseIfLower(firstSegmentTitle(slugOf(pageUrl), pageHost, dropYear = true)))
            .map { fileSafe(dropSeasonTail(it)).let { s -> plausible(s) } }.firstOrNull { it.isNotEmpty() } ?: return null
        val tag = (if (season > 0) "S%02d".format(Locale.ROOT, season) else "") + "E%02d".format(Locale.ROOT, episode)
        val title = clues?.episodeTitle.orEmpty().let { t -> if (t.isNotEmpty() && mark(t, withEpisodeOnly = true) == null && !t.equals(show, true)) fileSafe(t) else "" }
        return "$show $tag" + (if (title.isNotEmpty()) " - $title" else "")
    }

    /**
     * A page title without the site's noise: "Watch Dune: Part Two (2024) Full Movie Online Free - Doraby" -> "Dune - Part Two (2024)". The first part of the title that is not the site's name or only noise
     * words; "" when nothing is left (the caller falls back to the plain title).
     */
    fun cleanTitle(pageTitle: String, pageHost: String): String = firstSegmentTitle(pageTitle.replace(Regex("""\s+"""), " ").trim(), pageHost, dropYear = false, join = true)

    /**
     * A release-style file name made readable: "Ozark.S02E05.1080p.WEB.H264-NTb" -> "Ozark S02E05", "Dune.Part.Two.2024.1080p.WEB-DL.DDP5.1-FLUX" -> "Dune Part Two (2024)". Null unless the name really is
     * one (an episode marker, or a resolution / source / codec tag after the title): "Holiday_Clip" or "report.2023.final" are left alone.
     */
    fun fromRelease(stem: String): String? {
        val spaced = stem.trim().replace('_', ' ').replace('.', ' ').replace(Regex("""\s+"""), " ").trim()
        if (spaced.length < 5) return null
        mark(spaced, withEpisodeOnly = false)?.let { m ->
            val show = tidyShow(spaced.substring(0, m.start))
            return if (show.isNotEmpty()) "$show ${tag(m)}" else null
        }
        // a film: Title Words 2024 1080p WEB-DL ...
        val tokens = spaced.split(' ').filter { it.isNotEmpty() }
        val tagAt = tokens.indexOfFirst { RELEASE_TAG.matches(it) }
        if (tagAt < 1) return null // no tag after a title: not a release name
        val yearAt = tokens.indexOfFirst { YEAR.matches(it) }.let { if (it in 1 until tagAt) it else -1 }
        val title = tidyShow(tokens.subList(0, if (yearAt >= 1) yearAt else tagAt).joinToString(" "))
        if (title.isEmpty()) return null
        return if (yearAt >= 1) "$title (${tokens[yearAt]})" else title
    }

    private val YEAR = Regex("""(?:19|20)\d{2}""")
    private val RELEASE_TAG = Regex("""(?i)(?:480p|576p|720p|1080p|1440p|2160p|4k|uhd|hdrip|webrip|web-dl|webdl|bluray|blu-ray|brrip|bdrip|dvdrip|hdtv|hdcam|hdts|x264|x265|h264|h265|hevc|aac|ac3|dts|remux|repack)(?:-[A-Za-z0-9]+)?""")

    /** The show or film part of a release name: no year (unless that is all there is), no stray dashes. */
    private fun tidyShow(raw: String): String {
        val noYear = raw.replace(Regex("""\b(?:19|20)\d{2}\b"""), " ").replace(Regex("""\s+"""), " ").trim()
        return (noYear.ifEmpty { raw }).trim(' ', '-', '–', '—', '.', ',', '(', ')', '[', ']')
    }

    // ---- page-title helpers

    private fun slugOf(url: String): String {
        val noScheme = url.substringAfter("://", url).substringBefore('#').substringBefore('?')
        val path = noScheme.substringAfter('/', "")
        val decoded = try { java.net.URLDecoder.decode(path.replace("+", "%2B"), "UTF-8") } catch (e: Exception) { path }
        return decoded.replace(Regex("""[-_/+.]+"""), " ").replace(Regex("""\s+"""), " ").trim()
    }

    private fun showFromUrl(url: String, host: String): String {
        val slug = slugOf(url)
        val m = mark(slug, withEpisodeOnly = true) ?: return ""
        val words = slug.substring(0, m.start).split(' ').filter { it.isNotEmpty() && it.lowercase(Locale.ROOT) !in PATH_NOISE }
        return plausible(titleCaseIfLower(showBefore(words.joinToString(" "), host)))
    }

    /** A show name that is not itself an episode label ("Episode 5") and not empty. */
    private fun plausible(show: String): String =
        if (show.isEmpty() || mark(show, withEpisodeOnly = true) != null || Regex("""(?i)^(?:episode|ep|season|series)\b""").containsMatchIn(show)) "" else show

    /** The show's name out of the text in front of an episode marker ("Watch Ozark", "Doraby - Ozark (TV Series 2017-2022)"). */
    private fun showBefore(before: String, host: String): String = firstSegmentTitle(before, host, last = true, dropYear = true)

    /** RVW1 N3: a notification counter the browser tab shows in front of a title: "(3) Funny cat video", "[12] Inbox". */
    private val COUNTER = Regex("""^\s*[(\[]\d{1,3}\+?[)\]]\s*""")
    /** RVW1 N1: a segment that ends with a year in brackets is a complete film title ("Dune: Part Two (2024)"): nothing after it belongs to the name. */
    private val ENDS_WITH_YEAR = Regex("""[(\[](?:19|20)\d{2}[)\]]\s*$""")

    /**
     * [last]: the show's name is the last piece before an episode marker; otherwise the first piece. [join] (RVW1 N1, film and clip titles only): "Rick Astley - Never Gonna Give You Up (Official Music Video) - YouTube"
     * lost everything after the first " - " and was saved as "Rick Astley - 720p". With [join] the first TWO pieces that are not the site's own name are kept ("Artist - Title"), and the join stops after a
     * piece that ends with a year in brackets (a complete film title).
     */
    private fun firstSegmentTitle(text: String, host: String, last: Boolean = false, dropYear: Boolean, join: Boolean = false): String {
        var t = COUNTER.replace(SERIES_NOTE.replace(text, " "), "")
        if (dropYear) t = PAREN_WITH_YEAR.replace(t, " ")
        val labels = registrableLabel(host)
        // RVW5: measured on real pages (research/REVIEW_2026-11-23_RVW5.md): "Latest Videos | CNN", "Tiny Desk : NPR", "... | TED Talk", "... - Video Dailymotion", "... - Wikimedia Commons", "Vimeo Staff Picks on Vimeo"
        // kept the site in the file name because the host's label is under four letters or the site's name is one word of a longer tail. Only pieces AFTER the first are judged that way: the first is the title.
        val segments = t.split(SEPARATOR).map { dropOnSite(it.trim(), labels) }.filter { it.isNotEmpty() }
            .filterIndexed { i, s -> !isSite(s, host) && !(i > 0 && isSiteTail(s, labels)) }
        if (join && !last) {
            val parts = ArrayList<String>()
            for (seg in segments) {
                val one = tidySegment(seg)
                if (one.isEmpty()) continue
                parts.add(one)
                if (parts.size >= 2 || ENDS_WITH_YEAR.containsMatchIn(one)) break
            }
            return parts.joinToString(" - ")
        }
        return tidySegment((if (last) segments.lastOrNull() else segments.firstOrNull()) ?: return "")
    }

    /** One piece of a title without the words around a title that are not part of it (a leading "Watch", "online free" at the end, an "HD" tag); "" when nothing is left. */
    private fun tidySegment(piece: String): String {
        var s = LEADING_NOISE.replace(piece, "")
        var changed = true
        while (changed) {
            changed = false
            val trimmed = s.trimEnd(' ', '-', '–', '—', '|', ':', ',', '(', '[')
            if (trimmed.length != s.length) { s = trimmed; changed = true }
            val lower = s.lowercase(Locale.ROOT)
            for (p in TRAILING_NOISE) if (lower == p || lower.endsWith(" $p")) { s = if (lower == p) "" else s.substring(0, s.length - p.length).trimEnd(); changed = true; break }
            if (s.endsWith(" HD")) { s = s.removeSuffix(" HD"); changed = true }
        }
        if (s.isEmpty() || s.split(' ').all { it.lowercase(Locale.ROOT) in NOISE_WORDS }) return ""
        return fileSafe(s)
    }

    /** True when a piece of a page title is the site's own name ("Doraby", "Doraby Movies") or only noise words ("Watch Online Free"). */
    private fun isSite(segment: String, host: String): Boolean {
        val words = segment.lowercase(Locale.ROOT).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty() || words.all { it in NOISE_WORDS }) return true
        val label = host.lowercase(Locale.ROOT).removePrefix("www.").substringBefore('.')
        if (label.length >= 4) {
            val squashed = segment.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
            if (squashed == label || squashed.startsWith(label) && words.drop(1).all { it in NOISE_WORDS }) return true
        }
        return false
    }

    /** Words that sit next to a site's own name in a title tail: "TED Talk", "CNN Video", "BBC News", "Wikimedia Commons", "Video Dailymotion". */
    private val SITE_FILLER = setOf("video", "videos", "talk", "talks", "news", "official", "channel", "the", "tv", "app", "network", "home", "online", "site", "plus", "commons", "archive", "library", "watch", "player")
    private val SECOND_LEVEL = setOf("co", "com", "org", "net", "gov", "edu", "ac")

    /** The site's own name from its host: the label in front of the top-level domain ("www.ted.com" -> "ted", "commons.wikimedia.org" -> "wikimedia", "news.bbc.co.uk" -> "bbc"); "" when there is none. */
    private fun registrableLabel(host: String): String {
        val parts = host.lowercase(Locale.ROOT).substringBefore(':').split('.').filter { it.isNotEmpty() }
        if (parts.size < 2) return ""
        var i = parts.size - 2
        if (parts[i] in SECOND_LEVEL && i >= 1) i--
        return parts[i].filter { it.isLetterOrDigit() }
    }

    /**
     * A piece AFTER the first that is the site's own name with at most two words around it: "TED Talk", "Wikimedia Commons", "Video Dailymotion", "CNN". The site's label (at least four letters) may stand with any two words
     * ("Internet Archive"); a SHORT label (two or three letters: ted, cnn, npr, ign, bbc) only with the filler words of [SITE_FILLER], so "Ted Lasso" on ted.com is not cut.
     */
    private fun isSiteTail(segment: String, label: String): Boolean {
        if (label.length < 2) return false
        val words = segment.lowercase(Locale.ROOT).split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotEmpty() }
        if (words.isEmpty() || words.size > 3 || label !in words) return false
        return label.length >= 4 || words.all { it == label || it in SITE_FILLER }
    }

    /** "Vimeo Staff Picks on Vimeo" -> "Vimeo Staff Picks": a trailing " on <the site>" is the site's own tag line. Only when the last word IS the site's label. */
    private fun dropOnSite(segment: String, label: String): String {
        if (label.length < 3) return segment
        val m = ON_SITE.find(segment) ?: return segment
        return if (m.groupValues[1].lowercase(Locale.ROOT).filter { it.isLetterOrDigit() } == label) segment.substring(0, m.range.first).trim() else segment
    }
    private val ON_SITE = Regex("""\s+on\s+([A-Za-z0-9][A-Za-z0-9.]*)$""", OPTS)

    private fun titleCaseIfLower(s: String): String =
        if (s.isNotEmpty() && s == s.lowercase(Locale.ROOT)) s.split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.titlecase(Locale.ROOT) } } else s

    /** ":" reads badly in a file name (and is not allowed): "Dune: Part Two" -> "Dune - Part Two". The other characters a file system refuses are dropped. */
    private fun fileSafe(s: String): String =
        s.replace(Regex("""\s*:\s*"""), " - ").replace(Regex("""[\\/*?"<>|]+"""), " ").replace(Regex("""\s+"""), " ").trim().trim('-', ' ')
}
