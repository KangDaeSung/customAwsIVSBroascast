package com.amazon.ivs.broadcast.cache

import android.content.Context
import com.amazon.ivs.broadcast.models.Orientation
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

const val PREFERENCES_NAME = "app_preferences"

class PreferenceProvider(context: Context, preferencesName: String) {
    var isOnboardingDone by booleanPreference()
    var orientation by intPreference(Orientation.AUTO.id)
    var width by floatPreference(720f)
    var height by floatPreference(1280f)
    var autoAdjustBitrate by booleanPreference()
    var useCustomBitrateLimits by booleanPreference()
    var useCustomResolution by booleanPreference()
    var defaultCameraId by stringPreference()
    var defaultCameraPosition by stringPreference()

    private val sharedPreferences by lazy { context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE) }

    private fun booleanPreference() = object : ReadWriteProperty<Any?, Boolean> {

        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            sharedPreferences.getBoolean(property.name, false)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            sharedPreferences.edit().putBoolean(property.name, value).apply()
        }
    }

    private fun stringPreference() = object : ReadWriteProperty<Any?, String?> {

        override fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getString(property.name, null)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
            sharedPreferences.edit().putString(property.name, value).apply()
        }
    }

    private fun intPreference(initValue: Int) = object : ReadWriteProperty<Any?, Int> {

        override fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getInt(property.name, initValue)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            sharedPreferences.edit().putInt(property.name, value).apply()
        }
    }

    private fun floatPreference(initialValue: Float) = object : ReadWriteProperty<Any?, Float> {

        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            sharedPreferences.getFloat(property.name, initialValue)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
            sharedPreferences.edit().putFloat(property.name, value).apply()
        }
    }
}
