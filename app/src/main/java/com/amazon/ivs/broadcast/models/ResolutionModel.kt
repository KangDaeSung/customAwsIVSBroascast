package com.amazon.ivs.broadcast.models

data class ResolutionModel(
    var initialWidth: Float,
    var initialHeight: Float,
    var orientation: Int = Orientation.PORTRAIT.id,
    var isLandscape: Boolean = false
) {
    private val isWidthLonger get() = initialWidth > initialHeight

    val widthAgainstHeightRatio get() = width / height
    val width: Float
        get() {
            val result = when (orientation) {
                Orientation.LANDSCAPE.id -> if (isWidthLonger) initialWidth else initialHeight
                Orientation.PORTRAIT.id -> if (isWidthLonger) initialHeight else initialWidth
                Orientation.SQUARE.id -> if (isWidthLonger) initialHeight else initialWidth
                else -> {
                    if (isWidthLonger) initialWidth else initialHeight
                }
            }
            return result
        }
    val height: Float
        get() {
            return when (orientation) {
                Orientation.LANDSCAPE.id -> if (isWidthLonger) initialHeight else initialWidth
                Orientation.PORTRAIT.id -> if (isWidthLonger) initialWidth else initialHeight
                Orientation.SQUARE.id -> if (isWidthLonger) initialHeight else initialWidth
                else -> {
                    if (isWidthLonger) initialHeight else initialWidth
                }
            }
        }
}
