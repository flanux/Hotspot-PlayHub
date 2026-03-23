package com.hotspotplayhub.modules.scribble

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Custom view for drawing on canvas
 */
class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paths = mutableListOf<DrawPath>()
    private var currentPath: Path? = null
    private var currentPaint: Paint? = null
    
    var currentColor: Int = Color.BLACK
    var currentStrokeWidth: Float = 8f
    var drawingEnabled: Boolean = true
    
    var onDrawAction: ((Float, Float) -> Unit)? = null
    
    init {
        setBackgroundColor(Color.WHITE)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw all paths
        for (drawPath in paths) {
            canvas.drawPath(drawPath.path, drawPath.paint)
        }
        
        // Draw current path
        currentPath?.let { path ->
            currentPaint?.let { paint ->
                canvas.drawPath(path, paint)
            }
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!drawingEnabled) return false
        
        val x = event.x
        val y = event.y
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startPath(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                continuePath(x, y)
                onDrawAction?.invoke(x, y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                endPath()
                return true
            }
        }
        
        return false
    }
    
    private fun startPath(x: Float, y: Float) {
        currentPath = Path().apply {
            moveTo(x, y)
        }
        
        currentPaint = Paint().apply {
            color = currentColor
            strokeWidth = currentStrokeWidth
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
    }
    
    private fun continuePath(x: Float, y: Float) {
        currentPath?.lineTo(x, y)
        invalidate()
    }
    
    private fun endPath() {
        currentPath?.let { path ->
            currentPaint?.let { paint ->
                paths.add(DrawPath(Path(path), Paint(paint)))
            }
        }
        currentPath = null
        currentPaint = null
    }
    
    /**
     * Draw a point from another player
     */
    fun drawRemotePoint(x: Float, y: Float, color: Int, strokeWidth: Float) {
        val paint = Paint().apply {
            this.color = color
            this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x, y)
        }
        
        paths.add(DrawPath(path, paint))
        invalidate()
    }
    
    /**
     * Clear the canvas
     */
    fun clear() {
        paths.clear()
        currentPath = null
        currentPaint = null
        invalidate()
    }
    
    private data class DrawPath(
        val path: Path,
        val paint: Paint
    )
}
