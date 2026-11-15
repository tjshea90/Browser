package app.kestrel.browser.net

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * RX4-X1: what an APK file in the Downloads list really is. The file is read through its MediaStore address (a file descriptor, so nothing is copied); only the manifest and the signature are
 * parsed, nothing runs. IO thread only (the signature check of a 90 MB file takes a moment on this CPU).
 */
object ApkInspect {
    class Info(val pkg: String, val label: String, val versionName: String, val permissions: List<String>, val signerSha256: String, val installedLabel: String, val sameSigner: Boolean?)

    /** The installer behind [uri], or null when the file is not an Android installer (or cannot be read): a video, a picture, a PDF, a plain ZIP. */
    fun read(ctx: Context, uri: Uri): Info? {
        val pfd = try { ctx.contentResolver.openFileDescriptor(uri, "r") } catch (e: Exception) { null } ?: return null
        pfd.use {
            val path = "/proc/self/fd/${it.fd}"
            if (!hasManifest(path)) return null
            val pm = ctx.packageManager
            @Suppress("DEPRECATION")
            val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
            val pi = try { pm.getPackageArchiveInfo(path, flags) } catch (e: Throwable) { null } ?: return null
            val ai = pi.applicationInfo
            val label = try { if (ai != null) { ai.sourceDir = path; ai.publicSourceDir = path; ai.loadLabel(pm).toString() } else "" } catch (e: Throwable) { "" }
            val signers = signerDigests(pi)
            var installedLabel = ""; var same: Boolean? = null
            try {
                val inst = pm.getPackageInfo(pi.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                installedLabel = inst.applicationInfo?.loadLabel(pm)?.toString() ?: pi.packageName
                same = signers.isNotEmpty() && signers == signerDigests(inst)
            } catch (e: PackageManager.NameNotFoundException) { /* not installed */ } catch (e: Throwable) { /* unknown */ }
            return Info(pi.packageName, label, pi.versionName ?: "", pi.requestedPermissions?.toList() ?: emptyList(), signers.firstOrNull() ?: "", installedLabel, same)
        }
    }

    /** Is this file, whatever its name says, a ZIP that holds an Android manifest? (a ".mp4" that is really an installer). Cheap: reads the ZIP's directory only. */
    fun hasManifest(path: String): Boolean = try { ZipFile(File(path)).use { z -> z.getEntry("AndroidManifest.xml") != null } } catch (e: Exception) { false }

    private fun signerDigests(pi: android.content.pm.PackageInfo): Set<String> {
        val si = pi.signingInfo ?: return emptySet()
        val sigs = if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
        return sigs?.map { s -> MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { b -> "%02x".format(b) } }?.toSet() ?: emptySet()
    }
}
