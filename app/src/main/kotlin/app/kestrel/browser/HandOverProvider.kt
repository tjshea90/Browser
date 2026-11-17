package app.kestrel.browser

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Hands ONE file to ONE other app: what the mini keyboard learned (owner request 17, docs/KEYBOARD-LINK.md). It is not exported: only an app that gets a read grant with the intent (the Swoop keyboard's
 * "Add to Swoop" screen) can open a file, read only, and only a file directly inside cache/handover (no path can lead anywhere else). Nothing is left open between the apps.
 */
class HandOverProvider : ContentProvider() {
    override fun onCreate(): Boolean = true
    override fun getType(uri: Uri): String = "text/plain"

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val f = fileFor(uri) ?: return null
        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(cols).also { c -> c.addRow(Array<Any?>(cols.size) { i -> when (cols[i]) { OpenableColumns.DISPLAY_NAME -> f.name; OpenableColumns.SIZE -> f.length(); else -> null } }) }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("read only")
        val f = fileFor(uri) ?: throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = throw UnsupportedOperationException()

    private fun fileFor(uri: Uri): File? {
        val name = uri.pathSegments.singleOrNull() ?: return null
        if (name.isEmpty() || name.contains('/') || name.contains("..") || name.startsWith(".")) return null
        val f = File(dir(context ?: return null), name)
        return if (f.isFile) f else null
    }

    companion object {
        fun dir(ctx: android.content.Context): File = File(ctx.cacheDir, "handover").also { it.mkdirs() }
        fun authority(ctx: android.content.Context): String = ctx.packageName + ".handover"
        fun uriFor(ctx: android.content.Context, name: String): Uri = Uri.parse("content://" + authority(ctx) + "/" + Uri.encode(name))
    }
}
