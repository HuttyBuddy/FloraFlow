package com.example.ui.screens.restoration

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import kotlinx.coroutines.*
import kotlin.math.sin

class SoundscapeService : Service() {

    private val binder = SoundscapeBinder()
    
    // Audio players
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    
    // Audio state
    private var isPlaying = false
    private var currentTrackName = "Theta Meditate"
    private var baseFrequency = 200f // Hz
    private var diffFrequency = 6f   // Hz (Theta)
    
    // Volume levels
    private var ambientVolume = 0.5f
    private var binauralVolume = 0.3f
    
    // Threading for tone generation
    private var toneJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    inner class SoundscapeBinder : Binder() {
        fun getService(): SoundscapeService = this@SoundscapeService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

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
        
        // 1. Start Ambient loops
        playAmbientLoop()
        
        // 2. Start Binaural tones
        startBinauralGeneration()
        
        // 3. Promote to foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    fun pauseSoundscape() {
        if (!isPlaying) return
        isPlaying = false
        
        // 1. Pause media player
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 2. Stop tone generator
        stopBinauralGeneration()
        
        // 3. Stop foreground notification tracking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            stopForeground(false)
        }
        
        // Update notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    fun setTrack(name: String, baseFreq: Float, diffFreq: Float) {
        currentTrackName = name
        this.baseFrequency = baseFreq
        this.diffFrequency = diffFreq
        
        if (isPlaying) {
            stopBinauralGeneration()
            startBinauralGeneration()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }

    fun setAmbientVolume(vol: Float) {
        ambientVolume = vol
        try {
            mediaPlayer?.setVolume(vol, vol)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBinauralVolume(vol: Float) {
        binauralVolume = vol
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioTrack?.setVolume(vol)
        } else {
            audioTrack?.setStereoVolume(vol, vol)
        }
    }

    fun isPlaying(): Boolean = isPlaying
    fun getCurrentTrackName(): String = currentTrackName
    fun getAmbientVolume(): Float = ambientVolume
    fun getBinauralVolume(): Float = binauralVolume

    private fun playAmbientLoop() {
        try {
            if (mediaPlayer == null) {
                val mp = MediaPlayer.create(this, R.raw.wind_chime)
                if (mp != null) {
                    mp.isLooping = true
                    mediaPlayer = mp
                }
            }
            mediaPlayer?.setVolume(ambientVolume, ambientVolume)
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startBinauralGeneration() {
        toneJob?.cancel()
        
        try {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            if (minBufferSize <= 0) return
            
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setVolume(binauralVolume)
                } else {
                    setStereoVolume(binauralVolume, binauralVolume)
                }
                play()
            }

            toneJob = serviceScope.launch {
                val freqLeft = baseFrequency
                val freqRight = baseFrequency + diffFrequency
                val bufferSize = minBufferSize / 2 // in Shorts
                val buffer = ShortArray(bufferSize)
                var angleLeft = 0.0
                var angleRight = 0.0
                
                while (isActive && isPlaying) {
                    for (i in 0 until bufferSize step 2) {
                        // Left channel
                        buffer[i] = (sin(angleLeft) * Short.MAX_VALUE).toInt().toShort()
                        angleLeft += 2.0 * Math.PI * freqLeft / sampleRate
                        
                        // Right channel
                        if (i + 1 < bufferSize) {
                            buffer[i + 1] = (sin(angleRight) * Short.MAX_VALUE).toInt().toShort()
                            angleRight += 2.0 * Math.PI * freqRight / sampleRate
                        }
                    }
                    audioTrack?.write(buffer, 0, bufferSize)
                    yield()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopBinauralGeneration() {
        toneJob?.cancel()
        toneJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
    }

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
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("FloraFlow Restoration Journal")
            .setContentText("Listening to: $currentTrackName")
            .setOngoing(isPlaying)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            
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
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        stopBinauralGeneration()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "restoration_soundscapes"
        private const val NOTIFICATION_ID = 1001
    }
}
