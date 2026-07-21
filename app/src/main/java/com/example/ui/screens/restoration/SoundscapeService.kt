package com.example.ui.screens.restoration

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Generative eco-acoustic engine.
 *
 * Instead of looping a single audio file, every layer is synthesized in real time
 * and mixed on a single audio render loop:
 *  - Three seamless nature scenes (never loop, never repeat):
 *      Forest Breeze & Chimes / Gentle Rainfall / Ocean Waves
 *  - Binaural beat pair at a comfortable level with per-sample parameter smoothing
 *  - Equal-gain crossfades between scenes, soft fade-in/out on play/pause
 *  - Soft limiter on the master bus so layers never clip
 *  - Optional sleep timer that ends the session with a long gentle fade
 */
class SoundscapeService : Service() {

    private val binder = SoundscapeBinder()

    private var audioTrack: AudioTrack? = null

    // Playback state
    private var isPlaying = false
    private var currentTrackName = "Theta Meditate"

    // Parameter targets (render loop smooths toward these — no zipper noise)
    @Volatile private var ambientVolTarget = 0.5f
    @Volatile private var binauralVolTarget = 0.3f
    @Volatile private var baseFreqTarget = 200f
    @Volatile private var diffFreqTarget = 6f

    // Master fade (linear ramp, per-sample step set by play/pause/sleep-timer)
    @Volatile private var fadeTarget = 0f
    @Volatile private var fadeStep = 1f / (FADE_IN_SECONDS * SAMPLE_RATE)

    // Active ambient scenes (more than one only while crossfading)
    private val sceneLock = Any()
    private val sceneVoices = mutableListOf<SceneVoice>()
    private var currentSceneId = SCENE_RAIN

    private var renderJob: Job? = null
    private var sleepJob: Job? = null
    @Volatile private var sleepTimerEndTime: Long? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Set by the ViewModel so notification actions and the sleep timer keep the UI in sync. */
    @Volatile var onPlaybackChanged: ((Boolean) -> Unit)? = null

