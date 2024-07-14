package code.name.monkey.retromusic.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.service.AcceleroValueListener
import code.name.monkey.retromusic.util.logD
import code.name.monkey.retromusic.service.GPSRecordService
import code.name.monkey.retromusic.views.GMeterGraphicView
import code.name.monkey.retromusic.databinding.FragmentGForceMeterBinding

class GForceMeterFragment : Fragment(), AcceleroValueListener {

    private var _binding: FragmentGForceMeterBinding? = null
    private val binding get() = _binding!!

    private var delayMilliSecond = 1000L
    private var maxScaleValue = 5.0f
    private var viewWidth = 0
    private var viewHeight = 0

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

        logD("${(System.currentTimeMillis()/1000).toInt()}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
