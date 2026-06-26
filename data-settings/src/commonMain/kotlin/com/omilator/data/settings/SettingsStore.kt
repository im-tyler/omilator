package com.omilator.data.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class SettingsStore(
    private val readText: suspend (String) -> String?,
    private val writeText: suspend (String, String) -> Unit,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun loadAppSettings(path: String): AppSettings {
        val text = readText(path) ?: return AppSettings.DEFAULT
        return runCatching { json.decodeFromString<AppSettings>(text) }
            .getOrElse { AppSettings.DEFAULT }
    }

    suspend fun saveAppSettings(settings: AppSettings, path: String) {
        writeText(path, json.encodeToString(settings))
    }

    suspend fun loadGameSettings(path: String): GameSettings? {
        val text = readText(path) ?: return null
        return runCatching { json.decodeFromString<GameSettings>(text) }.getOrNull()
    }

    suspend fun saveGameSettings(settings: GameSettings, path: String) {
        writeText(path, json.encodeToString(settings))
    }
}
