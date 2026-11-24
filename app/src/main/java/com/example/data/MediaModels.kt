package com.example.data

import androidx.annotation.RawRes

enum class MediaSourceType {
    BUILT_IN,
    ONLINE_STREAM,
    LOCAL_DEVICE
}

enum class PlayerRepeatMode {
    OFF,
    ALL,
    ONE
}

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val durationLabel: String,
    val uriString: String,
    val sourceType: MediaSourceType,
    val accentHex: Long = 0xFFFF4D6D,
    @RawRes val rawResId: Int? = null
)

data class VideoItem(
    val id: String,
    val title: String,
    val creator: String,
    val category: String,
    val durationLabel: String,
    val uriString: String,
    val thumbnailUrl: String?,
    val sourceType: MediaSourceType,
    val formatTag: String = "MP4"
)

data class YouTubeVideo(
    val videoId: String,
    val title: String,
    val channel: String,
    val category: String,
    val durationLabel: String,
    val isLive: Boolean = false,
    val addedDateLabel: String = ""
) {
    val thumbnailUrl: String
        get() = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
}

enum class GameType(
    val id: String,
    val title: String,
    val subtitle: String,
    val genre: String
) {
    CITY_RACER(
        id = "city_racer",
        title = "Suncoast Heat",
        subtitle = "San Andreas-style city racing, car-jacking, chases & cash hustles",
        genre = "Open City"
    ),
    SNAKE(
        id = "snake",
        title = "Neon Snake",
        subtitle = "Steer the cyber serpent, grab glowing orbs & dodge walls",
        genre = "Arcade"
    ),
    BRICK_BREAKER(
        id = "brick_breaker",
        title = "Brick Breaker",
        subtitle = "60fps neon paddle action with multi-hit crystal bricks",
        genre = "Action"
    ),
    PUZZLE_2048(
        id = "puzzle_2048",
        title = "Pulse 2048",
        subtitle = "Slide and fuse glowing power tiles up to 2048",
        genre = "Puzzle"
    ),
    SIMON_PULSE(
        id = "simon_pulse",
        title = "Simon Pulse",
        subtitle = "Repeat the escalating synth-tone light sequences",
        genre = "Rhythm"
    )
}

data class CityRacerProgress(
    val cash: Int = 600,
    val racesWon: Int = 0,
    val carsStolen: Int = 0,
    val engineLevel: Int = 1,
    val nitroLevel: Int = 1,
    val armorLevel: Int = 1
)

data class HighScores(
    val cityRacerBestCash: Int = 600,
    val snakeBest: Int = 0,
    val brickBreakerBest: Int = 0,
    val puzzle2048Best: Int = 0,
    val simonBest: Int = 0
) {
    fun bestFor(gameType: GameType): Int = when (gameType) {
        GameType.CITY_RACER -> cityRacerBestCash
        GameType.SNAKE -> snakeBest
        GameType.BRICK_BREAKER -> brickBreakerBest
        GameType.PUZZLE_2048 -> puzzle2048Best
        GameType.SIMON_PULSE -> simonBest
    }

    val totalScore: Int
        get() = cityRacerBestCash + snakeBest + brickBreakerBest + puzzle2048Best + simonBest
}

enum class PlaylistItemType {
    MUSIC,
    VIDEO
}

data class PlaylistItem(
    val id: String,
    val itemType: PlaylistItemType,
    val title: String,
    val subtitle: String,
    val uriString: String,
    val durationLabel: String,
    val thumbnailUrl: String? = null,
    val formatTag: String = "AUDIO",
    val accentHex: Long = 0xFFFF4D6D
)

data class MediaPlaylist(
    val id: String,
    val name: String,
    val description: String = "",
    val items: List<PlaylistItem> = emptyList(),
    val createdAtMs: Long = System.currentTimeMillis()
)

fun MusicTrack.toPlaylistItem(uniqueId: String): PlaylistItem = PlaylistItem(
    id = uniqueId,
    itemType = PlaylistItemType.MUSIC,
    title = title,
    subtitle = "$artist • $genre",
    uriString = uriString,
    durationLabel = durationLabel,
    thumbnailUrl = null,
    formatTag = when (sourceType) {
        MediaSourceType.BUILT_IN -> "SYNTH"
        MediaSourceType.ONLINE_STREAM -> "STREAM"
        MediaSourceType.LOCAL_DEVICE -> "LOCAL"
    },
    accentHex = accentHex
)

fun VideoItem.toPlaylistItem(uniqueId: String): PlaylistItem = PlaylistItem(
    id = uniqueId,
    itemType = PlaylistItemType.VIDEO,
    title = title,
    subtitle = creator,
    uriString = uriString,
    durationLabel = durationLabel,
    thumbnailUrl = thumbnailUrl,
    formatTag = formatTag,
    accentHex = 0xFF38BDF8
)

fun PlaylistItem.toMusicTrack(): MusicTrack = MusicTrack(
    id = "pl_${id}",
    title = title,
    artist = subtitle,
    genre = formatTag,
    durationLabel = durationLabel,
    uriString = uriString,
    sourceType = if (uriString.startsWith("android.resource://")) {
        MediaSourceType.BUILT_IN
    } else if (uriString.startsWith("http")) {
        MediaSourceType.ONLINE_STREAM
    } else {
        MediaSourceType.LOCAL_DEVICE
    },
    accentHex = accentHex
)

fun PlaylistItem.toVideoItem(): VideoItem = VideoItem(
    id = "pl_${id}",
    title = title,
    creator = subtitle,
    category = "Playlist",
    durationLabel = durationLabel,
    uriString = uriString,
    thumbnailUrl = thumbnailUrl,
    sourceType = if (uriString.startsWith("http")) {
        MediaSourceType.ONLINE_STREAM
    } else {
        MediaSourceType.LOCAL_DEVICE
    },
    formatTag = formatTag
)
