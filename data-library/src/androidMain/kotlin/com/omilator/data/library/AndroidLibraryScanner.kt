package com.omilator.data.library

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidLibraryScanner : LibraryScanner {
    override suspend fun scan(directory: String): List<Game> = withContext(Dispatchers.IO) {
        val root = File(directory)
        if (!root.isDirectory) return@withContext emptyList()

        root.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                GameSystem.detectByExtension(file.extension) != null
            }
            .map { file ->
                val system = GameSystem.detectByExtension(file.extension)!!
                Game(
                    id = file.absolutePath,
                    title = file.nameWithoutExtension,
                    system = system,
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                )
            }
            .sortedBy { it.title.lowercase() }
            .toList()
    }
}
