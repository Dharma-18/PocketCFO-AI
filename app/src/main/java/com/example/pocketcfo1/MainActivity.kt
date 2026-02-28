package com.example.pocketcfo1

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private var totalBusinessProfit: Int = 0
    private var totalPersonalSpending: Int = 0
    private var isTamil = false // Language Toggle State

    private lateinit var tvProfit: TextView
    private lateinit var tvPersonal: TextView
    private lateinit var tvLang: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize UI
        tvProfit = findViewById(R.id.tvProfit)
        tvPersonal = findViewById(R.id.tvPersonal)
        tvLang = findViewById(R.id.tvLang)
        val btnSend = findViewById<Button>(R.id.btnSend)
        val etInput = findViewById<EditText>(R.id.etInput)
        val chatLayout = findViewById<LinearLayout>(R.id.chatLayout)
        val chatScroll = findViewById<NestedScrollView>(R.id.chatScroll)

        // 2. Load Saved Data (Persistence)
        loadFinancialData()

        // 3. Language Switcher Logic
        tvLang.setOnClickListener {
            isTamil = !isTamil
            tvLang.text = if (isTamil) "தமிழ் | EN" else "EN | தமிழ்"
            val welcome = if (isTamil) "வணக்கம் தர்மா! நான் உங்கள் பாக்கெட் சிஎஃப்ஓ (PocketCFO)." else "Welcome Dharma! I am your PocketCFO."
            addMessageBubble(welcome, false, chatLayout, chatScroll)
        }

        // 4. Initial Welcome
        addMessageBubble("📊 POCKET CFO SYSTEM ONLINE\nType or Speak to log your business activity.", false, chatLayout, chatScroll)

        btnSend.setOnClickListener {
            val userText = etInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                addMessageBubble(userText, true, chatLayout, chatScroll)
                etInput.text.clear()
                processIntelligentLogic(userText, chatLayout, chatScroll)
            }
        }
    }

    private fun processIntelligentLogic(input: String, container: LinearLayout, scroll: NestedScrollView) {
        container.postDelayed({
            val lower = input.lowercase()

            // Extract Number (Handles 30k, 500, etc)
            val p = Pattern.compile("(\\d+)\\s*(k)?")
            val m = p.matcher(lower)
            var amount = 0
            if (m.find()) {
                val num = m.group(1)?.toInt() ?: 0
                amount = if (m.group(2) == "k") num * 1000 else num
            }

            if (amount == 0) {
                val msg = if (isTamil) "தொகையை குறிப்பிடவும் (எ.கா. 500)" else "Please mention an amount (e.g., 500)"
                addMessageBubble(msg, false, container, scroll)
                return@postDelayed
            }

            // Logic matching your "Manager & User" views
            val category: String
            val insight: String
            val isIncome: Boolean

            when {
                lower.contains("sold") || lower.contains("sale") || lower.contains("get") || lower.contains("salary") -> {
                    isIncome = true
                    category = if (isTamil) "வணிக வருவாய்" else "BUSINESS REVENUE"
                    totalBusinessProfit += amount
                    insight = if (isTamil) "அருமை! இன்று உங்கள் வருமானம் அதிகரித்துள்ளது." else "💰 Strong cash inflow. Your daily revenue is up!"
                }
                lower.contains("home") || lower.contains("personal") || lower.contains("family") || lower.contains("eggs") -> {
                    isIncome = false
                    category = if (isTamil) "தனிப்பட்ட செலவு" else "PERSONAL LOG"
                    totalPersonalSpending += amount
                    insight = if (isTamil) "இது உங்கள் தனிப்பட்ட செலவு, வணிக லாபத்தில் சேராது." else "🏷️ Personal expense detected. Isolated from business tax logs."
                }
                else -> {
                    isIncome = false
                    category = if (isTamil) "வணிக செலவு" else "BUSINESS EXPENSE"
                    totalBusinessProfit -= amount
                    insight = if (isTamil) "இந்த செலவு உங்கள் வணிக கணக்கில் சேர்க்கப்பட்டது." else "📉 Business cost recorded. You are within your weekly budget."
                }
            }

            saveFinancialData()
            updateDashboard()

            val header = if (isTamil) "📊 நிதி பகுப்பாய்வு\n──────────────\n" else "📊 FINANCIAL ANALYSIS\n──────────────\n"
            val sign = if (isIncome) "+" else "-"
            val response = "${header}ENTRY: $category\nAMOUNT: $sign$amount\nSTATUS: Verified\n\n$insight"

            addMessageBubble(response, false, container, scroll)
        }, 1000)
    }

    private fun updateDashboard() {
        runOnUiThread {
            tvProfit.text = "₹ $totalBusinessProfit"
            tvPersonal.text = "₹ $totalPersonalSpending"
            tvProfit.setTextColor(if (totalBusinessProfit >= 0) Color.parseColor("#10B981") else Color.parseColor("#EF4444"))
        }
    }

    private fun saveFinancialData() {
        val sharedPref = getSharedPreferences("PocketCFOData", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("profit", totalBusinessProfit)
            putInt("personal", totalPersonalSpending)
            apply()
        }
    }

    private fun loadFinancialData() {
        val sharedPref = getSharedPreferences("PocketCFOData", Context.MODE_PRIVATE)
        totalBusinessProfit = sharedPref.getInt("profit", 0)
        totalPersonalSpending = sharedPref.getInt("personal", 0)
        updateDashboard()
    }

    private fun addMessageBubble(text: String, isUser: Boolean, container: LinearLayout, scrollView: NestedScrollView) {
        val textView = TextView(this)
        textView.text = text
        textView.setPadding(40, 30, 40, 30)
        textView.textSize = 15f
        textView.elevation = 4f
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 15, 0, 15)
        if (isUser) {
            textView.setBackgroundResource(R.drawable.bg_user_bubble)
            textView.setTextColor(Color.WHITE)
            params.gravity = Gravity.END
            params.marginStart = 120
        } else {
            textView.setBackgroundResource(R.drawable.bg_bot_bubble_pro)
            textView.setTextColor(ContextCompat.getColor(this, R.color.chat_black))
            params.gravity = Gravity.START
            params.marginEnd = 120
        }
        textView.layoutParams = params
        container.addView(textView)
        scrollView.post { scrollView.fullScroll(android.view.View.FOCUS_DOWN) }
    }
}