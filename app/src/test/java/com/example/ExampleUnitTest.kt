package com.example

import com.example.data.CityRacerProgress
import com.example.data.GameType
import com.example.data.MediaCatalog
import com.example.data.MediaPlaylist
import com.example.data.MediaSourceType
import com.example.data.MusicTrack
import com.example.data.PlaylistItemType
import com.example.data.VideoItem
import com.example.data.toMusicTrack
import com.example.data.toPlaylistItem
import com.example.data.toVideoItem
import com.example.player.EqPreset
import com.example.player.EqualizerSettings
import com.example.player.ReverbOption
import com.example.ui.games.cityracer.SuncoastCityMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `youtube url parser extracts video id from watch, short, shorts, live, and share text`() {
        assertEquals("jfKfPfyJRdk", MediaCatalog.extractYouTubeVideoId("https://www.youtube.com/watch?v=jfKfPfyJRdk"))
        assertEquals("LXb3EKWsInQ", MediaCatalog.extractYouTubeVideoId("https://youtu.be/LXb3EKWsInQ?t=12"))
        assertEquals("aqz-KE-bpKQ", MediaCatalog.extractYouTubeVideoId("https://youtube.com/shorts/aqz-KE-bpKQ"))
        assertEquals("4xDzrJKXOOY", MediaCatalog.extractYouTubeVideoId("https://www.youtube.com/live/4xDzrJKXOOY?si=test"))
        assertEquals("M7lc1UVf-VE", MediaCatalog.extractYouTubeVideoId("Watch today: https://youtu.be/M7lc1UVf-VE"))
        assertEquals("4xDzrJKXOOY", MediaCatalog.extractYouTubeVideoId("4xDzrJKXOOY"))
        assertNull(MediaCatalog.extractYouTubeVideoId("not-a-valid-url"))
    }

    @Test
    fun `music and video items convert cleanly to and from playlist items`() {
        val track = MusicTrack(
            id = "t1",
            title = "Neon Horizon",
            artist = "PulseDeck Synth",
            genre = "Synthwave",
            durationLabel = "0:16",
            uriString = "android.resource://com.example/123",
            sourceType = MediaSourceType.BUILT_IN
        )
        val playlistAudio = track.toPlaylistItem("pl_item_1")
        assertEquals(PlaylistItemType.MUSIC, playlistAudio.itemType)
        assertEquals("Neon Horizon", playlistAudio.toMusicTrack().title)

        val video = VideoItem(
            id = "v1",
            title = "Big Buck Bunny",
            creator = "Blender Foundation",
            category = "Animation",
            durationLabel = "9:56",
            uriString = "https://example.com/bbb.mp4",
            thumbnailUrl = null,
            sourceType = MediaSourceType.ONLINE_STREAM,
            formatTag = "MP4"
        )
        val playlistVideo = video.toPlaylistItem("pl_item_2")
        assertEquals(PlaylistItemType.VIDEO, playlistVideo.itemType)
        assertEquals("Big Buck Bunny", playlistVideo.toVideoItem().title)

        val playlist = MediaPlaylist(
            id = "pl1",
            name = "Favorites",
            items = listOf(playlistAudio, playlistVideo)
        )
        assertEquals(2, playlist.items.size)
    }

    @Test
    fun `suncoast heat city map and progress defaults are valid`() {
        val progress = CityRacerProgress()
        assertEquals(600, progress.cash)
        assertNotNull(GameType.CITY_RACER)
        assertTrue(SuncoastCityMap.staticActivities.isNotEmpty())
        assertEquals("Palms Strip", SuncoastCityMap.districtFor(400f, 400f))
        assertTrue(SuncoastCityMap.buildings.isNotEmpty())
    }

    @Test
    fun `equalizer presets and defaults have 5 bands and valid ranges`() {
        val defaults = EqualizerSettings()
        assertEquals(5, defaults.bandLevelsMb.size)
        assertEquals(5, defaults.bandLabels.size)
        assertEquals(ReverbOption.NONE, defaults.reverb)
        EqPreset.entries.forEach { preset ->
            assertEquals(5, preset.bandLevelsMb.size)
        }
    }
}
