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
import code.name.monkey.retromusic.util.logD

class BPMSignal @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var index = 0
    private var beat = 4
    private var intervalMillis: Long = 1000

    private val signals: MutableList<CircleView> = mutableListOf()
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
        signals.add(circleView)
    }

    private fun addSpace() {
        val space = View(context).apply {
            layoutParams = LayoutParams(0, 0, 1f)
        }
        addView(space)
        spaces.add(space)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    fun startTimer(intervalMillis: Long) {
        stopTimer()
        executor = Executors.newSingleThreadScheduledExecutor()

        val initialDelay = calculateInitialDelay()
        executor.scheduleWithFixedDelay({
            val currentTime = SystemClock.elapsedRealtimeNanos()
            logD("Current Time abcd: $index $currentTime")

            signals[index].signalOff()
            index = (index + 1) % beat
            signals[index].signalOn()

        }, 0, intervalMillis, TimeUnit.MILLISECONDS)
        this.intervalMillis = intervalMillis
    }

    fun stopTimer() {
        executor.shutdownNow()
    }

    private fun calculateInitialDelay(): Long {
        val currentTime = SystemClock.elapsedRealtime()
        val nextMinute = (currentTime / 60000 + 1) * 60000
        return nextMinute - currentTime
    }

    fun switchBeats(){
        if(beat == 4){
            beat = 3
            signals.last().visibility = View.GONE
            spaces.last().visibility = View.GONE
        }else {
            beat = 4
            signals.last().visibility = View.VISIBLE
            spaces.last().visibility = View.VISIBLE
        }
    }
}

class CircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var signalVisible = false

    private val paintOn = Paint().apply {
        isAntiAlias = true
        color = 0xFF0000FF.toInt()
    }

    private val paintOff = Paint().apply {
        isAntiAlias = true
        color = 0xFF444444.toInt()
    }

    fun signalOn(){
        signalVisible = true
        invalidate()
    }

    fun signalOff(){
        signalVisible = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = Math.min(width, height) / 2f
        if(signalVisible){
            canvas.drawCircle(width / 2f, height / 2f, radius, paintOn)
        }else{
            canvas.drawCircle(width / 2f, height / 2f, radius, paintOff)
        }
    }
}
