package app.kestrel.browser

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle

class MainActivity : BrowserActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // OPT1: the emulator lab starts Auto-tune's short run through this (exported) activity: the Auto-tune screen itself is not exported, and only a DEBUGGABLE build honours the extra
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 && intent?.getStringExtra("auto_tune") == "quick")
            startActivity(Intent(this, app.kestrel.browser.optimizer.OptimizerActivity::class.java).putExtra("auto", "quick"))
    }
}
