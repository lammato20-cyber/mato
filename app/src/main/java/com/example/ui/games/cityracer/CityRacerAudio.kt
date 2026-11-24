package com.example.ui.games.cityracer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

class CityRacerAudio {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    var isMuted: Boolean = false

    private var lastImpactAt = 0L
    private var lastSirenAt = 0L

    fun playCashChime() {
        playToneSeq(listOf(880.0 to 70, 1174.66 to 110), 0.25f, square = false)
    }

    fun playCarJack() {
        playToneSeq(listOf(220.0 to 55, 330.0 to 65, 165.0 to 90), 0.30f, square = true)
    }

    fun playNitroBurst() {
        playSweep(180.0, 520.0, 160, 0.26f)
    }

    fun playCrashImpact(heavy: Boolean = false) {
        val now = System.currentTimeMillis()
        if (now - lastImpactAt < 180L) return
        lastImpactAt = now
        playSweep(if (heavy) 130.0 else 170.0, 48.0, if (heavy) 160 else 95, if (heavy) 0.42f else 0.28f)
    }

    fun playSirenBlip() {
        val now = System.currentTimeMillis()
        if (now - lastSirenAt < 900L) return
        lastSirenAt = now
        playToneSeq(listOf(740.0 to 110, 587.0 to 110), 0.14f, square = false)
    }

    fun playRaceWin() {
        playToneSeq(listOf(523.25 to 90, 659.25 to 90, 783.99 to 110, 1046.5 to 220), 0.34f, square = false)
    }

    private fun playToneSeq(notes: List<Pair<Double, Int>>, gain: Float, square: Boolean) {
        if (isMuted) return
        scope.launch {
            runCatching {
                val sr = 22050
                val totalSamples = notes.sumOf { (sr * it.second) / 1000 }
                val buf = ShortArray(totalSamples)
                var offset = 0
                for ((freq, ms) in notes) {
                    val count = (sr * ms) / 1000
                    for (i in 0 until count) {
                        val env = if (i < 160) i / 160f else if (i > count - 250) (count - i) / 250f else 1f
                        val s = sin(2.0 * Math.PI * i * freq / sr)
                        val wave = if (square) (if (s >= 0) 0.7 else -0.7) else s
                        buf[offset + i] = (wave * gain * env * Short.MAX_VALUE).toInt().toShort()
                    }
                    offset += count
                }
                emitPcm(buf, sr)
            }
        }
    }

    private fun playSweep(startFreq: Double, endFreq: Double, ms: Int, gain: Float) {
        if (isMuted) return
        scope.launch {
            runCatching {
                val sr = 22050
                val count = (sr * ms) / 1000
                val buf = ShortArray(count)
                var phase = 0.0
                for (i in 0 until count) {
                    val t = i.toFloat() / count.toFloat()
                    val freq = startFreq + (endFreq - startFreq) * t
                    phase += 2.0 * Math.PI * freq / sr
                    val env = (1f - t)
                    buf[i] = (sin(phase) * gain * env * Short.MAX_VALUE).toInt().toShort()
                }
                emitPcm(buf, sr)
            }
        }
    }

    private suspend fun emitPcm(samples: ShortArray, sampleRate: Int) {
        if (samples.isEmpty()) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        val playMs = (samples.size * 1000L) / sampleRate + 40L
        delay(playMs)
        track.stop()
        track.release()
    }
}
