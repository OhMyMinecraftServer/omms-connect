package net.zhuruoling.omms.connect.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity

class PreferencesStorage private constructor(context: Context, region: String) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "omms-connect:$region",
        AppCompatActivity.MODE_PRIVATE
    )
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    fun putString(key: String, value: String): PreferencesStorage{
        editor.putString(key, value)
        return this
    }
    fun putFloat(key: String, value: Float): PreferencesStorage{
        editor.putFloat(key, value)
        return this
    }
    fun putInt(key: String, value: Int): PreferencesStorage{
        editor.putInt(key, value)
        return this
    }
    fun putLong(key: String, value: Long): PreferencesStorage{
        editor.putLong(key, value)
        return this
    }
    fun putBoolean(key: String, value: Boolean): PreferencesStorage{
        editor.putBoolean(key, value)
        return this
    }
    fun putStringSet(key: String, value: MutableSet<String>): PreferencesStorage{
        editor.putStringSet(key, value)
        return this
    }
    fun clear(): PreferencesStorage {
        editor.clear()
        return this
    }

    fun contains(key: String): Boolean{
        return sharedPreferences.contains(key)
    }

    fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue)!!
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun getStringSet(key: String, defaultValue: MutableSet<String>): MutableSet<String> {
        return sharedPreferences.getStringSet(key, defaultValue)!!
    }

    fun commit(): PreferencesStorage {
        editor.commit()
        return this
    }

    companion object {
        fun withContext(context: Context, region: String): PreferencesStorage {
            return PreferencesStorage(context, region)
        }
    }
}