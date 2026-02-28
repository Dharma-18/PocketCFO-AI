package com.example.pocketcfo1

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private var totalBusinessProfit: Int = 0
    private var totalPersonalSpending: Int = 0
    private var isTamil = false
    private var pendingItem: String? = null

    private lateinit var tvProfit: TextView
    private lateinit var tvPersonal: TextView
    private lateinit var etInput: EditText

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            etInput.setText(spokenText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvProfit = findViewById(R.id.tvProfit)
        tvPersonal = findViewById(R.id.tvPersonal)
        etInput = findViewById(R.id.etInput)
        val tvLangToggle = findViewById<TextView>(R.id.tvLangToggle)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnVoice = findViewById<ImageButton>(R.id.btnVoice)
        val chatLayout = findViewById<LinearLayout>(R.id.chatLayout)
        val chatScroll = findViewById<NestedScrollView>(R.id.chatScroll)

        loadData()

        tvLangToggle.setOnClickListener {
            isTamil = !isTamil
            tvLangToggle.text = if (isTamil) "தமிழ்" else "EN"
            val msg = if (isTamil) "வணக்கம்! நான் உங்கள் PocketCFO. இன்று நீங்கள் என்ன செலவு செய்தீர்கள்?" else "Hello! I am your PocketCFO. What did you spend on today?"
            addMessageBubble(msg, false, chatLayout, chatScroll)
        }

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessageBubble(text, true, chatLayout, chatScroll)
                etInput.text.clear()
                processAgenticBrain(text, chatLayout, chatScroll)
            }
        }

        btnVoice.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, if(isTamil) "ta-IN" else "en-IN")
            try { speechLauncher.launch(intent) } catch (e: Exception) {}
        }

        findViewById<Button>(R.id.chipIncome).setOnClickListener { setQuickText("I made a sale of ", etInput) }
        findViewById<Button>(R.id.chipExpense).setOnClickListener { setQuickText("Business expense for ", etInput) }
        findViewById<Button>(R.id.chipPersonal).setOnClickListener { setQuickText("Personal spend: ", etInput) }

        // Initial Greeting
        chatLayout.postDelayed({
            val msg = if (isTamil) "வணக்கம் தர்மா! உங்களின் நிதி விபரங்களை பதிவு செய்ய தொடங்கலாம்." else "Hello Dharma! Ready to log your finances for today?"
            val exists = chatLayout.childCount > 0
            if(!exists) addMessageBubble(msg, false, chatLayout, chatScroll)
        }, 500)
    }

    private fun setQuickText(text: String, et: EditText) {
        et.setText(text)
        et.setSelection(text.length)
        et.requestFocus()
    }

    private fun textToNumeric(text: String): String {
        var processed = text.lowercase()
        val wordMap = mapOf(
            "one" to "1", "two" to "2", "three" to "3", "four" to "4", "five" to "5",
            "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
            "hundred" to "00", "thousand" to "000", "lakh" to "00000"
        )
        wordMap.forEach { (word, num) -> processed = processed.replace(word, num) }
        return processed
    }

    private fun processAgenticBrain(input: String, container: LinearLayout, scroll: NestedScrollView) {
        container.postDelayed({
            val normalizedInput = textToNumeric(input)
            val lower = normalizedInput.lowercase()

            val p = Pattern.compile("(\\d+)\\s*(k|lakh)?")
            val m = p.matcher(lower)

            var detectedAmount = 0
            var foundAmount = false
            if (m.find()) {
                val num = m.group(1)?.toInt() ?: 0
                val unit = m.group(2) ?: ""
                detectedAmount = when(unit) {
                    "k" -> num * 1000
                    "lakh" -> num * 100000
                    else -> num
                }
                foundAmount = true
            }

            if (!foundAmount || detectedAmount == 0) {
                pendingItem = input
                val msg = if (isTamil) "எவ்வளவு தொகை? (₹)" else "Got it. What was the exact amount? (e.g., 500 or 1k)"
                addMessageBubble(msg, false, container, scroll)
                return@postDelayed
            }

            val incomeKeywords = listOf("salary", "sold", "sale", "made", "income", "received", "profit")
            val personalKeywords = listOf("home", "house", "personal", "kids", "gift", "eggs", "milk", "movie", "clothes", "dinner")

            val isIncome = incomeKeywords.any { lower.contains(it) } || (pendingItem != null && incomeKeywords.any { pendingItem!!.lowercase().contains(it) })
            val isPersonal = personalKeywords.any { lower.contains(it) } || (pendingItem != null && personalKeywords.any { pendingItem!!.lowercase().contains(it) })

            val oldProfit = totalBusinessProfit
            val oldPersonal = totalPersonalSpending

            if (isPersonal) {
                if (isIncome) totalPersonalSpending -= detectedAmount else totalPersonalSpending += detectedAmount
            } else {
                if (isIncome) totalBusinessProfit += detectedAmount else totalBusinessProfit -= detectedAmount
            }

            saveData()
            animateNumbers(oldProfit, totalBusinessProfit, tvProfit, isProfit = true)
            animateNumbers(oldPersonal, totalPersonalSpending, tvPersonal, isProfit = false)

            val header = if (isTamil) "📊 நிதி பகுப்பாய்வு\n────────────\n" else "📊 POCKET CFO LOG\n────────────\n"
            val type = if (isPersonal) "Personal" else "Business"
            val body = "✅ Added: ${if(isIncome)"+" else "-"}₹$detectedAmount\nCategory: $type"

            addMessageBubble(header + body, false, container, scroll)
            pendingItem = null
        }, 800)
    }

    private fun addMessageBubble(text: String, isUser: Boolean, container: LinearLayout, scroll: NestedScrollView) {
        val tv = TextView(this)
        tv.text = text
        tv.setPadding(48, 32, 48, 32)
        tv.elevation = 2f
        tv.textSize = 14f
        tv.letterSpacing = 0.02f
        
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 16, 0, 16)
        
        if (isUser) {
            tv.setBackgroundResource(R.drawable.bg_user_bubble)
            tv.setTextColor(Color.parseColor("#1E1B4B")) // primary_indigo
            params.gravity = Gravity.END
            params.marginStart = 140
        } else {
            tv.setBackgroundResource(R.drawable.bg_bot_bubble_pro)
            tv.setTextColor(Color.parseColor("#0F172A")) // text_main
            params.gravity = Gravity.START
            params.marginEnd = 140
        }
        
        tv.layoutParams = params
        
        // Add fade scale animation
        tv.alpha = 0f
        tv.scaleX = 0.9f
        tv.scaleY = 0.9f
        container.addView(tv)
        
        tv.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .withEndAction {
                scroll.postDelayed({ scroll.fullScroll(android.view.View.FOCUS_DOWN) }, 50)
            }.start()
    }

    private fun animateNumbers(startVal: Int, endVal: Int, textView: TextView, isProfit: Boolean) {
        val animator = ValueAnimator.ofInt(startVal, endVal)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            textView.text = "₹ ${animation.animatedValue}"
        }
        animator.start()

        if (isProfit) {
            textView.setTextColor(if (endVal >= 0) Color.WHITE else Color.parseColor("#FCA5A5"))
        }
    }

    private fun updateUI() {
        runOnUiThread {
            tvProfit.text = "₹ $totalBusinessProfit"
            tvPersonal.text = "₹ $totalPersonalSpending"
            tvProfit.setTextColor(if (totalBusinessProfit >= 0) Color.WHITE else Color.parseColor("#FCA5A5"))
        }
    }

    private fun saveData() {
        getSharedPreferences("CFO", Context.MODE_PRIVATE).edit().apply {
            putInt("p", totalBusinessProfit); putInt("s", totalPersonalSpending); apply()
        }
    }

    private fun loadData() {
        val sp = getSharedPreferences("CFO", Context.MODE_PRIVATE)
        totalBusinessProfit = sp.getInt("p", 0); totalPersonalSpending = sp.getInt("s", 0)
        updateUI()
    }
}