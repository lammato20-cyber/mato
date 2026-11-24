package com.example.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.MusicTrack
import com.example.data.PlayerRepeatMode
import com.example.data.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class EqPreset(val label: String, val bandLevelsMb: ShortArray) {
    FLAT("Flat", shortArrayOf(0, 0, 0, 0, 0)),
    BASS_BOOST("Bass Boost", shortArrayOf(550, 350, 0, 100, 200)),
    STUDIO("Studio", shortArrayOf(200, 100, -100, 250, 380)),
    ELECTRONIC("Electronic", shortArrayOf(480, 220, -150, 280, 480)),
    VOCAL("Vocal", shortArrayOf(-180, 120, 420, 360, 120)),
    CLUB("Club", shortArrayOf(400, 250, 100, 200, 300)),
    ROCK("Rock", shortArrayOf(450, 150, -120, 250, 420)),
    ACOUSTIC("Acoustic", shortArrayOf(250, 150, 100, 280, 200)),
    LATE_NIGHT("Late Night", shortArrayOf(300, 150, 0, -150, -250)),
    CUSTOM("Custom", shortArrayOf(0, 0, 0, 0, 0))
}

enum class ReverbOption(val label: String, val presetValue: Short) {
    NONE("Off", PresetReverb.PRESET_NONE),
    SMALL_ROOM("Room", PresetReverb.PRESET_SMALLROOM),
    MEDIUM_HALL("Hall", PresetReverb.PRESET_MEDIUMHALL),
    LARGE_HALL("Arena", PresetReverb.PRESET_LARGEHALL),
    PLATE("Plate", PresetReverb.PRESET_PLATE)
}

data class EqualizerSettings(
    val enabled: Boolean = true,
    val preset: EqPreset = EqPreset.FLAT,
    val bandLevelsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val bandLabels: List<String> = listOf("60Hz", "230Hz", "910Hz", "3.6k", "14k"),
    val minBandMb: Int = -1500,
    val maxBandMb: Int = 1500,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val reverb: ReverbOption = ReverbOption.NONE,
    val hardwareAttached: Boolean = false
)

enum class VideoResizeOption(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom")
}

