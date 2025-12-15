package com.example.kinetplayer.engine

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import com.example.kinetpixelmapper.model.Fixture
import com.example.kinetpixelmapper.model.PixelMap
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BitmapSamplerTest {

    @Test
    fun `test sample maps bitmap colors to fixtures`() {
        // Create a 2x2 Bitmap
        // Top-Left: Red, Top-Right: Green
        // Bottom-Left: Blue, Bottom-Right: White
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, AndroidColor.RED)
        bitmap.setPixel(1, 0, AndroidColor.GREEN)
        bitmap.setPixel(0, 1, AndroidColor.BLUE)
        bitmap.setPixel(1, 1, AndroidColor.WHITE)

        // Create a 2x2 PixelMap
        val fixtures = listOf(
            Fixture(0, 0, 0, 1, 1), // Top-Left
            Fixture(1, 1, 0, 1, 4), // Top-Right
            Fixture(2, 0, 1, 1, 7), // Bottom-Left
            Fixture(3, 1, 1, 1, 10) // Bottom-Right
        )
        val pixelMap = PixelMap(2, 2, fixtures)

        // Run Sampler
        BitmapSampler.sample(bitmap, pixelMap, fixtures)

        // Verify Colors
        // Note: Compose Color(1f, 0f, 0f) is Red
        assertEquals(Color(1f, 0f, 0f), fixtures[0].color)
        assertEquals(Color(0f, 1f, 0f), fixtures[1].color)
        assertEquals(Color(0f, 0f, 1f), fixtures[2].color)
        assertEquals(Color(1f, 1f, 1f), fixtures[3].color)
    }
}
