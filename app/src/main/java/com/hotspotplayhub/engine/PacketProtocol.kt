package com.hotspotplayhub.engine

import java.nio.ByteBuffer

/**
 * Binary packet protocol for Hotspot PlayHub
 * 
 * Packet structure:
 * | Type (1 byte) | PlayerID (1 byte) | Payload length (2 bytes) | Payload (variable) |
 */
object PacketProtocol {
    
    // Packet types
    const val TYPE_JOIN: Byte = 0x01
    const val TYPE_LEAVE: Byte = 0x02
    const val TYPE_CHAT: Byte = 0x03
    const val TYPE_GAME_INPUT: Byte = 0x04
    const val TYPE_GAME_STATE: Byte = 0x05
    const val TYPE_CLIPBOARD: Byte = 0x06
    const val TYPE_PHONE_FIND: Byte = 0x07
    const val TYPE_LOBBY_UPDATE: Byte = 0x08
    const val TYPE_MODULE_SWITCH: Byte = 0x09
    const val TYPE_HEARTBEAT: Byte = 0x0A
    
    const val HEADER_SIZE = 4 // Type + PlayerID + Length
    
    /**
     * Serialize a packet into binary format
     */
    fun serialize(type: Byte, playerId: Byte, payload: ByteArray): ByteArray {
        val length = payload.size.toShort()
        val buffer = ByteBuffer.allocate(HEADER_SIZE + payload.size)
        buffer.put(type)
        buffer.put(playerId)
        buffer.putShort(length)
        buffer.put(payload)
        return buffer.array()
    }
    
    /**
     * Parse binary packet
     */
    fun parse(buffer: ByteArray): Packet {
        val byteBuffer = ByteBuffer.wrap(buffer)
        val type = byteBuffer.get()
        val playerId = byteBuffer.get()
        val length = byteBuffer.short.toInt()
        val payload = ByteArray(length)
        byteBuffer.get(payload)
        return Packet(type, playerId, payload)
    }
    
    /**
     * Create a text message packet (for chat, lobby messages, etc.)
     */
    fun createTextPacket(type: Byte, playerId: Byte, message: String): ByteArray {
        val payload = message.toByteArray(Charsets.UTF_8)
        return serialize(type, playerId, payload)
    }
    
    /**
     * Extract text from payload
     */
    fun extractText(payload: ByteArray): String {
        return String(payload, Charsets.UTF_8)
    }
}

/**
 * Packet data class
 */
data class Packet(
    val type: Byte,
    val playerId: Byte,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Packet
        if (type != other.type) return false
        if (playerId != other.playerId) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }
    
    override fun hashCode(): Int {
        var result = type.toInt()
        result = 31 * result + playerId
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

fun Packet.toBytes(): ByteArray {
    return PacketProtocol.serialize(type, playerId, payload)
}
