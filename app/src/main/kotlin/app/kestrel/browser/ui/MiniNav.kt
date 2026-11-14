package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.InsetDrawable
import android.view.ViewConfiguration
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
import android.view.HapticFeedbackConstants
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
import app.kestrel.core.input.TextEdit
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
 * AU1 (owner message 35: "make the top right slide down box ... have no visible button at all"): the small tab is gone. A long press on the invisible square in the top right corner of the page ([HotCorner])
 * slides the panel open, and a long press on the same square (it sits ABOVE the open panel) slides it closed; [toggle] is what that calls.
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
        /** The titles of the pages visited and bookmarked (asked when the keyboard opens; answer on any thread): the words of the titles join the words that can be glided (AV2). */
        fun navGlideTitles(done: (List<String>) -> Unit) {}
        /** What the keyboard learned from the person's choices and typing (AU2), as the engine wrote it; read and written on the glide thread (any thread is fine). */
        fun navGlideLearned(): String? = null
        fun navGlideLearnedSave(text: String) {}
        /** AU3: the text field of the web page that has focus right now, or null (none, or this build has no page fields). Asked when the keyboard opens and before every key. */
        fun navPageField(): PageField? = null
        /** AU3: the keyboard opened or closed (while it is open the system keyboard must stay down). */
        fun navKeyboardChanged(showing: Boolean) {}
        /** The tab in front is a private one: the keyboard learns nothing there. */
        fun navPrivateTab(): Boolean = false
        /** AV1: where the person put the panel and how big they made the keyboard: the panel's distance from the right edge and from the top, the keyboard's width and the height of its rows of keys, all in dp; null = docked under the corner at the first size. */
        fun navGeometry(): IntArray? = null
        fun navGeometryChanged(g: IntArray?) {}
        /** A short tick on every key press (the system's touch feedback setting still applies; a tablet without a vibrator has none). */
        fun navKeyHaptics(): Boolean = true
    }

    private val model = MiniKeyboardModel()
    private val panel = PanelLayout(ctx)
    private val back = NavButton(ctx, R.drawable.ic_back, "Back") { l.navBack() }
    private val home = NavButton(ctx, R.drawable.ic_home, "Home") { l.navHome() }
    private val forward = NavButton(ctx, R.drawable.ic_forward, "Forward") { l.navForward() }
    private val bar = BarView(ctx)
    private val keys = KeysView(ctx)
    private var address = ""
    private var anim: Runnable? = null

    // ---- glide typing (AT1)
    private var glideOn = false
    private var glide: GlideTyping? = null
    private val glideExec by lazy { Executors.newSingleThreadExecutor { r -> Thread(r, "kestrel-glide").apply { isDaemon = true } } }
    private val previewExec by lazy { Executors.newSingleThreadExecutor { r -> Thread(r, "kestrel-glide-preview").apply { isDaemon = true; priority = Thread.NORM_PRIORITY - 1 } } }
    private var ownWords = UserWords()
    private var siteWords = emptyList<String>()
    private var pendingWord: String? = null     // the word the last glide put in: kept when the next thing is not a correction
    private var pendingPrev: String? = null     // the word before it (the language's context for what is learned)
    private var pendingPrev2: String? = null    // and the one before that
    private var topGuess: String? = null        // the first guess the decoder made for the last glide (what a pick or a deletion corrects)
    private var pendingGuessed: String? = null  // the word the person picked INSTEAD of this guess: the correction that is learned when the picked word is kept
    private var lastDeleted: String? = null     // the glided word that was just deleted: the word that is kept next may be what they meant instead
    private var deletedAt = 0L                  // when it was deleted: a glide a few seconds later that gives the same word first again is a retry, so that word moves to second place
    private var pendingWeight = 1f              // 3 when the person CHOSE it from the strip: learned (like a kept glide) only when the next thing is not a Backspace
    private var lastGlidePrev: String? = null   // the word before the glide that is being decoded
    private var lastGlidePrev2: String? = null  // and the one before that
    private var liftAt = 0L                     // when the finger lifted: the time until the word is on screen is counted
    private var titleCounts = emptyMap<String, Int>() // words of the titles of visited pages
    private var afterUndo = false               // a glided word was just deleted: the next word that is kept is the one meant instead
    private var retypeWatch = false             // a glided word was just deleted: if the person types the word letter by letter instead, the deleted glide's shape is remembered for it (AX1)
    private var retypeLetters = 0               // how many letters were typed since
    private var retypeAt = 0L                   // when the word was deleted
    /** Tests: where the engine comes from (the real one reads the shipped word list). */
    internal var glideFactory: (() -> GlideTyping)? = null

    // ---- typing into the page's own text field (AU3)
    private var pageField: PageField? = null         // the page field the keys go into (null: the bar is the address / search bar)
    private var fieldIdentity = 0                // which field it is: another number means the text typed so far belongs to a field that is gone
    private var learnedText: String? = null      // the text whose words were already handed to the keyboard's memory (closing after Enter must not count them twice)
    /** The keyboard is open and its keys go into a text field of the page. */
    val typesIntoPage: Boolean get() = keyboardShowing && pageField != null
    private var fieldOpenings = 0; private var addressOpenings = 0; private var fieldSent = 0; private var fieldEnters = 0; private var lastFieldLabel = "-"

    /** What menu > Diagnostics shows for typing into page fields: whether the keyboard found a field, what kind it was, how much went in. */
    fun fieldLine(): String =
        if (fieldOpenings + addressOpenings == 0) "Mini keyboard in page fields: not opened yet this run"
        else "Mini keyboard in page fields: opened $fieldOpenings times with a page field focused (last: $lastFieldLabel; $fieldSent characters sent, Enter pressed $fieldEnters times) and $addressOpenings times for the address bar"

    /** The panel is shown (sliding in counts as shown). */
    var isOpen = false; private set
    var keyboardShowing = false; private set

    /** False hides the panel altogether (full-screen video; Settings can switch the feature off). */
    var available = true
        set(v) { field = v; visibility = if (v) VISIBLE else GONE }

    init {
        isClickable = false // a touch outside the panel belongs to the page below
        panel.orientation = LinearLayout.VERTICAL
        panel.isClickable = true
        val edge = ctx.dp(PANEL_PAD_DP) + ctx.dp(BAND_DP) // the frame inside the outline plus the transparent band around it (where the grip and the corner marks are)
        panel.setPadding(edge, edge, edge, edge)
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        for (b in listOf(back, home, forward)) row.addView(b, LinearLayout.LayoutParams(ctx.dp(NAV_W_DP), ctx.dp(NAV_H_DP)))
        panel.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        panel.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(BAR_H_DP)).apply { topMargin = ctx.dp(4) })
        panel.addView(keys, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(KEYS_H_DP)).apply { topMargin = ctx.dp(4) })
        keys.visibility = GONE
        panel.visibility = GONE
        addView(panel, LayoutParams(ctx.dp(PANEL_W_DP) + 2 * ctx.dp(BAND_DP), LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END))
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
        if (keyboardShowing) syncField() // the page changed: the field it had may be gone
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

    /** Shows or hides the panel with a long press on the invisible corner (AU1): the other state. */
    fun toggle() { setOpen(!isOpen, true) }

    /** Shows or hides the panel; [animate] false jumps (the start of the app). */
    fun setOpen(open: Boolean, animate: Boolean = true, notify: Boolean = true) {
        if (open == isOpen) return
        isOpen = open
        cancelAnim()
        if (open) {
            val wasVisible = panel.visibility == VISIBLE // reopened while it was still sliding away: carry on from where it is
            panel.visibility = VISIBLE
            panel.measure(MeasureSpec.makeMeasureSpec((panel.layoutParams as LayoutParams).width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            val hidden = -(panelTopPx() + panel.measuredHeight).toFloat()
            if (animate) slide(if (wasVisible) panel.translationY else hidden, 0f, DecelerateInterpolator(2f)) {} else panel.translationY = 0f
        } else {
            if (keyboardShowing) closeKeyboard()
            val hidden = -(panelTopPx() + panel.height.coerceAtLeast(panel.measuredHeight)).toFloat()
            if (animate) slide(panel.translationY, hidden, AccelerateInterpolator(1.4f)) { panel.visibility = GONE; panel.translationY = 0f }
            else { panel.visibility = GONE; panel.translationY = 0f }
        }
        if (notify) l.navOpenChanged(open)
    }

    fun openKeyboard() {
        if (!isOpen || keyboardShowing) return
        val f = l.navPageField()              // a text field of the page has focus: the keys go there, from an empty text (the address is not selected)
        pageField = f; fieldIdentity = f?.identity ?: 0; learnedText = null
        if (f != null) { fieldOpenings++; lastFieldLabel = f.label } else addressOpenings++
        model.open(if (f != null) "" else address)
        keyboardShowing = true
        keys.visibility = VISIBLE
        applyGeometry()
        pendingWord = null
        preparedFor = ""
        startGlideEngine()
        barMode(); keys.invalidate()
        l.navKeyboardChanged(true)
    }

    fun closeKeyboard() {
        if (!keyboardShowing) return
        confirmGlide()
        if (pageField != null) learnFromText()     // typed into a page field: there is no Go to wait for
        glide?.saveLearned()
        keys.reset()
        keyboardShowing = false
        pageField = null
        keys.visibility = GONE
        applyGeometry()
        barMode()
        l.navKeyboardChanged(false)
    }

    /** The activity tells the panel that the page's focused field changed (focus moved, a field appeared or went away): the keys follow it. */
    fun pageFieldChanged() { if (keyboardShowing) syncField() }

    /** The bar's description says where the keys go. */
    private fun barMode() {
        bar.contentDescription = if (!keyboardShowing) "Address. Tap to type a new one." else if (pageField != null) "Typing into the text field of the page. Tap to close the keyboard." else "Address. Tap to close the keyboard."
        bar.invalidate()
    }

    // ------------------------------------------------------------------ glide typing (AT1)

    /** Settings switched glide typing on or off: the strip above the keys appears or goes, and the keys' view changes height by it. */
    fun setGlideEnabled(on: Boolean) {
        if (on == glideOn) return
        glideOn = on
        applyGeometry()
        keys.reset()
        if (keyboardShowing) { if (on) startGlideEngine(); keys.invalidate() }
    }

    /** Settings > "Forget what the keyboard learned": the own words, the learned word habits and the thumb habit are gone (the activity has cleared what is stored). */
    fun forgetLearned() {
        ownWords = UserWords(); siteWords = emptyList(); pendingWord = null; afterUndo = false; retypeWatch = false
        glide?.forgetAll()
        post { pushOwnWords() }
    }

    /** What menu > Diagnostics shows for it. */
    fun glideLine(): String = glide?.stats?.line() ?: "Glide typing: " + if (glideOn) "not used yet this run" else "off (Settings)"

    private fun startGlideEngine() {
        if (!glideOn) return
        if (glide == null) {
            glide = glideFactory?.invoke() ?: GlideTyping({ try { context.assets.open("glide/en.klx").use { it.readBytes() } } catch (e: Exception) { null } }, glideExec, loadPersonal = { l.navGlideLearned() }, savePersonal = { l.navGlideLearnedSave(it) }, previewExecutor = previewExec)
            ownWords = UserWords.parse(l.navGlideWords())
        }
        prepareGlide()
        pushOwnWords()
        // the names of the sites visited so far (asked at every opening: a site visited a minute ago can be glided now)
        l.navGlideHosts { hosts -> post { siteWords = hosts.mapNotNull { UserWords.wordOfHost(it) }.distinct().take(300); pushOwnWords() } }
        // ... and the words of the titles of the pages visited and bookmarked (a movie's name can be glided once its page was seen)
        l.navGlideTitles { titles -> post { titleCounts = titles.flatMap { UserWords.wordsOfTitle(it) }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(400).associate { it.key to it.value }; pushOwnWords() } }
    }

    private var preparedFor = ""

    /** The decoder needs the real key positions: they are known once the keys view has a size (and again when it changes). Cheap when nothing changed. */
    internal fun prepareGlide() {
        if (resizing) return
        val g = glide ?: return
        val k = keys.glideKeys() ?: return
        val tag = "${keys.width}x${keys.height}"
        if (tag == preparedFor) return
        preparedFor = tag
        g.prepare(k, l.navGlideHabit(), mergedWords())
    }

    private fun mergedWords(): UserWords {
        val m = UserWords.parse(ownWords.serialize(), 1400)
        for (w in siteWords) m.add(w)
        for ((w, n) in titleCounts) m.add(w, Math.min(n, 4))
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
        if (learnAllowed()) {
            // kept: the keyboard hears it (a word the person chose, or one that replaced a deleted one, counts more); a correction remembers what it was picked over (a pick) or put in the place of (a deletion)
            val guessed = pendingGuessed ?: if (afterUndo) lastDeleted else null
            g.learn(pendingPrev, w, maxOf(pendingWeight, if (afterUndo) 3f else 1f), pendingPrev2, guessed?.takeIf { it != w }, if (pendingGuessed != null) 1f else 0.5f, fromGlide = true, afterUndo = afterUndo)
        }
        pendingWeight = 1f; pendingGuessed = null
        afterUndo = false
    }

    /** A live guess for the glide that is still going on: the strip shows it (the first one is the word that would be typed if the finger lifted now). */
    private fun previewDone(res: GlideResult) {
        if (!keyboardShowing || !keys.isGliding) return
        if (res.candidates.isEmpty()) keys.clearCandidates() else keys.showCandidates(res.candidates.map { it.text })
    }

    private fun glideDone(res: GlideResult) {
        val g = glide ?: return
        if (liftAt != 0L) { g.stats.onShown(SystemClock.uptimeMillis() - liftAt); liftAt = 0L }
        if (!keyboardShowing) return
        var cands = res.candidates
        if (cands.isEmpty()) { keys.showNoWord(); return }
        // a retry: the word they just deleted comes up first again, so it is wrong again for them: the next one goes first and the deleted one stays in the strip, second
        val rejected = lastDeleted
        if (afterUndo && rejected != null && cands.size >= 2 && cands[0].text == rejected && SystemClock.uptimeMillis() - deletedAt <= RETRY_MS && cands[1].perLetter - cands[0].perLetter <= RETRY_GAP) {
            cands = listOf(cands[1], cands[0]) + cands.drop(2)
            g.stats.onRetryDemoted()
        }
        val best = cands[0]
        retypeWatch = false // a new glide: its own confirmation handles the deleted one
        syncField()
        val before = model.text
        model.commitGlide(best.text)
        pageField?.let { sendEdit(it, before) }
        pendingWord = best.text; pendingWeight = 1f; pendingGuessed = null
        topGuess = best.text
        pendingPrev = lastGlidePrev; pendingPrev2 = lastGlidePrev2
        keys.showCandidates(cands.map { it.text })
        bar.invalidate()
    }

    /** A candidate in the strip was tapped: it replaces the word the glide put in. */
    private fun pickCandidate(word: String) {
        val g = glide
        syncField()
        val before = model.text
        if (!model.swapGlide(word)) return
        pageField?.let { sendEdit(it, before) }
        pendingWord = word; pendingWeight = 3f // the person CHOSE this word over the keyboard's first guess: it counts three times, once it is kept (a Backspace right after takes it back)
        pendingGuessed = topGuess?.takeIf { it != word } // ... and the keyboard remembers it guessed the other one
        afterUndo = false
        g?.stats?.onPicked()
        g?.accepted(word) { h -> post { l.navGlideHabitChanged(h) } }
        keys.showCandidates(keys.candidateTexts().let { list -> listOf(word) + list.filter { it != word } }.take(3))
        bar.invalidate()
    }

    // ------------------------------------------------------------------ where the panel is and how big its keyboard is (AV1, owner message 36)

    private val density: Float get() = resources.displayMetrics.density
    // the panel's OUTLINE in dp: its distance from the right edge and from the top of the page area, the width it has while the keyboard is open, and the height of the rows of keys
    private var gRight = DOCK_RIGHT_DP.toFloat(); private var gTop = HotCorner.SIZE_DP.toFloat()
    private var gW = PANEL_KEYS_W_DP.toFloat(); private var gH = KEYS_H_DP.toFloat()
    private var availWdp = 100000f; private var availHdp = 100000f   // the page area, known once it has been measured
    private var resizing = false                                      // a corner is being dragged: the decoder is told the key positions when it is over, not at every frame
    init { loadGeometry() } // after the fields above: Kotlin runs initialisers in the order they are written, an earlier init block would be overwritten by their defaults

    /** The panel has been moved away from the corner it starts in. */
    val isDetached: Boolean get() = Math.abs(gRight - DOCK_RIGHT_DP) > 0.5f || Math.abs(gTop - HotCorner.SIZE_DP) > 0.5f
    /** The keyboard has another size than the first one. */
    val isResized: Boolean get() = Math.abs(gW - PANEL_KEYS_W_DP) > 0.5f || Math.abs(gH - KEYS_H_DP) > 0.5f

    private fun outlineW(): Float = if (keyboardShowing) gW else PANEL_W_DP.toFloat()
    private fun outlineH(): Float = 12f + NAV_H_DP + 4f + BAR_H_DP + (if (keyboardShowing) 4f + gH + (if (glideOn) STRIP_H_DP else 0) else 0f)
    private fun maxW(): Float = Math.max(MIN_KEYS_W_DP, Math.min(MAX_KEYS_W_DP, availWdp - 4f))
    private fun maxH(): Float = Math.max(MIN_KEYS_H_DP, Math.min(MAX_KEYS_H_DP, availHdp - (12f + NAV_H_DP + 4f + BAR_H_DP + 4f + (if (glideOn) STRIP_H_DP else 0)) - 2f * BAND_DP))
    /** Where the outline is shown: what was asked for, kept inside the page area (the stored place is not changed by this: a screen that is smaller for a moment must not move the panel for good). */
    private fun effRight(): Float = gRight.coerceIn(MIN_RIGHT_DP, Math.max(MIN_RIGHT_DP, availWdp - outlineW() - MIN_RIGHT_DP))
    private fun effTop(): Float = gTop.coerceIn(BAND_DP.toFloat(), Math.max(BAND_DP.toFloat(), availHdp - outlineH() - 4f))

    private fun panelTopPx(): Int = (panel.layoutParams as LayoutParams).topMargin

    /** The place and the size in the layout parameters: called whenever the geometry, the keyboard or the page area changes. */
    private fun placePanel() {
        val bandPx = context.dp(BAND_DP)
        val lp = panel.layoutParams as LayoutParams
        lp.width = Math.round(outlineW() * density) + 2 * bandPx
        lp.rightMargin = Math.round(effRight() * density) - bandPx
        lp.topMargin = Math.round(effTop() * density) - bandPx
        lp.gravity = Gravity.TOP or Gravity.END
        val klp = keys.layoutParams as LinearLayout.LayoutParams
        klp.height = Math.round((gH + if (glideOn) STRIP_H_DP else 0) * density)
    }

    private fun applyGeometry() { placePanel(); panel.requestLayout(); requestLayout(); panel.invalidate() }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        availWdp = MeasureSpec.getSize(widthSpec) / density; availHdp = MeasureSpec.getSize(heightSpec) / density
        placePanel() // the page area is known now: the margins follow it (no requestLayout: this IS the layout)
        super.onMeasure(widthSpec, heightSpec)
    }

    private fun loadGeometry() {
        val g = l.navGeometry()
        if (g != null && g.size == 4) {
            gRight = g[0].toFloat(); gTop = g[1].toFloat()
            gW = g[2].toFloat().coerceIn(MIN_KEYS_W_DP, MAX_KEYS_W_DP); gH = g[3].toFloat().coerceIn(MIN_KEYS_H_DP, MAX_KEYS_H_DP)
        }
        placePanel()
    }

    private fun saveGeometry() {
        l.navGeometryChanged(if (!isDetached && !isResized) null else intArrayOf(Math.round(gRight), Math.round(gTop), Math.round(gW), Math.round(gH)))
    }

    /** What was stored is brought inside the page area (after a drag). */
    private fun clampStored() {
        gW = gW.coerceIn(MIN_KEYS_W_DP, maxW()); gH = gH.coerceIn(MIN_KEYS_H_DP, maxH())
        gRight = effRight(); gTop = effTop()
    }

    /** Back under the corner it starts in; the size stays (a double tap on the grip does this). */
    fun dock() { gRight = DOCK_RIGHT_DP.toFloat(); gTop = HotCorner.SIZE_DP.toFloat(); applyGeometry(); saveGeometry() }

    /** Settings > "Reset the mini keyboard's size and position": the corner and the first size. */
    fun resetGeometry() {
        gRight = DOCK_RIGHT_DP.toFloat(); gTop = HotCorner.SIZE_DP.toFloat(); gW = PANEL_KEYS_W_DP.toFloat(); gH = KEYS_H_DP.toFloat()
        applyGeometry(); l.navGeometryChanged(null); keys.invalidate()
        if (keyboardShowing) post { prepareGlide() }
    }

    /** True when the window point is on the panel (the activity does not treat a long press there as the corner's). */
    fun coversWindowPoint(x: Float, y: Float): Boolean {
        if (!isOpen || panel.visibility != VISIBLE) return false
        val loc = IntArray(2); panel.getLocationInWindow(loc)
        return x >= loc[0] && x < loc[0] + panel.width && y >= loc[1] && y < loc[1] + panel.height
    }

    private var lastShiftAt = -1L

    private fun typed(key: Key) {
        syncField() // the page's field may have changed or gone since the last key
        if (key === Key.Shift) { // Shift tapped twice quickly: caps lock
            val now = SystemClock.uptimeMillis()
            if (model.shift && !model.shiftLock && lastShiftAt >= 0L && now - lastShiftAt < DOUBLE_TAP_MS) { model.lockShift(); lastShiftAt = -1L; bar.invalidate(); keys.invalidate(); return }
            lastShiftAt = now
        }
        val f = pageField
        when (key) {
            Key.Backspace -> if (model.glideActive) { // right after a glide: the word was wrong
                lastDeleted = pendingWord ?: topGuess; deletedAt = SystemClock.uptimeMillis(); pendingWord = null; pendingGuessed = null; afterUndo = true; glide?.stats?.onUndone()
                if (learnAllowed()) { glide?.glideDeleted(lastDeleted); retypeWatch = true; retypeLetters = 0; retypeAt = deletedAt } // its shape waits for the word that comes instead
            }
            Key.Shift, Key.ToLetters -> {}                                                       // the text does not change
            else -> { confirmGlide(); if (key is Key.Ch) afterUndo = false }                     // typing on, the symbols page or Go: the glided word stays
        }
        if (retypeWatch && key is Key.Ch && key.text.length == 1 && key.text[0].isLetter()) retypeLetters++
        if (f != null && key === Key.Go) { fieldGo(f); return }
        val before = model.text
        when (val r = model.press(key)) {
            is KeyResult.Submit -> {
                finishRetype()
                learnFromText() // what was typed letter by letter is remembered (the glided words were learned when they were kept)
                closeKeyboard(); l.navSubmit(r.text)
            }
            KeyResult.None -> {
                if (retypeWatch && key is Key.Ch && key.text == " ") finishRetype()
                if (f != null) sendEdit(f, before, key === Key.Backspace)
                if (!model.glideActive) keys.clearCandidates(); bar.invalidate(); keys.invalidate()
            }
        }
    }

    // ------------------------------------------------------------------ typing into the page's own text field (AU3)

    /**
     * The page's focused field may have changed since the keys last went somewhere. The same field: carry on. Another field, or one that appeared: the keys go there, from an empty text. A field that is gone
     * (the page moved on, something else took focus): the bar is the address / search bar again. What was typed into a field that is left behind counts for the keyboard's memory.
     */
    private fun syncField() {
        if (!keyboardShowing) return
        val f = l.navPageField()
        val was = pageField
        if (f == null && was == null) return
        if (f != null && was != null && f.identity == fieldIdentity) { pageField = f; return } // the same field (a fresh object: its type may have changed)
        if (was != null) { confirmGlide(); learnFromText() }
        pageField = f; fieldIdentity = f?.identity ?: 0
        if (f != null) { fieldOpenings++; lastFieldLabel = f.label }
        learnedText = null
        model.open(if (f != null) "" else address)
        pendingWord = null; afterUndo = false; retypeWatch = false
        keys.clearCandidates(); barMode(); keys.invalidate()
    }

    /** The text model changed from [before]: the same change goes into the page's field (what was taken off the end, what was added). */
    private fun sendEdit(f: PageField, before: String, backspace: Boolean = false) {
        var e = TextEdit.between(before, model.text)
        if (backspace && e.isEmpty) e = TextEdit(1, "") // nothing typed here is left to take off: the field's own text goes
        if (e.remove > 0) f.deleteBefore(e.remove)
        if (e.insert.isNotEmpty()) { f.insert(e.insert); fieldSent += e.insert.length }
    }

    /** Go in a page field presses Enter there; a field whose Enter submits (a search box) closes the keyboard, a text area stays open with an empty text for the next line. */
    private fun fieldGo(f: PageField) {
        finishRetype()
        learnFromText()
        f.enter(); fieldEnters++
        if (f.enterSubmits) closeKeyboard()
        else { model.open(""); learnedText = null; keys.clearCandidates(); bar.invalidate(); keys.invalidate() }
    }

    /**
     * The person deleted a glided word and typed another one letter by letter: the word just typed (at the space or Go) may be what the deleted glide meant, and the keyboard remembers the glide's
     * shape for it when the straight line through the word's letters is not far from it (AX1). Only the first word after the deletion, only within [RETYPE_MS], only after at least two letters.
     */
    private fun finishRetype() {
        if (!retypeWatch) return
        retypeWatch = false
        if (retypeLetters < 2 || SystemClock.uptimeMillis() - retypeAt > RETYPE_MS || !learnAllowed()) return
        model.previousWord()?.let { glide?.retyped(it) }
    }

    /** Nothing is learned from a private tab or a password field. */
    private fun learnAllowed(): Boolean {
        if (l.navPrivateTab()) return false
        val f = pageField ?: return true
        return !f.password && !f.privateTab
    }

    /** The words of the text (typed letter by letter or glided) go to the keyboard's memory, once per text. */
    private fun learnFromText() {
        val g = glide ?: return
        val text = model.text
        if (text.isEmpty() || text == learnedText || !learnAllowed()) return
        learnedText = text
        val typedWords = model.plainWords()
        if (typedWords.isNotEmpty()) g.learnTyped(typedWords, ownWords) { t -> post { l.navGlideWordsChanged(t) } }
        for ((prev2, prev, w) in model.typedContexts()) g.learn(prev, w, 1f, prev2) // what was typed letter by letter (the glided words were learned when they were kept)
    }

    /** Glide typing works unless the page field is a password (the guesses would show it on the screen). */
    private fun glideUsable(): Boolean = glideOn && pageField?.password != true

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
        panel.background = InsetDrawable(chamferSurface(context, 0xF2000000.toInt(), 8f, strokeColor = pal.deep, strokeDp = 1f, pressable = false), context.dp(BAND_DP))
        val edge = context.dp(PANEL_PAD_DP) + context.dp(BAND_DP)
        panel.setPadding(edge, edge, edge, edge) // setting a background takes the padding from the drawable (the inset): the frame and the band are set again here
        for (v in listOf<View>(back, home, forward, bar, keys)) v.invalidate()
    }

    // ------------------------------------------------------------------ for tests

    internal fun pressKeyForTest(label: String): Boolean = keys.pressLabel(label)
    internal fun pressBarForTest() = bar.tap()
    internal fun pressToggleForTest() = toggle()
    /** Tests: a finger on the keys view (pixels of that view), the way the screen would send it. */
    internal fun touchKeysForTest(action: Int, x: Float, y: Float) = keys.touch(action, x, y)
    internal fun keyCenterForTest(label: String): Pair<Float, Float>? = keys.centerOf(label)
    internal fun keysViewForTest(): View = keys
    internal fun chipCenterForTest(i: Int): Pair<Float, Float>? = keys.chipCenter(i)
    internal val candidatesForTest: List<String> get() = keys.candidateTexts()
    internal val keysHeightForTest: Int get() = keys.layoutParams.height
    /** Tests: a finger goes down and up on a button (so it lights up and acts), the way a touch would. */
    internal fun tapNavForTest(which: String) {
        val b = when (which) { "back" -> back; "home" -> home; else -> forward }
        val t = SystemClock.uptimeMillis()
        for (a in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) { val e = MotionEvent.obtain(t, t, a, 5f, 5f, 0); b.dispatchTouchEvent(e); e.recycle() }
    }
    internal val barTextForTest: String get() = bar.shownText()
    internal val typesIntoPageForTest: Boolean get() = typesIntoPage
    internal val bubbleForTest: String? get() = keys.bubbleTextForTest()
    internal val shiftLockForTest: Boolean get() = model.shiftLock
    internal val barDescriptionForTest: String get() = bar.contentDescription.toString()
    /** Tests: the outline of the panel in this view's pixels (left, top, right, bottom), and where its grip and corners are. */
    internal fun outlineForTest(): IntArray { val b = context.dp(BAND_DP); return intArrayOf(panel.left + b, panel.top + b, panel.right - b, panel.bottom - b) }
    internal fun geometryForTest(): FloatArray = floatArrayOf(gRight, gTop, gW, gH)
    /** Tests: how far the buttons, the bar and the keys are inside the outline (the frame). */
    internal fun frameForTest(): Int = panel.paddingLeft - context.dp(BAND_DP)
    internal fun keysSizeForTest(): IntArray = intArrayOf(keys.width, keys.height)
    internal fun zoneForTest(x: Float, y: Float): Int = panel.zoneAt(x - panel.left, y - panel.top)
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
        private val tag = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics); typeface = Typeface.DEFAULT_BOLD }

        init { contentDescription = "Address. Tap to type a new one."; isClickable = true }

        /** What the bar shows right now (also read by the tests). */
        fun shownText(): String = if (!keyboardShowing) display(address) else if (pageField?.password == true) "\u2022".repeat(model.text.length) else model.text

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
            var avail = (w - 2 * pad).coerceAtLeast(1f)
            if (keyboardShowing && pageField != null) { // the keys go into the page: a small tag says so
                tag.color = pal.accent
                val tw0 = tag.measureText("page")
                c.drawText("page", w - pad - tw0, (h - (tag.fontMetrics.ascent + tag.fontMetrics.descent)) / 2f, tag)
                avail = (avail - tw0 - context.dpf(8f)).coerceAtLeast(1f)
            }
            val raw = shownText()
            val fm = text.fontMetrics
            val base = (h - (fm.ascent + fm.descent)) / 2f
            if (raw.isEmpty()) {
                text.color = pal.muted
                c.drawText(TextUtils.ellipsize(if (keyboardShowing) (if (pageField != null) "type into the page" else "type an address or search") else "Search or type a URL", text, avail, TextUtils.TruncateAt.END).toString(), pad, base, text)
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

    /**
     * The panel (AV1, owner message 36: "resizeable by dragging corners ... able to detach and I can move it around the screen"). Its OUTLINE is what the person sees; around it is a transparent band that belongs to the
     * page, except for two things drawn and grabbed there: the GRIP (a short bar in the middle of the top edge: drag it and the panel leaves the corner and follows the finger; let go near the corner or double tap it and
     * the panel docks again) and, while the keyboard is open, the four CORNERS (drag one to change the keyboard's width and height). The keys, the buttons and the bar are inside the outline's frame and are never taken by a drag.
     */
    private inner class PanelLayout(ctx: Context) : LinearLayout(ctx) {
        private val bandPx = ctx.dp(BAND_DP)
        private val padPx = ctx.dp(PANEL_PAD_DP)
        private val zonePx = ctx.dpf(CORNER_ZONE_DP.toFloat())
        private val gripHalfPx = ctx.dpf(GRIP_ZONE_HALF_DP.toFloat())
        private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
        private var mode = DRAG_NONE
        private var corner = 0                       // bit 0: right side, bit 1: bottom
        private var downRawX = 0f; private var downRawY = 0f; private var downAt = 0L; private var moved = false
        private var right0 = 0f; private var top0 = 0f; private var w0 = 0f; private var h0 = 0f
        private var lastTapAt = -1L
        private val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        private val markPath = Path()

        init { setWillNotDraw(false); isClickable = true }

        /** 0 = the band (the page's), 1 = the panel itself, 2 = the grip, 3..6 = a corner. */
        internal fun zoneAt(x: Float, y: Float): Int {
            val w = width.toFloat(); val h = height.toFloat(); val b = bandPx.toFloat(); val inner = b + padPx
            if (x >= inner && x <= w - inner && y >= inner && y <= h - inner) return ZONE_BODY
            if (keyboardShowing) for (c in 0..3) {
                val cx = if (c and 1 != 0) w - b else b
                val cy = if (c and 2 != 0) h - b else b
                if (Math.abs(x - cx) <= zonePx && Math.abs(y - cy) <= zonePx) return ZONE_CORNER0 + c
            }
            if (y <= inner && Math.abs(x - w / 2f) <= gripHalfPx) return ZONE_GRIP
            return if (x >= b && x <= w - b && y >= b && y <= h - b) ZONE_BODY else ZONE_NONE
        }

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                mode = DRAG_NONE
                when (val z = zoneAt(ev.x, ev.y)) {
                    ZONE_NONE -> return false   // the transparent band: the page keeps this touch
                    ZONE_BODY -> {}
                    ZONE_GRIP -> begin(ev, DRAG_MOVE, 0)
                    else -> begin(ev, DRAG_SIZE, z - ZONE_CORNER0)
                }
                if (mode != DRAG_NONE) return true
            } else if (mode != DRAG_NONE) { drag(ev); return true }
            return super.dispatchTouchEvent(ev)
        }

        private fun begin(ev: MotionEvent, m: Int, c: Int) {
            parent?.requestDisallowInterceptTouchEvent(true)
            cancelAnim(); panel.translationY = if (isOpen) 0f else panel.translationY
            mode = m; corner = c; moved = false
            downRawX = ev.rawX; downRawY = ev.rawY; downAt = ev.eventTime
            right0 = effRight(); top0 = effTop(); w0 = gW; h0 = gH
            gRight = right0; gTop = top0
            if (m == DRAG_SIZE) resizing = true
            invalidate()
        }

        private fun drag(ev: MotionEvent) {
            val dx = (ev.rawX - downRawX) / density; val dy = (ev.rawY - downRawY) / density
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (!moved && Math.hypot((ev.rawX - downRawX).toDouble(), (ev.rawY - downRawY).toDouble()) > slop) moved = true
                    if (!moved) return
                    if (mode == DRAG_MOVE) moveBy(dx, dy) else sizeBy(dx, dy)
                }
                MotionEvent.ACTION_UP -> { if (moved) { if (mode == DRAG_MOVE) moveBy(dx, dy) else sizeBy(dx, dy) }; finish(ev) } // the lift's own position counts too: events that arrive late (a busy screen) are not lost
                MotionEvent.ACTION_CANCEL -> finish(ev)
            }
        }

        private fun moveBy(dx: Float, dy: Float) {
            gRight = (right0 - dx).coerceIn(MIN_RIGHT_DP, Math.max(MIN_RIGHT_DP, availWdp - outlineW() - MIN_RIGHT_DP))
            gTop = (top0 + dy).coerceIn(BAND_DP.toFloat(), Math.max(BAND_DP.toFloat(), availHdp - outlineH() - 4f))
            applyGeometry()
        }

        private fun sizeBy(dx: Float, dy: Float) {
            val right = corner and 1 != 0; val bottom = corner and 2 != 0
            // width: a right corner moves the right edge (the left edge stays), a left corner moves the left edge (the right edge stays)
            var nw = if (right) w0 + dx else w0 - dx
            val roomW = if (right) w0 + right0 - MIN_RIGHT_DP else availWdp - right0 - 2f
            nw = nw.coerceIn(MIN_KEYS_W_DP, Math.max(MIN_KEYS_W_DP, Math.min(maxW(), roomW)))
            gRight = if (right) right0 - (nw - w0) else right0
            // height: a bottom corner moves the bottom edge (the top stays), a top corner moves the top edge (the bottom stays)
            var nh = if (bottom) h0 + dy else h0 - dy
            val chrome = outlineH() - gH
            val roomH = if (bottom) availHdp - top0 - chrome - 4f else h0 + (top0 - BAND_DP)
            nh = nh.coerceIn(MIN_KEYS_H_DP, Math.max(MIN_KEYS_H_DP, Math.min(maxH(), roomH)))
            gTop = if (bottom) top0 else top0 + (h0 - nh)
            gW = nw; gH = nh
            applyGeometry()
        }

        private fun finish(ev: MotionEvent) {
            val tap = !moved && ev.eventTime - downAt < ViewConfiguration.getTapTimeout() + 150
            val m = mode
            mode = DRAG_NONE
            if (m == DRAG_MOVE) {
                if (tap) { if (lastTapAt >= 0L && ev.eventTime - lastTapAt < DOUBLE_TAP_MS) { lastTapAt = -1L; dock() } else lastTapAt = ev.eventTime }
                else if (Math.abs(gRight - DOCK_RIGHT_DP) <= SNAP_DP && Math.abs(gTop - HotCorner.SIZE_DP) <= SNAP_DP) dock() // let go near the corner: it docks again
            }
            clampStored()
            if (m == DRAG_SIZE) { resizing = false; preparedFor = ""; post { prepareGlide() } }
            applyGeometry(); saveGeometry(); invalidate()
        }

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            val w = width.toFloat(); val h = height.toFloat(); val b = bandPx.toFloat()
            val grabbing = mode != DRAG_NONE
            // the grip: a short bar in the middle of the band above the outline (dim while docked, lit when the panel is detached or being dragged)
            mark.strokeWidth = context.dpf(3.5f)
            mark.color = if (grabbing && mode == DRAG_MOVE) pal.accentHi else if (isDetached) pal.accent else pal.deep
            val gy = b * 0.5f
            c.drawLine(w / 2f - context.dpf(GRIP_W_DP / 2f), gy, w / 2f + context.dpf(GRIP_W_DP / 2f), gy, mark)
            if (!keyboardShowing) return
            // corner marks: an L hugging the outside of each corner of the outline
            mark.strokeWidth = context.dpf(2.5f)
            val arm = context.dpf(12f); val off = context.dpf(3f)
            for (k in 0..3) {
                val right = k and 1 != 0; val bottom = k and 2 != 0
                val cx = if (right) w - b + off else b - off
                val cy = if (bottom) h - b + off else b - off
                val dx = if (right) -arm else arm; val dy = if (bottom) -arm else arm
                mark.color = if (grabbing && mode == DRAG_SIZE && corner == k) pal.accentHi else if (isResized) pal.accent else pal.deep
                markPath.reset(); markPath.moveTo(cx, cy + dy); markPath.lineTo(cx, cy); markPath.lineTo(cx + dx, cy)
                c.drawPath(markPath, mark)
            }
        }
    }

    /**
     * The keys, drawn and hit-tested by one view (no view per key). A key lights up in the accent for [FLASH_MS] when pressed; Backspace repeats while held. With glide typing on, a strip of guesses sits
     * above the keys, and a finger that slides off the letter it went down on draws a word: a neon trail follows it, and on lift the path goes to the decoder.
     */
    private inner class KeysView(ctx: Context) : View(ctx) {
        var onKey: ((Key) -> Unit)? = null
        private val handler = Handler(Looper.getMainLooper())
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT }
        private val box = RectF()
        private var rowsFor = -1
        private var rows: List<List<KeySpec>> = emptyList()
        private var rects: List<List<KeyBox>> = emptyList()
        private var rectsW = -1; private var rectsH = -1; private var rectsTop = -1f
        private var down: KeySpec? = null
        private var litKey: KeySpec? = null
        private val unlight = Runnable { litKey = null; invalidate() }
        private val repeat = object : Runnable { override fun run() { down?.key?.let { onKey?.invoke(it) }; handler.postDelayed(this, REPEAT_MS) } }

        // ---- glide
        private var armed = false                 // the finger went down on a letter: it may turn into a glide
        private var gliding = false
        private var glideDown: KeySpec? = null
        private var pressedKey: KeySpec? = null   // the letter the finger went down on (what a tap that drifts a little still types)
        private var downBox: KeyBox? = null
        private var gx = FloatArray(256); private var gy = FloatArray(256); private var gn = 0
        private var fadeStart = 0L
        private var cands: List<String> = emptyList()
        private var noWordUntil = 0L
        private var stripDown = -1
        private var bubbleKey: KeySpec? = null; private var bubbleText = ""   // the key under the finger, shown large above it (a thumb covers the key itself)
        private var holdAlt: String? = null                                   // the finger stayed on a key: what it types now is its alternate (a digit on the top row)
        private val hold = Runnable { holdFired() }
        private val trail = Path()
        private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        private val fadeStep = object : Runnable {
            override fun run() {
                if (SystemClock.uptimeMillis() - fadeStart < TRAIL_FADE_MS) { invalidate(); postOnAnimation(this) } else { gn = 0; fadeStart = 0L; invalidate() }
            }
        }
        private val noWordOff = Runnable { noWordUntil = 0L; invalidate() }

        init { contentDescription = "Keyboard"; isClickable = true }

        /** Everything in flight stops (the keyboard closed, or glide typing was switched). */
        fun reset() {
            handler.removeCallbacks(repeat); handler.removeCallbacks(unlight); handler.removeCallbacks(noWordOff); handler.removeCallbacks(hold); removeCallbacks(fadeStep)
            down = null; litKey = null; armed = false; gliding = false; gn = 0; fadeStart = 0L; cands = emptyList(); noWordUntil = 0L; stripDown = -1; bubbleKey = null; holdAlt = null
            invalidate()
        }

        private fun stripPx() = if (glideOn) context.dpf(STRIP_H_DP.toFloat()) else 0f

        private fun rowsNow(): List<List<KeySpec>> {
            val k = (if (model.symbols) 1 else 0) or (if (model.shift) 2 else 0) or (if (pageField != null) 4 else 0)
            if (k != rowsFor) { rowsFor = k; rows = MiniKeyboardLayout.rows(model.symbols, model.shift, pageField != null); rectsW = -1 }
            return rows
        }

        private fun layoutKeys() {
            val rs = rowsNow()
            val top = stripPx()
            if (rectsW == width && rectsH == height && rectsTop == top && rects.isNotEmpty()) return
            rectsW = width; rectsH = height; rectsTop = top
            rects = MiniKeyboardGeometry.boxes(rs, width.toFloat(), height.toFloat(), context.dpf(3f), top)
        }

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w, h, ow, oh); if (glideOn && glide != null && !resizing) post { prepareGlide() } } // the decoder needs the real key positions

        /** The 26 letter keys' centres as laid out now (the same function that draws them), for the decoder. */
        fun glideKeys(): GlideKeys? {
            if (width <= 0 || height <= 0) return null
            val letters = MiniKeyboardLayout.rows(false)
            return try { GlideKeys.from(letters, MiniKeyboardGeometry.boxes(letters, width.toFloat(), height.toFloat(), context.dpf(3f), stripPx())) } catch (e: IllegalArgumentException) { null }
        }

        private fun keyAt(x: Float, y: Float): KeySpec? {
            layoutKeys()
            for ((i, row) in rects.withIndex()) for ((j, r) in row.withIndex()) if (r.contains(x, y)) return rows[i][j].takeIf { it.key != null }
            // between two keys: the nearest one in that row, so a thumb that lands on a gap still types
            for ((i, row) in rects.withIndex()) if (y >= row[0].top - context.dpf(1.5f) && y <= row[0].bottom + context.dpf(1.5f)) {
                var best: KeySpec? = null; var bestD = Float.MAX_VALUE
                for ((j, r) in row.withIndex()) { val k = rows[i][j]; if (k.key == null) continue; val d = Math.abs(x - r.centerX); if (d < bestD) { bestD = d; best = k } }
                return best
            }
            return null
        }

        private fun boxOf(k: KeySpec?): KeyBox? {
            if (k == null) return null
            for ((i, row) in rows.withIndex()) for ((j, s) in row.withIndex()) if (s === k) return rects[i][j]
            return null
        }

        private fun isLetter(k: KeySpec?): Boolean { val c = (k?.key as? Key.Ch)?.text ?: return false; return c.length == 1 && c[0].lowercaseChar() in 'a'..'z' }

        private fun pitch(): Float = if (rects.isNotEmpty() && rects[0].size > 1) rects[0][1].centerX - rects[0][0].centerX else context.dpf(29f)

        private fun light(k: KeySpec?) { litKey = k; invalidate(); handler.removeCallbacks(unlight); if (k != null) handler.postDelayed(unlight, FLASH_MS) }

        private fun isBubbleKey(k: KeySpec?): Boolean { val c = (k?.key as? Key.Ch)?.text ?: return false; return c.length == 1 && c != " " }
        /** The digit a top-row letter types when it is held (null for every other key, and on the symbols page). */
        private fun altOf(k: KeySpec?): String? {
            if (model.symbols || k == null) return null
            val i = TOP_ROW.indexOf(k.label.lowercase())
            return if (k.label.length == 1 && i >= 0) DIGITS.substring(i, i + 1) else null
        }
        private fun haptic() { if (l.navKeyHaptics()) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
        private fun holdFired() {
            val k = pressedKey ?: down ?: return
            if (gliding) return
            val alt = altOf(k) ?: return
            holdAlt = alt; bubbleKey = k; bubbleText = alt; invalidate(); haptic()
        }
        private fun clearHold() { handler.removeCallbacks(hold); holdAlt = null; bubbleKey = null }
        fun bubbleTextForTest(): String? = if (bubbleKey != null) bubbleText else null

        private fun drawBubble(c: Canvas) {
            val k = bubbleKey ?: return
            val kb = boxOf(k) ?: return
            val gap = context.dpf(3f); val room = kb.top - gap
            if (room < context.dpf(24f)) return // the top row of a keyboard without the strip has no room above it
            val w = Math.max((kb.right - kb.left) * 1.45f, context.dpf(38f)); val h = Math.min(context.dpf(46f), room)
            var left = (kb.left + kb.right) / 2f - w / 2f; if (left < 0f) left = 0f; if (left + w > width) left = width - w
            box.set(left, kb.top - gap - h, left + w, kb.top - gap)
            val r = context.dpf(8f)
            fill.color = 0xFF0C161D.toInt(); c.drawRoundRect(box, r, r, fill)
            line.strokeWidth = context.dpf(1.4f); line.color = pal.accent; c.drawRoundRect(box, r, r, line)
            text.color = pal.accentHi; text.textSize = h * 0.55f
            val fm = text.fontMetrics
            c.drawText(bubbleText, box.centerX(), box.centerY() - (fm.ascent + fm.descent) / 2f, text)
        }

        // ------------------------------------------------------------------ the strip of guesses

        fun showCandidates(list: List<String>) { cands = list; noWordUntil = 0L; invalidate() }
        fun clearCandidates() { if (cands.isNotEmpty() || noWordUntil != 0L) { cands = emptyList(); noWordUntil = 0L; invalidate() } }
        fun showNoWord() { cands = emptyList(); noWordUntil = SystemClock.uptimeMillis() + NO_WORD_MS; handler.removeCallbacks(noWordOff); handler.postDelayed(noWordOff, NO_WORD_MS); invalidate() }
        fun candidateTexts(): List<String> = cands

        /** The boxes of the guesses in the strip (computed from the strip's size, so touches work before the first draw too). */
        private fun chipBoxes(): List<RectF> {
            val h = stripPx(); val n = cands.size
            if (h <= 0f || n == 0 || width <= 0) return emptyList()
            val gap = context.dpf(3f)
            val cw = (width - gap * (n - 1)) / n
            return List(n) { i -> RectF(i * (cw + gap), 0f, i * (cw + gap) + cw, h - gap) }
        }

        private fun chipAt(x: Float, y: Float): Int {
            if (y >= stripPx() || cands.isEmpty()) return -1
            val gap = context.dpf(3f)
            for ((i, r) in chipBoxes().withIndex()) if (x >= r.left - gap / 2 && x < r.right + gap / 2) return i
            return -1
        }

        private fun drawStrip(c: Canvas) {
            val h = stripPx(); if (h <= 0f) return
            val gap = context.dpf(3f)
            val now = SystemClock.uptimeMillis()
            text.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13f, resources.displayMetrics)
            val fm = text.fontMetrics
            val list = cands
            if (list.isEmpty()) {
                val miss = now < noWordUntil
                val msg = if (miss) "no word fits - try again" else if (model.symbols) "" else "slide over the letters to type a word"
                if (msg.isEmpty()) return
                text.color = if (miss) pal.accent else pal.muted
                text.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
                c.drawText(TextUtils.ellipsize(msg, text, width - context.dpf(8f), TextUtils.TruncateAt.END).toString(), width / 2f, (h - gap) / 2f - (fm.ascent + fm.descent) / 2f, text)
                return
            }
            val r = context.dpf(6f)
            val boxes = chipBoxes()
            for ((i, w) in list.withIndex()) {
                val rect = boxes[i]; val cw = rect.width()
                fill.color = if (i == stripDown) (pal.accent and 0x00FFFFFF) or 0xAA000000.toInt() else 0xFF0E171E.toInt()
                c.drawRoundRect(rect, r, r, fill)
                line.strokeWidth = context.dpf(if (i == 0) 1.2f else 1f); line.color = if (i == 0) pal.accent else pal.deep
                box.set(rect); box.inset(context.dpf(0.6f), context.dpf(0.6f))
                c.drawRoundRect(box, r, r, line)
                text.color = if (i == stripDown) pal.accentHi else if (i == 0) pal.accentHi else pal.fg
                c.drawText(TextUtils.ellipsize(w, text, cw - context.dpf(8f), TextUtils.TruncateAt.END).toString(), rect.centerX(), rect.centerY() - (fm.ascent + fm.descent) / 2f, text)
            }
        }

        // ------------------------------------------------------------------ the trail

        private fun addPoint(x: Float, y: Float) {
            if (gn == gx.size) { if (gn >= MAX_TRAIL_POINTS) return; gx = gx.copyOf(gn * 2); gy = gy.copyOf(gn * 2) }
            gx[gn] = x; gy[gn] = y; gn++
        }

        private fun addMotion(e: MotionEvent) {
            for (h in 0 until e.historySize) addPoint(e.getHistoricalX(h), e.getHistoricalY(h))
            addPoint(e.x, e.y)
        }

        private fun pathPitches(): Float {
            var len = 0f
            for (i in 1 until gn) len += Math.hypot((gx[i] - gx[i - 1]).toDouble(), (gy[i] - gy[i - 1]).toDouble()).toFloat()
            return len / pitch()
        }

        private fun drawTrail(c: Canvas) {
            if (gn < 2) return
            val alpha = if (gliding || fadeStart == 0L) 1f else (1f - (SystemClock.uptimeMillis() - fadeStart).toFloat() / TRAIL_FADE_MS).coerceIn(0f, 1f)
            if (alpha <= 0f) return
            trail.reset(); trail.moveTo(gx[0], gy[0])
            for (i in 1 until gn) trail.lineTo(gx[i], gy[i])
            trailPaint.strokeWidth = context.dpf(8f); trailPaint.color = (pal.accent and 0x00FFFFFF) or ((80 * alpha).toInt() shl 24); c.drawPath(trail, trailPaint)
            trailPaint.strokeWidth = context.dpf(2.5f); trailPaint.color = (pal.accentHi and 0x00FFFFFF) or ((240 * alpha).toInt() shl 24); c.drawPath(trail, trailPaint)
        }

        private fun leftTheKey(x: Float, y: Float): Boolean {
            val b = downBox ?: return false
            val slop = pitch() * GLIDE_SLOP
            return x < b.left - slop || x > b.right + slop || y < b.top - slop || y > b.bottom + slop
        }

        val isGliding: Boolean get() = gliding
        private var lastPreviewAt = 0L; private var lastPreviewN = 0

        /** While the finger is still gliding, now and then (not faster than the decoder, not for every touch point) ask for a live guess of the path so far. */
        private fun maybePreview() {
            val g = glide ?: return
            val now = SystemClock.uptimeMillis()
            if (now - lastPreviewAt < PREVIEW_MS || gn - lastPreviewN < PREVIEW_POINTS) return
            lastPreviewAt = now; lastPreviewN = gn
            val two = model.previousTwo()
            g.preview(gx, gy, gn, two.second, two.first) { res -> post { previewDone(res) } }
        }

        private fun beginGlide() {
            lastPreviewAt = SystemClock.uptimeMillis(); lastPreviewN = 0 // the first live guess comes a moment into the glide
            clearHold()
            gliding = true
            glideDown = pressedKey
            down = null
            handler.removeCallbacks(repeat); handler.removeCallbacks(unlight)
            litKey = null
            removeCallbacks(fadeStep); fadeStart = 0L
        }

        private fun endGlide() {
            gliding = false; armed = false
            if (pathPitches() < MIN_GLIDE_PITCHES) { // a tap that slid a little: the key it went down on
                val k = glideDown
                gn = 0; glideDown = null; invalidate()
                if (k?.key != null) { light(k); onKey?.invoke(k.key!!) }
                return
            }
            glideDown = null
            fadeStart = SystemClock.uptimeMillis(); postOnAnimation(fadeStep)
            val g = glide
            if (g == null) return
            prepareGlide() // the key positions as they are now (a no-op when nothing changed since the keyboard opened)
            confirmGlide() // the glide before this one stays: learn from it first (one thread, in order)
            val two = model.previousTwo(); lastGlidePrev = two.second; lastGlidePrev2 = two.first
            liftAt = SystemClock.uptimeMillis()
            g.decode(gx, gy, gn, lastGlidePrev, lastGlidePrev2) { res -> post { glideDone(res) } }
        }

        // ------------------------------------------------------------------ draw and touch

        override fun onDraw(c: Canvas) {
            layoutKeys()
            val r = context.dpf(6f)
            for ((i, row) in rows.withIndex()) for ((j, k) in row.withIndex()) {
                val key = k.key ?: continue
                val kb = rects[i][j]
                box.set(kb.left, kb.top, kb.right, kb.bottom)
                val isLit = k === litKey
                val special = key !is Key.Ch || k.label.length > 1
                fill.color = when {
                    isLit -> (pal.accent and 0x00FFFFFF) or 0xAA000000.toInt()
                    key === Key.Go -> pal.accent
                    key === Key.Shift && model.shift -> if (model.shiftLock) pal.accent else pal.deep
                    special -> 0xFF16232C.toInt()
                    else -> 0xFF0E171E.toInt()
                }
                c.drawRoundRect(box, r, r, fill)
                text.color = when { isLit -> pal.accentHi; key === Key.Go -> pal.accentOn; else -> pal.fg }
                val keyScale = Math.min((kb.right - kb.left) / context.dpf(29.5f), (kb.bottom - kb.top) / context.dpf(38f)).coerceIn(0.75f, 1.8f) // the label grows and shrinks with the key
                text.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, (if (k.label.length > 1) 12f else 16f) * keyScale, resources.displayMetrics)
                val fm = text.fontMetrics
                c.drawText(k.label, box.centerX(), box.centerY() - (fm.ascent + fm.descent) / 2f, text)
                if (i == 0 && !model.symbols && k.label.length == 1) { // the digit a held top-row letter types, small in its corner
                    val d = TOP_ROW.indexOf(k.label.lowercase())
                    if (d >= 0) {
                        text.color = pal.muted; text.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8.5f * keyScale, resources.displayMetrics)
                        val hf = text.fontMetrics
                        c.drawText(DIGITS.substring(d, d + 1), box.right - context.dpf(5f) * keyScale, box.top + context.dpf(3f) - hf.ascent, text)
                    }
                }
            }
            drawStrip(c)
            drawBubble(c)
            drawTrail(c)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    layoutKeys()
                    armed = false; gliding = false; stripDown = -1
                    if (glideUsable() && e.y < stripPx()) { stripDown = chipAt(e.x, e.y); invalidate(); return true } // a guess in the strip was touched
                    removeCallbacks(fadeStep); gn = 0; fadeStart = 0L
                    down = keyAt(e.x, e.y); light(down)
                    clearHold(); if (isBubbleKey(down)) { bubbleKey = down; bubbleText = down!!.label }
                    if (down != null) { haptic(); if (altOf(down) != null) handler.postDelayed(hold, HOLD_MS) }
                    if (down?.key === Key.Backspace) { onKey?.invoke(Key.Backspace); handler.postDelayed(repeat, REPEAT_FIRST_MS) }
                    if (glideUsable() && !model.symbols && isLetter(down)) { armed = true; pressedKey = down; downBox = boxOf(down); addPoint(e.x, e.y) }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (stripDown >= 0) return true
                    if (armed) { // a letter: nothing happens until the finger leaves its key (a glide) or lifts (a tap)
                        addMotion(e)
                        if (!gliding && leftTheKey(e.x, e.y)) beginGlide()
                        if (gliding) { invalidate(); maybePreview() }
                        return true
                    }
                    val k = keyAt(e.x, e.y); if (k !== down) { down = k; handler.removeCallbacks(repeat); clearHold(); if (isBubbleKey(k)) { bubbleKey = k; bubbleText = k!!.label }; light(k) }
                }
                MotionEvent.ACTION_UP -> {
                    if (stripDown >= 0) {
                        val i = chipAt(e.x, e.y); val was = stripDown; stripDown = -1; invalidate()
                        if (i == was && i in cands.indices) pickCandidate(cands[i])
                        return true
                    }
                    if (gliding) { addMotion(e); endGlide(); return true }
                    handler.removeCallbacks(repeat)
                    val alt = holdAlt; clearHold(); invalidate()
                    if (armed) { // a tap on a letter types the letter it went down on, even when the finger drifted a little (held: its digit)
                        armed = false
                        val pk = pressedKey; pressedKey = null
                        if (alt != null) { light(pk); onKey?.invoke(Key.Ch(alt)) }
                        else if (pk?.key != null) { light(pk); onKey?.invoke(pk.key!!) }
                        down = null; gn = 0
                        return true
                    }
                    val k = keyAt(e.x, e.y)
                    if (k != null && k === down && k.key !== Key.Backspace) { light(k); onKey?.invoke(if (alt != null) Key.Ch(alt) else k.key!!) }
                    down = null; gn = 0
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(repeat); clearHold(); down = null; armed = false; pressedKey = null; stripDown = -1; invalidate()
                    if (gliding) { gliding = false; fadeStart = SystemClock.uptimeMillis(); postOnAnimation(fadeStep) }
                }
            }
            return true
        }

        /** Tests: press the key labelled [label] as a finger would. */
        fun pressLabel(label: String): Boolean {
            val k = rowsNow().flatten().firstOrNull { it.label == label && it.key != null } ?: return false
            light(k); onKey?.invoke(k.key!!)
            return true
        }

        /** Tests: the centre of the key labelled [label] in this view's pixels. */
        fun centerOf(label: String): Pair<Float, Float>? {
            layoutKeys()
            for ((i, row) in rows.withIndex()) for ((j, k) in row.withIndex()) if (k.label == label && k.key != null) return rects[i][j].centerX to rects[i][j].centerY
            return null
        }

        /** Tests: the centre of the i-th guess in the strip. */
        fun chipCenter(i: Int): Pair<Float, Float>? { val r = chipBoxes().getOrNull(i) ?: return null; return r.centerX() to r.centerY() }

        fun touch(action: Int, x: Float, y: Float) { val t = SystemClock.uptimeMillis(); val e = MotionEvent.obtain(t, t, action, x, y, 0); dispatchTouchEvent(e); e.recycle() }

        override fun performClick(): Boolean = super.performClick()
    }

    companion object {
        const val SLIDE_MS = 200L
        /** How long a pressed button, key or bar stays lit. */
        const val FLASH_MS = 130L
        private const val REPEAT_FIRST_MS = 380L
        private const val REPEAT_MS = 70L
        const val PANEL_W_DP = 176
        const val PANEL_KEYS_W_DP = 304
        /** The frame inside the outline, and the transparent band around the outline that carries the grip and the corner marks. */
        const val PANEL_PAD_DP = 6
        const val BAND_DP = 16
        /** Where the panel starts: this far from the right edge, under the invisible corner. */
        const val DOCK_RIGHT_DP = 6
        const val MIN_RIGHT_DP = 2f
        const val MIN_KEYS_W_DP = 232f
        const val MAX_KEYS_W_DP = 620f
        const val MIN_KEYS_H_DP = 100f
        const val MAX_KEYS_H_DP = 320f
        /** The corner you can grab (the square around the outline's corner, not the keys inside the frame). */
        const val CORNER_ZONE_DP = 26
        const val GRIP_W_DP = 44
        const val GRIP_ZONE_HALF_DP = 56
        /** Let go this close to the dock and the panel docks again. */
        const val SNAP_DP = 40
        const val DOUBLE_TAP_MS = 350L
        private const val DRAG_NONE = 0
        private const val DRAG_MOVE = 1
        private const val DRAG_SIZE = 2
        private const val ZONE_NONE = 0
        private const val ZONE_BODY = 1
        private const val ZONE_GRIP = 2
        private const val ZONE_CORNER0 = 3
        internal const val ZONE_GRIP_FOR_TEST = 2
        internal const val ZONE_CORNER_FOR_TEST = 3
        const val NAV_W_DP = 52
        const val NAV_H_DP = 42
        const val BAR_H_DP = 34
        const val KEYS_H_DP = 161
        /** The strip of guesses above the keys (glide typing on). */
        const val STRIP_H_DP = 30
        /** A finger that leaves the key it went down on by this many pitches has started a glide. */
        const val GLIDE_SLOP = 0.18f
        /** A path shorter than this (in key pitches) is a tap that slid a little, not a glide. */
        const val MIN_GLIDE_PITCHES = 1.2f
        const val TRAIL_FADE_MS = 260L
        const val NO_WORD_MS = 1500L
        /** The live guess while gliding: at most this often, and only after this many new touch points. */
        const val PREVIEW_MS = 110L
        const val PREVIEW_POINTS = 4
        /** Holding a top-row letter this long types its digit. */
        const val HOLD_MS = 420L
        /** A glide within this long after a deleted glide that gives the deleted word first again is a retry (the deleted word moves to second place), when the next candidate fits the path about as well (cost per letter, pitch squared). */
        const val RETRY_MS = 6000L
        const val RETRY_GAP = 1.5f
        private const val TOP_ROW = "qwertyuiop"
        private const val DIGITS = "1234567890"
        private const val MAX_TRAIL_POINTS = 4096
    }
}
