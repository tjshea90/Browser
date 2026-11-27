package app.kestrel.core.download

/**
 * RX4-X1 (research/REVIEW-EXTERNAL-4.md): a fake "Kestrel update" must not install over the app.
 *
 * Why: the signing key of the releases is public (signing/dev/, SECURITY.md SEC-1), and Android treats an APK with the app's package name and that key as an UPDATE that keeps the cookies,
 * sessions and permissions. A page can hand the browser such a file ("update your player"). The only sanctioned way a Kestrel APK reaches this tablet is the Updates screen (UpdateManager), so
 * an APK from the Downloads list that CLAIMS to be Kestrel (package of this app, its debug twin, or any `app.kestrel.*`) is refused; every other APK is opened only after the question has
 * said what it is (name, package, the powerful permissions in plain words) instead of the generic "can install software". Pure rules; the Android side reads the file
 * (PackageManager.getPackageArchiveInfo) and calls these.
 */
object ApkIdentity {
    const val OWN_PREFIX = "app.kestrel."

    enum class Verdict { OWN_UPDATE_OK, REFUSE_LOOKALIKE, SHOW_FACTS }

    /** The file says it is this app, its debug twin, a sibling (the phone app) or anything else of ours. */
    fun claimsToBeKestrel(pkg: String, ownPkg: String): Boolean = pkg == ownPkg || pkg.startsWith(OWN_PREFIX) || pkg.removeSuffix(".debug") == ownPkg.removeSuffix(".debug")

    /** [cameFromUpdater] = this is the copy the Updates screen saved itself (its file name and size are the offer's). */
    fun verdict(pkg: String, ownPkg: String, cameFromUpdater: Boolean): Verdict = when {
        !claimsToBeKestrel(pkg, ownPkg) -> Verdict.SHOW_FACTS
        cameFromUpdater -> Verdict.OWN_UPDATE_OK
        else -> Verdict.REFUSE_LOOKALIKE
    }

    const val LOOKALIKE_TITLE = "This is not a Kestrel update"
    const val LOOKALIKE_TEXT = "This file says it is Kestrel, but it did not come from Kestrel's own Updates screen (menu > Updates). Do not install it: a page can hand you a file like this to take over the app and its saved logins."

    /** Permission -> words a person understands (only the powerful ones; a normal app asks for many that mean little). Order = how worrying. */
    private val WORDS = linkedMapOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to "read and tap the screen (accessibility)",
        "android.permission.BIND_DEVICE_ADMIN" to "act as a device administrator",
        "android.permission.REQUEST_INSTALL_PACKAGES" to "install other apps",
        "android.permission.INSTALL_PACKAGES" to "install other apps",
        "android.permission.SYSTEM_ALERT_WINDOW" to "draw over other apps",
        "android.permission.SEND_SMS" to "send text messages",
        "android.permission.READ_SMS" to "read text messages",
        "android.permission.RECEIVE_SMS" to "read text messages",
        "android.permission.READ_CONTACTS" to "read your contacts",
        "android.permission.READ_CALL_LOG" to "read your call history",
        "android.permission.CALL_PHONE" to "make phone calls",
        "android.permission.RECORD_AUDIO" to "use the microphone",
        "android.permission.CAMERA" to "use the camera",
        "android.permission.ACCESS_FINE_LOCATION" to "know where you are",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "know where you are, also in the background",
        "android.permission.QUERY_ALL_PACKAGES" to "list every app on the tablet",
        "android.permission.WRITE_EXTERNAL_STORAGE" to "change your files",
        "android.permission.MANAGE_EXTERNAL_STORAGE" to "change all your files",
        "android.permission.READ_EXTERNAL_STORAGE" to "read your files",
    )

    /** The plain-words list for the permissions an app asks for, without repeats, the most worrying first. */
    fun dangerousWords(permissions: Collection<String>): List<String> {
        val have = permissions.toHashSet()
        val out = ArrayList<String>()
        for ((p, w) in WORDS) if (p in have && w !in out) out.add(w)
        return out
    }

    /**
     * The question for an APK that is not ours: what it is and what it asks for. [installedLabel] = the app of this package that is installed now ("" = none),
     * [sameSigner] = it is signed like the installed one (null = not known or not installed).
     */
    fun facts(label: String, pkg: String, permissionCount: Int, words: List<String>, installedLabel: String, sameSigner: Boolean?): String = buildString {
        append(if (label.isNotBlank() && label != pkg) "$label ($pkg)" else pkg)
        append(" is an app installer. It asks for ").append(permissionCount).append(if (permissionCount == 1) " permission" else " permissions")
        if (words.isNotEmpty()) append(", among them: ").append(words.take(6).joinToString(", ")).append(if (words.size > 6) " and more" else "")
        append('.')
        when {
            installedLabel.isNotBlank() && sameSigner == true -> append(" It would update ").append(installedLabel).append(", which is installed now.")
            installedLabel.isNotBlank() && sameSigner == false -> append(" ").append(installedLabel).append(" is installed, but this file is signed by somebody else, so Android will refuse the update.")
            else -> append(" It is not installed on this tablet.")
        }
        append(" Only continue if you trust the site it came from.")
    }

    /** Extensions of files that are not programs: a file with one of them that is really an installer is disguised. */
    fun extensionCanLie(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in HARMLESS_LOOKING

    private val HARMLESS_LOOKING = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "mp3", "m4a", "ogg", "wav", "flac", "jpg", "jpeg", "png", "gif", "webp", "pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx")
}
