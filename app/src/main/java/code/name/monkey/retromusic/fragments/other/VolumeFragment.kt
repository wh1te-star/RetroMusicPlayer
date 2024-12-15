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

import android.graphics.Color
import android.graphics.PorterDuff
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.base.AbsMusicServiceActivity
import code.name.monkey.retromusic.databinding.FragmentVolumeBinding
import code.name.monkey.retromusic.extensions.applyColor
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.volume.AudioVolumeObserver
import code.name.monkey.retromusic.volume.OnAudioVolumeChangedListener
import com.google.android.material.slider.Slider
import java.text.Bidi
import java.util.Locale


class VolumeViewModel : ViewModel() {
    private val _volume = MutableLiveData<Float>()
    val volume: LiveData<Float> get() = _volume

    fun setVolume(value: Float) {
        _volume.value = value
    }
}

class VolumeFragment : Fragment(), Slider.OnChangeListener, OnAudioVolumeChangedListener,
    View.OnClickListener {

    private var _binding: FragmentVolumeBinding? = null
    private val binding get() = _binding!!

    private lateinit var volumeViewModel: VolumeViewModel

    private var audioVolumeObserver: AudioVolumeObserver? = null

    private val audioManager: AudioManager
        get() = requireContext().getSystemService()!!

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
        setTintable(ThemeStore.accentColor(requireContext()))
        binding.volumeDown.setOnClickListener(this)
        binding.volumeUp.setOnClickListener(this)

        volumeViewModel = ViewModelProvider(requireActivity()).get(VolumeViewModel::class.java)
        binding.volumeSeekBar.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                volumeViewModel.setVolume(value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (audioVolumeObserver == null) {
            audioVolumeObserver = AudioVolumeObserver(requireActivity())
        }
        audioVolumeObserver?.register(AudioManager.STREAM_MUSIC, this)

        binding.volumeSeekBar.addOnChangeListener(this)

        // At the resume timing of the first application open, the MusicPlayerRemote.getVolume() cannot be called, because this returns null.
        // So I added this callback to wait for the MusicPlayerRemote.getVolume() to be initialized.
        (activity as AbsMusicServiceActivity).addOnServiceConnectedCallback {
            val systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val mediaPlayerVolume = MusicPlayerRemote.getVolume()
            binding.maxVolumeValue.text = String.format(Locale.getDefault(), "%.02f", systemVolume.toDouble())
            binding.minVolumeValue.text = String.format(Locale.getDefault(), "%.02f", 0.0)
            binding.currentVolumeValue.text = String.format(Locale.getDefault(), "%.02f", mediaPlayerVolume*systemVolume)
            binding.volumeSeekBar.value = mediaPlayerVolume
        }
    }

    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        if (_binding != null) {
            val mediaPlayerVolume = MusicPlayerRemote.getVolume()
            binding.volumeSeekBar.value = mediaPlayerVolume
            binding.volumeDown.setImageResource(if (currentVolume == 0) R.drawable.ic_volume_off else R.drawable.ic_volume_down)

            binding.maxVolumeValue.text = String.format(Locale.getDefault(), "%.02f", currentVolume.toDouble())
            binding.currentVolumeValue.text = String.format(Locale.getDefault(), "%.02f", mediaPlayerVolume*currentVolume)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioVolumeObserver?.unregister()
        _binding = null
    }

    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        MusicPlayerRemote.setVolume(value)
        binding.currentVolumeValue.text = String.format(Locale.getDefault(), "%.02f", value*audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
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

    fun setVolume(volume: Float){
        binding.volumeSeekBar.value = volume
    }

    companion object {
        fun newInstance(): VolumeFragment {
            return VolumeFragment()
        }
    }
}
