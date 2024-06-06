package code.name.monkey.retromusic.views

import android.graphics.Canvas;
import android.widget.EdgeEffect;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.RecyclerView;
import code.name.monkey.retromusic.activities.base.AbsSlidingMusicPanelActivity

class OverflowScrollRecyclerView(absSlidingMusicPanelActivity: AbsSlidingMusicPanelActivity) : RecyclerView.EdgeEffectFactory() {
    override fun createEdgeEffect(recyclerView: RecyclerView, direction: Int): EdgeEffect {
        return object : EdgeEffect(recyclerView.context) {
            private var anim: SpringAnimation? = null

            override fun onPull(deltaDistance: Float) {
                super.onPull(deltaDistance)
                handlePull(deltaDistance)
            }

            override fun onPull(deltaDistance: Float, displacement: Float) {
                super.onPull(deltaDistance, displacement)
                handlePull(deltaDistance)
            }

            private fun handlePull(deltaDistance: Float) {
                val sign = if (direction == DIRECTION_BOTTOM) -1 else 1
                val translationYDelta = sign * recyclerView.width * deltaDistance * 0.2f
                recyclerView.translationY += translationYDelta
                anim?.cancel()
            }

            override fun onRelease() {
                super.onRelease()
                if (recyclerView.translationY != 0f) {
                    anim = createAnim()
                    anim?.start()
                }
            }

            override fun onAbsorb(velocity: Int) {
                super.onAbsorb(velocity)
                val sign = if (direction == DIRECTION_BOTTOM) -1 else 1
                val translationVelocity = sign * velocity * 0.5f
                anim?.cancel()
                anim = createAnim()
                anim?.setStartVelocity(translationVelocity)
                anim?.start()
            }

            private fun createAnim(): SpringAnimation {
                return SpringAnimation(recyclerView, SpringAnimation.TRANSLATION_Y).setSpring(
                    SpringForce().setFinalPosition(0f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                        .setStiffness(SpringForce.STIFFNESS_LOW)
                )
            }

            override fun draw(canvas: Canvas): Boolean {
                return false
            }

            override fun isFinished(): Boolean {
                return anim?.isRunning != true
            }
        }
    }
}
