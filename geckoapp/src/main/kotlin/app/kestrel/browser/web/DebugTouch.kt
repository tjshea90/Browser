package app.kestrel.browser.web

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent

/**
 * DEBUG builds only (the emulator lab, tools/emu/soak.py; both screens ignore the command in a release build): plays ONE finger's path into the screen that is in front, as real MotionEvents made here with their own
 * times and handed to the Activity the way the system hands it a touch (Activity.dispatchTouchEvent: the same views, the same listeners).
 * WHY: the lab's adb shell is not root (a path written to /dev/input fails with 'Permission denied': lab runs 43918159286, 43921764750 and 43925029376 all lost their long presses this way) and Android 10's `input`
 * command can tap and swipe but cannot rest a finger first, which is what a long press + drag (the precise seek) needs.
 * Sent with `am broadcast -a app.kestrel.browser.DEBUG_TOUCH -p <package> --es script "<words>"` (NOT `am start`: starting the screen again pauses it, and a film inside the page stops with "the app was left": lab run 43927135769).
 * Only the screen that has the window focus plays it (the browser sits behind Kestrel's own player while that is open).
 * The script is words separated by spaces: `Dx,y` finger down, `Mx,y` move, `Ux,y` finger up (SCREEN pixels: the window's own offset is taken off), `Wms` wait before the next word.
 * Example: `D150,500 W500 M195,500 M240,500 W2000 U418,500` = rest 0.5 s, drag right in two steps, rest 2 s, lift.
 */
object DebugTouch {
    const val ACTION = "app.kestrel.browser.DEBUG_TOUCH"

    /** DEBUG builds only: the screen listens for the broadcast. Returns the receiver to give to [detach] (null = not a debug build). */
    fun attach(a: Activity): BroadcastReceiver? {
        if ((a.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0) return null
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { if (a.hasWindowFocus()) play(a, i.getStringExtra("script") ?: "") }
        }
        return try {
            if (Build.VERSION.SDK_INT >= 33) a.registerReceiver(r, IntentFilter(ACTION), Context.RECEIVER_EXPORTED) else a.registerReceiver(r, IntentFilter(ACTION))
            r
        } catch (t: Throwable) { null }
    }

    fun detach(a: Activity, r: BroadcastReceiver?) { if (r != null) try { a.unregisterReceiver(r) } catch (t: Throwable) { /* already gone */ } }

    fun play(a: Activity, script: String) {
        val loc = IntArray(2)
        a.window.decorView.getLocationOnScreen(loc)
        val main = Handler(Looper.getMainLooper())
        var at = 0L
        var downTime = 0L
        for (word in script.trim().split(Regex("\\s+"))) {
            if (word.length < 2) continue
            val kind = word[0]
            val nums = word.substring(1).split(",")
            if (kind == 'W') { at += nums[0].toLongOrNull() ?: 0L; continue }
            val action = when (kind) {
                'D' -> MotionEvent.ACTION_DOWN
                'M' -> MotionEvent.ACTION_MOVE
                'U' -> MotionEvent.ACTION_UP
                else -> continue
            }
            val x = (nums[0].toFloatOrNull() ?: continue) - loc[0]
            val y = (nums.getOrNull(1)?.toFloatOrNull() ?: continue) - loc[1]
            main.postDelayed({
                val now = SystemClock.uptimeMillis()
                if (action == MotionEvent.ACTION_DOWN) downTime = now
                val e = MotionEvent.obtain(downTime, now, action, x, y, if (action == MotionEvent.ACTION_UP) 0f else 1f, 1f, 0, 1f, 1f, 0, 0)
                e.source = InputDevice.SOURCE_TOUCHSCREEN
                try { a.dispatchTouchEvent(e) } catch (t: Throwable) { /* the screen went away: the path ends */ } finally { e.recycle() }
            }, at)
        }
    }
}
