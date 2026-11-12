package app.kestrel.browser.ui

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import app.kestrel.browser.Io
import app.kestrel.browser.data.BackupManager
import app.kestrel.browser.data.BrowserDb
import app.kestrel.browser.data.Prefs
import app.kestrel.core.backup.BackupCodec
import app.kestrel.core.backup.BackupData
import app.kestrel.core.backup.BackupException
import app.kestrel.core.backup.TabRef
import java.io.IOException
import java.io.InputStream

/**
 * Settings > Back up & restore (owner U1 / AA3-7: "so an uninstall-first loses nothing").
 *  - Back up now: one tap writes "Kestrel backup <date>.json" into Downloads (shared storage: it survives uninstalling the app) with no picker and no permission.
 *  - Restore: the system file picker (it needs no permission either), the file is read with the checks of [BackupCodec], the person is asked with what the file holds, then it is brought back.
 * The activities supply what only they know: the tabs, opening tabs, dialogs ([Host]); the rest is here so both browsers behave the same.
 */
class BackupFlow(private val a: Activity, private val host: Host) {
    interface Host {
        fun database(): BrowserDb?
        fun isPrivateWindow(): Boolean
        /** The regular tabs, in order, for the file (UI thread). */
        fun tabsForBackup(): List<TabRef>
        /** Opens [refs] as tabs that load when they are opened, skipping pages that are already open (UI thread); returns how many were added. */
        fun openTabs(refs: List<TabRef>): Int
        fun say(message: String)
        fun ask(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit)
        /** A restore changed settings, bookmarks or tabs (UI thread): refresh whatever shows them. */
        fun afterRestore()
        fun appName(): String
    }

    /** Back up now. */
    fun backupNow() {
        if (host.isPrivateWindow()) { host.say("Open a normal window to back up: a private window keeps nothing."); return }
        val db = host.database() ?: run { host.say("Kestrel is still starting. Try again in a moment."); return }
        val tabs = host.tabsForBackup()
        val prefs = Prefs.get(a); val app = host.appName()
        Io.exec {
            val message = try {
                val d = BackupManager.build(db, prefs, tabs, app)
                val name = save(BackupCodec.encode(d))
                "Saved \"$name\" in Downloads: " + BackupManager.describe(d) + ". Keep it somewhere safe before you uninstall Kestrel."
            } catch (e: Exception) { "The backup could not be saved: " + (e.message ?: e.javaClass.simpleName) }
            Io.ui { host.say(message) }
        }
    }

    /** Writes [text] as a new file in the Downloads folder and returns the name Android gave it (a second backup on the same day gets "(1)"). */
    private fun save(text: String): String {
        val cr = a.contentResolver
        val cv = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, BackupManager.fileName())
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: throw IOException("Android would not create the file")
        try {
            (cr.openOutputStream(uri, "w") ?: throw IOException("Android would not open the file")).use { it.write(text.toByteArray(Charsets.UTF_8)) }
            cr.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            try { cr.delete(uri, null, null) } catch (x: Exception) { /* nothing left to clean */ }
            throw e
        }
        return try { cr.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } } catch (e: Exception) { null } ?: BackupManager.fileName()
    }

    /** Restore from a file the person picks. */
    fun restoreFromFile() {
        if (host.isPrivateWindow()) { host.say("Open a normal window to restore."); return }
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain", "application/octet-stream"))
        }
        try { a.startActivityForResult(i, REQ_RESTORE) } catch (e: Exception) { host.say("This tablet has no file picker (the Files app), so a backup cannot be chosen.") }
    }

    /** The activity forwards its onActivityResult here; true when the result was ours. */
    fun onResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQ_RESTORE) return false
        val uri = data?.data
        if (resultCode != Activity.RESULT_OK || uri == null) return true
        val cr = a.contentResolver
        Io.exec {
            val read: Any = try {
                val bytes = (cr.openInputStream(uri) ?: throw IOException("no stream")).use { readBounded(it, BackupCodec.MAX_BYTES) }
                BackupCodec.decode(String(bytes, Charsets.UTF_8))
            } catch (e: BackupException) { e.message ?: "This is not a Kestrel backup file." } catch (e: Exception) { "The file could not be read." }
            Io.ui {
                if (read is BackupData) confirm(read) else host.say(read.toString())
            }
        }
        return true
    }

    private fun confirm(d: BackupData) {
        val left = if (d.skipped > 0) " (${d.skipped} unusable entries are left out)" else ""
        host.ask(
            "Restore this backup?",
            "It holds " + BackupManager.describe(d) + left + ".\n\nBookmarks, history and open tabs are added to yours. Your settings and per-site Shields choices are replaced by the backup's.",
            "Restore", "Cancel",
        ) { ok -> if (ok) apply(d) }
    }

    private fun apply(d: BackupData) {
        val db = host.database() ?: run { host.say("Kestrel is still starting. Try again in a moment."); return }
        val prefs = Prefs.get(a)
        Io.exec {
            val c = try { BackupManager.restore(db, prefs, d) } catch (e: Exception) { null }
            Io.ui {
                if (c == null) { host.say("The backup could not be restored."); return@ui }
                val opened = host.openTabs(d.tabs)
                host.afterRestore()
                host.say("Restored: ${c.bookmarks} bookmarks and ${c.history} history entries added, ${c.settings} settings and ${c.sites} site choices applied, $opened tabs opened.")
            }
        }
    }

    private fun readBounded(s: InputStream, max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream(64 * 1024)
        val buf = ByteArray(32 * 1024)
        while (true) {
            val n = s.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            if (out.size() > max) throw BackupException("This file is too big to be a Kestrel backup.")
        }
        return out.toByteArray()
    }

    companion object {
        const val REQ_RESTORE = 7351
    }
}
