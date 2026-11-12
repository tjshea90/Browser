package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import app.kestrel.browser.Flavor
import app.kestrel.browser.Io
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.update.UpdateManager
import app.kestrel.browser.update.UpdateManager.Phase
import app.kestrel.core.update.UpdateLogic

/**
 * The "Updates" sheet: what the updater knows, and the one button that makes sense next (Check now / Download / Pause / Install). It redraws itself whenever
 * [UpdateManager] changes (download progress included) and stops listening when the sheet closes.
 */
class UpdatePanel(ctx: Context, private val pal: Pal, private val prefs: Prefs, private val actions: Actions) : LinearLayout(ctx) {
    interface Actions {
        fun openPage(url: String)
        /** The user tapped Download: the activity asks first on a metered link (a hotspot), then calls [UpdateManager.download]. */
        fun download()
        fun openInstallSettings()
        /** Android's Storage screen, to free room for the installer. */
        fun openStorageSettings()
    }

    private val listener: () -> Unit = { render() }
    private var drawnPhase: Phase? = null
    private var bar: Bar? = null
    private var progressText: TextView? = null
    private var otherWay = false
    private var saving = false
    private var otherWayError: String? = null

    init {
        orientation = VERTICAL
        setPadding(ctx.dp(20), ctx.dp(4), ctx.dp(20), ctx.dp(18))
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); UpdateManager.addListener(listener); render(); UpdateManager.recheckSpace(context) } // a stale "not enough space" goes away as soon as there is enough
    override fun onDetachedFromWindow() { UpdateManager.removeListener(listener); super.onDetachedFromWindow() }

    private fun render() {
        val s = UpdateManager.state
        if (s.phase == Phase.DOWNLOADING && drawnPhase == Phase.DOWNLOADING && bar != null) { // a progress tick: move the bar, keep everything else
            bar!!.fraction = if (s.total > 0) s.done.toFloat() / s.total else 0f
            progressText?.text = progressLine(s)
            return
        }
        drawnPhase = s.phase
        removeAllViews()
        bar = null; progressText = null
        val ctx = context
        val o = s.offer
        val name = Flavor.title
        val installed = UpdateManager.installedName(ctx)

        fun headline(t: String, color: Int = pal.fg) = addView(label(ctx, t, 19f, color, bold = true).apply { includeFontPadding = true; setPadding(0, ctx.dp(8), 0, ctx.dp(4)) })
        fun sub(t: String, color: Int = pal.muted) = addView(label(ctx, t, 14f, color).apply { includeFontPadding = true; setPadding(0, 0, 0, ctx.dp(8)) })
        fun buttons(vararg b: TextView) {
            val row = LinearLayout(ctx).apply { orientation = HORIZONTAL; setPadding(0, ctx.dp(10), 0, ctx.dp(4)) }
            for ((i, v) in b.withIndex()) row.addView(v, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { if (i > 0) marginStart = ctx.dp(10) })
            addView(row)
        }
        fun primary(t: String, f: () -> Unit) = pillButton(ctx, t, pal.accent, pal.accentOn) { f() }
        fun secondary(t: String, f: () -> Unit) = outlineButton(ctx, t, pal.pill, pal.divider, pal.fg) { f() }

        when (s.phase) {
            Phase.IDLE, Phase.UP_TO_DATE -> {
                headline(if (s.checkedAt > 0) "$name is up to date" else "Check for a new version")
                sub("You have version $installed." + if (s.checkedAt > 0) " Checked ${ago(s.checkedAt)}." else "")
                buttons(primary("Check now") { UpdateManager.check(ctx, manual = true) })
            }
            Phase.CHECKING -> { headline("Checking GitHub…"); sub("A small request to GitHub Releases; no page you visit is involved.") }
            Phase.AVAILABLE -> {
                headline("Update available: ${o?.versionName ?: ""}")
                sub("You have $installed. " + (o?.let { UpdateLogic.formatSize(it.size) + " from GitHub Releases." } ?: ""))
                notes(o?.notes)
                val partial = s.done > 0 && o != null && s.done < o.size
                buttons(
                    primary(if (partial) "Continue download" else "Download " + (o?.let { UpdateLogic.formatSize(it.size) } ?: "")) { actions.download() },
                    secondary("Release page") { o?.pageUrl?.takeIf { it.isNotEmpty() }?.let { actions.openPage(it) } },
                )
            }
            Phase.DOWNLOADING -> {
                headline("Downloading ${o?.versionName ?: ""}")
                val b = Bar(ctx, pal); bar = b
                b.fraction = if (s.total > 0) s.done.toFloat() / s.total else 0f
                addView(b, LayoutParams(LayoutParams.MATCH_PARENT, ctx.dp(10)).apply { topMargin = ctx.dp(6); bottomMargin = ctx.dp(8) })
                val t = label(ctx, progressLine(s), 14f, pal.muted).apply { includeFontPadding = true; setPadding(0, 0, 0, ctx.dp(8)) }
                progressText = t; addView(t)
                buttons(secondary("Pause") { UpdateManager.pause() })
            }
            Phase.READY -> {
                headline("Ready to install ${o?.versionName ?: ""}")
                sub(if (o?.sha256 != null) "The file was checked against the checksum published with the release. Android will ask you to confirm." else "Android will ask you to confirm.")
                buttons(primary("Install now") { UpdateManager.install(ctx) }, secondary("Delete download") { UpdateManager.discard(ctx) })
            }
            Phase.INSTALLING -> { headline("Waiting for Android's installer…"); sub("Confirm on the screen Android shows. $name restarts when it is done.") }
            Phase.NEEDS_PERMISSION -> {
                headline("Android needs your OK first", pal.warn)
                sub("Open settings, switch on \"Allow from this source\" for $name, come back and tap Install. This is asked once.")
                buttons(primary("Open settings") { actions.openInstallSettings() }, secondary("Install") { UpdateManager.install(ctx) })
            }
            Phase.NEEDS_SPACE -> {
                val retry = { if (s.fileKept) UpdateManager.install(ctx) else actions.download() }
                if (otherWay) {
                    headline("Install the other way")
                    sub("1. Tap Save and uninstall. The update (${o?.let { UpdateLogic.formatSize(it.size) } ?: ""}) is copied to your Downloads folder, then Android asks whether to uninstall $name.", pal.fg)
                    sub("2. Confirm the uninstall.", pal.fg)
                    sub("3. Open Files, then Downloads, and tap the saved update (named like ${o?.apkName ?: "kestrel-g"}). Allow installing from Files if Android asks.", pal.fg)
                    sub("Bookmarks, history and settings of this version are not carried over; your downloaded files stay in Downloads.", pal.warn)
                    otherWayError?.let { sub(it, pal.danger) }
                    buttons(
                        primary(if (saving) "Saving…" else "Save and uninstall") {
                            if (!saving) {
                                saving = true; otherWayError = null; render()
                                UpdateManager.saveForManualInstall(ctx) { saved, why ->
                                    saving = false
                                    if (saved != null) { android.widget.Toast.makeText(ctx, "Saved to Downloads: $saved", android.widget.Toast.LENGTH_LONG).show(); UpdateManager.uninstallSelf(ctx) }
                                    else { otherWayError = why; render() }
                                }
                            }
                        },
                        secondary("Back") { otherWay = false; otherWayError = null; render() },
                    )
                } else {
                    headline("Android would not install it over the old version", pal.warn)
                    sub("It refused because this tablet is short of free storage for an update in place (it had ${UpdateLogic.formatSize(s.freeNow)} free). Android keeps ${UpdateLogic.formatSize(s.reserveBytes)} free after any install, and while it installs the update takes about ${UpdateLogic.formatSize(s.installedBytes)} on top, because the old version stays until the new one is in place.", pal.fg)
                    sub("Two ways: make room" + (if (s.shortBy > 0) " (about ${UpdateLogic.formatSize(s.shortBy)} more)" else "") + " and tap Try again, or install the other way: the update is saved to Downloads, $name is uninstalled and you tap the saved file. Uninstalling frees the old version's own storage, which is why that always worked.")
                    sub(if (s.fileKept) "The update file is already downloaded: nothing is downloaded again." else "The update file (${o?.let { UpdateLogic.formatSize(it.size) } ?: ""}) still has to be downloaded.")
                    val old = app.kestrel.browser.download.DownloadController.oldInstallers()
                    if (old.isNotEmpty()) {
                        val bytes = old.sumOf { maxOf(it.total, it.done) }
                        sub("Quick room: ${old.size} Kestrel installer ${if (old.size == 1) "file from an earlier update is" else "files from earlier updates are"} still in your Downloads (${UpdateLogic.formatSize(bytes)}).", pal.fg)
                        buttons(primary("Delete old installers") {
                            val freed = app.kestrel.browser.download.DownloadController.deleteOldInstallers()
                            android.widget.Toast.makeText(ctx, "Deleted ${UpdateLogic.formatSize(freed)}", android.widget.Toast.LENGTH_SHORT).show()
                            // the files go away on the download engine's own thread and Android's free-space figure lags a little: look again twice
                            Io.main.postDelayed({ UpdateManager.recheckSpace(ctx) }, 1200)
                            Io.main.postDelayed({ UpdateManager.recheckSpace(ctx) }, 3500)
                        }, secondary("Storage settings") { actions.openStorageSettings() })
                        buttons(primary("Try again") { retry() }, secondary("Install the other way") { otherWay = true; render() })
                    } else buttons(
                        primary("Try again") { retry() },
                        secondary("Install the other way") { otherWay = true; render() },
                    )
                    buttons(secondary("Storage settings") { actions.openStorageSettings() })
                }
            }
            Phase.ERROR -> {
                headline("That did not work", pal.danger)
                sub(s.message, pal.fg)
                val page = o?.pageUrl?.takeIf { it.isNotEmpty() }
                val retry = if (o != null && (s.message.contains("download", true) || s.message.contains("Connection lost", true) || s.message.contains("checksum", true) || s.message.contains("space", true)))
                    primary("Try again") { actions.download() } else primary("Check now") { UpdateManager.check(ctx, manual = true) }
                if (page != null) buttons(retry, secondary("Release page") { actions.openPage(page) }) else buttons(retry)
            }
        }

        // the one setting: look once a day (when the app opens) or only on request
        val row = LinearLayout(ctx).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, ctx.dp(14), 0, 0); minimumHeight = ctx.dp(56) }
        val txt = LinearLayout(ctx).apply { orientation = VERTICAL }
        txt.addView(label(ctx, "Look for updates by itself", 15f, pal.fg).apply { includeFontPadding = true })
        txt.addView(label(ctx, "Once a day, when the app opens: one tiny request, no background activity.", 12f, pal.muted).apply { includeFontPadding = true; setPadding(0, ctx.dp(2), 0, 0) })
        row.addView(txt, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Switch(ctx).apply { isChecked = prefs.autoUpdateCheck; setOnCheckedChangeListener { _, on -> prefs.autoUpdateCheck = on } })
        addView(row)
    }

    private fun notes(text: String?) {
        if (text.isNullOrBlank()) return
        val ctx = context
        addView(label(ctx, text, 14f, pal.fg).apply {
            maxLines = 8; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true
            setPadding(ctx.dp(12), ctx.dp(10), ctx.dp(12), ctx.dp(10))
            background = ChamferDrawable(pal.pill, ctx.dpf(8f), ChamferDrawable.TL or ChamferDrawable.BR)
        })
    }

    private fun progressLine(s: UpdateManager.State): String =
        if (s.total > 0) "${UpdateLogic.formatSize(s.done)} of ${UpdateLogic.formatSize(s.total)}  ·  ${(s.done * 100 / s.total).toInt()} %" else UpdateLogic.formatSize(s.done)

    private fun ago(t: Long): String {
        val m = (System.currentTimeMillis() - t) / 60_000
        return when { m < 1 -> "just now"; m < 60 -> "$m min ago"; m < 48 * 60 -> "${m / 60} h ago"; else -> "${m / 1440} days ago" }
    }

    /** A flat chamfered progress bar: no animation, it just redraws at the next tick. */
    private class Bar(ctx: Context, private val pal: Pal) : View(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val box = RectF()
        private val path = Path()
        var fraction = 0f
            set(v) { val c = v.coerceIn(0f, 1f); if (c != field) { field = c; invalidate() } }

        override fun onDraw(c: Canvas) {
            box.set(0f, 0f, width.toFloat(), height.toFloat())
            ChamferDrawable.path(box, context.dpf(4f), ChamferDrawable.TL or ChamferDrawable.BR, path)
            paint.color = pal.pill; c.drawPath(path, paint)
            c.save(); c.clipPath(path)
            paint.color = pal.accent; c.drawRect(0f, 0f, width * fraction, height.toFloat(), paint)
            c.restore()
        }
    }
}
