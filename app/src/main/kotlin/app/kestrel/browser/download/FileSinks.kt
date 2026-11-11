package app.kestrel.browser.download

import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import app.kestrel.core.download.DEST_APP_PRIVATE
import app.kestrel.core.download.Reason
import app.kestrel.core.download.Sink
import app.kestrel.core.download.SinkException
import app.kestrel.core.download.SinkFactory
import java.io.File
import java.io.FileDescriptor
import java.io.RandomAccessFile

/**
 * Files in the app's own folder (update APKs): `<name>.part` while downloading, renamed to `<name>` by [Sink.publish]. Written with positional `pwrite` on one descriptor like the public sink,
 * so the same parallel segments work. The uri is a plain `file:` URI so [AppSinks] can route reopen / delete.
 */
class FileSinks(private val dir: File) : SinkFactory {
    override fun create(name: String, mime: String?, total: Long, priv: Boolean): Sink = create(name, mime, total, priv, DEST_APP_PRIVATE)

    override fun create(name: String, mime: String?, total: Long, priv: Boolean, kind: Int): Sink {
        dir.mkdirs()
        val part = File(dir, "$name.part")
        val final = File(dir, name)
        try { part.delete(); final.delete() } catch (e: Exception) { /* ignore */ }
        val raf = try { RandomAccessFile(part, "rw") } catch (e: Exception) { throw SinkException(Reason.STORAGE, "could not create the file", e) }
        try { preallocate(raf.fd, total) } catch (e: SinkException) { try { raf.close() } catch (x: Exception) { /* ignore */ }; part.delete(); throw e }
        return FileSink(raf, part, final)
    }

    override fun reopen(uri: String, total: Long): Sink? {
        val f = pathOf(uri) ?: return null
        val part = if (f.name.endsWith(".part")) f else File(f.parentFile, f.name + ".part")
        if (!part.isFile) return null
        val raf = try { RandomAccessFile(part, "rw") } catch (e: Exception) { return null }
        return FileSink(raf, part, File(part.parentFile, part.name.removeSuffix(".part")))
    }

    override fun delete(uri: String): Boolean {
        val f = pathOf(uri) ?: return false
        val part = if (f.name.endsWith(".part")) f else File(f.parentFile, f.name + ".part")
        var ok = false
        for (x in listOf(f, part)) try { if (x.exists()) ok = x.delete() || ok } catch (e: Exception) { /* ignore */ }
        return ok
    }

    private fun pathOf(uri: String): File? {
        if (!uri.startsWith("file:")) return null
        val f = try { File(java.net.URI(uri).path) } catch (e: Exception) { return null }
        // never outside the app's own staging folder
        return if (f.canonicalPath.startsWith(dir.canonicalPath + File.separator)) f else null
    }

    private fun preallocate(fd: FileDescriptor, total: Long) {
        if (total <= 0) return
        try { Os.posix_fallocate(fd, 0, total) }
        catch (e: ErrnoException) {
            when (e.errno) {
                OsConstants.ENOSPC -> throw SinkException(Reason.NO_SPACE, "not enough free space", e)
                OsConstants.EFBIG -> throw SinkException(Reason.FS_LIMIT, "file too big for this storage", e)
                else -> try { Os.ftruncate(fd, total) } catch (e2: ErrnoException) { /* a sparse file is fine */ }
            }
        }
    }

    private class FileSink(val raf: RandomAccessFile, val part: File, val final: File) : Sink {
        override val uri: String? = part.toURI().toString()
        override val displayName: String? = final.name

        override fun pwrite(pos: Long, b: ByteArray, off: Int, len: Int) {
            var done = 0
            try {
                while (done < len) {
                    val n = Os.pwrite(raf.fd, b, off + done, len - done, pos + done)
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

        override fun sync() { try { Os.fsync(raf.fd) } catch (e: ErrnoException) { throw SinkException(Reason.STORAGE, "sync failed (${e.errno})", e) } }

        override fun length(): Long = try { Os.fstat(raf.fd).st_size } catch (e: Exception) { -1L }

        override fun publish(): String? {
            try { Os.fsync(raf.fd) } catch (e: ErrnoException) { /* synced by the engine already */ }
            try { raf.close() } catch (e: Exception) { /* ignore */ }
            if (!part.renameTo(final)) throw SinkException(Reason.STORAGE, "could not store the file")
            return final.toURI().toString()
        }

        override fun discard() {
            try { raf.close() } catch (e: Exception) { /* ignore */ }
            try { part.delete() } catch (e: Exception) { /* ignore */ }
        }
    }
}

/** Routes by destination: the public Downloads folder (MediaStore) or the app's private staging folder. */
class AppSinks(private val media: SinkFactory, private val files: SinkFactory) : SinkFactory {
    override fun create(name: String, mime: String?, total: Long, priv: Boolean): Sink = media.create(name, mime, total, priv)
    override fun create(name: String, mime: String?, total: Long, priv: Boolean, kind: Int): Sink =
        if (kind == DEST_APP_PRIVATE) files.create(name, mime, total, priv, kind) else media.create(name, mime, total, priv, kind)
    override fun reopen(uri: String, total: Long): Sink? = if (uri.startsWith("file:")) files.reopen(uri, total) else media.reopen(uri, total)
    override fun delete(uri: String): Boolean = if (uri.startsWith("file:")) files.delete(uri) else media.delete(uri)
}
