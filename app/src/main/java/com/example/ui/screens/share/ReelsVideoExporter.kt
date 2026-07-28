package com.example.ui.screens.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.core.content.FileProvider
import com.example.audio.OfflineSoundscapeRenderer
import com.example.audio.Soundscape
import com.example.data.model.PlantParentArchetype
import java.io.File
import kotlin.math.sin

/**
 * Exports the shareable 15-second ambient Reel: 1080x1920 H.264 video **with** the user's
 * actual binaural soundscape on an AAC track.
 *
 * Two things matter for this file's job as a distribution asset:
 *  - It carries real audio. The soundscape is the product's differentiator, and a silent
 *    clip is both a broken promise and suppressed by short-form feed ranking.
 *  - Frames reach the encoder through OpenGL ([SurfaceBitmapRenderer]) rather than
 *    `Surface.lockCanvas`, which is unsupported on a codec input surface and fails or
 *    corrupts frames depending on the GPU driver.
 */
object ReelsVideoExporter {

    private const val TAG = "ReelsVideoExporter"

    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val FPS = 30
    private const val DURATION_SECONDS = 15
    private const val BIT_RATE = 6_000_000 // 6 Mbps — crisp after TikTok/Reels re-encode

    /** Carrier tone the binaural pair is built on, matching the live Restoration session. */
    private const val BINAURAL_CARRIER_HZ = 200f

    data class Particle(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speedY: Float,
        var alpha: Int
    )

    /**
     * Renders and muxes the clip. Blocking — call from a background dispatcher.
     *
     * @param frequencyHz the binaural beat frequency; also drives which ambient scene plays
     * @param shareCode the viewer's short referral code, stamped into the watermark
     * @param onComplete receives the finished file and whether an audio track made it in —
     *   a device with no usable AAC encoder still gets a (muted) video rather than nothing
     */
    fun generate15SecondReelVideo(
        context: Context,
        score: Int = 88,
        archetype: PlantParentArchetype = PlantParentArchetype.JUNGLE_MAXIMALIST,
        frequencyHz: Float = 10.0f,
        shareCode: String? = null,
        onProgress: (Float) -> Unit = {},
        onComplete: (File, Boolean) -> Unit = { _, _ -> },
        onError: (Exception) -> Unit = {}
    ) {
        val outputFile = File(context.cacheDir, "floraflow_ambient_reel.mp4")
        if (outputFile.exists()) outputFile.delete()

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var glRenderer: SurfaceBitmapRenderer? = null
        var inputSurface: Surface? = null
        var muxerStarted = false

        try {
            // ---------------------------------------------------------------
            // 1. Audio: render the real soundscape, then encode it up front so
            //    the muxer can be started with both tracks registered.
            // ---------------------------------------------------------------
            val sceneId = sceneForBeatFrequency(frequencyHz)
            val encodedAudio = try {
                val pcm = OfflineSoundscapeRenderer.renderPcm16(
                    sceneId = sceneId,
                    baseFreqHz = BINAURAL_CARRIER_HZ,
                    beatFreqHz = frequencyHz,
                    durationSeconds = DURATION_SECONDS.toFloat()
                )
                AacAudioEncoder.encode(
                    pcm = pcm,
                    sampleRate = Soundscape.SAMPLE_RATE,
                    channelCount = 2
                )
            } catch (e: Exception) {
                // A device with no usable AAC encoder still gets a video — just muted.
                Log.w(TAG, "Audio encode failed, exporting video-only: ${e.message}")
                null
            }
            onProgress(0.1f)

            // ---------------------------------------------------------------
            // 2. Video encoder + GL input surface
            // ---------------------------------------------------------------
            val videoFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT
            ).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            // Non-null locals for use below; the nullable vars exist only so `finally` can
            // clean up whatever was constructed before a failure.
            val videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder = videoEncoder
            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val codecSurface = videoEncoder.createInputSurface()
            inputSurface = codecSurface
            videoEncoder.start()

            val renderer = SurfaceBitmapRenderer(codecSurface).apply { makeCurrent() }
            glRenderer = renderer

            val mediaMuxer =
                MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mediaMuxer
            var videoTrack = -1
            var audioTrack = -1
            var nextAudioPacket = 0

            val bufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()

            /**
             * Writes every audio packet up to [untilUs] so the two tracks stay interleaved.
             * Pass Long.MAX_VALUE to flush the remainder.
             */
            fun drainAudioUpTo(untilUs: Long) {
                val audio = encodedAudio ?: return
                if (audioTrack < 0) return
                while (nextAudioPacket < audio.packets.size) {
                    val packet = audio.packets[nextAudioPacket]
                    if (packet.presentationTimeUs > untilUs) break
                    val buffer = java.nio.ByteBuffer.wrap(packet.data)
                    audioBufferInfo.set(0, packet.data.size, packet.presentationTimeUs, packet.flags)
                    mediaMuxer.writeSampleData(audioTrack, buffer, audioBufferInfo)
                    nextAudioPacket++
                }
            }

            /** Pulls whatever the encoder has ready and muxes it. */
            fun drainVideo(endOfStream: Boolean) {
                // Bounded so a codec that never reports end-of-stream can't hang the export.
                var idleWaits = 0
                while (true) {
                    val status = videoEncoder.dequeueOutputBuffer(
                        bufferInfo,
                        if (endOfStream) 10_000 else 0
                    )
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (!endOfStream) return
                        if (++idleWaits > 100) {
                            Log.w(TAG, "Encoder never signalled end-of-stream; finishing anyway")
                            return
                        }
                        continue
                    }
                    idleWaits = 0

                    if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // Both track formats are known here — safe to start the muxer.
                        videoTrack = mediaMuxer.addTrack(videoEncoder.outputFormat)
                        if (encodedAudio != null) {
                            audioTrack = mediaMuxer.addTrack(encodedAudio.format)
                        }
                        mediaMuxer.start()
                        muxerStarted = true
                        continue
                    }
                    if (status < 0) return

                    val encodedData = videoEncoder.getOutputBuffer(status)
                    val isCodecConfig =
                        bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0

                    if (encodedData != null && bufferInfo.size > 0 && !isCodecConfig && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer.writeSampleData(videoTrack, encodedData, bufferInfo)
                        drainAudioUpTo(bufferInfo.presentationTimeUs)
                    }
                    videoEncoder.releaseOutputBuffer(status, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }

            // ---------------------------------------------------------------
            // 3. Frame loop
            // ---------------------------------------------------------------
            val totalFrames = FPS * DURATION_SECONDS
            val frameBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)
            val painter = FramePainter(shareCode)

