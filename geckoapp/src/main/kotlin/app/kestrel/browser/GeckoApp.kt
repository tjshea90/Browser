package app.kestrel.browser

import android.app.Application
import app.kestrel.browser.shield.Engine

class GeckoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // LAB5 (research/LAB5_STARTUP.md row 1): ask for the preference files the first screen reads NOW. Android parses a file on its own thread from the first request, so the XML is read while the activity is being built
        // instead of the UI thread waiting for the rest of it inside onCreate (kestrel = the settings, kestrel_update = the update check, kestrel_video = the decoder list). Only the UI process: the engine's child processes never read them.
        if (getProcessName() == packageName) for (n in arrayOf("kestrel", "kestrel_update", "kestrel_video")) getSharedPreferences(n, MODE_PRIVATE)
        app.kestrel.browser.diag.Health.mirror = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        Flavor.secureDnsBuiltIn = true
        Flavor.updateTagPrefix = "kestrel-g-v"
        Flavor.title = "Kestrel"
        Flavor.hasScrollFeel = true
        Flavor.hasEmbedWait = true
        Flavor.hasTabBudget = true
        Flavor.hasPullRefresh = true
        Flavor.hasPopupGuard = true
        Flavor.hasMiniNav = true
        Flavor.hasScriptlets = true
        Flavor.hasAdSkipper = true
        Flavor.hasTapSeek = true
        Flavor.hasFullscreenTurn = true
        Flavor.hasVideoBar = true
        Flavor.hasNetworkLab = true
        Flavor.engineVersion = try { Class.forName("org.mozilla.geckoview.BuildConfig").getField("MOZ_APP_VERSION").get(null) as? String ?: "" } catch (e: Throwable) { "" }
        Flavor.engineMetaUrl = "https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/maven-metadata.xml"
        // Every Firefox child process (content, GPU, socket...) creates this Application too: only the UI process may load the 4 MB filter set.
        if (getProcessName() == packageName) { app.kestrel.browser.download.DownloadController.setFetcher(1, app.kestrel.browser.net.GeckoFetcher(app.kestrel.browser.download.UrlConnectionFetcher())) }
        if (getProcessName() == packageName) app.kestrel.browser.diag.CrashNote.install(this) // AX2: an uncaught exception leaves a note the next Diagnostics shows
        if (getProcessName() == packageName) app.kestrel.browser.diag.StartTimeline.appCreated()
        if (getProcessName() == packageName) ProcessLife.init(this)
        if (getProcessName() == packageName) Engine.start(this) // background thread: filter snapshot + Public Suffix List (malware list, per-site Shields)
        if (getProcessName() == packageName) GeckoEngine.prewarmConfig(this) // LAB5: SnakeYAML (GeckoView's config reader) is loaded off the UI thread while the activity is being built
    }
}
