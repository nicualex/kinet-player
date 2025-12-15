package com.example.kinetplayer.source

import android.graphics.Bitmap

/**
 * Abstraction for any source that provides video frames.
 */
interface FrameSource {
    /**
     * Returns the current frame as a Bitmap.
     * Returns null if no frame is available.
     */
    fun getCurrentFrame(): Bitmap?
    
    /**
     * Prepare the source (e.g., connect to NDI stream).
     */
    fun start()
    
    /**
     * Release resources.
     */
    fun stop()
}
