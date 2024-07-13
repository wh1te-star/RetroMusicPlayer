package code.name.monkey.retromusic.fragments

import android.os.Bundle
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

class GForceMeterFragment : Fragment(), AcceleroValueListener{

    private var _binding: FragmentGForceMeterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGForceMeterBinding.inflate(inflater, container, false)
        val view = binding.root
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
        binding.GMeterGraphic.updateMeterPosition(x, y)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
