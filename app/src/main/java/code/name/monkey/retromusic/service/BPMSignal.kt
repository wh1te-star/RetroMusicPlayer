package code.name.monkey.retromusic.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.AttributeSet
import android.view.View
import android.os.SystemClock
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import android.widget.LinearLayout
import code.name.monkey.retromusic.R
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

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private var soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(audioAttributes)
        .build()
    private var soundId = soundPool.load(context, R.raw.tambourine01_1_mute, 1)

    init {
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
        executor.shutdownNow()
        executor = Executors.newSingleThreadScheduledExecutor()

        executor.scheduleWithFixedDelay({
            signals[index].signalOff()
            index = (index + 1) % beat
            signals[index].signalOn()

            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
            val currentTime = SystemClock.elapsedRealtimeNanos()
            logD("index, interval[nanosec], currenttime[nanosec]: $index, ${intervalMillis*1000000}, $currentTime")
        }, 0, intervalMillis, TimeUnit.MILLISECONDS)
        this.intervalMillis = intervalMillis
    }

    fun switchBeats(){
        if(beat == 4){
            beat = 3
            index = 0
            signals.last().visibility = View.GONE
            spaces.last().visibility = View.GONE
            signals[0].signalOff()
            signals[1].signalOff()
            signals[2].signalOff()
        }else {
            beat = 4
            index = 0
            signals.last().visibility = View.VISIBLE
            spaces.last().visibility = View.VISIBLE
            signals[0].signalOff()
            signals[1].signalOff()
            signals[2].signalOff()
            signals[3].signalOff()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        soundPool.release()
        executor.shutdownNow()
    }
}

class CircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var signalVisible = false

    private val paintFill = Paint().apply {
        isAntiAlias = true
        color = 0xFF0000FF.toInt()
        style = Paint.Style.FILL
    }

    private val paintStroke = Paint().apply {
        isAntiAlias = true
        color = 0xFF000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4.0f
    }

    fun signalOn() {
        signalVisible = true
        invalidate()
    }

    fun signalOff() {
        signalVisible = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = Math.min(width, height) / 2f - paintStroke.strokeWidth / 2

        if (signalVisible) {
            canvas.drawCircle(width / 2f, height / 2f, radius, paintFill)
        }

        canvas.drawCircle(width / 2f, height / 2f, radius, paintStroke)
    }
}
