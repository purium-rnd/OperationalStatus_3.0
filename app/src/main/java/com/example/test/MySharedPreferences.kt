package com.example.test

import android.content.Context
import android.content.SharedPreferences

class MySharedPreferences(context: Context) {
    var mContext = context
    var sub_title: String = context.getString(R.string.sub_title)
    var isOperationTimeLong = true

    val PREFS_FILE_NAME = "prefs"
    var prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILE_NAME, 0)

    fun save() {
        prefs.edit().putString("sub_title", sub_title).apply()
    }

    fun load() {
        sub_title = prefs.getString("sub_title", mContext.getString(R.string.sub_title))
    }
}