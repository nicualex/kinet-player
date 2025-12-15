package com.example.kinetplayer.engine

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import com.example.kinetplayer.model.Fixture
import com.example.kinetplayer.model.PixelMap

object BitmapSampler {
    fun sample(bitmap: Bitmap, pixelMap: PixelMap, fixtures: List<Fixture>) {
        for (fixture in fixtures) {
             // Access pixel directly assuming 1:1 mapping because we resized the bitmap
             // Validation to avoid crashes if map/bitmap mismatch
             if (fixture.x < bitmap.width && fixture.y < bitmap.height) {
                val pixel = bitmap.getPixel(fixture.x, fixture.y)
                
                val red = AndroidColor.red(pixel) / 255f
                val green = AndroidColor.green(pixel) / 255f
                val blue = AndroidColor.blue(pixel) / 255f
                
                fixture.color = Color(red, green, blue)
             }
        }
    }
}
