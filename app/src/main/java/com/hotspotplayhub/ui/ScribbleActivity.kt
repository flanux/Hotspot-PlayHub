package com.hotspotplayhub.ui

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.hotspotplayhub.R
import com.hotspotplayhub.engine.Client
import com.hotspotplayhub.engine.Packet
import com.hotspotplayhub.engine.PacketProtocol
import com.hotspotplayhub.engine.Server
import com.hotspotplayhub.modules.scribble.DrawingView
import com.hotspotplayhub.modules.scribble.ScribbleModule
import java.nio.ByteBuffer

class ScribbleActivity : AppCompatActivity() {
    
    companion object {
        var staticModule: ScribbleModule? = null
        var staticServer: Server? = null
        var staticClient: Client? = null
    }
    
    private lateinit var drawingView: DrawingView
    private lateinit var tvRoundInfo: TextView
    private lateinit var tvWord: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvGuesses: TextView
    private lateinit var scrollViewGuesses: ScrollView
    private lateinit var layoutDrawingTools: LinearLayout
    private lateinit var layoutGuessInput: LinearLayout
    private lateinit var etGuess: EditText
    private lateinit var btnSendGuess: Button
    private lateinit var btnColor: Button
    private lateinit var btnClear: Button
    private lateinit var seekBarStroke: SeekBar
    
    private var scribbleModule: ScribbleModule? = null
    private var server: Server? = null
    private var client: Client? = null
    private var isHost = false
    private var playerId: Byte = 0
    private var isDrawer = false
    
    private var countDownTimer: CountDownTimer? = null
    
