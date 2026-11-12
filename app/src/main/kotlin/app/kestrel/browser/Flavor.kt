package app.kestrel.browser

/** Facts about the engine build that shared UI text must get right. The WebView build keeps the defaults; the Gecko build sets them at startup. */
object Flavor {
    /** The engine encrypts DNS lookups itself (DNS over HTTPS), so Settings must not tell the user to configure Private DNS system-wide. */
    @JvmField var secureDnsBuiltIn = false

    /** Which GitHub Releases belong to THIS app: tags start with this ("kestrel-v" = Kestrel on the WebView, "kestrel-g-v" = Kestrel G). Used by the in-app updater. */
    @JvmField var updateTagPrefix = "kestrel-v"

    /** The app's name as shown in the updater ("Kestrel" / "Kestrel G"). */
    @JvmField var title = "Kestrel"

    /** The engine has the "Scroll feel" prefs (Gecko build only): Settings shows the row. */
    @JvmField var hasScrollFeel = false

    /** The engine keeps tabs live by a memory budget (Gecko build): Settings shows the "Tabs kept ready" row. */
    @JvmField var hasTabBudget = false

    /** The engine view reports pull-down gestures (Gecko build only): Settings shows the "Pull down to refresh" row. */
    @JvmField var hasPullRefresh = false

    /** Video and social embeds can wait for a tap (Gecko build only: the built-in extension does it): Shields and Settings show the "Embeds wait for a tap" switch. */
    @JvmField var hasEmbedWait = false

    /** The pop-up and forced-redirect guard runs in the Gecko extension only (Gecko build): Settings shows its switch. */
    @JvmField var hasPopupGuard = false

    /** The browser engine bundled in this build ("160.0", empty on the WebView build, whose engine is the system's) and where Mozilla lists the newest one; the daily look tells the owner when a newer one exists. */
    @JvmField var engineVersion = ""
    @JvmField var engineMetaUrl = ""

    /** The engine's network switches (DNS over HTTPS, HTTP/3) can be changed live (Gecko build): Settings shows the "Network lab" rows. */
    @JvmField var hasNetworkLab = false
}
