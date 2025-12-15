package com.example.kinetplayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.kinetplayer.model.Fixture

@Composable
fun PixelGridPreview(
    width: Int,
    height: Int,
    fixtures: List<Fixture>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(width.toFloat() / height.toFloat())
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / width
            val cellHeight = size.height / height
            val radius = minOf(cellWidth, cellHeight) / 2 * 0.8f

            fixtures.forEach { fixture ->
                val centerX = fixture.x * cellWidth + cellWidth / 2
                val centerY = fixture.y * cellHeight + cellHeight / 2
                
                drawCircle(
                    color = fixture.color,
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}
