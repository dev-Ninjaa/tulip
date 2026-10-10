# Tulip — Nature & Camping Journal

Tulip is a personal AI-powered nature and camping journal crafted for people who explore forests, jungles, mountains, waterfalls, open meadows, and wild landscapes.

Designed with a calm, light botanical field-notebook aesthetic (soft creams, muted sage, desaturated forest green, and warm clay accents), Tulip captures spontaneous thoughts quickly without turning into a generic chatbot or social network.

---

## Key Features

1. **Local-First & Offline Resilience**
   - Field notes and photographs are saved directly to local device storage via Room.
   - You can capture photos, draft observations, browse memories, and search through entries with zero internet connectivity.

2. **Selectable AI Providers (Gemini or Local Ollama)**
   - **Gemini (Default Cloud)**: Powered by `gemini-3.5-flash` for instant zero-configuration assistance.
   - **Local Ollama (Gemma 3 4B)**: Powered by the local multimodal model `gemma3:4b` hosted on your private LAN/Wi-Fi workstation. Your text and photos never leave your home network.
   - Seamlessly switch providers in **Settings > AI Provider**; your choice persists across restarts without altering journal content.

3. **Preserve Original Voice**
   - Write in any language or dialect.
   - Tulip **never** silently replaces your original words with AI-generated text.

4. **Fix & Translate to English**
   - Routes through your chosen AI provider (Gemini or Gemma 3 4B).
   - If English: minimally repairs spelling, punctuation, capitalization, and grammar while strictly preserving personal voice and nuances.
   - If non-English or mixed: seamlessly identifies and translates observations into natural English while protecting names, places, and cultural expressions.
   - Applying changes requires an explicit user action; original text remains recoverable.

5. **Contextual "Ask AI" on Photographs**
   - Long-press any attached photograph to open the floating **Ask AI** drawer.
   - Passes the actual photograph and user query to the active multimodal model (Gemini Vision or Gemma 3 4B Vision).
   - Answers are uncertainty-aware and **never** claim wild plants or fungi are safe to consume or touch.
   - Copy or insert responses directly into your field note under `[Field Observation]`.

6. **Explore & Rediscover**
   - Search across journal entries, locations, and tags.
   - Filter by custom observation tags (e.g., `#wildflowers`, `#fungi`, `#canopy`).

7. **Privacy & Data Control**
   - Location tagging is purely optional and manual (no continuous GPS tracking).
   - Local Ollama keeps text and photographs entirely within your home or camp network.
   - Full JSON data export and complete local data erasure options in Settings.

---

## AI Provider Configuration

### 1. Cloud: Google Gemini (Default)

- Configured out of the box via `GEMINI_API_KEY` in `.env` or AI Studio Secrets.
- Defaults to `gemini-3.5-flash`.
- Adjustable in **Settings > Configure Gemini Model Identifier**.

---

### 2. Local: Ollama + Gemma 3 4B

This mode runs AI inference entirely on your own machine — no cloud, no API key, no data leaving your network. Your phone talks directly to Ollama running on your PC/laptop over Wi-Fi.

#### Step 1 — Install Ollama

