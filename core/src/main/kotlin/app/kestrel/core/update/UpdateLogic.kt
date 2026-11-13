package app.kestrel.core.update

/** One file attached to a GitHub Release. [digest] is GitHub's own "sha256:<hex>" field when it sent one. */
class ReleaseAsset(val name: String, val url: String, val size: Long, val digest: String?)

/** A GitHub Release as the in-app updater needs it (the Android side fills this from the Releases API JSON). */
class Release(
    val tag: String,
    val title: String,
    val body: String,
    val prerelease: Boolean,
    val draft: Boolean,
    val pageUrl: String,
    val assets: List<ReleaseAsset>,
)

/** A release that is newer than the installed app and has an APK to install. */
class UpdateOffer(
    val tag: String,
    val versionName: String,
    val versionCode: Long,
    val apkName: String,
    val apkUrl: String,
    val size: Long,
    val sha256: String?,
    val notes: String,
    val pageUrl: String,
)

/**
 * The decisions of the in-app updater, free of Android and of any network code so they are unit-tested.
 *
 * What GitHub Actions publishes (`.github/scripts/publish-release.sh`): tag `kestrel-g-v<versionName>` (Kestrel G) or `kestrel-v<versionName>` (Kestrel),
 * an APK asset, and a body of the form
 *
 *     <change note>
 *
 *     **Kestrel G 0.2.0-alpha4** (versionCode 103). <about text>
 *
 *     - Install: ...
 *     - APK SHA-256: `<64 hex>`
 *
 * The versionCode in the body is what is compared with the installed app's versionCode; the SHA-256 in the body is what the downloaded file is checked against.
 * Whatever is installed is ALSO checked by Android itself: an update is accepted only when it carries the same signing certificate as the installed app.
 */
object UpdateLogic {
    private val CODE = Regex("""\(versionCode\s+(\d+)\)""")
    private val SHA = Regex("""APK SHA-256:\s*`?([0-9a-fA-F]{64})`?""")

    private val REF = Regex(""""ref"\s*:\s*"refs/tags/([^"]+)"""")

    /** The tag names in a `git/matching-refs/tags/<prefix>` answer, in whatever order GitHub sent them. */
    fun tagsFromRefs(json: String): List<String> = REF.findAll(json).map { it.groupValues[1] }.toList()

    /**
     * Natural order of release tags: runs of digits compare as NUMBERS, everything else as text ("...alpha10" is newer than "...alpha9"; plain text order gets this wrong, and so does GitHub's own
     * release list, which for this repository is sorted by tag name: alpha9, alpha8, ... alpha2, alpha13, alpha12, alpha11, alpha10).
     */
    fun compareTags(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val da = a[i].isDigit()
            val db = b[j].isDigit()
            if (da && db) {
                var ie = i; while (ie < a.length && a[ie].isDigit()) ie++
                var je = j; while (je < b.length && b[je].isDigit()) je++
                val na = a.substring(i, ie).trimStart('0')
                val nb = b.substring(j, je).trimStart('0')
                if (na.length != nb.length) return na.length - nb.length
                val c = na.compareTo(nb)
                if (c != 0) return c
                i = ie; j = je
            } else {
                if (da != db) return if (da) -1 else 1
                val c = a[i].compareTo(b[j])
                if (c != 0) return c
                i++; j++
            }
        }
        return (a.length - i) - (b.length - j)
    }

    /** The [count] newest tags that start with [prefix], newest first, out of tags given in ANY order. */
    fun newestTags(tags: Collection<String>, prefix: String, count: Int): List<String> =
        tags.filter { it.startsWith(prefix) }.sortedWith { x, y -> compareTags(y, x) }.take(count)

    fun versionCode(body: String): Long? = CODE.find(body)?.groupValues?.get(1)?.toLongOrNull()

