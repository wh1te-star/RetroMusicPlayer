package code.name.monkey.retromusic.fragments

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentSpeedSyncBinding
import code.name.monkey.retromusic.util.logD
import com.google.android.material.progressindicator.LinearProgressIndicator


class SpeedSyncFragment : Fragment() {

    private lateinit var binding: FragmentSpeedSyncBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private var progress = 0.0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSpeedSyncBinding.inflate(inflater, container, false)

        binding.speedProgressBar.progress = 0

        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        startLocationUpdates()

        return binding.root
    }

    private fun startLocationUpdates() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onSpeedChanged(location.speed)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0,
            0.0f,
            locationListener
        )
    }

    private fun onSpeedChanged(speed: Float) {
        progress = (speed * 3.6f -10.0f) * 100.0f / 110f

        binding.speedProgressBar.progress = progress.toInt()

        logD("speed: ${speed}, progress: ${progress}")
        val color = when {
            progress in 0.0..30.0 -> ContextCompat.getColor(requireContext(), code.name.monkey.appthemehelper.R.color.md_green_A700)
            progress in 30.0..50.0 -> ContextCompat.getColor(requireContext(), code.name.monkey.appthemehelper.R.color.md_blue_A400)
            progress in 50.0..80.0 -> ContextCompat.getColor(requireContext(), code.name.monkey.appthemehelper.R.color.md_yellow_A400)
            progress in 80.0..110.0 -> ContextCompat.getColor(requireContext(), code.name.monkey.appthemehelper.R.color.md_red_A400)
            else -> ContextCompat.getColor(requireContext(), R.color.dark_color)
        }
        binding.speedProgressBar.setIndicatorColor(color)

        binding.speedTextView.text = getSpeedString((speed*3.6).toInt().toString())
        binding.durationTextView.text = getSpeedString((speed*3.6).toString())
    }

    fun getSpeedString(speed: String): SpannableString {
        val text = "$speed km/h"
        val spannableString = SpannableString(text)

        spannableString.setSpan(
            RelativeSizeSpan(1.0f),
            0, speed.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannableString.setSpan(
            RelativeSizeSpan(0.7f),
            speed.length, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationManager.removeUpdates(locationListener)
    }
}
