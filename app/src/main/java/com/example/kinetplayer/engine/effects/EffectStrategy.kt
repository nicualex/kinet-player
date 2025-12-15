package com.example.kinetplayer.engine.effects

import com.example.kinetplayer.model.PixelMap

interface EffectStrategy {
    fun update(pixelMap: PixelMap, time: Float)
}
