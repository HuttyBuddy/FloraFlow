package com.example.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Guards the audio that ships inside shared Reels.
 *
 * The exported clip used to have no audio track at all. These tests assert the renderer
 * produces real, non-silent, correctly-shaped stereo PCM, since a silent export is both a
 * broken promise and suppressed by short-form feed ranking.
 */
class OfflineSoundscapeRendererTest {

    private val sampleRate = Soundscape.SAMPLE_RATE

    @Test
    fun render_producesCorrectlySizedStereoBuffer() {
        val seconds = 2f
        val pcm = OfflineSoundscapeRenderer.renderPcm16(
            sceneId = Soundscape.SCENE_RAIN,
            baseFreqHz = 200f,
            beatFreqHz = 6f,
            durationSeconds = seconds
        )
        // Interleaved stereo: two samples per frame.
        assertEquals((seconds * sampleRate).toInt() * 2, pcm.size)
    }

    @Test
    fun render_isNotSilent() {
        val pcm = OfflineSoundscapeRenderer.renderPcm16(
            sceneId = Soundscape.SCENE_BREEZE,
            baseFreqHz = 200f,
            beatFreqHz = 10f,
            durationSeconds = 2f
        )
        val peak = pcm.maxOf { abs(it.toInt()) }
        assertTrue("expected audible signal, peak was $peak", peak > 1000)
    }

    @Test
    fun render_neverClips() {
        val pcm = OfflineSoundscapeRenderer.renderPcm16(
            sceneId = Soundscape.SCENE_OCEAN,
            baseFreqHz = 200f,
            beatFreqHz = 2f,
            durationSeconds = 2f,
            ambientVolume = 1f,
            binauralVolume = 1f
        )
        // The tanh limiter must hold even with both layers at full level.
        val peak = pcm.maxOf { abs(it.toInt()) }
        assertTrue("output clipped at $peak", peak < 32767)
    }

    /** Head and tail must fade, or a looping clip clicks on every repeat. */
    @Test
    fun render_fadesInAndOut() {
        val pcm = OfflineSoundscapeRenderer.renderPcm16(
            sceneId = Soundscape.SCENE_RAIN,
            baseFreqHz = 200f,
            beatFreqHz = 6f,
            durationSeconds = 2f,
            fadeSeconds = 0.5f
        )
        assertEquals(0, pcm.first().toInt())
        assertTrue("tail should be near silence, was ${pcm.last()}", abs(pcm.last().toInt()) < 500)

        // The middle of the clip should be well above the faded edges.
        val midPeak = (pcm.size / 2 until pcm.size / 2 + sampleRate / 10)
            .maxOf { abs(pcm[it].toInt()) }
        assertTrue("expected louder middle, got $midPeak", midPeak > 1000)
    }

    /** The two ears must differ — that difference *is* the binaural beat. */
    @Test
    fun render_leftAndRightChannelsDiffer() {
        val pcm = OfflineSoundscapeRenderer.renderPcm16(
            sceneId = Soundscape.SCENE_RAIN,
            baseFreqHz = 200f,
            beatFreqHz = 6f,
            durationSeconds = 2f,
            ambientVolume = 0f,
            binauralVolume = 1f
        )
        var differing = 0
        for (frame in 0 until pcm.size / 2) {
            if (pcm[2 * frame] != pcm[2 * frame + 1]) differing++
        }
        assertTrue("channels were near-identical ($differing differing)", differing > pcm.size / 8)
    }

    @Test
    fun render_isDeterministicForAGivenSeed() {
        fun render() = OfflineSoundscapeRenderer.renderPcm16(
            sceneId = Soundscape.SCENE_BREEZE,
            baseFreqHz = 200f,
            beatFreqHz = 10f,
            durationSeconds = 1f,
            seed = 4242
        )
        assertTrue(render().contentEquals(render()))
    }

    @Test
    fun render_handlesSubSecondDurations() {
        val pcm = OfflineSoundscapeRenderer.renderPcm16(
            sceneId = Soundscape.SCENE_RAIN,
            baseFreqHz = 200f,
            beatFreqHz = 6f,
            durationSeconds = 0.25f
        )
        assertEquals((0.25f * sampleRate).toInt() * 2, pcm.size)
    }
}
