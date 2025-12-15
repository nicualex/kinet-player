package com.example.kinetplayer.engine.effects

import androidx.compose.ui.graphics.Color
import com.example.kinetplayer.model.PixelMap

class RainbowEffect : EffectStrategy {
    override fun update(pixelMap: PixelMap, time: Float) {
        pixelMap.fixtures.forEach { fixture ->
            val hue = (fixture.x * 10 + fixture.y * 10 + time * 20) % 360
            fixture.color = Color.hsv(hue, 1f, 1f)
        }
    }
}
