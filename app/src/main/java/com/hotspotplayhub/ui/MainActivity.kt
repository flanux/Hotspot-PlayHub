package com.hotspotplayhub.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hotspotplayhub.R
import com.hotspotplayhub.engine.Client
import com.hotspotplayhub.engine.HostDiscovery
import com.hotspotplayhub.engine.PacketProtocol
import com.hotspotplayhub.engine.Server
import com.hotspotplayhub.modules.scribble.ScribbleModule
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    
    private var server: Server? = null
    private var client: Client? = null
    private var isHosting = false
    private var scribbleModule: ScribbleModule? = null
    private var discoveryThread: Thread? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val btnHost = findViewById<Button>(R.id.btnHost)
        val btnJoin = findViewById<Button>(R.id.btnJoin)
        val btnScribble = findViewById<Button>(R.id.btnScribble)
        val tvInfo = findViewById<TextView>(R.id.tvInfo)
        
        btnHost.setOnClickListener {
            if (isHosting) {
                stopHosting(btnHost, btnJoin, btnScribble, tvInfo)
            } else {
                hostSession(btnHost, btnJoin, btnScribble, tvInfo)
            }
        }
        
        btnJoin.setOnClickListener {
            joinSession(tvInfo, btnScribble)
        }
        
        btnScribble.setOnClickListener {
            launchScribble()
        }
    }
    
    private fun hostSession(btnHost: Button, btnJoin: Button, btnScribble: Button, tvInfo: TextView) {
        try {
            server = Server()
            server?.start()
            isHosting = true
            
            // Initialize scribble module
            scribbleModule = ScribbleModule(server!!)
            server?.router?.registerModule(PacketProtocol.TYPE_SCRIBBLE, scribbleModule!!)
            
            // Start UDP discovery listener
            discoveryThread = HostDiscovery.startDiscoveryListener {
                runOnUiThread {
                    Toast.makeText(this, "Client discovered!", Toast.LENGTH_SHORT).show()
                }
            }
            
            val hostIp = Server.getLocalIpAddress() ?: "unknown"
            
            // Update UI
            btnHost.text = "Stop Hosting"
            btnHost.setBackgroundColor(getColor(android.R.color.holo_red_dark))
            btnJoin.isEnabled = false
            btnJoin.alpha = 0.5f
            btnScribble.isEnabled = true
            btnScribble.alpha = 1.0f
            
            tvInfo.text = "Hosting on: $hostIp:8888\n\nOthers can now join your hotspot and tap 'Join Session'"
            
            Toast.makeText(this, "Hosting session on $hostIp", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start server: ${e.message}", Toast.LENGTH_SHORT).show()
            isHosting = false
        }
    }
    
    private fun stopHosting(btnHost: Button, btnJoin: Button, btnScribble: Button, tvInfo: TextView) {
        try {
            discoveryThread?.interrupt()
            discoveryThread = null
            
            server?.stop()
            server = null
            scribbleModule = null
            isHosting = false
            
            // Reset UI
            btnHost.text = "Host Session"
            btnHost.setBackgroundColor(getColor(com.google.android.material.R.color.design_default_color_primary))
            btnJoin.isEnabled = true
            btnJoin.alpha = 1.0f
            btnScribble.isEnabled = false
            btnScribble.alpha = 0.5f
            
            tvInfo.text = "Host creates hotspot\nOthers join WiFi and tap Join"
            
            Toast.makeText(this, "Stopped hosting", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error stopping server: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun joinSession(tvInfo: TextView, btnScribble: Button) {
        tvInfo.text = "Broadcasting discovery..."
        Toast.makeText(this, "Looking for host...", Toast.LENGTH_SHORT).show()
        
        scope.launch {
            try {
                // Use UDP broadcast discovery - INSTANT!
                val hostIp = withContext(Dispatchers.IO) {
                    HostDiscovery.findHost()
                }
                
                if (hostIp != null) {
                    // Found host, try to connect
                    withContext(Dispatchers.IO) {
                        client = Client(hostIp)
                        val deviceName = android.os.Build.MODEL
                        val connected = client?.connect("Player", deviceName)
                        
                        withContext(Dispatchers.Main) {
                            if (connected == true) {
                                tvInfo.text = "Connected to host at $hostIp"
                                btnScribble.isEnabled = true
                                btnScribble.alpha = 1.0f
                                Toast.makeText(this@MainActivity, "Connected!", Toast.LENGTH_SHORT).show()
                            } else {
                                tvInfo.text = "Failed to connect to host"
                                Toast.makeText(this@MainActivity, "Connection failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    tvInfo.text = "No host found. Make sure you're connected to the host's hotspot."
                    Toast.makeText(this@MainActivity, "No host found", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                tvInfo.text = "Error joining: ${e.message}"
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun launchScribble() {
        val intent = Intent(this, ScribbleActivity::class.java)
        intent.putExtra("isHost", isHosting)
        intent.putExtra("playerId", if (isHosting) 0.toByte() else 1.toByte())
        startActivity(intent)
        
        // Pass module to activity via static
        if (scribbleModule != null) {
            ScribbleActivity.staticModule = scribbleModule
            ScribbleActivity.staticServer = server
            ScribbleActivity.staticClient = client
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        discoveryThread?.interrupt()
        server?.stop()
        client?.disconnect()
    }
}
