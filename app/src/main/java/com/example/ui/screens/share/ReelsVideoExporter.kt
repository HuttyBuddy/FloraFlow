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

    fun generate15SecondReelVideo(
        context: Context,
        score: Int = 88,
        archetype: PlantParentArchetype = PlantParentArchetype.JUNGLE_MAXIMALIST,
        frequencyHz: Float = 10.0f,
        onProgress: (Float) -> Unit = {},
        onComplete: (File) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val width = 720
        val height = 1280
        val fps = 30
        val durationSec = 15
        val totalFrames = fps * durationSec
        val bitRate = 4_000_000 // 4 Mbps for crisp mobile playback

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

            // Paint objects
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.parseColor("#061A12"), Color.parseColor("#0B2B1D"), Color.parseColor("#030D08")),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#7FE3B5")
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.12f
            }
            val scorePaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 80f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val labelPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#9AE6C4")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val wavePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#7FE3B5")
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            val footerPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#7FE3B5")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until totalFrames) {
                val canvas = surface.lockCanvas(null)
                
                // 1. Draw Background
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // 2. Draw Header
                canvas.drawText("FLORAFLOW • 15s AMBIENT REEL 🌿", 50f, 100f, titlePaint)

                // 3. Draw Pulsing Wave Audio Circle
                val progress = i.toFloat() / totalFrames
                val pulseRadius = 160f + (sin(i * 0.25) * 20).toFloat()
                
                wavePaint.alpha = (180 + (sin(i * 0.2) * 50)).toInt()
                canvas.drawCircle(width / 2f, 420f, pulseRadius, wavePaint)
                canvas.drawCircle(width / 2f, 420f, pulseRadius + 30f, wavePaint.apply { alpha = 80 })

                // 4. Score Display
                canvas.drawText("$score%", width / 2f, 445f, scorePaint)
                canvas.drawText("BIOPHILIC VITALITY SCORE", width / 2f, 620f, labelPaint)

                // 5. Archetype Pill
                val tagBgPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#267FE3B5")
                }
                val tagRect = RectF(90f, 680f, width - 90f, 760f)
                canvas.drawRoundRect(tagRect, 40f, 40f, tagBgPaint)

                val tagTextPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#E0F7ED")
                    textSize = 30f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("${archetype.icon} ${archetype.title}", width / 2f, 732f, tagTextPaint)

                // 6. Soundscape Wave Info
                val freqTextPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#7FE3B5")
                    textSize = 28f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("🎵 Active Soundscape: ${frequencyHz}Hz Binaural Flow", width / 2f, 850f, freqTextPaint)

                // 7. Footer
                canvas.drawText("Made with FloraFlow AI • floraflow.app", width / 2f, height - 80f, footerPaint)

                surface.unlockCanvasAndPost(canvas)

                // Drain Encoder Output
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

                onProgress(progress)
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
