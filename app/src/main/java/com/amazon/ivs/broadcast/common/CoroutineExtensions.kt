package com.amazon.ivs.broadcast.common

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import com.amazon.ivs.broadcast.CLog

private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

@Suppress("FunctionName")
fun <T> ConsumableSharedFlow(canReplay: Boolean = false) = MutableSharedFlow<T>(
    replay = if (canReplay) 1 else 0,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

fun <T> MutableSharedFlow<T>.emitNew(value: T) {
    if (replayCache.lastOrNull() != value) {
        tryEmit(value)
    }
}

inline fun <T> SharedFlow<T>.collectUI(fragment: Fragment, crossinline action: suspend (value: T) -> Unit) {
    fragment.launchUI {
        collect { action(it) }
    }
}

inline fun <T> SharedFlow<T>.collectUI(activity: AppCompatActivity, crossinline action: suspend (value: T) -> Unit) {
    activity.launchUI {
        collect { action(it) }
    }
}

fun launchMain(block: suspend CoroutineScope.() -> Unit) = mainScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        CLog.e(e)
    },
    block = block
)

fun Fragment.launchUI(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.() -> Unit
) = viewLifecycleOwner.lifecycleScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        CLog.e(e)
    }
) {
    repeatOnLifecycle(state = lifecycleState, block = block)
}

fun AppCompatActivity.launchUI(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.() -> Unit
) = lifecycleScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        CLog.e(e)
    }
) {
    repeatOnLifecycle(state = lifecycleState, block = block)
}

fun ViewModel.launch(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        CLog.e(e)
    },
    block = block,
)
