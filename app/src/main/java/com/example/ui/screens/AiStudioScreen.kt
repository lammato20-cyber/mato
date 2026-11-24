package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ai.MicAudioRecorder
import com.example.ai.SearchGroundedResult
import com.example.ai.StoryboardResult
import com.example.data.MusicTrack
import com.example.ui.theme.ArcadeMint
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.StreamCyan
import com.example.viewmodel.AiUiState
import kotlinx.coroutines.launch

private enum class AiStudioTool(
    val label: String,
    val icon: ImageVector,
    val accent: Color
) {
    IMAGE("Images", Icons.Filled.Image, PulseCoral),
    MUSIC("Music", Icons.Filled.GraphicEq, ArcadeMint),
    TRANSCRIBE("Transcribe", Icons.Filled.Mic, NeonAmber),
    SEARCH("Search", Icons.Filled.Public, StreamCyan),
    STORYBOARD("Storyboard", Icons.Filled.MovieCreation, Color(0xFFA855F7))
}

@Composable
fun AiStudioScreen(
    imageState: AiUiState<Bitmap>,
    musicGenState: AiUiState<MusicTrack>,
    transcribeState: AiUiState<String>,
    searchState: AiUiState<SearchGroundedResult>,
    storyboardState: AiUiState<StoryboardResult>,
    onGenerateOrEditImage: (String, Bitmap?) -> Unit,
    onComposeMusic: (String, Int) -> Unit,
    onPlayTrack: (MusicTrack) -> Unit,
    onTranscribeWav: (ByteArray) -> Unit,
    onAskWithSearch: (String) -> Unit,
    onGenerateStoryboard: (String, Bitmap?, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTool by rememberSaveable { mutableStateOf(AiStudioTool.IMAGE) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        activeTool.accent.copy(alpha = 0.45f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = activeTool.accent.copy(alpha = 0.18f),
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = activeTool.accent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PulseDeck AI Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gemini images, synth tracks, mic transcripts & search",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiStudioTool.entries.forEach { tool ->
                    FilterChip(
                        selected = activeTool == tool,
                        onClick = { activeTool = tool },
                        label = { Text(tool.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("ai_tool_${tool.name.lowercase()}")
                    )
                }
            }
        }

        item {
            when (activeTool) {
                AiStudioTool.IMAGE -> ImageForgeSection(
                    state = imageState,
                    onGenerate = onGenerateOrEditImage
                )
                AiStudioTool.MUSIC -> MusicLabSection(
                    state = musicGenState,
                    onCompose = onComposeMusic,
                    onPlayTrack = onPlayTrack
                )
                AiStudioTool.TRANSCRIBE -> MicTranscribeSection(
                    state = transcribeState,
                    onTranscribeWav = onTranscribeWav
                )
                AiStudioTool.SEARCH -> SearchGroundedSection(
                    state = searchState,
                    onAsk = onAskWithSearch,
                    onOpenUrl = { url ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                )
                AiStudioTool.STORYBOARD -> StoryboardSection(
                    state = storyboardState,
                    onGenerate = onGenerateStoryboard
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun ImageForgeSection(
    state: AiUiState<Bitmap>,
    onGenerate: (String, Bitmap?) -> Unit
) {
    val context = LocalContext.current
    var prompt by rememberSaveable { mutableStateOf("") }
    var referenceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            referenceBitmap = decodeBitmapFromUri(context, uri)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create & Edit Images",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Describe image or edit instruction") },
                placeholder = { Text("e.g. Neon muscle car racing downtown at sunset") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_image_prompt_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("ai_image_pick_photo_button")
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (referenceBitmap == null) "Pick Photo to Edit" else "Change Photo")
                }

                if (referenceBitmap != null) {
                    IconButton(onClick = { referenceBitmap = null }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear selected photo")
                    }
                }
            }

            referenceBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Selected photo to edit",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            Button(
                onClick = { onGenerate(prompt, referenceBitmap) },
                enabled = prompt.isNotBlank() && state !is AiUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = PulseCoral),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_image_generate_button")
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (referenceBitmap != null) "Edit Image with Gemini" else "Generate Image",
                    color = Color.White
                )
            }

            when (state) {
                is AiUiState.Idle -> Text(
                    text = "Powered by gemini-nano-banana-2.1 via Firebase AI Logic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                is AiUiState.Loading -> LoadingStatusBox("Generating image (typically 20–60s)...")
                is AiUiState.Error -> ErrorStatusBox(state.message)
                is AiUiState.Success -> Image(
                    bitmap = state.data.asImageBitmap(),
                    contentDescription = "Generated AI image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
    }
}

@Composable
private fun MusicLabSection(
    state: AiUiState<MusicTrack>,
    onCompose: (String, Int) -> Unit,
    onPlayTrack: (MusicTrack) -> Unit
) {
    var prompt by rememberSaveable { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "AI Synth Music Lab",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Gemini composes melody, bass & BPM and renders a playable WAV track directly into your Music library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Mood, genre & tempo") },
                placeholder = { Text("e.g. Driving Miami synthwave bassline 122 BPM") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_music_prompt_input")
            )
            Button(
                onClick = { onCompose(prompt, 18) },
                enabled = prompt.isNotBlank() && state !is AiUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = ArcadeMint),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_music_generate_button")
            ) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compose & Play Track", color = Color.Black)
            }

            when (state) {
                is AiUiState.Idle -> Unit
                is AiUiState.Loading -> LoadingStatusBox("Composing synth arrangement...")
                is AiUiState.Error -> ErrorStatusBox(state.message)
                is AiUiState.Success -> Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ArcadeMint.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(state.data.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${state.data.artist} • ${state.data.genre} • ${state.data.durationLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onPlayTrack(state.data) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play generated track", tint = ArcadeMint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MicTranscribeSection(
    state: AiUiState<String>,
    onTranscribeWav: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember { MicAudioRecorder() }
    var isRecording by remember { mutableStateOf(false) }
    var micError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { recorder.release() }
    }

    fun startMicCapture() {
        micError = null
        isRecording = true
        scope.launch {
            recorder.startRecording { err ->
                micError = err
                isRecording = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startMicCapture()
        } else {
            micError = "Microphone permission is required to record & transcribe audio."
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Voice & Audio Transcription",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Record live audio with your microphone and transcribe it via Gemini 3.8 Flash.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    if (isRecording) {
                        isRecording = false
                        val wav = recorder.stopAndBuildWav()
                        onTranscribeWav(wav)
                    } else {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            startMicCapture()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) PulseCoral else NeonAmber
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_transcribe_record_button")
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecording) "Stop & Transcribe" else "Record Audio",
                    color = Color.Black
                )
            }

            if (micError != null) {
                ErrorStatusBox(micError!!)
            }

            when (state) {
                is AiUiState.Idle -> Unit
                is AiUiState.Loading -> LoadingStatusBox("Transcribing audio with Gemini...")
                is AiUiState.Error -> ErrorStatusBox(state.message)
                is AiUiState.Success -> Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.data,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchGroundedSection(
    state: AiUiState<SearchGroundedResult>,
    onAsk: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Google Search Grounded Assistant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Ask anything with live Google Search") },
                placeholder = { Text("e.g. Latest Formula 1 race winner or top synth releases") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_search_query_input")
            )
            Button(
                onClick = { onAsk(query) },
                enabled = query.isNotBlank() && state !is AiUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = StreamCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_search_submit_button")
            ) {
                Icon(Icons.Filled.Public, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search & Answer", color = Color.Black)
            }

            when (state) {
                is AiUiState.Idle -> Unit
                is AiUiState.Loading -> LoadingStatusBox("Searching web & synthesizing grounded answer...")
                is AiUiState.Error -> ErrorStatusBox(state.message)
                is AiUiState.Success -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.data.answerText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    if (state.data.sources.isNotEmpty()) {
                        Text("Sources", style = MaterialTheme.typography.labelLarge)
                        state.data.sources.take(6).forEach { src ->
                            Surface(
                                onClick = { onOpenUrl(src.uri) },
                                shape = RoundedCornerShape(10.dp),
                                color = StreamCyan.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = src.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.OpenInNew,
                                        contentDescription = "Open source",
                                        tint = StreamCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    val rendered = state.data.searchEntryHtml
                    if (!rendered.isNullOrBlank()) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = WebViewClient()
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                }
                            },
                            update = { wv ->
                                wv.loadDataWithBaseURL(null, rendered, "text/html", "utf-8", null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryboardSection(
    state: AiUiState<StoryboardResult>,
    onGenerate: (String, Bitmap?, Boolean) -> Unit
) {
    val context = LocalContext.current
    var prompt by rememberSaveable { mutableStateOf("") }
    var landscape by rememberSaveable { mutableStateOf(true) }
    var startFrame by remember { mutableStateOf<Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            startFrame = decodeBitmapFromUri(context, uri)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Scene Storyboard & Keyframe",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Generates a 3-shot camera motion script and hero keyframe via Firebase AI Logic (MP4 playback is supported in Videos).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Describe the motion scene") },
                placeholder = { Text("e.g. Midnight street race drifting past neon billboards") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_storyboard_prompt_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = landscape,
                    onClick = { landscape = true },
                    label = { Text("16:9 Wide") }
                )
                FilterChip(
                    selected = !landscape,
                    onClick = { landscape = false },
                    label = { Text("9:16 Vertical") }
                )
                OutlinedButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text(if (startFrame == null) "Animate Photo" else "Change Photo")
                }
            }

            Button(
                onClick = { onGenerate(prompt, startFrame, landscape) },
                enabled = prompt.isNotBlank() && state !is AiUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_storyboard_generate_button")
            ) {
                Text("Generate Storyboard & Keyframe", color = Color.White)
            }

            when (state) {
                is AiUiState.Idle -> Unit
                is AiUiState.Loading -> LoadingStatusBox("Directing storyboard & rendering keyframe...")
                is AiUiState.Error -> ErrorStatusBox(state.message)
                is AiUiState.Success -> {
                    state.data.keyframeBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = state.data.sceneTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(if (landscape) 16f / 9f else 9f / 16f)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                    Text(state.data.sceneTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Camera: ${state.data.cameraDirection}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.data.shots.forEachIndexed { idx, shot ->
                        Text(
                            text = "${idx + 1}. $shot",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingStatusBox(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorStatusBox(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Suppress("DEPRECATION")
private fun decodeBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return runCatching {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val maxDim = 1024
        if (raw.width > maxDim || raw.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(raw.width, raw.height)
            Bitmap.createScaledBitmap(
                raw,
                (raw.width * scale).toInt().coerceAtLeast(1),
                (raw.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            raw
        }
    }.getOrNull()
}
