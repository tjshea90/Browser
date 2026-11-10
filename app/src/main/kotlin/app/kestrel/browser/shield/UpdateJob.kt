package app.kestrel.browser.shield

import android.app.job.JobParameters
import android.app.job.JobService

/** Periodic filter-list refresh. The real work (conditional downloads, compile, atomic swap) is added in the updater phase. */
class UpdateJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean = false
}
