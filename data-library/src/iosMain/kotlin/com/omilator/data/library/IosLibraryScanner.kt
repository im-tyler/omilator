@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.data.library

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSFileManager
import platform.Foundation.NSDirectoryEnumerationSkipsHiddenFiles

class IosLibraryScanner : LibraryScanner {
    override suspend fun scan(directory: String): List<Game> = withContext(Dispatchers.Default) {
        // On iOS, scan the app's Documents directory for ROMs.
        // Users add ROMs via Finder > iPhone > Files > Omilator (file sharing).
        val documentsDir = documentsDirectory() ?: return@withContext emptyList()
        scanDirectory(documentsDir)
    }

    private fun documentsDirectory(): String? {
        val paths = NSSearchPathForDirectoriesInDomains(
            platform.Foundation.NSDocumentDirectory,
            platform.Foundation.NSUserDomainMask,
            true,
        )
        return paths.firstOrNull() as? String
    }

    private fun scanDirectory(dirPath: String): List<Game> {
        val fileManager = NSFileManager.defaultManager
        val contents = fileManager.contentsOfDirectoryAtPath(dirPath, null)
            ?: return emptyList()

        return (contents as List<String>)
            .filter { filename ->
                val ext = filename.substringAfterLast('.', "")
                GameSystem.detectByExtension(ext) != null
            }
            .mapNotNull { filename ->
                val ext = filename.substringAfterLast('.', "")
                val system = GameSystem.detectByExtension(ext) ?: return@mapNotNull null
                val fullPath = "$dirPath/$filename"
                val attrs = fileManager.attributesOfItemAtPath(fullPath, null)
                val size = (attrs?.get("NSFileSize") as? Long) ?: 0L
                Game(
                    id = fullPath,
                    title = cleanRomTitle(filename.substringBeforeLast('.')),
                    system = system,
                    filePath = fullPath,
                    fileSizeBytes = size,
                )
            }
            .sortedBy { it.title.lowercase() }
    }
}

