package com.example.kinetplayer.network

import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.charset.Charset

class DiscoveryService {
    private var job: Job? = null
    private var socket: DatagramSocket? = null
    var deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    private val PORT = 6969
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null
    var onLog: ((String) -> Unit)? = null

    fun start(context: android.content.Context) {
        if (job != null) return
        
        // Acquire Multicast Lock
        val wifi = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        multicastLock = wifi.createMulticastLock("kinetDiscovery")
        multicastLock?.setReferenceCounted(true)
        multicastLock?.acquire()
        
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("DiscoveryService", "Starting Discovery Listener on $PORT")
                socket = DatagramSocket(PORT)
                socket?.broadcast = true
                val buffer = ByteArray(1024)
                
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    
                    val message = String(packet.data, 0, packet.length, Charset.forName("UTF-8")).trim()
                    Log.d("DiscoveryService", "Received: $message from ${packet.address.hostAddress}")
                    onLog?.invoke("Rx: $message from ${packet.address.hostAddress}")
                    
                    if (message == "KINCOM_DISCOVER") {
                        val responseJson = JSONObject().apply {
                            put("type", "kinet-player")
                            put("name", deviceName)
                            // We don't necessarily need to send IP, the receiver has it.
                        }
                        
                        val responseBytes = responseJson.toString().toByteArray()
                        val responsePacket = DatagramPacket(
                            responseBytes,
                            responseBytes.size,
                            packet.address,
                            packet.port
                        )
                        socket?.send(responsePacket)
                        Log.d("DiscoveryService", "Sent response to ${packet.address.hostAddress}:${packet.port}")
                        onLog?.invoke("Tx: To ${packet.address.hostAddress}:${packet.port}")
                    }
                }
            } catch (e: Exception) {
                Log.e("DiscoveryService", "Critical Error in Discovery Loop: ${e.message}", e)
                e.printStackTrace()
            } finally {
                socket?.close()
                if (multicastLock?.isHeld == true) {
                     multicastLock?.release()
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        socket?.close()
        socket = null
        if (multicastLock?.isHeld == true) {
            multicastLock?.release()
        }
        multicastLock = null
        Log.d("DiscoveryService", "Stopped")
    }
}
