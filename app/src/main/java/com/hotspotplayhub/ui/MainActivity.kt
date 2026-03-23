package com.hotspotplayhub.ui

import android.os.Bundle
import android.widget.Button
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
        
        btnHost.setOnClickListener {
            hostSession()
        }
        
        btnJoin.setOnClickListener {
            joinSession()
        }
    }
    
    private fun hostSession() {
        try {
            server = Server()
            server?.start()
            isHost = true
            
            Toast.makeText(this, "Hosting session on port 8888", Toast.LENGTH_SHORT).show()
            
            // Navigate to lobby
            // TODO: Implement lobby fragment
            
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start server: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun joinSession() {
        try {
            client = Client()
            val deviceName = android.os.Build.MODEL
            val connected = client?.connect("Player", deviceName)
            
            if (connected == true) {
                Toast.makeText(this, "Connected to host", Toast.LENGTH_SHORT).show()
                
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