Download and install Ollama for your OS from [https://ollama.com/download](https://ollama.com/download).

- **Windows**: Run the installer `.exe`, Ollama starts automatically as a background service.
- **macOS**: Open the `.dmg`, drag to Applications, launch it.
- **Linux**: `curl -fsSL https://ollama.com/install.sh | sh`

#### Step 2 — Download the Gemma 3 4B model

Open a terminal and run:

```bash
ollama pull gemma3:4b
```

This downloads ~3.3 GB. Only needs to be done once.

#### Step 3 — Allow Ollama to accept connections from your phone

By default Ollama only listens on `localhost`, so your phone can't reach it even on the same Wi-Fi. You need to set `OLLAMA_HOST=0.0.0.0` to make it listen on all network interfaces.

**Windows (permanent — do this once):**
1. Open Start → search **"Edit the system environment variables"**
2. Click **Environment Variables**
3. Under **User variables**, click **New**
4. Variable name: `OLLAMA_HOST` / Value: `0.0.0.0`
5. Click OK, then restart Ollama (kill it in Task Manager, relaunch from Start)

**macOS / Linux (per session):**
```bash
OLLAMA_HOST=0.0.0.0 ollama serve
```

**macOS / Linux (permanent):**
Add this to your `~/.zshrc` or `~/.bashrc`:
```bash
export OLLAMA_HOST=0.0.0.0
```
Then restart the terminal and run `ollama serve`.

#### Step 4 — Find your PC's local IP address

**Windows PowerShell:**
```powershell
Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -like "192.168.*" -or $_.IPAddress -like "10.*" } | Select-Object IPAddress, InterfaceAlias
```

**macOS / Linux:**
```bash
ip -4 addr show   # or: ifconfig | grep "inet "
```

Your IP will look like `192.168.x.x` or `10.x.x.x`.

#### Step 5 — Configure Tulip on your phone

1. Open Tulip → **Settings → AI Provider**
2. Select **Local Ollama (Gemma 3 4B)**
3. Enter your PC's IP:
   ```
   http://192.168.x.x:11434
   ```
4. Tap **Test Connection** — you should see a green confirmation.

> **Tip**: Open `http://192.168.x.x:11434` in your phone's browser first to confirm reachability. You should see "Ollama is running".

---

## The `local-ai-routing/` folder

The `local-ai-routing/` directory is an **optional Node.js reverse proxy** for the Ollama connection. It is not required for normal use — the Tulip app talks directly to Ollama on port `11434` by default.

### Why does it exist?

It was built to solve two specific scenarios:

1. **Firewall / port restrictions**: If your network or corporate Wi-Fi blocks direct connections to port `11434`, the proxy runs on port `8787` which is less likely to be blocked.
2. **Log sanitization & diagnostics**: The proxy strips Base64 image data and raw journal text from terminal logs before they hit your console, so you can debug connection issues without exposing private content.

### What it does

It's a lightweight Express.js TypeScript server that:
- Listens on your LAN at `http://<your-ip>:8787`
- Forwards `POST /api/chat` and `GET /api/tags` requests upstream to Ollama at `http://127.0.0.1:11434`
- Strips prompt content and Base64 images from console logs for privacy
- Provides setup, status, and teardown CLI scripts (`npm run setup`, `npm run start`, `npm run stop`, `npm run status`)
- Auto-detects your Wi-Fi IP and prints the exact URL to paste into Tulip

### When to use it

Use the proxy if:
- Direct connection to port `11434` fails on your network
- You want the full diagnostics/setup CLI (`npm run setup` checks RAM, pulls the model, runs test inferences, and prints your URL)

Use direct Ollama (`port 11434`) if:
- Everything works — it's simpler with one fewer moving part

### How to run it

```bash
cd local-ai-routing
npm install
npm run setup    # first time: checks requirements, pulls model, runs inference test
npm run start    # starts the proxy server
```

Then in Tulip Settings use `http://<your-ip>:8787` instead of `11434`.

To stop:
```bash
npm run stop
```

---

## Building & Testing

### Requirements
- JDK 17
- Android SDK 36 (minSdk 24)
- Gradle 9.x (via wrapper — use `./gradlew`, not `gradle`)

### Local Commands

```bash
# Run unit & Robolectric tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing credentials)
./gradlew assembleRelease
```

The debug APK is at: `app/build/outputs/apk/debug/app-debug.apk`

---

## GitHub Actions CI/CD

Two automated workflows live in `.github/workflows/`:

### `build.yml` — runs on every push & PR

- Sets up JDK 17, generates a debug keystore, injects `GEMINI_API_KEY` from GitHub Secrets
- Runs Robolectric unit tests
- Builds the debug APK and uploads it as the `tulip-debug-apk` artifact (downloadable from the Actions tab, retained 14 days)

### `releases.yml` — runs on `v*` tag push

- Same setup as above, plus runs verification tests
- If `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` secrets are present → builds a signed release APK
- If not → builds a debug APK as fallback
- Publishes a GitHub Release with the APK attached and auto-generated release notes

#### Creating a release

```powershell
git tag -a v1.0.1 -m "Your release message"
git push origin v1.0.1
```

#### GitHub Secrets

| Secret | Purpose |
| :--- | :--- |
| `GEMINI_API_KEY` | Bakes your Gemini API key into the APK at build time |
| `KEYSTORE_BASE64` | Base64-encoded release keystore (optional, for signed builds) |
| `KEYSTORE_PASSWORD` | Keystore store password |
| `KEY_ALIAS` | Key alias name |
| `KEY_PASSWORD` | Key private password |
