package com.hotspotplayhub.modules.scribble

import android.util.Log
import com.hotspotplayhub.engine.Module
import com.hotspotplayhub.engine.Packet
import com.hotspotplayhub.engine.PacketProtocol
import com.hotspotplayhub.engine.Player
import com.hotspotplayhub.engine.Server
import com.hotspotplayhub.engine.toBytes
import java.nio.ByteBuffer
import kotlin.random.Random

/**
 * Scribble module - multiplayer drawing guessing game
 * One player draws, others guess the word
 */
class ScribbleModule(private val server: Server) : Module {
    
    private val players = mutableListOf<ScribblePlayer>()
    private var currentDrawerIndex = 0
    private var currentWord: String = ""
    private var roundNumber = 0
    private val maxRounds = 3
    private val roundTimeSeconds = 60
    
    var onGameStateChanged: ((GameState) -> Unit)? = null
    var onDrawReceived: ((DrawAction) -> Unit)? = null
    var onGuessReceived: ((GuessAction) -> Unit)? = null
    var onClearReceived: (() -> Unit)? = null
    var onRoundStart: ((RoundInfo) -> Unit)? = null
    var onRoundEnd: ((RoundResult) -> Unit)? = null
    
    private val wordList = listOf(
        "cat", "dog", "house", "tree", "car", "phone", "computer", "sun", "moon", "star",
        "book", "pen", "cup", "chair", "table", "door", "window", "flower", "bird", "fish",
        "pizza", "apple", "banana", "cake", "ice cream", "mountain", "beach", "rain", "snow",
        "guitar", "piano", "ball", "bicycle", "airplane", "boat", "train", "rocket", "robot"
    )
    
    companion object {
        private const val TAG = "ScribbleModule"
        const val ACTION_DRAW: Byte = 0x01
        const val ACTION_CLEAR: Byte = 0x02
        const val ACTION_GUESS: Byte = 0x03
        const val ACTION_START_ROUND: Byte = 0x04
        const val ACTION_END_ROUND: Byte = 0x05
        const val ACTION_GAME_STATE: Byte = 0x06
    }
    
    override fun onStart() {
        Log.d(TAG, "Scribble module started")
        initializePlayers()
        startNewRound()
    }
    
    override fun onMessage(packet: Packet, fromPlayer: Player) {
        if (packet.type == PacketProtocol.TYPE_SCRIBBLE) {
            val buffer = ByteBuffer.wrap(packet.payload)
            val action = buffer.get()
            
            when (action) {
                ACTION_DRAW -> handleDrawAction(buffer, fromPlayer)
                ACTION_CLEAR -> handleClearAction(fromPlayer)
                ACTION_GUESS -> handleGuessAction(buffer, fromPlayer)
            }
        }
    }
    
    override fun onStop() {
        Log.d(TAG, "Scribble module stopped")
    }
    
    override fun getName(): String = "Scribble"
    
    private fun initializePlayers() {
        players.clear()
        server.getPlayers().forEach { player ->
            players.add(ScribblePlayer(player.id, player.name, 0))
        }
    }
    
    private fun startNewRound() {
        roundNumber++
        
        if (roundNumber > maxRounds) {
            endGame()
            return
        }
        
        // Pick random word
        currentWord = wordList[Random.nextInt(wordList.size)]
        
        // Get current drawer
        val drawer = players[currentDrawerIndex]
        
        val roundInfo = RoundInfo(
            roundNumber = roundNumber,
            maxRounds = maxRounds,
            drawerId = drawer.playerId,
            drawerName = drawer.playerName,
            word = currentWord,
            wordLength = currentWord.length,
            timeSeconds = roundTimeSeconds
        )
        
        onRoundStart?.invoke(roundInfo)
        
        // Broadcast round start to all players
        broadcastRoundStart(roundInfo)
        
        Log.d(TAG, "Round $roundNumber started. Drawer: ${drawer.playerName}, Word: $currentWord")
    }
    