    inner class SoundscapeBinder : Binder() {
        fun getService(): SoundscapeService = this@SoundscapeService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PLAY" -> startSoundscape()
            "ACTION_PAUSE" -> pauseSoundscape()
            "ACTION_STOP" -> stopSelf()
        }
        return START_STICKY
    }

    fun startSoundscape() {
        if (isPlaying) return
        isPlaying = true

        fadeStep = 1f / (FADE_IN_SECONDS * SAMPLE_RATE)
        fadeTarget = 1f

        synchronized(sceneLock) {
            if (sceneVoices.none { it.sceneId == currentSceneId && it.gainTarget > 0f }) {
                sceneVoices.add(SceneVoice(currentSceneId, createGenerator(currentSceneId)))
            }
        }

        if (renderJob?.isActive != true) startEngine()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        onPlaybackChanged?.invoke(true)
    }

    fun pauseSoundscape() = pauseSoundscape(FADE_OUT_SECONDS)

    private fun pauseSoundscape(fadeSeconds: Float) {
        if (!isPlaying) return
        isPlaying = false

        cancelSleepTimerInternal()

        // Let the render loop fade to silence and tear the AudioTrack down itself — no pop.
        fadeStep = 1f / (fadeSeconds * SAMPLE_RATE)
        fadeTarget = 0f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
        onPlaybackChanged?.invoke(false)
    }

    fun setTrack(name: String, baseFreq: Float, diffFreq: Float) {
        currentTrackName = name
        baseFreqTarget = baseFreq
        diffFreqTarget = diffFreq

        val sceneId = sceneForTrack(name)
        if (sceneId != null && sceneId != currentSceneId) {
            currentSceneId = sceneId
            synchronized(sceneLock) {
                // Equal-gain crossfade: old scenes ramp out while the new one ramps in.
                sceneVoices.forEach { it.gainTarget = 0f }
                if (isPlaying || renderJob?.isActive == true) {
                    sceneVoices.add(SceneVoice(sceneId, createGenerator(sceneId)))
                }
            }
        }

        if (isPlaying) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }

    fun setFrequencies(baseFreq: Float, diffFreq: Float) {
        baseFreqTarget = baseFreq
        diffFreqTarget = diffFreq
    }

    fun setAmbientVolume(vol: Float) {
        ambientVolTarget = vol.coerceIn(0f, 1f)
    }

    fun setBinauralVolume(vol: Float) {
        binauralVolTarget = vol.coerceIn(0f, 1f)
    }

    /** Ends the session after [minutes] with a long gentle fade. Pass 0 to cancel. */
    fun setSleepTimer(minutes: Int) {
        cancelSleepTimerInternal()
        if (minutes <= 0) return
        sleepTimerEndTime = System.currentTimeMillis() + minutes * 60_000L
        sleepJob = serviceScope.launch {
            delay(max(0L, minutes * 60_000L - (SLEEP_FADE_SECONDS * 1000).toLong()))
            sleepTimerEndTime = null
            pauseSoundscape(SLEEP_FADE_SECONDS)
        }
    }

    private fun cancelSleepTimerInternal() {
        sleepJob?.cancel()
        sleepJob = null
        sleepTimerEndTime = null
    }

    fun isPlaying(): Boolean = isPlaying
    fun getCurrentTrackName(): String = currentTrackName
    fun getAmbientVolume(): Float = ambientVolTarget
    fun getBinauralVolume(): Float = binauralVolTarget
    fun getBaseFrequency(): Float = baseFreqTarget
    fun getDiffFrequency(): Float = diffFreqTarget
    fun getSleepTimerEndTime(): Long? = sleepTimerEndTime

    // ------------------------------------------------------------------
    // Audio engine
    // ------------------------------------------------------------------

    private fun startEngine() {
        renderJob?.cancel()

        val minBufferBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferBytes <= 0) return

        // Generous buffer so synthesis hiccups never become audible underruns.
        val bufferBytes = minBufferBytes * 4

        val track = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferBytes,
                    AudioTrack.MODE_STREAM
                )
            }
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                android.util.Log.e("SoundscapeService", "AudioTrack init failed: ${e.message}")
            }
            return
        }
        track.play()
        audioTrack = track

        renderJob = serviceScope.launch {
            val frames = FRAMES_PER_BUFFER
            val sceneBus = FloatArray(frames * 2)
            val mix = FloatArray(frames * 2)
            val pcm = ShortArray(frames * 2)

            var masterGain = 0f
            var ambientCur = ambientVolTarget
            var binauralCur = binauralVolTarget
            var baseCur = baseFreqTarget
            var diffCur = diffFreqTarget
            var phaseL = 0.0
            var phaseR = 0.0

            while (isActive) {
                java.util.Arrays.fill(sceneBus, 0f)

                synchronized(sceneLock) {
                    val iterator = sceneVoices.iterator()
                    while (iterator.hasNext()) {
                        val voice = iterator.next()
                        voice.generator.render(sceneBus, frames, voice)
                        if (voice.isFinished()) iterator.remove()
                    }
                }

                for (i in 0 until frames) {
                    // Smooth every user-controlled parameter — no clicks or zipper noise.
                    ambientCur += (ambientVolTarget - ambientCur) * PARAM_SMOOTH
                    binauralCur += (binauralVolTarget - binauralCur) * PARAM_SMOOTH
                    baseCur += (baseFreqTarget - baseCur) * FREQ_SMOOTH
                    diffCur += (diffFreqTarget - diffCur) * FREQ_SMOOTH

                    // Binaural pair: phase-continuous, comfortable level (well below full scale).
                    phaseL += TWO_PI * baseCur / SAMPLE_RATE
                    phaseR += TWO_PI * (baseCur + diffCur) / SAMPLE_RATE
                    val binAmp = binauralCur * BINAURAL_PEAK

                    val target = fadeTarget
                    val step = fadeStep
                    masterGain += (target - masterGain).coerceIn(-step, step)

                    var l = sceneBus[2 * i] * ambientCur + sin(phaseL).toFloat() * binAmp
                    var r = sceneBus[2 * i + 1] * ambientCur + sin(phaseR).toFloat() * binAmp

                    // Soft limiter: transparent at normal levels, rounds off any hot peaks.
                    l = tanh(l * masterGain)
                    r = tanh(r * masterGain)

                    mix[2 * i] = l
                    mix[2 * i + 1] = r
                }

                if (phaseL > PHASE_WRAP) phaseL -= PHASE_WRAP
                if (phaseR > PHASE_WRAP) phaseR -= PHASE_WRAP

                for (i in pcm.indices) {
                    pcm[i] = (mix[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                }

                val written = try {
                    audioTrack?.write(pcm, 0, pcm.size) ?: break
                } catch (e: Exception) {
                    break
                }
                if (written < 0) break

                // Fade-out complete: tear down cleanly from the audio thread.
                if (fadeTarget == 0f && masterGain < 0.0005f) break
            }

            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                // Ignore
            }
            audioTrack = null
        }
    }

    private fun sceneForTrack(name: String): Int? = when {
        name.contains("Alpha") -> SCENE_BREEZE
        name.contains("Theta") -> SCENE_RAIN
        name.contains("Delta") -> SCENE_OCEAN
        else -> null // Custom frequencies keep the current scene
    }

    private fun createGenerator(sceneId: Int): AmbientGenerator = when (sceneId) {
        SCENE_BREEZE -> BreezeGenerator()
        SCENE_OCEAN -> OceanGenerator()
        else -> RainGenerator()
    }

    private fun sceneLabel(sceneId: Int): String = when (sceneId) {
        SCENE_BREEZE -> "Forest Breeze & Chimes"
        SCENE_OCEAN -> "Ocean Waves"
        else -> "Gentle Rainfall"
    }

    // ------------------------------------------------------------------
    // Generative ambient scenes
    // ------------------------------------------------------------------

    private class SceneVoice(val sceneId: Int, val generator: AmbientGenerator) {
        var gain = 0f
        var gainTarget = 1f
        val gainStep = 1f / (CROSSFADE_SECONDS * SAMPLE_RATE)
        fun isFinished() = gainTarget == 0f && gain <= 0f
    }

    private abstract class AmbientGenerator {
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
    private class BreezeGenerator : AmbientGenerator() {
        private val rnd = Random(System.nanoTime().toInt())
        private val pinkL = PinkNoise(rnd.nextInt())
        private val pinkR = PinkNoise(rnd.nextInt())
        private var lpL = 0f
        private var lpR = 0f
        private var lfoA = rnd.nextDouble() * TWO_PI
        private var lfoB = rnd.nextDouble() * TWO_PI

        private val chimes = Array(6) { ChimeVoice() }
        private var samplesToNextChime = (SAMPLE_RATE * 2.5).toInt()

        // A-major pentatonic — always consonant regardless of strike order.
        private val notes = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.0, 1046.5)

        override fun render(bus: FloatArray, frames: Int, voice: SceneVoice) {
            for (i in 0 until frames) {
                lfoA += TWO_PI * 0.045 / SAMPLE_RATE
                lfoB += TWO_PI * 0.011 / SAMPLE_RATE
                val env = (0.45f + 0.28f * sin(lfoA).toFloat() + 0.22f * sin(lfoB).toFloat())
                    .coerceIn(0.08f, 1f)

                lpL += 0.10f * (pinkL.next() - lpL)
                lpR += 0.10f * (pinkR.next() - lpR)
                var l = lpL * env * WIND_GAIN
                var r = lpR * env * WIND_GAIN

                if (--samplesToNextChime <= 0) {
                    chimes.firstOrNull { !it.active }?.trigger(
                        notes[rnd.nextInt(notes.size)], rnd
                    )
                    samplesToNextChime = ((3.0 + rnd.nextDouble() * 7.0) * SAMPLE_RATE).toInt()
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
                increments[p] = TWO_PI * freq * ratios[p] / SAMPLE_RATE
                amps[p] = startAmps[p]
                decays[p] = exp(-1f / (taus[p] * SAMPLE_RATE))
            }
            // Constant-power random pan so each strike hangs somewhere in the stereo field.
            val pan = rnd.nextFloat()
            panL = kotlin.math.sqrt(1f - pan)
            panR = kotlin.math.sqrt(pan)
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
    private class RainGenerator : AmbientGenerator() {
        private val rnd = Random(System.nanoTime().toInt())
        private var hpL = 0f
        private var hpR = 0f
        private var lpL = 0f
        private var lpR = 0f
        private var lfo = rnd.nextDouble() * TWO_PI

        private val drops = Array(8) { DropletVoice() }
        private var samplesToNextDrop = SAMPLE_RATE / 2

        override fun render(bus: FloatArray, frames: Int, voice: SceneVoice) {
            for (i in 0 until frames) {
                lfo += TWO_PI * 0.03 / SAMPLE_RATE
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
                    samplesToNextDrop = ((0.25 + rnd.nextDouble() * 1.1) * SAMPLE_RATE).toInt()
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
            increment = TWO_PI * freq / SAMPLE_RATE
            amp = 0.03f + rnd.nextFloat() * 0.03f
            decay = exp(-1f / (0.045f * SAMPLE_RATE))
            val pan = rnd.nextFloat()
            panL = kotlin.math.sqrt(1f - pan)
            panR = kotlin.math.sqrt(pan)
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
    private class OceanGenerator : AmbientGenerator() {
        private val rnd = Random(System.nanoTime().toInt())
        private var brownL = 0f
        private var brownR = 0f
        private var lpL = 0f
        private var lpR = 0f
        private var washLp = 0f
        private var wave1 = rnd.nextDouble() * TWO_PI
        private var wave2 = rnd.nextDouble() * TWO_PI

        override fun render(bus: FloatArray, frames: Int, voice: SceneVoice) {
            for (i in 0 until frames) {
                wave1 += TWO_PI / (12.0 * SAMPLE_RATE)
                wave2 += TWO_PI / (19.0 * SAMPLE_RATE)
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

    // ------------------------------------------------------------------
    // Notification
    // ------------------------------------------------------------------

    private fun createNotification(): Notification {
        val playPauseIntent = if (isPlaying) {
            Intent(this, SoundscapeService::class.java).apply { action = "ACTION_PAUSE" }
        } else {
            Intent(this, SoundscapeService::class.java).apply { action = "ACTION_PLAY" }
        }

        val playPausePendingIntent = PendingIntent.getService(
            this, 0, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, SoundscapeService::class.java).apply { action = "ACTION_STOP" }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 2, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_notification_heart)
            .setContentTitle("FloraFlow Restoration Journal")
            .setContentText("$currentTrackName • ${sceneLabel(currentSceneId)}")
            .setOngoing(isPlaying)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isPlaying) com.example.R.drawable.ic_pause else com.example.R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(com.example.R.drawable.ic_close, "Stop", stopPendingIntent)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Restoration Soundscapes",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background Nature & Binaural Beat playback for neural restoration"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isPlaying = false
        cancelSleepTimerInternal()
        renderJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "restoration_soundscapes"
        private const val NOTIFICATION_ID = 1001

        private const val SAMPLE_RATE = 44100
        private const val FRAMES_PER_BUFFER = 2048
        private const val TWO_PI = 2.0 * PI
        private const val PHASE_WRAP = 2.0 * PI * 1024.0

        private const val FADE_IN_SECONDS = 1.5f
        private const val FADE_OUT_SECONDS = 0.7f
        private const val SLEEP_FADE_SECONDS = 15f
        private const val CROSSFADE_SECONDS = 2.5f

        // Binaural carrier peaks at ~24% of full scale at max slider — present but never harsh.
        private const val BINAURAL_PEAK = 0.24f
        private const val PARAM_SMOOTH = 0.0008f
        private const val FREQ_SMOOTH = 0.00005f

        private const val SCENE_BREEZE = 0
        private const val SCENE_RAIN = 1
        private const val SCENE_OCEAN = 2
    }
}
