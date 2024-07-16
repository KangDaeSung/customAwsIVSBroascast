package com.amazon.ivs.broadcast.ui.fragments.autoconfiguration

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.amazon.ivs.broadcast.common.ConsumableLiveData
import com.amazon.ivs.broadcast.common.TIME_UNTIL_WARNING
import com.amazon.ivs.broadcast.common.launch
import com.amazonaws.ivs.broadcast.BroadcastConfiguration
import com.amazonaws.ivs.broadcast.BroadcastSession
import com.amazonaws.ivs.broadcast.BroadcastSessionTest
import dagger.hilt.android.lifecycle.HiltViewModel
import com.amazon.ivs.broadcast.CLog
import javax.inject.Inject

@HiltViewModel
class AutoConfigurationViewModel @Inject constructor() : ViewModel() {
    var rerunConfiguration = false
    var shouldTestContinue = true
    var isRanFromSettingsView = false

    val testStatus = ConsumableLiveData<BroadcastSessionTest.Status>()
    val onWarningReceived = ConsumableLiveData<Unit>()
    val testProgress = ConsumableLiveData<Int>()

    private var testSession: BroadcastSession? = null
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = Runnable {
        run {
            onWarningReceived.postConsumable(Unit)
        }
    }

    fun startTest(
        endpointUrl: String?,
        streamKey: String?,
        context: Context,
    ) = launch {
        startTimer()
        BroadcastSession(context, null, BroadcastConfiguration(), emptyArray()).apply {
            testSession = this
            recommendedVideoSettings(
                endpointUrl,
                streamKey
            ) { result ->
                launch {
                    if (!shouldTestContinue) stopTest()

                    testProgress.postConsumable((result.progress * 100).toInt())
                    CLog.d("Progress: ${(result.progress * 100).toInt()} ${result.exception}")
                    testStatus.postConsumable(result.status)
                }
            }
        }
    }

    private fun startTimer() {
        timerHandler.postDelayed(timerRunnable, TIME_UNTIL_WARNING)
    }

    fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    fun stopTest() {
        testSession?.stop()
        release()
        stopTimer()
    }

    fun release() {
        testSession?.release()
        testSession = null
    }
}
