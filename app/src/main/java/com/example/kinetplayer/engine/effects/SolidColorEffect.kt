package com.example.kinetplayer.engine.effects

import androidx.compose.ui.graphics.Color
import com.example.kinetplayer.model.PixelMap

class SolidColorEffect(private val color: Color = Color.Blue) : EffectStrategy {
    override fun update(pixelMap: PixelMap, time: Float) {
        pixelMap.fixtures.forEach { fixture ->
            fixture.color = color
        }
    }
}
