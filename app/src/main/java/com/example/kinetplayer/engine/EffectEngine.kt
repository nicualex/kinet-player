package com.example.kinetplayer.engine

import androidx.compose.ui.graphics.Color
import com.example.kinetplayer.engine.effects.EffectStrategy
import com.example.kinetplayer.model.PixelMap
import com.example.kinetplayer.network.KinetSender
import com.example.kinetplayer.network.KinetV1Packet
import com.example.kinetplayer.source.FrameSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EffectEngine(
    private val sender: KinetSender,
    private val pixelMap: PixelMap,
    private val frameSource: FrameSource? = null,
    private val effectStrategy: EffectStrategy
) {
    private var job: Job? = null
    private var isRunning = false
    private var time = 0f

    fun start(scope: CoroutineScope) {
        if (isRunning) return
        isRunning = true
        
        frameSource?.start()

        job = scope.launch(Dispatchers.Default) {
            while (isActive && isRunning) {
                updatePixels(time)
                sendData()
                time += 0.1f
                delay(33) // ~30 FPS
            }
        }
    }

    fun stop() {
        isRunning = false
        frameSource?.stop()
        job?.cancel()
        job = null
    }

    private fun updatePixels(t: Float) {
        val frame = frameSource?.getCurrentFrame()
        
        if (frame != null) {
            // Use Video/NDI Frame
            BitmapSampler.sample(frame, pixelMap, pixelMap.fixtures)
        } else {
            // Use Selected Effect
            effectStrategy.update(pixelMap, t)
        }
    }

    private suspend fun sendData() {
        // Group fixtures by universe
        val universeGroups = pixelMap.fixtures.groupBy { it.universe }

        universeGroups.forEach { (universe, fixtures) ->
            val dmxData = ByteArray(512)
            fixtures.forEach { fixture ->
                if (fixture.channel + 2 <= 512) {
                    // Convert Color to RGB bytes
                    // Note: Color.red/green/blue are 0.0-1.0 floats
                    dmxData[fixture.channel - 1] = (fixture.color.red * 255).toInt().toByte()
                    dmxData[fixture.channel] = (fixture.color.green * 255).toInt().toByte()
                    dmxData[fixture.channel + 1] = (fixture.color.blue * 255).toInt().toByte()
                }
            }

            val packet = KinetV1Packet(
                universe = universe,
                data = dmxData
            )
            sender.sendPacket(packet)
        }
    }
}
