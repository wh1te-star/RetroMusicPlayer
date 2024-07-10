package code.name.monkey.retromusic.fragments

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import code.name.monkey.retromusic.R


class GForceMeterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_g_force_meter, container, false)

        val circleContainer = view.findViewById<ConstraintLayout>(R.id.circleContainer)

        // Create a custom view to draw circles
        val circleView = object : View(requireContext()) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE // Outline color
                style = Paint.Style.STROKE
                strokeWidth = 5f // Set stroke width as needed
            }

            override fun onDraw(canvas: Canvas?) {
                super.onDraw(canvas)
                val roundMargin = 2
                val halfWidth = width.toFloat()/2 - roundMargin
                val radii = listOf(halfWidth * 1/4f, halfWidth * 2/4f, halfWidth * 3/4f, halfWidth)

                canvas?.let {
                    radii.forEach { radius ->
                        it.drawCircle(roundMargin + halfWidth, roundMargin + halfWidth, radius, paint)
                    }
                }
            }

            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
            }
        }

        circleView.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        ).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }

        circleContainer.addView(circleView)

        return view
    }
}
