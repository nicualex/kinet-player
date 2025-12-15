package com.example.kinetplayer.engine.effects

import androidx.compose.ui.graphics.Color
import com.example.kinetplayer.model.PixelMap
import kotlin.random.Random

class SparkleEffect : EffectStrategy {
    override fun update(pixelMap: PixelMap, time: Float) {
        pixelMap.fixtures.forEach { fixture ->
            // Fade out
            val currentAlpha = fixture.color.alpha
            if (currentAlpha > 0.05f) {
                fixture.color = fixture.color.copy(alpha = currentAlpha * 0.9f)
            } else {
                fixture.color = Color.Black
            }

            // Randomly ignite
            if (Random.nextFloat() > 0.98f) {
                fixture.color = Color.White
            }
        }
    }
}
