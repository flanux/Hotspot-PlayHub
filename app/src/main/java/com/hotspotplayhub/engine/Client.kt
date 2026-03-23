package com.hotspotplayhub.engine

import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Client that connects to the host server
 * Runs on phones that join the hotspot
 */
class Client(
    private val hostIp: String = Server.HOST_IP,
    private val port: Int = 8888
) {
    
    private var socket: Socket? = null
    private var output: DataOutputStream? = null
    private var input: DataInputStream? = null
    
    private val executor = Executors.newCachedThreadPool()
    private val running = AtomicBoolean(false)
    
    var onPacketReceived: ((Packet) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    
    companion object {
        private const val TAG = "Client"
    }
    
    /**
     * Connect to the host server
     */
    fun connect(playerName: String, deviceName: String): Boolean {
        return try {
            socket = Socket(hostIp, port)
            output = DataOutputStream(socket!!.getOutputStream())
            input = DataInputStream(socket!!.getInputStream())
            
            running.set(true)
            
            // Send join packet
            val joinPacket = PacketProtocol.createTextPacket(
                PacketProtocol.TYPE_JOIN,
                0, // ID will be assigned by server
                "$playerName|$deviceName"
            )
            sendPacket(joinPacket)
            
            // Start listening
            executor.submit {
                receivePackets()
            }
            
            Log.d(TAG, "Connected to host at $hostIp:$port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to host", e)
            false
        }
    }
    
    /**
     * Receive packets from server
     */
    private fun receivePackets() {
        while (running.get()) {
            try {
                val size = input?.readInt() ?: break
                val buffer = ByteArray(size)
                input?.readFully(buffer)
                
                val packet = PacketProtocol.parse(buffer)
                onPacketReceived?.invoke(packet)
                
            } catch (e: Exception) {
                if (running.get()) {
                    Log.e(TAG, "Error receiving packet", e)
                }
                break
            }
        }
        
        disconnect()
        onDisconnected?.invoke()
    }
    
    /**
     * Send a packet to the server
     */
    fun sendPacket(packet: ByteArray) {
        try {
            synchronized(output!!) {
                output?.writeInt(packet.size)
                output?.write(packet)
                output?.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending packet", e)
        }
    }
    
    /**
     * Disconnect from the server
     */
    fun disconnect() {
        running.set(false)
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        executor.shutdown()
        Log.d(TAG, "Disconnected from host")
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return running.get() && socket?.isConnected == true
    }
}
