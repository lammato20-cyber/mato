package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PlaylistItem
import com.example.data.PlaylistItemType
import com.example.data.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PulseDeck", appName)
    }

    @Test
    fun `create add reorder and remove playlist items persists in datastore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PreferencesRepository(context)

        val playlistId = repo.createPlaylist("Roadtrip Mix", "Music and clips")

        val audioItem = PlaylistItem(
            id = "item_1",
            itemType = PlaylistItemType.MUSIC,
            title = "Neon Horizon",
            subtitle = "PulseDeck Synth",
            uriString = "android.resource://com.example/1",
            durationLabel = "0:16"
        )
        val videoItem = PlaylistItem(
            id = "item_2",
            itemType = PlaylistItemType.VIDEO,
            title = "Big Buck Bunny",
            subtitle = "Blender Foundation",
            uriString = "https://example.com/bbb.mp4",
            durationLabel = "9:56",
            formatTag = "MP4"
        )

        repo.addItemToPlaylist(playlistId, audioItem)
        repo.addItemToPlaylist(playlistId, videoItem)

        var saved = repo.playlistsFlow.first().first { it.id == playlistId }
        assertEquals("Roadtrip Mix", saved.name)
        assertEquals(2, saved.items.size)
        assertEquals("item_1", saved.items[0].id)
        assertEquals("item_2", saved.items[1].id)

        // Reorder item_2 before item_1
        repo.movePlaylistItem(playlistId, fromIndex = 1, toIndex = 0)
        saved = repo.playlistsFlow.first().first { it.id == playlistId }
        assertEquals("item_2", saved.items[0].id)
        assertEquals("item_1", saved.items[1].id)

        // Remove item_1
        repo.removeItemFromPlaylist(playlistId, "item_1")
        saved = repo.playlistsFlow.first().first { it.id == playlistId }
        assertEquals(1, saved.items.size)
        assertEquals("item_2", saved.items[0].id)
    }
}
