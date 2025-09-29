# 📡 Luma – Offline Encrypted P2P Messenger

Luma is an **Android application** that enables **offline, end-to-end encrypted messaging** using **Wi-Fi Direct**.  
It supports **text and file transfer**, **multi-hop relaying**, and an **offline queue** to ensure messages are delivered once a connection is restored.

---

## 🌟 Features

- 🔍 **Peer Discovery** – Find nearby devices over Wi-Fi Direct.
- 🔐 **Secure Connections** – End-to-end encryption using **ECDH + AES-GCM**.
- 💬 **Encrypted Messaging & File Transfer** – Send text or files in chunks.
- 🕒 **Offline Queue** – Room DB stores unsent messages until reconnection.
- 🔁 **Multi-Hop Relay** – Messages can be forwarded across intermediate peers.

---

## 🏛️ Architecture Overview

| Layer | Description |
|------|------------|
| **Peer Discovery** | Detects nearby devices using Wi-Fi Direct |
| **Connection Management** | Establishes and maintains secure sockets |
| **Encryption** | Hybrid ECDH + AES-GCM for forward secrecy |
| **Messaging** | Handles text/file sending, relaying and retries |
| **Offline Queue** | Persists unsent messages/files in a Room database |
| **Routing** | Multi-hop relay routing using node-ID addressing |


---

## 🚀 Getting Started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) **Giraffe (or later)**
- **Android SDK 33+**
- At least **two Android devices** that support **Wi-Fi Direct**.

### 1️⃣ Clone the repository
bash
git clone https://github.com/Nexa/Luma.git
cd Luma
2️⃣ Open the project
Launch Android Studio.

Choose Open an existing project → select the cloned Luma folder.

3️⃣ Build & Run
Connect two or more physical devices (Wi-Fi Direct does not work on emulators).

Click Run ▶ in Android Studio.

Grant permissions when prompted:

Location

Nearby devices (Android 12+)

💡 Usage
Discover peers:
Open the app → tap Scan to find nearby devices.

Connect to a peer:
Select a device from the peer list and tap Connect.

Start chatting:
Send text messages or files. Messages are:

Encrypted end-to-end.

Stored in the offline queue if a peer goes offline.

Multi-hop relay:
If the target peer is unreachable, intermediate devices automatically relay the message without decrypting it.

🌍 Deployment
Luma is a standard Android app. To share it:

Generate a signed APK / App Bundle

In Android Studio: Build → Generate Signed Bundle / APK…

Follow the wizard to create a release key.

Distribute

Share the generated .apk directly with users, or

Publish to a private app store or GitHub Releases.

⚠️ Wi-Fi Direct requires real devices—this app cannot be fully tested on emulators.

🔐 Cryptography
Protocol	Purpose	Why
ECDH (P-256)	Ephemeral key exchange	Lightweight, fast, provides forward secrecy
HKDF-SHA256	Derive 256-bit AES key	Strong, context-specific keys
AES-GCM	Encrypt text & file chunks	Authenticated encryption; tamper detection

Handshake Flow

Exchange EC public keys.

Derive shared secret → HKDF → AES key.

Encrypt each message/file chunk with AES-GCM and a random IV.

🧪 Testing Tips
Multiple devices: At least two Android phones/tablets with Wi-Fi Direct.

Network isolation: Turn off mobile data and Wi-Fi (except Wi-Fi Direct) to simulate true offline messaging.

File transfer: Test with both small and large files to see chunked encryption in action.

👥 Authors
Blessing Hlongwane <br>
Talha Omargee<br>
Kagiso Lekhuleni <br>
