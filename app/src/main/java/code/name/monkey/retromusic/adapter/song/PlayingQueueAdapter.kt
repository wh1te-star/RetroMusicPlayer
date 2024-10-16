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

import android.content.res.Configuration
import android.graphics.Color
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import code.name.monkey.appthemehelper.ThemeStore.Companion.accentColor
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicPlayerRemote.isPlaying
import code.name.monkey.retromusic.helper.MusicPlayerRemote.playNextSong
import code.name.monkey.retromusic.helper.MusicPlayerRemote.removeFromQueue
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.ViewUtil
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionDefault
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem
import me.zhanghai.android.fastscroll.PopupTextProvider

class PlayingQueueAdapter(
    activity: FragmentActivity,
    dataSet: MutableList<Song>,
    private var current: Int,
    itemLayoutRes: Int,
) : SongAdapter(activity, dataSet, itemLayoutRes),
    DraggableItemAdapter<PlayingQueueAdapter.ViewHolder>,
    SwipeableItemAdapter<PlayingQueueAdapter.ViewHolder>,
    PopupTextProvider {

    private var songToRemove: Song? = null

    override fun createViewHolder(view: View): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongAdapter.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val song = dataSet[position]
        holder.time?.text = MusicUtil.getReadableDurationString(song.duration)
        if (isInQuickSelectMode){
            resetHighlight(holder)
            resetAlpha(holder)
        } else if (holder.itemViewType == HISTORY) {
            setAlpha(holder, 0.5f)
        } else if (holder.itemViewType == CURRENT) {
            setHighlight(holder)
        } else {
            resetHighlight(holder)
            resetAlpha(holder)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position < current) {
            return HISTORY
        } else if (position > current) {
            return UP_NEXT
        }
        return CURRENT
    }

    override fun loadAlbumCover(song: Song, holder: SongAdapter.ViewHolder) {
        if (holder.image == null) {
            return
        }
        Glide.with(activity)
            .load(RetroGlideExtension.getSongModel(song))
            .songCoverOptions(song)
            .into(holder.image!!)
    }

    fun swapDataSet(dataSet: List<Song>, position: Int) {
        this.dataSet = dataSet.toMutableList()
        current = position
        notifyDataSetChanged()
    }

    fun setCurrent(current: Int) {
        this.current = current
        notifyDataSetChanged()
    }

    private fun setAlpha(holder: SongAdapter.ViewHolder, alpha: Float) {
        holder.image?.alpha = alpha
        holder.title?.alpha = alpha
        holder.text?.alpha = alpha
        holder.paletteColorContainer?.alpha = alpha
        holder.dragView?.alpha = alpha
        holder.menu?.alpha = alpha
    }

    private fun resetAlpha(holder: SongAdapter.ViewHolder) {
        holder.image?.alpha = 1.0f
        holder.title?.alpha = 1.0f
        holder.text?.alpha = 1.0f
        holder.paletteColorContainer?.alpha = 1.0f
        holder.dragView?.alpha = 1.0f
        holder.menu?.alpha = 1.0f
    }

    private fun setHighlight(holder: SongAdapter.ViewHolder) {
        val color = accentColor(activity)
        val colorARGB = Color.alpha(color) shl 24 or (Color.red(color) shl 16) or (Color.green(color) shl 8) or Color.blue(color)
        val isDarkTheme = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val lighterColorARGB = if (isDarkTheme) {
            ColorUtils.blendARGB(colorARGB, Color.BLACK, 0.8f)
        } else {
            ColorUtils.blendARGB(colorARGB, Color.WHITE, 0.8f)
        }
        val lighterColor = Color.argb(Color.alpha(lighterColorARGB), Color.red(lighterColorARGB), Color.green(lighterColorARGB), Color.blue(lighterColorARGB))
        holder.currentSongColorView?.setBackgroundColor(lighterColor)
        holder.currentSongColorView?.visibility = View.VISIBLE
    }

    private fun resetHighlight(holder: SongAdapter.ViewHolder) {
        holder.currentSongColorView?.visibility = View.GONE
    }

    override fun getPopupText(position: Int): String {
        return MusicUtil.getSectionName(dataSet[position].title)
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean {
        return ViewUtil.hitTest(holder.imageText!!, x, y) || ViewUtil.hitTest(
            holder.dragView!!,
            x,
            y
        )
    }

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange? {
        return null
    }

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        MusicPlayerRemote.moveSong(fromPosition, toPosition)
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean {
        return true
    }

    override fun onItemDragStarted(position: Int) {
        notifyDataSetChanged()
    }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        notifyDataSetChanged()
    }

    fun setSongToRemove(song: Song) {
        songToRemove = song
    }

    inner class ViewHolder(itemView: View) : SongAdapter.ViewHolder(itemView) {
        @DraggableItemStateFlags
        private var mDragStateFlags: Int = 0

        override var songMenuRes: Int
            get() = R.menu.menu_item_playing_queue_song
            set(value) {
                super.songMenuRes = value
            }

        init {
            dragView?.isVisible = true
        }

        override fun onClick(v: View?) {
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
                notifyDataSetChanged()
            } else {
                MusicPlayerRemote.playSongAt(layoutPosition)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            notifyDataSetChanged()
            return super.onLongClick(v)
        }

        override fun onSongMenuItemClick(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_remove_from_playing_queue -> {
                    removeFromQueue(layoutPosition)
                    return true
                }
            }
            return super.onSongMenuItemClick(item)
        }

        @DraggableItemStateFlags
        override fun getDragStateFlags(): Int {
            return mDragStateFlags
        }

        override fun setDragStateFlags(@DraggableItemStateFlags flags: Int) {
            mDragStateFlags = flags
        }

        override fun getSwipeableContainerView(): View {
            return dummyContainer!!
        }
    }

    companion object {

        private const val HISTORY = 0
        private const val CURRENT = 1
        private const val UP_NEXT = 2
    }

    override fun onSwipeItem(holder: ViewHolder, position: Int, result: Int): SwipeResultAction {
        return if (result == SwipeableItemConstants.RESULT_CANCELED) {
            SwipeResultActionDefault()
        } else {
            SwipedResultActionRemoveItem(this, position, activity)
        }
    }

    override fun onGetSwipeReactionType(holder: ViewHolder, position: Int, x: Int, y: Int): Int {
        return if (onCheckCanStartDrag(holder, position, x, y)) {
            SwipeableItemConstants.REACTION_CAN_NOT_SWIPE_BOTH_H
        } else {
            SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H
        }
    }

    override fun onSwipeItemStarted(holder: ViewHolder, p1: Int) {
    }

    override fun onSetSwipeBackground(holder: ViewHolder, position: Int, result: Int) {
    }

    internal class SwipedResultActionRemoveItem(
        private val adapter: PlayingQueueAdapter,
        private val position: Int,
        private val activity: FragmentActivity
    ) : SwipeResultActionRemoveItem() {

        private val bottomPadding = 330
        private var songToRemove: Song? = null
        override fun onPerformAction() {
            // currentlyShownSnackbar = null
        }

        override fun onSlideAnimationEnd() {
            initializeSnackBar(position, activity)
            songToRemove = adapter.dataSet[position]
            // If song removed was the playing song, then play the next song
            if (isPlaying(songToRemove!!)) {
                playNextSong()
            }

            if (adapter.isChecked(songToRemove!!))
                adapter.toggleChecked(position)

            // Swipe animation is much smoother when we do the heavy lifting after it's completed
            adapter.setSongToRemove(songToRemove!!)
            removeFromQueue(position)
        }

        fun initializeSnackBar(position: Int, activity: FragmentActivity) {
            val view = activity.findViewById<FrameLayout>(R.id.slidingPanel)
            val snackbar = Snackbar.make(view, activity.getString(R.string.song_removed), Snackbar.LENGTH_LONG)
            snackbar.setAction(activity.getString(R.string.history_undo_button)) {
                MusicPlayerRemote.musicService?.addSong(position, songToRemove!!)
                if (MusicPlayerRemote.musicService?.position!! >= position) {
                    MusicPlayerRemote.musicService?.position = MusicPlayerRemote.musicService?.nextPosition!!
                }
            }
            snackbar.view.translationY -= bottomPadding
            snackbar.show()
        }
    }
}