            val random = java.util.Random(42)
            val particles = List(35) {
                Particle(
                    x = random.nextFloat() * WIDTH,
                    y = random.nextFloat() * HEIGHT,
                    radius = 8f + random.nextFloat() * 16f,
                    speedY = 1.5f + random.nextFloat() * 3.5f,
                    alpha = 60 + random.nextInt(120)
                )
            }

            for (i in 0 until totalFrames) {
                painter.drawFrame(
                    canvas = frameCanvas,
                    frameIndex = i,
                    score = score,
                    archetype = archetype,
                    frequencyHz = frequencyHz,
                    sceneLabel = Soundscape.sceneLabel(sceneId),
                    particles = particles,
                    random = random
                )

                renderer.drawBitmap(frameBitmap, WIDTH, HEIGHT)
                renderer.setPresentationTime(i * 1_000_000_000L / FPS)
                renderer.swapBuffers()

                drainVideo(endOfStream = false)

                // Frames are 90% of the work; audio encoding already claimed the first 10%.
                onProgress(0.1f + 0.9f * (i + 1).toFloat() / totalFrames)
            }

            videoEncoder.signalEndOfInputStream()
            drainVideo(endOfStream = true)

            // Any audio past the final video timestamp still belongs in the file.
            drainAudioUpTo(Long.MAX_VALUE)

