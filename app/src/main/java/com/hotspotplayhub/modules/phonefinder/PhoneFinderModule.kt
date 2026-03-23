package com.hotspotplayhub.modules.phonefinder

import android.util.Log
import com.hotspotplayhub.engine.Module
import com.hotspotplayhub.engine.Packet
import com.hotspotplayhub.engine.PacketProtocol
import com.hotspotplayhub.engine.toBytes
import com.hotspotplayhub.engine.Player
import com.hotspotplayhub.engine.Server

/**
 * Phone Finder module - makes selected phones play a ringtone
 */
class PhoneFinderModule(private val server: Server) : Module {
    
    var onRingRequested: ((Byte) -> Unit)? = null
    
    companion object {
        private const val TAG = "PhoneFinderModule"
    }
    
    override fun onStart() {
        Log.d(TAG, "Phone Finder module started")
    }
    
    override fun onMessage(packet: Packet, fromPlayer: Player) {
        if (packet.type == PacketProtocol.TYPE_PHONE_FIND) {
            val targetPlayerId = packet.payload[0]
            
            Log.d(TAG, "Ring request from ${fromPlayer.name} for player $targetPlayerId")
            
            // Send ring command to specific player
            server.sendToPlayer(targetPlayerId, packet.toBytes())
            
            onRingRequested?.invoke(targetPlayerId)
        }
    }
    
    override fun onStop() {
        Log.d(TAG, "Phone Finder module stopped")
    }
    
    override fun getName(): String = "Phone Finder"
    
    /**
     * Request a phone to ring
     */
    fun ringPhone(targetPlayerId: Byte) {
        val packet = PacketProtocol.serialize(
            PacketProtocol.TYPE_PHONE_FIND,
            0, // Server
            byteArrayOf(targetPlayerId)
        )
        server.sendToPlayer(targetPlayerId, packet)
    }
}
