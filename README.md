# Voice Nav — Native Mobile App

A voice-controlled, navigation-only assistant designed specifically for motorcycle riders in India. 

Designed for high-contrast visibility on handlebar mounts and hands-free helmet voice interaction. Strictly avoids LLMs or generative AI in the navigation engine in favor of **100% deterministic rule-based command parsing**.

---

## 1. Project Structure

```
├── App.tsx                             # Single-screen React Native rider shell
├── app.json                            # Expo app configuration & native permissions
├── package.json                        # Expo & React Native dependencies
├── tsconfig.json                       # TypeScript compiler configuration
├── .gitignore                          # Node, Expo, and Android build ignores
│
├── src/                                # Modular Native / Expo Architecture
│   ├── theme/
│   │   └── colors.ts                   # #12161B (Dark BG), #F4F2EC (Text), #F2A900 (Amber)
│   ├── types/
│   │   └── navigation.ts               # Strongly-typed commands, maneuvers, and sessions
│   ├── components/
│   │   ├── MicrophoneButton.tsx        # High-contrast circular "Ask" / "Listening" button
│   │   └── InlineSettings.tsx          # Local OpenRouteService API key input
│   └── modules/
│       ├── parser/
│       │   └── commandParser.ts        # Deterministic rule-based command engine (Zero AI)
│       ├── voice/
│       │   ├── voiceInput.ts           # Speech recognition interface & simulated fallback
│       │   └── speechOutput.ts         # Hands-free Text-to-Speech audio announcements
│       ├── location/
│       │   └── locationTracker.ts      # Hardware GPS telemetry (Zero fake coordinates)
│       ├── routing/
│       │   └── openRouteService.ts     # OpenRouteService Geocoding & Driving Directions
│       └── navigation/
│           └── navigationEngine.ts     # Route lifecycle, ETA, maneuvers, and error handling
│
└── app/                                # Pre-configured Native Android Gradle Project
    ├── src/main/java/com/example/      # Kotlin Jetpack Compose implementation
    └── src/main/AndroidManifest.xml    # Android audio, speech queries, & GPS permissions
```

---

## 2. Windows Setup & Installation

### Prerequisites on Windows
1. **Node.js (LTS 18 or 20)**: Download and install from [nodejs.org](https://nodejs.org/).
2. **Git for Windows**: Download and install from [git-scm.com](https://git-scm.com/).
3. **VS Code**: Recommended IDE.

### Installation Steps
Open **PowerShell** or **Command Prompt** as Administrator (or standard user in your project folder):

```powershell
# 1. Clone or extract the project
cd "C:\path\to\voice-nav"

# 2. Install project dependencies
npm install
```

---

## 3. How to Start the App

In PowerShell or Command Prompt inside the project directory:

```powershell
# Start the Expo development server
npx expo start
```

This launches the Expo CLI Metro Bundler and prints a QR code in the terminal.

---

## 4. How to Test on an Android Phone

### Option A: Using Expo Go (Quickest Interface Testing)
1. Install **Expo Go** from Google Play Store on your Android phone.
2. Ensure your Android phone and Windows PC are connected to the **same Wi-Fi network** (or start with `npx expo start --tunnel`).
3. Open the Expo Go app on your phone and tap **"Scan QR code"**.
4. Scan the QR code shown in your terminal. The Voice Nav single-screen interface will open instantly.

### Option B: Using an Android Emulator on Windows
1. Open Android Studio and launch any Android Virtual Device (AVD).
2. Run `npx expo start` and press `a` in your terminal to open on the Android emulator.

### Option C: Building a Standalone Native Android APK
1. To build a native Android APK directly via Gradle on Windows:
   ```powershell
   gradle :app:assembleDebug
   ```
2. The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
3. Install on a connected Android phone using `adb install app/build/outputs/apk/debug/app-debug.apk`.

---

## 5. Feature Implementation Status

| Feature | Status | Notes |
| :--- | :--- | :--- |
| **High-Contrast Dark Rider UI** | ✅ Implemented | `#12161B` background, `#F4F2EC` main text, `#F2A900` amber accent |
| **Large Instruction Text Area (~30px)** | ✅ Implemented | High-contrast, easily legible from motorcycle handlebars |
| **"You said: ..." Feedback Line** | ✅ Implemented | Instant visual confirmation of parsed rider command |
| **Circular "Ask" / "Listening" Button** | ✅ Implemented | Large touch target (>120dp), turns amber and displays "Listening" |
| **Rule-Based Command Parser** | ✅ Implemented | 100% deterministic TypeScript/Kotlin parsing. Zero LLM / cloud inference |
| **Supported Voice Commands** | ✅ Implemented | `"Take me to..."`, `"Nearest petrol pump"`, `"How far?"`, `"Next turn"`, `"Repeat"`, `"Reroute"`, `"Stop navigation"` |
| **Inline OpenRouteService Key Settings** | ✅ Implemented | Stored strictly in local device storage (`AsyncStorage` / `SharedPreferences`). Never hardcoded |
| **Speech Output (TTS)** | ✅ Implemented | Audio announcement of maneuvers, ETA, and errors |
| **Hardware GPS Location Tracking** | ✅ Implemented | Real GPS coordinates via `expo-location` / `FusedLocationProviderClient`. No fabricated data |
| **Interactive Map (Carto Dark / OSM)** | ✅ Implemented | High-contrast real interactive map renderer (OSMDroid), dark tile theme, multi-touch gestures |
| **Real GPS Telemetry & Pin** | ✅ Implemented | Real GPS hardware coordinates via `LocationTracker` with live user marker & recenter button |
| **Live Route Polyline & Destination** | ✅ Implemented | Real OpenRouteService directions rendered as amber polyline with destination marker |
| **Simulated Voice Mode (Milestone 1)** | ✅ Implemented | Clearly labelled fallback when running in emulators without native speech recognition services |
| **Native Background Continuous Audio** | ⏳ Milestone 2 | Requires custom Android foreground service with `RECORD_AUDIO` and `ACCESS_BACKGROUND_LOCATION` |
| **Offline Vector Map Tile Caching** | ⏳ Milestone 2 | Local tile cache storage on SD card/internal storage for zero-connectivity highway stretches |

---

## 6. Security & Privacy

- **No Hardcoded API Keys**: OpenRouteService API keys are stored locally on the device and never committed to source control.
- **Privacy-First Telemetry**: Location coordinates are only processed locally for calculating routes and never uploaded to external analytics services.
