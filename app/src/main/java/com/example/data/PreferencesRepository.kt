package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.player.EqPreset
import com.example.player.EqualizerSettings
import com.example.player.ReverbOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pulsedeck_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val SNAKE_BEST = intPreferencesKey("snake_best")
        val BRICK_BEST = intPreferencesKey("brick_best")
        val PUZZLE_2048_BEST = intPreferencesKey("puzzle_2048_best")
        val SIMON_BEST = intPreferencesKey("simon_best")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SAVED_YOUTUBE_IDS = stringPreferencesKey("saved_youtube_ids")
        val RECENT_YOUTUBE_PASTES = stringPreferencesKey("recent_youtube_pastes")
        val CUSTOM_AUDIO_URLS = stringPreferencesKey("custom_audio_urls")
        val CUSTOM_VIDEO_URLS = stringPreferencesKey("custom_video_urls")
        val MEDIA_PLAYLISTS_JSON = stringPreferencesKey("media_playlists_json")
        val CITY_CASH = intPreferencesKey("city_racer_cash")
        val CITY_RACES_WON = intPreferencesKey("city_racer_races_won")
        val CITY_CARS_STOLEN = intPreferencesKey("city_racer_cars_stolen")
        val CITY_ENGINE_LV = intPreferencesKey("city_racer_engine_lv")
        val CITY_NITRO_LV = intPreferencesKey("city_racer_nitro_lv")
        val CITY_ARMOR_LV = intPreferencesKey("city_racer_armor_lv")
        val EQ_SETTINGS_JSON = stringPreferencesKey("eq_settings_json")
    }

    val cityRacerProgressFlow: Flow<CityRacerProgress> = context.dataStore.data.map { prefs ->
        CityRacerProgress(
            cash = prefs[Keys.CITY_CASH] ?: 600,
            racesWon = prefs[Keys.CITY_RACES_WON] ?: 0,
            carsStolen = prefs[Keys.CITY_CARS_STOLEN] ?: 0,
            engineLevel = prefs[Keys.CITY_ENGINE_LV] ?: 1,
            nitroLevel = prefs[Keys.CITY_NITRO_LV] ?: 1,
            armorLevel = prefs[Keys.CITY_ARMOR_LV] ?: 1
        )
    }

    val highScoresFlow: Flow<HighScores> = context.dataStore.data.map { prefs ->
        HighScores(
            cityRacerBestCash = prefs[Keys.CITY_CASH] ?: 600,
            snakeBest = prefs[Keys.SNAKE_BEST] ?: 0,
            brickBreakerBest = prefs[Keys.BRICK_BEST] ?: 0,
            puzzle2048Best = prefs[Keys.PUZZLE_2048_BEST] ?: 0,
            simonBest = prefs[Keys.SIMON_BEST] ?: 0
        )
    }

    val darkThemeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: true
    }

    val savedYouTubeVideosFlow: Flow<List<YouTubeVideo>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.SAVED_YOUTUBE_IDS].orEmpty()
        if (raw.isBlank()) emptyList()
        else raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 2) {
                YouTubeVideo(
                    videoId = parts[0],
                    title = parts[1],
                    channel = parts.getOrElse(2) { "YouTube" },
                    category = "Saved",
                    durationLabel = parts.getOrElse(3) { "Stream" }
                )
            } else null
        }
    }

    val recentYouTubePastesFlow: Flow<List<YouTubeVideo>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.RECENT_YOUTUBE_PASTES].orEmpty()
        if (raw.isBlank()) emptyList()
        else raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 2) {
                YouTubeVideo(
                    videoId = parts[0],
                    title = parts[1],
                    channel = parts.getOrElse(2) { "Pasted Link" },
                    category = "Daily",
                    durationLabel = parts.getOrElse(3) { "HD" },
                    addedDateLabel = parts.getOrElse(4) { "" }
                )
            } else null
        }
    }

    val customAudioStreamsFlow: Flow<List<MusicTrack>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.CUSTOM_AUDIO_URLS].orEmpty()
        if (raw.isBlank()) emptyList()
        else raw.split("\n").mapIndexedNotNull { idx, line ->
            val parts = line.split("|")
            if (parts.size >= 2) {
                MusicTrack(
                    id = "custom_audio_${parts[0].hashCode()}_$idx",
                    title = parts[1],
                    artist = "Custom Stream",
                    genre = "Stream",
                    durationLabel = "Stream",
                    uriString = parts[0],
                    sourceType = MediaSourceType.ONLINE_STREAM,
                    accentHex = 0xFF38BDF8
                )
            } else null
        }
    }

    val customVideoStreamsFlow: Flow<List<VideoItem>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.CUSTOM_VIDEO_URLS].orEmpty()
        if (raw.isBlank()) emptyList()
        else raw.split("\n").mapIndexedNotNull { idx, line ->
            val parts = line.split("|")
            if (parts.size >= 2) {
                val url = parts[0]
                VideoItem(
                    id = "custom_video_${url.hashCode()}_$idx",
                    title = parts[1],
                    creator = "Custom Stream",
                    category = "Stream",
                    durationLabel = "Stream",
                    uriString = url,
                    thumbnailUrl = null,
                    sourceType = MediaSourceType.ONLINE_STREAM,
                    formatTag = if (url.contains(".m3u8", ignoreCase = true)) "HLS" else "URL"
                )
            } else null
        }
    }

    val playlistsFlow: Flow<List<MediaPlaylist>> = context.dataStore.data.map { prefs ->
        decodePlaylists(prefs[Keys.MEDIA_PLAYLISTS_JSON].orEmpty())
    }

    val equalizerSettingsFlow: Flow<EqualizerSettings> = context.dataStore.data.map { prefs ->
        decodeEqualizerSettings(prefs[Keys.EQ_SETTINGS_JSON].orEmpty())
    }

    suspend fun saveEqualizerSettings(settings: EqualizerSettings) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            settings.bandLevelsMb.forEach { arr.put(it) }
            val obj = JSONObject().apply {
                put("enabled", settings.enabled)
                put("preset", settings.preset.name)
                put("bandLevelsMb", arr)
                put("bassBoostStrength", settings.bassBoostStrength)
                put("virtualizerStrength", settings.virtualizerStrength)
                put("loudnessGainMb", settings.loudnessGainMb)
                put("reverb", settings.reverb.name)
            }
            prefs[Keys.EQ_SETTINGS_JSON] = obj.toString()
        }
    }

    private fun decodeEqualizerSettings(json: String): EqualizerSettings {
        if (json.isBlank()) return EqualizerSettings()
        return runCatching {
            val obj = JSONObject(json)
            val preset = runCatching { EqPreset.valueOf(obj.optString("preset", EqPreset.FLAT.name)) }
                .getOrDefault(EqPreset.FLAT)
            val reverb = runCatching { ReverbOption.valueOf(obj.optString("reverb", ReverbOption.NONE.name)) }
                .getOrDefault(ReverbOption.NONE)
            val bandsArr = obj.optJSONArray("bandLevelsMb")
            val bands = if (bandsArr != null && bandsArr.length() > 0) {
                List(bandsArr.length()) { idx -> bandsArr.optInt(idx, 0) }
            } else {
                listOf(0, 0, 0, 0, 0)
            }
            EqualizerSettings(
                enabled = obj.optBoolean("enabled", true),
                preset = preset,
                bandLevelsMb = bands,
                bassBoostStrength = obj.optInt("bassBoostStrength", 0),
                virtualizerStrength = obj.optInt("virtualizerStrength", 0),
                loudnessGainMb = obj.optInt("loudnessGainMb", 0),
                reverb = reverb
            )
        }.getOrDefault(EqualizerSettings())
    }

    suspend fun updateCityRacerProgress(progress: CityRacerProgress) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CITY_CASH] = progress.cash.coerceAtLeast(0)
            prefs[Keys.CITY_RACES_WON] = progress.racesWon
            prefs[Keys.CITY_CARS_STOLEN] = progress.carsStolen
            prefs[Keys.CITY_ENGINE_LV] = progress.engineLevel
            prefs[Keys.CITY_NITRO_LV] = progress.nitroLevel
            prefs[Keys.CITY_ARMOR_LV] = progress.armorLevel
        }
    }

    suspend fun updateHighScore(gameType: GameType, score: Int) {
        val key = when (gameType) {
            GameType.CITY_RACER -> Keys.CITY_CASH
            GameType.SNAKE -> Keys.SNAKE_BEST
            GameType.BRICK_BREAKER -> Keys.BRICK_BEST
            GameType.PUZZLE_2048 -> Keys.PUZZLE_2048_BEST
            GameType.SIMON_PULSE -> Keys.SIMON_BEST
        }
        context.dataStore.edit { prefs ->
            val current = prefs[key] ?: 0
            if (score > current) {
                prefs[key] = score
            }
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }

    suspend fun toggleBookmarkYouTube(video: YouTubeVideo) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAVED_YOUTUBE_IDS].orEmpty()
                .split("\n")
                .filter { it.isNotBlank() }
                .toMutableList()
            val existingIndex = current.indexOfFirst { it.startsWith("${video.videoId}|") }
            if (existingIndex >= 0) {
                current.removeAt(existingIndex)
            } else {
                val cleanTitle = video.title.replace("|", " ").replace("\n", " ")
                val cleanChannel = video.channel.replace("|", " ").replace("\n", " ")
                current.add(0, "${video.videoId}|$cleanTitle|$cleanChannel|${video.durationLabel}")
            }
            prefs[Keys.SAVED_YOUTUBE_IDS] = current.joinToString("\n")
        }
    }

    suspend fun recordPastedYouTubeVideo(video: YouTubeVideo, dateLabel: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_YOUTUBE_PASTES].orEmpty()
                .split("\n")
                .filter { it.isNotBlank() && !it.startsWith("${video.videoId}|") }
                .toMutableList()
            val cleanTitle = video.title.replace("|", " ").replace("\n", " ")
            val cleanChannel = video.channel.replace("|", " ").replace("\n", " ")
            val cleanDate = dateLabel.replace("|", " ").replace("\n", " ")
            current.add(0, "${video.videoId}|$cleanTitle|$cleanChannel|${video.durationLabel}|$cleanDate")
            prefs[Keys.RECENT_YOUTUBE_PASTES] = current.take(30).joinToString("\n")
        }
    }

    suspend fun removeRecentYouTubePaste(videoId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_YOUTUBE_PASTES].orEmpty()
                .split("\n")
                .filter { it.isNotBlank() && !it.startsWith("$videoId|") }
            prefs[Keys.RECENT_YOUTUBE_PASTES] = current.joinToString("\n")
        }
    }

    suspend fun clearRecentYouTubePastes() {
        context.dataStore.edit { prefs ->
            prefs[Keys.RECENT_YOUTUBE_PASTES] = ""
        }
    }

    suspend fun addCustomAudioStream(url: String, title: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_AUDIO_URLS].orEmpty()
                .split("\n")
                .filter { it.isNotBlank() }
                .toMutableList()
            val cleanUrl = url.trim().replace("|", "")
            val cleanTitle = title.trim().ifEmpty { "Audio Stream" }.replace("|", " ")
            current.add(0, "$cleanUrl|$cleanTitle")
            prefs[Keys.CUSTOM_AUDIO_URLS] = current.take(25).joinToString("\n")
        }
    }

    suspend fun addCustomVideoStream(url: String, title: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_VIDEO_URLS].orEmpty()
                .split("\n")
                .filter { it.isNotBlank() }
                .toMutableList()
            val cleanUrl = url.trim().replace("|", "")
            val cleanTitle = title.trim().ifEmpty { "Video Stream" }.replace("|", " ")
            current.add(0, "$cleanUrl|$cleanTitle")
            prefs[Keys.CUSTOM_VIDEO_URLS] = current.take(25).joinToString("\n")
        }
    }

    suspend fun createPlaylist(name: String, description: String = ""): String {
        val newId = UUID.randomUUID().toString()
        val cleanName = name.trim().ifEmpty { "Untitled Playlist" }
        context.dataStore.edit { prefs ->
            val list = decodePlaylists(prefs[Keys.MEDIA_PLAYLISTS_JSON].orEmpty()).toMutableList()
            list.add(
                0,
                MediaPlaylist(
                    id = newId,
                    name = cleanName,
                    description = description.trim(),
                    items = emptyList(),
                    createdAtMs = System.currentTimeMillis()
                )
            )
            prefs[Keys.MEDIA_PLAYLISTS_JSON] = encodePlaylists(list)
        }
        return newId
    }

    suspend fun renamePlaylist(playlistId: String, newName: String, newDescription: String) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return
        context.dataStore.edit { prefs ->
            val list = decodePlaylists(prefs[Keys.MEDIA_PLAYLISTS_JSON].orEmpty()).map { pl ->
                if (pl.id == playlistId) pl.copy(name = cleanName, description = newDescription.trim()) else pl
            }
            prefs[Keys.MEDIA_PLAYLISTS_JSON] = encodePlaylists(list)
        }
    }

    suspend fun deletePlaylist(playlistId: String) {
        context.dataStore.edit { prefs ->
            val list = decodePlaylists(prefs[Keys.MEDIA_PLAYLISTS_JSON].orEmpty())
                .filterNot { it.id == playlistId }
            prefs[Keys.MEDIA_PLAYLISTS_JSON] = encodePlaylists(list)
        }
    }

    suspend fun addItemToPlaylist(playlistId: String, item: PlaylistItem) {
        context.dataStore.edit { prefs ->
            val list = decodePlaylists(prefs[Keys.MEDIA_PLAYLISTS_JSON].orEmpty()).map { pl ->
                if (pl.id == playlistId) {
                    val alreadyExists = pl.items.any { it.uriString == item.uriString && it.itemType == item.itemType }
                    if (alreadyExists) pl else pl.copy(items = pl.items + item)
                } else {
                    pl
                }
            }
            prefs[Keys.MEDIA_PLAYLISTS_JSON] = encodePlaylists(list)
        }
    }

    suspend fun removeItemFromPlaylist(playlistId: String, itemId: String) {
        context.dataStore.edit { prefs ->
            val list = decodePlaylists(prefs[Keys.MEDIA_PLAYLISTS_JSON].orEmpty()).map { pl ->
                if (pl.id == playlistId) {
                    pl.copy(items = pl.items.filterNot { it.id == itemId })
                } else {
                    pl
                }
            }
            prefs[Keys.MEDIA_PLAYLISTS_JSON] = encodePlaylists(list)
        }
    }

    suspend fun movePlaylistItem(playlistId: String, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        context.dataStore.edit { prefs ->
            val list = decodePlaylists(prefs[Keys.MEDIA_PLAYLISTS_JSON].orEmpty()).map { pl ->
                if (pl.id == playlistId && fromIndex in pl.items.indices && toIndex in pl.items.indices) {
                    val mutable = pl.items.toMutableList()
                    val moved = mutable.removeAt(fromIndex)
                    mutable.add(toIndex, moved)
                    pl.copy(items = mutable)
                } else {
                    pl
                }
            }
            prefs[Keys.MEDIA_PLAYLISTS_JSON] = encodePlaylists(list)
        }
    }

    private fun encodePlaylists(playlists: List<MediaPlaylist>): String {
        val array = JSONArray()
        playlists.forEach { pl ->
            val obj = JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("description", pl.description)
                put("createdAtMs", pl.createdAtMs)
                val itemsArray = JSONArray()
                pl.items.forEach { item ->
                    itemsArray.put(
                        JSONObject().apply {
                            put("id", item.id)
                            put("itemType", item.itemType.name)
                            put("title", item.title)
                            put("subtitle", item.subtitle)
                            put("uriString", item.uriString)
                            put("durationLabel", item.durationLabel)
                            put("thumbnailUrl", item.thumbnailUrl ?: JSONObject.NULL)
                            put("formatTag", item.formatTag)
                            put("accentHex", item.accentHex)
                        }
                    )
                }
                put("items", itemsArray)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodePlaylists(json: String): List<MediaPlaylist> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val itemsArray = obj.optJSONArray("items") ?: JSONArray()
                    val items = buildList {
                        for (j in 0 until itemsArray.length()) {
                            val itemObj = itemsArray.getJSONObject(j)
                            val typeStr = itemObj.optString("itemType", PlaylistItemType.MUSIC.name)
                            val type = runCatching { PlaylistItemType.valueOf(typeStr) }
                                .getOrDefault(PlaylistItemType.MUSIC)
                            val thumbRaw = itemObj.opt("thumbnailUrl")
                            val thumb = if (thumbRaw == null || thumbRaw == JSONObject.NULL) null else thumbRaw.toString()
                            add(
                                PlaylistItem(
                                    id = itemObj.getString("id"),
                                    itemType = type,
                                    title = itemObj.getString("title"),
                                    subtitle = itemObj.optString("subtitle", ""),
                                    uriString = itemObj.getString("uriString"),
                                    durationLabel = itemObj.optString("durationLabel", "Media"),
                                    thumbnailUrl = thumb,
                                    formatTag = itemObj.optString("formatTag", "MEDIA"),
                                    accentHex = itemObj.optLong("accentHex", 0xFFFF4D6D)
                                )
                            )
                        }
                    }
                    add(
                        MediaPlaylist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            items = items,
                            createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
