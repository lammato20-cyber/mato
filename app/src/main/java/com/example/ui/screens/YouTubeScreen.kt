package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.MediaCatalog
import com.example.data.YouTubeVideo
import com.example.ui.theme.YoutubeRed

private enum class YouTubeMode(val label: String) {
    STREAM_PLAYER("Stream Player"),
    WEB_BROWSE("Browse YouTube")
}

@Composable
fun YouTubeScreen(
    activeVideo: YouTubeVideo,
    savedVideos: List<YouTubeVideo>,
    recentPastedVideos: List<YouTubeVideo>,
    onSelectVideo: (YouTubeVideo) -> Unit,
    onPlayUrlOrId: (String) -> Boolean,
    onToggleBookmark: (YouTubeVideo) -> Unit,
    onRemoveRecentPaste: (String) -> Unit,
    onClearRecentPastes: () -> Unit,
    onPauseBackgroundMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var mode by rememberSaveable { mutableStateOf(YouTubeMode.STREAM_PLAYER) }
    var urlInput by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var inputError by remember { mutableStateOf<String?>(null) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customFullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    if (customFullscreenView != null) {
        BackHandler {
            customFullscreenCallback?.onCustomViewHidden()
            customFullscreenView = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { customFullscreenView!! },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = {
                    customFullscreenCallback?.onCustomViewHidden()
                    customFullscreenView = null
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Exit fullscreen", tint = Color.White)
            }
        }
        return
    }

    val isBookmarked = remember(activeVideo.videoId, savedVideos) {
        savedVideos.any { it.videoId == activeVideo.videoId }
    }

    val categories = remember { listOf("All", "Daily", "Saved", "Music", "Nature", "Animation", "Tech") }

    val displayedVideos = remember(selectedCategory, savedVideos, recentPastedVideos) {
        when (selectedCategory) {
            "All" -> (recentPastedVideos + savedVideos + MediaCatalog.curatedYouTubeVideos).distinctBy { it.videoId }
            "Daily" -> recentPastedVideos
            "Saved" -> savedVideos
            else -> MediaCatalog.curatedYouTubeVideos.filter { it.category == selectedCategory }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Mode switcher
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            YouTubeMode.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = mode == item,
                    onClick = {
                        onPauseBackgroundMedia()
                        mode = item
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, YouTubeMode.entries.size),
                    icon = {
                        Icon(
                            imageVector = if (item == YouTubeMode.STREAM_PLAYER) {
                                Icons.Filled.SmartDisplay
                            } else {
                                Icons.Filled.Public
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("youtube_mode_${item.name.lowercase()}")
                ) {
                    Text(item.label)
                }
            }
        }

        if (mode == YouTubeMode.WEB_BROWSE) {
            YouTubeWebBrowserView(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onVideoCaptured = { capturedId ->
                    onSelectVideo(
                        YouTubeVideo(
                            videoId = capturedId,
                            title = "YouTube Video ($capturedId)",
                            channel = "YouTube Stream",
                            category = "Stream",
                            durationLabel = "HD"
                        )
                    )
                    mode = YouTubeMode.STREAM_PLAYER
                },
                onShowCustomView = { view, cb ->
                    customFullscreenView = view
                    customFullscreenCallback = cb
                },
                onHideCustomView = {
                    customFullscreenView = null
                    customFullscreenCallback = null
                },
                modifier = Modifier.weight(1f)
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            YoutubeRed.copy(alpha = 0.55f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black)
                        ) {
                            YouTubeIframeWebView(
                                videoId = activeVideo.videoId,
                                onShowCustomView = { view, cb ->
                                    customFullscreenView = view
                                    customFullscreenCallback = cb
                                },
                                onHideCustomView = {
                                    customFullscreenView = null
                                    customFullscreenCallback = null
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeVideo.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${activeVideo.channel} • ${activeVideo.durationLabel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onToggleBookmark(activeVideo) },
                                modifier = Modifier.testTag("youtube_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = "Bookmark video",
                                    tint = if (isBookmarked) YoutubeRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.youtube.com/watch?v=${activeVideo.videoId}")
                                    )
                                    runCatching { context.startActivity(intent) }
                                },
                                modifier = Modifier.testTag("youtube_open_external_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInNew,
                                    contentDescription = "Open in YouTube",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = {
                                urlInput = it
                                inputError = null
                            },
                            placeholder = { Text("Paste any YouTube URL or Video ID") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
                                        if (clipText.isEmpty()) {
                                            inputError = "Clipboard is empty — copy a YouTube link first"
                                        } else {
                                            urlInput = clipText
                                            inputError = null
                                            if (onPlayUrlOrId(clipText)) {
                                                urlInput = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("youtube_paste_clipboard_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentPaste,
                                        contentDescription = "Paste YouTube link from clipboard"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    if (onPlayUrlOrId(urlInput)) {
                                        urlInput = ""
                                    } else {
                                        inputError = "Enter a valid YouTube link or 11-char ID"
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("youtube_url_input")
                        )
                        Button(
                            onClick = {
                                if (onPlayUrlOrId(urlInput)) {
                                    urlInput = ""
                                } else {
                                    inputError = "Enter a valid YouTube link or 11-char ID"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = YoutubeRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("youtube_stream_url_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Stream", tint = Color.White)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Supports watch, shorts, live & youtu.be links — saved daily",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedCategory == "Daily" && recentPastedVideos.isNotEmpty()) {
                            IconButton(
                                onClick = onClearRecentPastes,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Clear daily history",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (inputError != null) {
                        Text(
                            text = inputError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                val labelText = if (cat == "Daily" && recentPastedVideos.isNotEmpty()) {
                                    "Daily (${recentPastedVideos.size})"
                                } else cat
                                Text(labelText)
                            },
                            modifier = Modifier.testTag("youtube_cat_${cat.lowercase()}")
                        )
                    }
                }
            }

            if (displayedVideos.isEmpty()) {
                item {
                    EmptyMediaStateCard(
                        title = if (selectedCategory == "Daily") "No Daily Pasted Links Yet" else "No Saved YouTube Videos",
                        subtitle = if (selectedCategory == "Daily") {
                            "Paste any YouTube URL above or tap the clipboard icon to save & watch it here."
                        } else {
                            "Tap the bookmark icon on any stream to save it here for quick access."
                        }
                    )
                }
            } else {
                items(displayedVideos, key = { it.videoId }) { video ->
                    YouTubeCardItem(
                        video = video,
                        isPlaying = video.videoId == activeVideo.videoId,
                        onRemoveRecent = if (video.addedDateLabel.isNotBlank()) {
                            { onRemoveRecentPaste(video.videoId) }
                        } else null,
                        onClick = { onSelectVideo(video) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeIframeWebView(
    videoId: String,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onHideCustomView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view != null && callback != null) {
                        onShowCustomView(view, callback)
                    }
                }

                override fun onHideCustomView() {
                    onHideCustomView()
                }
            }
        }
    }

    val html = remember(videoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <style>
                html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
                iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 0; }
            </style>
        </head>
        <body>
            <iframe
                src="https://www.youtube.com/embed/$videoId?playsinline=1&autoplay=1&rel=0&modestbranding=1"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
                allowfullscreen>
            </iframe>
        </body>
        </html>
        """.trimIndent()
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        update = { view ->
            if (view.tag != videoId) {
                view.tag = videoId
                view.loadDataWithBaseURL(
                    "https://www.youtube.com",
                    html,
                    "text/html",
                    "utf-8",
                    null
                )
            }
        },
        modifier = modifier
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeWebBrowserView(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onVideoCaptured: (String) -> Unit,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onHideCustomView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentUrl by remember { mutableStateOf("https://m.youtube.com") }

    val webView = remember(context) {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString().orEmpty()
                    currentUrl = url
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url != null) currentUrl = url
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view != null && callback != null) {
                        onShowCustomView(view, callback)
                    }
                }

                override fun onHideCustomView() {
                    onHideCustomView()
                }
            }
            loadUrl("https://m.youtube.com")
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search YouTube...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val q = Uri.encode(searchQuery.trim())
                        if (q.isNotEmpty()) {
                            webView.loadUrl("https://m.youtube.com/results?search_query=$q")
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("youtube_web_search_input")
            )
            IconButton(
                onClick = {
                    val q = Uri.encode(searchQuery.trim())
                    if (q.isNotEmpty()) {
                        webView.loadUrl("https://m.youtube.com/results?search_query=$q")
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Search YouTube")
            }

            val detectedId = MediaCatalog.extractYouTubeVideoId(currentUrl)
            if (detectedId != null) {
                Button(
                    onClick = { onVideoCaptured(detectedId) },
                    colors = ButtonDefaults.buttonColors(containerColor = YoutubeRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Pin", color = Color.White)
                }
            }
        }

        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
private fun YouTubeCardItem(
    video: YouTubeVideo,
    isPlaying: Boolean,
    onRemoveRecent: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isPlaying) {
            YoutubeRed.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isPlaying) 1.dp else 0.5.dp,
                color = if (isPlaying) YoutubeRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("youtube_item_${video.videoId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(video.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = if (video.isLive) YoutubeRed else Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = video.durationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val meta = buildString {
                    append(video.channel)
                    append(" • ")
                    append(video.category)
                    if (video.addedDateLabel.isNotBlank()) {
                        append(" • ")
                        append(video.addedDateLabel)
                    }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onRemoveRecent != null) {
                IconButton(onClick = onRemoveRecent) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove from daily history",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
