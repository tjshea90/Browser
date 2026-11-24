package app.kestrel.browser.optimizer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.kestrel.browser.ProcessLife
import app.kestrel.core.optimizer.Priority
import org.mozilla.geckoview.GeckoView

/**
 * OPT1 (research/OPTIMIZER.md): the Auto-tune screen. Its own activity (own GeckoView, own session) so the browser's tabs are never touched. Top: what is happening and how long it takes. Middle: the
 * benchmark page the engine is drawing right now (the tablet must keep it in front). Panels: the start questions, "paused", a question about the screen, and the report.
 */
class OptimizerActivity : Activity(), OptimizerRunner.Ui {
    private lateinit var runner: OptimizerRunner
    private lateinit var geckoView: GeckoView
    private lateinit var titleTv: TextView
    private lateinit var detailTv: TextView
    private lateinit var bar: ProgressBar
    private lateinit var panel: ScrollView
    private lateinit var panelBody: LinearLayout
    private lateinit var askBar: LinearLayout
    private lateinit var askText: TextView
    private var running = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun tv(text: String, size: Float = 15f, bold: Boolean = false, color: Int = Color.WHITE) = TextView(this).apply { this.text = text; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD); setPadding(0, dp(4), 0, dp(4)) }
    private fun btn(text: String, onClick: () -> Unit) = Button(this).apply { this.text = text; isAllCaps = false; setOnClickListener { onClick() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ProcessRestart.clearFallback(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#0f1419")) }
        val head = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(8)); setBackgroundColor(Color.parseColor("#17202a")) }
        titleTv = tv("Auto-tune", 17f, true); detailTv = tv("", 13f, false, Color.parseColor("#b8c4d0")); bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 1000 }
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(btn("Pause") { runner.pause("you paused it") }); buttons.addView(btn("Stop and restore my settings") { runner.stop() })
        head.addView(titleTv); head.addView(detailTv); head.addView(bar); head.addView(buttons)
        val body = FrameLayout(this)
        geckoView = GeckoView(this)
        body.addView(geckoView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        panelBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(20)) }
        panel = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#0f1419")); addView(panelBody); visibility = View.GONE }
        body.addView(panel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        askBar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#17202a")); setPadding(dp(14), dp(10), dp(14), dp(12)); visibility = View.GONE }
        askText = tv("", 16f, true); askBar.addView(askText)
        body.addView(askBar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        root.addView(head, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        runner = OptimizerRunner(this, geckoView, this)
        runner.begin()
        // the emulator lab (debuggable builds only): `am start ... --es auto quick` runs the short plan by itself
        if (intent.getStringExtra("auto") == "quick" && (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 && app.kestrel.browser.optimizer.OptimizerStore.load(this) == null) runner.startRun(Priority.BALANCED, true, true)
    }

    private var started = false
    /**
     * The app ends its own process a moment after the last screen goes away (ProcessLife). After a restart the system may bring back the BROWSER screen of the old task for a moment and destroy it again, which
     * says "no screen" while this one is in front (found by the emulator lab: the run died 2 s after the restart). So while this screen is on show it says "I am here" again every second.
     */
    private val beat = object : Runnable { override fun run() { if (started) { ProcessLife.uiAlive = true; ProcessLife.cancel(); app.kestrel.browser.Io.main.postDelayed(this, 1000) } } }
    override fun onStart() { super.onStart(); runner.activate(); started = true; ProcessLife.uiAlive = true; ProcessLife.cancel(); app.kestrel.browser.Io.main.removeCallbacks(beat); app.kestrel.browser.Io.main.postDelayed(beat, 1000) }

    override fun onStop() {
        super.onStop()
        started = false
        if (running && !isChangingConfigurations) runner.pause("you left the app")
        if (!isChangingConfigurations) { ProcessLife.uiAlive = false; ProcessLife.maybeExit() }
    }

    override fun onDestroy() { super.onDestroy(); runner.close() }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (running) runner.pause("you pressed Back") else super.onBackPressed() }

    // ---------------------------------------------------------------------------------------------------- Ui

    private fun showPanel(show: Boolean) { panel.visibility = if (show) View.VISIBLE else View.GONE; if (show) panelBody.removeAllViews() }

    override fun status(title: String, detail: String, done: Double, total: Double) = runOnUiThread {
        running = true; showPanel(false); askBar.visibility = View.GONE
        titleTv.text = title; detailTv.text = detail; bar.progress = if (total > 0) (1000 * done / total).toInt() else 0
    }

    override fun ask(text: String, yes: String, no: String, answer: (Boolean) -> Unit) = runOnUiThread {
        askText.text = text
        while (askBar.childCount > 1) askBar.removeViewAt(1)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(btn(yes) { askBar.visibility = View.GONE; answer(true) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(btn(no) { askBar.visibility = View.GONE; answer(false) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        askBar.addView(row); askBar.visibility = View.VISIBLE
    }

    override fun paused(reason: String) = runOnUiThread {
        running = false; showPanel(true); askBar.visibility = View.GONE
        titleTv.text = "Auto-tune is paused"; detailTv.text = ""
        panelBody.addView(tv("Auto-tune is paused: $reason.", 16f, true))
        panelBody.addView(tv("Nothing is lost. Continue where it stopped, or stop and keep your own settings.", 14f, false, Color.parseColor("#b8c4d0")))
        panelBody.addView(btn("Continue") { runner.cont() }); panelBody.addView(btn("Stop and restore my settings") { runner.stop() }); panelBody.addView(btn("Close") { finish() })
    }

    override fun report(lines: List<String>, canUndo: Boolean, restartNeeded: Boolean) = runOnUiThread {
        running = false; showPanel(true); askBar.visibility = View.GONE
        titleTv.text = "Auto-tune"; detailTv.text = ""; bar.progress = 1000
        for ((i, l) in lines.withIndex()) panelBody.addView(tv(l, if (i == 0) 17f else 13f, i == 0, if (i == 0) Color.WHITE else Color.parseColor("#d3dbe3")))
        if (restartNeeded) { panelBody.addView(tv("Kestrel must restart once to use the new settings.", 14f, true, Color.parseColor("#ffd27d"))); panelBody.addView(btn("Apply and restart Kestrel now") { runner.restartIntoBrowser() }) }
        else panelBody.addView(btn("Open the browser") { startActivity(Intent(this, app.kestrel.browser.MainActivity::class.java)); finish() })
        panelBody.addView(btn("Send the results to Claude") { runner.sendLast() })
        if (canUndo) panelBody.addView(btn("Undo: put my earlier settings back") { runner.undo() })
        panelBody.addView(btn("Close") { finish() })
    }

    override fun preflight(last: List<String>?) = runOnUiThread {
        running = false; showPanel(true); askBar.visibility = View.GONE
        titleTv.text = "Auto-tune"; detailTv.text = "Finds the settings that make Kestrel fastest on this tablet"
        panelBody.addView(tv("Auto-tune tries each speed setting one by one, measures it with fixed test pages, keeps only what is proven better and tells you what it kept.", 15f))
        panelBody.addView(tv("It takes about an hour. Keep the tablet plugged in with Battery Saver off, leave this screen in front, and close your private tabs. Kestrel restarts itself several times; if it does not come back, tap the notification. You can pause or stop at any time and your own settings come back.", 13f, false, Color.parseColor("#b8c4d0")))
        panelBody.addView(tv("What matters most to you?", 15f, true))
        val group = RadioGroup(this)
        val ids = HashMap<Int, Priority>()
        for (p in Priority.values()) { val r = RadioButton(this).apply { text = p.label; setTextColor(Color.WHITE); id = View.generateViewId() }; ids[r.id] = p; group.addView(r); if (p == Priority.BALANCED) r.isChecked = true }
        panelBody.addView(group)
        val again = CheckBox(this).apply { text = "Test everything again (ignore what it settled before)"; setTextColor(Color.WHITE) }
        panelBody.addView(again)
        panelBody.addView(btn("Start Auto-tune") { runner.startRun(ids[group.checkedRadioButtonId] ?: Priority.BALANCED, again.isChecked) })
        panelBody.addView(btn("Close") { finish() })
        if (last != null) { panelBody.addView(tv("The last run:", 15f, true)); for (l in last.take(40)) panelBody.addView(tv(l, 12f, false, Color.parseColor("#d3dbe3"))) }
    }

    override fun say(text: String) { runOnUiThread { Toast.makeText(this, text, Toast.LENGTH_LONG).show() } }
}
