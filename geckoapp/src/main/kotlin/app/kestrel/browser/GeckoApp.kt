package app.kestrel.browser

import android.app.Application
import app.kestrel.browser.shield.Engine

class GeckoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Flavor.secureDnsBuiltIn = true
        Flavor.updateTagPrefix = "kestrel-g-v"
        Flavor.title = "Kestrel G"
        // Every Firefox child process (content, GPU, socket...) creates this Application too: only the UI process may load the 4 MB filter set.
        if (getProcessName() == packageName) Engine.start(this) // background thread: filter snapshot + Public Suffix List (malware list, per-site Shields)
    }
}
