package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.FolderOpen
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.MediaPlaylist
import com.example.data.MediaSourceType
import com.example.data.VideoItem
import com.example.player.VideoPlayerState
import com.example.player.VideoResizeOption

private enum class VideoFilter(val label: String) {
    ALL("All"),
    ONLINE("Online"),
    LOCAL("Device")
}

@Composable
fun VideosScreen(
    videos: List<VideoItem>,
    playlists: List<MediaPlaylist>,
    videoState: VideoPlayerState,
    exoPlayer: ExoPlayer,
    isFullscreen: Boolean,
    onSetFullscreen: (Boolean) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onNextVideo: () -> Unit,
    onPreviousVideo: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onCycleResizeMode: () -> Unit,
    onAddLocalVideo: (Uri) -> Unit,
    onAddCustomVideoStream: (String, String) -> Unit,
    onAddVideoToPlaylist: (playlistId: String, VideoItem) -> Unit,
    onCreatePlaylistWithVideo: (name: String, VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable { mutableStateOf(VideoFilter.ALL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAddStreamDialog by remember { mutableStateOf(false) }
    var videoForPlaylist by remember { mutableStateOf<VideoItem?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onAddLocalVideo)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(onAddLocalVideo)
    }

    val filteredVideos = remember(videos, selectedFilter, searchQuery) {
        val bySource = when (selectedFilter) {
            VideoFilter.ALL -> videos
            VideoFilter.ONLINE -> videos.filter { it.sourceType == MediaSourceType.ONLINE_STREAM }
            VideoFilter.LOCAL -> videos.filter { it.sourceType == MediaSourceType.LOCAL_DEVICE }
        }
        val q = searchQuery.trim()
        if (q.isEmpty()) bySource
        else bySource.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.creator.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true)
        }
    }

    if (isFullscreen && videoState.currentVideo != null) {
        val context = LocalContext.current
        val view = LocalView.current
        var forceLandscape by remember { mutableStateOf(false) }

        DisposableEffect(forceLandscape) {
            val activity = context as? Activity
            val window = activity?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
            if (activity != null) {
                activity.requestedOrientation = if (forceLandscape) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
            onDispose {
                if (window != null) {
                    WindowCompat.getInsetsController(window, view)
                        .show(WindowInsetsCompat.Type.systemBars())
                }
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        BackHandler { onSetFullscreen(false) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ExoPlayerSurface(
                exoPlayer = exoPlayer,
                resizeOption = videoState.resizeOption,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onCycleResizeMode,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("fullscreen_resize_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AspectRatio,
                        contentDescription = "Cycle aspect ratio",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = { forceLandscape = !forceLandscape },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("fullscreen_rotate_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ScreenRotation,
                        contentDescription = "Toggle landscape",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = { onSetFullscreen(false) },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("exit_video_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FullscreenExit,
                        contentDescription = stringResource(R.string.cd_fullscreen),
                        tint = Color.White
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (videoState.currentVideo != null) {
                ActiveVideoPlayerCard(
                    video = videoState.currentVideo,
                    videoState = videoState,
                    exoPlayer = exoPlayer,
                    onNextVideo = onNextVideo,
                    onPreviousVideo = onPreviousVideo,
                    onSeekBy = onSeekBy,
                    onSetSpeed = onSetSpeed,
                    onToggleMute = onToggleMute,
                    onCycleResizeMode = onCycleResizeMode,
                    onEnterFullscreen = { onSetFullscreen(true) }
                )
            } else {
                VideoHeroBanner(
                    onWatchFeatured = { videos.firstOrNull()?.let(onPlayVideo) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pick_video_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = { filePickerLauncher.launch(arrayOf("video/*")) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("browse_video_file_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Files", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = { showAddStreamDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_video_stream_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddLink,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("URL", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search videos or streams...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_search_input")
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("video_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        if (filteredVideos.isEmpty()) {
            item {
                EmptyMediaStateCard(
                    title = "No Videos Found",
                    subtitle = "Try another filter or pick a local video from Gallery or Files."
                )
            }
        } else {
            items(filteredVideos, key = { it.id }) { item ->
                val isCurrent = videoState.currentVideo?.id == item.id
                VideoItemCard(
                    video = item,
                    isCurrent = isCurrent,
                    onAddToPlaylist = { videoForPlaylist = item },
                    onClick = { onPlayVideo(item) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showAddStreamDialog) {
        AddStreamDialog(
            title = "Stream MP4 / HLS Video URL",
            hintUrl = "https://example.com/video.mp4",
            onDismiss = { showAddStreamDialog = false },
            onConfirm = { url, name ->
                onAddCustomVideoStream(url, name)
                showAddStreamDialog = false
            }
        )
    }

    videoForPlaylist?.let { targetVideo ->
        AddToPlaylistPickerDialog(
            itemTitle = targetVideo.title,
            playlists = playlists,
            onSelectPlaylist = { playlistId -> onAddVideoToPlaylist(playlistId, targetVideo) },
            onCreateNewPlaylist = { name -> onCreatePlaylistWithVideo(name, targetVideo) },
            onDismiss = { videoForPlaylist = null }
        )
    }
}

@Composable
private fun VideoHeroBanner(onWatchFeatured: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onWatchFeatured() }
            .testTag("video_hero_card")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_video_hero),
                contentDescription = "Cinema showcase",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cinema & HLS Deck",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Play device videos or stream MP4 & HLS online",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    onClick = onWatchFeatured,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play),
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveVideoPlayerCard(
    video: VideoItem,
    videoState: VideoPlayerState,
    exoPlayer: ExoPlayer,
    onNextVideo: () -> Unit,
    onPreviousVideo: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onCycleResizeMode: () -> Unit,
    onEnterFullscreen: () -> Unit
) {
    val speedOptions = remember { listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f) }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
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
                ExoPlayerSurface(
                    exoPlayer = exoPlayer,
                    resizeOption = videoState.resizeOption,
                    modifier = Modifier.fillMaxSize()
                )
                if (videoState.isBuffering) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                IconButton(
                    onClick = onEnterFullscreen,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("enter_video_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = stringResource(R.string.cd_fullscreen),
                        tint = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.creator} • ${video.formatTag}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousVideo, modifier = Modifier.testTag("video_prev_button")) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous video")
                    }
                    IconButton(onClick = { onSeekBy(-10_000L) }, modifier = Modifier.testTag("video_replay_10_button")) {
                        Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10 seconds")
                    }
                    IconButton(onClick = { onSeekBy(10_000L) }, modifier = Modifier.testTag("video_forward_10_button")) {
                        Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds")
                    }
                    IconButton(onClick = onNextVideo, modifier = Modifier.testTag("video_next_button")) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next video")
                    }
                    IconButton(onClick = onToggleMute, modifier = Modifier.testTag("video_mute_button")) {
                        Icon(
                            imageVector = if (videoState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (videoState.isMuted) "Unmute" else "Mute",
                            tint = if (videoState.isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = {
                            val idx = speedOptions.indexOf(videoState.playbackSpeed)
                            onSetSpeed(speedOptions[(idx + 1) % speedOptions.size])
                        },
                        leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("${videoState.playbackSpeed}x") },
                        modifier = Modifier.testTag("video_speed_chip")
                    )
                    FilterChip(
                        selected = false,
                        onClick = onCycleResizeMode,
                        leadingIcon = { Icon(Icons.Filled.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(videoState.resizeOption.label) },
                        modifier = Modifier.testTag("video_resize_chip")
                    )
                }

                if (videoState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = videoState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerSurface(
    exoPlayer: ExoPlayer,
    resizeOption: VideoResizeOption = VideoResizeOption.FIT,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerView = remember(context) {
        PlayerView(context).apply {
            useController = true
            controllerAutoShow = true
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    DisposableEffect(playerView, exoPlayer) {
        playerView.player = exoPlayer
        onDispose {
            playerView.player = null
        }
    }

    AndroidView(
        factory = { playerView },
        update = { view ->
            view.resizeMode = when (resizeOption) {
                VideoResizeOption.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                VideoResizeOption.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                VideoResizeOption.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier
    )
}

@Composable
private fun VideoItemCard(
    video: VideoItem,
    isCurrent: Boolean,
    onAddToPlaylist: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrent) 1.dp else 0.5.dp,
                color = if (isCurrent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("video_item_${video.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (video.thumbnailUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(video.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${video.creator} • ${video.durationLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = video.formatTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier.testTag("add_video_to_playlist_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