            frameBitmap.recycle()
            onProgress(1f)
            onComplete(outputFile, encodedAudio != null && encodedAudio.packets.isNotEmpty())
        } catch (e: Exception) {
            Log.e(TAG, "Reel export failed", e)
            onError(e)
        } finally {
            try {
                glRenderer?.release()
            } catch (_: Exception) {
            }
            try {
                inputSurface?.release()
            } catch (_: Exception) {
            }
            try {
                encoder?.stop()
            } catch (_: Exception) {
            }
            try {
                encoder?.release()
            } catch (_: Exception) {
            }
            try {
                if (muxerStarted) muxer?.stop()
            } catch (_: Exception) {
            }
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
        }
    }

    /** Alpha/Theta/Delta bands each get the ambient scene the Restoration Journal pairs them with. */
    private fun sceneForBeatFrequency(frequencyHz: Float): Int = when {
        frequencyHz >= 8f -> Soundscape.SCENE_BREEZE // Alpha focus & above
        frequencyHz >= 4f -> Soundscape.SCENE_RAIN   // Theta meditation
        else -> Soundscape.SCENE_OCEAN               // Delta sleep
    }

    fun shareReelVideo(context: Context, videoFile: File, shareText: String? = null) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    shareText
                        ?: "My 15s Ambient Soundscape Reel 🌿✨ Check out FloraFlow AI for plant sanctuary focus vibes!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Ambient Reel Video")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Reel share failed", e)
        }
    }

    /**
     * Draws one frame. Paint objects are allocated once and reused — at 450 frames,
     * per-frame Paint allocation was measurable garbage pressure.
     */
    private class FramePainter(private val shareCode: String?) {

        private val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#04140D"),
                    Color.parseColor("#092B1C"),
                    Color.parseColor("#020B07")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        private val particlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#7FE3B5")
        }
        private val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#7FE3B5")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        private val archetypeIconPaint = Paint().apply {
            isAntiAlias = true
            textSize = 190f
            textAlign = Paint.Align.CENTER
        }
        private val archetypeTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 86f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        private val archetypeLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#9AE6C4")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.18f
        }
        private val scorePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        private val labelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#9AE6C4")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        private val waveArcPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#7FE3B5")
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        private val tagBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#267FE3B5")
        }
        private val freqTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#7FE3B5")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        private val waveLinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#52E09B")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        private val footerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#7FE3B5")
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        private val footerSubPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#88C3EBD9")
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        private val tagRect = RectF(120f, 1180f, WIDTH - 120f, 1300f)

        fun drawFrame(
            canvas: Canvas,
            frameIndex: Int,
            score: Int,
            archetype: PlantParentArchetype,
            frequencyHz: Float,
            sceneLabel: String,
            particles: List<Particle>,
            random: java.util.Random
        ) {
            val i = frameIndex

            canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

            // Floating oxygen & bio-energy particles
            particles.forEach { p ->
                p.y -= p.speedY
                if (p.y < -30f) {
                    p.y = HEIGHT.toFloat() + 30f
                    p.x = random.nextFloat() * WIDTH
                }
                particlePaint.alpha = p.alpha
                canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
            }

            canvas.drawText("FLORAFLOW • 15s AMBIENT REEL 🌿", 80f, 150f, titlePaint)

            // Pulsing aura, breathing with the beat frequency
            val auraCenterY = 700f
            val pulseRadius = 300f + (sin(i * 0.2) * 28).toFloat()
            waveArcPaint.alpha = (190 + (sin(i * 0.15) * 50)).toInt().coerceIn(0, 255)
            canvas.drawCircle(WIDTH / 2f, auraCenterY, pulseRadius, waveArcPaint)
            waveArcPaint.alpha = (90 + (sin(i * 0.25) * 40)).toInt().coerceIn(0, 255)
            canvas.drawCircle(WIDTH / 2f, auraCenterY, pulseRadius + 45f, waveArcPaint)

            // Archetype is the hero — it's the line people repeat when they share.
            canvas.drawText(archetype.icon, WIDTH / 2f, auraCenterY - 20f, archetypeIconPaint)
            canvas.drawText(archetype.title, WIDTH / 2f, auraCenterY + 110f, archetypeTitlePaint)
            canvas.drawText("PLANT PARENT ARCHETYPE", WIDTH / 2f, auraCenterY + 170f, archetypeLabelPaint)

            // Score demoted to supporting evidence
            canvas.drawText("$score%", WIDTH / 2f, 1080f, scorePaint)
            canvas.drawText("BIOPHILIC VITALITY SCORE", WIDTH / 2f, 1125f, labelPaint)

            canvas.drawRoundRect(tagRect, 60f, 60f, tagBgPaint)
            canvas.drawText("🎵 $sceneLabel", WIDTH / 2f, 1255f, freqTextPaint)

            canvas.drawText(
                "${frequencyHz.toInt()}Hz Binaural Flow • headphones on",
                WIDTH / 2f, 1390f, freqTextPaint
            )

            // Animated waveform
            val waveCenterY = 1520f
            var prevX = 140f
            var prevY = waveCenterY
            for (x in 140..940 step 15) {
                val angle = (x * 0.03f) + (i * 0.2f)
                val y = waveCenterY + (sin(angle) * 35f).toFloat()
                canvas.drawLine(prevX, prevY, x.toFloat(), y, waveLinePaint)
                prevX = x.toFloat()
                prevY = y
            }

            // Watermark: survives a re-screenshot and names where to go.
            canvas.drawText("@floraflow 🌿", WIDTH / 2f, HEIGHT - 150f, footerPaint)
            canvas.drawText(
                ShareLinks.displayLink(shareCode),
                WIDTH / 2f, HEIGHT - 100f, footerSubPaint
            )
        }
    }
}
