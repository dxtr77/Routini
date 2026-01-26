package com.dxtr.routini.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): ReorderableLazyListState {
    val scope = rememberCoroutineScope()
    return remember(lazyListState, scope) {
        ReorderableLazyListState(lazyListState, onMove, scope)
    }
}

class ReorderableLazyListState(
    val lazyListState: LazyListState,
    val onMove: (Int, Int) -> Unit,
    val scope: kotlinx.coroutines.CoroutineScope
) {
    var draggingItemIndex: MutableState<Int?> = mutableStateOf(null)
        private set

    internal var draggingItemInitialOffset: Int = 0
    internal val draggingItemOffset: MutableState<Float> = mutableStateOf(0f)
    internal val scrollOffset: MutableState<Float> = mutableStateOf(0f)

    val draggingItemKey: Any?
        get() = draggingItemIndex.value?.let { index ->
            lazyListState.layoutInfo.visibleItemsInfo.find { it.index == index }?.key
        }

    internal fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                offset.y.toInt() in item.offset..(item.offset + item.size)
            }?.let { item ->
                startDrag(item.index)
            }
    }
    
    fun startDrag(index: Int) {
         lazyListState.layoutInfo.visibleItemsInfo.find { it.index == index }?.let { item ->
            draggingItemIndex.value = index
            draggingItemInitialOffset = item.offset
            draggingItemOffset.value = 0f
        }
    }

    internal fun onDrag(change: Offset) {
        val index = draggingItemIndex.value ?: return
        val currentItem = lazyListState.layoutInfo.visibleItemsInfo.find { it.index == index } ?: return
        
        draggingItemOffset.value += change.y
        
        // Check for swap
        val startToCheck = currentItem.offset + draggingItemOffset.value
        val endToCheck = startToCheck + currentItem.size
        
        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.find { item ->
            item.index != index && 
            (startToCheck + currentItem.size / 2).toInt() in item.offset..(item.offset + item.size)
        }
        
        if (targetItem != null) {
             onMove(index, targetItem.index)
             draggingItemIndex.value = targetItem.index
             draggingItemOffset.value += (currentItem.offset - targetItem.offset)
        }
    }

    internal fun onDragEnd() {
        draggingItemIndex.value = null
        draggingItemOffset.value = 0f
    }
}

fun Modifier.reorderable(
    state: ReorderableLazyListState
): Modifier = composed {
    Modifier.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset -> state.onDragStart(offset) },
            onDrag = { change, dragAmount -> 
                change.consume()
                state.onDrag(dragAmount) 
            },
            onDragEnd = { state.onDragEnd() },
            onDragCancel = { state.onDragEnd() }
        )
    }
}

fun Modifier.draggableHandle(
    state: ReorderableLazyListState,
    index: Int
): Modifier = composed {
    Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { state.startDrag(index) },
            onDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount)
            },
            onDragEnd = { state.onDragEnd() },
            onDragCancel = { state.onDragEnd() }
        )
    }
}

fun Modifier.reorderableItem(
    state: ReorderableLazyListState,
    key: Any?
): Modifier = composed {
    val isDragging = state.draggingItemKey == key
    val offset by animateFloatAsState(if (isDragging) state.draggingItemOffset.value else 0f, label = "reorder_offset")
    
    this.zIndex(if (isDragging) 1f else 0f)
        .graphicsLayer {
            translationY = offset
            if (isDragging) {
                scaleX = 1.02f
                scaleY = 1.02f
                shadowElevation = 8f
            }
        }
}
