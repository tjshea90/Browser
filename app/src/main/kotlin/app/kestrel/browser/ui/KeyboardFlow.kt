package app.kestrel.browser.ui

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import app.kestrel.browser.Io
import app.kestrel.core.input.glide.LearnedExport
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings > "Export what the keyboard learned" / "Import what the keyboard learned" (AX1, owner message 38: "save this learned data in an exportable format because if the keyboard is good I will make
 * it my system keyboard for all apps and I want to import what it learned"). The same way as Back up & restore: ONE tap writes a plain-text file into Downloads (shared storage, no picker, no
 * permission), and a file is brought back with the system file picker. The file format is core/input/glide/LearnedExport (docs/KEYBOARD-EXPORT.md).
 */
class KeyboardFlow(private val a: Activity, private val host: Host) {
    interface Host {
        fun isPrivateWindow(): Boolean
        /** The export text (answers on any thread; called off the UI thread). */
        fun exportText(done: (String) -> Unit)
        /** Adds an export's contents to what the keyboard learned (called off the UI thread; [done] may run on any thread). */
        fun importText(text: String, done: (LearnedExport.Summary?) -> Unit)
        fun say(message: String)
        fun ask(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit)
    }

    /** Export now: one file in Downloads. */
    fun exportNow() {
        if (host.isPrivateWindow()) { host.say("Open a normal window to export: a private window keeps nothing."); return }
        Io.exec {
            host.exportText { text ->
                val message = try {
                    val name = save(text)
                    val s = LearnedExport.read(text)?.summary
                    "Saved \"$name\" in Downloads" + (if (s != null) ": " + s.describe() else "") + ". It lists words you typed: keep it private."
                } catch (e: Exception) { "The file could not be saved: " + (e.message ?: e.javaClass.simpleName) }
                Io.ui { host.say(message) }
            }
        }
    }

    /** Writes [text] as a new file in the Downloads folder and returns the name Android gave it. */
    private fun save(text: String): String {
        val cr = a.contentResolver
        val cv = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName())
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
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
        return try { cr.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } } catch (e: Exception) { null } ?: fileName()
    }

    /** Import from a file the person picks. */
    fun importFromFile() {
        if (host.isPrivateWindow()) { host.say("Open a normal window to import."); return }
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "application/octet-stream"))
        }
        try { a.startActivityForResult(i, REQ_IMPORT) } catch (e: Exception) { host.say("This tablet has no file picker (the Files app), so a file cannot be chosen.") }
    }

    /** The activity forwards its onActivityResult here; true when the result was ours. */
    fun onResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQ_IMPORT) return false
        val uri = data?.data
        if (resultCode != Activity.RESULT_OK || uri == null) return true
        val cr = a.contentResolver
        Io.exec {
            val read: Any = try {
                val bytes = (cr.openInputStream(uri) ?: throw IOException("no stream")).use { readBounded(it, LearnedExport.MAX_CHARS) }
                val text = String(bytes, Charsets.UTF_8)
                if (LearnedExport.read(text) == null) "This is not a file the Kestrel keyboard exported." else text
            } catch (e: TooBig) { "This file is too big to be one the Kestrel keyboard exported." } catch (e: Exception) { "The file could not be read." }
            Io.ui { if (read is String && read.startsWith(LearnedExport.MAGIC)) confirm(read) else host.say(read.toString()) }
        }
        return true
    }

    private fun confirm(text: String) {
        val s = LearnedExport.read(text)?.summary ?: run { host.say("This is not a file the Kestrel keyboard exported."); return }
        host.ask(
            "Import what the keyboard learned?",
            "The file holds " + s.describe() + ".\n\nIt is added to what this keyboard has learned. What is learned here stays; importing the same file again changes nothing.",
            "Import", "Cancel",
        ) { ok -> if (ok) Io.exec { host.importText(text) { sum -> Io.ui { host.say(if (sum == null) "The file could not be imported." else "Imported: " + sum.describe() + ".") } } } }
    }

    private class TooBig : IOException()

    private fun readBounded(s: InputStream, max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream(64 * 1024)
        val buf = ByteArray(32 * 1024)
        while (true) {
            val n = s.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            if (out.size() > max) throw TooBig()
        }
        return out.toByteArray()
    }

    companion object {
        const val REQ_IMPORT = 7352
        fun fileName(now: Long = System.currentTimeMillis()): String = "Kestrel keyboard learning " + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now)) + ".txt"
    }
}
