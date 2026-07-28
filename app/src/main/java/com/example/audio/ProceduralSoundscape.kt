package com.example.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * The generative eco-acoustic engine, shared by every surface that needs FloraFlow's sound.
 *
 * [SoundscapeService][com.example.ui.screens.restoration.SoundscapeService] renders these
 * generators in real time for playback; [OfflineSoundscapeRenderer] renders the exact same
 * DSP faster-than-real-time into a PCM buffer so the shareable Reels video carries the real
 * soundscape rather than silence. Keeping one implementation means a clip a user posts to
 * TikTok sounds like the session they actually listened to.
 */
object Soundscape {

    const val SAMPLE_RATE = 44100
    const val TWO_PI = 2.0 * PI
    const val PHASE_WRAP = 2.0 * PI * 1024.0

    const val SCENE_BREEZE = 0
    const val SCENE_RAIN = 1
    const val SCENE_OCEAN = 2

    const val CROSSFADE_SECONDS = 2.5f

    /** Binaural carrier peaks at ~24% of full scale at max slider — present but never harsh. */
    const val BINAURAL_PEAK = 0.24f

    const val PARAM_SMOOTH = 0.0008f
    const val FREQ_SMOOTH = 0.00005f

    fun createGenerator(sceneId: Int): AmbientGenerator = when (sceneId) {
        SCENE_BREEZE -> BreezeGenerator()
        SCENE_OCEAN -> OceanGenerator()
        else -> RainGenerator()
    }

    fun sceneLabel(sceneId: Int): String = when (sceneId) {
        SCENE_BREEZE -> "Forest Breeze & Chimes"
        SCENE_OCEAN -> "Ocean Waves"
        else -> "Gentle Rainfall"
    }

    /** Maps a preset track name to its scene, or null when the track uses custom frequencies. */
    fun sceneForTrack(name: String): Int? = when {
        name.contains("Alpha") -> SCENE_BREEZE
        name.contains("Theta") -> SCENE_RAIN
        name.contains("Delta") -> SCENE_OCEAN
        else -> null
    }
}

/**
 * One playing ambient scene. More than one is active only while crossfading.
 *
 * [initialGain] lets an offline render start at full level instead of ramping in over
 * [Soundscape.CROSSFADE_SECONDS] — a 15-second clip can't spend 2.5s fading up.
 */
class SceneVoice(
    val sceneId: Int,
    val generator: AmbientGenerator,
    initialGain: Float = 0f
) {
    var gain = initialGain
    var gainTarget = 1f
    val gainStep = 1f / (Soundscape.CROSSFADE_SECONDS * Soundscape.SAMPLE_RATE)
    fun isFinished() = gainTarget == 0f && gain <= 0f
}

abstract class AmbientGenerator {
    abstract fun render(bus: FloatArray, frames: Int, voice: SceneVoice)
}

/** Paul Kellet economy pink-noise filter — the basis of natural wind and rain beds. */
private class PinkNoise(seed: Int) {
    private val rnd = Random(seed)
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f
    fun next(): Float {
        val w = rnd.nextFloat() * 2f - 1f
        b0 = 0.99765f * b0 + w * 0.0990460f
        b1 = 0.96300f * b1 + w * 0.2965164f
        b2 = 0.57000f * b2 + w * 1.0526913f
        return (b0 + b1 + b2 + w * 0.1848f) * 0.2f
    }
}

/**
 * Forest Breeze & Chimes (Alpha Focus): dark pink-noise wind swelling on two slow
 * detuned cycles, with synthesized pentatonic wind chimes struck at random intervals.
 */
class BreezeGenerator(seed: Int = System.nanoTime().toInt()) : AmbientGenerator() {
    private val rnd = Random(seed)
    private val pinkL = PinkNoise(rnd.nextInt())
    private val pinkR = PinkNoise(rnd.nextInt())
    private var lpL = 0f
    private var lpR = 0f
    private var lfoA = rnd.nextDouble() * Soundscape.TWO_PI
    private var lfoB = rnd.nextDouble() * Soundscape.TWO_PI

    private val chimes = Array(6) { ChimeVoice() }
    private var samplesToNextChime = (Soundscape.SAMPLE_RATE * 2.5).toInt()

