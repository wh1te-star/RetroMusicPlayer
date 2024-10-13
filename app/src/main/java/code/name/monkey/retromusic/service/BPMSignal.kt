package code.name.monkey.retromusic.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.os.SystemClock
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import android.widget.LinearLayout
import code.name.monkey.retromusic.R

class BPMSignal @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val circles: MutableList<CircleView> = mutableListOf()
    private val spaces: MutableList<View> = mutableListOf()

    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(24.dp, 8.dp, 24.dp, 16.dp)
        }

        addCircleView(context)
        addSpace()
        addCircleView(context)
        addSpace()
        addCircleView(context)
        addSpace()
        addCircleView(context)
    }

    private fun addCircleView(context: Context) {
        val circleView = CircleView(context).apply {
            layoutParams = LayoutParams(30.dp, 30.dp)
        }
        addView(circleView)
        circles.add(circleView)
    }

    private fun addSpace() {
        val space = View(context).apply {
            layoutParams = LayoutParams(0, 0, 1f)
        }
        addView(space)
        spaces.add(space)
    }

    fun switchBeats(){
        if(circles.last().visibility == View.GONE){
            circles.last().visibility = View.VISIBLE
            spaces.last().visibility = View.VISIBLE
        }else {
            circles.last().visibility = View.GONE
            spaces.last().visibility = View.GONE
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}

class BPMTimer {

    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val intervalMillis: Long = 1000

    fun start() {
        val initialDelay = calculateInitialDelay()
        executor.scheduleWithFixedDelay({
            val currentTime = SystemClock.elapsedRealtimeNanos()
            println("Current Time: $currentTime")
            ////
        }, initialDelay, intervalMillis, TimeUnit.MILLISECONDS)
    }

    fun setInterval(interval: Long){

    }

    private fun calculateInitialDelay(): Long {
        val currentTime = SystemClock.elapsedRealtime()
        val nextMinute = (currentTime / 60000 + 1) * 60000
        return nextMinute - currentTime
    }

    fun stop() {
        executor.shutdown()
    }
}

class CircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = 0xFF0000FF.toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = Math.min(width, height) / 2f
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
    }
}
