package app.kestrel.browser

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import app.kestrel.core.Kestrel

/** Skeleton placeholder; the real browser UI arrives in Phase 4. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = Kestrel.NAME })
    }
}
