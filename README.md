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
   - **Local Ollama (Gemma 3 4B)**: Powered by the local multimodal model `gemma3:4b` hosted on your private LAN/Wi-Fi workstation.
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

### 2. Local: Ollama (Gemma 3 4B Multimodal)
- **Model Requirement**: `gemma3:4b` (supports both text and image input). Run on your host computer:
  ```bash
  ollama pull gemma3:4b
  ```
- **Network Setup**:
  1. Ensure Ollama listens on your local network:
     - On macOS/Linux: `OLLAMA_HOST=0.0.0.0 ollama serve`
     - On Windows: set environment variable `OLLAMA_HOST=0.0.0.0`
  2. Find your computer's local IP address (e.g. `192.168.1.100`).
  3. In Tulip, navigate to **Settings > AI Provider**, select **Local Ollama (Gemma 3 4B)**, and enter:
     `http://192.168.1.100:11434`
  4. Tap **Test Connection** to verify that the server is reachable and `gemma3:4b` is installed.
- *Note on Android Networking*: `localhost` on Android points to the phone itself. To connect to an Ollama server running on a laptop or desktop, enter the computer's reachable LAN IP address or a secure HTTPS tunnel. Tulip's network security configuration explicitly permits cleartext HTTP for private RFC 1918 LAN addresses (`192.168.*`, `10.*`, `172.16-31.*`) while strictly blocking cleartext to the public internet.

---

## Building & Testing

### Requirements
- JDK 17
- Android SDK 36 (minSdk: 24)
- Gradle 9.x+

### Local Commands
```bash
# Run unit & Robolectric tests
gradle testDebugUnitTest

# Build debug APK
gradle assembleDebug

# Build release APK (requires signing credentials)
gradle assembleRelease
```

The debug APK output is located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## GitHub Actions CI/CD

Tulip includes two automated GitHub Actions workflows located in `.github/workflows/`:

### 1. Push-triggered Builds (`build.yml`)
- Triggers on every push and pull request to any branch.
- Sets up JDK 17 and Android SDK.
- Executes automated Robolectric and unit tests.
- Compiles the debug APK and uploads it as an artifact named `tulip-debug-apk`.

### 2. Tag-triggered Releases (`releases.yml`)
- Triggers whenever a version tag matching `v*` (e.g. `v1.0.0`) is pushed.
- Runs verification tests and builds the release APK.
- Publishes a formal GitHub Release with the APK attached and auto-generated release notes.

#### PowerShell Commands for Creating & Pushing a Release
```powershell
git tag v1.0.0
git push origin v1.0.0
```

#### GitHub Secrets for Release Signing (Optional)
To produce a cryptographically signed release APK:
- `KEYSTORE_BASE64`: Base64-encoded keystore file
- `KEYSTORE_PASSWORD`: Keystore store password
- `KEY_ALIAS`: Key alias name
- `KEY_PASSWORD`: Key private password
