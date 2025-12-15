package com.example.kinetplayer.model

/**
 * Represents the entire grid of fixtures.
 */
data class PixelMap(
    val width: Int,
    val height: Int,
    val fixtures: List<Fixture>
) {
    companion object {
        fun fromManifest(manifest: ShowManifest): PixelMap {
            // Flatten all pixels from all fixtures
            val runtimeFixtures = manifest.fixtures.flatMap { fixtureDef ->
                fixtureDef.pixels.map { pixelDef ->
                    Fixture(
                        id = pixelDef.id.hashCode(), // Use hashcode or custom ID logic
                        x = pixelDef.x,
                        y = pixelDef.y,
                        universe = pixelDef.dmxInfo.universe,
                        channel = pixelDef.dmxInfo.channel
                        // Color init to black
                    )
                }
            }
            // Logic width/height might be explicit in manifest or calculated?
            // Manifest has individual fixture width/height, but not GLOBAL grid size?
            // Usually we want global bounding box.
            val maxX = runtimeFixtures.maxOfOrNull { it.x } ?: 0
            val maxY = runtimeFixtures.maxOfOrNull { it.y } ?: 0
            
            return PixelMap(maxX + 1, maxY + 1, runtimeFixtures)
        }

        /**
         * Creates a simple rectangular grid of fixtures.
         * Assumes 3 channels per fixture (RGB).
         */
        fun createGrid(width: Int, height: Int, startUniverse: Int = 0): PixelMap {
            val fixtures = mutableListOf<Fixture>()
            var currentUniverse = startUniverse
            var currentChannel = 1
            var idCounter = 0

            for (y in 0 until height) {
                for (x in 0 until width) {
                    // Check if we need to jump to next universe
                    if (currentChannel + 2 > 512) {
                        currentUniverse++
                        currentChannel = 1
                    }

                    fixtures.add(
                        Fixture(
                            id = idCounter++,
                            x = x,
                            y = y,
                            universe = currentUniverse,
                            channel = currentChannel
                        )
                    )
                    currentChannel += 3 // RGB
                }
            }
            return PixelMap(width, height, fixtures)
        }
    }
}
