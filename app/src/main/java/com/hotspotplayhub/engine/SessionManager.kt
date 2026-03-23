package com.hotspotplayhub.engine

import android.util.Log

/**
 * Manages the session state (lobby, players, module selection)
 */
class SessionManager(private val server: Server) {
    
    private val players = mutableListOf<Player>()
    
    var onPlayerJoinedCallback: ((Player) -> Unit)? = null
    var onPlayerLeftCallback: ((Player) -> Unit)? = null
    var onModuleStartedCallback: ((String) -> Unit)? = null
    
    companion object {
        private const val TAG = "SessionManager"
    }
    
    /**
     * Handle player join
     */
    fun onPlayerJoin(player: Player) {
        players.add(player)
        Log.d(TAG, "Player joined: ${player.name}")
        
        // Broadcast lobby update to all clients
        broadcastLobbyUpdate()
        
        onPlayerJoinedCallback?.invoke(player)
    }
    
    /**
     * Handle player leave
     */
    fun onPlayerLeave(player: Player) {
        players.remove(player)
        Log.d(TAG, "Player left: ${player.name}")
        
        // Broadcast lobby update to all clients
        broadcastLobbyUpdate()
        
        onPlayerLeftCallback?.invoke(player)
    }
    
    /**
     * Broadcast current lobby state to all clients
     */
    private fun broadcastLobbyUpdate() {
        val playerList = players.joinToString("|") { "${it.id}:${it.name}:${it.deviceName}" }
        val packet = PacketProtocol.createTextPacket(
            PacketProtocol.TYPE_LOBBY_UPDATE,
            0, // Server ID
            playerList
        )
        server.broadcast(packet)
    }
    
    /**
     * Start a module
     */
    fun startModule(moduleName: String) {
        Log.d(TAG, "Starting module: $moduleName")
        
        // Notify all clients to switch to this module
        val packet = PacketProtocol.createTextPacket(
            PacketProtocol.TYPE_MODULE_SWITCH,
            0,
            moduleName
        )
        server.broadcast(packet)
        
        onModuleStartedCallback?.invoke(moduleName)
    }
    
    /**
     * Get all players in session
     */
    fun getPlayers(): List<Player> = players.toList()
    
    /**
     * Get player by ID
     */
    fun getPlayer(id: Byte): Player? = players.find { it.id == id }
}
