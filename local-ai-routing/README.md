# Tulip Local AI Routing — Cross-Platform Ollama + Gemma 3 4B

Self-contained cross-platform local inference router and setup toolkit for the **Tulip Android Application**. Exposes a reverse proxy over your local Wi-Fi network to connect Tulip's Kotlin `OllamaAiService` directly to a local Ollama instance running the **Gemma 3 4B** (`gemma3:4b`) multimodal model.

---

## 📋 Features

- **Cross-Platform**: Full support for Windows (PowerShell/CMD), macOS (Terminal), and Linux shells.
- **Automated Setup CLI**: Guides installation, checks RAM requirements, starts Ollama, downloads `gemma3:4b`, and runs real end-to-end text and vision inference checks.
- **Reverse Proxy Router**: Listens on your local network (e.g. `http://192.168.1.X:8787`) and safely forwards request payloads (`POST /api/chat`, `GET /api/tags`) to Ollama.
- **Privacy & Log Sanitization**: Strips prompt contents and Base64 image streams from terminal console logs.
- **Isolated Process Cleanup**: Clean shutdown via `npm run stop` that only stops launcher-managed processes without terminating pre-existing background Ollama instances.

---

## 💻 System Requirements & Prerequisites

| Requirement | Specification |
| :--- | :--- |
| **Supported OS** | Windows 10/11, macOS 12+, Linux (Ubuntu, Debian, Fedora, Arch) |
| **Node.js** | Node.js v18.0.0 or higher (`node -v`) |
| **RAM / VRAM** | Minimum 8 GB RAM recommended (Gemma 3 4B model size ~3.3 GB) |
| **Network** | Laptop and Android phone connected to the **same Wi-Fi network** |

---

## 🚀 Quick Start Guide

### 1. Installation & Setup

Open a terminal inside `local-ai-routing/` and install dependencies:

```bash
cd local-ai-routing
npm install
```

Run the automated interactive setup tool:

```bash
npm run setup
```

The setup script will:
1. Detect your operating system.
2. Search for the Ollama executable in system PATH and standard directories.
3. Check available RAM and issue a warning if total memory is below 8 GB.
4. Start Ollama server if not currently responding.
5. Check if `gemma3:4b` is installed (and ask confirmation before pulling if missing).
6. Verify model registration and run real **Text** and **Vision/Multimodal** inference tests.
7. Discover candidate local Wi-Fi IPv4 addresses and select an available port.
8. Output the exact LAN URL to copy into Tulip on Android.

---

## 📱 Configuring Tulip Android Application

1. Open the **Tulip App** on your Android phone.
2. Go to **Settings** -> **AI Provider** -> **Local Ollama**.
3. Enter the URL provided by `npm run setup` or `npm run status`:
   ```text
   http://<YOUR-LAPTOP-WIFI-IP>:8787
   ```
4. Tap **Test Connection**.
5. Test a **Fix & Translate** request or submit an **Ask AI** query with a photograph.

---

## 🛠️ Commands Reference

| Command | Action |
| :--- | :--- |
| `npm run setup` | Runs complete 15-step interactive environment check, model pull, and verification |
| `npm run start` | Launches the local AI routing reverse proxy server on LAN |
| `npm run stop` | Safely stops the router server and launcher-spawned Ollama instance |
| `npm run status` | Displays full diagnostic status report (Ollama, model, inference, router, LAN URL) |
| `npm run test` | Executes automated Vitest unit test suite |
| `npm run test:inference` | Performs a real text inference verification against Ollama |
| `npm run test:vision` | Performs a real vision / multimodal inference verification with sample image |

---

## 🔍 Wi-Fi Network Address Discovery & Troubleshooting

### How to verify your laptop's correct Wi-Fi IP address manually:

- **Windows PowerShell**:
  ```powershell
  Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias "Wi-Fi*" | Select-Object IPAddress
  ```
- **macOS / Linux**:
  ```bash
  ifconfig | grep "inet " | grep -v 127.0.0.1
  # or
  ip -4 addr show
  ```

### Common Troubleshooting Steps:

1. **Android "Connection Refused" / Timeout**:
   - Verify that your laptop and Android phone are on the **exact same Wi-Fi network**.
   - Check Windows Defender Firewall / macOS Application Firewall. Allow incoming connections on port `8787` (TCP).
   - Temporary PowerShell rule to allow port 8787 on Windows:
     ```powershell
     New-NetFirewallRule -DisplayName "Tulip Local AI Router" -Direction Inbound -LocalPort 8787 -Protocol TCP -Action Allow
     ```
2. **"Model gemma3:4b not found"**:
   - Manually pull the model in terminal:
     ```bash
     ollama pull gemma3:4b
     ```
3. **Low RAM / Slow Response**:
   - Close heavy background applications. Gemma 3 4B requires ~3.5 GB of free RAM/VRAM during active generation.

---

## 🔒 Security & Privacy Notice

- The routing server is designed **strictly for local network development and usage**.
- Upstream Ollama connections are bound strictly to `127.0.0.1:11434` (loopback).
- Console logging sanitizes image Base64 payloads and raw journal prompt contents.
- **Do not expose port 8787 to the public internet** via port forwarding or unauthenticated tunnels.

---

## 🧪 Testing

Run the automated test suite:

```bash
npm test
```
