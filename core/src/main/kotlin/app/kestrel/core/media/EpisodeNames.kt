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

    private class Mark(val start: Int, val end: Int, val season: Int, val episode: Int)

    // S02E05, s2e5, S02 E05, S02.E05, S02-E05 (the digits must stand alone: "ABS2E5X" is not a marker)
    private val SXXEXX = Regex("""(?<![A-Za-z0-9])S(\d{1,2})[ ._-]?E(\d{1,3})(?![0-9])""", OPTS)
    // Season 2 Episode 5, Season 2, Episode 5, Season 2 - Ep. 5, Series 2 | Episode 5
    private val SEASON_EPISODE = Regex("""(?<![A-Za-z])(?:season|series)[ ._-]*(\d{1,2})(?![0-9])[^0-9A-Za-z]{0,8}(?:episode|ep)\b[ ._-]*(\d{1,3})(?![0-9])""", OPTS)
    // Episode 5 (Season 2), Ep 5 - Season 2
    private val EPISODE_SEASON = Regex("""(?<![A-Za-z])(?:episode|ep)\b[ ._-]*(\d{1,3})(?![0-9])[^0-9A-Za-z]{0,8}(?:season|series)[ ._-]*(\d{1,2})(?![0-9])""", OPTS)
    // 2x05 (but never 1920x1080: the digits before the x must stand alone and be one or two)
    private val NXM = Regex("""(?<![0-9A-Za-z])(\d{1,2})x(\d{2,3})(?![0-9A-Za-z])""", OPTS)
    // Episode 220 (no season: anime sites)
    private val EPISODE_ONLY = Regex("""(?<![A-Za-z])(?:episode|ep)\b[ ._-]*(\d{1,3})(?![0-9])""", OPTS)
    private val YEAR_PAREN = Regex("""[(\[]\s*((?:19|20)\d{2})\s*[)\]]""")
    private val PAREN_WITH_YEAR = Regex("""\s*[(\[][^)\]]*(?:19|20)\d{2}[^)\]]*[)\]]""")

    private fun find(text: String): Mark? {
        SXXEXX.find(text)?.let { return Mark(it.range.first, it.range.last + 1, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        SEASON_EPISODE.find(text)?.let { return Mark(it.range.first, it.range.last + 1, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        EPISODE_SEASON.find(text)?.let { return Mark(it.range.first, it.range.last + 1, it.groupValues[2].toInt(), it.groupValues[1].toInt()) }
        NXM.find(text)?.let { return Mark(it.range.first, it.range.last + 1, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        EPISODE_ONLY.find(text)?.let { return Mark(it.range.first, it.range.last + 1, 0, it.groupValues[1].toInt()) }
        return null
    }

    private fun tag(m: Mark): String = (if (m.season > 0) "S%02d".format(Locale.ROOT, m.season) else "") + "E%02d".format(Locale.ROOT, m.episode)

    private val SEPARATOR = Regex("""\s+[-–—|:·•»«/]+\s+|\s*[|•»«]+\s*|\s+::\s+""")
    private val LEADING_NOISE = Regex("""^(?:watch(?:ing)?|stream(?:ing)?|download|play)\s+(?:(?:online|free|now|full|hd)\s+)*""", OPTS)
    private val TRAILING_NOISE = listOf(
        "online free", "free online", "watch online", "watch free", "watch now", "free streaming", "streaming online", "online streaming", "full movie", "full episode", "full episodes",
        "full hd", "online hd", "hd online", "free download", "english subbed", "english dubbed", "english sub", "english dub", "eng sub", "online", "streaming", "subbed", "dubbed",
    )
    private val NOISE_WORDS = setOf("watch", "watching", "stream", "streaming", "download", "play", "free", "online", "full", "hd", "movie", "movies", "film", "films", "tv", "show", "shows", "series", "episode", "episodes", "season", "seasons", "now", "new", "latest", "english", "subbed", "dubbed", "sub", "dub", "eng", "free.")
    private val PATH_NOISE = setOf("tv", "watch", "series", "serie", "show", "shows", "movie", "movies", "film", "stream", "streaming", "play", "video", "episode", "episodes", "season", "seasons", "online", "free", "embed", "player", "details", "detail", "title", "titles", "media", "view", "view", "view", "s", "e")

    /** The name a page's title and address give: "Ozark S02E05", "Naruto Shippuden E220"; null when they name no episode (a film page, a listing, a page with no title). */
    fun fromPage(pageTitle: String, pageUrl: String, pageHost: String): String? {
        val title = pageTitle.replace(Regex("""\s+"""), " ").trim()
        find(title)?.let { m ->
            val show = showBefore(title.substring(0, m.start), pageHost).ifEmpty { showFromUrl(pageUrl, pageHost) }
            if (show.isNotEmpty()) return "$show ${tag(m)}"
        }
        val slug = slugOf(pageUrl)
        find(slug)?.let { m ->
            val show = firstSegmentTitle(title, pageHost).ifEmpty { showBefore(slug.substring(0, m.start).split(' ').filter { it !in PATH_NOISE }.joinToString(" "), pageHost).let(::titleCaseIfLower) }
            if (show.isNotEmpty()) return "$show ${tag(m)}"
        }
        return null
    }

    /**
     * A page title without the site's noise: "Watch Dune: Part Two (2024) Full Movie Online Free - Doraby" -> "Dune - Part Two (2024)". The first part of the title that is not the site's name or only noise
     * words; "" when nothing is left (the caller falls back to the plain title).
     */
    fun cleanTitle(pageTitle: String, pageHost: String): String = firstSegmentTitle(pageTitle.replace(Regex("""\s+"""), " ").trim(), pageHost)

    /**
     * A release-style file name made readable: "Ozark.S02E05.1080p.WEB.H264-NTb" -> "Ozark S02E05", "Dune.Part.Two.2024.1080p.WEB-DL.DDP5.1-FLUX" -> "Dune Part Two (2024)". Null unless the name really is
     * one (an episode marker, or a year/resolution/source tag after the title): "Holiday_Clip" or "report.2023.final" are left alone.
     */
    fun fromRelease(stem: String): String? {
        val s = stem.trim()
        if (s.length < 5) return null
        val spaced = s.replace('_', ' ').replace('.', ' ').replace(Regex("""\s+"""), " ").trim()
        // an episode marker first
        val m = SXXEXX.find(spaced)?.let { Mark(it.range.first, it.range.last + 1, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
            ?: NXM.find(spaced)?.let { Mark(it.range.first, it.range.last + 1, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
            ?: SEASON_EPISODE.find(spaced)?.let { Mark(it.range.first, it.range.last + 1, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        if (m != null) {
            val show = tidyShow(spaced.substring(0, m.start))
            return if (show.isNotEmpty()) "$show ${tag(m)}" else null
        }
        // a film: Title Words 2024 1080p WEB-DL ...
        val tokens = spaced.split(' ').filter { it.isNotEmpty() }
        val tagAt = tokens.indexOfFirst { RELEASE_TAG.matches(it) }
        val yearAt = tokens.indexOfFirst { YEAR.matches(it) }.let { if (it >= 1) it else -1 }
        if (tagAt < 1 && !(yearAt >= 1 && tagAt >= 0)) return null // no tag after a title: not a release name
        val cut = listOf(tagAt, yearAt).filter { it >= 1 }.minOrNull() ?: return null
        val title = tidyShow(tokens.subList(0, cut).joinToString(" "))
        if (title.isEmpty()) return null
        return if (yearAt >= 1) "$title (${tokens[yearAt]})" else title
    }

    private val YEAR = Regex("""(?:19|20)\d{2}""")
    private val RELEASE_TAG = Regex("""(?i)(?:480p|576p|720p|1080p|1440p|2160p|4k|uhd|hdrip|webrip|web-dl|webdl|web|bluray|blu-ray|brrip|bdrip|dvdrip|hdtv|hdcam|hdts|cam|x264|x265|h264|h265|hevc|aac|ac3|dts|remux|proper|repack|extended|unrated|internal|amzn|nf|hmax|dsnp|atvp)(?:-[A-Za-z0-9]+)?""")

    /** The show or film part of a release name: no year, no stray dashes. */
    private fun tidyShow(raw: String): String {
        var s = raw.replace(Regex("""\b(?:19|20)\d{2}\b"""), " ").replace(Regex("""\s+"""), " ").trim()
        s = s.trim(' ', '-', '–', '—', '.', ',', '(', ')', '[', ']')
        return s
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
        val m = find(slug) ?: return ""
        val words = slug.substring(0, m.start).split(' ').filter { it.isNotEmpty() && it.lowercase(Locale.ROOT) !in PATH_NOISE }
        return titleCaseIfLower(showBefore(words.joinToString(" "), host))
    }

    /** The show's name out of the text in front of an episode marker ("Watch Ozark", "Doraby - Ozark (TV Series 2017-2022)"). */
    private fun showBefore(before: String, host: String): String = firstSegmentTitle(before, host, last = true)

    private fun firstSegmentTitle(text: String, host: String, last: Boolean = false): String {
        var t = text.replace(PAREN_WITH_YEAR.takeIf { false } ?: Regex("""\s*\((?:tv\s+series|series|tv|movie)[^)]*\)""", OPTS), " ")
        val segments = t.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.filterNot { isSite(it, host) }
        val pick = (if (last) segments.lastOrNull() else segments.firstOrNull()) ?: return ""
        var s = pick
        s = LEADING_NOISE.replace(s, "")
        var changed = true
        while (changed) {
            changed = false
            val trimmed = s.trimEnd(' ', '-', '–', '—', '|', ':', ',', '(', '[')
            if (trimmed.length != s.length) { s = trimmed; changed = true }
            val lower = s.lowercase(Locale.ROOT)
            for (p in TRAILING_NOISE) if (lower.endsWith(" $p") || lower == p) { s = if (lower == p) "" else s.substring(0, s.length - p.length).trimEnd(); changed = true; break }
            if (s.endsWith(" HD")) { s = s.removeSuffix(" HD"); changed = true }
        }
        if (s.split(' ').all { it.lowercase(Locale.ROOT) in NOISE_WORDS }) return ""
        return fileSafe(s)
    }

    /** True when a piece of a page title is the site's own name ("Doraby", "Doraby - Movies") or only noise words ("Watch Online Free"). */
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

    private fun titleCaseIfLower(s: String): String =
        if (s.isNotEmpty() && s == s.lowercase(Locale.ROOT)) s.split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.titlecase(Locale.ROOT) } } else s

    /** ":" reads badly in a file name (and is not allowed): "Dune: Part Two" -> "Dune - Part Two". The other characters a file system refuses are dropped. */
    private fun fileSafe(s: String): String =
        s.replace(Regex("""\s*:\s*"""), " - ").replace(Regex("""[\\/*?"<>|]+"""), " ").replace(Regex("""\s+"""), " ").trim().trim('-', ' ')
}
