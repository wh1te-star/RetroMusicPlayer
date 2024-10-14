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
package code.name.monkey.retromusic.adapter.song

import android.content.res.ColorStateList
import android.content.res.Resources
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import code.name.monkey.retromusic.EXTRA_ALBUM_ID
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.base.AbsMultiSelectAdapter
import code.name.monkey.retromusic.adapter.base.MediaEntryViewHolder
import code.name.monkey.retromusic.db.SongAnalysisDao
import code.name.monkey.retromusic.extensions.uri
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.glide.RetroMusicColoredTarget
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.SortOrder
import code.name.monkey.retromusic.helper.menu.SongMenuHelper
import code.name.monkey.retromusic.helper.menu.SongsMenuHelper
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.service.BPMAnalyzer
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.RetroUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.koin.android.ext.android.inject
import java.text.DecimalFormat

/**
 * Created by hemanths on 13/08/17.
 */

open class BPMAdapter(
    override val activity: FragmentActivity,
    var dataSet: MutableList<SongBPM>,
    protected var itemLayoutRes: Int,
    showSectionName: Boolean = true,
) : AbsMultiSelectAdapter<BPMAdapter.ViewHolder, SongBPM>(
    activity,
    R.menu.menu_media_selection,
), PopupTextProvider {

    private var showSectionName = showSectionName

    init {
        this.setHasStableIds(true)
    }

    open fun swapDataSet(dataSet: List<SongBPM>) {
        this.dataSet = ArrayList(dataSet)
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].song.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = try {
            LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        } catch (e: Resources.NotFoundException) {
            LayoutInflater.from(activity).inflate(R.layout.item_list, parent, false)
        }
        return createViewHolder(view)
    }

    protected open fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = dataSet[position]
        val isChecked = isChecked(song)
        holder.itemView.isActivated = isChecked
        holder.menu?.isGone = isChecked
        holder.title?.text = getSongTitle(song.song)
        holder.text?.text = getSongText(song.song)
        holder.text2?.text = getSongText2(song.song)

        if (BPMAnalyzer.isRunning(song.song.id)) {
            holder.bpmValue?.isGone = true
            holder.analysisIndicator?.isGone = false
        } else {
            holder.bpmValue?.isGone = false
            holder.analysisIndicator?.isGone = true
            CoroutineScope(Dispatchers.IO).launch {
                val bpm = BPMAnalyzer.getAnalyzedValue(song.song.id, "bpm")
                val manualBPM = BPMAnalyzer.getAnalyzedValue(song.song.id, "manualBPM")
                withContext(Dispatchers.Main) {
                    val decimalFormat = DecimalFormat("000.0")
                    val formattedBpm = bpm?.let { decimalFormat.format(it) } ?: "N/A"
                    val spannableString = SpannableString(formattedBpm)
                    if (manualBPM != null) {
                        spannableString.setSpan(UnderlineSpan(), 0, spannableString.length, 0)
                    }
                    holder.bpmValue?.text = spannableString
                }
            }
        }

        loadAlbumCover(song.song, holder)
        val landscape = RetroUtil.isLandscape
        if ((PreferenceUtil.songGridSize > 2 && !landscape) || (PreferenceUtil.songGridSizeLand > 5 && landscape)) {
            holder.menu?.isVisible = false
        }

        holder.menu?.invalidate()
    }

    private fun setColors(color: MediaNotificationProcessor, holder: ViewHolder) {
        holder.title?.setTextColor(color.primaryTextColor)
        holder.text?.setTextColor(color.secondaryTextColor)
        holder.paletteColorContainer?.setBackgroundColor(color.backgroundColor)
        holder.menu?.imageTintList = ColorStateList.valueOf(color.primaryTextColor)
        holder.mask?.backgroundTintList = ColorStateList.valueOf(color.primaryTextColor)
    }

    protected open fun loadAlbumCover(song: Song, holder: ViewHolder) {
        holder.image?.let {
            Glide.with(activity)
                .asBitmapPalette()
                .songCoverOptions(song)
                .load(RetroGlideExtension.getSongModel(song))
                .into(object : RetroMusicColoredTarget(it) {
                    override fun onColorReady(colors: MediaNotificationProcessor) {
                        setColors(colors, holder)
                    }
                })
        }
    }

    private fun getSongTitle(song: Song): String {
        return song.title
    }

    private fun getSongText(song: Song): String {
        return song.artistName
    }

    private fun getSongText2(song: Song): String {
        return song.albumName
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): SongBPM? {
        return dataSet[position]
    }

    override fun getName(model: SongBPM): String {
        return model.song.title
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selectionBPM: List<SongBPM>) {
        val selection = selectionBPM.map { it.song }
        SongsMenuHelper.handleMenuClick(activity, selection, menuItem.itemId)
    }

    override fun getPopupText(position: Int): String {
        val sectionName: String? = when (PreferenceUtil.songSortOrder) {
            SortOrder.SongSortOrder.SONG_DEFAULT -> return MusicUtil.getSectionName(
                dataSet[position].song.title,
                true
            )
            SortOrder.SongSortOrder.SONG_A_Z, SortOrder.SongSortOrder.SONG_Z_A -> dataSet[position].song.title
            SortOrder.SongSortOrder.SONG_ALBUM -> dataSet[position].song.albumName
            SortOrder.SongSortOrder.SONG_ARTIST -> dataSet[position].song.artistName
            SortOrder.SongSortOrder.SONG_YEAR -> return MusicUtil.getYearString(dataSet[position].song.year)
            SortOrder.SongSortOrder.COMPOSER -> dataSet[position].song.composer
            SortOrder.SongSortOrder.SONG_ALBUM_ARTIST -> dataSet[position].song.albumArtist
            else -> {
                return ""
            }
        }
        return MusicUtil.getSectionName(sectionName)
    }

    fun startProcess(id: Long) {
        notifyDataSetChanged()
    }

    fun finishProcess(id: Long) {
        notifyDataSetChanged()
    }

    open inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        protected open var songMenuRes = R.menu.menu_item_bpm
        protected open val song: Song
            get() = dataSet[layoutPosition].song

        val currentSongColorView: View? = itemView.findViewById(R.id.currentSongColorView)
        val bpmValue: TextView? = itemView.findViewById(R.id.bpmValue)
        val analysisIndicator: LinearProgressIndicator? = itemView.findViewById(R.id.bpmProcessingIndicator)

        init {
            menu?.setOnClickListener(object : SongMenuHelper.OnClickSongMenu(activity) {
                override val song: Song
                    get() = this@ViewHolder.song

                override val menuRes: Int
                    get() = songMenuRes

                override fun onMenuItemClick(item: MenuItem): Boolean {
                    return onSongMenuItemClick(item) || super.onMenuItemClick(item)
                }
            })
        }

        protected open fun onSongMenuItemClick(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_analyze_bpm -> {
                    if (BPMAnalyzer.isRunning(song.id)) {
                        BPMAnalyzer.stopAnalysis(song.id)
                        Toast.makeText(activity, activity.getString(R.string.stop_analysis), Toast.LENGTH_LONG).show()
                    } else {
                        BPMAnalyzer.analyzeBPM(activity, song.id, song.uri, CoroutineScope(Dispatchers.IO))
                    }
                    return true
                }
            }
            return false
        }

        override fun onClick(v: View?) {
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
            } else {
                val item = dataSet[layoutPosition].song
                MusicPlayerRemote.playNext(item)
                MusicPlayerRemote.playNextSong()
            }
        }

        override fun onLongClick(v: View?): Boolean {
            return toggleChecked(layoutPosition)
        }
    }

    companion object {
        val TAG: String = BPMAdapter::class.java.simpleName
    }
}

data class SongBPM(val song: Song, val bpm: Double?)