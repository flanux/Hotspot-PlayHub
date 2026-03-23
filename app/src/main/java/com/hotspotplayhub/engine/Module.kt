package com.hotspotplayhub.engine

/**
 * Base interface for all modules in Hotspot PlayHub
 * 
 * Modules are pluggable features that handle specific functionality:
 * - Chat
 * - Games (Chess, Ludo, etc.)
 * - Utilities (Clipboard, Phone Finder, etc.)
 */
interface Module {
    
    /**
     * Called when the module is started
     */
    fun onStart()
    
    /**
     * Handle incoming message for this module
     */
    fun onMessage(packet: Packet, fromPlayer: Player)
    
    /**
     * Called when the module is stopped
     */
    fun onStop()
    
    /**
     * Get the module name
     */
    fun getName(): String
}
