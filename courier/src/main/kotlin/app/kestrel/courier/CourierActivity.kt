package app.kestrel.courier

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import java.util.Locale

/** The one screen: is the phone ready, which code to type on the tablet, may it run with the screen off, and what it is downloading right now. */
class CourierActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private val body = LinearLayout(this)
    /** Where the pieces are added while a screen is being built (a scratch container; [body] is only touched when the content really changed). */
    private var target: LinearLayout = body
    private val sig = StringBuilder()
    private var lastSig = ""
    private lateinit var prefs: CourierPrefs
    private var resumed = false

    private val BG = 0xFF000000.toInt()
    private val CARD = 0xFF07121A.toInt()
    private val FG = 0xFFE6F8FF.toInt()
    private val MUTED = 0xFF7FA3B3.toInt()
    private val ACCENT = 0xFF14D4F8.toInt()
    private val ON_ACCENT = 0xFF001018.toInt()
    private val OK = 0xFF4DE3A5.toInt()
    private val WARN = 0xFFFFB84D.toInt()
    private val DANGER = 0xFFFF5C7A.toInt()

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = CourierPrefs(this)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(16), dp(8), dp(16), dp(24))
        val scroll = ScrollView(this).apply { setBackgroundColor(BG); isFillViewport = true; addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)) }
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) { val b = insets.getInsets(WindowInsets.Type.systemBars()); v.setPadding(b.left, b.top, b.right, b.bottom) }
            else @Suppress("DEPRECATION") v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        if (prefs.ready && !CourierRuntime.isRunning) try { CourierService.start(this) } catch (e: Exception) { /* shown as "Off" below */ }
        ui.post(tick)
    }

    override fun onPause() { resumed = false; ui.removeCallbacks(tick); super.onPause() }

    private val tick = object : Runnable { override fun run() { if (!resumed) return; render(); ui.postDelayed(this, 1000) } }

    // ------------------------------------------------------------------------------------------------------------------ drawing
    private fun render() {
        val scratch = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        target = scratch; sig.setLength(0)
        try { build() } finally { target = body }
        val now = sig.toString()
        if (now == lastSig) return                                          // nothing changed: leave the views (and a selected code) alone
        lastSig = now
        body.removeAllViews()
        while (scratch.childCount > 0) { val c = scratch.getChildAt(0); val lp = c.layoutParams; scratch.removeViewAt(0); body.addView(c, lp) }
    }

    private fun build() {
        target.addView(text("Kestrel Courier", 24f, FG, bold = true).apply { setPadding(dp(4), dp(16), 0, dp(2)) })
        target.addView(text("Downloads for your tablet's Kestrel browser, over your hotspot.", 14f, MUTED).apply { setPadding(dp(4), 0, 0, dp(12)) })

        val on = CourierRuntime.isRunning
        card {
            addView(text(if (on) "Ready for the tablet" else "Off", 22f, if (on) OK else MUTED, bold = true))
            val addrs = CourierNet.hotspotAddresses()
            val line = CourierRuntime.lastError ?: when {
                !on -> "Switch it on, then turn on your phone's hotspot and join it with the tablet."
                addrs.isEmpty() -> "No hotspot address yet. Turn on your phone's hotspot (Settings > Network > Hotspot)."
                else -> "The tablet finds this phone at " + addrs.joinToString(" or ") { "$it:${CourierRuntime.port}" } + "."
            }
            addView(text(line, 14f, if (CourierRuntime.lastError != null) DANGER else MUTED).apply { setPadding(0, dp(4), 0, dp(8)) })
            val row = LinearLayout(this@CourierActivity).apply { gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(52) }
            row.addView(text("Ready for the tablet", 16f, FG), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            sig.append(if (on) "<on>" else "<off>")
            val sw = Switch(this@CourierActivity).apply { isChecked = on }
            sw.setOnCheckedChangeListener { _, checked -> if (checked) turnOn() else CourierService.stop(this@CourierActivity); ui.postDelayed({ render() }, 300) }
            row.addView(sw)
            addView(row)
        }

        card {
            addView(text("Pairing code", 12f, ACCENT, bold = true))
            addView(text(prefs.code(), 26f, FG, bold = true).apply { typeface = Typeface.MONOSPACE; setPadding(0, dp(6), 0, dp(6)); setTextIsSelectable(true) })
            addView(text("Type this code once on the tablet: Kestrel > Settings > Download on my phone > Pair with the phone. Only a tablet that knows the code can use this phone.", 13f, MUTED))
            addView(button("Make a new code") {
                AlertDialog.Builder(this@CourierActivity).setTitle("Make a new code?").setMessage("The tablet stops working with this phone until you type the new code there.")
                    .setPositiveButton("New code") { _, _ -> prefs.newSecret(); CourierRuntime.restart(this@CourierActivity); render() }.setNegativeButton("Cancel", null).show()
            })
        }

        card {
            val pm = getSystemService(PowerManager::class.java)
            val free = pm.isIgnoringBatteryOptimizations(packageName)
            addView(text("Keep it running", 12f, ACCENT, bold = true))
            addView(text(if (free) "Allowed to run with the screen off." else "Android may stop the app when the screen is off, and the tablet would not find the phone.", 14f, if (free) OK else WARN).apply { setPadding(0, dp(4), 0, dp(4)) })
            if (!free) addView(button("Allow it to run with the screen off") {
                try { startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))) }
                catch (e: Exception) { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            })
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                addView(button("Allow the \"ready\" notification") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1) })
        }

        val list = CourierRuntime.views()
        card {
            addView(text("Downloading for the tablet", 12f, ACCENT, bold = true))
            if (list.isEmpty()) addView(text("Nothing right now. Downloads you start on the tablet while \"Use the phone\" is on appear here.", 14f, MUTED).apply { setPadding(0, dp(4), 0, 0) })
            for (v in list.take(20)) addView(row(v))
        }

        val free = try { StatFs(filesDir.path).availableBytes } catch (e: Exception) { 0L }
        val ver = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "?" }
        target.addView(text("Free space on this phone: ${bytes(free)}   ·   Courier $ver", 12f, MUTED).apply { setPadding(dp(4), dp(8), 0, 0) })
    }

    private fun turnOn() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        try { CourierService.start(this) } catch (e: Exception) { AlertDialog.Builder(this).setTitle("Could not start").setMessage("Android did not allow the service to start: ${e.javaClass.simpleName}").setPositiveButton("OK", null).show() }
    }

    private fun row(v: DownloadView): View {
        val r = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(10), 0, 0) }
        r.addView(text(v.name, 15f, FG, bold = true).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END })
        val pct = if (v.total > 0) (v.done * 100 / v.total).toInt() else -1
        val status = when (v.state) {
            DlState.RUNNING -> (if (pct >= 0) "$pct%" else bytes(v.done)) + (if (v.bytesPerSec > 0) "  ·  ${bytes(v.bytesPerSec)}/s" else "") + (if (v.total > 0) "  ·  ${bytes(v.done)} of ${bytes(v.total)}" else "")
            DlState.COMPLETED -> "Done  ·  ${bytes(maxOf(v.total, v.done))}  ·  waiting for the tablet to collect it"
            DlState.PAUSED_USER, DlState.PAUSED_SYSTEM -> "Paused"
            DlState.WAITING_NETWORK -> "Waiting for the phone's connection"
            DlState.FAILED -> "Failed"
            DlState.CANCELED -> "Canceled"
            else -> "Starting"
        }
        r.addView(text(status, 13f, if (v.state == DlState.FAILED) DANGER else MUTED))
        if (v.total > 0 && !v.state.terminal) sig.append("#").append(v.done * 1000 / v.total)
        if (v.total > 0 && !v.state.terminal) r.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 1000; progress = (v.done * 1000 / v.total).toInt() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)).apply { topMargin = dp(4) })
        if (!v.state.terminal) r.addView(button("Cancel") { CourierRuntime.cancel(v.id); render() }.apply { minHeight = dp(40); minimumHeight = dp(40) })
        return r
    }

    // ------------------------------------------------------------------------------------------------------------------ pieces
    private fun card(block: LinearLayout.() -> Unit) {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply { setColor(CARD); cornerRadius = dp(12).toFloat(); setStroke(dp(1), 0xFF12384A.toInt()) }
        }
        c.block()
        target.addView(c, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
    }

    private fun text(s: String, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        sig.append(s).append('|')
        text = s; textSize = sp; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD; includeFontPadding = true
    }

    private fun button(label: String, click: () -> Unit) = Button(this).apply {
        sig.append('[').append(label).append(']')
        text = label; isAllCaps = false; textSize = 14f; setTextColor(ON_ACCENT); minimumHeight = dp(48); minHeight = dp(48)
        background = GradientDrawable().apply { setColor(ACCENT); cornerRadius = dp(10).toFloat() }
        setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) }
    }

    private fun bytes(b: Long): String = when {
        b >= 1L shl 30 -> String.format(Locale.US, "%.2f GB", b / 1073741824.0)
        b >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", b / 1048576.0)
        b >= 1L shl 10 -> String.format(Locale.US, "%.0f KB", b / 1024.0)
        else -> "$b B"
    }
}
