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
package code.name.monkey.retromusic.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import code.name.monkey.retromusic.BuildConfig
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.base.AbsBaseActivity.Companion.LOCATION_PERMISSION_REQUEST
import code.name.monkey.retromusic.databinding.FragmentDriveModeBinding
import code.name.monkey.retromusic.db.toSongEntity
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.extensions.drawAboveSystemBars
import code.name.monkey.retromusic.fragments.base.AbsPlayerFragment
import code.name.monkey.retromusic.glide.BlurTransformation
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper.Callback
import code.name.monkey.retromusic.helper.PlayPauseButtonOnClickHandler
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.repository.RealRepository
import code.name.monkey.retromusic.service.GPSRecordService
import code.name.monkey.retromusic.service.MusicService
import code.name.monkey.retromusic.service.GPSRecordingListener
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File


/**
 * Created by hemanths on 2020-02-02.
 */

class DriveModeFragment : AbsPlayerFragment(R.layout.fragment_drive_mode), GPSRecordingListener, Callback {

    private lateinit var binding: FragmentDriveModeBinding
    private lateinit var gpsRecordService: GPSRecordService
    private var lastPlaybackControlsColor: Int = Color.GRAY
    private var lastDisabledPlaybackControlsColor: Int = Color.GRAY
    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper
    private val repository: RealRepository by inject()
    private lateinit var gpsRecordServiceIntent: Intent
    private var isRecordingGPS = false
    private lateinit var binder: GPSRecordService.LocalBinder

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binder = service as GPSRecordService.LocalBinder
            gpsRecordService = binder.getService()
            gpsRecordService.registerListener(this@DriveModeFragment)
        }
        override fun onServiceDisconnected(name: ComponentName) {
            gpsRecordService.unregisterListener()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDriveModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpMusicControllers()

        gpsRecordServiceIntent = Intent(requireContext(), GPSRecordService::class.java)
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this)
        lastPlaybackControlsColor = accentColor()
        binding.close?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.repeatButton?.drawAboveSystemBars()

        requireActivity().bindService(gpsRecordServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        requireActivity().startService(gpsRecordServiceIntent)

        /*
        binding.gpsValue?.isSingleLine = false
        binding.gpsValue?.text = "GPS Value\n+XXX.XXXXXXXX\n+XXX.XXXXXXXX"
        binding.gpsValue?.setOnClickListener {
            showDialogWithSingleSelectAndEditText()
        }

        val speedText = SpannableString("speed (km/h)")
        speedText.setSpan(AbsoluteSizeSpan(40), 0, speedText.length, SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
        val speedValue = SpannableString("XXX")
        speedValue.setSpan(AbsoluteSizeSpan(70), 0, speedValue.length, SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
        val finalText = TextUtils.concat(speedText, "\n", speedValue)
        binding.speedValue.isSingleLine = false
        binding.speedValue.text = finalText
         */
    }

    private fun setUpMusicControllers() {
        setUpPlayPauseFab()
        setUpPrevNext()
        setUpRepeatButton()
        setUpShuffleButton()
        setUpGPSRecordButton()
        setUpProgressSlider()
        setupFavouriteToggle()
    }

    private fun setupFavouriteToggle() {
        binding.songFavourite?.setOnClickListener {
            toggleFavorite(MusicPlayerRemote.currentSong)
        }
    }

    override fun playerToolbar(): Toolbar {
        return MaterialToolbar(requireContext())
    }

    override fun onShow() { }

    override fun onHide() { }

    override fun toolbarIconColor(): Int {
        return 100
    }

    override fun toggleFavorite(song: Song) {
        lifecycleScope.launch(Dispatchers.IO) {
            val playlist = repository.favoritePlaylist()
            val songEntity = song.toSongEntity(playlist.playListId)
            val isFavorite = repository.isSongFavorite(song.id)
            if (isFavorite) {
                repository.removeSongFromPlaylist(songEntity)
            } else {
                repository.insertSongs(listOf(song.toSongEntity(playlist.playListId)))
            }
            requireContext().sendBroadcast(Intent(MusicService.FAVORITE_STATE_CHANGED))
        }
    }

    private fun updateFavorite() {
        lifecycleScope.launch(Dispatchers.IO) {
            val isFavorite: Boolean =
                repository.isSongFavorite(MusicPlayerRemote.currentSong.id)
            withContext(Dispatchers.Main) {
                binding.songFavourite?.setImageResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            }
        }
    }

    private fun setUpProgressSlider() {
        binding.progressSlider.addOnChangeListener { _: Slider, progress: Float, fromUser: Boolean ->
            if (fromUser) {
                MusicPlayerRemote.seekTo(progress.toInt())
                onUpdateProgressViews(
                    MusicPlayerRemote.songProgressMillis,
                    MusicPlayerRemote.songDurationMillis
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
    }

    override val paletteColor: Int
        get() = 100

    override fun onColorChanged(color: MediaNotificationProcessor) { }

    override fun onFavoriteToggled() { }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
    }

    private fun setUpPrevNext() {
        binding.nextButton?.setOnClickListener { MusicPlayerRemote.playNextSong() }
        binding.previousButton?.setOnClickListener { MusicPlayerRemote.back() }
    }

    private fun setUpShuffleButton() {
        binding.shuffleButton.setOnClickListener { MusicPlayerRemote.toggleShuffleMode() }
    }

    private fun setUpGPSRecordButton() {
        binding.recordGPSButton.setOnClickListener {
            if (isRecordingGPS == false) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ),
                        LOCATION_PERMISSION_REQUEST
                    )
                } else {
                    isRecordingGPS=true
                    gpsRecordService.startRecording()
                }
            } else {
                isRecordingGPS=false
                gpsRecordService.stopRecording()
                val mostRecentFile = requireContext().getExternalFilesDir(null)?.listFiles()
                    ?.filter { it.name.matches(Regex("\\d{14}")) }
                    ?.sortedByDescending { it.name }
                    ?.firstOrNull()
                shareFile(mostRecentFile)
            }
        }
        binding.recordGPSButton.setOnLongClickListener {
            if (!isWifiConnected(requireContext())) {
                AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.not_wifi_connect_warning))
                    .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                        dialog.dismiss()
                        showFileSelectionDialog()
                    }
                    .setNegativeButton(getString(R.string.no), null)
                    .show()
            }
            else{
                showFileSelectionDialog()
            }
            true
        }
    }

    private fun setUpRepeatButton() {
        binding.repeatButton?.setOnClickListener { MusicPlayerRemote.cycleRepeatMode() }
    }

    private fun setUpPlayPauseFab() {
        binding.playPauseButton?.setOnClickListener(PlayPauseButtonOnClickHandler())
    }

    override fun onRepeatModeChanged() {
        super.onRepeatModeChanged()
        updateRepeatState()
    }

    override fun onShuffleModeChanged() {
        super.onShuffleModeChanged()
        updateShuffleState()
    }

    override fun onPlayStateChanged() {
        super.onPlayStateChanged()
        updatePlayPauseDrawableState()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updatePlayPauseDrawableState()
        updateSong()
        updateRepeatState()
        updateShuffleState()
        updateGPSRecordState()
        updateFavorite()
    }

    private fun updatePlayPauseDrawableState() {
        if (MusicPlayerRemote.isPlaying) {
            binding.playPauseButton?.setImageResource(R.drawable.ic_pause)
        } else {
            binding.playPauseButton?.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    fun updateShuffleState() {
        when (MusicPlayerRemote.shuffleMode) {
            MusicService.SHUFFLE_MODE_SHUFFLE -> binding.shuffleButton.setColorFilter(
                lastPlaybackControlsColor,
                PorterDuff.Mode.SRC_IN
            )

            else -> binding.shuffleButton.setColorFilter(
                lastDisabledPlaybackControlsColor,
                PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun updateRepeatState() {
        when (MusicPlayerRemote.repeatMode) {
            MusicService.REPEAT_MODE_ALL -> {
                binding.repeatButton?.setImageResource(R.drawable.ic_repeat)
                binding.repeatButton?.setColorFilter(
                    lastPlaybackControlsColor,
                    PorterDuff.Mode.SRC_IN
                )
            }

            MusicService.REPEAT_MODE_THIS -> {
                binding.repeatButton?.setImageResource(R.drawable.ic_repeat_one)
                binding.repeatButton?.setColorFilter(
                    lastPlaybackControlsColor,
                    PorterDuff.Mode.SRC_IN
                )
            }
        }
    }

    fun updateGPSRecordState() {
        when (isRecordingGPS) {
            true -> {
                binding.recordGPSButton.setImageResource(R.drawable.ic_gps_recording)
                binding.recordGPSButton.setColorFilter(
                    lastPlaybackControlsColor,
                    PorterDuff.Mode.SRC_IN
                )
            }
            false -> {
                binding.recordGPSButton.setImageResource(R.drawable.ic_gps_recording)
                binding.recordGPSButton.setColorFilter(
                    lastDisabledPlaybackControlsColor,
                    PorterDuff.Mode.SRC_IN
                )
                binding.gpsValue?.text = "GPS Value\n+XXX.XXXXXXXX\n+XXX.XXXXXXXX"
            }
        }
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateSong()
        updateFavorite()
    }

    override fun onFavoriteStateChanged() {
        super.onFavoriteStateChanged()
        updateFavorite()
    }

    private fun updateSong() {
        val song = MusicPlayerRemote.currentSong

        binding.songTitle.text = song.title
        binding.songText.text = song.artistName

        Glide.with(this)
            .load(RetroGlideExtension.getSongModel(song))
            .songCoverOptions(song)
            .transform(BlurTransformation.Builder(requireContext()).build())
            .into(binding.image)
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        binding.progressSlider.run {
            valueTo = total.toFloat()
            value = progress.toFloat().coerceIn(valueFrom, valueTo)
        }

        binding.songTotalTime.text = MusicUtil.getReadableDurationString(total.toLong())
        binding.songCurrentProgress.text = MusicUtil.getReadableDurationString(progress.toLong())
    }

    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo?.isConnected == true && activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
        }
    }

    fun showFileSelectionDialog() {
        val files = requireContext().getExternalFilesDir(null)?.listFiles()
            ?.filter { it.name.matches(Regex("\\d{14}")) }
            ?.sortedByDescending { it.name } ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_file_to_upload))
            .setItems(files.map { it.name }.toTypedArray()) { dialog, which ->
                dialog.dismiss()
                shareFile(files[which])
            }
            .setPositiveButton(getString(R.string.close), null)
            .show()
    }

    private fun shareFile(file: File?) {
        if (file != null) {
            val fileUri = FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.provider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share File"))
        }
    }

    fun showDialogWithSingleSelectAndEditText() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_gps_accuracy_change, null)
        val editText1 = dialogView.findViewById<EditText>(R.id.editText1)
        val editText2 = dialogView.findViewById<EditText>(R.id.editText2)
        editText1.setText(gpsRecordService.textviewMinTimeMs.toString())
        editText2.setText(gpsRecordService.textviewMinDistanceM.toString())
        builder.setView(dialogView)
            .setPositiveButton("OK") { dialog, id ->
                gpsRecordService.changeTextviewAccuracy(editText1.text.toString().toLong(), editText2.text.toString().toFloat())
            }
            .setNegativeButton("Cancel") { dialog, id ->
            }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unbindService(serviceConnection)
        requireActivity().stopService(gpsRecordServiceIntent)
    }

    override fun onRecordingStarted() {
        updateGPSRecordState()
    }

    override fun onRecordingStopped() {
        updateGPSRecordState()
    }

    override fun onFileSizeExceeded() {
        isRecordingGPS = false
        Toast.makeText(context, getString(R.string.recording_file_size_exceeds_limit), Toast.LENGTH_LONG).show()
        updateGPSRecordState()
    }

    override fun updateGPSTextView(latitude: Double, longitude: Double, altitude: Double, bearing: Float, speed: Float, provider: String?) {
        val formattedLatitude = String.format("%+013.8f", latitude)
        val formattedLongitude = String.format("%+013.8f", longitude)
        val formattedAltitude = String.format("%+013.8f", altitude)
        binding.latitudeValue?.text = "latitude\n${formattedLatitude}"
        binding.longitudeValue?.text = "longitude\n${formattedLongitude}"
        binding.altitudeValue?.text = "longitude\n${formattedAltitude}"
        binding.bearingValue?.text = "bearing\n${bearing}"
        binding.speedValue.text = "speed (km/h)\n${speed * 3.6}"
        binding.providerValue?.text = "provider\n${provider}"
    }

    override fun updateAcceleroTextView(x: Float, y: Float) {
        /*
        val formattedLatitude = String.format("%+013.8f", x)
        val formattedLongitude = String.format("%+013.8f", y)
        binding.latitudeValue?.text = "$formattedLatitude"
        binding.longitudeValue?.text = "$formattedLongitude"
         */
    }
}
