package app.kestrel.browser

import android.app.Application
import app.kestrel.browser.shield.Engine

class GeckoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Flavor.secureDnsBuiltIn = true
        Engine.start(this) // background thread: loads the filter snapshot + Public Suffix List (the malware list and per-site Shields use it)
    }
}
