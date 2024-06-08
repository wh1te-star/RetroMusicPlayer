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
package code.name.monkey.retromusic.activities.base

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.GravityCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.commit
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavInflater
import androidx.navigation.contains
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.ALBUM_COVER_STYLE
import code.name.monkey.retromusic.ALBUM_COVER_TRANSFORM
import code.name.monkey.retromusic.CAROUSEL_EFFECT
import code.name.monkey.retromusic.CIRCLE_PLAY_BUTTON
import code.name.monkey.retromusic.EXTRA_SONG_INFO
import code.name.monkey.retromusic.KEEP_SCREEN_ON
import code.name.monkey.retromusic.NOW_PLAYING_SCREEN_ID
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.SCREEN_ON_LYRICS
import code.name.monkey.retromusic.SWIPE_ANYWHERE_NOW_PLAYING
import code.name.monkey.retromusic.SWIPE_DOWN_DISMISS
import code.name.monkey.retromusic.TOGGLE_ADD_CONTROLS
import code.name.monkey.retromusic.TOGGLE_FULL_SCREEN
import code.name.monkey.retromusic.TOGGLE_VOLUME
import code.name.monkey.retromusic.activities.PermissionActivity
import code.name.monkey.retromusic.adapter.NavigationMenuAdapter
import code.name.monkey.retromusic.databinding.SlidingMusicPanelLayoutBinding
import code.name.monkey.retromusic.extensions.currentFragment
import code.name.monkey.retromusic.extensions.darkAccentColor
import code.name.monkey.retromusic.extensions.dip
import code.name.monkey.retromusic.extensions.findNavController
import code.name.monkey.retromusic.extensions.getBottomInsets
import code.name.monkey.retromusic.extensions.inflate
import code.name.monkey.retromusic.extensions.isColorLight
import code.name.monkey.retromusic.extensions.keepScreenOn
import code.name.monkey.retromusic.extensions.maybeSetScreenOn
import code.name.monkey.retromusic.extensions.peekHeightAnimate
import code.name.monkey.retromusic.extensions.setLightNavigationBar
import code.name.monkey.retromusic.extensions.setLightNavigationBarAuto
import code.name.monkey.retromusic.extensions.setLightStatusBar
import code.name.monkey.retromusic.extensions.setLightStatusBarAuto
import code.name.monkey.retromusic.extensions.setNavigationBarColorPreOreo
import code.name.monkey.retromusic.extensions.setTaskDescriptionColor
import code.name.monkey.retromusic.extensions.surfaceColor
import code.name.monkey.retromusic.extensions.whichFragment
import code.name.monkey.retromusic.fragments.LibraryViewModel
import code.name.monkey.retromusic.fragments.NowPlayingScreen
import code.name.monkey.retromusic.fragments.base.AbsPlayerFragment
import code.name.monkey.retromusic.fragments.other.MiniPlayerFragment
import code.name.monkey.retromusic.fragments.player.normal.PlayerFragment
import code.name.monkey.retromusic.fragments.queue.PlayingQueueFragment
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.CategoryInfo
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.ViewUtil
import code.name.monkey.retromusic.util.logD
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_SETTLING
import com.google.android.material.bottomsheet.BottomSheetBehavior.from
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.zip.Inflater


