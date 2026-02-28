# 💸 PocketCFO 
**The "WhatsApp" of Accounting for Indian Micro-Businesses**

PocketCFO is a Voice-First, AI-powered accounting companion designed specifically for Kirana stores, tea stalls, and freelancers who find traditional accounting software too intimidating. It replaces confusing ledgers with a simple, familiar chat-bubble interface.

This project was built during a Hackathon to solve the massive problem of unorganized cash flow tracking in the Indian micro-business sector.

---

## ⚡ Core Features

*   🗣️ **Trilingual Voice Logging:** Owners can speak naturally in English, Tamil, or Hindi ("Got 500" or "Sold tea for 50 rupees"). The NLP engine instantly understands the intent and logs the amount.
*   📩 **Invisible UPI Auto-Capture:** Built-in Android `BroadcastReceivers` run in the background, listening for Paytm, PhonePe, and GPay SMS alerts and automatically recording digital income without manual entry.
*   💬 **Conversational Dashboard:** Replaces intimidating Excel spreadsheets with a friendly "WhatsApp-style" chat interface. Green bubbles for money in, red bubbles for money out.
*   📊 **AI-Powered Insights:** Automatically analyzes spending trends and provides concise, actionable local tips to the shop owner.
*   📄 **1-Tap PDF Exports:** Instantly generates professional Daily, Weekly, or Full Financial Reports directly to device storage.

---

## 🛠️ Technology Stack

PocketCFO is built natively for Android to ensure lightning-fast, offline-first performance suitable for budget smartphones.

*   **Kotlin** – Core application logic and Trilingual NLP engine.
*   **Android Jetpack** – UI structure, navigation, and core components.
*   **Room Database (SQLite)** – Secure, offline-first storage of daily transactions.
*   **Kotlin Coroutines** – Asynchronous background data processing.
*   **Broadcast Receivers** – Silently capturing background Paytm/PhonePe SMS alerts.
*   **Android Telephony API** – Extracting merchant names and amounts from text messages.
*   **Android Speech Recognizer** – Powering voice-to-text logging.
*   **Native PDF Documents** – Generating end-of-day financial reports.
*   **MPAndroidChart** – Rendering the visual expense graphs.

---

## 🚀 How to Run the Project

1. Clone this repository to your local machine:
   ```bash
   git clone https://github.com/your-username/PocketCFO.git
   ```
2. Open the project in **Android Studio** (Koala or newer recommended).
3. Connect a physical Android device or start an Emulator.
4. Hit **Run ('app')** in Android Studio.
5. *Note:* Make sure to grant **Microphone**, **SMS**, and **Notification** permissions when prompted to enable voice logging and auto-capture features!

---

## 💡 About 
Built with ❤️ during an intensive Hackathon to bring financial clarity to the undocumented backbone of the Indian economy.