    // A-major pentatonic — always consonant regardless of strike order.
    private val notes = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.0, 1046.5)

    override fun render(bus: FloatArray, frames: Int, voice: SceneVoice) {
        for (i in 0 until frames) {
            lfoA += Soundscape.TWO_PI * 0.045 / Soundscape.SAMPLE_RATE
            lfoB += Soundscape.TWO_PI * 0.011 / Soundscape.SAMPLE_RATE
            val env = (0.45f + 0.28f * sin(lfoA).toFloat() + 0.22f * sin(lfoB).toFloat())
                .coerceIn(0.08f, 1f)

            lpL += 0.10f * (pinkL.next() - lpL)
            lpR += 0.10f * (pinkR.next() - lpR)
            var l = lpL * env * WIND_GAIN
            var r = lpR * env * WIND_GAIN

            if (--samplesToNextChime <= 0) {
                chimes.firstOrNull { !it.active }?.trigger(notes[rnd.nextInt(notes.size)], rnd)
                samplesToNextChime = ((3.0 + rnd.nextDouble() * 7.0) * Soundscape.SAMPLE_RATE).toInt()
            }
            for (chime in chimes) {
                if (!chime.active) continue
                val s = chime.nextSample()
                l += s * chime.panL
                r += s * chime.panR
            }

            voice.gain += (voice.gainTarget - voice.gain).coerceIn(-voice.gainStep, voice.gainStep)
            bus[2 * i] += l * voice.gain
            bus[2 * i + 1] += r * voice.gain
        }
    }

    /**
     * Strikes a chime immediately instead of waiting out the random interval. A 15-second
     * clip would otherwise often contain no chime at all — and the chimes are the part of
     * this scene people recognize.
     */
    fun strikeChimeNow() {
        samplesToNextChime = 1
    }

    companion object {
        private const val WIND_GAIN = 0.55f
    }
}

/** A struck chime tine: three inharmonic partials with independent exponential decay. */
private class ChimeVoice {
    var active = false
    var panL = 0.7f
    var panR = 0.7f
    private val ratios = doubleArrayOf(1.0, 2.76, 5.40)
    private val phases = DoubleArray(3)
    private val increments = DoubleArray(3)
    private val amps = FloatArray(3)
    private val decays = FloatArray(3)

    fun trigger(freq: Double, rnd: Random) {
        val startAmps = floatArrayOf(0.10f, 0.05f, 0.018f)
        val taus = floatArrayOf(3.8f, 1.4f, 0.5f)
        for (p in 0..2) {
            phases[p] = 0.0
            increments[p] = Soundscape.TWO_PI * freq * ratios[p] / Soundscape.SAMPLE_RATE
            amps[p] = startAmps[p]
            decays[p] = exp(-1f / (taus[p] * Soundscape.SAMPLE_RATE))
        }
        // Constant-power random pan so each strike hangs somewhere in the stereo field.
        val pan = rnd.nextFloat()
        panL = sqrt(1f - pan)
        panR = sqrt(pan)
        active = true
    }

    fun nextSample(): Float {
        var s = 0f
        for (p in 0..2) {
            s += sin(phases[p]).toFloat() * amps[p]
            phases[p] += increments[p]
            amps[p] *= decays[p]
        }
        if (amps[0] < 1e-4f) active = false
        return s
    }
}

/**
 * Gentle Rainfall (Theta Meditate): band-limited noise bed with a slowly breathing
 * intensity, plus quiet high droplet plinks scattered across the stereo field.
 */
class RainGenerator(seed: Int = System.nanoTime().toInt()) : AmbientGenerator() {
    private val rnd = Random(seed)
    private var hpL = 0f
    private var hpR = 0f
    private var lpL = 0f
    private var lpR = 0f
    private var lfo = rnd.nextDouble() * Soundscape.TWO_PI

    private val drops = Array(8) { DropletVoice() }
    private var samplesToNextDrop = Soundscape.SAMPLE_RATE / 2