    /** The checksum of the APK: the body line we wrote ourselves first, GitHub's per-asset digest as the fallback. Lower case, or null. */
    fun sha256(body: String, asset: ReleaseAsset?): String? {
        SHA.find(body)?.groupValues?.get(1)?.let { return it.lowercase() }
        val d = asset?.digest ?: return null
        if (!d.startsWith("sha256:")) return null
        return d.substring(7).takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' } }?.lowercase()
    }

    /** The human change note: everything before the generated "**Title version** (versionCode N)" paragraph. */
    fun notes(body: String): String {
        val at = Regex("""(?m)^\*\*.+\*\*\s+\(versionCode""").find(body)?.range?.first ?: body.length
        return body.substring(0, at).trim().take(700)
    }

    /**
     * The newest release of THIS app (its [tagPrefix]) whose versionCode is above [currentCode] and that has an APK; null when the app is up to date.
     * Drafts are never offered. Pre-releases are offered (every Kestrel G build so far is an alpha).
     */
    fun choose(releases: List<Release>, tagPrefix: String, currentCode: Long): UpdateOffer? {
        var best: UpdateOffer? = null
        for (r in releases) {
            if (r.draft || !r.tag.startsWith(tagPrefix)) continue
            val code = versionCode(r.body) ?: continue
            if (code <= currentCode || (best != null && code <= best.versionCode)) continue
            val apk = r.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: continue
            best = UpdateOffer(r.tag, r.tag.removePrefix(tagPrefix), code, apk.name, apk.url, apk.size, sha256(r.body, apk), notes(r.body), r.pageUrl)
        }
        return best
    }

    /** 92.4 MB style size for the confirm dialog and progress line. */
    fun formatSize(bytes: Long): String = when {
        bytes < 0 -> "unknown size"
        bytes < 1024 -> "$bytes B"
        bytes < 1024L * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(java.util.Locale.ROOT, bytes / 1048576.0)
        else -> "%.2f GB".format(java.util.Locale.ROOT, bytes / 1073741824.0)
    }

    /** The system defaults of Android 10's low-storage reserve (StorageManager.DEFAULT_THRESHOLD_PERCENTAGE / DEFAULT_THRESHOLD_MAX_BYTES). */
    const val DEFAULT_LOW_PERCENT = 5
    const val DEFAULT_LOW_MAX_BYTES = 500L shl 20

    /**
     * What Android keeps free after any install: StorageManager.getStorageLowBytes = the smaller of [percent] % of the storage and [maxBytes] (settings `sys_storage_threshold_percentage` and
     * `sys_storage_threshold_max_bytes`; the defaults are 5 % and 500 MiB). Android 10 source, and measured on the emulator (research/ADB_FOR_THE_OWNER.md): 5 % of 5.8 GiB = 297.6 MiB moved to 119 MiB
     * with 2 %, to 100 MiB with a 100 MiB cap, and stayed at the 500 MiB cap with 20 %. A 27.75 GB tablet therefore keeps 500 MiB free, NOT a tenth (2.8 GB) as alpha8-alpha30 assumed.
     */
    fun lowReserve(totalBytes: Long, percent: Int = DEFAULT_LOW_PERCENT, maxBytes: Long = DEFAULT_LOW_MAX_BYTES): Long =
        minOf(totalBytes.coerceAtLeast(0L) * percent.coerceIn(0, 100) / 100, maxBytes.coerceAtLeast(0L))

