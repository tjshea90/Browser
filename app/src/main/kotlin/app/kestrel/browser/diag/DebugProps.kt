package app.kestrel.browser.diag

import android.content.Context
import android.content.pm.ApplicationInfo

/** DEBUG builds only: a system property set from a computer with `adb shell setprop name value` (the emulator test rig). A release build always gets null, so nothing here can be switched from outside. */
object DebugProps {
    fun get(ctx: Context, name: String): String? {
        if ((ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0) return null
        return try { (Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, name) as? String)?.takeIf { it.isNotBlank() } } catch (e: Throwable) { null }
    }
}
