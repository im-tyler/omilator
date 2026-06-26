package com.omilator.data.saves

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SaveState(
    val id: String,
    val gameId: String,
    val slot: Int,
    val createdAt: Instant,
    val thumbnailPath: String? = null,
    val sizeBytes: Long,
)

class SaveStateRepository(
    private val listDir: suspend (String) -> List<String>,
    private val fileSize: suspend (String) -> Long?,
) {
    suspend fun list(gameId: String, directory: String): List<SaveState> {
        val files = listDir(directory).filter { it.startsWith(gameId) && it.endsWith(".state") }
        return files.mapIndexed { index, path ->
            SaveState(
                id = path,
                gameId = gameId,
                slot = index,
                createdAt = Clock.System.now(),
                sizeBytes = fileSize(path) ?: 0L,
            )
        }
    }
}
