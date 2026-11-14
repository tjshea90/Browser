package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import app.kestrel.browser.R
import app.kestrel.core.input.Key
import app.kestrel.core.input.KeyBox
import app.kestrel.core.input.KeyResult
import app.kestrel.core.input.KeySpec
import app.kestrel.core.input.MiniKeyboardGeometry
import app.kestrel.core.input.MiniKeyboardLayout
import app.kestrel.core.input.MiniKeyboardModel
import app.kestrel.core.input.glide.GlideKeys
import app.kestrel.core.input.glide.GlideResult
import app.kestrel.core.input.glide.GlideTyping
import app.kestrel.core.input.glide.UserWords
import java.util.concurrent.Executors

/**
 * The one-hand mini navigation panel (AR2, owner message 32: "My tablet screen is big, and I sometimes browse with one hand only at the top of the tablet. Make a small quick menu at the top right corner of the screen
 * that is black ... a back button on the left, home button in the middle, a forward button on the right, and a bar underneath which when I press it, it selects everything in the url bar and puts a small floating
 * keyboard ... at the top right ... There should be a small toggle at the top right of the screen to display or hide this mini navigation panel ... a quick, smooth animation of the mini panel sliding down from
 * the top ... When I press any button or the bar ... a very quick light up neon blue").
 *
 * Everything is drawn by a few small views (no ripple, no animator objects): the panel slides with its own clock and the lit state of a pressed button is a timer, because the owner runs Android with animations turned
 * OFF (the system's animation scale would make a system animator jump straight to its end; the neon glow already works the same way, see [Neon]). Android's own keyboard cannot be moved by an app, so the
 * keyboard here is the app's own ([KeysView] over [MiniKeyboardModel] in the core module). The page below keeps every touch outside the panel's own views.
 *
 * Glide typing (AT1, owner message 34: "glide typing like Gboard ... make the glide word predictions accurate"): a finger that slides off the key it went down on draws a word over the letters;
 * on lift the core decoder (core/input/glide) turns the path into words, the best goes into the bar with a space after it and the next two show in a strip above the keys.
 */
class MiniNav(ctx: Context, private var pal: Pal, private val l: Listener) : FrameLayout(ctx) {
    interface Listener {
        fun navBack()
        fun navHome()
        fun navForward()
        /** [text] is what was typed: an address, or words to search for. */
        fun navSubmit(text: String)
        /** The panel was shown or hidden (remember it). */
        fun navOpenChanged(open: Boolean)
        /** Glide typing is switched on in Settings. */
        fun navGlideEnabled(): Boolean = false
        /** The thumb habit saved earlier (key pitches, x then y), and where to save the new one. */
        fun navGlideHabit(): FloatArray? = null
        fun navGlideHabitChanged(habit: FloatArray) {}
        /** The words typed letter by letter that the word list did not have ("word count" per line), and where to save the new list. */
        fun navGlideWords(): String = ""
        fun navGlideWordsChanged(text: String) {}
        /** The host names of the sites visited (asked when the keyboard opens; answer on any thread): names like doraby join the words that can be glided. */
        fun navGlideHosts(done: (List<String>) -> Unit) {}
    }

    private val model = MiniKeyboardModel()
    private val panel = LinearLayout(ctx)
    private val back = NavButton(ctx, R.drawable.ic_back, "Back") { l.navBack() }
    private val home = NavButton(ctx, R.drawable.ic_home, "Home") { l.navHome() }
    private val forward = NavButton(ctx, R.drawable.ic_forward, "Forward") { l.navForward() }
    private val bar = BarView(ctx)
    private val keys = KeysView(ctx)
    private val toggle = ToggleView(ctx)
    private var address = ""
    private var anim: Runnable? = null

    // ---- glide typing (AT1)
    private var glideOn = false
    private var glide: GlideTyping? = null
    private val glideExec by lazy { Executors.newSingleThreadExecutor { r -> Thread(r, "kestrel-glide").apply { isDaemon = true } } }
    private var ownWords = UserWords()
    private var siteWords = emptyList<String>()
    private var pendingWord: String? = null     // the word the last glide put in: kept when the next thing is not a correction
    private var hostsAsked = false
    /** Tests: where the engine comes from (the real one reads the shipped word list). */
    internal var glideFactory: (() -> GlideTyping)? = null