    override fun render(bus: FloatArray, frames: Int, voice: SceneVoice) {
        for (i in 0 until frames) {
            lfo += Soundscape.TWO_PI * 0.03 / Soundscape.SAMPLE_RATE
            val intensity = 0.85f + 0.15f * sin(lfo).toFloat()

            var wL = rnd.nextFloat() * 2f - 1f
            var wR = rnd.nextFloat() * 2f - 1f
            // High-pass out the rumble, then low-pass to the soft hiss band of real rain.
            hpL += 0.008f * (wL - hpL); wL -= hpL
            hpR += 0.008f * (wR - hpR); wR -= hpR
            lpL += 0.22f * (wL - lpL)
            lpR += 0.22f * (wR - lpR)
            var l = lpL * RAIN_GAIN * intensity
            var r = lpR * RAIN_GAIN * intensity

            if (--samplesToNextDrop <= 0) {
                drops.firstOrNull { !it.active }?.trigger(rnd)
                samplesToNextDrop = ((0.25 + rnd.nextDouble() * 1.1) * Soundscape.SAMPLE_RATE).toInt()
            }
            for (drop in drops) {
                if (!drop.active) continue
                val s = drop.nextSample()
                l += s * drop.panL
                r += s * drop.panR
            }

            voice.gain += (voice.gainTarget - voice.gain).coerceIn(-voice.gainStep, voice.gainStep)
            bus[2 * i] += l * voice.gain
            bus[2 * i + 1] += r * voice.gain
        }
    }

    companion object {
        private const val RAIN_GAIN = 0.5f
    }
}

/** A single raindrop hitting a leaf: a short, fast-decaying high sine ping. */
private class DropletVoice {
    var active = false
    var panL = 0.7f
    var panR = 0.7f
    private var phase = 0.0
    private var increment = 0.0
    private var amp = 0f
    private var decay = 0f

    fun trigger(rnd: Random) {
        val freq = 1600.0 + rnd.nextDouble() * 1900.0
        phase = 0.0
        increment = Soundscape.TWO_PI * freq / Soundscape.SAMPLE_RATE
        amp = 0.03f + rnd.nextFloat() * 0.03f
        decay = exp(-1f / (0.045f * Soundscape.SAMPLE_RATE))
        val pan = rnd.nextFloat()
        panL = sqrt(1f - pan)
        panR = sqrt(pan)
        active = true
    }

    fun nextSample(): Float {
        val s = sin(phase).toFloat() * amp
        phase += increment
        amp *= decay
        if (amp < 1e-4f) active = false
        return s
    }
}

/**
 * Ocean Waves (Delta Sleep): deep brown-noise swells on two overlapping wave periods,
 * with a bright "wash" of foam that only appears as each wave crests.
 */
class OceanGenerator(seed: Int = System.nanoTime().toInt()) : AmbientGenerator() {
    private val rnd = Random(seed)
    private var brownL = 0f
    private var brownR = 0f
    private var lpL = 0f
    private var lpR = 0f
    private var washLp = 0f
    private var wave1 = rnd.nextDouble() * Soundscape.TWO_PI
    private var wave2 = rnd.nextDouble() * Soundscape.TWO_PI

    override fun render(bus: FloatArray, frames: Int, voice: SceneVoice) {
        for (i in 0 until frames) {
            wave1 += Soundscape.TWO_PI / (12.0 * Soundscape.SAMPLE_RATE)
            wave2 += Soundscape.TWO_PI / (19.0 * Soundscape.SAMPLE_RATE)
            val s1 = max(0.0, sin(wave1)).pow(2.0).toFloat()
            val s2 = max(0.0, sin(wave2)).pow(2.0).toFloat()
            val swell = 0.22f + 0.78f * (s1 * 0.6f + s2 * 0.4f)

            brownL = (brownL + 0.02f * (rnd.nextFloat() * 2f - 1f)) * 0.998f
            brownR = (brownR + 0.02f * (rnd.nextFloat() * 2f - 1f)) * 0.998f
            lpL += 0.15f * (brownL - lpL)
            lpR += 0.15f * (brownR - lpR)

            // Foam wash: bright noise that rises with the square of the swell (crest only).
            washLp += 0.3f * ((rnd.nextFloat() * 2f - 1f) - washLp)
            val wash = washLp * swell * swell * 0.10f

            val l = lpL * OCEAN_GAIN * swell + wash
            val r = lpR * OCEAN_GAIN * swell + wash

            voice.gain += (voice.gainTarget - voice.gain).coerceIn(-voice.gainStep, voice.gainStep)
            bus[2 * i] += l * voice.gain
            bus[2 * i + 1] += r * voice.gain
        }
    }

