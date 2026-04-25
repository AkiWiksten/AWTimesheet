package com.akiwiksten.worktime30.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

private val SCROLLBAR_THICKNESS = 4.dp
private val SCROLLBAR_PADDING = 2.dp
private val SCROLLBAR_MIN_THUMB_SIZE = 24.dp
private const val SCROLLBAR_ALPHA = 0.65f
private val DEFAULT_SCROLLBAR_STYLE = ScrollbarStyle(
    thickness = SCROLLBAR_THICKNESS,
    padding = SCROLLBAR_PADDING,
    minThumbSize = SCROLLBAR_MIN_THUMB_SIZE
)

@Composable
fun Modifier.verticalScrollbar(scrollState: ScrollState): Modifier {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SCROLLBAR_ALPHA)
    return drawScrollbar(
        orientation = Orientation.Vertical,
        progressProvider = {
            val maxValue = scrollState.maxValue.toFloat()
            if (maxValue <= 0f) {
                ScrollbarProgress.Unavailable
            } else {
                ScrollbarProgress(
                    offset = scrollState.value.toFloat(),
                    range = maxValue
                )
            }
        },
        color = color,
        style = DEFAULT_SCROLLBAR_STYLE
    )
}

@Composable
fun Modifier.lazyVerticalScrollbar(listState: LazyListState): Modifier {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SCROLLBAR_ALPHA)
    return drawScrollbar(
        orientation = Orientation.Vertical,
        progressProvider = { listState.lazyListProgress() },
        color = color,
        style = DEFAULT_SCROLLBAR_STYLE
    )
}

private fun Modifier.drawScrollbar(
    orientation: Orientation,
    progressProvider: () -> ScrollbarProgress,
    color: Color,
    style: ScrollbarStyle
): Modifier = drawWithContent {
    drawContent()

    val thicknessPx = style.thickness.toPx()
    val paddingPx = style.padding.toPx()
    val minThumbSizePx = style.minThumbSize.toPx()

    val progress = progressProvider()
    if (progress.range <= 0f) return@drawWithContent

    if (orientation == Orientation.Vertical) {
        val viewportSize = size.height
        val totalContentSize = viewportSize + progress.range
        val trackSize = (viewportSize - paddingPx * 2f).coerceAtLeast(0f)
        if (trackSize <= 0f || totalContentSize <= viewportSize) return@drawWithContent

        val rawThumb = viewportSize * viewportSize / totalContentSize
        val thumbSize = max(minThumbSizePx, rawThumb).coerceAtMost(trackSize)
        val scrollFraction = (progress.offset / progress.range).coerceIn(0f, 1f)
        val top = paddingPx + (trackSize - thumbSize) * scrollFraction
        val left = size.width - thicknessPx - paddingPx

        drawRoundRect(
            color = color,
            topLeft = Offset(x = left, y = top),
            size = Size(width = thicknessPx, height = thumbSize),
            cornerRadius = CornerRadius(x = thicknessPx, y = thicknessPx)
        )
    } else {
        val viewportSize = size.width
        val totalContentSize = viewportSize + progress.range
        val trackSize = (viewportSize - paddingPx * 2f).coerceAtLeast(0f)
        if (trackSize <= 0f || totalContentSize <= viewportSize) return@drawWithContent

        val rawThumb = viewportSize * viewportSize / totalContentSize
        val thumbSize = max(minThumbSizePx, rawThumb).coerceAtMost(trackSize)
        val scrollFraction = (progress.offset / progress.range).coerceIn(0f, 1f)
        val left = paddingPx + (trackSize - thumbSize) * scrollFraction
        val top = size.height - thicknessPx - paddingPx

        drawRoundRect(
            color = color,
            topLeft = Offset(x = left, y = top),
            size = Size(width = thumbSize, height = thicknessPx),
            cornerRadius = CornerRadius(x = thicknessPx, y = thicknessPx)
        )
    }
}

private data class ScrollbarProgress(
    val offset: Float,
    val range: Float
) {
    companion object {
        val Unavailable = ScrollbarProgress(offset = 0f, range = 0f)
    }
}

private data class ScrollbarStyle(
    val thickness: Dp,
    val padding: Dp,
    val minThumbSize: Dp
)

private fun LazyListState.lazyListProgress(): ScrollbarProgress {
    val layoutInfo = layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val viewportSize = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
    val averageItemSize = if (visibleItems.isEmpty()) {
        0f
    } else {
        visibleItems.sumOf { it.size }.toFloat() / visibleItems.size
    }
    val hasInvalidLayout = visibleItems.isEmpty() || layoutInfo.totalItemsCount == 0
    val hasInvalidDimensions = viewportSize <= 0f || averageItemSize <= 0f

    if (hasInvalidLayout || hasInvalidDimensions) {
        return ScrollbarProgress.Unavailable
    }

    val totalContentSize = averageItemSize * layoutInfo.totalItemsCount +
        layoutInfo.beforeContentPadding +
        layoutInfo.afterContentPadding
    val maxScroll = (totalContentSize - viewportSize).coerceAtLeast(0f)

    return if (maxScroll <= 0f) {
        ScrollbarProgress.Unavailable
    } else {
        val scrollOffset = (firstVisibleItemIndex * averageItemSize + firstVisibleItemScrollOffset)
            .coerceIn(0f, maxScroll)
        ScrollbarProgress(offset = scrollOffset, range = maxScroll)
    }
}
