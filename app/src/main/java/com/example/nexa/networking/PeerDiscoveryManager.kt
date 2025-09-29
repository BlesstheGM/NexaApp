package com.example.nexa.networking

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.util.Log
import androidx.annotation.RequiresPermission

class PeerDiscoveryManager(private val context: Context) {
    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = manager.initialize(context, context.mainLooper, null)

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun discoverPeers() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d("Discovery", "Started") }
            override fun onFailure(reason: Int) { Log.e("Discovery", "Failed: $reason") }
        })
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun requestPeers(onPeersFound: (List<WifiP2pDevice>) -> Unit) {
        manager.requestPeers(channel) { list -> onPeersFound(list.deviceList.toList()) }
    }

    fun requestConnectionInfo(onInfo: (WifiP2pInfo) -> Unit) {
        manager.requestConnectionInfo(channel) { info -> onInfo(info) }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun connectToPeer(deviceAddress: String, onConnected: () -> Unit, onFailure: (reason: Int) -> Unit) {
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
            wps.setup = WpsInfo.PBC
        }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onConnected()
            override fun onFailure(reason: Int) = onFailure(reason)
        })
    }

    fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
        context.registerReceiver(receiver, filter)
    }
    fun unregisterReceiver(receiver: BroadcastReceiver) {
        context.unregisterReceiver(receiver)
    }
}
