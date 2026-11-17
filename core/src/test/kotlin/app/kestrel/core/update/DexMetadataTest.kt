package app.kestrel.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** LAB6: the pure rules of compile-at-install (the dex-metadata file next to the update's APK). */
class DexMetadataTest {
    private fun real(): ByteArray = DexMetadataTest::class.java.getResourceAsStream("/update/agp-baseline-1.dm")!!.readBytes()

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> for ((n, b) in entries) { z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() } }
        return out.toByteArray()
    }

    @Test fun theFileIsNamedAfterItsApk() {
        assertEquals("kestrel-g-0.3.0-alpha65.dm", DexMetadata.nameFor("kestrel-g-0.3.0-alpha65.apk"))
        assertEquals("x.dm", DexMetadata.nameFor("x.APK"))
        assertEquals("noext.dm", DexMetadata.nameFor("noext"))
    }

    @Test fun onlyAndroid9To11GetAFile() {
        val now = 1_000_000_000_000L
        assertFalse(DexMetadata.mayAttach(27, 0L, now))
        assertTrue(DexMetadata.mayAttach(28, 0L, now))
        assertTrue(DexMetadata.mayAttach(29, 0L, now))   // the owner's tablet
        assertTrue(DexMetadata.mayAttach(30, 0L, now))
        assertFalse(DexMetadata.mayAttach(31, 0L, now))
        assertFalse(DexMetadata.mayAttach(36, 0L, now))  // the Android 16 phone: no profile of its format is published
    }

    @Test fun afterARefusalNothingIsAttachedForTwoWeeks() {
        val now = 1_000_000_000_000L
        assertFalse(DexMetadata.mayAttach(29, now - 1000, now))
        assertFalse(DexMetadata.mayAttach(29, now - DexMetadata.REFUSED_FOR_MS + 1, now))
        assertTrue(DexMetadata.mayAttach(29, now - DexMetadata.REFUSED_FOR_MS, now))
        assertTrue("a clock that went back is not a refusal for ever", DexMetadata.mayAttach(29, now + 5000, now))
    }

    @Test fun theRealFileAgpWritesIsPlausible() {
        val b = real()
        assertTrue("the AGP file is ${b.size} bytes", b.size < 20_000)
        assertTrue(DexMetadata.isPlausible(b))
    }

    @Test fun obviouslyWrongFilesAreNot() {
        val good = zip("primary.prof" to ByteArray(500) { 1 }, "primary.profm" to ByteArray(50) { 2 })
        assertTrue(DexMetadata.isPlausible(good))
        assertFalse("empty", DexMetadata.isPlausible(ByteArray(0)))
        assertFalse("an error page saved as .dm", DexMetadata.isPlausible("<html><body>Not Found</body></html> padding padding padding".toByteArray()))
        assertFalse("random bytes", DexMetadata.isPlausible(ByteArray(400) { (it * 31).toByte() }))
        assertFalse("a truncated download", DexMetadata.isPlausible(real().copyOf(real().size / 2)))
        assertFalse("a zip without the profile", DexMetadata.isPlausible(zip("readme.txt" to ByteArray(100))))
        assertFalse("a folder inside", DexMetadata.isPlausible(zip("primary.prof" to ByteArray(100), "x/y.txt" to ByteArray(10))))
        assertFalse("a path that climbs", DexMetadata.isPlausible(zip("primary.prof" to ByteArray(100), "../evil" to ByteArray(10))))
        assertFalse("too many entries", DexMetadata.isPlausible(zip("primary.prof" to ByteArray(10), "a" to ByteArray(1), "b" to ByteArray(1), "c" to ByteArray(1), "d" to ByteArray(1), "e" to ByteArray(1), "f" to ByteArray(1))))
        assertFalse("far too big", DexMetadata.isPlausible(zip("primary.prof" to ByteArray(DexMetadata.MAX_BYTES.toInt() + 10) { (it % 251).toByte() })))
    }

    @Test fun theInstallersRefusalOfTheFileIsRecognisedByItsText() {
        assertTrue(DexMetadata.isRefusal("INSTALL_FAILED_BAD_DEX_METADATA: Failed to verify dex metadata files"))
        assertTrue(DexMetadata.isRefusal("Failure [INSTALL_FAILED_BAD_DEX_METADATA: ...]"))
        assertTrue(DexMetadata.isRefusal("the dex metadata file is not valid"))
        assertFalse(DexMetadata.isRefusal("INSTALL_FAILED_INSUFFICIENT_STORAGE"))
        assertFalse(DexMetadata.isRefusal("INSTALL_FAILED_UPDATE_INCOMPATIBLE: signatures do not match"))
        assertFalse(DexMetadata.isRefusal(null))
        assertFalse(DexMetadata.isRefusal(""))
    }

    @Test fun anOfferCarriesTheMetadataAssetOfItsApkAndItsDigestOnly() {
        val sha = "a".repeat(64)
        val apk = ReleaseAsset("kestrel-g-0.3.0-alpha65.apk", "https://x/apk", 95_000_000, "sha256:" + "b".repeat(64))
        val dm = ReleaseAsset("kestrel-g-0.3.0-alpha65.dm", "https://x/dm", 6941, "sha256:" + sha.uppercase())
        val other = ReleaseAsset("kestrel-g-0.3.0-alpha64.dm", "https://x/old", 1, null)
        val rel = Release("kestrel-g-v0.3.0-alpha65", "t", "note\n\n**Kestrel G 0.3.0-alpha65** (versionCode 166). about\n\n- APK SHA-256: `${"c".repeat(64)}`", true, false, "p", listOf(other, apk, dm))
        val o = UpdateLogic.choose(listOf(rel), "kestrel-g-v", 165)!!
        assertEquals("kestrel-g-0.3.0-alpha65.dm", o.dmName)
        assertEquals("https://x/dm", o.dmUrl)
        assertEquals(6941L, o.dmSize)
        assertEquals("the file's own digest, lower case", sha, o.dmSha256)
        assertEquals("the APK keeps the checksum line of the release text", "c".repeat(64), o.sha256)
    }

    @Test fun aReleaseWithoutTheMetadataAssetOffersNone() {
        val apk = ReleaseAsset("kestrel-g-0.3.0-alpha65.apk", "https://x/apk", 95_000_000, null)
        val rel = Release("kestrel-g-v0.3.0-alpha65", "t", "n\n\n**Kestrel G 0.3.0-alpha65** (versionCode 166). a", true, false, "p", listOf(apk))
        val o = UpdateLogic.choose(listOf(rel), "kestrel-g-v", 165)!!
        assertNull(o.dmName); assertNull(o.dmUrl); assertEquals(-1L, o.dmSize); assertNull(o.dmSha256)
    }
}
