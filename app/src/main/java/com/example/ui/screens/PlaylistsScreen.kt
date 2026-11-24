package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.MediaPlaylist
import com.example.data.MusicTrack
import com.example.data.PlaylistItem
import com.example.data.PlaylistItemType
import com.example.data.VideoItem
import com.example.player.MusicPlayerState
import com.example.player.VideoPlayerState
import com.example.ui.theme.ArcadeMint
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.StreamCyan

@Composable
fun PlaylistsScreen(
    playlists: List<MediaPlaylist>,
    selectedPlaylistId: String?,
    allMusicTracks: List<MusicTrack>,
    allVideoItems: List<VideoItem>,
    musicState: MusicPlayerState,
    videoState: VideoPlayerState,
    videoExoPlayer: ExoPlayer,
    onSelectPlaylist: (String?) -> Unit,
    onCreatePlaylist: (name: String, description: String) -> Unit,
    onRenamePlaylist: (playlistId: String, name: String, description: String) -> Unit,
    onDeletePlaylist: (playlistId: String) -> Unit,
    onAddMusicToPlaylist: (playlistId: String, track: MusicTrack) -> Unit,
    onAddVideoToPlaylist: (playlistId: String, video: VideoItem) -> Unit,
    onAddLocalAudioToPlaylist: (playlistId: String, uris: List<Uri>) -> Unit,
    onAddLocalVideoToPlaylist: (playlistId: String, uri: Uri) -> Unit,
    onRemovePlaylistItem: (playlistId: String, itemId: String) -> Unit,
    onMovePlaylistItem: (playlistId: String, fromIndex: Int, toIndex: Int) -> Unit,
    onPlayPlaylistItem: (MediaPlaylist, PlaylistItem) -> Unit,
    onPlayPlaylistFromStart: (MediaPlaylist) -> Unit,
    onEnterVideoFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPlaylist = remember(playlists, selectedPlaylistId) {
        playlists.find { it.id == selectedPlaylistId }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<MediaPlaylist?>(null) }

    if (selectedPlaylist != null) {
        BackHandler { onSelectPlaylist(null) }
        PlaylistDetailView(
            playlist = selectedPlaylist,
            allMusicTracks = allMusicTracks,
            allVideoItems = allVideoItems,
            musicState = musicState,
            videoState = videoState,
            videoExoPlayer = videoExoPlayer,
            onBack = { onSelectPlaylist(null) },
            onRenameClick = { editingPlaylist = selectedPlaylist },
            onAddMusic = { track -> onAddMusicToPlaylist(selectedPlaylist.id, track) },
            onAddVideo = { video -> onAddVideoToPlaylist(selectedPlaylist.id, video) },
            onAddLocalAudio = { uris -> onAddLocalAudioToPlaylist(selectedPlaylist.id, uris) },
            onAddLocalVideo = { uri -> onAddLocalVideoToPlaylist(selectedPlaylist.id, uri) },
            onRemoveItem = { itemId -> onRemovePlaylistItem(selectedPlaylist.id, itemId) },
            onMoveItem = { from, to -> onMovePlaylistItem(selectedPlaylist.id, from, to) },
            onPlayItem = { item -> onPlayPlaylistItem(selectedPlaylist, item) },
            onPlayAll = { onPlayPlaylistFromStart(selectedPlaylist) },
            onEnterVideoFullscreen = onEnterVideoFullscreen,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PlaylistHubBanner(
                    playlistCount = playlists.size,
                    totalItems = playlists.sumOf { it.items.size },
                    onCreateClick = { showCreateDialog = true }
                )
            }

            if (playlists.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QueueMusic,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "No Saved Playlists Yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Create a custom mix combining your favorite music tracks and videos. Your playlists stay saved automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonAmber,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("empty_create_playlist_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First Playlist")
                            }
                        }
                    }
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistSummaryCard(
                        playlist = playlist,
                        onOpen = { onSelectPlaylist(playlist.id) },
                        onPlayAll = { onPlayPlaylistFromStart(playlist) },
                        onRename = { editingPlaylist = playlist },
                        onDelete = { onDeletePlaylist(playlist.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showCreateDialog) {
        PlaylistFormDialog(
            dialogTitle = "New Playlist",
            initialName = "",
            initialDescription = "",
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc ->
                onCreatePlaylist(name, desc)
                showCreateDialog = false
            }
        )
    }

    editingPlaylist?.let { target ->
        PlaylistFormDialog(
            dialogTitle = "Edit Playlist",
            initialName = target.name,
            initialDescription = target.description,
            confirmLabel = "Save",
            onDismiss = { editingPlaylist = null },
            onConfirm = { name, desc ->
                onRenamePlaylist(target.id, name, desc)
                editingPlaylist = null
            }
        )
    }
}

