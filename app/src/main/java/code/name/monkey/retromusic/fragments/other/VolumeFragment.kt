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
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.VOLUME_ALWAYS_LIMIT
import code.name.monkey.retromusic.VOLUME_MAX_VALUE_TO
import code.name.monkey.retromusic.VOLUME_WARN_THRESHOLD
import code.name.monkey.retromusic.databinding.FragmentVolumeBinding
import code.name.monkey.retromusic.extensions.applyColor
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.logD
import code.name.monkey.retromusic.volume.AudioVolumeObserver
import code.name.monkey.retromusic.volume.OnAudioVolumeChangedListener
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView

class VolumeFragment : Fragment(), Slider.OnChangeListener, OnAudioVolumeChangedListener,
    View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private var _binding: FragmentVolumeBinding? = null
    private val binding get() = _binding!!

    private var audioVolumeObserver: AudioVolumeObserver? = null

    private val audioManager: AudioManager
        get() = requireContext().getSystemService()!!

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private var previousVolume = 0.0f

    private var currentMaxVolume = 0.0f

    private var isDialogShown = false

    private val ZERO = 1
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVolumeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.minVolumeValue.text = "0.0"
        binding.maxVolumeValue.text = sharedPreferences.getInt(VOLUME_MAX_VALUE_TO, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toString()
        binding.currentVolumeValue.text = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat().toString()

        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        currentMaxVolume = sharedPreferences.getInt(VOLUME_MAX_VALUE_TO, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toFloat()
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
            volumeWarningSeekBar.value = sharedPreferences.getInt(VOLUME_WARN_THRESHOLD, 0).toFloat()
            volumeWarningValueTextView.text = sharedPreferences.getInt(VOLUME_WARN_THRESHOLD, 0).toString()
            volumeWarningSeekBar.valueTo = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
            volumeWarningMaxTextView.text = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toString()
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
            val alwaysLimitCheckbox = dialogLayout.findViewById<MaterialCheckBox>(R.id.alwaysLimitCheckbox)
            limitToCurrentVolumeButton.setOnClickListener{
                currentMaxVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                binding.volumeSeekBar.valueTo = currentMaxVolume.toFloat()
                binding.maxVolumeValue.text = currentMaxVolume.toString()
                sharedPreferences.edit()
                    .putInt(VOLUME_MAX_VALUE_TO, currentMaxVolume.toInt())
                    .apply()
            }
            resetLimitButton.setOnClickListener {
                currentMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
                binding.volumeSeekBar.valueTo = currentMaxVolume.toFloat()
                binding.maxVolumeValue.text = currentMaxVolume.toString()
                sharedPreferences.edit()
                    .putInt(VOLUME_MAX_VALUE_TO, currentMaxVolume.toInt())
                    .apply()
            }
            alwaysLimitCheckbox.isChecked = PreferenceUtil.alwaysLimitVolume
            alwaysLimitCheckbox.setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit()
                    .putBoolean(VOLUME_ALWAYS_LIMIT, isChecked)
                    .apply()
                if (isChecked){
                    binding.volumeSeekBar.valueTo =
                        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()

                }
            }

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(getString(R.string.max_volume_limit))
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
        binding.volumeSeekBar.value =
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        binding.volumeSeekBar.valueTo = if (binding.volumeSeekBar.valueTo < binding.volumeSeekBar.value)
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        else
            sharedPreferences.getInt(VOLUME_MAX_VALUE_TO, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toFloat()
        binding.volumeSeekBar.addOnChangeListener(this)

        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(this)

        binding.currentVolumeValue.text = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat().toString()
    }

    override fun onAudioVolumeChanged(changedVolume: Int, maxVolume: Int) {
        if (_binding != null) {
            val currentVolume =
                if (changedVolume == 0) ZERO
                else changedVolume
            if (PreferenceUtil.alwaysLimitVolume || currentVolume > binding.volumeSeekBar.valueTo) {
                binding.volumeSeekBar.valueTo = currentVolume.toFloat()
                sharedPreferences.edit()
                    .putInt(VOLUME_MAX_VALUE_TO, currentVolume)
                    .apply()
                binding.maxVolumeValue.text = currentVolume.toString()
            }
            previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
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
                    .setTitle(getString(R.string.warning))
                    .setMessage(
                        getString(R.string.high_volume_warning)
                    )
                    .setPositiveButton(R.string.yes) { dialog, _ ->
                        binding.volumeSeekBar.value = value
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            value.toInt(),
                            0
                        )
                        isDialogShown = false
                    }
                    .setNegativeButton(R.string.no) { dialog, _ ->
                        binding.volumeSeekBar.value = previousVolume
                        isDialogShown = false
                        dialog.dismiss()
                    }
                    .setOnCancelListener{
                        binding.volumeSeekBar.value = previousVolume
                        isDialogShown = false
                    }
                    .show()
            } else {
                binding.volumeSeekBar.value = value
                setFloatVolue(value)
            }
        }
        binding.currentVolumeValue.text = value.toString()

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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == VOLUME_MAX_VALUE_TO){
            sharedPreferences!!.edit().putInt(VOLUME_MAX_VALUE_TO, currentMaxVolume.toInt()).apply()
        }
    }

    fun setFloatVolue(volume: Float){
        val systemVolume = Math.ceil(volume.toDouble()).toInt()
        val mediaPlayerVolume = volume / systemVolume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, systemVolume, 0)
        MusicPlayerRemote.musicService?.playback?.setVolume(mediaPlayerVolume)
    }
}
