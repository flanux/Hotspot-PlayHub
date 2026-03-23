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
    private var isHosting = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val btnHost = findViewById<Button>(R.id.btnHost)
        val btnJoin = findViewById<Button>(R.id.btnJoin)
        val tvInfo = findViewById<TextView>(R.id.tvInfo)
        
        btnHost.setOnClickListener {
            if (isHosting) {
                stopHosting(btnHost, tvInfo)
            } else {
                startHosting(btnHost, tvInfo)
            }
        }
        
        btnJoin.setOnClickListener {
            joinSession()
        }
    }
    
    private fun startHosting(btnHost: Button, tvInfo: TextView) {
        try {
            server = Server()
            server?.start()
            isHosting = true
            
            val hostIp = Server.getLocalIpAddress() ?: "unknown"
            
            tvInfo.text = "Hosting on: $hostIp:8888\n\nOthers connect to this IP"
            btnHost.text = "Stop Hosting"
            
            Toast.makeText(this, "Hosting on $hostIp", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopHosting(btnHost: Button, tvInfo: TextView) {
        server?.stop()
        server = null
        isHosting = false
        
        btnHost.text = "Host Session"
        tvInfo.text = "Host creates hotspot\nOthers join WiFi and tap Join"
        
        Toast.makeText(this, "Stopped hosting", Toast.LENGTH_SHORT).show()
    }
    
    private fun joinSession() {
        try {
            client = Client()
            val deviceName = android.os.Build.MODEL
            val connected = client?.connect("Player", deviceName)
            
            if (connected == true) {
                Toast.makeText(this, "Connected to host", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        client?.disconnect()
    }
}
