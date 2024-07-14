package code.name.monkey.retromusic.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import code.name.monkey.retromusic.service.AcceleroValueListener
import code.name.monkey.retromusic.service.GPSRecordService
import code.name.monkey.retromusic.databinding.FragmentGForceMeterBinding
import code.name.monkey.retromusic.helper.MusicPlayerRemote

class GForceMeterFragment : Fragment(), AcceleroValueListener {

    private var _binding: FragmentGForceMeterBinding? = null
    private val binding get() = _binding!!

    private var delayMilliSecond = 1000L
    private var maxScaleValue = 5.0f
    private var viewWidth = 0
    private var viewHeight = 0

    private lateinit var gestureDetector: GestureDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGForceMeterBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.post {
            viewWidth = view.width
            viewHeight = view.height
        }

        gestureDetector = GestureDetector(requireContext(), SwipeGestureListener(this))

        binding.GMeterGraphic.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
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
        Handler(Looper.getMainLooper()).postDelayed({
            updateMeterGraphic(x, y)
        }, delayMilliSecond)
    }

    fun updateMeterGraphic(x: Float, y: Float){
        val scaledX = x * viewWidth / maxScaleValue /2
        val scaledY = y * viewHeight / maxScaleValue /2
        binding.GMeterGraphic.updateMeterPosition(scaledX,scaledY)
        binding.leftScaleMark.updateMeterPosition(scaledX, scaledY)
        binding.topScaleMark.updateMeterPosition(scaledX, scaledY)
        binding.rightScaleMark.updateMeterPosition(scaledX, scaledY)
        binding.bottomScaleMark.updateMeterPosition(scaledX, scaledY)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SwipeGestureListener(private val context: GForceMeterFragment) : GestureDetector.SimpleOnGestureListener() {

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if(MusicPlayerRemote.isPlaying)
            MusicPlayerRemote.pauseSong()
        else
            MusicPlayerRemote.resumePlaying()
        return super.onSingleTapUp(e)
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null || e2 == null) return false

        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y

        if (Math.abs(diffX) > Math.abs(diffY)) {
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    onSwipeRight()
                } else {
                    onSwipeLeft()
                }
                return true
            }
        }
        return false
    }

    private fun onSwipeRight() {
        Toast.makeText(context.requireContext(), "Swiped Right", Toast.LENGTH_SHORT).show()
        MusicPlayerRemote.playNextSong()
    }

    private fun onSwipeLeft() {
        Toast.makeText(context.requireContext(), "Swiped Left", Toast.LENGTH_SHORT).show()
        MusicPlayerRemote.playPreviousSong()
    }
}
