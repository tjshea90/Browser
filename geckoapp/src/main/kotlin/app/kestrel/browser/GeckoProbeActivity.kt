package app.kestrel.browser

import android.app.Activity
import android.os.Bundle
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/** Build probe: proves GeckoView resolves, dexes and links. Replaced by the real browser activity. */
class GeckoProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = GeckoView(this)
        val session = GeckoSession()
        session.open(GeckoRuntime.create(this))
        view.setSession(session)
        setContentView(view)
        session.loadUri("https://example.com/")
    }
}
