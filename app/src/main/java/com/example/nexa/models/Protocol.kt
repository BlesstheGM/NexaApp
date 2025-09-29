package com.example.nexa.models

object FrameType {
    const val HELLO = 1           // ephemeral ECDH pubkey + nodeId
    const val HELLO_ACK = 2
    const val MSG = 3             // encrypted text
    const val FILE_META = 4       // filename, size, mime (encrypted)
    const val FILE_CHUNK = 5      // encrypted chunk
    const val FILE_DONE = 6
    const val RELAY_ENVELOPE = 7  // {destNodeId, type, payloadB64}
}
