/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.fragments.other

import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.VOLUME_WARN_THRESHOLD
import code.name.monkey.retromusic.databinding.FragmentVolumeBinding
import code.name.monkey.retromusic.extensions.applyColor
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.volume.AudioVolumeObserver
import code.name.monkey.retromusic.volume.OnAudioVolumeChangedListener
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView

class VolumeFragment : Fragment(), Slider.OnChangeListener, OnAudioVolumeChangedListener,
    View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private var _binding: FragmentVolumeBinding? = null
    private val binding get() = _binding!!

    private var audioVolumeObserver: AudioVolumeObserver? = null

    private val audioManager: AudioManager
        get() = requireContext().getSystemService()!!

    private var previousVolume = 0

    private var currentMaxVolume = 0

    private var isDialogShown = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVolumeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        currentMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        binding.volumeSeekBar.valueTo = currentMaxVolume.toFloat()
        binding.volumeSeekBar.value = previousVolume.toFloat()
        setTintable(ThemeStore.accentColor(requireContext()))
        binding.volumeDown.setOnClickListener(this)
        binding.volumeUp.setOnClickListener(this)
        binding.volumeDown.setOnLongClickListener {
            val dialogLayout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_volume_warning_threshold, null)
            val volumeWarningSeekBar = dialogLayout.findViewById<Slider>(R.id.volumeWarningSeekBar)
            val volumeWarningValueTextView = dialogLayout.findViewById<MaterialTextView>(R.id.volumeWarningValue)
            val volumeWarningMaxTextView = dialogLayout.findViewById<MaterialTextView>(R.id.volumeWarningMax)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            volumeWarningSeekBar.value = sharedPreferences.getInt(VOLUME_WARN_THRESHOLD, 0).toFloat()
            volumeWarningValueTextView.text = sharedPreferences.getInt(VOLUME_WARN_THRESHOLD, 0).toString()
            volumeWarningSeekBar.valueTo = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
            volumeWarningMaxTextView.text = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toInt().toString()
            volumeWarningSeekBar.addOnChangeListener { slider, value, fromUser ->
                volumeWarningValueTextView.text = value.toInt().toString()
            }
            volumeWarningSeekBar.setLabelFormatter { value ->
                value.toInt().toString()
            }

                val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(R.string.volume_warning_threshold)
            builder.setView(dialogLayout)
            builder.setPositiveButton("OK") { dialog, _ ->
                sharedPreferences.edit()
                    .putInt(VOLUME_WARN_THRESHOLD, volumeWarningSeekBar .value.toInt())
                    .apply()
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
            true
        }
        binding.volumeUp.setOnLongClickListener {
            val dialogLayout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_volume_limit, null)
            val limitToCurrentVolumeButton = dialogLayout.findViewById<Button>(R.id.limitToCurrentVolumeButton)
            val resetLimitButton = dialogLayout.findViewById<Button>(R.id.resetLimitButton)
            limitToCurrentVolumeButton.setOnClickListener{
                currentMaxVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                binding.volumeSeekBar.valueTo = currentMaxVolume.toFloat()
            }
            resetLimitButton.setOnClickListener {
                currentMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                binding.volumeSeekBar.valueTo = currentMaxVolume.toFloat()
            }

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Current Max Volume Limit")
            builder.setView(dialogLayout)
            builder.setPositiveButton(R.string.close) { dialog, _ -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (audioVolumeObserver == null) {
            audioVolumeObserver = AudioVolumeObserver(requireActivity())
        }
        audioVolumeObserver?.register(AudioManager.STREAM_MUSIC, this)

        val audioManager = audioManager
        binding.volumeSeekBar.valueTo =
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        binding.volumeSeekBar.value =
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        binding.volumeSeekBar.addOnChangeListener(this)

        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        if (_binding != null) {
            if (currentVolume > binding.volumeSeekBar.valueTo) {
                binding.volumeSeekBar.valueTo = currentVolume.toFloat()
            }
            previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            binding.volumeSeekBar.value = currentVolume.toFloat()
            binding.volumeDown.setImageResource(if (currentVolume == 0) R.drawable.ic_volume_off else R.drawable.ic_volume_down)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioVolumeObserver?.unregister()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        val audioManager = audioManager
        if (fromUser && !isDialogShown) {
            if (value > previousVolume &&
                value > PreferenceUtil.volumeWarnThreshold
            ) {
                isDialogShown = true
                AlertDialog.Builder(requireContext())
                    .setTitle("Warning")
                    .setMessage(
                        "This is extremely high volume audio." +
                                "Are you sure you want to increase the volume?"
                    )
                    .setPositiveButton("Yes") { dialog, _ ->
                        val newVolume =
                            binding.volumeSeekBar.value *
                            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) /
                            binding.volumeSeekBar.valueTo
                        binding.volumeSeekBar.value = newVolume
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            newVolume.toInt(),
                            0
                        )
                        isDialogShown = false
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        binding.volumeSeekBar.value = previousVolume.toFloat()
                        isDialogShown = false
                        dialog.dismiss()
                    }
                    .setOnCancelListener{
                        binding.volumeSeekBar.value = previousVolume.toFloat()
                        isDialogShown = false
                    }
                    .show()
            } else {
                binding.volumeSeekBar.value = value
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value.toInt(), 0)
            }
        }
        setPauseWhenZeroVolume(value < 1f)
        binding.volumeDown.setImageResource(if (value == 0f) R.drawable.ic_volume_off else R.drawable.ic_volume_down)
    }

    override fun onClick(view: View) {
        val audioManager = audioManager
        when (view.id) {
            R.id.volumeDown -> audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0
            )
            R.id.volumeUp -> audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0
            )
        }
    }

    fun tintWhiteColor() {
        val color = Color.WHITE
        binding.volumeDown.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        binding.volumeUp.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        binding.volumeSeekBar.applyColor(color)
    }

    fun setTintable(color: Int) {
        binding.volumeSeekBar.applyColor(color)
    }

    private fun setPauseWhenZeroVolume(pauseWhenZeroVolume: Boolean) {
        if (PreferenceUtil.isPauseOnZeroVolume)
            if (MusicPlayerRemote.isPlaying && pauseWhenZeroVolume)
                MusicPlayerRemote.pauseSong()
    }

    fun setTintableColor(color: Int) {
        binding.volumeDown.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        binding.volumeUp.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        // TintHelper.setTint(volumeSeekBar, color, false)
        binding.volumeSeekBar.applyColor(color)
    }

    companion object {
        fun newInstance(): VolumeFragment {
            return VolumeFragment()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {}
}
