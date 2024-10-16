package code.name.monkey.retromusic.views

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import code.name.monkey.retromusic.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TapOnlyFloatingActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.floatingActionButtonStyle
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private val gestureDetector: GestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            performClick()
            return false
        }
    })
    private var underlyingView: View? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    fun setUnderlyingView(view: View?){
        underlyingView = view
    }

    fun setUnderlyingView(){
        underlyingView = findUnderlyingView()
    }

    fun findUnderlyingView(): View? {
        val rootView = rootView as? ViewGroup ?: return null
        val fabLocation = IntArray(2)
        getLocationOnScreen(fabLocation)
        val fabX = fabLocation[0]
        val fabY = fabLocation[1]

        return findScrollableViewAtPosition(rootView, fabX, fabY)
    }

    private fun findScrollableViewAtPosition(viewGroup: ViewGroup, x: Int, y: Int): View? {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            val childLocation = IntArray(2)
            child.getLocationOnScreen(childLocation)
            val childX = childLocation[0]
            val childY = childLocation[1]

            if (x >= childX && x < childX + child.width &&
                y >= childY && y < childY + child.height) {
                if (child.canScrollVertically(1) || child.canScrollVertically(-1) ||
                    child.canScrollHorizontally(1) || child.canScrollHorizontally(-1)) {
                    return child
                } else if (child is ViewGroup) {
                    val result = findScrollableViewAtPosition(child, x, y)
                    if (result != null) {
                        return result
                    }
                }
            }
        }
        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        underlyingView?.onTouchEvent(event)
        return true
    }
}
