# 🟩 BLOCKY WEATHER

> A hyper-tactile, retro-modern Android weather application powered by OpenGL ES 2.0 graph paper grids, physical block deformations, location-adaptive quips, matrix typography severe alerts, and Material Design 3.

---

## 🌟 Key Features

- **🎮 OpenGL ES 2.0 Weather Physics Grid**
  - **Graph Paper Rendering**: High-performance fragment-shader grid rendering 24 FPS / 60 FPS graph paper backdrop.
  - **Reactive Block Physics**: Live weather conditions physically deform grid blocks — squishy oscillating blocks for rain, rigid sheared spikes for wind, drifting mist bands for fog, and high-frequency jitter for electrical thunderstorms.
  - **Touch-Excited Hops**: Interactive touch gestures propel tactile blocks stepping fluidly across grid coordinates.

- **📜 24-Block Progressive Pull-To-Refresh**
  - Zero-oscillation custom pull-to-refresh box with a 24-block progressive matrix indicator that fills dynamically as you pull.

- **⚡ Severe Weather Alert Matrix Overlay**
  - High-contrast emergency hazard overlay in matrix typography triggered instantly when extreme weather conditions (Thunderstorms, Heavy Rain, Gale Winds, High UV, Hazardous AQI, Heatwaves, Freezing Cold) are detected.

- **💬 Location & Climate-Adaptive Weather Quips Engine**
  - Smart, contextual quip generator that considers temperature range, humidity, UV index, wind speed, air quality, and local climate tendencies before delivering hilarious, relatable nudges.

- **☀️ Inverse Solar & Lunar Arc Visualizer**
  - Clean blocky Sun and Moon representations traversing a smooth inverted daylight curve with a blank block aura and daylight percentage telemetry.

- **📌 Pinned Status Bar Notifications & Auto-Refresh**
  - Pinned system notifications with real-time temperature updates and severe hazard alerts. Customizable background sync intervals (Off, 15m, 30m, 1h, 3h).

---

## 🏗️ Technical Architecture

**Blocky Weather** is engineered following modern Android architecture patterns (MVVM + Clean Architecture) with strict separation of concerns:

```
├── app
│   ├── src/main/java/com/example
│   │   ├── data
│   │   │   ├── api          # Retrofit Open-Meteo & Geocoding API service clients
│   │   │   ├── local        # Room Database entities, DAOs & DataStore preferences
│   │   │   └── repository   # Unified Weather & Location repository pattern
│   │   ├── gl               # OpenGL ES 2.0 Renderer & Touch Physics Controller
│   │   ├── notification     # System Notification Manager & Alert Channels
│   │   ├── ui
│   │   │   ├── components   # Custom M3 Composable Visuals & Matrix Drawers
│   │   │   ├── screens      # Home Screen, Search Screen & Settings Screen
│   │   │   ├── theme        # Material 3 Color Schemes, Silkscreen Typography
│   │   │   └── viewmodel    # Weather & Settings ViewModels with StateFlow
│   │   └── util             # Weather Quip Engine & Location Matrix Helpers
```

### 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Graphics & Rendering**: OpenGL ES 2.0 (`GLSurfaceView` + custom vertex/fragment shaders)
- **Local Database**: Room DB (Offline caching & search history)
- **Preferences**: DataStore (Unit preferences, auto-refresh interval, theme settings)
- **Networking**: Retrofit 2 + Ktor / Moshi Converter
- **Asynchronous Flow**: Kotlin Coroutines + `StateFlow` / `SharedFlow`
- **Typography**: Silkscreen pixel typography

---

## 🚀 Building & Running

### Prerequisites
- **Android Studio**: Ladybug / 2024.2+ or IntelliJ IDEA with Android plugin
- **JDK**: JDK 17
- **Android SDK**: `compileSdk = 35`, `minSdk = 26`

### Build Command (CLI)
```bash
# Debug APK Build
gradle assembleDebug

# Run Unit Tests
gradle testDebugUnitTest
```

---

## 📦 GitHub Actions Release APK Workflow

### Creating a Release Keystore

If you don't already have a release keystore, you can create one using `keytool` (included with Java / JDK):

```bash
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias blockykey
```

It will ask you to set:
- **Keystore Password** (e.g. `MySecurePassword123`)
- **First & Last Name, Organization** (can be anything or your name)
- **Key Password** (press Enter to use the same password as the keystore)

---

### Setting Up Required Repository Secrets

To enable signed release APK generation, add the following **Repository Secrets** in your GitHub repository (**Settings -> Secrets and variables -> Actions -> New repository secret**):

1. **`RELEASE_KEYSTORE_BASE64`**: The Base64-encoded string of your `.jks` or `.keystore` file.
   *Commands by OS:*
   - **Windows (PowerShell)**:
     ```powershell
     [Convert]::ToBase64String([IO.File]::ReadAllBytes("path\to\release-key.jks")) | Set-Clipboard
     ```
   - **Windows (CMD)**:
     ```cmd
     certutil -encode path\to\release-key.jks keystore_b64.txt
     ```
   - **macOS**:
     ```bash
     base64 -i path/to/release-key.jks
     ```
   - **Linux**:
     ```bash
     base64 -w 0 path/to/release-key.jks
     ```
   - **Python (Works everywhere)**:
     ```python
     python -c "import base64; print(base64.b64encode(open('release-key.jks','rb').read()).decode())"
     ```
2. **`RELEASE_KEYSTORE_PASSWORD`**: Keystore password.
3. **`RELEASE_KEY_ALIAS`**: Key alias name in the keystore.
4. **`RELEASE_KEY_PASSWORD`**: Password for the key alias.

### Triggering a Release
- Push a tag starting with `v` (e.g. `git tag v1.0.0 && git push origin v1.0.0`), or
- Manually trigger the **Build Signed Release APK** workflow from the GitHub **Actions** tab.

---

## 📄 License
Designed & Developed for Google AI Studio Build.
