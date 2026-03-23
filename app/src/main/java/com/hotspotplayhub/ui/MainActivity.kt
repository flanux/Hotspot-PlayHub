package com.hotspotplayhub.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hotspotplayhub.R
import com.hotspotplayhub.engine.Client
import com.hotspotplayhub.engine.Server
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    
    private var server: Server? = null
    private var client: Client? = null
    private var isHosting = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val btnHost = findViewById<Button>(R.id.btnHost)
        val btnJoin = findViewById<Button>(R.id.btnJoin)
        val tvInfo = findViewById<TextView>(R.id.tvInfo)
        
        btnHost.setOnClickListener {
            if (isHosting) {
                stopHosting(btnHost, btnJoin, tvInfo)
            } else {
                hostSession(btnHost, btnJoin, tvInfo)
            }
        }
        
        btnJoin.setOnClickListener {
            joinSession(tvInfo)
        }
    }
    
    private fun hostSession(btnHost: Button, btnJoin: Button, tvInfo: TextView) {
        try {
            server = Server()
            server?.start()
            isHosting = true
            
            val hostIp = Server.getLocalIpAddress() ?: "unknown"
            
            // Update UI
            btnHost.text = "Stop Hosting"
            btnHost.setBackgroundColor(getColor(android.R.color.holo_red_dark))
            btnJoin.isEnabled = false
            btnJoin.alpha = 0.5f
            
            tvInfo.text = "Hosting on: $hostIp:8888\n\nOthers can now join your hotspot and tap 'Join Session'"
            
            Toast.makeText(this, "Hosting session on $hostIp", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start server: ${e.message}", Toast.LENGTH_SHORT).show()
            isHosting = false
        }
    }
    
    private fun stopHosting(btnHost: Button, btnJoin: Button, tvInfo: TextView) {
        try {
            server?.stop()
            server = null
            isHosting = false
            
            // Reset UI
            btnHost.text = "Host Session"
            btnHost.setBackgroundColor(getColor(com.google.android.material.R.color.design_default_color_primary))
            btnJoin.isEnabled = true
            btnJoin.alpha = 1.0f
            
            tvInfo.text = "Host creates hotspot\nOthers join WiFi and tap Join"
            
            Toast.makeText(this, "Stopped hosting", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error stopping server: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun joinSession(tvInfo: TextView) {
        tvInfo.text = "Searching for host..."
        Toast.makeText(this, "Searching for host...", Toast.LENGTH_SHORT).show()
        
        scope.launch {
            try {
                // Try to find the host
                val hostIp = findHost()
                
                if (hostIp != null) {
                    // Found host, try to connect
                    withContext(Dispatchers.IO) {
                        client = Client(hostIp)
                        val deviceName = android.os.Build.MODEL
                        val connected = client?.connect("Player", deviceName)
                        
                        withContext(Dispatchers.Main) {
                            if (connected == true) {
                                tvInfo.text = "Connected to host at $hostIp"
                                Toast.makeText(this@MainActivity, "Connected to host!", Toast.LENGTH_SHORT).show()
                                
                                // Navigate to lobby
                                // TODO: Implement lobby fragment
                            } else {
                                tvInfo.text = "Failed to connect to host"
                                Toast.makeText(this@MainActivity, "Failed to connect to host", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    tvInfo.text = "No host found. Make sure you're connected to the host's hotspot."
                    Toast.makeText(this@MainActivity, "No host found on network", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                tvInfo.text = "Error joining: ${e.message}"
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Automatically find the host on the network
     * Tries common hotspot gateway IPs and scans the local subnet
     */
    private suspend fun findHost(): String? = withContext(Dispatchers.IO) {
        val port = 8888
        
        // Common Android hotspot gateway IPs to try first
        val commonIps = listOf(
            "192.168.43.1",  // Most common Android hotspot
            "192.168.49.1",  // Some Samsung devices
            "192.168.45.1",  // Some other devices
        )
        
        // Try common IPs first (faster)
        for (ip in commonIps) {
            if (isHostReachable(ip, port)) {
                return@withContext ip
            }
        }
        
        // If not found, scan local subnet
        val localIp = Server.getLocalIpAddress()
        if (localIp != null) {
            val subnet = localIp.substringBeforeLast(".")
            
            // Scan subnet (but skip our own IP and .0/.255)
            for (i in 1..254) {
                val testIp = "$subnet.$i"
                if (testIp != localIp && isHostReachable(testIp, port)) {
                    return@withContext testIp
                }
            }
        }
        
        return@withContext null
    }
    
    /**
     * Check if a host is reachable at the given IP and port
     */
    private fun isHostReachable(ip: String, port: Int): Boolean {
        return try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(ip, port), 1000) // 1 second timeout
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        server?.stop()
        client?.disconnect()
    }
}
