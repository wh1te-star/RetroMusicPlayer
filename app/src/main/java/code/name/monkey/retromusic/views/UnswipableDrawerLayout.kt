package code.name.monkey.retromusic.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import code.name.monkey.retromusic.util.logD

class UnswipableDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DrawerLayout(context, attrs, defStyleAttr) {

    var isSwipeOpenEnabled: Boolean = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isSwipeOpenEnabled) {
            if ((isDrawerOpen(GravityCompat.START) && !isTouchInsideDrawer(ev, GravityCompat.START)) ||
                (isDrawerOpen(GravityCompat.END) && !isTouchInsideDrawer(ev, GravityCompat.END))) {
                closeDrawer(if (isDrawerOpen(GravityCompat.START)) GravityCompat.START else GravityCompat.END)
                return true
            }
            return false
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun isTouchInsideDrawer(ev: MotionEvent, gravity: Int): Boolean {
        val drawerView = getDrawerView(gravity)
        if (drawerView != null) {
            val drawerWidth = drawerView.width
            val drawerLeft = if (gravity == GravityCompat.START) 0 else width - drawerWidth
            logD("isTouchInsideDrawer: drawerWidth = $drawerWidth, ev.x = ${ev.x}, drawerLeft = $drawerLeft")
            return ev.x >= drawerLeft && ev.x <= drawerLeft + drawerWidth
        }
        return false
    }

    private fun getDrawerView(gravity: Int): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as DrawerLayout.LayoutParams
            if (lp.gravity == gravity) {
                return child
            }
        }
        return null
    }
}
