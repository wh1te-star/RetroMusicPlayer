package code.name.monkey.retromusic.extensions

import androidx.core.view.WindowInsetsCompat
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.RetroUtil

fun WindowInsetsCompat?.getBottomInsets(): Int {
    return if (PreferenceUtil.isFullScreenMode) {
        return 0
    } else {
        this?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: RetroUtil.navigationBarHeight
    }
}

fun WindowInsetsCompat?.getTopInsets(): Int {
    return if (PreferenceUtil.isFullScreenMode) {
        0
    } else {
        val systemBarsInsets = this?.getInsets(WindowInsetsCompat.Type.systemBars())?.top ?: 0
        val displayCutoutInsets = this?.getInsets(WindowInsetsCompat.Type.displayCutout())?.top ?: 0
        maxOf(systemBarsInsets, displayCutoutInsets)
    }
}
