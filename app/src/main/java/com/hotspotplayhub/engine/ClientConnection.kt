package com.hotspotplayhub.engine

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue

/**
 * Manages connection for a single client
 */
class ClientConnection(
    val socket: Socket,
    val player: Player
) {
    private val output = DataOutputStream(socket.getOutputStream())
    private val input = DataInputStream(socket.getInputStream())
    
    val sendQueue = LinkedBlockingQueue<ByteArray>()
    
    @Volatile
    var isConnected = true
    
    /**
     * Read a packet from the client
     */
    fun readPacket(): Packet? {
        return try {
            val size = input.readInt()
            val buffer = ByteArray(size)
            input.readFully(buffer)
            PacketProtocol.parse(buffer)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Send a packet to the client
     */
    fun sendPacket(packet: ByteArray) {
        try {
            synchronized(output) {
                output.writeInt(packet.size)
                output.write(packet)
                output.flush()
            }
        } catch (e: Exception) {
            isConnected = false
        }
    }
    
    /**
     * Close the connection
     */
    fun close() {
        isConnected = false
        try {
            socket.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
