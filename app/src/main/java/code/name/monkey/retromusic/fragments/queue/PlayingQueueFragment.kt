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
package code.name.monkey.retromusic.fragments.queue

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.base.AbsSlidingMusicPanelActivity
import code.name.monkey.retromusic.adapter.song.PlayingQueueAdapter
import code.name.monkey.retromusic.databinding.FragmentPlayingQueueBinding
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.ThemedFastScroller
import com.google.android.material.snackbar.Snackbar
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils

class PlayingQueueFragment : AbsMainActivityFragment(R.layout.fragment_playing_queue) {

    private var _binding: FragmentPlayingQueueBinding? = null
    private val binding get() = _binding!!
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null
    private var recyclerViewSwipeManager: RecyclerViewSwipeManager? = null
    private var recyclerViewTouchActionGuardManager: RecyclerViewTouchActionGuardManager? = null
    private var playingQueueAdapter: PlayingQueueAdapter? = null
    private lateinit var linearLayoutManager: LinearLayoutManager

    private fun getUpNextAndQueueTime(): String {
        val duration = MusicPlayerRemote.getQueueDurationMillis(MusicPlayerRemote.position)
        return MusicUtil.buildInfoString(
            resources.getString(R.string.up_next),
            MusicUtil.getReadableDurationString(duration)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayingQueueBinding.bind(view)

        setupToolbar()
        setUpRecyclerView()

        checkForPadding()
        mainActivity.collapsePanel()

        val activity = activity as? AbsSlidingMusicPanelActivity
        activity?.optionButton?.fitsSystemWindows = PreferenceUtil.isFullScreenMode

        mainActivity.optionButton.show()
        mainActivity.optionButton.setImageResource(R.drawable.avd_music_note)
        mainActivity.optionButton.apply {
            setOnClickListener {
                linearLayoutManager.scrollToPositionWithOffset(MusicPlayerRemote.position + 1, 0)
            }
            accentColor()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        //yet implemented
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        //yet implemented
        return false
    }

    private fun setUpRecyclerView() {
        recyclerViewTouchActionGuardManager = RecyclerViewTouchActionGuardManager()
        recyclerViewDragDropManager = RecyclerViewDragDropManager()
        recyclerViewSwipeManager = RecyclerViewSwipeManager()

        recyclerViewSwipeManager?.onItemSwipeEventListener = object : RecyclerViewSwipeManager.OnItemSwipeEventListener {
            lateinit var songToRemove: Song
            private var positionToRemove = 0
            override fun onItemSwipeStarted(position: Int) {
                positionToRemove = position
                songToRemove = playingQueueAdapter?.dataSet?.get(position) as Song
            }
            override fun onItemSwipeFinished(position: Int, result: Int, afterSwipeReaction: Int) {
                if (result == SwipeableItemConstants.RESULT_SWIPED_LEFT || result == SwipeableItemConstants.RESULT_SWIPED_RIGHT) {
                    playingQueueAdapter?.notifyItemRemoved(positionToRemove)
                    val snackbar = Snackbar.make(binding.recyclerView,
                        getString(R.string.song_removed), Snackbar.LENGTH_LONG)
                    snackbar.setAction(R.string.snackbar_undo_button) {
                        MusicPlayerRemote.musicService?.addSong(positionToRemove, songToRemove)
                        if (MusicPlayerRemote.musicService?.position!! >= positionToRemove) {
                            MusicPlayerRemote.musicService?.position =
                                MusicPlayerRemote.musicService?.nextPosition!!
                        }
                    }
                    snackbar.show()
                }
            }
        }

        playingQueueAdapter = PlayingQueueAdapter(
            requireActivity(),
            MusicPlayerRemote.playingQueue.toMutableList(),
            MusicPlayerRemote.position,
            R.layout.item_queue
        )
        wrappedAdapter = recyclerViewDragDropManager?.createWrappedAdapter(playingQueueAdapter!!)
        wrappedAdapter = wrappedAdapter?.let { recyclerViewSwipeManager?.createWrappedAdapter(it) }

        linearLayoutManager = LinearLayoutManager(requireContext())


        binding.recyclerView.apply {
            layoutManager = linearLayoutManager
            adapter = wrappedAdapter
            itemAnimator = DraggableItemAnimator()
            recyclerViewTouchActionGuardManager?.attachRecyclerView(this)
            recyclerViewDragDropManager?.attachRecyclerView(this)
            recyclerViewSwipeManager?.attachRecyclerView(this)
        }
        linearLayoutManager.scrollToPositionWithOffset(MusicPlayerRemote.position + 1, 0)

        ThemedFastScroller.create(binding.recyclerView)
    }

    private fun checkForPadding() {
    }

    override fun onQueueChanged() {
        if (MusicPlayerRemote.playingQueue.isEmpty()) {
            findNavController().navigateUp()
            return
        }
        checkForPadding()
        updateQueue()
        updateCurrentSong()
    }

    override fun onMediaStoreChanged() {
        updateQueue()
        updateCurrentSong()
    }

    private fun updateCurrentSong() {
        binding.appBarLayout.toolbar.subtitle = getUpNextAndQueueTime()
    }

    override fun onPlayingMetaChanged() {
        updateQueuePosition()
    }

    private fun updateQueuePosition() {
        playingQueueAdapter?.setCurrent(MusicPlayerRemote.position)
        resetToCurrentPosition()
        binding.appBarLayout.toolbar.subtitle = getUpNextAndQueueTime()
    }

    private fun updateQueue() {
        playingQueueAdapter?.swapDataSet(MusicPlayerRemote.playingQueue, MusicPlayerRemote.position)
    }

    private fun resetToCurrentPosition() {
        binding.recyclerView.stopScroll()
    }

    override fun onPause() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager!!.cancelDrag()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager!!.release()
            recyclerViewDragDropManager = null
        }
        if (recyclerViewSwipeManager != null) {
            recyclerViewSwipeManager?.release()
            recyclerViewSwipeManager = null
        }
        if (wrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(wrappedAdapter)
            wrappedAdapter = null
        }
        playingQueueAdapter = null
        super.onDestroy()
    }

    private fun setupToolbar() {
        binding.appBarLayout.toolbar.subtitle = getUpNextAndQueueTime()
        binding.appBarLayout.toolbar.isTitleCentered = false
        binding.appBarLayout.pinWhenScrolled()
        binding.appBarLayout.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            setTitle(R.string.now_playing_queue)
            setTitleTextAppearance(context, R.style.ToolbarTextAppearanceNormal)
            setNavigationIcon(R.drawable.ic_arrow_back)
            ToolbarContentTintHelper.colorBackButton(this)
        }
    }
}

