package com.hotspotplayhub.modules.chat

import android.util.Log
import com.hotspotplayhub.engine.Module
import com.hotspotplayhub.engine.Packet
import com.hotspotplayhub.engine.PacketProtocol
import com.hotspotplayhub.engine.Player
import com.hotspotplayhub.engine.Server

/**
 * Chat module for text messaging between players
 */
class ChatModule(private val server: Server) : Module {
    
    private val messages = mutableListOf<ChatMessage>()
    
    var onMessageReceived: ((ChatMessage) -> Unit)? = null
    
    companion object {
        private const val TAG = "ChatModule"
    }
    
    override fun onStart() {
        Log.d(TAG, "Chat module started")
        messages.clear()
    }
    
    override fun onMessage(packet: Packet, fromPlayer: Player) {
        if (packet.type == PacketProtocol.TYPE_CHAT) {
            val text = PacketProtocol.extractText(packet.payload)
            
            val message = ChatMessage(
                playerName = fromPlayer.name,
                playerId = fromPlayer.id,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            
            messages.add(message)
            
            // Broadcast to all clients
            server.broadcast(packet)
            
            onMessageReceived?.invoke(message)
            
            Log.d(TAG, "Chat message from ${fromPlayer.name}: $text")
        }
    }
    
    override fun onStop() {
        Log.d(TAG, "Chat module stopped")
    }
    
    override fun getName(): String = "Chat"
    
    /**
     * Get all chat messages
     */
    fun getMessages(): List<ChatMessage> = messages.toList()
}

/**
 * Chat message data class
 */
data class ChatMessage(
    val playerName: String,
    val playerId: Byte,
    val text: String,
    val timestamp: Long
)
