package code.name.monkey.retromusic.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentSpeedSyncBinding
import com.google.android.material.progressindicator.LinearProgressIndicator


class SpeedSyncFragment : Fragment() {

    private lateinit var binding: FragmentSpeedSyncBinding

    private var progress = 0.0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSpeedSyncBinding.inflate(inflater, container, false)

        binding.speedProgressBar.progress = 0

        startVibrationAnimation(binding.speedProgressBar)

        return binding.root
    }
    private fun startVibrationAnimation(progressBar: LinearProgressIndicator) {
        val animator = ValueAnimator.ofFloat(0f, 100f).apply {
            duration = 5000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE

            addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                progressBar.progress = progress.toInt()

                val color = when {
                    progress in 0.0..30.0 -> ContextCompat.getColor(requireContext(), code.name.monkey.appthemehelper.R.color.md_green_A700)
                    progress in 30.0..50.0 -> ContextCompat.getColor(requireContext(), code.name.monkey.appthemehelper.R.color.md_blue_A400)
                    progress in 50.0..80.0 -> ContextCompat.getColor(requireContext(), code.name.monkey.appthemehelper.R.color.md_yellow_A400)
                    progress in 80.0..110.0 -> ContextCompat.getColor(requireContext(), code.name.monkey.appthemehelper.R.color.md_red_A400)
                    else -> ContextCompat.getColor(requireContext(), R.color.dark_color)
                }
                progressBar.setIndicatorColor(color)
            }
        }
        animator.start()
    }
}
