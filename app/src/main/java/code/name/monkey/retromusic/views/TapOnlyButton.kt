package code.name.monkey.retromusic.views

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TapOnlyFloatingActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private val gestureDetector: GestureDetector
    private var underlyingView: View? = null

    init {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                performClick()
                return true
            }

            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                underlyingView?.let {
                    it.scrollBy(0, distanceY.toInt())
                }
                return false // Return false to allow the event to propagate
            }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    fun setUnderlyingView(view: View?){
        underlyingView = view
    }

    fun setUnderlyingView(){
        underlyingView = findUnderlyingView()
    }

    fun findUnderlyingView(): RecyclerView? {
        val rootView = rootView as? ViewGroup ?: return null
        val fabLocation = IntArray(2)
        getLocationOnScreen(fabLocation)
        val fabX = fabLocation[0]
        val fabY = fabLocation[1]

        return findViewAtPosition(rootView, fabX, fabY)
    }

    private fun findViewAtPosition(viewGroup: ViewGroup, x: Int, y: Int): RecyclerView? {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is RecyclerView) {
                val childLocation = IntArray(2)
                child.getLocationOnScreen(childLocation)
                val childX = childLocation[0]
                val childY = childLocation[1]

                if (x >= childX && x < childX + child.width &&
                    y >= childY && y < childY + child.height) {
                    return child
                }
            } else if (child is ViewGroup) {
                val result = findViewAtPosition(child, x, y)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = gestureDetector.onTouchEvent(event)

        if (handled) {
            underlyingView?.onTouchEvent(event)
        }
        return true
    }
}