abstract class AbsSlidingMusicPanelActivity : AbsMusicServiceActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        val TAG: String = AbsSlidingMusicPanelActivity::class.java.simpleName
    }

    protected lateinit var navController: NavController
    protected lateinit var navInflater: NavInflater
    protected lateinit var navGraph: NavGraph
    protected lateinit var appBarConfiguration: AppBarConfiguration

    var fromNotification = false
    private var windowInsets: WindowInsetsCompat? = null
    protected val libraryViewModel by viewModel<LibraryViewModel>()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var playerFragment: AbsPlayerFragment
    private var miniPlayerFragment: MiniPlayerFragment? = null
    private var nowPlayingScreen: NowPlayingScreen? = null
    private var taskColor: Int = 0
    private var paletteColor: Int = Color.WHITE
    private var navigationBarColor = 0

    private val panelState: Int
        get() = bottomSheetBehavior.state
    private lateinit var binding: SlidingMusicPanelLayoutBinding
    private var isInOneTabMode = false

    private var navigationBarColorAnimator: ValueAnimator? = null
    private val argbEvaluator: ArgbEvaluator = ArgbEvaluator()

    lateinit var optionButton: FloatingActionButton
    private var leftButtonBottomMargin = 0
    private var optionButtonBottomMargin = 0
    private var rightButtonBottomMargin = 0

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            println("Handle back press ${bottomSheetBehavior.state}")
            if (!handleBackPress()) {
                remove()
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private val bottomSheetCallbackList by lazy {
        object : BottomSheetCallback() {

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                setMiniPlayerAlphaProgress(slideOffset)
                navigationBarColorAnimator?.cancel()
                setNavigationBarColorPreOreo(
                    argbEvaluator.evaluate(
                        slideOffset,
                        surfaceColor(),
                        navigationBarColor
                    ) as Int
                )
                val screenHeight = Resources.getSystem().displayMetrics.heightPixels
                val peekHeight = bottomSheetBehavior.peekHeight
                val statusBarHeight = getStatusBarHeight(binding.root)
                val height = screenHeight - peekHeight + statusBarHeight

                val adjustedMergin = peekHeight + height * slideOffset

                setButtonMargin(binding.menuButtonLeft,  adjustedMergin.toInt() + leftButtonBottomMargin)
                setButtonMargin(binding.optionButton,    adjustedMergin.toInt() + optionButtonBottomMargin)
                setButtonMargin(binding.menuButtonRight, adjustedMergin.toInt() + rightButtonBottomMargin)
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                onBackPressedCallback.isEnabled = newState == STATE_EXPANDED
                when (newState) {
                    STATE_EXPANDED -> {
                        onPanelExpanded()
                        if (PreferenceUtil.lyricsScreenOn && PreferenceUtil.showLyrics) {
                            keepScreenOn(true)
                        }
                    }

                    STATE_COLLAPSED -> {
                        onPanelCollapsed()
                        if ((PreferenceUtil.lyricsScreenOn && PreferenceUtil.showLyrics) || !PreferenceUtil.isScreenOnEnabled) {
                            keepScreenOn(false)
                        }
                    }

                    STATE_SETTLING, STATE_DRAGGING -> {
                        if (fromNotification) {
                            fromNotification = false
                        }
                    }

                    STATE_HIDDEN -> {
                        MusicPlayerRemote.clearQueue()
                    }

                    else -> {
                        logD("Do a flip")
                    }
                }
            }
        }
    }

    private fun getButtonMargin(){
        val density = resources.displayMetrics.density
        val leftLayoutParams = binding.menuButtonLeft?.layoutParams as ViewGroup.MarginLayoutParams
        leftButtonBottomMargin = (leftLayoutParams.bottomMargin / density).toInt()
        val optionLayoutParams = binding.optionButton?.layoutParams as ViewGroup.MarginLayoutParams
        optionButtonBottomMargin = (optionLayoutParams.bottomMargin / density).toInt()
        val rightLayoutParams = binding.menuButtonRight?.layoutParams as ViewGroup.MarginLayoutParams
        rightButtonBottomMargin = (rightLayoutParams.bottomMargin / density).toInt()
    }

    private fun setButtonMargin(button: FloatingActionButton, margin: Int) {
        val actualMargin = maxOf(margin, windowInsets.getBottomInsets())
        button.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomMargin = actualMargin
        }
    }

    fun getStatusBarHeight(view: View): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsets = view.rootWindowInsets
            windowInsets?.getInsets(WindowInsets.Type.statusBars())?.top ?: 0
        } else {
            val resourceId = view.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                view.resources.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        }
    }
    fun getBottomSheetBehavior() = bottomSheetBehavior

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasPermissions()) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
        }
        binding = SlidingMusicPanelLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.setOnApplyWindowInsetsListener { _, insets ->
            windowInsets = WindowInsetsCompat.toWindowInsetsCompat(insets)
            insets
        }
        chooseFragmentForTheme()
        setupSlidingUpPanel()
        setupBottomSheet()
        updateColor()
        if (!PreferenceUtil.materialYou) {
            binding.slidingPanel.backgroundTintList = ColorStateList.valueOf(darkAccentColor())
        }

        navigationBarColor = surfaceColor()
        setupNavigationController()

        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        optionButton = binding.optionButton

        setupMenu()
    }

    private fun setupMenu(){
        val items = listOf(
            Triple(R.drawable.avd_face, "Home", R.id.action_home),
            Triple(R.drawable.avd_queue, "Playing", R.id.action_playing),
            Triple(R.drawable.avd_playlist, "Playlists", R.id.action_playlist),
            Triple(R.drawable.avd_folder, "Folder", R.id.action_folder),
            Triple(R.drawable.avd_music_note, "Song", R.id.action_song),
            Triple(R.drawable.avd_album, "Album", R.id.action_album),
            Triple(R.drawable.avd_artist, "Artist", R.id.action_artist),
            Triple(R.drawable.avd_guitar, "Genre", R.id.action_genre),
            Triple(R.drawable.ic_search, "Search", R.id.action_search),
        )

        getButtonMargin()
        binding.menuButtonLeft.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.menuButtonRight.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        val leftDrawerInflaterRoot = layoutInflater.inflate(R.layout.fragment_left_drawer_menu, binding.leftDrawer, false)
        binding.leftDrawer.addView(leftDrawerInflaterRoot)
        leftDrawerInflaterRoot.findViewById<RecyclerView>(R.id.songInfoLeft).apply {
            layoutManager = LinearLayoutManager(this@AbsSlidingMusicPanelActivity)
            adapter = NavigationMenuAdapter(navController, binding.drawerLayout, items)
        }
        leftDrawerInflaterRoot.findViewById<RecyclerView>(R.id.navigationMenuLeft).apply {
            layoutManager = LinearLayoutManager(this@AbsSlidingMusicPanelActivity)
            adapter = NavigationMenuAdapter(navController, binding.drawerLayout, items)
        }

        val rightMenuRecyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = LinearLayoutManager(this@AbsSlidingMusicPanelActivity)
            setPadding(0, 500, 0, 0)
            adapter = NavigationMenuAdapter(navController, binding.drawerLayout, items)
        }

        binding.rightDrawer.addView(rightMenuRecyclerView)

        binding.menuButtonLeft.setImageResource(R.drawable.ic_arrow_forward)
        binding.menuButtonRight.setImageResource(R.drawable.ic_arrow_back)
    }

    protected fun setupNavigationController() {
        navController = findNavController(R.id.fragment_container)
        navInflater = navController.navInflater
        navGraph = navInflater.inflate(R.navigation.main_graph)

        val categoryInfo: CategoryInfo = PreferenceUtil.libraryCategory.first { it.visible }
        if (categoryInfo.visible) {
            if (!navGraph.contains(PreferenceUtil.lastTab)) PreferenceUtil.lastTab =
                categoryInfo.category.id
            navGraph.setStartDestination(
                if (PreferenceUtil.rememberLastTab) {
                    PreferenceUtil.lastTab.let {
                        if (it == 0) {
                            categoryInfo.category.id
                        } else {
                            it
                        }
                    }
                } else categoryInfo.category.id
            )
        }
        navController.graph = navGraph
        navigationView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == navGraph.startDestinationId) {
                currentFragment(R.id.fragment_container)?.enterTransition = null
            }
            when (destination.id) {
                R.id.action_home, R.id.action_song, R.id.action_album, R.id.action_artist, R.id.action_folder, R.id.action_playlist, R.id.action_genre, R.id.action_search -> {
                    // Save the last tab
                    if (PreferenceUtil.rememberLastTab) {
                        //saveTab(destination.id)
                    }
                }
            }
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = from(binding.slidingPanel)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallbackList)
        bottomSheetBehavior.isHideable = PreferenceUtil.swipeDownToDismiss
        bottomSheetBehavior.significantVelocityThreshold = 300
        setMiniPlayerAlphaProgress(0F)
    }

    override fun onResume() {
        super.onResume()
        PreferenceUtil.registerOnSharedPreferenceChangedListener(this)
        if (bottomSheetBehavior.state == STATE_EXPANDED) {
            setMiniPlayerAlphaProgress(1f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallbackList)
        PreferenceUtil.unregisterOnSharedPreferenceChangedListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            SWIPE_DOWN_DISMISS -> {
                bottomSheetBehavior.isHideable = PreferenceUtil.swipeDownToDismiss
            }

            TOGGLE_ADD_CONTROLS -> {
                miniPlayerFragment?.setUpButtons()
            }

            NOW_PLAYING_SCREEN_ID -> {
                chooseFragmentForTheme()
                binding.slidingPanel.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                    onServiceConnected()
                }
            }

            ALBUM_COVER_TRANSFORM, CAROUSEL_EFFECT,
            ALBUM_COVER_STYLE, TOGGLE_VOLUME, EXTRA_SONG_INFO, CIRCLE_PLAY_BUTTON,
            -> {
                chooseFragmentForTheme()
                onServiceConnected()
            }

            SWIPE_ANYWHERE_NOW_PLAYING -> {
                playerFragment.addSwipeDetector()
            }

            TOGGLE_FULL_SCREEN -> {
                recreate()
            }

            SCREEN_ON_LYRICS -> {
                keepScreenOn(bottomSheetBehavior.state == STATE_EXPANDED && PreferenceUtil.lyricsScreenOn && PreferenceUtil.showLyrics || PreferenceUtil.isScreenOnEnabled)
            }

            KEEP_SCREEN_ON -> {
                maybeSetScreenOn()
            }
        }
    }

    fun collapsePanel() {
        bottomSheetBehavior.state = STATE_COLLAPSED
    }

    fun expandPanel() {
        bottomSheetBehavior.state = STATE_EXPANDED
    }

    private fun setMiniPlayerAlphaProgress(progress: Float) {
        if (progress < 0) return
        val alpha = 1 - progress
        miniPlayerFragment?.view?.alpha = 1 - (progress / 0.2F)
        miniPlayerFragment?.view?.isGone = alpha == 0f
        binding.playerFragmentContainer.alpha = (progress - 0.2F) / 0.2F
    }

    private fun animateNavigationBarColor(color: Int) {
        if (VersionUtils.hasOreo()) return
        navigationBarColorAnimator?.cancel()
        navigationBarColorAnimator = ValueAnimator
            .ofArgb(window.navigationBarColor, color).apply {
                duration = ViewUtil.RETRO_MUSIC_ANIM_TIME.toLong()
                interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
                addUpdateListener { animation: ValueAnimator ->
                    setNavigationBarColorPreOreo(
                        animation.animatedValue as Int
                    )
                }
                start()
            }
    }

    open fun onPanelCollapsed() {
        setMiniPlayerAlphaProgress(0F)
        // restore values
        animateNavigationBarColor(surfaceColor())
        setLightStatusBarAuto()
        setLightNavigationBarAuto()
        setTaskDescriptionColor(taskColor)
        //playerFragment?.onHide()
    }

    open fun onPanelExpanded() {
        setMiniPlayerAlphaProgress(1F)
        onPaletteColorChanged()
        //playerFragment?.onShow()
    }

    private fun setupSlidingUpPanel() {
        binding.slidingPanel.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.slidingPanel.viewTreeObserver.removeOnGlobalLayoutListener(this)
                binding.slidingPanel.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
                when (panelState) {
                    STATE_EXPANDED -> onPanelExpanded()
                    STATE_COLLAPSED -> onPanelCollapsed()
                    else -> {
                        // playerFragment!!.onHide()
                    }
                }
            }
        })
    }

    val navigationView get() = binding.leftDrawer

    val slidingPanel get() = binding.slidingPanel

    override fun onServiceConnected() {
        super.onServiceConnected()
        hideBottomSheet(false)
    }

    override fun onQueueChanged() {
        super.onQueueChanged()
        // Mini player should be hidden in Playing Queue
        // it may pop up if hideBottomSheet is called
        if (currentFragment(R.id.fragment_container) !is PlayingQueueFragment) {
            hideBottomSheet(MusicPlayerRemote.playingQueue.isEmpty())
        }
    }

    private fun handleBackPress(): Boolean {
        if (panelState == STATE_EXPANDED) {
            collapsePanel()
            return true
        }
        return false
    }

    private fun onPaletteColorChanged() {
        if (panelState == STATE_EXPANDED) {
            navigationBarColor = surfaceColor()
            setTaskDescColor(paletteColor)
            val isColorLight = paletteColor.isColorLight
            setLightNavigationBar(true)
            setLightStatusBar(isColorLight)
        }
    }

    private fun setTaskDescColor(color: Int) {
        taskColor = color
        if (panelState == STATE_COLLAPSED) {
            setTaskDescriptionColor(color)
        }
    }

    private fun updateColor() {
        libraryViewModel.paletteColor.observe(this) { color ->
            this.paletteColor = color
            onPaletteColorChanged()
        }
    }

    fun hideBottomSheet(
        hide: Boolean,
        animate: Boolean = false,
    ) {
        val heightOfBar = windowInsets.getBottomInsets() + dip(R.dimen.mini_player_height)
        val heightOfBarWithTabs = heightOfBar + dip(R.dimen.bottom_nav_height)
        if (hide) {
            bottomSheetBehavior.peekHeight = -windowInsets.getBottomInsets()
            bottomSheetBehavior.state = STATE_COLLAPSED
            libraryViewModel.setFabMargin(
                this,
                0
            )
        } else {
            if (MusicPlayerRemote.playingQueue.isNotEmpty()) {
                binding.slidingPanel.elevation = 0F
                logD("Details")
                if (animate) {
                    bottomSheetBehavior.peekHeightAnimate(heightOfBar).doOnEnd {
                        binding.slidingPanel.bringToFront()
                    }
                } else {
                    bottomSheetBehavior.peekHeight = heightOfBar
                    binding.slidingPanel.bringToFront()
                }
                libraryViewModel.setFabMargin(this, dip(R.dimen.mini_player_height))
            }
        }

        setButtonMargin(binding.menuButtonLeft,  bottomSheetBehavior.peekHeight + leftButtonBottomMargin)
        setButtonMargin(binding.optionButton,    bottomSheetBehavior.peekHeight + optionButtonBottomMargin)
        setButtonMargin(binding.menuButtonRight, bottomSheetBehavior.peekHeight + rightButtonBottomMargin)
    }

    fun setAllowDragging(allowDragging: Boolean) {
        bottomSheetBehavior.isDraggable = allowDragging
        hideBottomSheet(false)
    }

    private fun chooseFragmentForTheme() {
        val fragment: AbsPlayerFragment = PlayerFragment()
        supportFragmentManager.commit {
            replace(R.id.playerFragmentContainer, fragment)
        }
        supportFragmentManager.executePendingTransactions()
        playerFragment = whichFragment(R.id.playerFragmentContainer)
        miniPlayerFragment = whichFragment<MiniPlayerFragment>(R.id.miniPlayerFragment)
        miniPlayerFragment?.view?.setOnClickListener { expandPanel() }
    }
}
