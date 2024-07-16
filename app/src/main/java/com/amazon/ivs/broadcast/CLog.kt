package com.amazon.ivs.broadcast

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import java.io.PrintWriter
import java.io.StringWriter

object CLog {
    val TAG = "[PD]"
    private var SHOW_LOG = true //false이면 로그 출력 안함

    fun e(obj: Any,log: String) {
        val tag = if (obj::class.simpleName != null) obj::class.simpleName!! else TAG
        e(tag, log)
    }

    fun e(log: String) {
        e(TAG, log)
    }
    fun e(e: Exception) { e(TAG, e) }

    fun e(e: Throwable) { e(TAG, e)}

    fun e(TAG: String, log: String) {
        if (SHOW_LOG) {
            Log.e(TAG, log)
        }
    }

    fun v(log: String) { v(TAG, log) }
    fun v(TAG: String, log: String) {
        if (SHOW_LOG) {
            Log.v(TAG, log)
        }
    }
    fun d(log: String) { d(TAG, log) }
    fun d(tag: String, log: String) {
        if (SHOW_LOG) {
            Log.d(tag, log)
        }
    }

    private const val MAX_LOG_LENGTH = 3000
    fun longLog(header:String, log:String) {
        if (log.length > MAX_LOG_LENGTH) {
            Log.v(TAG, "sb.length = " + log.length)
            val chunkCount = log.length / MAX_LOG_LENGTH // integer division
            for (i in 0..chunkCount) {
                val max = MAX_LOG_LENGTH * (i + 1)
                if (max >= log.length) {
                    Log.d("OkHttp","<-- Response : " + header + "\n(" + i + "/" + chunkCount + ")\n" + log.substring(MAX_LOG_LENGTH * i))
                } else {
                    Log.d("OkHttp","<-- Response : " + header + "\n(" + i + "/" + chunkCount + ")\n" + log.substring(MAX_LOG_LENGTH * i, max))
                }
            }
        } else {
            Log.d("OkHttp", "<-- Response : $header\n$log")
        }

    }

    fun i(log: String) { i(TAG, log) }
    fun i(tag: String, log: String) {
        if (SHOW_LOG) {
            Log.i(tag, log)
        }
    }
    fun w(log: String) { w(TAG, log) }
    fun w(tag: String, log: String) {
        if (SHOW_LOG) {
            Log.w(tag, log)
        }
    }

    fun saveUseLog(context:Context, isUseLog: Boolean) {
        val pref = context.getSharedPreferences("log_config", Activity.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putBoolean("isUseLog", isUseLog)
        editor.apply()
        SHOW_LOG = isUseLog
    }

    fun loadUseLog(context:Context) : Boolean {
        val pref = context.getSharedPreferences("log_config", Activity.MODE_PRIVATE)
        return try {
            pref.getBoolean("isUseLog", false)
        } catch (e: Exception) {
            false
        }
    }

    fun SetUseLog(isUseLog: Boolean) {
        SHOW_LOG = isUseLog
    }

    fun getUseLog() : Boolean {
        return SHOW_LOG
    }

    fun e(TAG: String, e: Exception) {
        Log.e(TAG, "catch Exception")
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val exceptionAsString = sw.toString()
        Log.e(TAG, exceptionAsString)
    }

    fun e(TAG: String, e: Throwable) {
        Log.e(TAG, "catch Exception")
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val exceptionAsString = sw.toString()
        Log.e(TAG, exceptionAsString)
    }

    fun getClassAddress(obj: Any): String {
        return obj.toString().substring(obj.toString().lastIndexOf("@") + 5)
    }

    fun layout(log: String, view: View) { layout(TAG, log, view) }
    fun layout(tag: String, desc: String, view: View) {
        e(tag, desc + " l = " + view.left + " r = " + view.right + " t = " + view.top + " b = " + view.bottom)
    }
}