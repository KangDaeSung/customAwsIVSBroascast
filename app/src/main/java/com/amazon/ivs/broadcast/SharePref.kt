package com.amazon.ivs.broadcast

import android.app.Activity
import android.content.Context

/**
 * Created by android on 2016-10-13.
 */
object SharePref {
    const val configName = "config"

    fun put(key: String, value: String) {
        put(CApp.get(), configName, key, value)
    }

    fun put(context: Context, config: String, key: String, value: String) {
        val pref = context.getSharedPreferences(config, Activity.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun put(key: String, value: Boolean) {
        put(configName, key, value)
    }

    fun put(config: String, key: String, value: Boolean) {
        val pref = CApp.get().getSharedPreferences(config, Activity.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun put(key: String, value: Long) {
        put(configName, key, value)
    }

    fun put(config: String, key: String, value: Long) {
        val pref = CApp.get().getSharedPreferences(config, Activity.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun put(key: String, value: Int) {
        put(configName, key, value)
    }

    fun put(config: String, key: String, value: Int) {
        val pref = CApp.get().getSharedPreferences(config, Activity.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    operator fun get(key: String, value: String): String {
        return SharePref[CApp.get(), configName, key, value]
    }

    operator fun get(context: Context, config: String, key: String, dftValue: String): String {
        val pref = context.getSharedPreferences(config, Activity.MODE_PRIVATE)
        return try {
            pref.getString(key, dftValue) ?: ""
        } catch (e: Exception) {
            dftValue
        }
    }

    operator fun get(key: String, value: Long): Long {
        return SharePref[configName, key, value]
    }

    operator fun get(config: String, key: String, dftValue: Long): Long {
        val pref = CApp.get().getSharedPreferences(config, Activity.MODE_PRIVATE)
        return try {
            pref.getLong(key, dftValue)
        } catch (e: Exception) {
            dftValue
        }
    }

    operator fun get(key: String, value: Int): Int {
        return SharePref[configName, key, value]
    }

    operator fun get(config: String, key: String, dftValue: Int): Int {
        val pref = CApp.get().getSharedPreferences(config, Activity.MODE_PRIVATE)
        return try {
            pref.getInt(key, dftValue)
        } catch (e: Exception) {
            dftValue
        }
    }

    operator fun get(key: String, value: Boolean): Boolean {
        return SharePref[configName, key, value]
    }

    operator fun get(context: Context, key: String, value: Boolean): Boolean {
        return SharePref[context, configName, key, value]
    }

    operator fun get(config: String, key: String, dftValue: Boolean): Boolean {
        return SharePref[CApp.get(), config, key, dftValue]
    }

    operator fun get(context: Context, config: String, key: String, dftValue: Boolean): Boolean {
        val pref = context.getSharedPreferences(config, Activity.MODE_PRIVATE)
        return try {
            pref.getBoolean(key, dftValue)
        } catch (e: Exception) {
            dftValue
        }
    }
}