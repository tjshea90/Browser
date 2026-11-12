package app.kestrel.core.courier

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Request authentication for the courier (research/PHONE_OFFLOAD.md sections 5 and 6.2): every request except `hello` carries
 * `X-K-Ts` (milliseconds, the sender's clock corrected by the skew the phone reported) and
 * `X-K-Sig = base64url(HMAC-SHA256(K, METHOD \n target \n ts \n hex(sha256(body))))`. [K] comes from the pairing secret, which is typed once.
 */
object Sign {
    const val H_TS = "x-k-ts"      // request header names are compared lower-case
    const val H_SIG = "x-k-sig"
    const val WINDOW_MS = 60_000L

    private val ENC = Base64.getUrlEncoder().withoutPadding()
    private val DEC = Base64.getUrlDecoder()
    private const val HEX = "0123456789abcdef"

    fun b64(b: ByteArray): String = ENC.encodeToString(b)
    fun unb64(s: String): ByteArray? = try { DEC.decode(s) } catch (e: IllegalArgumentException) { null }

    fun hex(b: ByteArray): String {
        val c = CharArray(b.size * 2)
        for (i in b.indices) { val v = b[i].toInt() and 0xFF; c[i * 2] = HEX[v shr 4]; c[i * 2 + 1] = HEX[v and 15] }
        return String(c)
    }

    fun sha256(b: ByteArray, off: Int = 0, len: Int = b.size): ByteArray {
        val m = MessageDigest.getInstance("SHA-256"); m.update(b, off, len); return m.digest()
    }

    fun hmac(key: ByteArray, vararg parts: ByteArray): ByteArray {
        val m = Mac.getInstance("HmacSHA256")
        m.init(SecretKeySpec(key, "HmacSHA256"))
        for (p in parts) m.update(p)
        return m.doFinal()
    }

    /** The long-lived key both sides derive from the pairing secret. */
    fun deriveKey(secret: ByteArray): ByteArray = hmac("kestrel-courier/v1".toByteArray(Charsets.UTF_8), secret)

    fun canonical(method: String, target: String, ts: Long, bodySha256Hex: String): ByteArray =
        (method.uppercase() + "\n" + target + "\n" + ts + "\n" + bodySha256Hex).toByteArray(Charsets.UTF_8)

    fun sign(key: ByteArray, method: String, target: String, ts: Long, body: ByteArray = EMPTY): String =
        b64(hmac(key, canonical(method, target, ts, hex(sha256(body)))))

    enum class Result { OK, MISSING, SKEW, BAD, REPLAY }

    /** Checks one request. [now] is the verifier's clock; a [replay] guard (optional) rejects a signature seen before. */
    fun check(key: ByteArray, method: String, target: String, tsHeader: String?, sigHeader: String?, body: ByteArray, now: Long, replay: ReplayGuard? = null): Result {
        if (tsHeader == null || sigHeader == null) return Result.MISSING
        val ts = tsHeader.trim().toLongOrNull() ?: return Result.BAD
        val got = unb64(sigHeader.trim()) ?: return Result.BAD
        val want = hmac(key, canonical(method, target, ts, hex(sha256(body))))
        if (!MessageDigest.isEqual(want, got)) return Result.BAD            // constant time
        if (Math.abs(now - ts) > WINDOW_MS) return Result.SKEW
        if (replay != null && !replay.firstTime(sigHeader.trim(), now)) return Result.REPLAY
        return Result.OK
    }

    /** What the phone answers to `hello?c=<challenge>`: proof that it knows [key], so the tablet never sends URLs or cookies to an impostor. */
    fun helloProof(key: ByteArray, challenge: String, t: Long): String =
        b64(hmac(key, "hello\n$challenge\n$t".toByteArray(Charsets.UTF_8)))

    fun helloProofOk(key: ByteArray, challenge: String, t: Long, proof: String?): Boolean {
        val got = if (proof == null) null else unb64(proof)
        val want = unb64(helloProof(key, challenge, t))!!
        return got != null && MessageDigest.isEqual(want, got)
    }

    private val EMPTY = ByteArray(0)
}

/** Remembers the signatures of the last two windows so the same signed request cannot be played twice. */
class ReplayGuard(private val keepMs: Long = Sign.WINDOW_MS * 2 + 1000, private val max: Int = 4096) {
    private val seen = LinkedHashMap<String, Long>()

    @Synchronized fun firstTime(sig: String, now: Long): Boolean {
        val it = seen.entries.iterator()
        while (it.hasNext()) { if (now - it.next().value > keepMs) it.remove() else break }   // oldest first
        if (seen.containsKey(sig)) return false
        if (seen.size >= max) { val e = seen.entries.iterator(); e.next(); e.remove() }
        seen[sig] = now
        return true
    }
}

/** Slows down guessing: 5 bad requests from one address block that address for a minute. */
class AuthThrottle(private val maxFails: Int = 5, private val blockMs: Long = 60_000L, private val maxTracked: Int = 256) {
    private class E { var fails = 0; var firstAt = 0L; var blockedUntil = 0L }
    private val map = LinkedHashMap<String, E>()

    @Synchronized fun blocked(ip: String, now: Long): Boolean { val e = map[ip] ?: return false; return now < e.blockedUntil }

    @Synchronized fun failed(ip: String, now: Long) {
        var e = map[ip]
        if (e == null) {
            if (map.size >= maxTracked) { val it = map.entries.iterator(); it.next(); it.remove() }
            e = E(); map[ip] = e
        }
        if (now - e.firstAt > blockMs) { e.fails = 0; e.firstAt = now }     // old failures are forgotten
        e.fails++
        if (e.fails >= maxFails) { e.blockedUntil = now + blockMs; e.fails = 0; e.firstAt = now }
    }

    @Synchronized fun ok(ip: String) { map.remove(ip) }
}

/** The pairing secret: 80 random bits shown as `ABCD-EFGH-JKLM-NPQR` and typed once on the tablet. Nothing travels at pairing time. */
object Pairing {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"      // 32 symbols, no I, O, 0, 1
    const val SECRET_BYTES = 10
    const val CODE_CHARS = 16

    fun newSecret(rng: SecureRandom = SecureRandom()): ByteArray { val b = ByteArray(SECRET_BYTES); rng.nextBytes(b); return b }

    fun format(secret: ByteArray): String {
        require(secret.size == SECRET_BYTES)
        val sb = StringBuilder()
        var acc = 0; var bits = 0; var n = 0
        for (x in secret) {
            acc = (acc shl 8) or (x.toInt() and 0xFF); bits += 8
            while (bits >= 5) {
                bits -= 5
                if (n > 0 && n % 4 == 0) sb.append('-')
                sb.append(ALPHABET[(acc shr bits) and 31]); n++
            }
            acc = acc and ((1 shl bits) - 1)
        }
        return sb.toString()
    }

    /** Reads what the owner typed (upper or lower case, spaces and dashes anywhere); null when it is not a valid code. */
    fun parse(text: String): ByteArray? {
        val chars = text.uppercase().filter { it != '-' && !it.isWhitespace() }
        if (chars.length != CODE_CHARS) return null
        val out = ByteArray(SECRET_BYTES)
        var acc = 0; var bits = 0; var o = 0
        for (c in chars) {
            val v = ALPHABET.indexOf(c)
            if (v < 0) return null
            acc = (acc shl 5) or v; bits += 5
            if (bits >= 8) { bits -= 8; out[o++] = ((acc shr bits) and 0xFF).toByte(); acc = acc and ((1 shl bits) - 1) }
        }
        return if (o == SECRET_BYTES) out else null
    }
}
