package com.example.ai

import android.content.Context
import android.net.Uri
import com.example.data.MediaSourceType
import com.example.data.MusicTrack
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

data class ComposedSynthSpec(
    val title: String,
    val genre: String,
    val bpm: Int,
    val waveform: String,
    val melodyHz: List<Double>,
    val bassHz: List<Double>
)

object WavTrackRenderer {

    fun renderToMusicTrack(
        context: Context,
        spec: ComposedSynthSpec,
        durationSeconds: Int = 18
    ): MusicTrack {
        val sampleRate = 22050
        val clampedDuration = durationSeconds.coerceIn(10, 30)
        val totalSamples = sampleRate * clampedDuration
        val pcmBytes = ByteArray(totalSamples * 2)

        val bpm = spec.bpm.coerceIn(70, 170)
        val stepDurationSec = 60.0 / bpm / 2.0 // eighth notes
        val melody = spec.melodyHz.ifEmpty { listOf(261.63, 329.63, 392.00, 523.25) }
        val bass = spec.bassHz.ifEmpty { listOf(130.81, 130.81, 164.81, 196.00) }
        val waveKind = spec.waveform.lowercase()

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val stepIdx = (t / stepDurationSec).toInt()
            val localStepT = t - stepIdx * stepDurationSec
            val noteEnv = exp(-localStepT * 6.5)

            val mFreq = melody[stepIdx % melody.size].coerceIn(140.0, 1200.0)
            val bFreq = bass[(stepIdx / 2) % bass.size].coerceIn(55.0, 320.0)

            val leadSample = when (waveKind) {
                "square" -> if (sin(2.0 * PI * mFreq * t) >= 0) 0.55 else -0.55
                "saw" -> {
                    val phase = (t * mFreq) % 1.0
                    (phase * 2.0 - 1.0) * 0.55
                }
                else -> sin(2.0 * PI * mFreq * t) * 0.75 + 0.2 * sin(2.0 * PI * mFreq * 2.0 * t)
            }
            val bassSample = sin(2.0 * PI * bFreq * t) * 0.52

            val fadeOut = if (t > clampedDuration - 1.0) (clampedDuration - t).coerceIn(0.0, 1.0) else 1.0
            val mixed = ((leadSample * noteEnv * 0.58 + bassSample * 0.42) * fadeOut).coerceIn(-0.98, 0.98)
            val pcmVal = (mixed * Short.MAX_VALUE).toInt().toShort()

            pcmBytes[i * 2] = (pcmVal.toInt() and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = ((pcmVal.toInt() shr 8) and 0xFF).toByte()
        }

        val dir = File(context.filesDir, "ai_music").apply { mkdirs() }
        val id = "ai_track_${UUID.randomUUID().toString().take(8)}"
        val wavFile = File(dir, "$id.wav")

        val dataSize = pcmBytes.size
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(dataSize + 36)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort())
            putShort(1.toShort())
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2.toShort())
            putShort(16.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()

        FileOutputStream(wavFile).use { fos ->
            fos.write(header)
            fos.write(pcmBytes)
        }

        val mins = clampedDuration / 60
        val secs = clampedDuration % 60
        val durationLabel = "$mins:${secs.toString().padStart(2, '0')}"

        return MusicTrack(
            id = id,
            title = spec.title.ifBlank { "AI Synth Track" },
            artist = "AI Studio • ${bpm} BPM",
            genre = spec.genre.ifBlank { "AI Synth" },
            durationLabel = durationLabel,
            uriString = Uri.fromFile(wavFile).toString(),
            sourceType = MediaSourceType.LOCAL_DEVICE,
            accentHex = 0xFFA855F7
        )
    }
}
