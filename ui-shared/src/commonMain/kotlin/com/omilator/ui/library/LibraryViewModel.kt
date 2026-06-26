package com.omilator.ui.library

import com.omilator.data.library.Game
import com.omilator.data.library.GameSystem
import com.omilator.data.library.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = false,
    val games: List<Game> = emptyList(),
    val selectedSystem: GameSystem? = null,
    val searchQuery: String = "",
    val error: String? = null,
) {
    val visibleGames: List<Game>
        get() {
            val bySystem = selectedSystem?.let { sys -> games.filter { it.system == sys } } ?: games
            val q = searchQuery.trim().lowercase()
            return if (q.isEmpty()) bySystem else bySystem.filter { it.title.lowercase().contains(q) }
        }
}

class LibraryViewModel(
    private val repository: LibraryRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    fun setSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun selectSystem(system: GameSystem?) {
        _state.value = _state.value.copy(selectedSystem = system)
    }

    fun rescan(directory: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val games = repository.rescan(directory)
                _state.value = _state.value.copy(isLoading = false, games = games)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(isLoading = false, error = t.message ?: "Unknown error")
            }
        }
    }
}
