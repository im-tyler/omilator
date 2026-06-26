package com.omilator.data.library

interface LibraryScanner {
    suspend fun scan(directory: String): List<Game>
}

class LibraryRepository(
    private val scanner: LibraryScanner,
) {
    private var cached: List<Game> = emptyList()

    suspend fun rescan(directory: String): List<Game> {
        cached = scanner.scan(directory)
        return cached
    }

    fun games(): List<Game> = cached

    fun gamesBySystem(system: GameSystem): List<Game> =
        cached.filter { it.system == system }

    fun search(query: String): List<Game> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return cached
        return cached.filter { it.title.lowercase().contains(q) }
    }
}
