package com.example.kinetplayer.model

import com.google.gson.annotations.SerializedName

data class ShowManifest(
    val version: Int,
    val name: String,
    val mediaFile: String,
    val mediaTransform: MediaTransform? = null,
    val fixtures: List<FixtureDefinition>,
    val settings: PlaybackSettings
)

data class MediaTransform(
    val scaleX: Float,
    val scaleY: Float,
    val translateX: Float,
    val translateY: Float,
    val rotation: Float,
    val crop: CropRect? = null
)

data class CropRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class FixtureDefinition(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val protocol: String,
    val width: Int,
    val height: Int,
    val pixels: List<PixelDefinition>
)

data class PixelDefinition(
    val id: String,
    val x: Int,
    val y: Int,
    val fixtureId: String,
    val dmxInfo: DmxInfo
)

data class DmxInfo(
    val universe: Int,
    val channel: Int
)

data class PlaybackSettings(
    val loop: Boolean,
    val autoPlay: Boolean
)
