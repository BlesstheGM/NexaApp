package com.example.nexa.networking

import android.app.Service
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.nexa.models.ChatMessage
import com.example.nexa.models.FrameType
import com.example.nexa.models.MessageType
import com.example.nexa.security.Crypto
import com.example.nexa.storage.AppDatabase
import com.example.nexa.storage.QueueRepository
import com.example.nexa.utils.DeviceId
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import android.os.Binder
import android.os.IBinder

/**
 * Wi‑Fi Direct socket service:
 * - Server/client roles
 * - ECDH handshake with per-device nodeId
 * - AES‑GCM encryption for messages and file chunks
 * - Offline queue retry
 * - Basic multi‑hop relaying with nodeId addressing
 */
class P2pConnectionService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Sockets
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null

    // Session
    private var aesKey: ByteArray? = null
    private val port = 8988
    private val myNodeId: String by lazy { DeviceId.get(this) }
    private var peerNodeId: String = ""

    // Routing: nodeId -> output stream
    private val routingTable = mutableMapOf<String, DataOutputStream>()

    private val _connected = MutableLiveData(false)
    val connected: LiveData<Boolean> get() = _connected

    private val _incoming = MutableLiveData<ChatMessage>()
    val incoming: LiveData<ChatMessage> get() = _incoming

    private lateinit var queueRepo: QueueRepository

    inner class LocalBinder : Binder() {
        fun service(): P2pConnectionService = this@P2pConnectionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        queueRepo = QueueRepository(AppDatabase.get(this).queueDao())
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        closeAll()
    }

    fun startAsGroupOwner() {
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d("P2P", "Server listening on $port")
                socket = serverSocket!!.accept()
                onSocketReady(socket!!)
            } catch (e: IOException) {
                Log.e("P2P", "Server error", e)
                _connected.postValue(false)
            }
        }
    }

    fun startAsClient(groupOwnerAddress: String) {
        scope.launch {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(groupOwnerAddress, port), 15000)
                socket = s
                onSocketReady(s)
            } catch (e: IOException) {
                Log.e("P2P", "Client connect error", e)
                _connected.postValue(false)
            }
        }
    }

    private suspend fun onSocketReady(s: Socket) {
        input = DataInputStream(BufferedInputStream(s.getInputStream()))
        output = DataOutputStream(BufferedOutputStream(s.getOutputStream()))
        performHandshake()
        routingTable[peerNodeId] = output!!
        _connected.postValue(true)

        scope.launch { readLoop() }
        scope.launch { flushQueue() }
    }

    private fun performHandshake() {
        val myKP = Crypto.generateEcKeyPair()
        val myPub = myKP.public.encoded
        val nodeIdBytes = myNodeId.toByteArray(Charsets.UTF_8)

        // Send HELLO: pubkey + nodeId
        output!!.writeInt(FrameType.HELLO)
        output!!.writeInt(myPub.size); output!!.write(myPub)
        output!!.writeInt(nodeIdBytes.size); output!!.write(nodeIdBytes)
        output!!.flush()

        // Receive peer HELLO
        val frame = input!!.readInt()
        if (frame != FrameType.HELLO) throw IOException("Expected HELLO")
        val peerKeyLen = input!!.readInt()
        val peerKeyBytes = ByteArray(peerKeyLen); input!!.readFully(peerKeyBytes)
        val peerNodeIdLen = input!!.readInt()
        val peerNodeIdBytes = ByteArray(peerNodeIdLen); input!!.readFully(peerNodeIdBytes)
        peerNodeId = String(peerNodeIdBytes, Charsets.UTF_8)

        // Send ACK
        output!!.writeInt(FrameType.HELLO_ACK)
        output!!.flush()

        val peerPubKey: PublicKey = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(peerKeyBytes))
        val shared = Crypto.deriveSharedKey(myKP.private, peerPubKey)
        aesKey = Crypto.hkdfSha256(shared)
    }

    private suspend fun readLoop() {
        try {
            while (true) {
                val frame = input!!.readInt()
                when (frame) {
                    FrameType.MSG -> readMsgFrame()
                    FrameType.FILE_META -> readFileMeta()
                    FrameType.FILE_CHUNK -> readFileChunk()
                    FrameType.FILE_DONE -> readFileDone()
                    FrameType.RELAY_ENVELOPE -> readRelayEnvelope()
                    else -> Log.w("P2P", "Unknown frame: $frame")
                }
            }
        } catch (e: IOException) {
            Log.e("P2P", "Read loop ended", e)
            _connected.postValue(false)
            closeAll()
        }
    }

    private fun readMsgFrame() {
        val ivLen = input!!.readInt()
        val iv = ByteArray(ivLen); input!!.readFully(iv)
        val ctLen = input!!.readInt()
        val ct = ByteArray(ctLen); input!!.readFully(ct)
        val pt = Crypto.aesGcmDecrypt(aesKey!!, iv, ct)
        val text = String(pt, Charsets.UTF_8)
        _incoming.postValue(ChatMessage(fromMe = false, type = MessageType.TEXT, content = text))
    }

    private fun readFileMeta() {
        val ivLen = input!!.readInt()
        val iv = ByteArray(ivLen); input!!.readFully(iv)
        val metaLen = input!!.readInt()
        val ct = ByteArray(metaLen); input!!.readFully(ct)
        val metaJson = String(Crypto.aesGcmDecrypt(aesKey!!, iv, ct), Charsets.UTF_8)
        _incoming.postValue(ChatMessage(fromMe = false, type = MessageType.TEXT, content = "[Incoming file meta] $metaJson"))
    }

    private fun readFileChunk() {
        val ivLen = input!!.readInt()
        val iv = ByteArray(ivLen); input!!.readFully(iv)
        val len = input!!.readInt()
        val ct = ByteArray(len); input!!.readFully(ct)
        val chunk = Crypto.aesGcmDecrypt(aesKey!!, iv, ct)
        _incoming.postValue(ChatMessage(fromMe = false, type = MessageType.TEXT, content = "[Received ${chunk.size} bytes]"))
    }

    private fun readFileDone() {
        _incoming.postValue(ChatMessage(fromMe = false, type = MessageType.TEXT, content = "[File received]"))
    }

    private fun readRelayEnvelope() {
        val jsonLen = input!!.readInt()
        val jsonBytes = ByteArray(jsonLen); input!!.readFully(jsonBytes)
        val obj = JSONObject(String(jsonBytes, Charsets.UTF_8))
        val dest = obj.getString("dest")
        val type = obj.getInt("type")
        val payloadB64 = obj.getString("payload")
        val payload = android.util.Base64.decode(payloadB64, android.util.Base64.DEFAULT)

        if (dest == myNodeId) {
            handleIncomingFrame(type, payload)
        } else {
            routingTable[dest]?.let { out ->
                out.writeInt(type)
                out.write(payload)
                out.flush()
            } ?: Log.w("P2P", "No route to $dest")
        }
    }

    // Decode payload for MSG/FILE frames (used when receiving relayed content)
    private fun handleIncomingFrame(type: Int, payload: ByteArray) {
        val dis = DataInputStream(ByteArrayInputStream(payload))
        when (type) {
            FrameType.MSG -> {
                val ivLen = dis.readInt()
                val iv = ByteArray(ivLen); dis.readFully(iv)
                val ctLen = dis.readInt()
                val ct = ByteArray(ctLen); dis.readFully(ct)
                val pt = Crypto.aesGcmDecrypt(aesKey!!, iv, ct)
                val text = String(pt, Charsets.UTF_8)
                _incoming.postValue(ChatMessage(fromMe = false, type = MessageType.TEXT, content = text))
            }
            FrameType.FILE_META -> {
                val ivLen = dis.readInt()
                val iv = ByteArray(ivLen); dis.readFully(iv)
                val metaLen = dis.readInt()
                val ct = ByteArray(metaLen); dis.readFully(ct)
                val metaJson = String(Crypto.aesGcmDecrypt(aesKey!!, iv, ct), Charsets.UTF_8)
                _incoming.postValue(ChatMessage(fromMe = false, type = MessageType.TEXT, content = "[Incoming file meta] $metaJson"))
            }
            FrameType.FILE_CHUNK -> {
                val ivLen = dis.readInt()
                val iv = ByteArray(ivLen); dis.readFully(iv)
                val len = dis.readInt()
                val ct = ByteArray(len); dis.readFully(ct)
                val chunk = Crypto.aesGcmDecrypt(aesKey!!, iv, ct)
                _incoming.postValue(ChatMessage(fromMe = false, type = MessageType.TEXT, content = "[Received ${chunk.size} bytes]"))
            }
            FrameType.FILE_DONE -> {
                _incoming.postValue(ChatMessage(fromMe = false, type = MessageType.TEXT, content = "[File received]"))
            }
        }
    }

    private fun closeAll() {
        try { input?.close() } catch (_: Exception) {}
        try { output?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        input = null; output = null; socket = null; serverSocket = null
    }

    // Direct encrypted text
    fun sendText(text: String, destNodeId: String, onQueued: (() -> Unit)? = null) {
        val key = aesKey
        val out = output
        if (key == null || out == null) {
            scope.launch { queueRepo.enqueue(destNodeId, isFile = false, content = text); onQueued?.invoke() }
            return
        }
        scope.launch {
            try {
                val (iv, ct) = Crypto.aesGcmEncrypt(key, text.toByteArray(Charsets.UTF_8))
                out.writeInt(FrameType.MSG)
                out.writeInt(iv.size); out.write(iv)
                out.writeInt(ct.size); out.write(ct)
                out.flush()
            } catch (e: IOException) {
                Log.e("P2P", "Send text failed, queueing", e)
                queueRepo.enqueue(destNodeId, isFile = false, content = text)
                _connected.postValue(false)
                closeAll()
            }
        }
    }

    // Direct encrypted file in chunks
    fun sendFile(file: File, destNodeId: String, chunkSize: Int = 64 * 1024, onQueued: (() -> Unit)? = null) {
        val key = aesKey
        val out = output
        if (key == null || out == null) {
            scope.launch { queueRepo.enqueue(destNodeId, isFile = true, content = file.absolutePath); onQueued?.invoke() }
            return
        }
        scope.launch {
            try {
                val meta = """{"name":"${file.name}","size":${file.length()},"mime":"application/octet-stream"}"""
                val (ivMeta, ctMeta) = Crypto.aesGcmEncrypt(key, meta.toByteArray())
                out.writeInt(FrameType.FILE_META)
                out.writeInt(ivMeta.size); out.write(ivMeta)
                out.writeInt(ctMeta.size); out.write(ctMeta)
                out.flush()

                file.inputStream().use { fis ->
                    val buf = ByteArray(chunkSize)
                    while (true) {
                        val read = fis.read(buf)
                        if (read <= 0) break
                        val data = buf.copyOf(read)
                        val (iv, ct) = Crypto.aesGcmEncrypt(key, data)
                        out.writeInt(FrameType.FILE_CHUNK)
                        out.writeInt(iv.size); out.write(iv)
                        out.writeInt(ct.size); out.write(ct)
                        out.flush()
                    }
                }
                out.writeInt(FrameType.FILE_DONE)
                out.flush()
            } catch (e: IOException) {
                Log.e("P2P", "Send file failed, queueing", e)
                queueRepo.enqueue(destNodeId, isFile = true, content = file.absolutePath)
                _connected.postValue(false)
                closeAll()
            }
        }
    }

    // Relay envelope to current link (forwarding happens downstream)
    fun sendViaRelay(destNodeId: String, frameType: Int, payload: ByteArray) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("dest", destNodeId)
                    put("type", frameType)
                    put("payload", android.util.Base64.encodeToString(payload, android.util.Base64.NO_WRAP))
                }.toString().toByteArray(Charsets.UTF_8)
                output?.writeInt(FrameType.RELAY_ENVELOPE)
                output?.writeInt(json.size)
                output?.write(json)
                output?.flush()
            } catch (e: IOException) {
                Log.e("P2P", "Relay send failed", e)
            }
        }
    }

    // Build payload and relay a text message
    fun sendTextMultiHop(destNodeId: String, text: String) {
        val key = aesKey ?: return
        val (iv, ct) = Crypto.aesGcmEncrypt(key, text.toByteArray())
        val payload = ByteArrayOutputStream().apply {
            DataOutputStream(this).use { dos ->
                dos.writeInt(iv.size); dos.write(iv)
                dos.writeInt(ct.size); dos.write(ct)
            }
        }.toByteArray()
        sendViaRelay(destNodeId, FrameType.MSG, payload)
    }

    private suspend fun flushQueue() {
        val items = queueRepo.allQueued()
        for (item in items) {
            if (aesKey == null || output == null) return
            try {
                if (item.isFile) sendFile(File(item.content), item.peerNodeId)
                else sendText(item.content, item.peerNodeId)
            } catch (_: Exception) {}
        }
    }
}