    /** The panel is shown (sliding in counts as shown). */
    var isOpen = false; private set
    var keyboardShowing = false; private set

    /** False hides the toggle and the panel altogether (full-screen video; Settings can switch the feature off). */
    var available = true
        set(v) { field = v; visibility = if (v) VISIBLE else GONE }

    private val topGap = ctx.dp(TOGGLE_H_DP + 2)

    init {
        isClickable = false // a touch outside the toggle and the panel belongs to the page below
        panel.orientation = LinearLayout.VERTICAL
        panel.isClickable = true
        panel.setPadding(ctx.dp(6), ctx.dp(6), ctx.dp(6), ctx.dp(6))
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        for (b in listOf(back, home, forward)) row.addView(b, LinearLayout.LayoutParams(ctx.dp(NAV_W_DP), ctx.dp(NAV_H_DP)))
        panel.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        panel.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(BAR_H_DP)).apply { topMargin = ctx.dp(4) })
        panel.addView(keys, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(KEYS_H_DP)).apply { topMargin = ctx.dp(4) })
        keys.visibility = GONE
        panel.visibility = GONE
        addView(panel, LayoutParams(ctx.dp(PANEL_W_DP), LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply { topMargin = topGap })
        addView(toggle, LayoutParams(ctx.dp(TOGGLE_W_DP), ctx.dp(TOGGLE_H_DP), Gravity.TOP or Gravity.END))
        keys.onKey = { key -> typed(key) }
        applyColors()
    }

    // ------------------------------------------------------------------ what the host tells the panel

    /** Called whenever the page in front changes (or its history): what the buttons can do, whether Home is on screen, and the address the bar shows ("" on Home / a new tab). */
    fun sync(canBack: Boolean, canForward: Boolean, homeShown: Boolean, address: String) {
        back.dim = !canBack
        forward.dim = !canForward
        home.active = homeShown
        if (this.address != address) { this.address = address; if (!keyboardShowing) bar.invalidate() }
    }

    fun applyPal(p: Pal) {
        pal = p
        applyColors()
    }

    /** The Back key / gesture: closes the keyboard first. True when it did something. */
    fun onBackPressed(): Boolean {
        if (!keyboardShowing) return false
        closeKeyboard()
        return true
    }

    // ------------------------------------------------------------------ show, hide, the keyboard

    /** Shows or hides the panel; [animate] false jumps (the start of the app). The toggle is always there. */
    fun setOpen(open: Boolean, animate: Boolean = true, notify: Boolean = true) {
        if (open == isOpen) return
        isOpen = open
        toggle.invalidate()
        cancelAnim()
        if (open) {
            val wasVisible = panel.visibility == VISIBLE // reopened while it was still sliding away: carry on from where it is
            panel.visibility = VISIBLE
            panel.measure(MeasureSpec.makeMeasureSpec(panelWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            val hidden = -(panel.measuredHeight + topGap).toFloat()
            if (animate) slide(if (wasVisible) panel.translationY else hidden, 0f, DecelerateInterpolator(2f)) {} else panel.translationY = 0f
        } else {
            if (keyboardShowing) closeKeyboard()
            val hidden = -(panel.height.coerceAtLeast(panel.measuredHeight) + topGap).toFloat()
            if (animate) slide(panel.translationY, hidden, AccelerateInterpolator(1.4f)) { panel.visibility = GONE; panel.translationY = 0f }
            else { panel.visibility = GONE; panel.translationY = 0f }
        }
        if (notify) l.navOpenChanged(open)
    }

    fun openKeyboard() {
        if (!isOpen || keyboardShowing) return
        model.open(address)
        keyboardShowing = true
        keys.visibility = VISIBLE
        setPanelWidth(PANEL_KEYS_W_DP)
        pendingWord = null
        startGlideEngine()
        bar.invalidate(); keys.invalidate()
    }

    fun closeKeyboard() {
        if (!keyboardShowing) return
        confirmGlide()
        keys.reset()
        keyboardShowing = false
        keys.visibility = GONE
        setPanelWidth(PANEL_W_DP)
        bar.invalidate()
    }

    // ------------------------------------------------------------------ glide typing (AT1)

    /** Settings switched glide typing on or off: the strip above the keys appears or goes, and the keys' view changes height by it. */
    fun setGlideEnabled(on: Boolean) {
        if (on == glideOn) return
        glideOn = on
        val lp = keys.layoutParams
        lp.height = context.dp(KEYS_H_DP + if (on) STRIP_H_DP else 0)
        keys.layoutParams = lp
        keys.reset()
        if (keyboardShowing) { if (on) startGlideEngine(); keys.invalidate() }
    }

    /** What menu > Diagnostics shows for it. */
    fun glideLine(): String = glide?.stats?.line() ?: "Glide typing: " + if (glideOn) "not used yet this run" else "off (Settings)"

    private fun startGlideEngine() {
        if (!glideOn) return
        if (glide == null) {
            glide = glideFactory?.invoke() ?: GlideTyping({ try { context.assets.open("glide/en.klx").use { it.readBytes() } } catch (e: Exception) { null } }, glideExec)
            ownWords = UserWords.parse(l.navGlideWords())
        }
        prepareGlide()
        if (!hostsAsked) { hostsAsked = true; l.navGlideHosts { hosts -> post { siteWords = hosts.mapNotNull { UserWords.wordOfHost(it) }.distinct().take(300); pushOwnWords() } } }
        else pushOwnWords()
    }

    /** The decoder needs the real key positions: they are known once the keys view has a size (and again when it changes). */
    internal fun prepareGlide() {
        val g = glide ?: return
        val k = keys.glideKeys() ?: return
        g.prepare(k, l.navGlideHabit(), mergedWords())
    }

    private fun mergedWords(): UserWords {
        val m = UserWords.parse(ownWords.serialize())
        for (w in siteWords) m.add(w)
        return m
    }

    private fun pushOwnWords() { glide?.ownWords(mergedWords()) }

    /** The glide that was just put in stays: the decoder learns from it where this thumb lands (before the next glide is decoded: they share one thread, in order). */
    private fun confirmGlide() {
        val w = pendingWord ?: return
        pendingWord = null
        val g = glide ?: return
        g.stats.onKept()
        g.accepted(w) { h -> post { l.navGlideHabitChanged(h) } }
    }

    private fun glideDone(res: GlideResult) {
        val g = glide ?: return
        if (!keyboardShowing) return
        val best = res.candidates.firstOrNull()
        if (best == null) { keys.showNoWord(); return }
        model.commitGlide(best.text)
        pendingWord = best.text
        keys.showCandidates(res.candidates.map { it.text })
        bar.invalidate()
    }

    /** A candidate in the strip was tapped: it replaces the word the glide put in. */
    private fun pickCandidate(word: String) {
        val g = glide
        if (!model.swapGlide(word)) return
        pendingWord = null
        g?.stats?.onPicked()
        g?.accepted(word) { h -> post { l.navGlideHabitChanged(h) } }
        keys.showCandidates(keys.candidateTexts().let { list -> listOf(word) + list.filter { it != word } }.take(3))
        bar.invalidate()
    }

    private fun panelWidth() = context.dp(if (keyboardShowing) PANEL_KEYS_W_DP else PANEL_W_DP)

    private fun setPanelWidth(dp: Int) {
        val lp = panel.layoutParams as LayoutParams
        lp.width = context.dp(dp)
        panel.layoutParams = lp
    }

    private fun typed(key: Key) {
        when (key) {
            Key.Backspace -> if (model.glideActive) { pendingWord = null; glide?.stats?.onUndone() } // right after a glide: the word was wrong
            Key.Shift, Key.ToLetters -> {}                                                       // the text does not change
            else -> confirmGlide()                                                                // typing on, the symbols page or Go: the glided word stays
        }
        when (val r = model.press(key)) {
            is KeyResult.Submit -> {
                val typedWords = model.plainWords()
                if (typedWords.isNotEmpty()) glide?.learnTyped(typedWords, ownWords) { text -> post { l.navGlideWordsChanged(text) } }
                closeKeyboard(); l.navSubmit(r.text)
            }
            KeyResult.None -> { if (!model.glideActive) keys.clearCandidates(); bar.invalidate(); keys.invalidate() }
        }
    }

    // ------------------------------------------------------------------ the slide (its own clock: independent of Android's animation scale)

    /** Where the slide gets its clock and its next frame from: the real ones, unless a test steps them by hand. */
    internal var clock: () -> Long = { SystemClock.uptimeMillis() }
    internal var nextFrame: (Runnable) -> Unit = { postOnAnimation(it) }
    private var slideToken = 0

    private fun cancelAnim() { slideToken++; anim?.let { removeCallbacks(it) }; anim = null; panel.setLayerType(LAYER_TYPE_NONE, null) }

    private fun slide(from: Float, to: Float, ip: Interpolator, done: () -> Unit) {
        panel.translationY = from
        panel.setLayerType(LAYER_TYPE_HARDWARE, null)
        val start = clock()
        val token = ++slideToken
        val step = object : Runnable {
            override fun run() {
                if (token != slideToken) return // a newer slide (or a jump) took over
                val t = ((clock() - start).toFloat() / SLIDE_MS).coerceIn(0f, 1f)
                panel.translationY = from + (to - from) * ip.getInterpolation(t)
                if (t < 1f) nextFrame(this) else { anim = null; panel.setLayerType(LAYER_TYPE_NONE, null); done() }
            }
        }
        anim = step
        nextFrame(step)
    }

    // ------------------------------------------------------------------ colours

    private fun applyColors() {
        panel.background = chamferSurface(context, 0xF2000000.toInt(), 8f, strokeColor = pal.deep, strokeDp = 1f, pressable = false)
        for (v in listOf<View>(back, home, forward, bar, keys, toggle)) v.invalidate()
    }

    // ------------------------------------------------------------------ for tests

    internal fun pressKeyForTest(label: String): Boolean = keys.pressLabel(label)
    internal fun pressBarForTest() = bar.tap()
    internal fun pressToggleForTest() = toggle.tap()
    /** Tests: a finger goes down and up on a button (so it lights up and acts), the way a touch would. */
    internal fun tapNavForTest(which: String) {
        val b = when (which) { "back" -> back; "home" -> home; else -> forward }
        val t = SystemClock.uptimeMillis()
        for (a in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) { val e = MotionEvent.obtain(t, t, a, 5f, 5f, 0); b.dispatchTouchEvent(e); e.recycle() }
    }
    internal val barTextForTest: String get() = bar.shownText()
    internal val selectedForTest: Boolean get() = keyboardShowing && model.allSelected
    internal val panelShownForTest: Boolean get() = panel.visibility == VISIBLE
    internal val panelOffsetForTest: Float get() = panel.translationY
    internal fun backLit() = back.lit
    internal fun backDim() = back.dim
    internal fun forwardDim() = forward.dim
    internal fun homeActive() = home.active

    // ================================================================== views

    /** A view that lights up in the accent for [FLASH_MS] when pressed: a timer, not an animator. */
    private abstract inner class LitView(ctx: Context) : View(ctx) {
        var lit = false; private set
        private val unlight = Runnable { lit = false; invalidate() }
        fun flash() { lit = true; invalidate(); removeCallbacks(unlight); postDelayed(unlight, FLASH_MS) }
        protected fun inside(e: MotionEvent) = e.x >= 0f && e.x <= width && e.y >= 0f && e.y <= height
    }

    private inner class NavButton(ctx: Context, icon: Int, desc: String, val action: () -> Unit) : LitView(ctx) {
        private val d = ctx.getDrawable(icon)!!.mutate()
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = ctx.dpf(1.2f) }
        private val box = RectF()
        var dim = false; set(v) { if (field != v) { field = v; invalidate() } }
        var active = false; set(v) { if (field != v) { field = v; invalidate() } }

        init { contentDescription = desc; isClickable = true }

        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (lit) {
                val inset = context.dpf(3f); val r = context.dpf(7f)
                box.set(inset, inset, w - inset, h - inset)
                fill.color = (pal.accent and 0x00FFFFFF) or 0x66000000; line.color = pal.accent
                c.drawRoundRect(box, r, r, fill); c.drawRoundRect(box, r, r, line)
            }
            val s = context.dp(22); val x = ((w - s) / 2).toInt(); val y = ((h - s) / 2).toInt()
            d.setBounds(x, y, x + s, y + s)
            d.setTint(if (lit) pal.accentHi else if (active) pal.accent else pal.fg)
            d.alpha = if (dim && !lit) 90 else 255
            d.draw(c)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { flash(); isPressed = true }
                MotionEvent.ACTION_UP -> { isPressed = false; if (inside(e)) performClick() }
                MotionEvent.ACTION_CANCEL -> isPressed = false
            }
            return true
        }

        override fun performClick(): Boolean { action(); return super.performClick() }
    }

    /** The bar under the buttons: the page's address, or what is being typed with everything selected when the keyboard opens. */
    private inner class BarView(ctx: Context) : LitView(ctx) {
        private val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13f, resources.displayMetrics); typeface = Typeface.DEFAULT }
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = ctx.dpf(1f) }
        private val box = RectF()

        init { contentDescription = "Address. Tap to type a new one."; isClickable = true }

        /** What the bar shows right now (also read by the tests). */
        fun shownText(): String = if (keyboardShowing) model.text else display(address)

        private fun display(url: String) = url.removePrefix("https://").removePrefix("http://").removePrefix("www.").removeSuffix("/")

        fun tap() { flash(); if (keyboardShowing) closeKeyboard() else openKeyboard() }

        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat(); val r = context.dpf(6f)
            box.set(0f, 0f, w, h)
            box.inset(context.dpf(0.5f), context.dpf(0.5f))
            fill.color = if (lit) (pal.accent and 0x00FFFFFF) or 0x66000000 else 0xFF0A1218.toInt()
            line.color = if (lit || keyboardShowing) pal.accent else pal.deep
            c.drawRoundRect(box, r, r, fill); c.drawRoundRect(box, r, r, line)
            val pad = context.dpf(10f)
            val avail = (w - 2 * pad).coerceAtLeast(1f)
            val raw = shownText()
            val fm = text.fontMetrics
            val base = (h - (fm.ascent + fm.descent)) / 2f
            if (raw.isEmpty()) {
                text.color = pal.muted
                c.drawText(TextUtils.ellipsize(if (keyboardShowing) "type an address or search" else "Search or type a URL", text, avail, TextUtils.TruncateAt.END).toString(), pad, base, text)
                if (keyboardShowing) drawCaret(c, pad, h)
                return
            }
            val shown = TextUtils.ellipsize(raw, text, avail - if (keyboardShowing) context.dpf(4f) else 0f, if (keyboardShowing) TextUtils.TruncateAt.START else TextUtils.TruncateAt.END).toString()
            val tw = text.measureText(shown)
            if (keyboardShowing && model.allSelected) { // "selects everything": the whole address lit, the way a selection looks
                fill.color = (pal.accent and 0x00FFFFFF) or 0x77000000
                c.drawRoundRect(RectF(pad - context.dpf(2f), h * 0.2f, pad + tw + context.dpf(2f), h * 0.8f), context.dpf(3f), context.dpf(3f), fill)
            }
            text.color = pal.fg
            c.drawText(shown, pad, base, text)
            if (keyboardShowing && !model.allSelected) drawCaret(c, pad + tw + context.dpf(1.5f), h)
        }

        private fun drawCaret(c: Canvas, x: Float, h: Float) {
            fill.color = pal.accent
            c.drawRect(x, h * 0.25f, x + context.dpf(1.5f), h * 0.75f, fill)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { flash(); isPressed = true }
                MotionEvent.ACTION_UP -> { isPressed = false; if (inside(e)) { if (keyboardShowing) closeKeyboard() else openKeyboard() } }
                MotionEvent.ACTION_CANCEL -> isPressed = false
            }
            return true
        }

        override fun performClick(): Boolean { tap(); return super.performClick() }
    }

    /** The small tab at the very top right that shows or hides the panel. */
    private inner class ToggleView(ctx: Context) : LitView(ctx) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = ctx.dpf(1f) }
        private val chevron = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = ctx.dpf(2f); strokeCap = Paint.Cap.SQUARE; strokeJoin = Paint.Join.MITER }
        private val box = RectF()
        private val path = Path()

        init { contentDescription = "Mini navigation panel"; isClickable = true }

        fun tap() { flash(); setOpen(!isOpen, true) }

        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat(); val r = context.dpf(7f)
            box.set(context.dpf(4f), 0f, w, h - context.dpf(2f))
            fill.color = if (lit) (pal.accent and 0x00FFFFFF) or 0x77000000 else 0xB3000000.toInt()
            line.color = if (lit) pal.accent else pal.deep
            c.drawRoundRect(box, r, r, fill); c.drawRoundRect(box, r, r, line)
            chevron.color = if (lit) pal.accentHi else pal.muted
            val cx = (box.left + box.right) / 2f; val cy = (box.top + box.bottom) / 2f
            val a = context.dpf(5f); val b = context.dpf(2.5f)
            path.reset()
            if (isOpen) { path.moveTo(cx - a, cy + b); path.lineTo(cx, cy - b); path.lineTo(cx + a, cy + b) } // an arrow up: hide
            else { path.moveTo(cx - a, cy - b); path.lineTo(cx, cy + b); path.lineTo(cx + a, cy - b) }        // an arrow down: show
            c.drawPath(path, chevron)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { flash(); isPressed = true }
                MotionEvent.ACTION_UP -> { isPressed = false; if (inside(e)) setOpen(!isOpen, true) }
                MotionEvent.ACTION_CANCEL -> isPressed = false
            }
            return true
        }

        override fun performClick(): Boolean { tap(); return super.performClick() }
    }

    /** The keys, drawn and hit-tested by one view (no view per key). A key lights up in the accent for [FLASH_MS] when pressed; Backspace repeats while held. */
    private inner class KeysView(ctx: Context) : View(ctx) {
        var onKey: ((Key) -> Unit)? = null
        private val handler = Handler(Looper.getMainLooper())
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT }
        private val box = RectF()
        private var rowsFor = -1
        private var rows: List<List<KeySpec>> = emptyList()
        private var rects: List<List<RectF>> = emptyList()
        private var rectsW = -1; private var rectsH = -1
        private var down: KeySpec? = null
        private var litKey: KeySpec? = null
        private val unlight = Runnable { litKey = null; invalidate() }
        private val repeat = object : Runnable { override fun run() { down?.key?.let { onKey?.invoke(it) }; handler.postDelayed(this, REPEAT_MS) } }

        init { contentDescription = "Keyboard"; isClickable = true }

        private fun rowsNow(): List<List<KeySpec>> {
            val k = (if (model.symbols) 1 else 0) or (if (model.shift) 2 else 0)
            if (k != rowsFor) { rowsFor = k; rows = MiniKeyboardLayout.rows(model.symbols, model.shift); rectsW = -1 }
            return rows
        }

        private fun layoutKeys() {
            val rs = rowsNow()
            if (rectsW == width && rectsH == height && rects.isNotEmpty()) return
            rectsW = width; rectsH = height
            val gap = context.dpf(3f)
            val rowH = (height - gap * (rs.size - 1)) / rs.size
            val out = ArrayList<List<RectF>>()
            for ((i, row) in rs.withIndex()) {
                val total = row.sumOf { it.weight.toDouble() }.toFloat()
                val unit = (width - gap * (row.size - 1)) / total
                var x = 0f
                val line = ArrayList<RectF>()
                for (k in row) { val w = k.weight * unit; line.add(RectF(x, i * (rowH + gap), x + w, i * (rowH + gap) + rowH)); x += w + gap }
                out.add(line)
            }
            rects = out
        }

        private fun keyAt(x: Float, y: Float): KeySpec? {
            layoutKeys()
            for ((i, row) in rects.withIndex()) for ((j, r) in row.withIndex()) if (r.contains(x, y)) return rows[i][j].takeIf { it.key != null }
            // between two keys: the nearest one in that row, so a thumb that lands on a gap still types
            for ((i, row) in rects.withIndex()) if (y >= row[0].top - context.dpf(1.5f) && y <= row[0].bottom + context.dpf(1.5f)) {
                var best: KeySpec? = null; var bestD = Float.MAX_VALUE
                for ((j, r) in row.withIndex()) { val k = rows[i][j]; if (k.key == null) continue; val d = Math.abs(x - r.centerX()); if (d < bestD) { bestD = d; best = k } }
                return best
            }
            return null
        }

        private fun light(k: KeySpec?) { litKey = k; invalidate(); handler.removeCallbacks(unlight); if (k != null) handler.postDelayed(unlight, FLASH_MS) }

        override fun onDraw(c: Canvas) {
            layoutKeys()
            val r = context.dpf(6f)
            for ((i, row) in rows.withIndex()) for ((j, k) in row.withIndex()) {
                val key = k.key ?: continue
                box.set(rects[i][j])
                val isLit = k === litKey
                val special = key !is Key.Ch || k.label.length > 1
                fill.color = when {
                    isLit -> (pal.accent and 0x00FFFFFF) or 0xAA000000.toInt()
                    key === Key.Go -> pal.accent
                    key === Key.Shift && model.shift -> pal.deep
                    special -> 0xFF16232C.toInt()
                    else -> 0xFF0E171E.toInt()
                }
                c.drawRoundRect(box, r, r, fill)
                text.color = when { isLit -> pal.accentHi; key === Key.Go -> pal.accentOn; else -> pal.fg }
                text.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, if (k.label.length > 1) 12f else 16f, resources.displayMetrics)
                val fm = text.fontMetrics
                c.drawText(k.label, box.centerX(), box.centerY() - (fm.ascent + fm.descent) / 2f, text)
            }
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    down = keyAt(e.x, e.y); light(down)
                    if (down?.key === Key.Backspace) { onKey?.invoke(Key.Backspace); handler.postDelayed(repeat, REPEAT_FIRST_MS) }
                }
                MotionEvent.ACTION_MOVE -> { val k = keyAt(e.x, e.y); if (k !== down) { down = k; handler.removeCallbacks(repeat); light(k) } }
                MotionEvent.ACTION_UP -> {
                    val k = keyAt(e.x, e.y)
                    handler.removeCallbacks(repeat)
                    if (k != null && k === down && k.key !== Key.Backspace) { light(k); onKey?.invoke(k.key!!) }
                    down = null
                }
                MotionEvent.ACTION_CANCEL -> { handler.removeCallbacks(repeat); down = null }
            }
            return true
        }

        /** Tests: press the key labelled [label] as a finger would. */
        fun pressLabel(label: String): Boolean {
            val k = rowsNow().flatten().firstOrNull { it.label == label && it.key != null } ?: return false
            light(k); onKey?.invoke(k.key!!)
            return true
        }

        override fun performClick(): Boolean = super.performClick()
    }

    companion object {
        const val SLIDE_MS = 200L
        /** How long a pressed button, key or bar stays lit. */
        const val FLASH_MS = 130L
        private const val REPEAT_FIRST_MS = 380L
        private const val REPEAT_MS = 70L
        const val TOGGLE_W_DP = 44
        const val TOGGLE_H_DP = 26
        const val PANEL_W_DP = 176
        const val PANEL_KEYS_W_DP = 304
        const val NAV_W_DP = 52
        const val NAV_H_DP = 42
        const val BAR_H_DP = 34
        const val KEYS_H_DP = 161
    }
}
