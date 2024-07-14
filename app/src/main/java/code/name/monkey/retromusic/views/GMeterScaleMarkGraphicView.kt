package code.name.monkey.retromusic.views
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import code.name.monkey.retromusic.R

class GMeterScaleMarkGraphicView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val markFillPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        setShadowLayer(10f, 5f, 5f, Color.BLACK)
    }
    private val markStrokePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3.0f
    }
    private var direction = Direction.LEFT
    private val roundingMargin = 2.0f
    private val arrowWidth = 30.0f
    private var viewWidth = 0f
    private var viewHeight = 0f
    private var baseCenterX = 0f
    private var baseCenterY = 0f
    private var centerX = baseCenterX
    private var centerY = baseCenterY

    private val trianglePath = Path()

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.GMeterScaleMarkGraphicView, 0, 0)
            .apply {
                direction = Direction.fromInt(
                    getInt(
                        R.styleable.GMeterScaleMarkGraphicView_direction,
                        Direction.LEFT.value
                    )
                )
            }.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            updatePaths(direction)
            canvas.drawPath(trianglePath, markFillPaint)
            canvas.drawPath(trianglePath, markStrokePaint)
        }
    }

    fun updateMeterPosition(x: Float, y: Float) {
        centerX = baseCenterX + x
        centerY = baseCenterY + y
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()
        baseCenterX = viewWidth / 2
        baseCenterY = viewHeight / 2
    }

    fun updatePaths(direction: Direction) {
        when (direction) {
            Direction.LEFT -> {
                trianglePath.reset()
                trianglePath.moveTo(roundingMargin, centerY - arrowWidth)
                trianglePath.lineTo(roundingMargin, centerY + arrowWidth)
                trianglePath.lineTo(viewWidth - roundingMargin, centerY)
                trianglePath.close()
            }

            Direction.RIGHT -> {
                trianglePath.reset()
                trianglePath.moveTo(viewWidth - roundingMargin, centerY - arrowWidth)
                trianglePath.lineTo(viewWidth - roundingMargin, centerY + arrowWidth)
                trianglePath.lineTo(roundingMargin, centerY)
                trianglePath.close()
            }

            Direction.TOP -> {
                trianglePath.reset()
                trianglePath.moveTo(centerX - arrowWidth, roundingMargin)
                trianglePath.lineTo(centerX + arrowWidth, roundingMargin)
                trianglePath.lineTo(centerX, viewHeight - roundingMargin)
                trianglePath.close()
            }

            else -> {
                trianglePath.reset()
                trianglePath.moveTo(centerX - arrowWidth, viewHeight - roundingMargin)
                trianglePath.lineTo(centerX + arrowWidth, viewHeight - roundingMargin)
                trianglePath.lineTo(centerX, roundingMargin)
                trianglePath.close()
            }
        }
    }
}

enum class Direction(val value: Int) {
    LEFT(0),
    TOP(1),
    RIGHT(2),
    BOTTOM(3);

    companion object {
        fun fromInt(value: Int) = values().firstOrNull { it.value == value } ?: LEFT
    }
}
