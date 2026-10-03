package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.domain.SoundLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class TinyAudioPlayer {

    private val sampleRate = 44100
    private var soundLevel: SoundLevel = SoundLevel.LOW
    private var lastPlayTime = 0L
    private val minIntervalMs = 60L // Debounce rapid spam

    private val soundBuffers = ConcurrentHashMap<String, ByteArray>()
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            preloadSounds()
        }
    }

    fun setSoundLevel(level: SoundLevel) {
        this.soundLevel = level
    }

    private fun preloadSounds() {
        try {
            soundBuffers["pop"] = generatePopPcm()
            soundBuffers["chime"] = generateTonePcm(880f, 0.35f, 0.4f)
            soundBuffers["sparkle"] = generateSparklePcm()
            soundBuffers["bird"] = generateBirdPcm()
            soundBuffers["rain"] = generateRaindropPcm()
            soundBuffers["car"] = generateCarPcm()
            soundBuffers["apple"] = generateTonePcm(340f, 0.18f, 0.2f)
            soundBuffers["camera"] = generateCameraClickPcm()

            // Musical pentatonic notes (C4, D4, E4, G4, A4, C5)
            val pentatonic = listOf(
                "note_C" to 261.63f,
                "note_D" to 293.66f,
                "note_E" to 329.63f,
                "note_G" to 392.00f,
                "note_A" to 440.00f,
                "note_C5" to 523.25f
            )
            pentatonic.forEach { (name, freq) ->
                soundBuffers[name] = generateTonePcm(freq, 0.35f, 0.6f)
            }
        } catch (e: Exception) {
            Log.e("TinyAudioPlayer", "Error preloading sounds", e)
        }
    }

    fun play(soundKey: String) {
        val volume = soundLevel.volumeFraction
        if (volume <= 0f) return

        val now = System.currentTimeMillis()
        if (now - lastPlayTime < minIntervalMs) return
        lastPlayTime = now

        scope.launch {
            val pcmData = soundBuffers[soundKey] ?: return@launch
            playPcm(pcmData, volume)
        }
    }

    fun playNote(index: Int) {
        val notes = listOf("note_C", "note_D", "note_E", "note_G", "note_A", "note_C5")
        val key = notes[index.coerceIn(0, notes.size - 1)]
        play(key)
    }

    private fun playPcm(pcmData: ByteArray, volume: Float) {
        var track: AudioTrack? = null
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val scaledData = ByteArray(pcmData.size)
            for (i in pcmData.indices step 2) {
                if (i + 1 < pcmData.size) {
                    val sample = (pcmData[i].toInt() and 0xFF) or (pcmData[i + 1].toInt() shl 8)
                    val shortVal = sample.toShort()
                    val scaled = (shortVal * volume).toInt().coerceIn(-32768, 32767).toShort()
                    scaledData[i] = (scaled.toInt() and 0xFF).toByte()
                    scaledData[i + 1] = ((scaled.toInt() shr 8) and 0xFF).toByte()
                }
            }

            track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(scaledData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(scaledData, 0, scaledData.size)
            track.play()

            val durationMs = (scaledData.size * 1000L) / (sampleRate * 2)
            Thread.sleep(durationMs + 20)
        } catch (e: Exception) {
            // Silently ignore audio playback errors
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Sound Generators
    private fun generatePopPcm(): ByteArray {
        val durationSec = 0.09f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = t / durationSec
            // Frequency rises from 250 Hz to 650 Hz
            val freq = 250f + (progress * 400f)
            val envelope = (1f - progress) * (1f - progress)
            val sample = sin(2.0 * PI * freq * t) * envelope
            buffer[i] = (sample * 24000).toInt().toShort()
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateTonePcm(freq: Float, durationSec: Float, decayRate: Float): ByteArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * (decayRate * 10f))
            // Rich fundamental + warm second harmonic
            val sample = (0.75 * sin(2.0 * PI * freq * t) + 0.25 * sin(2.0 * PI * (freq * 2) * t)) * envelope
            buffer[i] = (sample * 22000).toInt().toShort()
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateSparklePcm(): ByteArray {
        val durationSec = 0.28f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        val freqs = floatArrayOf(523f, 659f, 784f, 1046f)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val stage = (t / durationSec * freqs.size).toInt().coerceIn(0, freqs.size - 1)
            val f = freqs[stage]
            val envelope = 1f - (t / durationSec)
            val sample = sin(2.0 * PI * f * t) * envelope
            buffer[i] = (sample * 18000).toInt().toShort()
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateBirdPcm(): ByteArray {
        val durationSec = 0.14f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = t / durationSec
            val f = 1900f + (sin(progress * PI * 3) * 600f).toFloat()
            val envelope = sin(progress * PI).toFloat()
            val sample = sin(2.0 * PI * f * t) * envelope
            buffer[i] = (sample * 16000).toInt().toShort()
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateRaindropPcm(): ByteArray {
        val durationSec = 0.08f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = t / durationSec
            val f = 450f + (progress * 300f)
            val envelope = exp(-progress * 5f)
            val sample = sin(2.0 * PI * f * t) * envelope
            buffer[i] = (sample * 18000).toInt().toShort()
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateCarPcm(): ByteArray {
        val durationSec = 0.22f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val envelope = (1f - (t / durationSec) * 0.8f)
            val sample = (0.7 * sin(2.0 * PI * 180f * t) + 0.3 * sin(2.0 * PI * 360f * t)) * envelope
            buffer[i] = (sample * 19000).toInt().toShort()
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateCameraClickPcm(): ByteArray {
        val durationSec = 0.07f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = t / durationSec
            // Dual transient pulse click
            val pulse = if (progress < 0.3f || (progress in 0.5f..0.8f)) 1f else 0.2f
            val noise = ((Math.random() * 2.0) - 1.0) * pulse * exp(-progress * 6f)
            buffer[i] = (noise * 16000).toInt().toShort()
        }
        return shortArrayToByteArray(buffer)
    }

    private fun shortArrayToByteArray(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        for (i in shorts.indices) {
            val s = shorts[i].toInt()
            bytes[i * 2] = (s and 0xFF).toByte()
            bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}
