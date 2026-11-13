package app.kestrel.browser

import android.app.Application
import app.kestrel.browser.shield.Engine

class GeckoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app.kestrel.browser.diag.Health.mirror = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        Flavor.secureDnsBuiltIn = true
        Flavor.updateTagPrefix = "kestrel-g-v"
        Flavor.title = "Kestrel G"
        Flavor.hasScrollFeel = true
        Flavor.hasEmbedWait = true
        Flavor.hasTabBudget = true
        Flavor.hasPullRefresh = true
        Flavor.hasPopupGuard = true
        Flavor.hasNetworkLab = true
        Flavor.engineVersion = try { Class.forName("org.mozilla.geckoview.BuildConfig").getField("MOZ_APP_VERSION").get(null) as? String ?: "" } catch (e: Throwable) { "" }
        Flavor.engineMetaUrl = "https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/maven-metadata.xml"
        // Every Firefox child process (content, GPU, socket...) creates this Application too: only the UI process may load the 4 MB filter set.
        if (getProcessName() == packageName) { app.kestrel.browser.download.DownloadController.setFetcher(1, app.kestrel.browser.net.GeckoFetcher(app.kestrel.browser.download.UrlConnectionFetcher())) }
        if (getProcessName() == packageName) app.kestrel.browser.diag.StartTimeline.appCreated()
        if (getProcessName() == packageName) ProcessLife.init(this)
        if (getProcessName() == packageName) Engine.start(this) // background thread: filter snapshot + Public Suffix List (malware list, per-site Shields)
    }
}
