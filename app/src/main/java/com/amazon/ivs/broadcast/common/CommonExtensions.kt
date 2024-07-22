package com.amazon.ivs.broadcast.common

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.net.TrafficStats
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.navigation.findNavController
import com.amazon.ivs.broadcast.R
import com.amazon.ivs.broadcast.common.LiveConfig.BYTES_TO_MEGABYTES_FACTOR
import com.amazon.ivs.broadcast.common.LiveConfig.CPU_TEMP_PATHS
import com.amazon.ivs.broadcast.common.LiveConfig.DISABLE_DURATION
import com.amazonaws.ivs.broadcast.BroadcastConfiguration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.delay
import java.io.RandomAccessFile

fun AppCompatActivity.openFragment(id: Int) {
    findNavController(R.id.nav_host_fragment).navigate(id)
}

fun AppCompatActivity.getCurrentFragment() =
    supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.firstOrNull()

fun View.setVisible(isVisible: Boolean = true, hideOption: Int = View.GONE) {
    visibility = if (isVisible) View.VISIBLE else hideOption
}

fun View.onDrawn(onDrawn: () -> Unit) {
    invalidate()
    requestLayout()
    doOnLayout { onDrawn() }
}

fun Context.getCpuTemperature(): String {
    var temp = 0f
    var count = 0
    CPU_TEMP_PATHS.forEach { file ->
        try {
            val reader = RandomAccessFile(file, "r")
            temp += reader.readLine().toFloat()
            count++
        } catch (e: Exception) {
            /* Ignored */
        }
    }
    val averageTemp = (temp / count / 1000.0f).toInt()
    return if (averageTemp <= 0) resources.getString(R.string.not_available_short) else resources.getString(
        R.string.cpu_temp_template,
        (averageTemp).toString()
    )
}

fun Context.getUsedMemory(): String {
    val memoryInfo = ActivityManager.MemoryInfo()
    (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
    val usedMegaBytes = (memoryInfo.totalMem - memoryInfo.availMem) / BYTES_TO_MEGABYTES_FACTOR
    return resources.getString(R.string.used_memory_template, usedMegaBytes.toInt().toString())
}

fun BottomSheetBehavior<View>.setCollapsed() = run { state = BottomSheetBehavior.STATE_COLLAPSED }

fun getSessionUsedBytes(startBytes: Float) =
    ((TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()) - startBytes)

fun View.disableAndEnable(millis: Long = DISABLE_DURATION) = launchMain {
    isEnabled = false
    delay(millis)
    isEnabled = true
}

fun Context.isViewLandscape() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

fun BroadcastConfiguration.asString() = "Broadcast configuration:\n" +
        "Video.initialBitrate: ${video.initialBitrate}\n" +
        "Video.minBitrate: ${video.minBitrate}\n" +
        "Video.maxBitrate: ${video.maxBitrate}\n" +
        "Video.isUseAutoBitrate: ${video.isUseAutoBitrate}\n" +
        "Video.isUseBFrames: ${video.isUseBFrames}\n" +
        "Video.keyframeInterval: ${video.keyframeInterval}\n" +
        "Video.size: (${video.size.x}, ${video.size.x})\n" +
        "Video.targetFramerate: ${video.targetFramerate}\n" +
        "Mixer.slots.size: ${mixer.slots.size}\n"
