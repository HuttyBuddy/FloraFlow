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
import com.example.audio.AmbientGenerator
import com.example.audio.SceneVoice
import com.example.audio.Soundscape
import kotlinx.coroutines.*
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Real-time playback of the generative eco-acoustic engine.
 *
 * The DSP itself lives in [Soundscape] so the Reels exporter can render the identical
 * sound offline — a shared clip carries the same soundscape the user heard. This service
 * owns only playback concerns: the AudioTrack, fades, crossfades and the notification.
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

    private fun sceneForTrack(name: String): Int? = Soundscape.sceneForTrack(name)

    private fun createGenerator(sceneId: Int): AmbientGenerator = Soundscape.createGenerator(sceneId)

    private fun sceneLabel(sceneId: Int): String = Soundscape.sceneLabel(sceneId)


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

        private const val FRAMES_PER_BUFFER = 2048

        private const val FADE_IN_SECONDS = 1.5f
        private const val FADE_OUT_SECONDS = 0.7f
        private const val SLEEP_FADE_SECONDS = 15f

        // Playback-side aliases for the shared engine's DSP constants, so the live mix and
        // the offline Reels render can never drift apart.
        private const val SAMPLE_RATE = Soundscape.SAMPLE_RATE
        private const val TWO_PI = Soundscape.TWO_PI
        private const val PHASE_WRAP = Soundscape.PHASE_WRAP
        private const val BINAURAL_PEAK = Soundscape.BINAURAL_PEAK
        private const val PARAM_SMOOTH = Soundscape.PARAM_SMOOTH
        private const val FREQ_SMOOTH = Soundscape.FREQ_SMOOTH

        private const val SCENE_RAIN = Soundscape.SCENE_RAIN
    }
}
