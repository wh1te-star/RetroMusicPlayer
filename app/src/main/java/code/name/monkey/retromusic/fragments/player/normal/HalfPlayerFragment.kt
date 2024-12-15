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
package code.name.monkey.retromusic.fragments.player.normal

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import code.name.monkey.appthemehelper.util.ATHUtil
import code.name.monkey.appthemehelper.util.MaterialValueHelper
import code.name.monkey.appthemehelper.util.TintHelper
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.SNOWFALL
import code.name.monkey.retromusic.databinding.FragmentHalfPlayerBinding
import code.name.monkey.retromusic.extensions.*
import code.name.monkey.retromusic.fragments.base.AbsPlayerFragment
import code.name.monkey.retromusic.fragments.other.VolumeFragment
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.retromusic.fragments.MusicSeekSkipTouchListener
import code.name.monkey.retromusic.fragments.base.AbsPlayerControlsFragment.Companion.SLIDER_ANIMATION_TIME
import code.name.monkey.retromusic.fragments.other.VolumeViewModel
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
import code.name.monkey.retromusic.service.MusicService
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.ViewUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import code.name.monkey.retromusic.util.logD
import code.name.monkey.retromusic.views.DrawableGradient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider

class HalfPlayerFragment : AbsPlayerFragment(R.layout.fragment_half_player),
    SharedPreferences.OnSharedPreferenceChangeListener, MusicProgressViewUpdateHelper.Callback {

    private var lastColor: Int = 0
    override val paletteColor: Int
        get() = lastColor

    private var valueAnimator: ValueAnimator? = null

    private var _binding: FragmentHalfPlayerBinding? = null
    private val binding get() = _binding!!
    private var viewReadyListener: (() -> Unit)? = null

    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    val seekBar: SeekBar? = null
    val progressSlider: Slider
        get() = binding.progressSlider
    private var isSeeking = false

    val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    val songTotalTime: TextView
        get() = binding.songTotalTime

    val repeatButton: ImageButton
        get() = binding.repeatButton

    val previousButton: ImageButton
        get() = binding.previousButton

    val playPauseButton: FloatingActionButton
        get() = binding.playPauseButton

    val nextButton: ImageButton
        get() = binding.nextButton

    val shuffleButton: ImageButton
        get() = binding.shuffleButton

    var volumeFragment: VolumeFragment? = null
    private lateinit var volumeViewModel: VolumeViewModel

    fun setOnViewReadyListener(listener: () -> Unit) {
        viewReadyListener = listener
    }

    fun setHiddenAreaHeight(expandRatio: Float) {
        val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics
        val displayHeight = displayMetrics.heightPixels
        val newHeight = (displayHeight * (1 - expandRatio)).toInt()
        binding.hiddenAreaView.updateLayoutParams<ViewGroup.LayoutParams> {
            height = newHeight + dip(R.dimen.mini_player_height)
        }
        binding.hiddenAreaView.requestLayout()
    }

    private fun colorize(i: Int) {
        if (valueAnimator != null) {
            valueAnimator?.cancel()
        }

        valueAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            surfaceColor(),
            i
        )
        valueAnimator?.addUpdateListener { animation ->
            if (isAdded) {
                val drawable = DrawableGradient(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        animation.animatedValue as Int,
                        surfaceColor()
                    ), 0
                )
            }
        }
        valueAnimator?.setDuration(ViewUtil.RETRO_MUSIC_ANIM_TIME.toLong())?.start()
    }

    private fun onProgressChange(value: Int, fromUser: Boolean) {
        if (fromUser) {
            onUpdateProgressViews(value, MusicPlayerRemote.songDurationMillis)
        }
    }

    private fun setUpProgressSlider() {
        progressSlider.addOnChangeListener(Slider.OnChangeListener { _, value, fromUser ->
            onProgressChange(value.toInt(), fromUser)
        })
        progressSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                logD("onStartTrackingTouch")
                onStartTrackingTouch()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                onStopTrackingTouch(slider.value.toInt())
            }
        })

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onProgressChange(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                onStartTrackingTouch()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                onStopTrackingTouch(seekBar?.progress ?: 0)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        setUpProgressSlider()
        progressViewUpdateHelper.start()
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
    }

    private fun onStartTrackingTouch() {
        isSeeking = true
        progressViewUpdateHelper.stop()
        //progressAnimator?.cancel()
    }

    private fun onStopTrackingTouch(value: Int) {
        isSeeking = false
        MusicPlayerRemote.seekTo(value)
        progressViewUpdateHelper.start()
    }

    override fun onShow() { }

    override fun onHide() { }

    override fun toolbarIconColor() = colorControlNormal()

    override fun onColorChanged(color: MediaNotificationProcessor) {
        setColor(color)
        lastColor = color.backgroundColor
        libraryViewModel.updateColor(color.backgroundColor)

        ToolbarContentTintHelper.colorizeToolbar(
            binding.playerToolbar,
            colorControlNormal(),
            requireActivity()
        )

        if (PreferenceUtil.isAdaptiveColor) {
            colorize(color.backgroundColor)
        }
    }

    fun setColor(color: MediaNotificationProcessor) {
        val colorBg = ATHUtil.resolveColor(requireContext(), android.R.attr.colorBackground)
        if (ColorUtil.isColorLight(colorBg)) {
            lastColor = MaterialValueHelper.getSecondaryTextColor(requireContext(), true)
        } else {
            lastColor = MaterialValueHelper.getPrimaryTextColor(requireContext(), false)
        }

        val colorFinal = if (PreferenceUtil.isAdaptiveColor) {
            color.primaryTextColor
        } else {
            accentColor()
        }.ripAlpha()

        TintHelper.setTintAuto(
            binding.playPauseButton,
            MaterialValueHelper.getPrimaryTextColor(
                requireContext(),
                ColorUtil.isColorLight(colorFinal)
            ),
            false
        )
        TintHelper.setTintAuto(binding.playPauseButton, colorFinal, true)
        binding.progressSlider.applyColor(colorFinal)
        (binding.volumeFragmentContainer as VolumeFragment).setTintable(colorFinal)
        updateRepeatState()
        updateShuffleState()
        updatePrevNextColor()
    }

    override fun toggleFavorite(song: Song) {
        super.toggleFavorite(song)
        if (song.id == MusicPlayerRemote.currentSong.id) {
            updateIsFavorite()
        }
    }

    override fun onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.currentSong)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHalfPlayerBinding.bind(view)

        lastColor = 100000000
        setUpSubFragments()
        setUpProgressSlider()
        setUpPlayPauseFab()
        setUpPrevNext()
        setUpRepeatButton()
        setUpShuffleButton()
        setUpPlayerToolbar()
        updatePlayPauseDrawableState()

        viewReadyListener?.invoke()

        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(this)
        playerToolbar().drawAboveSystemBars()

        hideVolumeIfAvailable()

        volumeViewModel = ViewModelProvider(requireActivity()).get(VolumeViewModel::class.java)
        volumeViewModel.volume.observe(viewLifecycleOwner, { volume ->
            volumeFragment?.setVolume(volume)
            logD("view model")
        })
    }

    private fun hideVolumeIfAvailable() {
        if (PreferenceUtil.isVolumeVisibilityMode) {
            childFragmentManager.commit {
                replace<VolumeFragment>(R.id.volumeFragmentContainer)
            }
            childFragmentManager.executePendingTransactions()
        }
        volumeFragment = whichFragment(R.id.volumeFragmentContainer)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(this)
        _binding = null
    }

    private fun setUpSubFragments() {
        //controlsFragment = whichFragment(R.id.playbackControlsFragment)
    }

    private fun setUpPlayPauseFab() {
        binding.playPauseButton.setOnClickListener {
            if (MusicPlayerRemote.isPlaying) {
                MusicPlayerRemote.pauseSong()
            } else {
                MusicPlayerRemote.resumePlaying()
            }
            updatePlayPauseDrawableState()
            it.showBounceAnimation()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpPrevNext() {
        nextButton?.setOnTouchListener(MusicSeekSkipTouchListener(requireActivity(), true))
        previousButton?.setOnTouchListener(MusicSeekSkipTouchListener(requireActivity(), false))
    }

    private fun setUpShuffleButton() {
        shuffleButton.setOnClickListener {
            MusicPlayerRemote.toggleShuffleMode()
            updateShuffleState()
        }
    }

    private fun setUpRepeatButton() {
        repeatButton.setOnClickListener {
            MusicPlayerRemote.cycleRepeatMode()
            updateRepeatState()
        }
    }

    private fun updatePlayPauseDrawableState() {
        if (MusicPlayerRemote.isPlaying) {
            binding.playPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.playPauseButton.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    fun updatePrevNextColor() {
        nextButton?.setColorFilter(lastColor, PorterDuff.Mode.SRC_IN)
        previousButton?.setColorFilter(lastColor, PorterDuff.Mode.SRC_IN)
    }

    fun updateShuffleState() {
        val colorBg = ATHUtil.resolveColor(requireContext(), android.R.attr.colorBackground)
        val lastPlaybackControlsColor = if(ColorUtil.isColorLight(colorBg))
            MaterialValueHelper.getSecondaryTextColor(requireContext(), true)
            else
            MaterialValueHelper.getPrimaryTextColor(requireContext(), false)
        val lastDisabledPlaybackControlsColor = if(ColorUtil.isColorLight(colorBg))
            MaterialValueHelper.getSecondaryDisabledTextColor(requireContext(), true)
            else
            MaterialValueHelper.getPrimaryDisabledTextColor(requireContext(), false)
        shuffleButton.setColorFilter(
            when (MusicPlayerRemote.shuffleMode) {
                MusicService.SHUFFLE_MODE_SHUFFLE -> lastPlaybackControlsColor
                else -> lastDisabledPlaybackControlsColor
            }, PorterDuff.Mode.SRC_IN
        )
    }

    fun updateRepeatState() {
        when (MusicPlayerRemote.repeatMode) {
            MusicService.REPEAT_MODE_ALL -> {
                repeatButton.setImageResource(R.drawable.ic_repeat)
            }
            MusicService.REPEAT_MODE_THIS -> {
                repeatButton.setImageResource(R.drawable.ic_repeat_one)
            }
        }
    }

    private fun setUpPlayerToolbar() {
        binding.playerToolbar.inflateMenu(R.menu.menu_player)
        //binding.playerToolbar.menu.setUpWithIcons()
        binding.playerToolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.playerToolbar.setOnMenuItemClickListener(this)

        ToolbarContentTintHelper.colorizeToolbar(
            binding.playerToolbar,
            colorControlNormal(),
            requireActivity()
        )
    }

    fun View.showBounceAnimation() {
        clearAnimation()
        scaleX = 0.9f
        scaleY = 0.9f
        isVisible = true
        pivotX = (width / 2).toFloat()
        pivotY = (height / 2).toFloat()

        animate().setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .scaleX(1.1f)
            .scaleY(1.1f)
            .withEndAction {
                animate().setDuration(200)
                    .setInterpolator(AccelerateInterpolator())
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .start()
            }
            .start()
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        if (seekBar == null) {
            progressSlider.valueTo = total.toFloat()

            progressSlider.value =
                progress.toFloat().coerceIn(progressSlider.valueFrom, progressSlider.valueTo)
        } else {
            seekBar?.max = total

            if (isSeeking) {
                seekBar?.progress = progress
            } else {
                /*
                progressAnimator =
                    ObjectAnimator.ofInt(seekBar, "progress", progress).apply {
                        duration = SLIDER_ANIMATION_TIME
                        interpolator = LinearInterpolator()
                        start()
                    }
                 */

            }
        }
        songTotalTime.text = MusicUtil.getReadableDurationString(total.toLong())
        songCurrentProgress.text = MusicUtil.getReadableDurationString(progress.toLong())
    }

    override fun onServiceConnected() {
        updateIsFavorite()
    }

    override fun onPlayingMetaChanged() {
        updateIsFavorite()
    }

    override fun playerToolbar(): Toolbar {
        return binding.playerToolbar
    }

    companion object {

        fun newInstance(): HalfPlayerFragment {
            return HalfPlayerFragment()
        }
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        //nothing
    }
}
