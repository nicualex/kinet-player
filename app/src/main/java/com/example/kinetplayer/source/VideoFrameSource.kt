package com.example.kinetplayer.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import com.example.kinetplayer.model.PixelMap

/**
 * FrameSource that plays a video file and extracts frames from a TextureView.
 */
class VideoFrameSource(
    private val context: Context,
    private val videoUri: Uri,
    private val textureView: TextureView,
    private val pixelMap: PixelMap
) : FrameSource, TextureView.SurfaceTextureListener {

    private var mediaPlayer: MediaPlayer? = null
    private var surface: Surface? = null

    init {
        textureView.surfaceTextureListener = this
    }

    override fun start() {
        if (textureView.isAvailable) {
            startPlayer(textureView.surfaceTexture!!)
        }
        // If not available, onSurfaceTextureAvailable will be called later
    }

    private fun startPlayer(surfaceTexture: SurfaceTexture) {
        try {
            surface = Surface(surfaceTexture)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, videoUri)
                setSurface(surface)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        surface?.release()
        surface = null
    }

    override fun getCurrentFrame(): Bitmap? {
        // Downscale directly to grid size for performance
        return textureView.getBitmap(pixelMap.width, pixelMap.height)
    }

    // TextureView Listener
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        startPlayer(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stop()
        return true
    }
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
}
