package com.example.kinetplayer.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class KinetSender(private val destinationIp: String, private val destinationPort: Int = 6038) {

    private var socket: DatagramSocket? = null

    suspend fun sendPacket(packet: KinetPacket) {
        withContext(Dispatchers.IO) {
            try {
                if (socket == null || socket!!.isClosed) {
                    socket = DatagramSocket()
                }

                val data = packet.toBytes()
                val address = InetAddress.getByName(destinationIp)
                val udpPacket = DatagramPacket(data, data.size, address, destinationPort)

                socket?.send(udpPacket)
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error (log it, etc.)
            }
        }
    }

    fun close() {
        socket?.close()
        socket = null
    }
}