data class MusicPlayerState(
    val currentTrack: MusicTrack? = null,
    val queue: List<MusicTrack> = emptyList(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val shuffleEnabled: Boolean = false,
    val repeatMode: PlayerRepeatMode = PlayerRepeatMode.ALL,
    val eqPreset: EqPreset = EqPreset.FLAT,
    val eqSettings: EqualizerSettings = EqualizerSettings(),
    val sleepTimerRemainingSec: Int? = null,
    val errorMessage: String? = null
)

data class VideoPlayerState(
    val currentVideo: VideoItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false,
    val resizeOption: VideoResizeOption = VideoResizeOption.FIT,
    val errorMessage: String? = null
)

class PulseMediaManager(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    val musicPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    val videoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _musicState = MutableStateFlow(MusicPlayerState())
    val musicState: StateFlow<MusicPlayerState> = _musicState.asStateFlow()

    private val _videoState = MutableStateFlow(VideoPlayerState())
    val videoState: StateFlow<VideoPlayerState> = _videoState.asStateFlow()

    private var currentQueue: List<MusicTrack> = emptyList()
    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null
    private var attachedSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    init {
        musicPlayer.repeatMode = Player.REPEAT_MODE_ALL

        musicPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _musicState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    if (videoPlayer.isPlaying) videoPlayer.pause()
                    ensureEqualizerAttached()
                    startPositionTicker()
                } else {
                    stopPositionTicker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId ?: return
                val matched = currentQueue.find { it.id == id }
                if (matched != null) {
                    val dur = musicPlayer.duration.takeIf { it > 0L } ?: 0L
                    _musicState.update {
                        it.copy(
                            currentTrack = matched,
                            positionMs = 0L,
                            durationMs = dur,
                            errorMessage = null
                        )
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val buffering = playbackState == Player.STATE_BUFFERING
                val dur = musicPlayer.duration.takeIf { it > 0L } ?: 0L
                _musicState.update {
                    it.copy(
                        isBuffering = buffering,
                        durationMs = dur,
                        errorMessage = if (playbackState == Player.STATE_READY) null else it.errorMessage
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _musicState.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = "Audio playback failed: ${error.localizedMessage ?: "Check connection or file"}"
                    )
                }
            }
        })

        videoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && musicPlayer.isPlaying) {
                    musicPlayer.pause()
                }
                _videoState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _videoState.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        errorMessage = if (playbackState == Player.STATE_READY) null else it.errorMessage
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _videoState.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = "Video error: ${error.localizedMessage ?: "Unable to stream media"}"
                    )
                }
            }
        })
    }

    private fun ensureEqualizerAttached() {
        val sessionId = musicPlayer.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) return
        if (equalizer != null && attachedSessionId == sessionId) return
        releaseAudioEffects()
        attachedSessionId = sessionId

        var eqAttached = false
        runCatching {
            val eq = Equalizer(0, sessionId)
            val bands = eq.numberOfBands.toInt().coerceIn(1, 10)
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            val labels = (0 until bands).map { b ->
                val hz = eq.getCenterFreq(b.toShort()) / 1000
                if (hz >= 1000) {
                    val k = hz / 1000f
                    if (k % 1f == 0f) "${k.toInt()}k" else String.format("%.1fk", k)
                } else {
                    "${hz}Hz"
                }
            }
            equalizer = eq
            eqAttached = true
            _musicState.update { state ->
                val adjustedLevels = List(bands) { idx ->
                    state.eqSettings.bandLevelsMb.getOrElse(idx) { 0 }.coerceIn(minMb, maxMb)
                }
                state.copy(
                    eqSettings = state.eqSettings.copy(
                        bandLabels = labels,
                        bandLevelsMb = adjustedLevels,
                        minBandMb = minMb,
                        maxBandMb = maxMb,
                        hardwareAttached = true
                    )
                )
            }
        }
        runCatching { bassBoost = BassBoost(0, sessionId) }
        runCatching { virtualizer = Virtualizer(0, sessionId) }
        runCatching { loudnessEnhancer = LoudnessEnhancer(sessionId) }
        runCatching { presetReverb = PresetReverb(0, sessionId) }

        if (eqAttached) {
            applyAllAudioEffects(_musicState.value.eqSettings)
        }
    }

    private fun applyAllAudioEffects(settings: EqualizerSettings) {
        val active = settings.enabled
        runCatching {
            equalizer?.let { eq ->
                eq.enabled = active
                val bands = eq.numberOfBands.toInt().coerceAtLeast(0)
                val range = eq.bandLevelRange
                val minMb = range[0].toInt()
                val maxMb = range[1].toInt()
                for (b in 0 until bands) {
                    val target = if (active) {
                        settings.bandLevelsMb.getOrElse(b) { 0 }.coerceIn(minMb, maxMb)
                    } else 0
                    eq.setBandLevel(b.toShort(), target.toShort())
                }
            }
        }
        runCatching {
            bassBoost?.let { bb ->
                bb.enabled = active && settings.bassBoostStrength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(
                        (if (active) settings.bassBoostStrength else 0)
                            .coerceIn(0, 1000).toShort()
                    )
                }
            }
        }
        runCatching {
            virtualizer?.let { vz ->
                vz.enabled = active && settings.virtualizerStrength > 0
                if (vz.strengthSupported) {
                    vz.setStrength(
                        (if (active) settings.virtualizerStrength else 0)
                            .coerceIn(0, 1000).toShort()
                    )
                }
            }
        }
        runCatching {
            loudnessEnhancer?.let { le ->
                val gain = if (active) settings.loudnessGainMb.coerceIn(0, 1200) else 0
                le.enabled = gain > 0
                le.setTargetGain(gain)
            }
        }
        runCatching {
            presetReverb?.let { rv ->
                val enableReverb = active && settings.reverb != ReverbOption.NONE
                rv.preset = if (enableReverb) settings.reverb.presetValue else PresetReverb.PRESET_NONE
                rv.enabled = enableReverb
            }
        }
    }

    private fun releaseAudioEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        runCatching { presetReverb?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        presetReverb = null
        attachedSessionId = C.AUDIO_SESSION_ID_UNSET
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                val pos = musicPlayer.currentPosition.coerceAtLeast(0L)
                val dur = musicPlayer.duration.takeIf { it > 0L } ?: _musicState.value.durationMs
                _musicState.update { it.copy(positionMs = pos, durationMs = dur) }
                delay(250L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionJob?.cancel()
        positionJob = null
    }

    fun playMusicTrack(track: MusicTrack, queue: List<MusicTrack>) {
        val effectiveQueue = if (queue.any { it.id == track.id }) queue else listOf(track) + queue
        currentQueue = effectiveQueue
        if (videoPlayer.isPlaying) videoPlayer.pause()

        val startIndex = effectiveQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val mediaItems = effectiveQueue.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(Uri.parse(item.uriString))
                .build()
        }

        _musicState.update {
            it.copy(
                currentTrack = track,
                queue = effectiveQueue,
                positionMs = 0L,
                durationMs = 0L,
                errorMessage = null
            )
        }

        musicPlayer.setMediaItems(mediaItems, startIndex, 0L)
        musicPlayer.prepare()
        musicPlayer.play()
    }

    fun toggleMusicPlayPause() {
        if (_musicState.value.currentTrack == null && currentQueue.isNotEmpty()) {
            playMusicTrack(currentQueue.first(), currentQueue)
            return
        }
        if (musicPlayer.isPlaying) {
            musicPlayer.pause()
        } else {
            if (videoPlayer.isPlaying) videoPlayer.pause()
            musicPlayer.play()
        }
    }

    fun seekMusicTo(positionMs: Long) {
        musicPlayer.seekTo(positionMs.coerceAtLeast(0L))
        _musicState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun setMusicSpeed(speed: Float) {
        musicPlayer.setPlaybackSpeed(speed)
        _musicState.update { it.copy(playbackSpeed = speed) }
    }

    fun applyEqualizerSettings(settings: EqualizerSettings) {
        val clampedBands = settings.bandLevelsMb.map {
            it.coerceIn(settings.minBandMb, settings.maxBandMb)
        }
        val normalized = settings.copy(
            bandLevelsMb = clampedBands,
            hardwareAttached = equalizer != null
        )
        _musicState.update {
            it.copy(
                eqPreset = normalized.preset,
                eqSettings = normalized
            )
        }
        applyAllAudioEffects(normalized)
    }

    fun setEqEnabled(enabled: Boolean) {
        val updated = _musicState.value.eqSettings.copy(enabled = enabled)
        applyEqualizerSettings(updated)
    }

    fun setEqPreset(preset: EqPreset) {
        val cur = _musicState.value.eqSettings
        val bandCount = cur.bandLevelsMb.size.coerceAtLeast(5)
        val levels = if (preset == EqPreset.CUSTOM) {
            cur.bandLevelsMb
        } else {
            List(bandCount) { idx ->
                preset.bandLevelsMb.getOrElse(idx) { 0 }.toInt()
                    .coerceIn(cur.minBandMb, cur.maxBandMb)
            }
        }
        val updated = cur.copy(
            enabled = true,
            preset = preset,
            bandLevelsMb = levels
        )
        applyEqualizerSettings(updated)
    }

    fun setEqBandLevel(bandIndex: Int, levelMb: Int) {
        val cur = _musicState.value.eqSettings
        if (bandIndex !in cur.bandLevelsMb.indices) return
        val mutable = cur.bandLevelsMb.toMutableList()
        mutable[bandIndex] = levelMb.coerceIn(cur.minBandMb, cur.maxBandMb)
        val updated = cur.copy(
            enabled = true,
            preset = EqPreset.CUSTOM,
            bandLevelsMb = mutable
        )
        applyEqualizerSettings(updated)
    }

    fun setBassBoostStrength(strength: Int) {
        val cur = _musicState.value.eqSettings
        applyEqualizerSettings(cur.copy(enabled = true, bassBoostStrength = strength.coerceIn(0, 1000)))
    }

    fun setVirtualizerStrength(strength: Int) {
        val cur = _musicState.value.eqSettings
        applyEqualizerSettings(cur.copy(enabled = true, virtualizerStrength = strength.coerceIn(0, 1000)))
    }

    fun setLoudnessGainMb(gainMb: Int) {
        val cur = _musicState.value.eqSettings
        applyEqualizerSettings(cur.copy(enabled = true, loudnessGainMb = gainMb.coerceIn(0, 1200)))
    }

    fun setReverbOption(reverb: ReverbOption) {
        val cur = _musicState.value.eqSettings
        applyEqualizerSettings(cur.copy(enabled = true, reverb = reverb))
    }

    fun resetEqualizer() {
        val cur = _musicState.value.eqSettings
        applyEqualizerSettings(
            cur.copy(
                enabled = true,
                preset = EqPreset.FLAT,
                bandLevelsMb = List(cur.bandLevelsMb.size.coerceAtLeast(5)) { 0 },
                bassBoostStrength = 0,
                virtualizerStrength = 0,
                loudnessGainMb = 0,
                reverb = ReverbOption.NONE
            )
        )
    }

    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        if (minutes == null || minutes <= 0) {
            _musicState.update { it.copy(sleepTimerRemainingSec = null) }
            return
        }
        val totalSeconds = minutes * 60
        _musicState.update { it.copy(sleepTimerRemainingSec = totalSeconds) }
        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining -= 1
                _musicState.update { it.copy(sleepTimerRemainingSec = remaining) }
            }
            if (isActive && remaining <= 0) {
                if (musicPlayer.isPlaying) musicPlayer.pause()
                _musicState.update { it.copy(sleepTimerRemainingSec = null) }
            }
        }
    }

    fun toggleShuffle() {
        val nextShuffle = !_musicState.value.shuffleEnabled
        musicPlayer.shuffleModeEnabled = nextShuffle
        _musicState.update { it.copy(shuffleEnabled = nextShuffle) }
    }

    fun cycleRepeatMode() {
        val next = when (_musicState.value.repeatMode) {
            PlayerRepeatMode.ALL -> PlayerRepeatMode.ONE
            PlayerRepeatMode.ONE -> PlayerRepeatMode.OFF
            PlayerRepeatMode.OFF -> PlayerRepeatMode.ALL
        }
        musicPlayer.repeatMode = when (next) {
            PlayerRepeatMode.ALL -> Player.REPEAT_MODE_ALL
            PlayerRepeatMode.ONE -> Player.REPEAT_MODE_ONE
            PlayerRepeatMode.OFF -> Player.REPEAT_MODE_OFF
        }
        _musicState.update { it.copy(repeatMode = next) }
    }

    fun playNextTrack(queueOverride: List<MusicTrack>? = null) {
        if (musicPlayer.hasNextMediaItem()) {
            musicPlayer.seekToNextMediaItem()
            musicPlayer.play()
            return
        }
        val queue = queueOverride ?: currentQueue
        if (queue.isEmpty()) return
        val current = _musicState.value.currentTrack
        val idx = queue.indexOfFirst { it.id == current?.id }
        playMusicTrack(queue[(idx + 1) % queue.size], queue)
    }

    fun playPreviousTrack(queueOverride: List<MusicTrack>? = null) {
        if (musicPlayer.currentPosition > 3000L) {
            seekMusicTo(0L)
            return
        }
        if (musicPlayer.hasPreviousMediaItem()) {
            musicPlayer.seekToPreviousMediaItem()
            musicPlayer.play()
            return
        }
        val queue = queueOverride ?: currentQueue
        if (queue.isEmpty()) return
        val current = _musicState.value.currentTrack
        val idx = queue.indexOfFirst { it.id == current?.id }
        val prevIdx = if (idx <= 0) queue.lastIndex else idx - 1
        playMusicTrack(queue[prevIdx], queue)
    }

    fun playVideo(video: VideoItem) {
        if (musicPlayer.isPlaying) musicPlayer.pause()
        _videoState.update {
            it.copy(
                currentVideo = video,
                errorMessage = null
            )
        }
        val item = MediaItem.fromUri(Uri.parse(video.uriString))
        videoPlayer.setMediaItem(item)
        videoPlayer.volume = if (_videoState.value.isMuted) 0f else 1f
        videoPlayer.setPlaybackSpeed(_videoState.value.playbackSpeed)
        videoPlayer.prepare()
        videoPlayer.play()
    }

    fun setVideoSpeed(speed: Float) {
        videoPlayer.setPlaybackSpeed(speed)
        _videoState.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleVideoMute() {
        val nextMuted = !_videoState.value.isMuted
        videoPlayer.volume = if (nextMuted) 0f else 1f
        _videoState.update { it.copy(isMuted = nextMuted) }
    }

    fun cycleVideoResizeOption() {
        val next = when (_videoState.value.resizeOption) {
            VideoResizeOption.FIT -> VideoResizeOption.FILL
            VideoResizeOption.FILL -> VideoResizeOption.ZOOM
            VideoResizeOption.ZOOM -> VideoResizeOption.FIT
        }
        _videoState.update { it.copy(resizeOption = next) }
    }

    fun seekVideoBy(deltaMs: Long) {
        val dur = videoPlayer.duration
        val target = (videoPlayer.currentPosition + deltaMs).coerceAtLeast(0L)
        val clamped = if (dur > 0L) target.coerceAtMost(dur) else target
        videoPlayer.seekTo(clamped)
    }

    fun pauseAllForExternalMedia() {
        if (musicPlayer.isPlaying) musicPlayer.pause()
        if (videoPlayer.isPlaying) videoPlayer.pause()
    }

    fun release() {
        stopPositionTicker()
        sleepTimerJob?.cancel()
        releaseAudioEffects()
        musicPlayer.release()
        videoPlayer.release()
    }
}
