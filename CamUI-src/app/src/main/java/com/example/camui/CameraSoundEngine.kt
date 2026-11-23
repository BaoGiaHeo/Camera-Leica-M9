package com.example.camui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * Low-latency camera feedback synthesized as 44.1 kHz mono PCM16.
 * The PCM peak is limited to -3 dBFS. Actual latency depends on Android audio
 * policy and device scheduling; the engine does not promise a fixed <30 ms.
 */
internal object CameraSoundEngine {
    private const val TAG = "FUJI_CAMERA_SOUND"
    private const val SAMPLE_RATE = 44_100
    private const val MAX_ACTIVE_TRACKS = 6
    private val lock = Any()
    private val activeTracks = ArrayDeque<AudioTrack>()
    private var masterVolume = 0.85f

    enum class Sound { UI_CLICK, CONFIRM }

    fun configure(context: Context, volumePercent: Int) {
        synchronized(lock) { masterVolume = volumePercent.coerceIn(0, 100) / 100f }
    }

    private fun allowed(context: Context): Boolean {
        val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
        if (masterVolume <= 0f) return false
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return audioManager?.ringerMode != AudioManager.RINGER_MODE_SILENT
    }

    private fun systemScaledVolume(context: Context): Float {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return masterVolume
        val maximum = audio.getStreamMaxVolume(AudioManager.STREAM_SYSTEM).coerceAtLeast(1)
        val current = audio.getStreamVolume(AudioManager.STREAM_SYSTEM)
        return masterVolume * (current.toFloat() / maximum).coerceIn(0f, 1f)
    }

    private fun pcm(
        durationMs: Int,
        baseFrequency: Double,
        overtone: Double = 0.0,
        attackMs: Int = 3,
        releaseMs: Int = 18,
        loop: Boolean = false
    ): ShortArray {
        val frames = SAMPLE_RATE * durationMs.coerceIn(20, 600) / 1_000
        val output = ShortArray(frames)
        val attack = (SAMPLE_RATE * attackMs / 1_000).coerceAtLeast(1)
        val release = (SAMPLE_RATE * releaseMs / 1_000).coerceAtLeast(1)
        val peak = 0.707f
        for (i in output.indices) {
            val envelope = if (loop) {
                0.72 + 0.28 * sin(2.0 * PI * i / frames)
            } else {
                min(1.0, i.toDouble() / attack) * min(1.0, (frames - 1 - i).toDouble() / release)
            }
            val t = i.toDouble() / SAMPLE_RATE
            val fundamental = sin(2.0 * PI * baseFrequency * t)
            val harmonic = if (overtone > 0.0) sin(2.0 * PI * overtone * t) * 0.22 else 0.0
            output[i] = ((fundamental * 0.78 + harmonic) * envelope * peak * Short.MAX_VALUE)
                .roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    private fun createTrack(context: Context, samples: ShortArray): AudioTrack {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        val written = track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        check(written == samples.size) {
            "AudioTrack wrote " + written + "/" + samples.size + " PCM frames"
        }
        track.setVolume(systemScaledVolume(context))
        return track
    }

    private fun releaseTrack(track: AudioTrack) {
        synchronized(lock) {
            activeTracks.remove(track)
        }
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.release()
        }.onFailure { Log.w(TAG, "AudioTrack release failed", it) }
    }

    private fun registerTrack(track: AudioTrack, releaseOnMarker: Boolean = true) {
        synchronized(lock) {
            while (activeTracks.size >= MAX_ACTIVE_TRACKS) {
                val oldest = activeTracks.removeFirst()
                runCatching { oldest.pause(); oldest.flush(); oldest.release() }
            }
            activeTracks.addLast(track)
        }
        if (releaseOnMarker) {
            track.setNotificationMarkerPosition(track.bufferSizeInFrames.coerceAtLeast(1))
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(audioTrack: AudioTrack) = releaseTrack(audioTrack)
                override fun onPeriodicNotification(audioTrack: AudioTrack) = Unit
            }, Handler(Looper.getMainLooper()))
        }
    }

    fun play(context: Context, sound: Sound) {
        val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
        configure(context, prefs.getInt("sound_master_volume", 85))
        val enabled = prefs.getBoolean("ui_sound_enabled", true)
        if (!enabled || !allowed(context)) return
        val (duration, frequency, overtone) = when (sound) {
            Sound.UI_CLICK -> Triple(42, 1_450.0, 2_100.0)
            Sound.CONFIRM -> Triple(74, 1_080.0, 1_620.0)
        }
        runCatching {
            val track = createTrack(context, pcm(duration, frequency, overtone))
            registerTrack(track)
            track.play()
        }.onFailure { Log.w(TAG, "Could not play " + sound, it) }
    }

    fun stopAll() {
        val tracks = synchronized(lock) { activeTracks.toList().also { activeTracks.clear() } }
        tracks.forEach { track -> runCatching { track.stop(); track.release() } }
    }
}
