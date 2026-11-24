package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import com.example.data.MusicTrack
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GroundedSource(
    val title: String,
    val uri: String
)

data class SearchGroundedResult(
    val answerText: String,
    val sources: List<GroundedSource>,
    val searchEntryHtml: String?
)

data class StoryboardResult(
    val sceneTitle: String,
    val cameraDirection: String,
    val shots: List<String>,
    val keyframeBitmap: Bitmap?
)

class AiStudioRepository {

    private val flashModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private val groundedSearchModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch())
        )
    }

    private val imageModelDefault by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private val musicComposerModel by lazy {
        val musicSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Short evocative track title"),
                "genre" to Schema.string(description = "Music genre label, e.g. Synthwave, Lo-Fi, Trap"),
                "bpm" to Schema.integer(description = "Tempo in beats per minute between 80 and 160"),
                "waveform" to Schema.enumeration(
                    values = listOf("sine", "square", "saw"),
                    description = "Synthesizer oscillator waveform"
                ),
                "melodyHz" to Schema.array(
                    items = Schema.double(description = "Note frequency in Hz between 180 and 880"),
                    description = "8 to 16 lead melody note frequencies in Hz"
                ),
                "bassHz" to Schema.array(
                    items = Schema.double(description = "Bass frequency in Hz between 65 and 220"),
                    description = "4 to 8 bassline frequencies in Hz"
                )
            )
        )
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = musicSchema
            }
        )
    }

    private val storyboardModel by lazy {
        val storyboardSchema = Schema.obj(
            properties = mapOf(
                "sceneTitle" to Schema.string(description = "Short cinematic title"),
                "cameraDirection" to Schema.string(description = "Motion & lighting guidance"),
                "shots" to Schema.array(
                    items = Schema.string(),
                    description = "3 concise shot-by-shot motion descriptions"
                ),
                "keyframePrompt" to Schema.string(description = "Detailed visual prompt for generating the hero keyframe")
            )
        )
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = storyboardSchema
            }
        )
    }

    suspend fun generateOrEditImage(
        prompt: String,
        inputBitmap: Bitmap? = null
    ): Bitmap {
        val response = withTimeout(60_000L) {
            if (inputBitmap != null) {
                imageModelDefault.generateContent(
                    content {
                        image(inputBitmap)
                        text(prompt)
                    }
                )
            } else {
                imageModelDefault.generateContent(prompt)
            }
        }
        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
        return imagePart?.image ?: throw IllegalStateException("No image was returned by Gemini.")
    }

    suspend fun askWithGoogleSearch(query: String): SearchGroundedResult {
        val response = withTimeout(60_000L) {
            groundedSearchModel.generateContent(query)
        }
        val text = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Model returned an empty grounded response.")

        val metadata = response.candidates.firstOrNull()?.groundingMetadata
        val sources = metadata?.groundingChunks
            ?.mapNotNull { chunk ->
                val web = chunk.web
                val uri = web?.uri
                if (!uri.isNullOrBlank()) {
                    GroundedSource(
                        title = web.title?.takeIf { it.isNotBlank() } ?: uri,
                        uri = uri
                    )
                } else null
            }
            ?.distinctBy { it.uri }
            .orEmpty()

        val searchEntryHtml = metadata?.searchEntryPoint?.renderedContent
        return SearchGroundedResult(
            answerText = text,
            sources = sources,
            searchEntryHtml = searchEntryHtml
        )
    }

    suspend fun transcribeAudio(wavBytes: ByteArray): String {
        if (wavBytes.isEmpty()) throw IllegalArgumentException("Recorded audio is empty.")
        val response = withTimeout(60_000L) {
            flashModel.generateContent(
                content {
                    inlineData(wavBytes, "audio/wav")
                    text("Transcribe the spoken audio accurately. If no speech is audible, state what sounds are heard.")
                }
            )
        }
        return response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("No transcription returned by Gemini.")
    }

    suspend fun composeAndRenderTrack(
        context: Context,
        prompt: String,
        durationSeconds: Int = 18
    ): MusicTrack {
        val response = withTimeout(60_000L) {
            musicComposerModel.generateContent(
                "Compose a catchy instrumental synth track matching this mood/prompt: $prompt"
            )
        }
        val jsonText = response.text
            ?: throw IllegalStateException("Empty composition returned by Gemini.")
        val json = JSONObject(jsonText)
        val melodyArray = json.optJSONArray("melodyHz")
        val bassArray = json.optJSONArray("bassHz")
        val melody = buildList {
            if (melodyArray != null) {
                for (i in 0 until melodyArray.length()) {
                    add(melodyArray.optDouble(i, 329.63))
                }
            }
        }
        val bass = buildList {
            if (bassArray != null) {
                for (i in 0 until bassArray.length()) {
                    add(bassArray.optDouble(i, 130.81))
                }
            }
        }
        val spec = ComposedSynthSpec(
            title = json.optString("title", "AI Synth Track"),
            genre = json.optString("genre", "Electronic"),
            bpm = json.optInt("bpm", 118),
            waveform = json.optString("waveform", "saw"),
            melodyHz = melody,
            bassHz = bass
        )
        return WavTrackRenderer.renderToMusicTrack(context, spec, durationSeconds)
    }

    suspend fun generateStoryboardAndKeyframe(
        prompt: String,
        inputBitmap: Bitmap?,
        landscape: Boolean
    ): StoryboardResult {
        val ratio = if (landscape) AspectRatio.LANDSCAPE_16x9 else AspectRatio.PORTRAIT_9x16
        val planResp = withTimeout(45_000L) {
            if (inputBitmap != null) {
                storyboardModel.generateContent(
                    content {
                        image(inputBitmap)
                        text("Create a 3-shot cinematic motion storyboard starting from this image and prompt: $prompt")
                    }
                )
            } else {
                storyboardModel.generateContent(
                    "Create a 3-shot cinematic motion storyboard for this video idea: $prompt"
                )
            }
        }
        val jsonText = planResp.text
            ?: throw IllegalStateException("Empty storyboard returned by Gemini.")
        val json = JSONObject(jsonText)
        val shotsArr = json.optJSONArray("shots")
        val shots = buildList {
            if (shotsArr != null) {
                for (i in 0 until shotsArr.length()) {
                    add(shotsArr.optString(i))
                }
            }
        }
        val keyframePrompt = json.optString("keyframePrompt", prompt)

        val keyframeModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(ratio)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
        val keyframeResp = withTimeout(60_000L) {
            if (inputBitmap != null) {
                keyframeModel.generateContent(
                    content {
                        image(inputBitmap)
                        text(keyframePrompt)
                    }
                )
            } else {
                keyframeModel.generateContent(keyframePrompt)
            }
        }
        val keyframeBitmap = keyframeResp.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()?.image

        return StoryboardResult(
            sceneTitle = json.optString("sceneTitle", "AI Scene Concept"),
            cameraDirection = json.optString("cameraDirection", "Smooth tracking shot"),
            shots = shots,
            keyframeBitmap = keyframeBitmap
        )
    }
}
