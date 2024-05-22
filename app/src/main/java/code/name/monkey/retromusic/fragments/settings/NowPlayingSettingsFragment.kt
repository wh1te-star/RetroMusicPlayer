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
package code.name.monkey.retromusic.fragments.settings

import android.content.Context.AUDIO_SERVICE
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import code.name.monkey.appthemehelper.common.prefs.supportv7.ATESeekBarPreference
import code.name.monkey.retromusic.*
import code.name.monkey.retromusic.util.PreferenceUtil

/**
 * @author Hemanth S (h4h13).
 */

class NowPlayingSettingsFragment : AbsSettingsFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun invalidateSettings() {
        val carouselEffect: TwoStatePreference? = findPreference(CAROUSEL_EFFECT)
        carouselEffect?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean && !App.isProVersion()) {
                showProToastAndNavigate(getString(R.string.pref_title_toggle_carousel_effect))
                return@setOnPreferenceChangeListener false
            }
            return@setOnPreferenceChangeListener true
        }
    }

    fun setMaxVolume(){
        val audioManager = requireContext().getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        val volumeWarnPreference = findPreference<Preference>("volume_warn_threshold")
        if (volumeWarnPreference!= null && volumeWarnPreference is ATESeekBarPreference) {
            volumeWarnPreference.max = maxVolume
        }
        val volumeMaxPreference = findPreference<Preference>("volume_max_value_to")
        if (volumeMaxPreference!= null && volumeMaxPreference is ATESeekBarPreference) {
            volumeMaxPreference.max = maxVolume
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_now_playing_screen)
        setMaxVolume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PreferenceUtil.registerOnSharedPreferenceChangedListener(this)
        val preference: Preference? = findPreference(ALBUM_COVER_TRANSFORM)
        preference?.setOnPreferenceChangeListener { albumPrefs, newValue ->
            setSummary(albumPrefs, newValue)
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PreferenceUtil.unregisterOnSharedPreferenceChangedListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        invalidateSettings()
        val updatedValue = sharedPreferences.getInt(VOLUME_MAX_VALUE_TO, 3)
        val preference = findPreference<Preference>(VOLUME_MAX_VALUE_TO)
        if (preference is ATESeekBarPreference) {
            preference.value = updatedValue
        }
    }
}
