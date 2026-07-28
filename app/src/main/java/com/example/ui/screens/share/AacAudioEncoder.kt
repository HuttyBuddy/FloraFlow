package com.example.ui.screens.share

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encodes 16-bit PCM into AAC-LC packets held in memory.
 *
 * The Reels exporter needs the audio track's [MediaFormat] *before* it can start the
 * MediaMuxer, and MediaMuxer only reports a track's format once its encoder has produced
 * output. Encoding the whole audio stream up front (a 15-second clip is a few hundred KB)
 * sidesteps that ordering problem, and lets the video loop interleave audio packets by
 * timestamp as it goes.
 */
object AacAudioEncoder {

    private const val TIMEOUT_US = 10_000L

    /** One encoded AAC access unit, ready to hand to MediaMuxer.writeSampleData. */
    class Packet(val data: ByteArray, val presentationTimeUs: Long, val flags: Int)

    class EncodedAudio(val format: MediaFormat, val packets: List<Packet>)

    /**
     * @param pcm interleaved signed 16-bit samples; stereo means L,R,L,R…
     * @return the encoded stream plus the exact output format the muxer needs
     * @throws java.io.IOException if no AAC encoder is available on the device
     */
    fun encode(
        pcm: ShortArray,
        sampleRate: Int,
        channelCount: Int,
        bitRate: Int = 128_000
    ): EncodedAudio {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channelCount
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val packets = ArrayList<Packet>()
        var outputFormat: MediaFormat? = null

        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            // PCM as little-endian bytes — the byte order every AAC encoder expects.
            val pcmBytes = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            pcmBytes.asShortBuffer().put(pcm)
            pcmBytes.rewind()

            val bytesPerFrame = 2 * channelCount
            val bufferInfo = MediaCodec.BufferInfo()
            var framesSubmitted = 0L
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        inputBuffer.clear()

                        val remaining = pcmBytes.remaining()
                        // Never split a sample frame across two input buffers.
                        val chunk = minOf(inputBuffer.capacity(), remaining) / bytesPerFrame * bytesPerFrame

                        if (chunk <= 0) {
                            codec.queueInputBuffer(
                                inputIndex, 0, 0,
                                framesSubmitted * 1_000_000L / sampleRate,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            val slice = ByteArray(chunk)
                            pcmBytes.get(slice)
                            inputBuffer.put(slice)
                            codec.queueInputBuffer(
                                inputIndex, 0, chunk,
                                framesSubmitted * 1_000_000L / sampleRate,
                                0
                            )
                            framesSubmitted += chunk / bytesPerFrame
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outputFormat = codec.outputFormat
                    else -> if (outputIndex >= 0) {
                        val encoded = codec.getOutputBuffer(outputIndex)
                        val isCodecConfig =
                            bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0

                        // Codec-config bytes belong in the track format, not the sample stream.
                        if (encoded != null && bufferInfo.size > 0 && !isCodecConfig) {
                            val bytes = ByteArray(bufferInfo.size)
                            encoded.position(bufferInfo.offset)
                            encoded.get(bytes, 0, bufferInfo.size)
                            packets.add(
                                Packet(
                                    data = bytes,
                                    presentationTimeUs = bufferInfo.presentationTimeUs,
                                    flags = bufferInfo.flags and
                                        MediaCodec.BUFFER_FLAG_CODEC_CONFIG.inv()
                                )
                            )
                        }
                        codec.releaseOutputBuffer(outputIndex, false)

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }
        } finally {
            try {
                codec.stop()
            } catch (_: Exception) {
                // Already stopped or never started — nothing to recover.
            }
            codec.release()
        }

        // Some encoders never emit INFO_OUTPUT_FORMAT_CHANGED; the configured format still
        // carries everything MediaMuxer needs for AAC-LC.
        return EncodedAudio(outputFormat ?: format, packets)
    }
}
