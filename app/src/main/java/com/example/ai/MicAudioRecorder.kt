package com.example.ai

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

class MicAudioRecorder {
    private val isRecording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private val pcmStream = ByteArrayOutputStream()

    @SuppressLint("MissingPermission")
    suspend fun startRecording(onError: (String) -> Unit) = withContext(Dispatchers.IO) {
        if (isRecording.get()) return@withContext
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuf <= 0) {
            onError("Microphone hardware unavailable on this device.")
            return@withContext
        }

        val bufferSize = (minBuf * 2).coerceAtLeast(4096)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            onError("Unable to initialize microphone recorder.")
            return@withContext
        }

        pcmStream.reset()
        audioRecord = recorder
        isRecording.set(true)
        recorder.startRecording()

        val buffer = ByteArray(bufferSize)
        val maxBytes = sampleRate * 2 * 90 // Cap at 90s mono PCM16 (~2.88MB)
        try {
            while (isRecording.get() && pcmStream.size() < maxBytes) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    pcmStream.write(buffer, 0, read)
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Audio recording interrupted.")
        }
    }

    fun stopAndBuildWav(): ByteArray {
        isRecording.set(false)
        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
        }
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null

        val pcmData = pcmStream.toByteArray()
        pcmStream.reset()
        if (pcmData.isEmpty()) return ByteArray(0)
        return prependWavHeader(pcmData, sampleRate = 16000, channels = 1, bitsPerSample = 16)
    }

    fun release() {
        isRecording.set(false)
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
    }

    private fun prependWavHeader(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort()) // PCM
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((channels * bitsPerSample / 8).toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(totalAudioLen)
        }.array()
        return header + pcmData
    }
}
