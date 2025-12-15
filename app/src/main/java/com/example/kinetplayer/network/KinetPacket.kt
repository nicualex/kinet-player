package com.example.kinetplayer.network

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Represents a KiNET packet.
 */
sealed interface KinetPacket {
    fun toBytes(): ByteArray
}

/**
 * KiNET v1 Packet (Type 0x0101)
 * Used for simple DMX data transmission.
 */
data class KinetV1Packet(
    val version: Short = 0x0001,
    val type: Short = 0x0101,
    val sequence: Int = 0,
    val port: Byte = 0,
    val padding: Byte = 0,
    val flags: Short = 0,
    val timer: Int = 0,
    val universe: Int,
    val data: ByteArray
) : KinetPacket {
    override fun toBytes(): ByteArray {
        val buffer = ByteBuffer.allocate(24 + data.size)
        buffer.order(ByteOrder.LITTLE_ENDIAN) // KiNET uses Little Endian

        buffer.putInt(0x0401dc4a) // Magic Number
        buffer.putShort(version)
        buffer.putShort(type)
        buffer.putInt(sequence)
        buffer.put(port)
        buffer.put(padding)
        buffer.putShort(flags)
        buffer.putInt(timer)
        buffer.putInt(universe)
        buffer.put(data)

        return buffer.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KinetV1Packet

        if (version != other.version) return false
        if (type != other.type) return false
        if (sequence != other.sequence) return false
        if (port != other.port) return false
        if (padding != other.padding) return false
        if (flags != other.flags) return false
        if (timer != other.timer) return false
        if (universe != other.universe) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.toInt()
        result = 31 * result + type
        result = 31 * result + sequence
        result = 31 * result + port
        result = 31 * result + padding
        result = 31 * result + flags
        result = 31 * result + timer
        result = 31 * result + universe
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * KiNET v2 Packet (Type 0x0108 - PORTOUT)
 * Supports synchronization and multiple ports.
 */
data class KinetV2Packet(
    val version: Short = 0x0002,
    val type: Short = 0x0108,
    val sequence: Int = 0,
    val universe: Int, // In v2, this is often mapped to Port ID
    val port: Byte = 0,
    val padding: Byte = 0,
    val flags: Short = 0, // Bit 0: Sync
    val data: ByteArray
) : KinetPacket {
    override fun toBytes(): ByteArray {
        // V2 Header is larger
        val buffer = ByteBuffer.allocate(24 + data.size) // Adjust size as needed for full V2 spec
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(0x0401dc4a) // Magic Number
        buffer.putShort(version)
        buffer.putShort(type)
        buffer.putInt(sequence)
        buffer.putInt(universe) // Port/Universe ID
        buffer.put(port)
        buffer.put(padding)
        buffer.putShort(flags)
        // Note: V2 has more fields like Length, but for basic PORTOUT this is often sufficient or needs adjustment based on specific device requirements.
        // For strict V2 compliance, we might need 'Length' field before data.
        // Let's stick to a simplified V2 structure often accepted by controllers, or refine if needed.
        // Actually, standard V2 PORTOUT (0x0108) structure:
        // Magic (4), Ver (2), Type (2), Seq (4), Universe/Port (4), Port (1), Pad (1), Flags (2), Length (2), Data (...)
        
        // Re-allocating to include Length
        val realBuffer = ByteBuffer.allocate(26 + data.size)
        realBuffer.order(ByteOrder.LITTLE_ENDIAN)
        realBuffer.putInt(0x0401dc4a)
        realBuffer.putShort(version)
        realBuffer.putShort(type)
        realBuffer.putInt(sequence)
        realBuffer.putInt(universe)
        realBuffer.put(port)
        realBuffer.put(padding)
        realBuffer.putShort(flags)
        realBuffer.putShort(data.size.toShort()) // Length
        realBuffer.put(data)

        return realBuffer.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KinetV2Packet

        if (version != other.version) return false
        if (type != other.type) return false
        if (sequence != other.sequence) return false
        if (universe != other.universe) return false
        if (port != other.port) return false
        if (padding != other.padding) return false
        if (flags != other.flags) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.toInt()
        result = 31 * result + type
        result = 31 * result + sequence
        result = 31 * result + universe
        result = 31 * result + port
        result = 31 * result + padding
        result = 31 * result + flags
        result = 31 * result + data.contentHashCode()
        return result
    }
}
