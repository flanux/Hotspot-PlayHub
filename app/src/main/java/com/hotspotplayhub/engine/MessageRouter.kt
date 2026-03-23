package com.hotspotplayhub.engine

import android.util.Log

/**
 * Routes incoming packets to the appropriate module
 */
class MessageRouter(private val server: Server) {
    
    private val modules = mutableMapOf<Byte, Module>()
    private var currentModule: Module? = null
    
    companion object {
        private const val TAG = "MessageRouter"
    }
    
    /**
     * Register a module to handle specific packet types
     */
    fun registerModule(type: Byte, module: Module) {
        modules[type] = module
        Log.d(TAG, "Registered module: ${module.getName()} for type $type")
    }
    
    /**
     * Set the currently active module
     */
    fun setActiveModule(module: Module) {
        currentModule?.onStop()
        currentModule = module
        currentModule?.onStart()
        Log.d(TAG, "Active module: ${module.getName()}")
    }
    
    /**
     * Route a packet to the appropriate module
     */
    fun route(packet: Packet, fromPlayer: Player) {
        val module = modules[packet.type]
        
        if (module != null) {
            module.onMessage(packet, fromPlayer)
        } else {
            // Default routing for common types
            when (packet.type) {
                PacketProtocol.TYPE_CHAT,
                PacketProtocol.TYPE_GAME_INPUT,
                PacketProtocol.TYPE_GAME_STATE,
                PacketProtocol.TYPE_CLIPBOARD,
                PacketProtocol.TYPE_PHONE_FIND -> {
                    currentModule?.onMessage(packet, fromPlayer)
                }
                PacketProtocol.TYPE_HEARTBEAT -> {
                    // Handled by connection manager
                }
                else -> {
                    Log.w(TAG, "No module registered for packet type: ${packet.type}")
                }
            }
        }
    }
    
    /**
     * Get current active module
     */
    fun getCurrentModule(): Module? = currentModule
}
