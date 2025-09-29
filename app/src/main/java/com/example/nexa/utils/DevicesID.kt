package com.example.nexa.utils

import android.content.Context
import java.util.UUID

object DeviceId {
    private const val KEY = "node_id"
    fun get(context: Context): String {
        val prefs = context.getSharedPreferences("nexa_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString(KEY, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY, id).apply()
        }
        return id
    }
}
