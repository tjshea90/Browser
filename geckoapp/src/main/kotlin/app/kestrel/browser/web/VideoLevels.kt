package app.kestrel.browser.web

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager

/**
 * ANS57 (owner message 57: "if I drag up or down on the left side of the video, it increases or decreases video brightness, and if I drag up or down on the right side of the video, it increases or decreases
 * volume. Model this off how vlc media player handles this"): the two levels a vertical drag changes, the way VLC for Android does it. Volume = the system MUSIC stream (never the page's own element: the
 * film plays in Kestrel's player or the page's, both are music to Android). Brightness = the BRIGHTNESS OF THE WINDOW that shows the film (VLC: `lp.screenBrightness`); it starts from the window's own value, else
 * half when the tablet's automatic brightness is on (VLC's choice: the real value is not known), else the system value, and [restore] gives the window back to the system when the film ends: nothing is changed for good.
 */
class VideoLevels(private val activity: Activity) {
    private val audio = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var changed = false

    private fun max(): Int = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

    /** 0..1 */
    fun volume(): Float = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max()

    fun setVolume(f: Float) {
        val idx = Math.round(f.coerceIn(0f, 1f) * max())
        if (idx == audio.getStreamVolume(AudioManager.STREAM_MUSIC)) return
        try { audio.setStreamVolume(AudioManager.STREAM_MUSIC, idx, 0) } catch (e: SecurityException) { /* a policy that forbids it: the bar still moves, the sound does not */ }
    }

    /** 0..1 */
    fun brightness(): Float {
        val lp = activity.window.attributes
        if (lp.screenBrightness >= 0f) return lp.screenBrightness
        return try {
            val auto = Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            if (auto) 0.5f else (Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f).coerceIn(0.01f, 1f)
        } catch (e: Exception) { 0.5f }
    }

    fun setBrightness(f: Float) {
        val lp = activity.window.attributes
        lp.screenBrightness = f.coerceIn(0.01f, 1f)
        activity.window.attributes = lp
        changed = true
    }

    /** The film ended: the window is the tablet's again (its own brightness setting rules). */
    fun restore() {
        if (!changed) return
        changed = false
        try {
            val lp = activity.window.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            activity.window.attributes = lp
        } catch (e: Throwable) { /* the window is gone */ }
    }
}
