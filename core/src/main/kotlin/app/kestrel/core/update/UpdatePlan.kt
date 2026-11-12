package app.kestrel.core.update

/**
 * What the once-a-day look knows about the browser engine: Mozilla publishes every GeckoView build as a Maven artifact, and its `maven-metadata.xml` names the newest release. Kestrel G bundles
 * one engine version inside the APK (the app cannot swap it at run time), so "a newer engine exists" is information for the owner, and it takes a new app build to use it.
 * Pure functions, unit-tested; the Android side only fetches the XML (a conditional GET: an unchanged file is a 304).
 */
object EngineWatch {
    private val RELEASE = Regex("""<release>\s*([^<\s]+)\s*</release>""")
    private val VERSION = Regex("""<version>\s*([^<\s]+)\s*</version>""")

    /** The newest release version in a `maven-metadata.xml` ("161.0.20261110123456"), or null when the text names none. */
    fun newest(xml: String): String? {
        val all = VERSION.findAll(xml).map { it.groupValues[1] }.filter { major(it) > 0 }.toList()
        val best = all.maxWithOrNull { a, b -> compare(a, b) }
        val declared = RELEASE.find(xml)?.groupValues?.get(1)?.takeIf { major(it) > 0 }
        return when {
            declared == null -> best
            best == null -> declared
            else -> if (compare(declared, best) >= 0) declared else best
        }
    }

    /** "160.0.20261104181552" -> 160; 0 when the text does not start with a number. */
    fun major(v: String): Int = v.substringBefore('.').toIntOrNull() ?: 0

    private fun minor(v: String): Int = v.split('.').getOrNull(1)?.toIntOrNull() ?: 0

    /** Orders versions by Firefox major, then minor, then the build stamp at the end (digits only). */
    fun compare(a: String, b: String): Int {
        major(a).compareTo(major(b)).let { if (it != 0) return it }
        minor(a).compareTo(minor(b)).let { if (it != 0) return it }
        return build(a).compareTo(build(b))
    }

    private fun build(v: String): Long = v.split('.').drop(2).lastOrNull()?.toLongOrNull() ?: 0L

    /** True when [upstream] is a newer Firefox release than [bundled]: a higher major, or the same major with a higher minor. A rebuild of the same release does not count. */
    fun isNewer(bundled: String?, upstream: String?): Boolean {
        if (bundled.isNullOrEmpty() || upstream.isNullOrEmpty()) return false
        val mb = major(bundled); val mu = major(upstream)
        if (mb == 0 || mu == 0) return false
        return mu > mb || (mu == mb && minor(upstream) > minor(bundled))
    }

    /** "161" or "161.1": what a person calls the version. */
    fun label(v: String): String = if (minor(v) > 0) "${major(v)}.${minor(v)}" else "${major(v)}"
}

/**
 * The dialog the owner gets when the daily look finds something: which things changed and what the one button does. Built from plain values (no Android) so the wording and the "show it once"
 * rule are unit-tested.
 */
object UpdatePlan {
    enum class Action { DOWNLOAD, INSTALL, NONE }

    /** [lines] are shown one under the other; [key] names the exact set of news, so each set is shown once (the next day's check finds the same news and stays quiet). */
    class Notice(val key: String, val title: String, val lines: List<String>, val action: Action)

    /** An app update the check found. [ready]: its file is already downloaded and checked, only the install is left. */
    class AppNews(val tag: String, val versionName: String, val sizeText: String, val ready: Boolean)

    /**
     * Null when there is nothing worth interrupting for, or when this very set of news was shown before ([shownKey]). Lists are refreshed by the app on its own and only ride along as a
     * line when a dialog is shown anyway ([listsLine]); they never open a dialog alone.
     */
    fun build(appTitle: String, installedName: String, news: AppNews?, engineBundled: String?, engineNewest: String?, listsLine: String?, shownKey: String?, engineName: String = "Firefox"): Notice? {
        val engineNews = EngineWatch.isNewer(engineBundled, engineNewest)
        if (news == null && !engineNews) return null
        val key = "app:${news?.tag ?: "-"}:${if (news?.ready == true) "ready" else "new"}|eng:${if (engineNews) engineNewest?.let { EngineWatch.label(it) } else "-"}"
        if (key == shownKey) return null
        val lines = ArrayList<String>()
        if (news != null) {
            lines.add("$appTitle ${news.versionName}" + (if (news.sizeText.isNotEmpty() && !news.ready) " (${news.sizeText})" else "") + " - you have $installedName.")
        }
        if (engineNews) {
            val v = EngineWatch.label(engineNewest!!); val have = EngineWatch.label(engineBundled!!)
            lines.add("Browser engine: $engineName $v is out (this app has $have)." + if (news == null) " $appTitle will move to it in a coming update." else "")
        }
        if (!listsLine.isNullOrEmpty()) lines.add(listsLine)
        val title = when {
            news?.ready == true -> "Update ready to install"
            news != null -> "Update available"
            else -> "Newer browser engine available"
        }
        val action = when { news == null -> Action.NONE; news.ready -> Action.INSTALL; else -> Action.DOWNLOAD }
        return Notice(key, title, lines, action)
    }
}