@Composable
private fun PlaylistHubBanner(
    playlistCount: Int,
    totalItems: Int,
    onCreateClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                NeonAmber.copy(alpha = 0.45f),
                RoundedCornerShape(22.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            NeonAmber.copy(alpha = 0.22f),
                            PulseCoral.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MIXED MEDIA PLAYLISTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your Saved Collections",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "$playlistCount playlists • $totalItems saved items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onCreateClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("create_playlist_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New")
                }
            }
        }
    }
}

@Composable
private fun PlaylistSummaryCard(
    playlist: MediaPlaylist,
    onOpen: () -> Unit,
    onPlayAll: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val musicCount = playlist.items.count { it.itemType == PlaylistItemType.MUSIC }
    val videoCount = playlist.items.count { it.itemType == PlaylistItemType.VIDEO }

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.75.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                RoundedCornerShape(18.dp)
            )
            .testTag("playlist_card_${playlist.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = NeonAmber.copy(alpha = 0.18f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (playlist.description.isNotBlank()) {
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$musicCount audio • $videoCount video",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArcadeMint
                )
            }

            if (playlist.items.isNotEmpty()) {
                IconButton(
                    onClick = onPlayAll,
                    modifier = Modifier.testTag("play_playlist_${playlist.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play),
                        tint = NeonAmber
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("playlist_menu_${playlist.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Playlist options"
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename / Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Playlist", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: MediaPlaylist,
    allMusicTracks: List<MusicTrack>,
    allVideoItems: List<VideoItem>,
    musicState: MusicPlayerState,
    videoState: VideoPlayerState,
    videoExoPlayer: ExoPlayer,
    onBack: () -> Unit,
    onRenameClick: () -> Unit,
    onAddMusic: (MusicTrack) -> Unit,
    onAddVideo: (VideoItem) -> Unit,
    onAddLocalAudio: (List<Uri>) -> Unit,
    onAddLocalVideo: (Uri) -> Unit,
    onRemoveItem: (String) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onPlayItem: (PlaylistItem) -> Unit,
    onPlayAll: () -> Unit,
    onEnterVideoFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddMediaSheet by remember { mutableStateOf(false) }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onAddLocalAudio(uris)
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onAddLocalVideo)
    }

    // Track if active video belongs to this playlist
    val activePlaylistVideo = remember(videoState.currentVideo, playlist.items) {
        val currentUri = videoState.currentVideo?.uriString ?: return@remember null
        playlist.items.find { it.itemType == PlaylistItemType.VIDEO && it.uriString == currentUri }
    }

    // Drag-to-reorder state
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 84.dp.toPx() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("playlist_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlist.items.size} items • Drag handle or arrows to reorder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRenameClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit playlist")
                }
                if (playlist.items.isNotEmpty()) {
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("playlist_play_all_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play")
                    }
                }
            }
        }

        if (activePlaylistVideo != null) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StreamCyan.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black)
                        ) {
                            ExoPlayerSurface(
                                exoPlayer = videoExoPlayer,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (videoState.isBuffering) {
                                CircularProgressIndicator(
                                    color = StreamCyan,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            IconButton(
                                onClick = onEnterVideoFullscreen,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Filled.Fullscreen,
                                    contentDescription = stringResource(R.string.cd_fullscreen),
                                    tint = Color.White
                                )
                            }
                        }
                        Text(
                            text = "Playing Video: ${activePlaylistVideo.title}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddMediaSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("playlist_add_from_library_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Library", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = { audioPicker.launch(arrayOf("audio/*")) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("playlist_pick_audio_button")
                ) {
                    Icon(Icons.Outlined.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Audio", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("playlist_pick_video_button")
                ) {
                    Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Video", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (playlist.items.isEmpty()) {
            item {
                EmptyMediaStateCard(
                    title = "Playlist Is Empty",
                    subtitle = "Tap Library, Audio, or Video above to add music and video files."
                )
            }
        } else {
            itemsIndexed(playlist.items, key = { _, item -> item.id }) { index, item ->
                val isDragging = draggedIndex == index
                val isPlayingItem = when (item.itemType) {
                    PlaylistItemType.MUSIC -> musicState.currentTrack?.uriString == item.uriString && musicState.isPlaying
                    PlaylistItemType.VIDEO -> videoState.currentVideo?.uriString == item.uriString && videoState.isPlaying
                }

                PlaylistEntryRow(
                    index = index,
                    totalCount = playlist.items.size,
                    item = item,
                    isPlaying = isPlayingItem,
                    isDragging = isDragging,
                    translationY = if (isDragging) dragOffsetY else 0f,
                    onPlay = { onPlayItem(item) },
                    onMoveUp = {
                        if (index > 0) onMoveItem(index, index - 1)
                    },
                    onMoveDown = {
                        if (index < playlist.items.lastIndex) onMoveItem(index, index + 1)
                    },
                    onRemove = { onRemoveItem(item.id) },
                    onDragStart = {
                        draggedIndex = index
                        dragOffsetY = 0f
                    },
                    onDragDelta = { deltaY ->
                        val activeIdx = draggedIndex ?: return@PlaylistEntryRow
                        dragOffsetY += deltaY
                        if (dragOffsetY > rowHeightPx * 0.65f && activeIdx < playlist.items.lastIndex) {
                            onMoveItem(activeIdx, activeIdx + 1)
                            draggedIndex = activeIdx + 1
                            dragOffsetY -= rowHeightPx
                        } else if (dragOffsetY < -rowHeightPx * 0.65f && activeIdx > 0) {
                            onMoveItem(activeIdx, activeIdx - 1)
                            draggedIndex = activeIdx - 1
                            dragOffsetY += rowHeightPx
                        }
                    },
                    onDragEnd = {
                        draggedIndex = null
                        dragOffsetY = 0f
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showAddMediaSheet) {
        AddLibraryMediaToPlaylistDialog(
            playlist = playlist,
            musicTracks = allMusicTracks,
            videoItems = allVideoItems,
            onAddMusic = onAddMusic,
            onAddVideo = onAddVideo,
            onDismiss = { showAddMediaSheet = false }
        )
    }
}

@Composable
private fun PlaylistEntryRow(
    index: Int,
    totalCount: Int,
    item: PlaylistItem,
    isPlaying: Boolean,
    isDragging: Boolean,
    translationY: Float,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val context = LocalContext.current
    val accent = if (item.itemType == PlaylistItemType.VIDEO) StreamCyan else Color(item.accentHex)

    Surface(
        onClick = onPlay,
        shape = RoundedCornerShape(16.dp),
        color = when {
            isDragging -> MaterialTheme.colorScheme.surfaceVariant
            isPlaying -> accent.copy(alpha = 0.16f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        },
        shadowElevation = if (isDragging) 10.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 2f else 0f)
            .graphicsLayer { this.translationY = translationY }
            .border(
                width = if (isDragging || isPlaying) 1.2.dp else 0.5.dp,
                color = if (isDragging || isPlaying) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("playlist_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .pointerInput(index, totalCount) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount.y)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    }
                    .testTag("drag_handle_${item.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnailUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (item.itemType == PlaylistItemType.VIDEO) {
                            Icons.Filled.Movie
                        } else {
                            Icons.Filled.MusicNote
                        },
                        contentDescription = null,
                        tint = accent
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.itemType == PlaylistItemType.VIDEO) "VIDEO" else "AUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("move_up_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Move up",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("move_down_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Move down",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.testTag("remove_playlist_item_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Remove from playlist",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun AddLibraryMediaToPlaylistDialog(
    playlist: MediaPlaylist,
    musicTracks: List<MusicTrack>,
    videoItems: List<VideoItem>,
    onAddMusic: (MusicTrack) -> Unit,
    onAddVideo: (VideoItem) -> Unit,
    onDismiss: () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val existingUris = remember(playlist.items) {
        playlist.items.map { it.uriString }.toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${playlist.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) {
                        Text("Music (${musicTracks.size})")
                    }
                    SegmentedButton(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) {
                        Text("Videos (${videoItems.size})")
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tabIndex == 0) {
                        items(musicTracks, key = { it.id }) { track ->
                            val added = track.uriString in existingUris
                            Surface(
                                onClick = { if (!added) onAddMusic(track) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = Color(track.accentHex)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            track.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            track.artist,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Icon(
                                        imageVector = if (added) Icons.Filled.CheckCircle else Icons.Filled.Add,
                                        contentDescription = if (added) "Added" else "Add",
                                        tint = if (added) ArcadeMint else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
                        items(videoItems, key = { it.id }) { video ->
                            val added = video.uriString in existingUris
                            Surface(
                                onClick = { if (!added) onAddVideo(video) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Movie,
                                        contentDescription = null,
                                        tint = StreamCyan
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            video.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            video.creator,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Icon(
                                        imageVector = if (added) Icons.Filled.CheckCircle else Icons.Filled.Add,
                                        contentDescription = if (added) "Added" else "Add",
                                        tint = if (added) ArcadeMint else StreamCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun PlaylistFormDialog(
    dialogTitle: String,
    initialName: String,
    initialDescription: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    placeholder = { Text("Late Night Synth & Clips") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, description) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_playlist_form_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddToPlaylistPickerDialog(
    itemTitle: String,
    playlists: List<MediaPlaylist>,
    onSelectPlaylist: (playlistId: String) -> Unit,
    onCreateNewPlaylist: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add \"$itemTitle\"",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Create new playlist") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_new_playlist_input")
                    )
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreateNewPlaylist(newName)
                                onDismiss()
                            }
                        },
                        enabled = newName.isNotBlank(),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Create and add")
                    }
                }

                if (playlists.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Existing Playlists",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            Surface(
                                onClick = {
                                    onSelectPlaylist(pl.id)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = NeonAmber
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pl.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${pl.items.size} items",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Filled.Add, contentDescription = "Add to ${pl.name}")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