    private val colors = listOf(
        Color.BLACK, Color.RED, Color.BLUE, Color.GREEN,
        Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.parseColor("#FF5722")
    )
    private var currentColorIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scribble)
        
        initializeViews()
        setupDrawingTools()
        setupGuessInput()
        
        isHost = intent.getBooleanExtra("isHost", false)
        playerId = intent.getByteExtra("playerId", 0)
        
        // Setup with static references
        scribbleModule = staticModule
        server = staticServer
        client = staticClient
        
        if (scribbleModule != null) {
            setupModule(scribbleModule!!, server, client)
        }
        
        // Client listens for packets
        if (!isHost) {
            listenForPackets()
        }
    }
    
    private fun initializeViews() {
        drawingView = findViewById(R.id.drawingView)
        tvRoundInfo = findViewById(R.id.tvRoundInfo)
        tvWord = findViewById(R.id.tvWord)
        tvTimer = findViewById(R.id.tvTimer)
        tvGuesses = findViewById(R.id.tvGuesses)
        scrollViewGuesses = findViewById(R.id.scrollViewGuesses)
        layoutDrawingTools = findViewById(R.id.layoutDrawingTools)
        layoutGuessInput = findViewById(R.id.layoutGuessInput)
        etGuess = findViewById(R.id.etGuess)
        btnSendGuess = findViewById(R.id.btnSendGuess)
        btnColor = findViewById(R.id.btnColor)
        btnClear = findViewById(R.id.btnClear)
        seekBarStroke = findViewById(R.id.seekBarStroke)
    }
    
    private fun setupDrawingTools() {
        btnColor.setBackgroundColor(colors[currentColorIndex])
        btnColor.setOnClickListener {
            currentColorIndex = (currentColorIndex + 1) % colors.size
            drawingView.currentColor = colors[currentColorIndex]
            btnColor.setBackgroundColor(colors[currentColorIndex])
        }
        
        btnClear.setOnClickListener {
            drawingView.clear()
            broadcastClear()
        }
        
        seekBarStroke.max = 50
        seekBarStroke.progress = 8
        seekBarStroke.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawingView.currentStrokeWidth = progress.toFloat().coerceAtLeast(1f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        drawingView.onDrawAction = { x, y ->
            if (isDrawer) {
                broadcastDrawing(x, y)
            }
        }
    }
    
    private fun setupGuessInput() {
        btnSendGuess.setOnClickListener {
            sendGuess()
        }
        
        etGuess.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                sendGuess()
                true
            } else {
                false
            }
        }
    }
    
    fun setupModule(module: ScribbleModule, srv: Server?, cli: Client?) {
        scribbleModule = module
        server = srv
        client = cli
        
        // Listen for game events
        module.onRoundStart = { roundInfo ->
            runOnUiThread {
                handleRoundStart(roundInfo)
            }
        }
        
        module.onDrawReceived = { drawAction ->
            runOnUiThread {
                drawingView.drawRemotePoint(
                    drawAction.x, drawAction.y, drawAction.color, drawAction.strokeWidth
                )
            }
        }
        
        module.onGuessReceived = { guessAction ->
            runOnUiThread {
                addGuess("${guessAction.playerName}: ${guessAction.guess}${if (guessAction.isCorrect) " ✓" else ""}")
            }
        }
        
        module.onClearReceived = {
            runOnUiThread {
                drawingView.clear()
            }
        }
        
        module.onRoundEnd = { result ->
            runOnUiThread {
                handleRoundEnd(result)
            }
        }
    }
    
    private fun handleRoundStart(roundInfo: com.hotspotplayhub.modules.scribble.RoundInfo) {
        tvRoundInfo.text = "Round ${roundInfo.roundNumber}/${roundInfo.maxRounds}"
        
        isDrawer = roundInfo.drawerId == playerId
        
        if (isDrawer) {
            tvWord.text = roundInfo.word
            layoutDrawingTools.visibility = View.VISIBLE
            layoutGuessInput.visibility = View.GONE
            drawingView.drawingEnabled = true
        } else {
            tvWord.text = "_".repeat(roundInfo.wordLength).toCharArray().joinToString(" ")
            layoutDrawingTools.visibility = View.GONE
            layoutGuessInput.visibility = View.VISIBLE
            drawingView.drawingEnabled = false
        }
        
        drawingView.clear()
        tvGuesses.text = ""
        
        startTimer(roundInfo.timeSeconds)
    }
    
    private fun handleRoundEnd(result: com.hotspotplayhub.modules.scribble.RoundResult) {
        countDownTimer?.cancel()
        tvWord.text = "Word was: ${result.word}"
        
        // Show scores
        val scoresText = result.scores.joinToString("\n") { "${it.name}: ${it.score}" }
        addGuess("\n--- Round End ---\n$scoresText\n")
        
        // Wait 5 seconds then start next round (host only)
        if (isHost) {
            android.os.Handler(mainLooper).postDelayed({
                scribbleModule?.triggerNextRound()
            }, 5000)
        }
    }
    
    private fun startTimer(seconds: Int) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(seconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                tvTimer.text = "${secondsLeft}s"
            }
            
            override fun onFinish() {
                tvTimer.text = "Time's up!"
            }
        }.start()
    }
    
    private fun sendGuess() {
        val guess = etGuess.text.toString().trim()
        if (guess.isEmpty()) return
        
        val packet = scribbleModule?.createGuessPacket(playerId, guess)
        packet?.let {
            if (isHost) {
                server?.broadcast(it)
            } else {
                client?.sendPacket(it)
            }
        }
        
        etGuess.setText("")
    }
    
    private fun broadcastDrawing(x: Float, y: Float) {
        scribbleModule?.let { module ->
            val packet = module.createDrawPacket(
                playerId, x, y, drawingView.currentColor, drawingView.currentStrokeWidth
            )
            
            if (isHost) {
                server?.broadcast(packet)
            } else {
                client?.sendPacket(packet)
            }
        }
    }
    
    private fun broadcastClear() {
        scribbleModule?.let { module ->
            val packet = module.createClearPacket(playerId)
            
            if (isHost) {
                server?.broadcast(packet)
            } else {
                client?.sendPacket(packet)
            }
        }
    }
    
    private fun addGuess(text: String) {
        tvGuesses.append("$text\n")
        scrollViewGuesses.post {
            scrollViewGuesses.fullScroll(View.FOCUS_DOWN)
        }
    }
    
    private fun listenForPackets() {
        client?.onPacketReceived = { packet ->
            if (packet.type == PacketProtocol.TYPE_SCRIBBLE) {
                handlePacket(packet)
            }
        }
    }
    
    private fun handlePacket(packet: Packet) {
        val buffer = ByteBuffer.wrap(packet.payload)
        val action = buffer.get()
        
        when (action) {
            ScribbleModule.ACTION_DRAW -> {
                val x = buffer.float
                val y = buffer.float
                val color = buffer.int
                val strokeWidth = buffer.float
                runOnUiThread {
                    drawingView.drawRemotePoint(x, y, color, strokeWidth)
                }
            }
            ScribbleModule.ACTION_CLEAR -> {
                runOnUiThread {
                    drawingView.clear()
                }
            }
            ScribbleModule.ACTION_START_ROUND -> {
                val dataLength = buffer.int
                val dataBytes = ByteArray(dataLength)
                buffer.get(dataBytes)
                val data = String(dataBytes, Charsets.UTF_8)
                
                runOnUiThread {
                    if (data.startsWith("DRAW:")) {
                        val word = data.substring(5)
                        tvWord.text = word
                        isDrawer = true
                        layoutDrawingTools.visibility = View.VISIBLE
                        layoutGuessInput.visibility = View.GONE
                        drawingView.drawingEnabled = true
                    } else if (data.startsWith("GUESS:")) {
                        val hint = data.substring(6)
                        tvWord.text = hint
                        isDrawer = false
                        layoutDrawingTools.visibility = View.GONE
                        layoutGuessInput.visibility = View.VISIBLE
                        drawingView.drawingEnabled = false
                    }
                    drawingView.clear()
                }
            }
            ScribbleModule.ACTION_GUESS -> {
                val dataLength = buffer.int
                val dataBytes = ByteArray(dataLength)
                buffer.get(dataBytes)
                val text = String(dataBytes, Charsets.UTF_8)
                runOnUiThread {
                    addGuess(text)
                }
            }
            ScribbleModule.ACTION_END_ROUND -> {
                val dataLength = buffer.int
                val dataBytes = ByteArray(dataLength)
                buffer.get(dataBytes)
                val data = String(dataBytes, Charsets.UTF_8)
                runOnUiThread {
                    // Parse and display round end
                    val parts = data.split("|")
                    if (parts.size >= 2) {
                        val word = parts[0].substringAfter("WORD:")
                        tvWord.text = "Word was: $word"
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
