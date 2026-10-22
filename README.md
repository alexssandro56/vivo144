# 🚀 Vivo & iQOO 144FPS Unlocker

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material Design 3](https://img.shields.io/badge/Design-Material%203-7C3AED?logo=materialdesign&logoColor=white)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A powerful performance optimization and display refresh rate management utility designed specifically for **Vivo** and **iQOO** smartphones running **OriginOS** and **FuntouchOS**. Unlock true **144Hz / 120Hz / 90Hz** high refresh rates in games and apps, manage floating real-time FPS overlays, configure per-game performance profiles, and monitor thermal/battery metrics with ease.

---

## ✨ Features Overview

### 🎯 1. Display Refresh Rate & High FPS Unlock
- **1-Click Refresh Rate Override**: Force display refresh rates to **60Hz, 90Hz, 120Hz, or 144Hz** or reset to dynamic **Auto/LTPO**.
- **OriginOS & FuntouchOS Optimization**: Seamlessly applies system display settings (`peak_refresh_rate`, `min_refresh_rate`, `user_refresh_rate`) via Android Secure Settings.
- **Game Frame Rate Unlock**: Bypasses 60 FPS caps in supported games and applications.
- **Failsafe System Restore**: One-tap restore button instantly reverts display and system properties back to factory defaults.

---

### 🔍 2. Live Compatibility Engine & Google Search Grounding
- **AI-Powered Grounded Verification**: Dynamically verifies smartphone hardware and firmware compatibility against live Google Search specifications via Gemini.
- **Supported Models Catalog**: Pre-verified database of flagship & gaming devices:
  - **iQOO Flagships**: iQOO 13, iQOO 12/12 Pro, iQOO 11/11 Pro, iQOO 9/9 Pro, iQOO 7
  - **iQOO Neo Series**: Neo 9S Pro+, Neo 9 Pro, Neo 9, Neo 8/8 Pro, Neo 7 Pro, Neo 6
  - **iQOO Z Series**: Z9 Turbo+, Z9 Turbo, Z9, Z8
  - **Vivo X Series**: X100 Ultra, X100 Pro, X100, X90 Pro+, X90, X80 Pro
  - **Vivo T & V Series**: T3 Ultra, T3 Pro, V40 Pro, V30 Pro
- **Proactive Device Alerts**: Clear visual warnings and actionable ADB workarounds when running on unlisted variants or generic non-Vivo Android devices.
- **Interactive Model Search**: Search and test any custom phone model to inspect its refresh rate limits and Ultra Game Mode capabilities.

---

### 📊 3. Real-Time Floating FPS Overlay
- **Live Performance HUD**: Movable on-screen floating overlay showing real-time **FPS**, **Frame Time (ms)**, **Jitter**, and **Battery Temperature (°C)**.
- **Interactive Frame Graph**: Live sparkline chart showing instantaneous frame pacing dips and stability metrics.
- **Customizable Appearance**: Adjustable HUD opacity, scale, accent color themes, and edge-snapping dock.
- **Game Auto-Launch**: Automatically turns on the overlay when a recognized game starts and disables it when exiting.

---

### 🎮 4. Per-Game Performance Profiles
- **Game Library Manager**: Automatically detects installed games and allows custom tuning per title.
- **Individual Target Frame Rates**: Configure 60 FPS, 90 FPS, 120 FPS, or 144 FPS per specific game.
- **Touch Sampling & Response**: Boost touch response rates for competitive gaming (MOBA, FPS, Battle Royale).
- **Resolution & Frame Interpolation (MEMC)**: Tailor visual fidelity and motion smoothing preferences on supported chipsets.

---

### ⚡ 5. Performance Modes & Tuning
- **Ultra / Monster Mode**: Maximum CPU/GPU clock boost and high-priority frame rendering for esports titles.
- **Performance Mode**: Balanced high-refresh gaming with intelligent thermal management.
- **Balanced Mode**: Smooth 90Hz/120Hz daily experience with moderate battery consumption.
- **Battery Saver Mode**: Locks display to efficient refresh rates with aggressive background optimization.

---

### 🔌 6. ADB & Shizuku Integration
- **Zero-Root Required**: Operates completely without root via standard Android `WRITE_SECURE_SETTINGS` permission.
- **Wireless ADB Helper**: Step-by-step pairing and connection wizard for Android 11+ Wireless Debugging directly from the device.
- **Shizuku Integration**: Direct integration with Shizuku for elevated system command execution without a PC.
- **Built-in Diagnostic Tests**: Interactive permission checker and terminal command generator for quick PC setup.

---

### 🌡️ 7. Thermal & Battery Health Diagnostics
- **Thermal Monitor**: Real-time battery and CPU thermal zone telemetry with customizable over-temperature safety alerts.
- **Battery Analytics**: Real-time charging/discharging current (mA), power consumption (W), voltage, and battery health state.
- **Throttling Detection**: Visual indicator alerts when thermal throttling or battery protection limits display refresh rates.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.0+ (100% Coroutines & Flow)
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: Clean Architecture / MVVM with Unidirectional Data Flow (UDF)
- **Local Persistence**: Jetpack Room Database for per-game configurations and overlay preferences
- **AI Grounding**: Gemini API with Google Search Grounding for real-time device specifications
- **Hardware Integration**: Android DisplayManager, BatteryManager, WindowManager Overlay, ADB Shell & Shizuku API
- **Testing**: Robolectric & Roborazzi for local JVM screenshot and Critical User Journey (CUJ) verification

---

## 🚀 Getting Started

### Prerequisites
1. Android 10 (API level 29) or higher (Android 12+ recommended for OriginOS / FuntouchOS).
2. A supported Vivo / iQOO smartphone or any high-refresh-rate Android device.
3. USB Debugging or Wireless Debugging enabled for granting `WRITE_SECURE_SETTINGS`.

### Setup & Permission Grant

1. **Install the APK** on your device.
2. Grant the required Secure Settings permission via **Wireless ADB**, **Shizuku**, or via a PC using ADB:

```bash
adb shell pm grant com.aistudio.vivo.fps android.permission.WRITE_SECURE_SETTINGS
```

3. (Optional) For floating overlay support, grant the "Display over other apps" permission when prompted in the app.
4. Launch the app, select your preferred refresh rate or profile, and enjoy high-FPS gaming!

---

## 🔒 Security & Privacy

- **No Root Required**: All tweaks utilize standard Android system APIs and secure settings.
- **Completely Offline-Ready**: Works fully offline with built-in device specifications database; online search grounding is only triggered when explicitly requested.
- **Safe Reversible Changes**: Built-in 1-tap restore immediately reverts all modified settings back to system defaults.

---

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
