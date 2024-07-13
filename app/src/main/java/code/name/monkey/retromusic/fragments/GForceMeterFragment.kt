package code.name.monkey.retromusic.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.service.AcceleroValueListener
import code.name.monkey.retromusic.util.logD
import code.name.monkey.retromusic.service.GPSRecordService


class GForceMeterFragment : Fragment(), AcceleroValueListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_g_force_meter, container, false)

        val circleContainer = view.findViewById<ConstraintLayout>(R.id.circleContainer)

        val circleDrawingView = CircleDrawingView(requireContext()).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }

        circleContainer.addView(circleDrawingView)

        fun updateGValues(newX: Float, newY: Float) {
            circleDrawingView.updateGValues(newX, newY)
        }

        return view
    }

    fun registerAcceleroListener(gpsRecordService: GPSRecordService){
        gpsRecordService.registerAcceleroListener(this)
    }

    fun unregisterAcceleroListener(gpsRecordService: GPSRecordService){
        gpsRecordService.unregisterAcceleroListener()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun updateAcceleroTextView(x: Float, y: Float) {
        logD("accelero value update: ${x}, ${y}")
    }
}
class CircleDrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val scaleMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
        strokeWidth = 5f
    }

    var gValueX = 50.0f
    var gValueY = 0f

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val roundMargin = 2
        val halfWidth = width.toFloat() / 2 - roundMargin
        val radii = listOf(halfWidth * 1/4f, halfWidth * 2/4f, halfWidth * 3/4f, halfWidth)

        canvas?.let {
            val centerX = roundMargin + halfWidth
            val centerY = roundMargin + halfWidth
            radii.forEach { radius ->
                it.drawCircle(centerX, centerY, radius, scaleMarkPaint)
            }
            it.drawCircle(centerX + gValueX * 10, centerY + gValueY * 10, 10.0f, indicatorPaint)
        }
    }

    fun updateGValues(newX: Float, newY: Float) {
        gValueX = newX
        gValueY = newY
        invalidate()
    }
}
