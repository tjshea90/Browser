package app.kestrel.browser.update

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import app.kestrel.browser.ProcessLife
import app.kestrel.browser.data.Prefs

/**
 * The daily look for a new release when the app is not open (owner request 8: updates in the background). One conditional request (an ETag makes "nothing new" a 304); when a release is found
 * and "download updates in the background" is on, the download is handed to the download service and this job ends at once. If the process was started only for this, it leaves again
 * ([ProcessLife.maybeExit]) unless the download needs it.
 */
class ReleaseCheckJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        val app = applicationContext
        if (!Prefs.get(app).autoUpdateCheck) { jobFinished(params, false); return false }
        UpdateManager.init(app)
        UpdateManager.check(app, manual = false)
        // check() runs on its own thread and may start the download; give it a moment, then let the process go if nothing needs it
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ jobFinished(params, false); ProcessLife.maybeExit(3000) }, 15_000)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = false

    companion object {
        private const val JOB_ID = 4712
        private const val DAY = 24L * 60 * 60 * 1000

        /** Idempotent; call at app start (cheap). */
        fun schedule(ctx: Context) {
            val js = ctx.getSystemService(JobScheduler::class.java) ?: return
            if (!Prefs.get(ctx).autoUpdateCheck) { js.cancel(JOB_ID); return }
            if (js.getPendingJob(JOB_ID) != null) return
            js.schedule(JobInfo.Builder(JOB_ID, ComponentName(ctx, ReleaseCheckJob::class.java)).setPeriodic(DAY).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setRequiresBatteryNotLow(true).build())
        }
    }
}
