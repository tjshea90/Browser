package app.kestrel.browser

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20

/**
 * The tablet's GPU as the driver reports it (vendor / renderer / GL version, which for Mali carries the driver build "v1.rNpM"), read once through a 1x1 pbuffer
 * context for Diagnostics. It proves which GPU the engine is really drawing with and whether the driver is one Mozilla's Mali scroll bugs apply to
 * (research/SCROLL.md Q4, A3). No engine pref is touched: WebGL's renderer string stays sanitised for pages.
 */
object GlInfo {
    @Volatile var text = "GPU: (not read yet)"
    @Volatile private var done = false

    /** Blocking (tens of ms): call off the UI thread. Safe to call again; reads once. */
    fun read() {
        if (done) return
        done = true
        text = try { query(3) ?: query(2) ?: "GPU: no OpenGL ES context could be created" } catch (e: Throwable) { "GPU: unreadable ($e)" }
    }

    private fun query(version: Int): String? {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return null
        val v = IntArray(2)
        if (!EGL14.eglInitialize(display, v, 0, v, 1)) return null
        var ctx = EGL14.EGL_NO_CONTEXT
        var surf = EGL14.EGL_NO_SURFACE
        try {
            val renderable = if (version >= 3) 0x40 /* EGL_OPENGL_ES3_BIT_KHR */ else EGL14.EGL_OPENGL_ES2_BIT
            val cfgAttrs = intArrayOf(EGL14.EGL_RENDERABLE_TYPE, renderable, EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE)
            val cfgs = arrayOfNulls<EGLConfig>(1); val n = IntArray(1)
            if (!EGL14.eglChooseConfig(display, cfgAttrs, 0, cfgs, 0, 1, n, 0) || n[0] < 1) return null
            ctx = EGL14.eglCreateContext(display, cfgs[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, version, EGL14.EGL_NONE), 0)
            if (ctx == EGL14.EGL_NO_CONTEXT) return null
            surf = EGL14.eglCreatePbufferSurface(display, cfgs[0], intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0)
            if (surf == EGL14.EGL_NO_SURFACE || !EGL14.eglMakeCurrent(display, surf, surf, ctx)) return null
            return "GPU: ${GLES20.glGetString(GLES20.GL_VENDOR)} / ${GLES20.glGetString(GLES20.GL_RENDERER)} / ${GLES20.glGetString(GLES20.GL_VERSION)}"
        } finally {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (surf != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surf)
            if (ctx != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, ctx)
            EGL14.eglTerminate(display)
        }
    }
}
