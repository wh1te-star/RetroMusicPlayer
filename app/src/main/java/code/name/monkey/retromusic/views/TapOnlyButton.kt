package code.name.monkey.retromusic.views

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.util.logD
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TapOnlyFloatingActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private val gestureDetector: GestureDetector
    private var underlyingView: RecyclerView? = null

    init {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                performClick()
                return true
            }

            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                underlyingView?.let {
                    it.scrollBy(distanceX.toInt(), distanceY.toInt())
                    logD("taponlybutton")
                }
                return true
            }
        })
    }

    fun setUnderlyingView(view: RecyclerView) {
        this.underlyingView = view
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
}
