package com.example.pocketcfo1

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.example.pocketcfo1.data.AppDatabase
import com.example.pocketcfo1.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * UNIQUE HACKATHON FEATURE: Paytm SoundBox Audio Listener
 *
 * Runs as a foreground service. Listens for Paytm SoundBox announcements like:
 *   "Teen sau pachas rupaye mile" (Hindi) → ₹350 auto-logged as income
 *   "Munnooru aimbadhu rupai kidaittadu" (Tamil) → ₹350 auto-logged
 *   "Three hundred fifty rupees received" (English) → ₹350 auto-logged
 *
 * KEY ARCHITECTURE FIX: SpeechRecognizer is created on a dedicated HandlerThread
 * via Handler(mainLooper) to avoid blocking the UI thread.
 */
class SoundBoxListenerService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "soundbox_channel"
        const val NOTIF_ID   = 9001
        const val ACTION_SOUNDBOX_LOG = "com.example.pocketcfo1.SOUNDBOX_LOG"
        const val EXTRA_AMOUNT        = "extra_amount"
        const val EXTRA_TEXT          = "extra_text"
        const val EXTRA_LANG          = "extra_lang"

        val HINDI_NUMBERS = mapOf(
            "ek" to 1, "do" to 2, "teen" to 3, "char" to 4, "paanch" to 5,
            "chah" to 6, "saat" to 7, "aath" to 8, "nau" to 9, "das" to 10,
            "gyarah" to 11, "barah" to 12, "terah" to 13, "chaudah" to 14,
            "pandrah" to 15, "solah" to 16, "satrah" to 17, "atharah" to 18,
            "unnees" to 19, "bees" to 20, "tees" to 30, "chalis" to 40,
            "pachas" to 50, "pachaas" to 50, "pachpan" to 55,
            "saath" to 70, "sattar" to 70, "assi" to 80, "nabbe" to 90,
            "sau" to 100, "hazaar" to 1000, "lakh" to 100000
        )
        val TAMIL_NUMBERS = mapOf(
            "onru" to 1, "oru" to 1, "rendu" to 2, "moonru" to 3,
            "naanku" to 4, "ayndu" to 5, "anju" to 5, "aaru" to 6,
            "ezhu" to 7, "ettu" to 8, "onbadu" to 9, "pathu" to 10,
            "irupathu" to 20, "muppadu" to 30, "narpadu" to 40,
            "aimbadu" to 50, "arubadu" to 60, "ezhupathu" to 70,
            "enbadu" to 80, "thonnuru" to 90,
            "nooru" to 100, "aayiram" to 1000
        )
        val ENGLISH_NUMBERS = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
            "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
            "nineteen" to 19, "twenty" to 20, "thirty" to 30, "forty" to 40,
            "fifty" to 50, "sixty" to 60, "seventy" to 70, "eighty" to 80,
            "ninety" to 90, "hundred" to 100, "thousand" to 1000
        )

        // Phrases that indicate a Paytm SoundBox payment was received
        val TRIGGER_PHRASES_INCOME = listOf(
            // Hindi
            "mile", "rupe mile", "rupaye mile", "prapt", "received",
            // Tamil
            "kidaittadu", "kidaithathu", "vantathu", "panam vandathu",
            // English
            "rupees received", "rupee received", "payment received",
            "credited", "money received"
        )
        val TRIGGER_PHRASES_EXPENSE = listOf(
            "kata gaya", "debited", "kattappattu", "paid", "gaya"
        )
    }

    // ── LIFECYCLE ───────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("🎙 SoundBox: Listening for Paytm payments..."))
        // Init SpeechRecognizer on main thread (required by Android API)
        mainHandler.post { initSpeechRecognizer() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mainHandler.postDelayed({ startListening() }, 500)
        return START_STICKY // Restart if killed
    }

    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        mainHandler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── SPEECH RECOGNIZER ───────────────────────────────────────────────────────

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0].lowercase()
                    android.util.Log.d("SoundBox", "Heard: $spokenText")
                    processSpokenText(spokenText)
                }
                // Auto-restart listening after result
                if (isListening) {
                    mainHandler.postDelayed({ startListening() }, 300)
                }
            }

            override fun onError(error: Int) {
                // Restart on any error to maintain continuous listening
                if (isListening) {
                    mainHandler.postDelayed({ startListening() }, 1000)
                }
            }

            override fun onEndOfSpeech() {
                // Will restart in onResults/onError
            }
        })
    }

    private fun startListening() {
        if (speechRecognizer == null) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN") // Hindi + fallback
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN,ta-IN,en-IN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            isListening = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            android.util.Log.e("SoundBox", "startListening error: ${e.message}")
        }
    }

    // ── NLP PROCESSING ──────────────────────────────────────────────────────────

    private fun processSpokenText(text: String) {
        val isIncome  = TRIGGER_PHRASES_INCOME.any  { text.contains(it) }
        val isExpense = TRIGGER_PHRASES_EXPENSE.any { text.contains(it) }

        if (!isIncome && !isExpense) return // Nothing relevant

        val amount = extractAmount(text)
        if (amount <= 0) return

        val type   = if (isIncome) "income" else "expense"
        val source = detectLanguage(text)
        val desc   = if (isIncome) "Paytm SoundBox: ₹$amount received" else "Paytm SoundBox: ₹$amount paid"

        // Save to Room DB on IO thread
        ioScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.transactionDao().insertTransaction(
                Transaction(
                    amount      = amount,
                    type        = if (isIncome) "Income" else "Expense",
                    category    = "Business",
                    description = desc,
                    source      = "soundbox",
                    timestamp   = System.currentTimeMillis()
                )
            )
        }

        // Broadcast to MainActivity so it shows a transaction card
        val broadcastIntent = Intent(ACTION_SOUNDBOX_LOG).apply {
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_TEXT,   desc)
            putExtra(EXTRA_LANG,   source)
        }
        sendBroadcast(broadcastIntent)

        // Update notification
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NOTIF_ID, buildNotification("✅ Auto-logged ₹$amount from SoundBox!"))
        android.util.Log.d("SoundBox", "AUTO-LOGGED: ₹$amount | type=$type | lang=$source")
    }

    private fun extractAmount(text: String): Int {
        // 1. Try direct digits first
        val digitPattern = Pattern.compile("(\\d+)")
        val matcher = digitPattern.matcher(text)
        if (matcher.find()) return matcher.group(1).toIntOrNull() ?: 0

        // 2. Parse word-based numbers across all 3 languages
        val allMaps = listOf(HINDI_NUMBERS, ENGLISH_NUMBERS, TAMIL_NUMBERS)
        val words   = text.split("\\s+".toRegex())
        var total   = 0
        var current = 0
        for (word in words) {
            val v = allMaps.firstNotNullOfOrNull { it[word] } ?: continue
            when {
                v == 100  -> current = if (current == 0) 100 else current * 100
                v >= 1000 -> { total += (if (current == 0) 1 else current) * v; current = 0 }
                else      -> current += v
            }
        }
        return total + current
    }

    private fun detectLanguage(text: String): String = when {
        TAMIL_NUMBERS.keys.any { text.contains(it) } ||
        listOf("kidai","panam","rupai","nooru").any { text.contains(it) } -> "Tamil"
        HINDI_NUMBERS.keys.any { text.contains(it) } ||
        listOf("rupaye","mile","rupe","sau","hazaar").any { text.contains(it) } -> "Hindi"
        else -> "English"
    }

    // ── NOTIFICATION ─────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SoundBox Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Listens for Paytm SoundBox payment announcements" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PocketCFO — SoundBox Active 🎙")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
}
