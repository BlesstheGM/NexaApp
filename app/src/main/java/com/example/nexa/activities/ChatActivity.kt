package com.example.nexa.activities

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nexa.R
import com.example.nexa.messaging.ChatAdapter
import com.example.nexa.models.ChatMessage
import com.example.nexa.models.MessageType
import com.example.nexa.networking.P2pConnectionService

class ChatActivity : AppCompatActivity() {

    private var service: P2pConnectionService? = null
    private var bound = false
    private lateinit var adapter: ChatAdapter

    private var isGroupOwner = false
    private var ownerAddr = ""
    private var peerAddressLabel = "" // shown in UI; not used for routing

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as P2pConnectionService.LocalBinder
            service = b.service()
            bound = true
            if (isGroupOwner) service?.startAsGroupOwner()
            else service?.startAsClient(ownerAddr)

            service?.incoming?.observe(this@ChatActivity) { msg ->
                adapter.add(msg)
                findViewById<RecyclerView>(R.id.chatRecycler)
                    .scrollToPosition(adapter.itemCount - 1)
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            service = null
        }
    }

    private val pickFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = res.data?.data
            if (uri != null) {
                val file = copyUriToCache(uri)
                // Direct send using the currently connected peer's nodeId (service tracks it)
                service?.sendFile(file, destNodeId = servicePeerNodeId())
                adapter.add(
                    ChatMessage(
                        fromMe = true,
                        type = MessageType.TEXT,
                        content = "[Sending file ${file.name}]"
                    )
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        isGroupOwner = intent.getBooleanExtra("GROUP_OWNER", false)
        ownerAddr = intent.getStringExtra("OWNER_ADDR") ?: ""
        peerAddressLabel = intent.getStringExtra("PEER_ADDR") ?: ""

        
        val prefs = getSharedPreferences("nexa_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", "Me")  // default "Me" if not found

        val chatTitleView = findViewById<TextView>(R.id.chatTitle)
        chatTitleView.text = buildString {
            append(username)  // display your saved username
            append("  •  ")
            append(peerAddressLabel.ifBlank { "Unknown Device" })  // optional: still show device name
            if (isGroupOwner) append("  •  (Owner)")
            else append("  •  (Client)")
        }


        val recycler = findViewById<RecyclerView>(R.id.chatRecycler)
        recycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        adapter = ChatAdapter(mutableListOf())
        recycler.adapter = adapter

        findViewById<Button>(R.id.sendButton).setOnClickListener {
            val input = findViewById<EditText>(R.id.messageInput)
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                service?.sendText(text, destNodeId = servicePeerNodeId())
                adapter.add(ChatMessage(fromMe = true, type = MessageType.TEXT, content = text))
                recycler.scrollToPosition(adapter.itemCount - 1)
                input.setText("")
            }
        }

        findViewById<Button>(R.id.fileButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            pickFile.launch(Intent.createChooser(intent, "Select file"))
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, P2pConnectionService::class.java),
            connection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        if (bound) unbindService(connection)
        bound = false
    }

    private fun copyUriToCache(uri: Uri): java.io.File {
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "file.bin"
        val dest = java.io.File(cacheDir, name)
        contentResolver.openInputStream(uri)?.use { input ->
            java.io.FileOutputStream(dest).use { out -> input.copyTo(out) }
        }
        return dest
    }

    // Helper: use the currently connected peer's nodeId stored inside the service
    private fun servicePeerNodeId(): String {
        // For this minimal implementation, the service uses peerNodeId for the single active link.
        // If you extend to multiple concurrent links, expose a method to target a specific nodeId.
        return try {
            val f = P2pConnectionService::class.java.getDeclaredField("peerNodeId")
            f.isAccessible = true
            f.get(service) as String
        } catch (e: Exception) {
            ""
        }
    }
}
