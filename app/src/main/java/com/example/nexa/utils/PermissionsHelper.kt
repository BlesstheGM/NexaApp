package com.example.nexa.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionsHelper {
    private const val REQ = 100

    fun requestAll(activity: Activity) {
        val need = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            need += Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED)
            need += Manifest.permission.NEARBY_WIFI_DEVICES

        if (need.isNotEmpty())
            ActivityCompat.requestPermissions(activity, need.toTypedArray(), REQ)
    }
}
