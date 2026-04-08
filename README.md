<div align="center">
  <img src="https://img.shields.io/badge/Pocket-CFO-1E1B4B?style=for-the-badge&logoColor=white" alt="PocketCFO Logo" width="200"/>
  <h1>PocketCFO-AI</h1>
  <p><b>The "WhatsApp" of Accounting for Indian Micro-Businesses.</b></p>
  
  [![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)](#)
  [![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?&style=flat-square&logo=kotlin&logoColor=white)](#)
  [![Status](https://img.shields.io/badge/status-active-success.svg?style=flat-square)](#)
  [![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](#)
</div>

<br/>

## 📖 Introduction
**PocketCFO-AI** is a Voice-First, AI-powered financial companion specifically engineered for Kirana stores, tea stalls, and local freelancers. Built to eliminate the friction of traditional accounting software, it uses a hyper-intuitive, familiar chat-based interface. Simply tap the mic, speak your transaction in your native language, and let the AI do the heavy lifting.

---

## ✨ Key Features

*   🗣️ **Trilingual Voice Logging** – Speak naturally in **English, Tamil, or Hindi** (`"Got ₹500 from Table 4"`). The built-in NLP engine auto-detects the amount, intent, and category instantly.
*   📩 **Invisible UPI Auto-Capture** – A secure background listener intercepts Paytm, PhonePe, and GPay SMS alerts, logging digital payments silently without user intervention.
*   💬 **Conversational Dashboard** – No scary ledgers. Transactions appear as familiar chat bubbles—Green for income, Red for expense.
*   📊 **AI-Powered Insights** – Contextual, localized financial tips based on weekly spending trends.
*   📄 **1-Tap Report Export** – Generate and download professional Daily, Weekly, or Full Financial PDF reports straight to local storage.

---

## 🛠️ Technology Stack

We leveraged native Android technologies to ensure lightning-fast, offline-first performance suitable for budget smartphones in diverse network conditions.

<div align="center">
  <a href="https://skillicons.dev">
    <img src="https://skillicons.dev/icons?i=kotlin,androidstudio,sqlite,materialui,github,figma" alt="Tech Stack Core" />
  </a>
</div>

<br/>

### Core Architecture & Libraries
- **Language:** [Kotlin](https://kotlinlang.org/) (Trilingual NLP regex engine & core logic)
- **Framework:** Android Jetpack / Native SDK
- **Database:** [Room (SQLite)](https://developer.android.com/training/data-storage/room) for secure, offline-first persistent storage
- **Asynchrony:** Kotlin Coroutines (Background SMS parsing & thread management)
- **Background Tasks:** Android `BroadcastReceiver` (Silent UPI SMS capture)
- **Visualization:** MPAndroidChart (Trend analysis graphs)
- **Export:** Native Android PDF Document API

---

## 🚀 Installation & Setup

Because this is a native Android application, you will need Android Studio to run and compile the project.

```bash
# 1. Clone the repository
git clone https://github.com/Dharma-18/PocketCFO-AI.git

# 2. Navigate to the project directory
cd PocketCFO-AI
```

### Running on Android Studio:
1. Open **Android Studio** (Flamingo or newer recommended).
2. Select **File > Open** and choose the `PocketCFO-AI` directory.
3. Allow Gradle to sync and download necessary dependencies.
4. Connect a physical Android device via USB/Wi-Fi Debugging, or launch an Emulator.
5. Click the **Run ('app')** ▶️ button.

> **Note:** Upon first launch, ensure you grant **Microphone**, **SMS Read**, and **Notification** permissions to fully enable voice logging and auto-capture functionalities.

---

## 📂 Project Structure

```text
PocketCFO-AI/
├── app/
│   ├── src/main/java/com/example/pocketcfo1/
│   │   ├── data/          # Room DB, DAOs, Entities
│   │   ├── viewmodel/     # MainViewModel & NLP Logic
│   │   ├── sms/           # BroadcastReceivers for Auto-UPI
│   │   └── ui/            # Fragments (Home, Logs, Tips, Profile)
│   ├── src/main/res/      # Layout XMLs, Drawables, Values & Themes
│   └── AndroidManifest.xml
├── build.gradle.kts       # App-level dependencies
└── settings.gradle.kts    # Project settings
```

---

## 🤝 Contributing
Contributions, issues, and feature requests are welcome! 
1. Fork the project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## ⚠️ Troubleshooting

- **Voice Mic Not Working:** Verify that Google Speech Services is installed and updated on your Android device/emulator. Ensure microphone permissions are explicitly granted in App Settings.
- **SMS Auto-Capture Fails:** Check if the physical device allows background broadcast receivers (some OEMs restrict this by default). Ensure `RECEIVE_SMS` is granted.

---

## 👨‍💻 Author

**Dharmalingam (Dharma-18)**
- GitHub: [@Dharma-18](https://github.com/Dharma-18)

---
<div align="center">
  <i>Built with ❤️ to empower local businesses.</i>
</div>
