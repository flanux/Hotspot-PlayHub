package com.hotspotplayhub.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hotspotplayhub.R
import com.hotspotplayhub.engine.Client
import com.hotspotplayhub.engine.Server

class MainActivity : AppCompatActivity() {
    
    private var server: Server? = null
    private var client: Client? = null
    private var isHost = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val btnHost = findViewById<Button>(R.id.btnHost)
        val btnJoin = findViewById<Button>(R.id.btnJoin)
        val tvInfo = findViewById<TextView>(R.id.tvInfo)
        
        btnHost.setOnClickListener {
            hostSession(tvInfo)
        }
        
        btnJoin.setOnClickListener {
            joinSession()
        }
    }
    
    private fun hostSession(tvInfo: TextView) {
        try {
            server = Server()
            server?.start()
            isHost = true
            
            val hostIp = Server.getLocalIpAddress() ?: "unknown"
            
            tvInfo.text = "Hosting on: $hostIp:8888\n\nOthers connect to this IP"
            
            Toast.makeText(this, "Hosting session on $hostIp", Toast.LENGTH_LONG).show()
            
            // Navigate to lobby
            // TODO: Implement lobby fragment
            
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start server: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun joinSession() {
        try {
            // Get host IP from user input
            // TODO: Add EditText for host IP
            val hostIp = "192.168.43.1" // Placeholder - need to get from user
            
            client = Client(hostIp)
            val deviceName = android.os.Build.MODEL
            val connected = client?.connect("Player", deviceName)
            
            if (connected == true) {
                Toast.makeText(this, "Connected to host at $hostIp", Toast.LENGTH_SHORT).show()
                
                // Navigate to lobby
                // TODO: Implement lobby fragment
            } else {
                Toast.makeText(this, "Failed to connect to host", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error joining: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        client?.disconnect()
    }
}
