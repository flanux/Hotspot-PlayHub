package com.hotspotplayhub.engine

import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tick loop for game state updates
 * Runs at a fixed rate (e.g., 20 ticks per second)
 */
class TickLoop(
    private val tickRate: Int = 20 // 20 ticks per second = 50ms per tick
) {
    
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val running = AtomicBoolean(false)
    
    private var tickCount = 0L
    private var onTickCallback: ((Long) -> Unit)? = null
    
    companion object {
        private const val TAG = "TickLoop"
    }
    
    /**
     * Start the tick loop
     */
    fun start(onTick: (Long) -> Unit) {
        if (running.get()) {
            Log.w(TAG, "Tick loop already running")
            return
        }
        
        onTickCallback = onTick
        running.set(true)
        tickCount = 0
        
        val periodMs = 1000L / tickRate
        
        executor.scheduleAtFixedRate({
            try {
                if (running.get()) {
                    tickCount++
                    onTickCallback?.invoke(tickCount)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in tick loop", e)
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS)
        
        Log.d(TAG, "Tick loop started at $tickRate ticks/sec")
    }
    
    /**
     * Stop the tick loop
     */
    fun stop() {
        running.set(false)
        executor.shutdown()
        Log.d(TAG, "Tick loop stopped at tick $tickCount")
    }
    
    /**
     * Get current tick count
     */
    fun getCurrentTick(): Long = tickCount
}
