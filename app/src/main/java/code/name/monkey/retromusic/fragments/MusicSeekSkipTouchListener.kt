package code.name.monkey.retromusic.fragments

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import kotlinx.coroutines.*
import kotlin.math.abs
enum class ButtonAction {
    NEXT_BUTTON,
    PREVIOUS_BUTTON,
    FROM_START_BUTTON,
    SHORT_REWIND_BUTTON,
    SHORT_FORWARD_BUTTON
}

class MusicSeekSkipTouchListener(val activity: FragmentActivity, val action: ButtonAction) :
    View.OnTouchListener {

    private var startX = 0f
    private var startY = 0f

    private val scaledTouchSlop = ViewConfiguration.get(activity).scaledTouchSlop

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val endX = event.x
                val endY = event.y
                if (isAClick(startX, endX, startY, endY)) {
                    when (action) {
                        ButtonAction.NEXT_BUTTON -> {
                            MusicPlayerRemote.playNextSong()
                        }
                        ButtonAction.PREVIOUS_BUTTON -> {
                            MusicPlayerRemote.playPreviousSong()
                        }
                        ButtonAction.FROM_START_BUTTON -> {
                            MusicPlayerRemote.seekTo(0)
                        }
                        ButtonAction.SHORT_REWIND_BUTTON -> {
                            MusicPlayerRemote.seekTo(MusicPlayerRemote.songProgressMillis - 10000)
                        }
                        ButtonAction.SHORT_FORWARD_BUTTON -> {
                            MusicPlayerRemote.seekTo(MusicPlayerRemote.songProgressMillis + 10000)
                        }
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                // Handle cancel if necessary
            }
        }
        return false
    }

    private fun isAClick(startX: Float, endX: Float, startY: Float, endY: Float): Boolean {
        val differenceX = abs(startX - endX)
        val differenceY = abs(startY - endY)
        return !(differenceX > scaledTouchSlop || differenceY > scaledTouchSlop)
    }
}
