import com.hotspotplayhub.engine.toBytes
package com.hotspotplayhub.modules.clipboard

import android.util.Log
import com.hotspotplayhub.engine.Module
import com.hotspotplayhub.engine.Packet
import com.hotspotplayhub.engine.PacketProtocol
import com.hotspotplayhub.engine.Player
import com.hotspotplayhub.engine.Server

/**
 * Clipboard module for sharing text between devices
 */
class ClipboardModule(private val server: Server) : Module {
    
    var onClipboardReceived: ((ClipboardData) -> Unit)? = null
    
    companion object {
        private const val TAG = "ClipboardModule"
    }
    
    override fun onStart() {
        Log.d(TAG, "Clipboard module started")
    }
    
    override fun onMessage(packet: Packet, fromPlayer: Player) {
        if (packet.type == PacketProtocol.TYPE_CLIPBOARD) {
            val text = PacketProtocol.extractText(packet.payload)
            
            val clipboardData = ClipboardData(
                playerName = fromPlayer.name,
                playerId = fromPlayer.id,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            
            // Broadcast to all clients
            server.broadcast(packet.toBytes())
            
            onClipboardReceived?.invoke(clipboardData)
            
            Log.d(TAG, "Clipboard from ${fromPlayer.name}: ${text.take(50)}")
        }
    }
    
    override fun onStop() {
        Log.d(TAG, "Clipboard module stopped")
    }
    
    override fun getName(): String = "Clipboard"
    
    /**
     * Share clipboard text
     */
    fun shareClipboard(playerId: Byte, text: String) {
        val packet = PacketProtocol.createTextPacket(
            PacketProtocol.TYPE_CLIPBOARD,
            playerId,
            text
        )
        server.broadcast(packet)
    }
}

/**
 * Clipboard data class
 */
data class ClipboardData(
    val playerName: String,
    val playerId: Byte,
    val text: String,
    val timestamp: Long
)
