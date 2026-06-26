package com.omilator.data.library

class IosLibraryScanner : LibraryScanner {
    override suspend fun scan(directory: String): List<Game> {
        return emptyList()
    }
}
