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
import android.view.Surface
import androidx.core.content.FileProvider
import com.example.data.model.PlantParentArchetype
import java.io.File
import kotlin.math.sin

object ReelsVideoExporter {

    data class Particle(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speedY: Float,
        var alpha: Int
    )

    fun generate15SecondReelVideo(
        context: Context,
        score: Int = 88,
        archetype: PlantParentArchetype = PlantParentArchetype.JUNGLE_MAXIMALIST,
        frequencyHz: Float = 10.0f,
        onProgress: (Float) -> Unit = {},
        onComplete: (File) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val width = 1080
        val height = 1920
        val fps = 30
        val durationSec = 15
        val totalFrames = fps * durationSec
        val bitRate = 6_000_000 // 6 Mbps HD bit rate for crisp TikTok / Reels upload

        val outputFile = File(context.cacheDir, "floraflow_ambient_reel.mp4")
        if (outputFile.exists()) outputFile.delete()

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface: Surface = encoder.createInputSurface()
            encoder.start()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()

            // Initialize Floating Bio-Energy Particles
            val random = java.util.Random(42)
            val particles = List(35) {
                Particle(
                    x = random.nextFloat() * width,
                    y = random.nextFloat() * height,
                    radius = 8f + random.nextFloat() * 16f,
                    speedY = 1.5f + random.nextFloat() * 3.5f,
                    alpha = 60 + random.nextInt(120)
                )
            }

            // Paint objects setup
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        Color.parseColor("#04140D"),
                        Color.parseColor("#092B1C"),
                        Color.parseColor("#020B07")
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            val particlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#7FE3B5")
            }
            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#7FE3B5")
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.15f
            }
            val scorePaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 120f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val labelPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#9AE6C4")
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.1f
            }
            val waveArcPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#7FE3B5")
                style = Paint.Style.STROKE
                strokeWidth = 10f
            }
            val footerPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#7FE3B5")
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until totalFrames) {
                val canvas = surface.lockCanvas(null)

                // 1. Draw Deep Emerald Background Gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // 2. Draw Floating Oxygen & Bio-Energy Particles
                particles.forEach { p ->
                    p.y -= p.speedY
                    if (p.y < -30f) {
                        p.y = height.toFloat() + 30f
                        p.x = random.nextFloat() * width
                    }
                    particlePaint.alpha = p.alpha
                    canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
                }

                // 3. Header Branding
                canvas.drawText("FLORAFLOW • 15s AMBIENT REEL 🌿", 80f, 150f, titlePaint)

                // 4. Pulsing Wave Audio Aura Circles
                val pulseRadius = 240f + (sin(i * 0.2) * 28).toFloat()
                val circleY = 620f

                waveArcPaint.alpha = (190 + (sin(i * 0.15) * 50)).toInt()
                canvas.drawCircle(width / 2f, circleY, pulseRadius, waveArcPaint)
                
                waveArcPaint.alpha = (90 + (sin(i * 0.25) * 40)).toInt()
                canvas.drawCircle(width / 2f, circleY, pulseRadius + 45f, waveArcPaint)

                // 5. Biophilic Vitality Score Display inside Pulsing Aura
                canvas.drawText("$score%", width / 2f, circleY + 40f, scorePaint)
                canvas.drawText("BIOPHILIC VITALITY SCORE", width / 2f, circleY + 300f, labelPaint)

                // 6. Plant Parent Archetype Pill Container
                val tagBgPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#267FE3B5")
                }
                val tagRect = RectF(120f, 1060f, width - 120f, 1180f)
                canvas.drawRoundRect(tagRect, 60f, 60f, tagBgPaint)

                val tagTextPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#E0F7ED")
                    textSize = 42f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("${archetype.icon} ${archetype.title}", width / 2f, 1135f, tagTextPaint)

                // 7. Active Soundscape Frequency Badge
                val freqTextPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#7FE3B5")
                    textSize = 38f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("🎵 Active Soundscape: ${frequencyHz}Hz Binaural Flow", width / 2f, 1310f, freqTextPaint)

                // 8. Animated Sine Wave Audio Waveform
                val waveLinePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#52E09B")
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                }
                val waveCenterY = 1480f
                var prevX = 140f
                var prevY = waveCenterY
                for (x in 140..940 step 15) {
                    val angle = (x * 0.03f) + (i * 0.2f)
                    val y = waveCenterY + (sin(angle) * 35f).toFloat()
                    canvas.drawLine(prevX, prevY, x.toFloat(), y, waveLinePaint)
                    prevX = x.toFloat()
                    prevY = y
                }

                // 9. Footer Watermark
                canvas.drawText("Made with FloraFlow AI • floraflow.app 🌿", width / 2f, height - 120f, footerPaint)

                surface.unlockCanvasAndPost(canvas)

                // Drain Encoder Output Buffers
                var encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                while (encoderStatus >= 0) {
                    val encodedData = encoder.getOutputBuffer(encoderStatus)
                    if (encodedData != null) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0) {
                            if (!muxerStarted) {
                                trackIndex = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                            bufferInfo.presentationTimeUs = (i * 1_000_000L) / fps
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(encoderStatus, false)
                    }
                    encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 0)
                }

                onProgress(i.toFloat() / totalFrames)
            }

            encoder.signalEndOfInputStream()
            encoder.stop()
            encoder.release()

            if (muxerStarted) {
                muxer.stop()
                muxer.release()
            }

            onComplete(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e)
        }
    }

    fun shareReelVideo(context: Context, videoFile: File) {
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
                    "My 15s Ambient Soundscape Reel 🌿✨ Check out FloraFlow AI for plant sanctuary focus vibes!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Ambient Reel Video")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
