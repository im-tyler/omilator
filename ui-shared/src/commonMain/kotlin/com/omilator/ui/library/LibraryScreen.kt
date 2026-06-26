package com.omilator.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omilator.data.library.GameSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onAddDirectory: () -> Unit,
    onOpenGame: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Omilator", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onAddDirectory) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add ROM directory")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(padding)
            state.games.isEmpty() -> EmptyState(padding, onAddDirectory)
            else -> GridState(padding, state.visibleGames, onOpenGame)
        }
    }
}

@Composable
private fun LoadingState(padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("Scanning library...", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyState(padding: PaddingValues, onAddDirectory: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No games yet",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Add a directory of ROMs to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            IconButton(onClick = onAddDirectory, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = "Add")
            }
        }
    }
}

@Composable
private fun GridState(
    padding: PaddingValues,
    games: List<com.omilator.data.library.Game>,
    onOpenGame: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(games, key = { it.id }) { game ->
            GameCard(game = game)
        }
    }
}
