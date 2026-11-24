package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiStudioRepository
import com.example.ai.SearchGroundedResult
import com.example.ai.StoryboardResult
import com.example.data.CityRacerProgress
import com.example.data.GameType
import com.example.data.HighScores
import com.example.data.MediaCatalog
import com.example.data.MediaPlaylist
import com.example.data.MediaSourceType
import com.example.data.MusicTrack
import com.example.data.PlaylistItem
import com.example.data.PlaylistItemType
import com.example.data.PreferencesRepository
import com.example.data.VideoItem
import com.example.data.YouTubeVideo
import com.example.data.toMusicTrack
import com.example.data.toPlaylistItem
import com.example.data.toVideoItem
import com.example.player.EqPreset
import com.example.player.MusicPlayerState
import com.example.player.PulseMediaManager
import com.example.player.ReverbOption
import com.example.player.VideoPlayerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface AiUiState<out T> {
    data object Idle : AiUiState<Nothing>
    data object Loading : AiUiState<Nothing>
    data class Success<T>(val data: T) : AiUiState<T>
    data class Error(val message: String) : AiUiState<Nothing>
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    val mediaManager = PulseMediaManager(application)
    private val aiRepo = AiStudioRepository()

    private val builtInAndOnlineMusic = MediaCatalog.builtInMusicTracks(application)
    private val _deviceMusicTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    private val _aiGeneratedTracks = MutableStateFlow<List<MusicTrack>>(emptyList())

    val musicTracks: StateFlow<List<MusicTrack>> = combine(
        _aiGeneratedTracks,
        _deviceMusicTracks,
        prefsRepo.customAudioStreamsFlow
    ) { aiTracks, deviceTracks, customStreams ->
        aiTracks + deviceTracks + customStreams + builtInAndOnlineMusic
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = builtInAndOnlineMusic
    )

    private val _deviceVideos = MutableStateFlow<List<VideoItem>>(emptyList())

    val videoItems: StateFlow<List<VideoItem>> = combine(
        _deviceVideos,
        prefsRepo.customVideoStreamsFlow
    ) { deviceVids, customStreams ->
        deviceVids + customStreams + MediaCatalog.sampleOnlineVideos
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MediaCatalog.sampleOnlineVideos
    )

    val musicState: StateFlow<MusicPlayerState> = mediaManager.musicState
    val videoState: StateFlow<VideoPlayerState> = mediaManager.videoState

    val highScores: StateFlow<HighScores> = prefsRepo.highScoresFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HighScores()
    )

    val cityRacerProgress: StateFlow<CityRacerProgress> = prefsRepo.cityRacerProgressFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CityRacerProgress()
    )

    val darkTheme: StateFlow<Boolean> = prefsRepo.darkThemeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    val savedYouTubeVideos: StateFlow<List<YouTubeVideo>> = prefsRepo.savedYouTubeVideosFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val recentYouTubePastes: StateFlow<List<YouTubeVideo>> = prefsRepo.recentYouTubePastesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _activeYouTubeVideo = MutableStateFlow(MediaCatalog.curatedYouTubeVideos.first())
    val activeYouTubeVideo: StateFlow<YouTubeVideo> = _activeYouTubeVideo.asStateFlow()

    private val _activeGame = MutableStateFlow<GameType?>(null)
    val activeGame: StateFlow<GameType?> = _activeGame.asStateFlow()

    private val _isVideoFullscreen = MutableStateFlow(false)
    val isVideoFullscreen: StateFlow<Boolean> = _isVideoFullscreen.asStateFlow()

    private val _imageGenState = MutableStateFlow<AiUiState<Bitmap>>(AiUiState.Idle)
    val imageGenState: StateFlow<AiUiState<Bitmap>> = _imageGenState.asStateFlow()

    private val _searchState = MutableStateFlow<AiUiState<SearchGroundedResult>>(AiUiState.Idle)
    val searchState: StateFlow<AiUiState<SearchGroundedResult>> = _searchState.asStateFlow()

    private val _transcribeState = MutableStateFlow<AiUiState<String>>(AiUiState.Idle)
    val transcribeState: StateFlow<AiUiState<String>> = _transcribeState.asStateFlow()

    private val _musicGenState = MutableStateFlow<AiUiState<MusicTrack>>(AiUiState.Idle)
    val musicGenState: StateFlow<AiUiState<MusicTrack>> = _musicGenState.asStateFlow()

    private val _storyboardState = MutableStateFlow<AiUiState<StoryboardResult>>(AiUiState.Idle)
    val storyboardState: StateFlow<AiUiState<StoryboardResult>> = _storyboardState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedEq = prefsRepo.equalizerSettingsFlow.first()
            mediaManager.applyEqualizerSettings(savedEq)
        }
    }

    fun toggleDarkTheme() {
        viewModelScope.launch {
            prefsRepo.setDarkTheme(!darkTheme.value)
        }
    }

    fun playMusic(track: MusicTrack) {
        mediaManager.playMusicTrack(track, musicTracks.value)
    }

    fun toggleMusicPlayPause() {
        val currentQueue = musicTracks.value
        if (musicState.value.currentTrack == null && currentQueue.isNotEmpty()) {
            mediaManager.playMusicTrack(currentQueue.first(), currentQueue)
        } else {
            mediaManager.toggleMusicPlayPause()
        }
    }

    fun playNextMusic() = mediaManager.playNextTrack(musicTracks.value)

    fun playPreviousMusic() = mediaManager.playPreviousTrack(musicTracks.value)

    fun seekMusic(ms: Long) = mediaManager.seekMusicTo(ms)

    fun setMusicSpeed(speed: Float) = mediaManager.setMusicSpeed(speed)

    fun setEqEnabled(enabled: Boolean) {
        mediaManager.setEqEnabled(enabled)
        persistCurrentEq()
    }

    fun setEqPreset(preset: EqPreset) {
        mediaManager.setEqPreset(preset)
        persistCurrentEq()
    }

    fun setEqBandLevel(bandIndex: Int, levelMb: Int) {
        mediaManager.setEqBandLevel(bandIndex, levelMb)
        persistCurrentEq()
    }

    fun setBassBoostStrength(strength: Int) {
        mediaManager.setBassBoostStrength(strength)
        persistCurrentEq()
    }

    fun setVirtualizerStrength(strength: Int) {
        mediaManager.setVirtualizerStrength(strength)
        persistCurrentEq()
    }

    fun setLoudnessGainMb(gainMb: Int) {
        mediaManager.setLoudnessGainMb(gainMb)
        persistCurrentEq()
    }

    fun setReverbOption(reverb: ReverbOption) {
        mediaManager.setReverbOption(reverb)
        persistCurrentEq()
    }

    fun resetEqualizer() {
        mediaManager.resetEqualizer()
        persistCurrentEq()
    }

    private fun persistCurrentEq() {
        val snapshot = mediaManager.musicState.value.eqSettings
        viewModelScope.launch {
            prefsRepo.saveEqualizerSettings(snapshot)
        }
    }

    fun setSleepTimerMinutes(minutes: Int?) = mediaManager.setSleepTimerMinutes(minutes)

    fun toggleShuffle() = mediaManager.toggleShuffle()

    fun cycleRepeatMode() = mediaManager.cycleRepeatMode()

    fun addLocalAudioUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val cr = getApplication<Application>().contentResolver
        val added = uris.map { uri ->
            try {
                cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
            val displayName = resolveDisplayName(uri) ?: "Local Audio"
            MusicTrack(
                id = "local_audio_${uri.toString().hashCode()}",
                title = displayName.substringBeforeLast('.'),
                artist = "Device Audio",
                genre = "Local",
                durationLabel = "File",
                uriString = uri.toString(),
                sourceType = MediaSourceType.LOCAL_DEVICE,
                accentHex = 0xFF00E5A3
            )
        }
        _deviceMusicTracks.update { existing ->
            val existingUris = existing.map { it.uriString }.toSet()
            added.filterNot { it.uriString in existingUris } + existing
        }
        added.firstOrNull()?.let { first ->
            mediaManager.playMusicTrack(first, musicTracks.value)
        }
    }

    fun addCustomAudioStream(url: String, title: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            prefsRepo.addCustomAudioStream(url, title)
        }
    }

    fun playVideo(video: VideoItem) {
        mediaManager.playVideo(video)
    }

    fun setVideoSpeed(speed: Float) = mediaManager.setVideoSpeed(speed)

    fun toggleVideoMute() = mediaManager.toggleVideoMute()

    fun cycleVideoResizeOption() = mediaManager.cycleVideoResizeOption()

    fun seekVideoBy(deltaMs: Long) = mediaManager.seekVideoBy(deltaMs)

    fun playNextVideo() {
        val list = videoItems.value
        if (list.isEmpty()) return
        val currentId = videoState.value.currentVideo?.id
        val idx = list.indexOfFirst { it.id == currentId }
        playVideo(list[(idx + 1) % list.size])
    }

    fun playPreviousVideo() {
        val list = videoItems.value
        if (list.isEmpty()) return
        val currentId = videoState.value.currentVideo?.id
        val idx = list.indexOfFirst { it.id == currentId }
        val prevIdx = if (idx <= 0) list.lastIndex else idx - 1
        playVideo(list[prevIdx])
    }

    fun setVideoFullscreen(fullscreen: Boolean) {
        _isVideoFullscreen.value = fullscreen
    }

    fun addLocalVideoUri(uri: Uri) {
        val cr = getApplication<Application>().contentResolver
        try {
            cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }
        val displayName = resolveDisplayName(uri) ?: "Device Video"
        val item = VideoItem(
            id = "local_vid_${uri.toString().hashCode()}",
            title = displayName.substringBeforeLast('.'),
            creator = "From Device",
            category = "Local",
            durationLabel = "Local",
            uriString = uri.toString(),
            thumbnailUrl = uri.toString(),
            sourceType = MediaSourceType.LOCAL_DEVICE,
            formatTag = "LOCAL"
        )
        _deviceVideos.update { existing ->
            listOf(item) + existing.filterNot { it.uriString == item.uriString }
        }
        playVideo(item)
    }

    fun addCustomVideoStream(url: String, title: String) {
        if (url.isBlank()) return
        val cleanUrl = url.trim()
        viewModelScope.launch {
            prefsRepo.addCustomVideoStream(cleanUrl, title)
        }
        playVideo(
            VideoItem(
                id = "custom_vid_${cleanUrl.hashCode()}",
                title = title.trim().ifEmpty { "Custom Stream" },
                creator = "Online URL",
                category = "Stream",
                durationLabel = "Stream",
                uriString = cleanUrl,
                thumbnailUrl = null,
                sourceType = MediaSourceType.ONLINE_STREAM,
                formatTag = if (cleanUrl.contains(".m3u8", ignoreCase = true)) "HLS" else "STREAM"
            )
        )
    }

    fun selectYouTubeVideo(video: YouTubeVideo) {
        mediaManager.pauseAllForExternalMedia()
        _activeYouTubeVideo.value = video
    }

    fun playYouTubeFromInput(rawInput: String): Boolean {
        val videoId = MediaCatalog.extractYouTubeVideoId(rawInput) ?: return false
        val existing = MediaCatalog.curatedYouTubeVideos.find { it.videoId == videoId }
            ?: savedYouTubeVideos.value.find { it.videoId == videoId }
            ?: recentYouTubePastes.value.find { it.videoId == videoId }
        val todayLabel = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
        val video = existing?.copy(addedDateLabel = todayLabel) ?: YouTubeVideo(
            videoId = videoId,
            title = "YouTube Stream ($videoId)",
            channel = "Pasted Link",
            category = "Daily",
            durationLabel = "HD",
            addedDateLabel = todayLabel
        )
        selectYouTubeVideo(video)
        viewModelScope.launch {
            prefsRepo.recordPastedYouTubeVideo(video, todayLabel)
        }
        return true
    }

    fun removeRecentYouTubePaste(videoId: String) {
        viewModelScope.launch {
            prefsRepo.removeRecentYouTubePaste(videoId)
        }
    }

    fun clearRecentYouTubePastes() {
        viewModelScope.launch {
            prefsRepo.clearRecentYouTubePastes()
        }
    }

    fun toggleBookmarkYouTube(video: YouTubeVideo) {
        viewModelScope.launch {
            prefsRepo.toggleBookmarkYouTube(video)
        }
    }

    fun launchGame(gameType: GameType) {
        _activeGame.value = gameType
    }

    fun closeGame() {
        _activeGame.value = null
    }

    fun reportGameScore(gameType: GameType, score: Int) {
        if (score <= 0) return
        viewModelScope.launch {
            prefsRepo.updateHighScore(gameType, score)
        }
    }

    fun saveCityRacerProgress(progress: CityRacerProgress) {
        viewModelScope.launch {
            prefsRepo.updateCityRacerProgress(progress)
        }
    }

    fun generateOrEditImage(prompt: String, inputBitmap: Bitmap?) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _imageGenState.value = AiUiState.Loading
            try {
                val bitmap = aiRepo.generateOrEditImage(clean, inputBitmap)
                _imageGenState.value = AiUiState.Success(bitmap)
            } catch (e: TimeoutCancellationException) {
                Log.e("AiStudio", "Image generation timed out", e)
                _imageGenState.value = AiUiState.Error("Image generation timed out after 60 seconds. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AiStudio", "Image generation failed", e)
                _imageGenState.value = AiUiState.Error(e.localizedMessage ?: "Image generation failed.")
            }
        }
    }

    fun askWithSearch(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _searchState.value = AiUiState.Loading
            try {
                val result = aiRepo.askWithGoogleSearch(clean)
                _searchState.value = AiUiState.Success(result)
            } catch (e: TimeoutCancellationException) {
                Log.e("AiStudio", "Grounded search timed out", e)
                _searchState.value = AiUiState.Error("Search query timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AiStudio", "Grounded search failed", e)
                _searchState.value = AiUiState.Error(e.localizedMessage ?: "Google Search grounding failed.")
            }
        }
    }

    fun transcribeRecordedWav(wavBytes: ByteArray) {
        if (wavBytes.isEmpty()) {
            _transcribeState.value = AiUiState.Error("No audio captured. Hold or tap Record to speak.")
            return
        }
        viewModelScope.launch {
            _transcribeState.value = AiUiState.Loading
            try {
                val transcript = aiRepo.transcribeAudio(wavBytes)
                _transcribeState.value = AiUiState.Success(transcript)
            } catch (e: TimeoutCancellationException) {
                Log.e("AiStudio", "Audio transcription timed out", e)
                _transcribeState.value = AiUiState.Error("Transcription timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AiStudio", "Audio transcription failed", e)
                _transcribeState.value = AiUiState.Error(e.localizedMessage ?: "Audio transcription failed.")
            }
        }
    }

    fun composeAiMusicTrack(prompt: String, durationSeconds: Int = 18) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _musicGenState.value = AiUiState.Loading
            try {
                val track = aiRepo.composeAndRenderTrack(getApplication(), clean, durationSeconds)
                _aiGeneratedTracks.update { listOf(track) + it }
                _musicGenState.value = AiUiState.Success(track)
                mediaManager.playMusicTrack(track, listOf(track) + musicTracks.value)
            } catch (e: TimeoutCancellationException) {
                Log.e("AiStudio", "AI music composition timed out", e)
                _musicGenState.value = AiUiState.Error("Music generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AiStudio", "AI music composition failed", e)
                _musicGenState.value = AiUiState.Error(e.localizedMessage ?: "Music generation failed.")
            }
        }
    }

    fun generateStoryboard(prompt: String, inputBitmap: Bitmap?, landscape: Boolean) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _storyboardState.value = AiUiState.Loading
            try {
                val result = aiRepo.generateStoryboardAndKeyframe(clean, inputBitmap, landscape)
                _storyboardState.value = AiUiState.Success(result)
            } catch (e: TimeoutCancellationException) {
                Log.e("AiStudio", "Storyboard generation timed out", e)
                _storyboardState.value = AiUiState.Error("Storyboard generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AiStudio", "Storyboard generation failed", e)
                _storyboardState.value = AiUiState.Error(e.localizedMessage ?: "Storyboard generation failed.")
            }
        }
    }

    val playlists: StateFlow<List<MediaPlaylist>> = prefsRepo.playlistsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId.asStateFlow()

    fun selectPlaylist(playlistId: String?) {
        _selectedPlaylistId.value = playlistId
    }

    fun createPlaylist(name: String, description: String = "", initialItem: PlaylistItem? = null) {
        viewModelScope.launch {
            val newId = prefsRepo.createPlaylist(name, description)
            if (initialItem != null) {
                prefsRepo.addItemToPlaylist(newId, initialItem)
            }
        }
    }

    fun renamePlaylist(playlistId: String, newName: String, newDescription: String) {
        viewModelScope.launch {
            prefsRepo.renamePlaylist(playlistId, newName, newDescription)
        }
    }

    fun deletePlaylist(playlistId: String) {
        if (_selectedPlaylistId.value == playlistId) {
            _selectedPlaylistId.value = null
        }
        viewModelScope.launch {
            prefsRepo.deletePlaylist(playlistId)
        }
    }

    fun addMusicTrackToPlaylist(playlistId: String, track: MusicTrack) {
        viewModelScope.launch {
            prefsRepo.addItemToPlaylist(
                playlistId,
                track.toPlaylistItem(UUID.randomUUID().toString())
            )
        }
    }

    fun addVideoItemToPlaylist(playlistId: String, video: VideoItem) {
        viewModelScope.launch {
            prefsRepo.addItemToPlaylist(
                playlistId,
                video.toPlaylistItem(UUID.randomUUID().toString())
            )
        }
    }

    fun addLocalAudioToPlaylist(playlistId: String, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val cr = getApplication<Application>().contentResolver
        val tracks = uris.map { uri ->
            try {
                cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
            val displayName = resolveDisplayName(uri) ?: "Local Audio"
            MusicTrack(
                id = "local_audio_${uri.toString().hashCode()}",
                title = displayName.substringBeforeLast('.'),
                artist = "Device Audio",
                genre = "Local",
                durationLabel = "File",
                uriString = uri.toString(),
                sourceType = MediaSourceType.LOCAL_DEVICE,
                accentHex = 0xFF00E5A3
            )
        }
        _deviceMusicTracks.update { existing ->
            val existingUris = existing.map { it.uriString }.toSet()
            tracks.filterNot { it.uriString in existingUris } + existing
        }
        viewModelScope.launch {
            tracks.forEach { track ->
                prefsRepo.addItemToPlaylist(
                    playlistId,
                    track.toPlaylistItem(UUID.randomUUID().toString())
                )
            }
        }
    }

    fun addLocalVideoToPlaylist(playlistId: String, uri: Uri) {
        val cr = getApplication<Application>().contentResolver
        try {
            cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }
        val displayName = resolveDisplayName(uri) ?: "Device Video"
        val item = VideoItem(
            id = "local_vid_${uri.toString().hashCode()}",
            title = displayName.substringBeforeLast('.'),
            creator = "From Device",
            category = "Local",
            durationLabel = "Local",
            uriString = uri.toString(),
            thumbnailUrl = uri.toString(),
            sourceType = MediaSourceType.LOCAL_DEVICE,
            formatTag = "LOCAL"
        )
        _deviceVideos.update { existing ->
            listOf(item) + existing.filterNot { it.uriString == item.uriString }
        }
        viewModelScope.launch {
            prefsRepo.addItemToPlaylist(
                playlistId,
                item.toPlaylistItem(UUID.randomUUID().toString())
            )
        }
    }

    fun removePlaylistItem(playlistId: String, itemId: String) {
        viewModelScope.launch {
            prefsRepo.removeItemFromPlaylist(playlistId, itemId)
        }
    }

    fun movePlaylistItem(playlistId: String, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            prefsRepo.movePlaylistItem(playlistId, fromIndex, toIndex)
        }
    }

    fun playPlaylistItem(playlist: MediaPlaylist, item: PlaylistItem) {
        when (item.itemType) {
            PlaylistItemType.MUSIC -> {
                val musicQueue = playlist.items
                    .filter { it.itemType == PlaylistItemType.MUSIC }
                    .map { it.toMusicTrack() }
                val target = musicQueue.find { it.id == "pl_${item.id}" } ?: item.toMusicTrack()
                mediaManager.playMusicTrack(target, musicQueue.ifEmpty { listOf(target) })
            }
            PlaylistItemType.VIDEO -> {
                playVideo(item.toVideoItem())
            }
        }
    }

    fun playPlaylistFromStart(playlist: MediaPlaylist) {
        val first = playlist.items.firstOrNull() ?: return
        playPlaylistItem(playlist, first)
    }

    private fun resolveDisplayName(uri: Uri): String? {
        val cr = getApplication<Application>().contentResolver
        return try {
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaManager.release()
    }
}
