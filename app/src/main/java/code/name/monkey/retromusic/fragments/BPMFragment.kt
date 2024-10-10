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

import android.app.AlertDialog
import android.database.DataSetObserver
import android.os.Bundle
import android.renderscript.ScriptGroup.Binding
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isGone
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import code.name.monkey.appthemehelper.common.ATHToolbarActivity
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.base.AbsSlidingMusicPanelActivity
import code.name.monkey.retromusic.adapter.song.BPMAdapter
import code.name.monkey.retromusic.adapter.song.SongBPM
import code.name.monkey.retromusic.databinding.FragmentBpmBinding
import code.name.monkey.retromusic.databinding.SlidingMusicPanelLayoutBinding
import code.name.monkey.retromusic.dialogs.CreatePlaylistDialog
import code.name.monkey.retromusic.dialogs.ImportPlaylistDialog
import code.name.monkey.retromusic.extensions.uri
import code.name.monkey.retromusic.fragments.base.AbsRecyclerViewFragment
import code.name.monkey.retromusic.service.AnalysisProcessCallback
import code.name.monkey.retromusic.service.BPMAnalyzer
import code.name.monkey.retromusic.util.logD
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Comparator

class BPMFragment : AbsRecyclerViewFragment<BPMAdapter, GridLayoutManager>() {
    private var _binding: FragmentBpmBinding? = null
    private val binding get() = _binding!!
    private lateinit var menu: Menu
    private lateinit var processAllMenuItem: MenuItem

    private lateinit var originalOrderDataset: List<SongBPM>
    private var ascendingOrder = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getSongs().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                val bpmAnalyzer = BPMAnalyzer.getInstance(
                    requireContext(),
                    (activity as AbsSlidingMusicPanelActivity).bpmAnalysisCallback
                )
                val songBPMList = it.map { song ->
                    val bpm = bpmAnalyzer.getBPMValue(song.id)
                    SongBPM(song, bpm)
                }
                originalOrderDataset = songBPMList
                adapter?.swapDataSet(songBPMList)
            } else {
                adapter?.swapDataSet(listOf())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override val titleRes: Int
        get() = R.string.action_bpm

    override val emptyMessage: Int
        get() = R.string.no_songs

    override val isShuffleVisible: Boolean
        get() = true

    override fun onPrepareMenu(menu: Menu) {
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(requireActivity(), toolbar)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_bpm, menu)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            requireContext(),
            toolbar,
            menu,
            ATHToolbarActivity.getToolbarBackgroundColor(toolbar)
        )

        this.menu = menu
        this.processAllMenuItem = menu.findItem(R.id.action_analysis_bpm_all)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        val bpmAnalyzer = BPMAnalyzer.getInstance(requireContext(), (activity as AbsSlidingMusicPanelActivity).bpmAnalysisCallback)
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        when (item.itemId) {
            R.id.action_analysis_bpm_all -> {
                if(bpmAnalyzer.isRunning()){
                    bpmAnalyzer.stopAllAnalysis()
                    Toast.makeText(context, "Stopping analysis...", Toast.LENGTH_LONG).show()
                    item.title = getString(R.string.analysis_bpm_all)
                } else {
                    val dataSet = if (adapter == null) mutableListOf() else adapter!!.dataSet
                    val idList = dataSet.map { song -> song.song.id }
                    val uriList = dataSet.map { song -> song.song.uri }
                    scope.launch {
                        bpmAnalyzer.analyzeAll(idList, uriList)
                    }
                    item.title = getString(R.string.action_stop_bpm_process_all)
                }
            }
            R.id.action_sort_order_bpm_reset -> {
                resetOrder()
            }
            R.id.action_sort_order_bpm -> {
                if(ascendingOrder){
                    sortItemsByValue(false)
                    ascendingOrder = false
                    item.title = getString(R.string.sort_by_bpm_ascending)
                } else {
                    sortItemsByValue(true)
                    ascendingOrder = true
                    item.title = getString(R.string.sort_by_bpm_descending)
                }
            }
            R.id.action_delete_bpm_all -> {
                val builder = AlertDialog.Builder(context)
                builder.setMessage(getString(R.string.delete_all_bpm_value))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        CoroutineScope(Dispatchers.Main).launch {
                            bpmAnalyzer.deleteAllBPMs()
                            adapter?.notifyDataSetChanged()
                        }
                    }
                    .setNegativeButton(R.string.no) { dialog, id ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
        }
        return false
    }

    fun resetOrder() {
        if (adapter != null) {
            adapter!!.dataSet = originalOrderDataset.toMutableList()
            adapter!!.notifyDataSetChanged()
        }
    }

    override fun onShuffleClicked() {
        libraryViewModel.shuffleSongs()
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireActivity(), 1)
    }

    override fun createAdapter(): BPMAdapter {
        val dataSet = if (adapter == null) mutableListOf() else adapter!!.dataSet
        return BPMAdapter(
            requireActivity(),
            dataSet,
            R.layout.item_bpm,
        )
    }

    fun sortItemsByValue(ascending: Boolean) {
        if (adapter != null) {
            val sortedDataSet = if (ascending) {
                adapter!!.dataSet.sortedWith(compareBy(nullsLast()) { it.bpm })
            } else {
                adapter!!.dataSet.sortedWith(compareByDescending(nullsFirst()) { it.bpm })
            }
            adapter!!.dataSet = sortedDataSet.toMutableList()
            adapter!!.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Songs)
    }

    override fun onPause() {
        super.onPause()
        adapter?.actionMode?.finish()
    }

    fun onSingleProcessStart(id: Long) {
        adapter?.startProcess(id)
    }

    fun onSingleProcessFinish(id: Long) {
        adapter?.finishProcess(id)
    }

    fun onAllProcessFinish() {
        processAllMenuItem.title = getString(R.string.analysis_bpm_all)
        for (id in adapter?.isProcessing?.keys!!) {
            adapter?.finishProcess(id)
        }
    }

    companion object {
        @JvmField
        var TAG: String = BPMFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(): BPMFragment {
            return BPMFragment ()
        }
    }
}
