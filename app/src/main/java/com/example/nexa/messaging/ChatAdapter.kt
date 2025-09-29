package com.example.nexa.messaging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nexa.R
import com.example.nexa.models.ChatMessage
import com.example.nexa.models.MessageType

class ChatAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val LEFT = 1
    private val RIGHT = 2

    override fun getItemViewType(position: Int): Int =
        if (messages[position].fromMe) RIGHT else LEFT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == RIGHT) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.chat_item_text_right, parent, false)
            RightVH(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.chat_item_text_left, parent, false)
            LeftVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val m = messages[position]
        val text = if (m.type == MessageType.TEXT) m.content else "[File] ${m.content}"
        if (holder is RightVH) holder.text.text = text
        if (holder is LeftVH) holder.text.text = text
    }

    override fun getItemCount(): Int = messages.size

    fun add(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    class LeftVH(v: View): RecyclerView.ViewHolder(v) { val text: TextView = v.findViewById(R.id.messageText) }
    class RightVH(v: View): RecyclerView.ViewHolder(v) { val text: TextView = v.findViewById(R.id.messageText) }
}
