package com.omilator.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omilator.data.library.Game

@Composable
fun GameCard(
    game: Game,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            coverGradientTop(game),
                            coverGradientBottom(game),
                        ),
                    ),
                ),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                text = game.system.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(8.dp),
            )
        }
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun coverGradientTop(game: Game): Color =
    when (game.system) {
        com.omilator.data.library.GameSystem.NES -> Color(0xFFB23A48)
        com.omilator.data.library.GameSystem.SNES -> Color(0xFF4A6FA5)
        com.omilator.data.library.GameSystem.GAME_BOY -> Color(0xFF6B8E23)
        com.omilator.data.library.GameSystem.GAME_BOY_COLOR -> Color(0xFFB8860B)
        com.omilator.data.library.GameSystem.GAME_BOY_ADVANCE -> Color(0xFF8B3A62)
        com.omilator.data.library.GameSystem.GENESIS -> Color(0xFF2D4356)
        com.omilator.data.library.GameSystem.NINTENDO_64 -> Color(0xFF5D4E75)
        com.omilator.data.library.GameSystem.PLAYSTATION -> Color(0xFF1F4E79)
    }

private fun coverGradientBottom(game: Game): Color =
    coverGradientTop(game).copy(alpha = 0.6f)
