import com.hotspotplayhub.engine.toBytes
package com.hotspotplayhub.modules.chess

import android.util.Log
import com.hotspotplayhub.engine.Module
import com.hotspotplayhub.engine.Packet
import com.hotspotplayhub.engine.PacketProtocol
import com.hotspotplayhub.engine.Player
import com.hotspotplayhub.engine.Server

/**
 * Chess module - turn-based game for 2 players
 */
class ChessModule(private val server: Server) : Module {
    
    private val board = Array(8) { Array(8) { Piece.EMPTY } }
    private var currentTurn: Byte = 1 // Player 1 starts (White)
    private var whitePlayer: Byte = 1
    private var blackPlayer: Byte = 2
    
    var onMoveReceived: ((ChessMove) -> Unit)? = null
    var onGameStateChanged: ((GameState) -> Unit)? = null
    
    companion object {
        private const val TAG = "ChessModule"
    }
    
    override fun onStart() {
        Log.d(TAG, "Chess module started")
        initializeBoard()
        currentTurn = whitePlayer
        broadcastGameState()
    }
    
    override fun onMessage(packet: Packet, fromPlayer: Player) {
        if (packet.type == PacketProtocol.TYPE_GAME_INPUT) {
            val moveData = PacketProtocol.extractText(packet.payload)
            val parts = moveData.split("|")
            
            if (parts.size == 4) {
                val fromRow = parts[0].toInt()
                val fromCol = parts[1].toInt()
                val toRow = parts[2].toInt()
                val toCol = parts[3].toInt()
                
                processMove(fromPlayer, fromRow, fromCol, toRow, toCol)
            }
        }
    }
    
    override fun onStop() {
        Log.d(TAG, "Chess module stopped")
    }
    
    override fun getName(): String = "Chess"
    
    /**
     * Initialize chess board with standard setup
     */
    private fun initializeBoard() {
        // Empty board first
        for (i in 0..7) {
            for (j in 0..7) {
                board[i][j] = Piece.EMPTY
            }
        }
        
        // Black pieces (top)
        board[0][0] = Piece.BLACK_ROOK
        board[0][1] = Piece.BLACK_KNIGHT
        board[0][2] = Piece.BLACK_BISHOP
        board[0][3] = Piece.BLACK_QUEEN
        board[0][4] = Piece.BLACK_KING
        board[0][5] = Piece.BLACK_BISHOP
        board[0][6] = Piece.BLACK_KNIGHT
        board[0][7] = Piece.BLACK_ROOK
        for (i in 0..7) {
            board[1][i] = Piece.BLACK_PAWN
        }
        
        // White pieces (bottom)
        for (i in 0..7) {
            board[6][i] = Piece.WHITE_PAWN
        }
        board[7][0] = Piece.WHITE_ROOK
        board[7][1] = Piece.WHITE_KNIGHT
        board[7][2] = Piece.WHITE_BISHOP
        board[7][3] = Piece.WHITE_QUEEN
        board[7][4] = Piece.WHITE_KING
        board[7][5] = Piece.WHITE_BISHOP
        board[7][6] = Piece.WHITE_KNIGHT
        board[7][7] = Piece.WHITE_ROOK
    }
    
    /**
     * Process a chess move
     */
    private fun processMove(player: Player, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        // Check if it's this player's turn
        if (player.id != currentTurn) {
            Log.w(TAG, "Not ${player.name}'s turn")
            return
        }
        
        // Validate move (simplified - no chess rules validation for V1)
        if (fromRow !in 0..7 || fromCol !in 0..7 || toRow !in 0..7 || toCol !in 0..7) {
            return
        }
        
        // Make the move
        val piece = board[fromRow][fromCol]
        board[toRow][toCol] = piece
        board[fromRow][fromCol] = Piece.EMPTY
        
        val move = ChessMove(
            playerId = player.id,
            fromRow = fromRow,
            fromCol = fromCol,
            toRow = toRow,
            toCol = toCol,
            piece = piece
        )
        
        onMoveReceived?.invoke(move)
        
        // Switch turn
        currentTurn = if (currentTurn == whitePlayer) blackPlayer else whitePlayer
        
        // Broadcast new game state
        broadcastGameState()
        
        Log.d(TAG, "Move: ${player.name} moved from ($fromRow,$fromCol) to ($toRow,$toRow)")
    }
    
    /**
     * Broadcast current game state to all clients
     */
    private fun broadcastGameState() {
        val state = serializeGameState()
        val packet = PacketProtocol.createTextPacket(
            PacketProtocol.TYPE_GAME_STATE,
            0, // Server
            state
        )
        server.broadcast(packet.toBytes())
        
        onGameStateChanged?.invoke(GameState(board, currentTurn))
    }
    
    /**
     * Serialize board state to string
     */
    private fun serializeGameState(): String {
        val boardStr = board.joinToString(";") { row ->
            row.joinToString(",") { it.symbol.toString() }
        }
        return "$currentTurn|$boardStr"
    }
    
    /**
     * Get current board state
     */
    fun getBoard(): Array<Array<Piece>> = board
    
    /**
     * Get current turn
     */
    fun getCurrentTurn(): Byte = currentTurn
}

/**
 * Chess pieces
 */
enum class Piece(val symbol: Char) {
    EMPTY('.'),
    WHITE_PAWN('P'),
    WHITE_ROOK('R'),
    WHITE_KNIGHT('N'),
    WHITE_BISHOP('B'),
    WHITE_QUEEN('Q'),
    WHITE_KING('K'),
    BLACK_PAWN('p'),
    BLACK_ROOK('r'),
    BLACK_KNIGHT('n'),
    BLACK_BISHOP('b'),
    BLACK_QUEEN('q'),
    BLACK_KING('k');
    
    fun isWhite() = symbol.isUpperCase()
    fun isBlack() = symbol.isLowerCase() && this != EMPTY
}

/**
 * Chess move data
 */
data class ChessMove(
    val playerId: Byte,
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val piece: Piece
)

/**
 * Game state snapshot
 */
data class GameState(
    val board: Array<Array<Piece>>,
    val currentTurn: Byte
)
