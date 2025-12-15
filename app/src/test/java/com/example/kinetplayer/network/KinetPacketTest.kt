package com.example.kinetplayer.network

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class KinetPacketTest {

    @Test
    fun `test KinetV1Packet serialization`() {
        val data = byteArrayOf(0xFF.toByte(), 0x00.toByte(), 0x00.toByte()) // Red
        val packet = KinetV1Packet(
            universe = 1,
            data = data
        )

        val bytes = packet.toBytes()

        // Verify Header
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0x0401dc4a, buffer.int) // Magic
        assertEquals(0x0001.toShort(), buffer.short) // Version
        assertEquals(0x0101.toShort(), buffer.short) // Type
        assertEquals(0, buffer.int) // Sequence
        assertEquals(0.toByte(), buffer.get()) // Port
        assertEquals(0.toByte(), buffer.get()) // Padding
        assertEquals(0.toShort(), buffer.short) // Flags
        assertEquals(0, buffer.int) // Timer
        assertEquals(1, buffer.int) // Universe

        // Verify Data
        val actualData = ByteArray(3)
        buffer.get(actualData)
        assertArrayEquals(data, actualData)
    }

    @Test
    fun `test KinetV2Packet serialization`() {
        val data = byteArrayOf(0x00.toByte(), 0xFF.toByte(), 0x00.toByte()) // Green
        val packet = KinetV2Packet(
            universe = 2,
            data = data
        )

        val bytes = packet.toBytes()

        // Verify Header
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0x0401dc4a, buffer.int) // Magic
        assertEquals(0x0002.toShort(), buffer.short) // Version
        assertEquals(0x0108.toShort(), buffer.short) // Type
        assertEquals(0, buffer.int) // Sequence
        assertEquals(2, buffer.int) // Universe/Port ID
        assertEquals(0.toByte(), buffer.get()) // Port
        assertEquals(0.toByte(), buffer.get()) // Padding
        assertEquals(0.toShort(), buffer.short) // Flags
        assertEquals(3.toShort(), buffer.short) // Length

        // Verify Data
        val actualData = ByteArray(3)
        buffer.get(actualData)
        assertArrayEquals(data, actualData)
    }
}
