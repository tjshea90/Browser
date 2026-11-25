package app.kestrel.browser.optimizer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Process

/**
 * The trampoline of a restart (its own process, `:restart`): the old main process asks the system to start this activity and ends itself; this one makes sure the old process is gone, starts the real target
 * (Auto-tune's screen) as a fresh task and ends. The system lets an app start an activity from a visible window, and this activity has one, so no background-start rule gets in the way.
 */
class RestartActivity : Activity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val oldPid = intent.getIntExtra("pid", -1)
        val target = intent.getParcelableExtra<Intent>("target")
        val t0 = android.os.SystemClock.elapsedRealtime()
        if (oldPid > 0 && oldPid != Process.myPid()) try { Process.killProcess(oldPid) } catch (e: Exception) { /* already gone */ }
        // lab v9: after a wedged engine the old process needed 12 s to really die (a kill is only a request); starting the new one while the system still counted the old one alive lost the restart for good. So: wait until it is gone.
        var waited = 0
        while (oldPid > 0 && oldPid != Process.myPid() && alive(oldPid) && waited < 30_000) { try { Thread.sleep(100) } catch (e: InterruptedException) { break }; waited += 100 }
        android.util.Log.i("KestrelRestart", "old process $oldPid ${if (alive(oldPid)) "STILL ALIVE" else "gone"} after ${android.os.SystemClock.elapsedRealtime() - t0} ms; starting the target")
        try { Thread.sleep(300) } catch (e: InterruptedException) { /* go on */ }
        if (target != null) { target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK); try { startActivity(target) } catch (e: Exception) { android.util.Log.w("KestrelRestart", "start refused: $e") /* the notification fallback is still there */ } }
        finish()
        Process.killProcess(Process.myPid())
    }

    /** A process that was killed is still listed (as a zombie) until the system has reaped it: only a missing entry or state Z / X counts as gone. */
    private fun alive(pid: Int): Boolean = try {
        val f = java.io.File("/proc/$pid/stat")
        if (!f.exists()) false else { val st = f.readText(); val state = st.substring(st.lastIndexOf(')') + 2).firstOrNull(); state != 'Z' && state != 'X' }
    } catch (e: Exception) { false }
}