    private fun handleDrawAction(buffer: ByteBuffer, fromPlayer: Player) {
        // Only drawer can draw
        val currentDrawer = players[currentDrawerIndex]
        if (fromPlayer.id != currentDrawer.playerId) {
            return
        }
        
        val x = buffer.float
        val y = buffer.float
        val color = buffer.int
        val strokeWidth = buffer.float
        
        val drawAction = DrawAction(
            playerId = fromPlayer.id,
            x = x,
            y = y,
            color = color,
            strokeWidth = strokeWidth
        )
        
        // Broadcast to all clients except drawer
        val packet = createDrawPacket(fromPlayer.id, x, y, color, strokeWidth)
        server.broadcast(packet, excludePlayer = fromPlayer.id)
        
        onDrawReceived?.invoke(drawAction)
    }
    
    private fun handleClearAction(fromPlayer: Player) {
        // Only drawer can clear
        val currentDrawer = players[currentDrawerIndex]
        if (fromPlayer.id != currentDrawer.playerId) {
            return
        }
        
        val packet = createClearPacket(fromPlayer.id)
        server.broadcast(packet, excludePlayer = fromPlayer.id)
        
        onClearReceived?.invoke()
    }
    
    private fun handleGuessAction(buffer: ByteBuffer, fromPlayer: Player) {
        // Drawer cannot guess
        val currentDrawer = players[currentDrawerIndex]
        if (fromPlayer.id == currentDrawer.playerId) {
            return
        }
        
        val guessLength = buffer.int
        val guessBytes = ByteArray(guessLength)
        buffer.get(guessBytes)
        val guess = String(guessBytes, Charsets.UTF_8)
        
        val isCorrect = guess.equals(currentWord, ignoreCase = true)
        
        val guessAction = GuessAction(
            playerId = fromPlayer.id,
            playerName = players.find { it.playerId == fromPlayer.id }?.playerName ?: "Unknown",
            guess = guess,
            isCorrect = isCorrect
        )
        
        onGuessReceived?.invoke(guessAction)
        
        // Broadcast guess to all players
        broadcastGuess(guessAction)
        
        if (isCorrect) {
            // Award points
            val player = players.find { it.playerId == fromPlayer.id }
            player?.score = (player?.score ?: 0) + 100
            
            Log.d(TAG, "${fromPlayer.name} guessed correctly!")
            
            // Check if all players guessed
            // For simplicity, end round after first correct guess
            endRound()
        }
    }
    
    private fun endRound() {
        val result = RoundResult(
            word = currentWord,
            scores = players.map { PlayerScore(it.playerId, it.playerName, it.score) }
        )
        
        onRoundEnd?.invoke(result)
        broadcastRoundEnd(result)
        
        // Move to next drawer
        currentDrawerIndex = (currentDrawerIndex + 1) % players.size
        
        // Start new round after delay (handled by activity)
    }
    
    private fun endGame() {
        val winner = players.maxByOrNull { it.score }
        Log.d(TAG, "Game ended! Winner: ${winner?.playerName} with ${winner?.score} points")
        
        // Broadcast game end
    }
    
    private fun broadcastRoundStart(roundInfo: RoundInfo) {
        // Send word to drawer
        val drawerPacket = createRoundStartPacket(roundInfo, showWord = true)
        server.sendToPlayer(roundInfo.drawerId, drawerPacket)
        
        // Send word hint to others (just length)
        val othersPacket = createRoundStartPacket(roundInfo, showWord = false)
        server.broadcast(othersPacket, excludePlayer = roundInfo.drawerId)
    }
    
    private fun broadcastGuess(guessAction: GuessAction) {
        val packet = createGuessPacket(guessAction)
        server.broadcast(packet)
    }
    
    private fun broadcastRoundEnd(result: RoundResult) {
        val packet = createRoundEndPacket(result)
        server.broadcast(packet)
    }
    
