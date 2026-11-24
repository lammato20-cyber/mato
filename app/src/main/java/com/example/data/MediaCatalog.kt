package com.example.data

import android.content.Context
import com.example.R

object MediaCatalog {

    fun builtInMusicTracks(context: Context): List<MusicTrack> {
        val pkg = context.packageName
        return listOf(
            MusicTrack(
                id = "builtin_neon_horizon",
                title = "Neon Horizon",
                artist = "PulseDeck Synth",
                genre = "Synthwave",
                durationLabel = "0:16",
                uriString = "android.resource://$pkg/${R.raw.track_neon_horizon}",
                sourceType = MediaSourceType.BUILT_IN,
                accentHex = 0xFFFF4D6D,
                rawResId = R.raw.track_neon_horizon
            ),
            MusicTrack(
                id = "builtin_midnight_groove",
                title = "Midnight Groove",
                artist = "PulseDeck Studio",
                genre = "Chillout",
                durationLabel = "0:16",
                uriString = "android.resource://$pkg/${R.raw.track_midnight_groove}",
                sourceType = MediaSourceType.BUILT_IN,
                accentHex = 0xFF38BDF8,
                rawResId = R.raw.track_midnight_groove
            ),
            MusicTrack(
                id = "builtin_arcade_pulse",
                title = "Arcade Pulse 8-Bit",
                artist = "PulseDeck Chiptune",
                genre = "Chiptune",
                durationLabel = "0:14",
                uriString = "android.resource://$pkg/${R.raw.track_arcade_pulse}",
                sourceType = MediaSourceType.BUILT_IN,
                accentHex = 0xFF00E5A3,
                rawResId = R.raw.track_arcade_pulse
            ),
            MusicTrack(
                id = "online_soundhelix_1",
                title = "Electronic Odyssey",
                artist = "SoundHelix Stream",
                genre = "Electronic",
                durationLabel = "6:12",
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                sourceType = MediaSourceType.ONLINE_STREAM,
                accentHex = 0xFFFBBF24
            ),
            MusicTrack(
                id = "online_soundhelix_2",
                title = "Cybernetic Drift",
                artist = "SoundHelix Stream",
                genre = "Ambient",
                durationLabel = "7:05",
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                sourceType = MediaSourceType.ONLINE_STREAM,
                accentHex = 0xFFA855F7
            ),
            MusicTrack(
                id = "online_soundhelix_8",
                title = "Solar Flare Beat",
                artist = "SoundHelix Stream",
                genre = "Dance",
                durationLabel = "5:25",
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                sourceType = MediaSourceType.ONLINE_STREAM,
                accentHex = 0xFF00E5A3
            )
        )
    }

    val sampleOnlineVideos: List<VideoItem> = listOf(
        VideoItem(
            id = "sample_bbb",
            title = "Big Buck Bunny (HD)",
            creator = "Blender Foundation",
            category = "Animation",
            durationLabel = "9:56",
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
            sourceType = MediaSourceType.ONLINE_STREAM,
            formatTag = "MP4 HD"
        ),
        VideoItem(
            id = "sample_mux_hls",
            title = "Adaptive HLS Showcase",
            creator = "Mux Open Stream",
            category = "Stream",
            durationLabel = "Live VOD",
            uriString = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
            sourceType = MediaSourceType.ONLINE_STREAM,
            formatTag = "HLS"
        ),
        VideoItem(
            id = "sample_elephants_dream",
            title = "Elephants Dream",
            creator = "Orange Open Movie",
            category = "Sci-Fi",
            durationLabel = "10:53",
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
            sourceType = MediaSourceType.ONLINE_STREAM,
            formatTag = "MP4"
        ),
        VideoItem(
            id = "sample_blazes",
            title = "For Bigger Blazes",
            creator = "Google Sample Media",
            category = "Showcase",
            durationLabel = "0:15",
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg",
            sourceType = MediaSourceType.ONLINE_STREAM,
            formatTag = "MP4"
        ),
        VideoItem(
            id = "sample_sintel",
            title = "Sintel Open Film",
            creator = "Blender Institute",
            category = "Fantasy",
            durationLabel = "14:48",
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
            sourceType = MediaSourceType.ONLINE_STREAM,
            formatTag = "MP4 HD"
        )
    )

    val curatedYouTubeVideos: List<YouTubeVideo> = listOf(
        YouTubeVideo(
            videoId = "jfKfPfyJRdk",
            title = "lofi hip hop radio - beats to relax/study to",
            channel = "Lofi Girl",
            category = "Music",
            durationLabel = "LIVE",
            isLive = true
        ),
        YouTubeVideo(
            videoId = "4xDzrJKXOOY",
            title = "synthwave radio - chill synth / retro beats",
            channel = "Lofi Girl Synthwave",
            category = "Music",
            durationLabel = "LIVE",
            isLive = true
        ),
        YouTubeVideo(
            videoId = "LXb3EKWsInQ",
            title = "COSTA RICA IN 4K 60fps HDR (Ultra HD)",
            channel = "Jacob + Katie Schwarz",
            category = "Nature",
            durationLabel = "5:14"
        ),
        YouTubeVideo(
            videoId = "aqz-KE-bpKQ",
            title = "Big Buck Bunny 60fps 4K - Blender Open Movie",
            channel = "Blender Foundation",
            category = "Animation",
            durationLabel = "10:34"
        ),
        YouTubeVideo(
            videoId = "BHACKCNDMW8",
            title = "Relaxing Nature Scenes & Ambient Music 4K",
            channel = "Scenic Relaxation",
            category = "Nature",
            durationLabel = "3:01:00"
        ),
        YouTubeVideo(
            videoId = "M7lc1UVf-VE",
            title = "YouTube IFrame Player API Showcase",
            channel = "YouTube Developers",
            category = "Tech",
            durationLabel = "2:23"
        ),
        YouTubeVideo(
            videoId = "tO01J-M3g0U",
            title = "Earth From Space 4K Ultra HD",
            channel = "Space & Universe",
            category = "Tech",
            durationLabel = "12:15"
        )
    )

    /**
     * Extracts an 11-character YouTube video ID from standard watch, short, embed, live, or youtu.be URLs,
     * or returns the trimmed input if it already matches an 11-char ID.
     */
    fun extractYouTubeVideoId(rawInput: String): String? {
        val input = rawInput.trim()
        if (input.isEmpty()) return null

        val idPattern = Regex("^[A-Za-z0-9_-]{11}$")
        if (idPattern.matches(input)) return input

        val patterns = listOf(
            Regex("(?:v=|/v/|youtu\\.be/|/embed/|/shorts/|/live/)([A-Za-z0-9_-]{11})")
        )
        for (regex in patterns) {
            val match = regex.find(input)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }
}
