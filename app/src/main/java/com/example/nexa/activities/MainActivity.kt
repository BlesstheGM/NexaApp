package com.example.nexa.activities

import android.Manifest
import android.content.*
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nexa.R
import com.example.nexa.models.Peer
import com.example.nexa.networking.PeerAdapter
import com.example.nexa.networking.PeerDiscoveryManager
import com.example.nexa.utils.PermissionsHelper

class MainActivity : AppCompatActivity() {
    private lateinit var peerDiscoveryManager: PeerDiscoveryManager
    private lateinit var peerAdapter: PeerAdapter
    private val discoveredPeers = mutableListOf<Peer>()

    private var connectionInfo: WifiP2pInfo? = null
    private var selectedPeerAddress: String = ""

    private val p2pReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    peerDiscoveryManager.requestPeers { devices ->
                        populatePeers(devices)
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<android.net.NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        peerDiscoveryManager.requestConnectionInfo { info ->
                            connectionInfo = info
                            // Launch chat once group formed
                            startActivity(Intent(this@MainActivity, ChatActivity::class.java).apply {
                                putExtra("GROUP_OWNER", info.isGroupOwner)
                                putExtra("OWNER_ADDR", info.groupOwnerAddress?.hostAddress ?: "")
                                putExtra("PEER_ADDR", selectedPeerAddress)  // used as peer address display
                            })
                        }
                    }
                }
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionsHelper.requestAll(this)

        peerDiscoveryManager = PeerDiscoveryManager(this)

        val peerListView = findViewById<RecyclerView>(R.id.peerList)
        peerListView.layoutManager = LinearLayoutManager(this)
        peerAdapter = PeerAdapter(discoveredPeers) { peer ->
            selectedPeerAddress = peer.address
            connectToPeer(peer.address)
        }
        peerListView.adapter = peerAdapter

        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        registerReceiver(p2pReceiver, filter)

        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            peerDiscoveryManager.discoverPeers()
        }

        peerDiscoveryManager.discoverPeers()
    }

    private fun populatePeers(devices: List<WifiP2pDevice>) {
        discoveredPeers.clear()
        devices.forEach { d ->
            discoveredPeers.add(Peer(d.deviceName, d.deviceAddress, true))
        }
        peerAdapter.notifyDataSetChanged()
    }

    private fun connectToPeer(address: String) {
        peerDiscoveryManager.connectToPeer(address, onConnected = {
            Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
        }, onFailure = { reason ->
            Toast.makeText(this, "Connect failed: $reason", Toast.LENGTH_SHORT).show()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(p2pReceiver)
    }
}
