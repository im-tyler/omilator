package com.omilator.core.libretro.api

interface CoreController {
    val isLoaded: Boolean

    suspend fun loadCore(path: String): SystemInfo
    suspend fun loadGame(romPath: String): AvInfo

    fun runFrame()

    fun reset()
    fun unloadGame()
    fun unloadCore()

    fun attach(video: VideoSink, audio: AudioSink, input: InputSource)
    fun detach()

    fun saveState(path: String): Boolean
    fun loadState(path: String): Boolean

    val memorySize: UInt
    fun readMemory(region: UInt, offset: UInt, size: UInt): ByteArray
    fun writeMemory(region: UInt, offset: UInt, data: ByteArray)
}

class CoreNotLoadedException(message: String) : RuntimeException(message)
class CoreLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class GameLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
