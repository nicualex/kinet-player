package com.example.kinetplayer.model

import androidx.compose.ui.graphics.Color

/**
 * Represents a single lighting fixture or pixel.
 */
data class Fixture(
    val id: Int,
    val x: Int, // Grid X position
    val y: Int, // Grid Y position
    val universe: Int, // DMX Universe (or Port for KiNET v2)
    val channel: Int, // Starting DMX Channel (1-512)
    var color: Color = Color.Black // Current color state
)
