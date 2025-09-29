package com.example.nexa.networking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nexa.R
import com.example.nexa.models.Peer

class PeerAdapter(
    private val peers: MutableList<Peer>,
    private val onConnectClick: (Peer) -> Unit
) : RecyclerView.Adapter<PeerAdapter.PeerViewHolder>() {

    inner class PeerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val peerName: TextView = itemView.findViewById(R.id.peerName)
        val connectButton: Button = itemView.findViewById(R.id.connectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.peer_item, parent, false)
        return PeerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        val peer = peers[position]
        holder.peerName.text = "${peer.name} (${peer.address})"
        holder.connectButton.setOnClickListener { onConnectClick(peer) }
    }

    override fun getItemCount(): Int = peers.size
}
