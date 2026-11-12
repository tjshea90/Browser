package app.kestrel.courier

import android.content.Context
import app.kestrel.core.courier.Pairing
import app.kestrel.core.courier.Sign

/** What the app remembers: the pairing secret, whether the owner switched "Ready" on, the port. Private to this app (no backup). */
class CourierPrefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("courier", Context.MODE_PRIVATE)

    /** The pairing secret: 80 random bits made on first use; the owner types its 16-character form once on the tablet. */
    @Synchronized fun secret(): ByteArray {
        val s = sp.getString("secret", null)?.let { Sign.unb64(it) }
        if (s != null && s.size == Pairing.SECRET_BYTES) return s
        return newSecret()
    }

    /** A new secret: every tablet paired before stops being served. */
    @Synchronized fun newSecret(): ByteArray {
        val b = Pairing.newSecret()
        sp.edit().putString("secret", Sign.b64(b)).apply()
        return b
    }

    fun code(): String = Pairing.format(secret())

    /** The owner left "Ready for the tablet" on: the service is started again after a reboot. */
    var ready: Boolean
        get() = sp.getBoolean("ready", false)
        set(v) { sp.edit().putBoolean("ready", v).apply() }

    var port: Int
        get() = sp.getInt("port", app.kestrel.core.courier.CourierPorts.API).coerceIn(1024, 65535)
        set(v) { sp.edit().putInt("port", v.coerceIn(1024, 65535)).apply() }
}
