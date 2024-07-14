package code.name.monkey.retromusic.views
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import code.name.monkey.retromusic.R

class GMeterScaleMarkGraphicView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val scaleMarkPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }
    private var direction = Direction.LEFT
    private val roundingMargin = 2.0f
    private val arrowWidth = 10.0f
    private var viewWidth = 0f
    private var viewHeight = 0f
    private var baseCenterX = 0f
    private var baseCenterY = 0f
    private var centerX = baseCenterX
    private var centerY = baseCenterY

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.GMeterScaleMarkGraphicView, 0, 0).apply {
            direction = Direction.fromInt(getInt(R.styleable.GMeterScaleMarkGraphicView_direction, Direction.LEFT.value))
        }.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            if (direction == Direction.LEFT) {
                val path = Path().apply {
                    moveTo(roundingMargin, centerY - arrowWidth)
                    lineTo(roundingMargin, centerY + arrowWidth)
                    lineTo(viewWidth - roundingMargin, centerY)
                    close()
                }
                canvas.drawPath(path, scaleMarkPaint)
            }
            if (direction == Direction.RIGHT) {
                val path = Path().apply {
                    moveTo(viewWidth - roundingMargin, centerY - arrowWidth)
                    lineTo(viewWidth - roundingMargin, centerY + arrowWidth)
                    lineTo(roundingMargin, centerY)
                    close()
                }
                canvas.drawPath(path, scaleMarkPaint)
            }
            if (direction == Direction.TOP) {
                val path = Path().apply {
                    moveTo(centerX - arrowWidth, roundingMargin)
                    lineTo(centerX + arrowWidth, roundingMargin)
                    lineTo(centerX, viewHeight - roundingMargin)
                    close()
                }
                canvas.drawPath(path, scaleMarkPaint)
            }
            if (direction == Direction.BOTTOM) {
                val path = Path().apply {
                    moveTo(centerX - arrowWidth, viewHeight - roundingMargin)
                    lineTo(centerX + arrowWidth, viewHeight - roundingMargin)
                    lineTo(centerX, roundingMargin)
                    close()
                }
                canvas.drawPath(path, scaleMarkPaint)
            }
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
