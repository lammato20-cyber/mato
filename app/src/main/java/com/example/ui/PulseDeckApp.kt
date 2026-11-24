package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.GameType
import com.example.data.toPlaylistItem
import com.example.player.MusicPlayerState
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.EqualizerSheet
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.screens.YouTubeScreen
import com.example.ui.theme.ArcadeMint
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.StreamCyan
import com.example.ui.theme.YoutubeRed
import com.example.viewmodel.MainViewModel
import java.util.UUID

const val ROUTE_MUSIC = "music"
const val ROUTE_VIDEOS = "videos"
const val ROUTE_YOUTUBE = "youtube"
const val ROUTE_PLAYLISTS = "playlists"
const val ROUTE_GAMES = "games"
const val ROUTE_STUDIO = "studio"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val accent: Color
)

@Composable
fun PulseDeckApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_MUSIC

    val musicTracks by viewModel.musicTracks.collectAsStateWithLifecycle()
    val musicState by viewModel.musicState.collectAsStateWithLifecycle()
    val videoItems by viewModel.videoItems.collectAsStateWithLifecycle()
    val videoState by viewModel.videoState.collectAsStateWithLifecycle()
    val isVideoFullscreen by viewModel.isVideoFullscreen.collectAsStateWithLifecycle()
    val activeYouTube by viewModel.activeYouTubeVideo.collectAsStateWithLifecycle()
    val savedYouTube by viewModel.savedYouTubeVideos.collectAsStateWithLifecycle()
    val recentYouTubePastes by viewModel.recentYouTubePastes.collectAsStateWithLifecycle()
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()
    val highScores by viewModel.highScores.collectAsStateWithLifecycle()
    val cityRacerProgress by viewModel.cityRacerProgress.collectAsStateWithLifecycle()
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsStateWithLifecycle()
    val imageGenState by viewModel.imageGenState.collectAsStateWithLifecycle()
    val musicGenState by viewModel.musicGenState.collectAsStateWithLifecycle()
    val transcribeState by viewModel.transcribeState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val storyboardState by viewModel.storyboardState.collectAsStateWithLifecycle()

    var showFullMusicPlayer by rememberSaveable { mutableStateOf(false) }
    var showEqualizer by rememberSaveable { mutableStateOf(false) }

    val bottomTabs = listOf(
        NavTab(ROUTE_MUSIC, R.string.tab_music, Icons.Filled.MusicNote, Icons.Outlined.MusicNote, PulseCoral),
        NavTab(ROUTE_VIDEOS, R.string.tab_videos, Icons.Filled.Movie, Icons.Outlined.Movie, StreamCyan),
        NavTab(ROUTE_YOUTUBE, R.string.tab_youtube, Icons.Filled.SmartDisplay, Icons.Outlined.SmartDisplay, YoutubeRed),
        NavTab(ROUTE_PLAYLISTS, R.string.tab_playlists, Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic, NeonAmber),
        NavTab(ROUTE_GAMES, R.string.tab_games, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, ArcadeMint)
    )
    val railTabs = bottomTabs + NavTab(
        ROUTE_STUDIO,
        R.string.tab_studio,
        Icons.Filled.AutoAwesome,
        Icons.Outlined.AutoAwesome,
        Color(0xFFA855F7)
    )

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val isFullGameShell = currentRoute == ROUTE_GAMES && activeGame == GameType.CITY_RACER
        val showChrome = !isVideoFullscreen && !isFullGameShell

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && showChrome) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                ) {
                    Spacer(modifier = Modifier.size(12.dp))
                    railTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTo(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            modifier = Modifier.testTag("nav_${tab.route}")
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    if (showChrome && !showFullMusicPlayer) {
                        val activeAccent = railTabs.firstOrNull { it.route == currentRoute }?.accent ?: PulseCoral
                        PulseTopBar(
                            darkTheme = darkTheme,
                            onToggleTheme = { viewModel.toggleDarkTheme() },
                            isStudioActive = currentRoute == ROUTE_STUDIO,
                            onOpenStudio = { navigateTo(ROUTE_STUDIO) },
                            activeAccent = activeAccent
                        )
                    }
                },
                bottomBar = {
                    if (showChrome && !showFullMusicPlayer) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            AnimatedVisibility(visible = musicState.currentTrack != null) {
                                MiniMusicBar(
                                    state = musicState,
                                    onTogglePlayPause = { viewModel.toggleMusicPlayPause() },
                                    onNext = { viewModel.playNextMusic() },
                                    onOpenEq = { showEqualizer = true },
                                    onOpen = {
                                        showFullMusicPlayer = true
                                        navigateTo(ROUTE_MUSIC)
                                    }
                                )
                            }

                            if (!useRail) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    bottomTabs.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTo(tab.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = stringResource(tab.labelRes)
                                                )
                                            },
                                            label = { Text(stringResource(tab.labelRes)) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = tab.accent,
                                                selectedTextColor = tab.accent,
                                                indicatorColor = tab.accent.copy(alpha = 0.16f)
                                            ),
                                            modifier = Modifier.testTag("nav_${tab.route}")
                                        )
                                    }
                                }
                            } else {
                                Spacer(
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_MUSIC,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(ROUTE_MUSIC) {
                        MusicScreen(
                            tracks = musicTracks,
                            playlists = playlists,
                            musicState = musicState,
                            onPlayTrack = { viewModel.playMusic(it) },
                            onTogglePlayPause = { viewModel.toggleMusicPlayPause() },
                            onNext = { viewModel.playNextMusic() },
                            onPrevious = { viewModel.playPreviousMusic() },
                            onSeek = { viewModel.seekMusic(it) },
                            onSetSpeed = { viewModel.setMusicSpeed(it) },
                            onOpenEqualizer = { showEqualizer = true },
                            onSetSleepTimer = { viewModel.setSleepTimerMinutes(it) },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onCycleRepeat = { viewModel.cycleRepeatMode() },
                            onAddLocalAudio = { viewModel.addLocalAudioUris(it) },
                            onAddCustomStream = { url, title -> viewModel.addCustomAudioStream(url, title) },
                            onAddTrackToPlaylist = { playlistId, track ->
                                viewModel.addMusicTrackToPlaylist(playlistId, track)
                            },
                            onCreatePlaylistWithTrack = { name, track ->
                                viewModel.createPlaylist(
                                    name = name,
                                    initialItem = track.toPlaylistItem(UUID.randomUUID().toString())
                                )
                            },
                            showFullPlayer = showFullMusicPlayer,
                            onSetFullPlayer = { showFullMusicPlayer = it }
                        )
                    }
                    composable(ROUTE_VIDEOS) {
                        VideosScreen(
                            videos = videoItems,
                            playlists = playlists,
                            videoState = videoState,
                            exoPlayer = viewModel.mediaManager.videoPlayer,
                            isFullscreen = isVideoFullscreen,
                            onSetFullscreen = { viewModel.setVideoFullscreen(it) },
                            onPlayVideo = { viewModel.playVideo(it) },
                            onNextVideo = { viewModel.playNextVideo() },
                            onPreviousVideo = { viewModel.playPreviousVideo() },
                            onSeekBy = { viewModel.seekVideoBy(it) },
                            onSetSpeed = { viewModel.setVideoSpeed(it) },
                            onToggleMute = { viewModel.toggleVideoMute() },
                            onCycleResizeMode = { viewModel.cycleVideoResizeOption() },
                            onAddLocalVideo = { viewModel.addLocalVideoUri(it) },
                            onAddCustomVideoStream = { url, title -> viewModel.addCustomVideoStream(url, title) },
                            onAddVideoToPlaylist = { playlistId, video ->
                                viewModel.addVideoItemToPlaylist(playlistId, video)
                            },
                            onCreatePlaylistWithVideo = { name, video ->
                                viewModel.createPlaylist(
                                    name = name,
                                    initialItem = video.toPlaylistItem(UUID.randomUUID().toString())
                                )
                            }
                        )
                    }
                    composable(ROUTE_YOUTUBE) {
                        YouTubeScreen(
                            activeVideo = activeYouTube,
                            savedVideos = savedYouTube,
                            recentPastedVideos = recentYouTubePastes,
                            onSelectVideo = { viewModel.selectYouTubeVideo(it) },
                            onPlayUrlOrId = { viewModel.playYouTubeFromInput(it) },
                            onToggleBookmark = { viewModel.toggleBookmarkYouTube(it) },
                            onRemoveRecentPaste = { viewModel.removeRecentYouTubePaste(it) },
                            onClearRecentPastes = { viewModel.clearRecentYouTubePastes() },
                            onPauseBackgroundMedia = { viewModel.mediaManager.pauseAllForExternalMedia() }
                        )
                    }
                    composable(ROUTE_PLAYLISTS) {
                        if (isVideoFullscreen && videoState.currentVideo != null) {
                            VideosScreen(
                                videos = videoItems,
                                playlists = playlists,
                                videoState = videoState,
                                exoPlayer = viewModel.mediaManager.videoPlayer,
                                isFullscreen = true,
                                onSetFullscreen = { viewModel.setVideoFullscreen(it) },
                                onPlayVideo = { viewModel.playVideo(it) },
                                onNextVideo = { viewModel.playNextVideo() },
                                onPreviousVideo = { viewModel.playPreviousVideo() },
                                onSeekBy = { viewModel.seekVideoBy(it) },
                                onSetSpeed = { viewModel.setVideoSpeed(it) },
                                onToggleMute = { viewModel.toggleVideoMute() },
                                onCycleResizeMode = { viewModel.cycleVideoResizeOption() },
                                onAddLocalVideo = { viewModel.addLocalVideoUri(it) },
                                onAddCustomVideoStream = { url, title -> viewModel.addCustomVideoStream(url, title) },
                                onAddVideoToPlaylist = { playlistId, video ->
                                    viewModel.addVideoItemToPlaylist(playlistId, video)
                                },
                                onCreatePlaylistWithVideo = { name, video ->
                                    viewModel.createPlaylist(
                                        name = name,
                                        initialItem = video.toPlaylistItem(UUID.randomUUID().toString())
                                    )
                                }
                            )
                        } else {
                            PlaylistsScreen(
                                playlists = playlists,
                                selectedPlaylistId = selectedPlaylistId,
                                allMusicTracks = musicTracks,
                                allVideoItems = videoItems,
                                musicState = musicState,
                                videoState = videoState,
                                videoExoPlayer = viewModel.mediaManager.videoPlayer,
                                onSelectPlaylist = { viewModel.selectPlaylist(it) },
                                onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                                onRenamePlaylist = { id, name, desc -> viewModel.renamePlaylist(id, name, desc) },
                                onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                                onAddMusicToPlaylist = { id, track -> viewModel.addMusicTrackToPlaylist(id, track) },
                                onAddVideoToPlaylist = { id, video -> viewModel.addVideoItemToPlaylist(id, video) },
                                onAddLocalAudioToPlaylist = { id, uris -> viewModel.addLocalAudioToPlaylist(id, uris) },
                                onAddLocalVideoToPlaylist = { id, uri -> viewModel.addLocalVideoToPlaylist(id, uri) },
                                onRemovePlaylistItem = { id, itemId -> viewModel.removePlaylistItem(id, itemId) },
                                onMovePlaylistItem = { id, from, to -> viewModel.movePlaylistItem(id, from, to) },
                                onPlayPlaylistItem = { pl, item -> viewModel.playPlaylistItem(pl, item) },
                                onPlayPlaylistFromStart = { pl -> viewModel.playPlaylistFromStart(pl) },
                                onEnterVideoFullscreen = { viewModel.setVideoFullscreen(true) }
                            )
                        }
                    }
                    composable(ROUTE_GAMES) {
                        GamesScreen(
                            activeGame = activeGame,
                            highScores = highScores,
                            cityRacerProgress = cityRacerProgress,
                            onSaveCityRacerProgress = { viewModel.saveCityRacerProgress(it) },
                            onLaunchGame = { viewModel.launchGame(it) },
                            onCloseGame = { viewModel.closeGame() },
                            onReportScore = { game, score -> viewModel.reportGameScore(game, score) }
                        )
                    }
                    composable(ROUTE_STUDIO) {
                        AiStudioScreen(
                            imageState = imageGenState,
                            musicGenState = musicGenState,
                            transcribeState = transcribeState,
                            searchState = searchState,
                            storyboardState = storyboardState,
                            onGenerateOrEditImage = { prompt, bmp ->
                                viewModel.generateOrEditImage(prompt, bmp)
                            },
                            onComposeMusic = { prompt, dur ->
                                viewModel.composeAiMusicTrack(prompt, dur)
                            },
                            onPlayTrack = { track ->
                                viewModel.playMusic(track)
                            },
                            onTranscribeWav = { wav ->
                                viewModel.transcribeRecordedWav(wav)
                            },
                            onAskWithSearch = { query ->
                                viewModel.askWithSearch(query)
                            },
                            onGenerateStoryboard = { prompt, bmp, landscape ->
                                viewModel.generateStoryboard(prompt, bmp, landscape)
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = showEqualizer) {
            val activeAccent = musicState.currentTrack?.let { Color(it.accentHex) } ?: PulseCoral
            EqualizerSheet(
                settings = musicState.eqSettings,
                isPlaying = musicState.isPlaying,
                trackTitle = musicState.currentTrack?.title,
                accentColor = activeAccent,
                onSetEnabled = { viewModel.setEqEnabled(it) },
                onSetPreset = { viewModel.setEqPreset(it) },
                onSetBandLevel = { band, level -> viewModel.setEqBandLevel(band, level) },
                onSetBassBoost = { viewModel.setBassBoostStrength(it) },
                onSetVirtualizer = { viewModel.setVirtualizerStrength(it) },
                onSetLoudnessGain = { viewModel.setLoudnessGainMb(it) },
                onSetReverb = { viewModel.setReverbOption(it) },
                onReset = { viewModel.resetEqualizer() },
                onClose = { showEqualizer = false }
            )
        }
    }
}

@Composable
private fun PulseTopBar(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    isStudioActive: Boolean,
    onOpenStudio: () -> Unit,
    activeAccent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(activeAccent)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "PulseDeck",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                onClick = onOpenStudio,
                shape = RoundedCornerShape(16.dp),
                color = if (isStudioActive) {
                    Color(0xFFA855F7).copy(alpha = 0.24f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier
                    .border(
                        1.dp,
                        Color(0xFFA855F7).copy(alpha = if (isStudioActive) 0.85f else 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("nav_studio")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = stringResource(R.string.tab_studio),
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.tab_studio),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MiniMusicBar(
    state: MusicPlayerState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenEq: () -> Unit,
    onOpen: () -> Unit
) {
    val track = state.currentTrack ?: return
    val accent = Color(track.accentHex)

    Surface(
        onClick = onOpen,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = accent.copy(alpha = 0.45f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .testTag("mini_music_bar")
    ) {
        Column {
            val progress = if (state.durationMs > 0L) {
                (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progress },
                color = accent,
                trackColor = accent.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accent.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onOpenEq,
                    modifier = Modifier.testTag("mini_bar_eq")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Equalizer,
                        contentDescription = "Open Equalizer",
                        tint = if (state.eqSettings.enabled && state.eqSettings.preset != com.example.player.EqPreset.FLAT) {
                            accent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.testTag("mini_bar_play_pause")
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play),
                        tint = accent
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("mini_bar_next")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.cd_next)
                    )
                }
            }
        }
    }
}
