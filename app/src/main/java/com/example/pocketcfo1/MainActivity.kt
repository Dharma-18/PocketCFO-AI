package com.example.pocketcfo1

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSend = findViewById<Button>(R.id.btnSend)
        val etInput = findViewById<EditText>(R.id.etInput)
        val chatLayout = findViewById<LinearLayout>(R.id.chatLayout)
        val chatScroll = findViewById<NestedScrollView>(R.id.chatScroll)

        // Show welcome message immediately
        addMessageBubble("Hello Dharma! I am your PocketCFO. Type your daily business activity to get started.", false, chatLayout, chatScroll)

        btnSend.setOnClickListener {
            val userText = etInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                addMessageBubble(userText, true, chatLayout, chatScroll)
                etInput.text.clear()

                chatLayout.postDelayed({
                    val aiResponse = generateAgenticResponse(userText)
                    addMessageBubble(aiResponse, false, chatLayout, chatScroll)
                }, 1000)
            }
        }
    }

    private fun generateAgenticResponse(input: String): String {
        val lower = input.lowercase()
        val header = "🤖 PocketCFO\n──────────────\n"

        // 1. Extract Amount (Looks for any numbers in the text)
        val p = Pattern.compile("\\d+")
        val m = p.matcher(input)
        val amount = if (m.find()) m.group() else "0"

        // 2. Intelligent Category Detection
        val category: String
        val insight: String
        val type: String

        when {
            lower.contains("clothes") || lower.contains("personal") || lower.contains("gift") -> {
                category = "Personal"
                type = "Expense"
                insight = "💡 CFO Insight: I've flagged this as Personal. It won't be deducted from your business profits."
            }
            lower.contains("flour") || lower.contains("milk") || lower.contains("stock") -> {
                category = "Inventory"
                type = "Expense"
                insight = "💡 CFO Insight: Inventory costs are tracked. You are $amount closer to your stock limit."
            }
            lower.contains("sold") || lower.contains("sale") || lower.contains("made") || lower.contains("get") -> {
                category = "Revenue"
                type = "Income"
                insight = "💡 CFO Insight: Great sale! Your daily revenue is trending upwards."
            }
            else -> {
                category = "General"
                type = "Expense"
                insight = "💡 CFO Insight: Transaction recorded. Keep logging for a full profit report."
            }
        }

        val sign = if (type == "Income") "+" else "-"
        return "${header}✅ Transaction Logged\n• Item: Found in text\n• Amount: $sign$amount\n• Category: $category\n\n$insight"
    }

    private fun addMessageBubble(text: String, isUser: Boolean, container: LinearLayout, scrollView: NestedScrollView) {
        val textView = TextView(this)
        textView.text = text
        textView.setPadding(40, 30, 40, 30)
        textView.textSize = 15f
        textView.elevation = 4f

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 15, 0, 15)

        if (isUser) {
            textView.setBackgroundResource(R.drawable.bg_user_bubble)
            textView.setTextColor(Color.WHITE)
            params.gravity = Gravity.END
            params.marginStart = 100
        } else {
            textView.setBackgroundResource(R.drawable.bg_bot_bubble)
            textView.setTextColor(ContextCompat.getColor(this, R.color.chat_black))
            params.gravity = Gravity.START
            params.marginEnd = 100
        }

        textView.layoutParams = params
        container.addView(textView)

        scrollView.post {
            scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }
}