    /**
     * What Android's installer needs before it will finish an install or an update. MEASURED on the Android 10 emulator (research/ADB_FOR_THE_OWNER.md), fresh installs by `adb install`, bisecting the free
     * space: it refuses (INSTALL_FAILED_INSUFFICIENT_STORAGE) unless the free space BEFORE the install is at least [reserve] ([lowReserve]) + TWICE (package + unpacked native libraries) + a little for optimised code:
     * 2 MB app: 302-304 MiB (= 297.6 + 2 x 2.3); Gecko APK 95.5 MiB + 187.4 MiB of libraries: 861 fails / 868 passes (= 297.6 + 565.8 = 863.4), 676 / 685 with a 2 % reserve (= 119 + 565.8), passes at 760
     * with a 100 MiB cap. (Not the 10 % of the partition that research/UPDATE_STORAGE.md first took from a logcat line: that line is the periodic storage monitor asking for 2 x the reserve.)
     * [free] = free space + the caches the system may clear (StorageStatsManager.getFreeBytes). Our own downloaded copy is already counted as used; it is deleted once the installer holds its own copy
     * ([StoragePlan.fitsDropping]), which hands one APK of room back.
     * This is why "uninstall first" always worked: the uninstall frees the old install and its data (about a gigabyte), so the same APK then clears the bar. In place and fresh need the same free space BEFORE.
     */
    class StoragePlan(val needKeep: Long, val needDrop: Long, val free: Long, val reserve: Long, val installTakes: Long) {
        /** Starts with our downloaded file kept (so a retry needs no new download). */
        val fitsKeeping: Boolean get() = free >= needKeep
        /** Fits only if our own copy is deleted right after the installer has taken its copy. */
        val fitsDropping: Boolean get() = !fitsKeeping && free >= needDrop
        /** Bytes that must still be freed (the copy being dropped); 0 = it fits. */
        val shortBy: Long get() = if (free >= needDrop) 0L else needDrop - free
    }

    /** [marginBytes]: the optimised code the installer compiles (measured 31 MiB for the release build; 48 leaves room for a larger dex). */
    fun storagePlan(apkBytes: Long, nativeLibBytes: Long, freeBytes: Long, reserveBytes: Long, marginBytes: Long = 48L shl 20): StoragePlan {
        val needDrop = reserveBytes + apkBytes + nativeLibBytes + marginBytes // our own copy is deleted once the installer has its own: that frees one APK
        return StoragePlan(needKeep = needDrop + apkBytes, needDrop = needDrop, free = freeBytes, reserve = reserveBytes, installTakes = 2 * apkBytes + nativeLibBytes + marginBytes)
    }

    /**
     * "Install the other way" (uninstall first, then install the saved file) is only a cure when the uninstall frees enough: the owner's tablet (alpha27 Diagnostics, 2026-11-13) had 1.62 GB free of 27.75 GB,
     * the install needs about 3.1 GB, and an uninstall hands back only about 0.5 GB, so it would have left NO Kestrel G on the tablet and Android still refusing the saved file.
     * [freeAfter]: the free space once the uninstall has handed back [ownBytes] (package, unpacked libraries, optimised code, data, cache), after the saved copy in Downloads took one APK (our own downloaded
     * copy goes with the app's data: it is in [ownBytes] when it exists, and added here when the file still has to be downloaded). [need]: the bar for the fresh install with that copy kept
     * ([StoragePlan.needKeep]).
     */
    class OtherWayPlan(val freeAfter: Long, val need: Long) {
        val fits: Boolean get() = freeAfter >= need
        val shortBy: Long get() = if (fits) 0L else need - freeAfter
    }

    fun otherWayPlan(plan: StoragePlan, ownBytes: Long, apkBytes: Long, fileKept: Boolean): OtherWayPlan {
        val ours = if (fileKept) ownBytes else ownBytes + apkBytes // a copy that is still to be downloaded will be on the disk (and leave with the app) by then
        return OtherWayPlan((plan.free + ours - apkBytes).coerceAtLeast(0L), plan.needKeep)
    }

    /** Round a byte count UP to whole tens of megabytes (what the owner is told to free). */
    fun roundUpTenMb(bytes: Long): Long { val ten = 10L shl 20; return ((bytes + ten - 1) / ten) * ten }

    /** A download may continue from byte [have] only when the server answered 206 for exactly that offset (else the partial file must be restarted). */
    fun canResume(have: Long, status: Int, contentRangeStart: Long?): Boolean = have > 0 && status == 206 && contentRangeStart == have
}
