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
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
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
import code.name.monkey.retromusic.fragments.base.AbsPlayerControlsFragment.Companion.SLIDER_ANIMATION_TIME
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
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

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
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
        //updateRepeatState()
        //updateShuffleState()
        //updatePrevNextColor()
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
        setUpSubFragments()
        setUpPlayerToolbar()

        viewReadyListener?.invoke()

        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(this)
        playerToolbar().drawAboveSystemBars()

        hideVolumeIfAvailable()
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

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        logD("progressupdate")
        if (seekBar == null) {
            progressSlider.valueTo = total.toFloat()

            progressSlider.value =
                progress.toFloat().coerceIn(progressSlider.valueFrom, progressSlider.valueTo)
        } else {
            seekBar?.max = total

            /*
            if (isSeeking) {
                seekBar?.progress = progress
            } else {
                progressAnimator =
                    ObjectAnimator.ofInt(seekBar, "progress", progress).apply {
                        duration = SLIDER_ANIMATION_TIME
                        interpolator = LinearInterpolator()
                        start()
                    }

            }
             */
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
