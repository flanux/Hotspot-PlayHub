package com.hotspotplayhub.engine

import android.util.Log
import java.net.*

/**
 * UDP Broadcast-based host discovery for instant connection
 * Like Mini Militia - broadcast and get instant response
 */
object HostDiscovery {
    
    private const val TAG = "HostDiscovery"
    private const val BROADCAST_PORT = 8889
    private const val SERVER_PORT = 8888
    private const val DISCOVERY_MESSAGE = "HOTSPOT_PLAYHUB_DISCOVER"
    private const val RESPONSE_MESSAGE = "HOTSPOT_PLAYHUB_HOST"
    private const val TIMEOUT_MS = 3000
    
    /**
     * Find host using UDP broadcast - INSTANT like Mini Militia
     */
    fun findHost(): String? {
        Log.d(TAG, "Starting UDP broadcast discovery...")
        
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = TIMEOUT_MS
            
            // Create discovery broadcast packet
            val discoverMsg = DISCOVERY_MESSAGE.toByteArray()
            val broadcastAddresses = getBroadcastAddresses()
            
            Log.d(TAG, "Broadcasting to ${broadcastAddresses.size} addresses")
            
            // Send broadcast to all network interfaces
            for (broadcastAddr in broadcastAddresses) {
                try {
                    val packet = DatagramPacket(
                        discoverMsg,
                        discoverMsg.size,
                        InetAddress.getByName(broadcastAddr),
                        BROADCAST_PORT
                    )
                    socket.send(packet)
                    Log.d(TAG, "Sent broadcast to $broadcastAddr")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to broadcast to $broadcastAddr: ${e.message}")
                }
            }
            
            // Listen for response
            val buffer = ByteArray(1024)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            
            try {
                socket.receive(receivePacket)
                val response = String(receivePacket.data, 0, receivePacket.length)
                
                if (response.startsWith(RESPONSE_MESSAGE)) {
                    val hostIp = receivePacket.address.hostAddress
                    Log.d(TAG, "Found host at $hostIp")
                    return hostIp
                }
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "No host responded within timeout")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Discovery error: ${e.message}")
        } finally {
            socket?.close()
        }
        
        return null
    }
    
    /**
     * Start listening for discovery broadcasts (HOST SIDE)
     */
    fun startDiscoveryListener(onDiscovered: () -> Unit): Thread {
        val thread = Thread {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(BROADCAST_PORT)
                socket.broadcast = true
                
                Log.d(TAG, "Discovery listener started on port $BROADCAST_PORT")
                
                val buffer = ByteArray(1024)
                
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        
                        val message = String(packet.data, 0, packet.length)
                        
                        if (message == DISCOVERY_MESSAGE) {
                            Log.d(TAG, "Discovery request from ${packet.address.hostAddress}")
                            
                            // Send response
                            val response = RESPONSE_MESSAGE.toByteArray()
                            val responsePacket = DatagramPacket(
                                response,
                                response.size,
                                packet.address,
                                packet.port
                            )
                            socket.send(responsePacket)
                            
                            Log.d(TAG, "Sent response to ${packet.address.hostAddress}")
                            onDiscovered()
                        }
                    } catch (e: Exception) {
                        if (!Thread.currentThread().isInterrupted) {
                            Log.e(TAG, "Listener error: ${e.message}")
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listener: ${e.message}")
            } finally {
                socket?.close()
                Log.d(TAG, "Discovery listener stopped")
            }
        }
        
        thread.start()
        return thread
    }
    
    /**
     * Get all broadcast addresses for current network interfaces
     */
    private fun getBroadcastAddresses(): List<String> {
        val broadcastList = mutableListOf<String>()
        
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                
                if (networkInterface.isLoopback || !networkInterface.isUp) {
                    continue
                }
                
                networkInterface.interfaceAddresses.forEach { interfaceAddress ->
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null) {
                        broadcastList.add(broadcast.hostAddress)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting broadcast addresses: ${e.message}")
        }
        
        // Fallback broadcast addresses
        if (broadcastList.isEmpty()) {
            broadcastList.add("255.255.255.255")
            broadcastList.add("192.168.43.255")
            broadcastList.add("192.168.49.255")
        }
        
        return broadcastList
    }
}
