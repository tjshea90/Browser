package app.kestrel.browser.download

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import app.kestrel.core.download.Reason
import app.kestrel.core.download.Sink
import app.kestrel.core.download.SinkException
import app.kestrel.core.download.SinkFactory
import java.io.IOException

/**
 * Files land in the public Download folder through a pending MediaStore row (IS_PENDING=1 until the last byte is fsynced), written with positional
 * `pwrite` on ONE shared descriptor so several connections can fill different ranges at once (research/DOWNLOADS.md 7). The provider renames a clashing name.
 */
class MediaStoreSinks(private val ctx: Context) : SinkFactory {
    private val resolver: ContentResolver = ctx.contentResolver

    override fun create(name: String, mime: String?, total: Long, priv: Boolean): Sink {
        val v = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = try { resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) } catch (e: Exception) { throw SinkException(Reason.STORAGE, "could not create the file", e) }
            ?: throw SinkException(Reason.STORAGE, "could not create the file")
        val real = try { resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } } catch (e: Exception) { null } ?: name
        val pfd = try { resolver.openFileDescriptor(uri, "rw") } catch (e: Exception) { null }
        if (pfd == null) { try { resolver.delete(uri, null, null) } catch (e: Exception) { /* ignore */ }; throw SinkException(Reason.STORAGE, "could not open the file") }
        try { preallocate(pfd, total) } catch (e: SinkException) { try { pfd.close() } catch (x: Exception) { /* ignore */ }; try { resolver.delete(uri, null, null) } catch (x: Exception) { /* ignore */ }; throw e }
        return MediaSink(resolver, uri, pfd, real)
    }

    /** Reserve the space up front (so "no space" shows at the start, not at 90 %). Falls back step by step; a sparse file is fine. */
    private fun preallocate(pfd: ParcelFileDescriptor, total: Long) {
        if (total <= 0) return
        val fd = pfd.fileDescriptor
        if (Build.VERSION.SDK_INT >= 26) {
            try { ctx.getSystemService(StorageManager::class.java)?.allocateBytes(fd, total); return }
            catch (e: IOException) { throw SinkException(Reason.NO_SPACE, "not enough free space", e) }
            catch (e: Exception) { /* not supported for this file: try the next way */ }
        }
        try { Os.posix_fallocate(fd, 0, total) }
        catch (e: ErrnoException) {
            when (e.errno) {
                OsConstants.ENOSPC -> throw SinkException(Reason.NO_SPACE, "not enough free space", e)
                OsConstants.EFBIG -> throw SinkException(Reason.FS_LIMIT, "file too big for this storage", e)
                else -> try { Os.ftruncate(fd, total) } catch (e2: ErrnoException) { if (e2.errno == OsConstants.EFBIG) throw SinkException(Reason.FS_LIMIT, "file too big", e2) }
            }
        }
    }

    override fun reopen(uri: String, total: Long): Sink? {
        val u = try { Uri.parse(uri) } catch (e: Exception) { return null }
        val pfd = try { resolver.openFileDescriptor(u, "rw") } catch (e: Exception) { null } ?: return null // the user deleted it, or the pending row expired
        val name = try { resolver.query(u, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } } catch (e: Exception) { null }
        return MediaSink(resolver, u, pfd, name)
    }

    override fun delete(uri: String): Boolean = try { resolver.delete(Uri.parse(uri), null, null) > 0 } catch (e: Exception) { false }

    private class MediaSink(val resolver: ContentResolver, val u: Uri, val pfd: ParcelFileDescriptor, val shownName: String?) : Sink {
        override val uri: String? = u.toString()
        override val displayName: String? = shownName

        override fun pwrite(pos: Long, b: ByteArray, off: Int, len: Int) {
            var done = 0
            try {
                while (done < len) {
                    val n = Os.pwrite(pfd.fileDescriptor, b, off + done, len - done, pos + done)
                    if (n <= 0) throw SinkException(Reason.STORAGE, "write returned $n")
                    done += n
                }
            } catch (e: ErrnoException) {
                throw when (e.errno) {
                    OsConstants.ENOSPC -> SinkException(Reason.NO_SPACE, "not enough free space", e)
                    OsConstants.EFBIG -> SinkException(Reason.FS_LIMIT, "file too big for this storage", e)
                    else -> SinkException(Reason.STORAGE, "write failed (${e.errno})", e)
                }
            }
        }

        override fun sync() { try { Os.fsync(pfd.fileDescriptor) } catch (e: ErrnoException) { throw SinkException(Reason.STORAGE, "sync failed (${e.errno})", e) } }

        override fun length(): Long = try { Os.fstat(pfd.fileDescriptor).st_size } catch (e: Exception) { -1L }

        override fun descriptor(): java.io.FileDescriptor? = pfd.fileDescriptor

        override fun publish(): String? {
            try { Os.fsync(pfd.fileDescriptor) } catch (e: ErrnoException) { /* the data was synced by the engine already */ }
            try { pfd.close() } catch (e: Exception) { /* ignore */ }
            val v = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            try { resolver.update(u, v, null, null) } catch (e: Exception) { throw SinkException(Reason.STORAGE, "could not publish the file", e) }
            return u.toString()
        }

        override fun discard() {
            try { pfd.close() } catch (e: Exception) { /* ignore */ }
            try { resolver.delete(u, null, null) } catch (e: Exception) { /* ignore */ }
        }
    }
}