    // Packet creation methods
    fun createDrawPacket(playerId: Byte, x: Float, y: Float, color: Int, strokeWidth: Float): ByteArray {
        val buffer = ByteBuffer.allocate(18)
        buffer.put(ACTION_DRAW)
        buffer.putFloat(x)
        buffer.putFloat(y)
        buffer.putInt(color)
        buffer.putFloat(strokeWidth)
        
        return PacketProtocol.serialize(PacketProtocol.TYPE_SCRIBBLE, playerId, buffer.array())
    }
    
    fun createClearPacket(playerId: Byte): ByteArray {
        val buffer = ByteBuffer.allocate(1)
        buffer.put(ACTION_CLEAR)
        return PacketProtocol.serialize(PacketProtocol.TYPE_SCRIBBLE, playerId, buffer.array())
    }
    
    fun createGuessPacket(playerId: Byte, guess: String): ByteArray {
        val guessBytes = guess.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(1 + 4 + guessBytes.size)
        buffer.put(ACTION_GUESS)
        buffer.putInt(guessBytes.size)
        buffer.put(guessBytes)
        return PacketProtocol.serialize(PacketProtocol.TYPE_SCRIBBLE, playerId, buffer.array())
    }
    
    private fun createGuessPacket(guessAction: GuessAction): ByteArray {
        val text = "${guessAction.playerName}: ${guessAction.guess}${if (guessAction.isCorrect) " ✓" else ""}"
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(1 + 4 + textBytes.size + 1)
        buffer.put(ACTION_GUESS)
        buffer.putInt(textBytes.size)
        buffer.put(textBytes)
        buffer.put(if (guessAction.isCorrect) 1.toByte() else 0.toByte())
        return PacketProtocol.serialize(PacketProtocol.TYPE_SCRIBBLE, 0, buffer.array())
    }
    
    private fun createRoundStartPacket(roundInfo: RoundInfo, showWord: Boolean): ByteArray {
        val data = if (showWord) {
            "DRAW:${roundInfo.word}"
        } else {
            "GUESS:${"_".repeat(roundInfo.wordLength)}"
        }
        val dataBytes = data.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(1 + 4 + dataBytes.size)
        buffer.put(ACTION_START_ROUND)
        buffer.putInt(dataBytes.size)
        buffer.put(dataBytes)
        return PacketProtocol.serialize(PacketProtocol.TYPE_SCRIBBLE, 0, buffer.array())
    }
    
    private fun createRoundEndPacket(result: RoundResult): ByteArray {
        val data = "WORD:${result.word}|SCORES:${result.scores.joinToString(",") { "${it.name}:${it.score}" }}"
        val dataBytes = data.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(1 + 4 + dataBytes.size)
        buffer.put(ACTION_END_ROUND)
        buffer.putInt(dataBytes.size)
        buffer.put(dataBytes)
        return PacketProtocol.serialize(PacketProtocol.TYPE_SCRIBBLE, 0, buffer.array())
    }
    
    fun triggerNextRound() {
        startNewRound()
    }
}

// Data classes
data class ScribblePlayer(
    val playerId: Byte,
    val playerName: String,
    var score: Int
)

data class DrawAction(
    val playerId: Byte,
    val x: Float,
    val y: Float,
    val color: Int,
    val strokeWidth: Float
)

data class GuessAction(
    val playerId: Byte,
    val playerName: String,
    val guess: String,
    val isCorrect: Boolean
)

data class RoundInfo(
    val roundNumber: Int,
    val maxRounds: Int,
    val drawerId: Byte,
    val drawerName: String,
    val word: String,
    val wordLength: Int,
    val timeSeconds: Int
)

data class RoundResult(
    val word: String,
    val scores: List<PlayerScore>
)

data class PlayerScore(
    val playerId: Byte,
    val name: String,
    val score: Int
)

data class GameState(
    val players: List<ScribblePlayer>,
    val currentDrawerIndex: Int,
    val roundNumber: Int
)