    companion object {
        private const val OCEAN_GAIN = 1.1f
    }
}

/**
 * Renders the soundscape offline — as fast as the CPU allows — into 16-bit stereo PCM.
 *
 * Used by the Reels exporter so the exported MP4 carries the user's actual binaural
 * frequency and ambient scene. Rendering 15 seconds takes well under a second.
 */
object OfflineSoundscapeRenderer {

    /** Frames rendered per inner-loop pass. Only affects locality, not the output. */
    private const val BLOCK_FRAMES = 2048

    /**
     * @param sceneId one of [Soundscape.SCENE_BREEZE] / [Soundscape.SCENE_RAIN] / [Soundscape.SCENE_OCEAN]
     * @param baseFreqHz binaural carrier frequency for the left ear
     * @param beatFreqHz the beat frequency — the right ear runs at [baseFreqHz] + this
     * @param durationSeconds length of the rendered buffer
     * @param fadeSeconds fade applied to both head and tail so the clip loops without a click
     * @return interleaved stereo PCM 16-bit at [Soundscape.SAMPLE_RATE]
     */
    fun renderPcm16(
        sceneId: Int,
        baseFreqHz: Float,
        beatFreqHz: Float,
        durationSeconds: Float,
        ambientVolume: Float = 0.55f,
        binauralVolume: Float = 0.45f,
        fadeSeconds: Float = 0.6f,
        seed: Int = 1337
    ): ShortArray {
        val sampleRate = Soundscape.SAMPLE_RATE
        val totalFrames = (durationSeconds * sampleRate).toInt().coerceAtLeast(1)
        val out = ShortArray(totalFrames * 2)

        val generator = when (sceneId) {
            Soundscape.SCENE_BREEZE -> BreezeGenerator(seed).also {
                // Short clip: make sure at least one chime lands early.
                it.strikeChimeNow()
            }
            Soundscape.SCENE_OCEAN -> OceanGenerator(seed)
            else -> RainGenerator(seed)
        }
        // Start at full level — there is no crossfade to perform in an offline render.
        val voice = SceneVoice(sceneId, generator, initialGain = 1f)

        val fadeFrames = (fadeSeconds * sampleRate).toInt().coerceIn(1, totalFrames / 2)

        val sceneBus = FloatArray(BLOCK_FRAMES * 2)
        var phaseL = 0.0
        var phaseR = 0.0
        var frame = 0

        while (frame < totalFrames) {
            val frames = minOf(BLOCK_FRAMES, totalFrames - frame)
            java.util.Arrays.fill(sceneBus, 0f)
            generator.render(sceneBus, frames, voice)

            for (i in 0 until frames) {
                val globalFrame = frame + i

                phaseL += Soundscape.TWO_PI * baseFreqHz / sampleRate
                phaseR += Soundscape.TWO_PI * (baseFreqHz + beatFreqHz) / sampleRate
                val binAmp = binauralVolume * Soundscape.BINAURAL_PEAK

                // Equal-power fade in at the head and out at the tail.
                val env = when {
                    globalFrame < fadeFrames -> globalFrame.toFloat() / fadeFrames
                    globalFrame >= totalFrames - fadeFrames ->
                        (totalFrames - globalFrame).toFloat() / fadeFrames
                    else -> 1f
                }.coerceIn(0f, 1f)

                var l = sceneBus[2 * i] * ambientVolume + sin(phaseL).toFloat() * binAmp
                var r = sceneBus[2 * i + 1] * ambientVolume + sin(phaseR).toFloat() * binAmp

                // Same soft limiter as the live bus so exported audio matches playback.
                l = tanh(l * env)
                r = tanh(r * env)

                out[2 * globalFrame] = (l.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                out[2 * globalFrame + 1] = (r.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            }

            if (phaseL > Soundscape.PHASE_WRAP) phaseL -= Soundscape.PHASE_WRAP
            if (phaseR > Soundscape.PHASE_WRAP) phaseR -= Soundscape.PHASE_WRAP

            frame += frames
        }

        return out
    }
}
