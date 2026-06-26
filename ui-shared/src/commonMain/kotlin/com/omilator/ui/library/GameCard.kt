package com.omilator.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omilator.data.library.Game
import com.omilator.data.library.GameSystem

@Composable
fun GameCard(
    game: Game,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            coverTopFor(game.system),
                            coverBottomFor(game.system),
                        ),
                    ),
                ),
            contentAlignment = Alignment.BottomStart,
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = game.system.shortLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = game.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

private fun GameSystem.shortLabel(): String = when (this) {
    GameSystem.NES -> "NES"
    GameSystem.SNES -> "SNES"
    GameSystem.GAME_BOY -> "GB"
    GameSystem.GAME_BOY_COLOR -> "GBC"
    GameSystem.GAME_BOY_ADVANCE -> "GBA"
    GameSystem.GENESIS -> "MD"
    GameSystem.NINTENDO_64 -> "N64"
    GameSystem.PLAYSTATION -> "PS1"
    GameSystem.NINTENDO_DS -> "DS"
    GameSystem.PSP -> "PSP"
    GameSystem.GAMECUBE -> "GC"
    GameSystem.WII -> "Wii"
    GameSystem.NINTENDO_3DS -> "3DS"
    GameSystem.PLAYSTATION_2 -> "PS2"
    GameSystem.DREAMCAST -> "DC"
    GameSystem.SATURN -> "SAT"
}

private fun coverTopFor(system: GameSystem): Color = when (system) {
    GameSystem.NES -> Color(0xFFB23A48)
    GameSystem.SNES -> Color(0xFF4A6FA5)
    GameSystem.GAME_BOY -> Color(0xFF6B8E23)
    GameSystem.GAME_BOY_COLOR -> Color(0xFFD1772B)
    GameSystem.GAME_BOY_ADVANCE -> Color(0xFF8B3A62)
    GameSystem.GENESIS -> Color(0xFF2D4356)
    GameSystem.NINTENDO_64 -> Color(0xFF5D4E75)
    GameSystem.PLAYSTATION -> Color(0xFF1F4E79)
    GameSystem.NINTENDO_DS -> Color(0xFF777777)
    GameSystem.PSP -> Color(0xFF003366)
    GameSystem.GAMECUBE -> Color(0xFF6A0DAD)
    GameSystem.WII -> Color(0xFF8BABC4)
    GameSystem.NINTENDO_3DS -> Color(0xFFA52A2A)
    GameSystem.PLAYSTATION_2 -> Color(0xFF1E4D88)
    GameSystem.DREAMCAST -> Color(0xFFFF6600)
    GameSystem.SATURN -> Color(0xFF3399CC)
}

private fun coverBottomFor(system: GameSystem): Color =
    coverTopFor(system).copy(alpha = 0.55f)
