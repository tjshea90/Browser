package app.kestrel.core.livediag

/**
 * ND1d (owner 2026-11-27, "Use the newest diagnostics and improve the browser as much as possible"; research/DIAG_2026-11-27.md section 4): claude.ai keeps the page's processor busy for 90 percent of a 12 to 25
 * second first paint, and no setting moves it, because nobody can see WHAT it is busy with: on this tablet the page process is hidden from the app's own CPU meter (Android 10 hides other users' processes) and
 * only the engine itself can sample it. GeckoView has a profiler (`ProfilerController`); this is the rule for using it: one tap in Settings, on a normal tab only, never while a private tab exists (the
 * recording covers the whole page process), never for a sensitive site, only with the owner's live-diagnostics switch and token (the file goes to the PRIVATE repository and nowhere else).
 *
 * Pure: the Android side (geckoapp `PageProfiler`) asks [whyNot] first and uses the constants.
 */
object ProfileRule {
    /** After the page's load finished, keep recording this long (late scripts, the first interaction). */
    const val LOAD_TAIL_MS = 4_000L
    /** A recording of a load never runs longer than this (the engine's buffer is finite and the tablet is slow). */
    const val LOAD_MAX_MS = 100_000L
    /** "What I do now": this long, with no reload (typing, scrolling). */
    const val NOW_MS = 25_000L
    /** A recording bigger than this stays on the tablet (the upload is base64 inside a JSON request on a hotspot). */
    const val MAX_BYTES = 12 * 1024 * 1024

    /** The threads the engine samples (name parts; a thread whose name contains one of them): the page's main thread, its workers and style threads, drawing, network and the disk cache. */
    val THREADS = arrayOf("GeckoMain", "DOM Worker", "StyleThread", "Compositor", "Renderer", "Socket Thread", "Cache2 I/O", "URL Classifier", "Classifier Update", "IPDL Background", "AndroidUI")

    /** What it records per sample: script stacks (with the script's file) and the processor time each thread used. No native stack walking: it slows a weak processor down and cannot be read without symbols. */
    val FEATURES = arrayOf("js", "cpu")

    /** Why a recording may NOT start now, in the words the person sees; empty when it may. */
    fun whyNot(privateTab: Boolean, privateOpen: Boolean, url: String, diagOn: Boolean, hasToken: Boolean, recording: Boolean): String {
        if (recording) return "A recording is already running."
        if (!diagOn || !hasToken) return "Turn on Live diagnostics and save the GitHub token first (this section): the recording is sent to your private repository, nowhere else."
        if (privateTab) return "Not in a private tab: nothing from a private tab is recorded."
        if (privateOpen) return "Close the private tabs first: the recording covers the whole page process, private tabs too."
        if (!url.startsWith("https://") && !url.startsWith("http://")) return "Open the page you want to record first."
        val host = url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#').substringAfterLast('@').substringBefore(':').lowercase()
        if (isLocal(host)) return "Not for a page on this tablet or on a local network."
        return if (DiagScrub.site(url) == DiagScrub.SENSITIVE) "Not for sign-in, bank, mail, health, government or other sensitive sites." else ""
    }

    /** A page whose address is a number, `localhost` or a local-network name: the tablet itself, the router, a lab server. Nothing of those is worth recording and some of it is private. */
    internal fun isLocal(host: String): Boolean =
        host.isEmpty() || host == "localhost" || host.startsWith("[") || Regex("""\d{1,3}(\.\d{1,3}){3}""").matches(host) || host.endsWith(".local") || host.endsWith(".lan") || host.endsWith(".internal") || !host.contains('.')

    /** The name a recording gets in the private repository: profiles/<date>/<time>-<install>.json.gz (a new name each time; the companion file next to it ends in .meta.json). */
    fun path(atUtc: String, installId: String, attempt: Int = 0): String {
        val date = atUtc.substringBefore('T').filter { it.isDigit() || it == '-' }.ifEmpty { "undated" }
        val time = atUtc.substringAfter('T', "").filter { it.isDigit() }.take(6).ifEmpty { "000000" }
        return "profiles/$date/$time-${installId.filter { it.isLetterOrDigit() }.take(12)}" + (if (attempt > 0) "-" + (attempt + 1) else "") + ".json.gz"
    }

    fun metaPath(profilePath: String): String = profilePath.removeSuffix(".json.gz") + ".meta.json"
}
