package com.hotspotplayhub.engine

import android.util.Log
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Host server that manages all client connections
 * Runs on the phone that creates the hotspot
 */
class Server(private val port: Int = 8888) {
    
    private val serverSocket = ServerSocket(port)
    private val clients = ConcurrentHashMap<Byte, ClientConnection>()
    private val executor = Executors.newCachedThreadPool()
    private val running = AtomicBoolean(false)
    
    private var nextPlayerId: Byte = 1
    
    lateinit var router: MessageRouter
    lateinit var sessionManager: SessionManager
    
    companion object {
        private const val TAG = "Server"
        const val HOST_IP = "192.168.43.1" // Standard hotspot IP
    }
    
    /**
     * Start the server and accept connections
     */
    fun start() {
        running.set(true)
        router = MessageRouter(this)
        sessionManager = SessionManager(this)
        
        Log.d(TAG, "Server starting on port $port")
        
        executor.submit {
            acceptConnections()
        }
        
        Log.d(TAG, "Server started")
    }
    
    /**
     * Accept incoming client connections
     */
    private fun acceptConnections() {
        while (running.get()) {
            try {
                val socket = serverSocket.accept()
                Log.d(TAG, "Client connected: ${socket.inetAddress.hostAddress}")
                
                // Assign player ID
                val playerId = nextPlayerId++
                val player = Player(
                    id = playerId,
                    name = "Player $playerId",
                    deviceName = socket.inetAddress.hostName,
                    socket = socket
                )
                
                val connection = ClientConnection(socket, player)
                clients[playerId] = connection
                
                // Notify session manager
                sessionManager.onPlayerJoin(player)
                
                // Start listening to this client
                executor.submit {
                    handleClient(connection)
                }
                
                // Start sending queued packets
                executor.submit {
                    sendQueuedPackets(connection)
                }
                
            } catch (e: Exception) {
                if (running.get()) {
                    Log.e(TAG, "Error accepting connection", e)
                }
            }
        }
    }
    
    /**
     * Handle incoming packets from a client
     */
    private fun handleClient(connection: ClientConnection) {
        while (connection.isConnected && running.get()) {
            try {
                val packet = connection.readPacket()
                if (packet != null) {
                    router.route(packet, connection.player)
                } else {
                    break
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading from client ${connection.player.id}", e)
                break
            }
        }
        
        // Client disconnected
        handleDisconnect(connection)
    }
    
    /**
     * Send queued packets to a client
     */
    private fun sendQueuedPackets(connection: ClientConnection) {
        while (connection.isConnected && running.get()) {
            try {
                val packet = connection.sendQueue.take()
                connection.sendPacket(packet)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to client ${connection.player.id}", e)
                break
            }
        }
    }
    
    /**
     * Handle client disconnect
     */
    private fun handleDisconnect(connection: ClientConnection) {
        connection.close()
        clients.remove(connection.player.id)
        sessionManager.onPlayerLeave(connection.player)
        Log.d(TAG, "Client disconnected: ${connection.player.name}")
    }
    
    /**
     * Broadcast a packet to all connected clients
     */
    fun broadcast(packet: ByteArray, excludePlayer: Byte? = null) {
        clients.values.forEach { connection ->
            if (excludePlayer == null || connection.player.id != excludePlayer) {
                connection.sendQueue.offer(packet)
            }
        }
    }
    
    /**
     * Send packet to a specific player
     */
    fun sendToPlayer(playerId: Byte, packet: ByteArray) {
        clients[playerId]?.sendQueue?.offer(packet)
    }
    
    /**
     * Get all connected players
     */
    fun getPlayers(): List<Player> {
        return clients.values.map { it.player }
    }
    
    /**
     * Stop the server
     */
    fun stop() {
        running.set(false)
        clients.values.forEach { it.close() }
        clients.clear()
        serverSocket.close()
        executor.shutdown()
        Log.d(TAG, "Server stopped")
    }
}
