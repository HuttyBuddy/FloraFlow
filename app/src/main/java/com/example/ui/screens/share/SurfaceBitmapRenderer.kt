package com.example.ui.screens.share

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Uploads Bitmaps into a MediaCodec input Surface via OpenGL ES.
 *
 * A codec input Surface is a BufferQueue owned by the encoder, not a normal software
 * canvas: `Surface.lockCanvas()` on one is unsupported and either throws or produces
 * garbage frames depending on the device's GPU driver. Drawing through EGL is the
 * supported path, and it is what actually makes the export hardware-accelerated —
 * the texture upload and blit run on the GPU.
 *
 * Usage: [makeCurrent], then per frame [drawBitmap] + [setPresentationTime] + [swapBuffers],
 * and [release] when finished.
 */
class SurfaceBitmapRenderer(private val surface: Surface) {

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var program = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureHandle = 0
    private var textureId = 0

    private val vertexBuffer: FloatBuffer = createFloatBuffer(
        // x, y — a full-screen quad in clip space
        floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
        )
    )

    private val texCoordBuffer: FloatBuffer = createFloatBuffer(
        // u, v — flipped vertically: bitmaps are top-down, GL textures are bottom-up
        floatArrayOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )
    )

    fun makeCurrent() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed" }

        val version = IntArray(2)
        check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { "eglInitialize failed" }

        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            // Tells the driver this surface feeds a video encoder.
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        check(
            EGL14.eglChooseConfig(
                eglDisplay, configAttribs, 0, configs, 0, configs.size, numConfigs, 0
            ) && numConfigs[0] > 0
        ) { "eglChooseConfig failed" }

        eglContext = EGL14.eglCreateContext(
            eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
        )
        check(eglContext != EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed" }

        eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, configs[0], surface, intArrayOf(EGL14.EGL_NONE), 0
        )
        check(eglSurface != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface failed" }

        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            "eglMakeCurrent failed"
        }

        setupProgram()
    }

    private fun setupProgram() {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        check(linked[0] == GLES20.GL_TRUE) {
            "Shader link failed: ${GLES20.glGetProgramInfoLog(program)}"
        }

        // Shader objects are retained by the linked program.
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        check(compiled[0] == GLES20.GL_TRUE) {
            "Shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}"
        }
        return shader
    }

    /** Uploads [bitmap] and draws it over the whole surface. */
    fun drawBitmap(bitmap: Bitmap, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glUniform1i(textureHandle, 0)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)

        texCoordBuffer.position(0)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    /** Stamps the frame's timestamp, which becomes the encoder's presentation time. */
    fun setPresentationTime(nanoseconds: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nanoseconds)
    }

    /** Hands the finished frame to the encoder. */
    fun swapBuffers(): Boolean = EGL14.eglSwapBuffers(eglDisplay, eglSurface)

    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            if (program != 0) GLES20.glDeleteProgram(program)
            if (textureId != 0) GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            EGL14.eglMakeCurrent(
                eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
            )
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
        program = 0
        textureId = 0
    }

    private fun createFloatBuffer(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }

    companion object {
        /** EGL_RECORDABLE_ANDROID — not exposed as a constant by the EGL14 bindings. */
        private const val EGL_RECORDABLE_ANDROID = 0x3142

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}
