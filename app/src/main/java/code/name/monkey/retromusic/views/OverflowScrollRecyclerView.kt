package code.name.monkey.retromusic.views

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OverflowScrollRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
    private val startOverflow: Int = 0, private val endOverflow: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    init {
        layoutManager = LinearLayoutManager(context)
        setPadding(0, startOverflow, 0, endOverflow)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        adjustScrollPosition()
    }

    private fun adjustScrollPosition() {
        val layoutManager = layoutManager as? LinearLayoutManager ?: return
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

        val firstView = layoutManager.findViewByPosition(firstVisibleItemPosition)
        val lastView = layoutManager.findViewByPosition(lastVisibleItemPosition)

        if (firstView != null && firstView.top < startOverflow) {
            scrollBy(0, firstView.top - startOverflow)
        }

        if (lastView != null && lastView.bottom > height - endOverflow) {
            scrollBy(0, lastView.bottom - (height - endOverflow))
        }
    }
}
