package com.omilator.data.launcher

actual class StandaloneRegistry {
    actual val all: List<StandaloneBackend> = emptyList()
    actual fun available(): List<StandaloneBackend> = emptyList()
    actual fun forSystem(systemId: String): StandaloneBackend? = null
}
