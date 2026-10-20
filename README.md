# Nappy Food Journal 🥑

A minimalist, high-craft food and calorie tracking application for Android powered by Jetpack Compose and Gemini AI.

Inspired by clean studio aesthetics, **Nappy Food Journal** removes the friction from nutritional logging with natural language input, instant deterministic macro breakdowns, evidence-backed calorie estimation, and streak tracking.

---

## ✨ Features

- **Natural Language Food Input**: Log meals as you naturally think of them (e.g., *"2 poached eggs on sourdough with a side of avocado"*).
- **Hybrid Nutrition Pipeline**:
  - **Deterministic Calculator**: Accurately computes exact macros when precise portions are given.
  - **Gemini AI Integration**: Intelligently parses messy real-world food descriptions, portion sizes, and preparation styles.
  - **Evidence Engine**: Validates nutritional data against verifiable web queries and reference sources (supported by Tavily search integration).
- **Minimalist Studio Design**: Clean typography, calm warm-toned canvas palette, custom micro-interactions, and Lottie animations (celebrating streaks, loading states, and trophies).
- **Home Screen Calorie Widget**: Quick-glance daily progress directly from the Android home screen.
- **Privacy-First**: Operates directly on-device with your personal API keys stored strictly in your local environment.

---

## 🛠️ Tech Stack & Architecture

- **Platform**: Android (Min SDK 26, Target SDK 35)
- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose + Material 3 Design
- **AI & ML**: 
  - Google Gemini API (`gemini-2.5-flash` / `gemini-1.5-flash`)
  - ML Kit Barcode Scanning & TensorFlow Lite models
- **Networking & Serialization**: OkHttp 4, Kotlinx Serialization JSON
- **Animations**: Lottie Compose
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Ladybug / Jellyfish or newer recommended)
- **Android SDK** API 35
- **JDK**: Java 17

### Installation & Setup

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd nappy-food-journal
   ```

2. **Configure API Keys**:
   Copy `local.properties.example` to `local.properties`:
   ```bash
   cp local.properties.example local.properties
   ```
   Add your keys in `local.properties`:
   ```properties
   sdk.dir=PATH_TO_YOUR_ANDROID_SDK
   gemini_api_key=YOUR_GEMINI_API_KEY
   tavily_api_key=YOUR_TAVILY_API_KEY # Optional
   ```
   > ⚠️ **Note**: `local.properties` is ignored by Git to ensure your credentials are never committed.

3. **Build the Project**:
   Open the project in Android Studio or compile via command line:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run Unit Tests**:
   ```bash
   ./gradlew test
   ```

---

## 🔒 Security & Privacy

- All sensitive credentials and API keys are read via Gradle `buildConfigField` from `local.properties` or environment variables at build time.
- Secrets are never hardcoded into repository source files.
- Keystores, signing certificates, and local machine SDK paths are strictly excluded via `.gitignore`.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
