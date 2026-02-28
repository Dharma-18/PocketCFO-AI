package com.example.pocketcfo1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketcfo1.data.AppDatabase
import com.example.pocketcfo1.data.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).transactionDao()

    val allTransactions = dao.getAllTransactions()

    val totalBusinessProfit = combine(dao.getBusinessIncome(), dao.getBusinessExpense()) { inc, exp ->
        (inc ?: 0) - (exp ?: 0)
    }

    val totalPersonalSpending = combine(dao.getPersonalExpense(), dao.getPersonalIncome()) { exp, inc ->
        (exp ?: 0) - (inc ?: 0)
    }

    private val _pendingMessage = MutableStateFlow<String?>(null)
    val pendingMessage: StateFlow<String?> = _pendingMessage

    // ── TRILINGUAL NLP Engine ──────────────────────────────────────────────────

    fun processInput(rawInput: String, isTamil: Boolean, onReply: (String) -> Unit) {
        val normalized = textToNumeric(rawInput).lowercase()

        // 1. Entity Extraction: Amount
        val p = Pattern.compile("(\\d+)\\s*(k|lakh)?")
        val m = p.matcher(normalized)
        var detectedAmount = 0
        if (m.find()) {
            val num = m.group(1)?.toInt() ?: 0
            val unit = m.group(2) ?: ""
            detectedAmount = when (unit) {
                "k"    -> num * 1000
                "lakh" -> num * 100000
                else   -> num
            }
        }

        if (detectedAmount == 0) {
            _pendingMessage.value = rawInput
            val msg = if (isTamil)
                "எவ்வளவு தொகை? (₹) - எ.கா: 500 அல்லது 1k"
            else
                "Got it. What was the exact amount? (e.g. 500 or 2k)"
            onReply(msg)
            return
        }

        // 2. Intent Detection (Income/Expense) — trilingual keywords
        val incomeKeywords = listOf(
            // English
            "salary", "sold", "sale", "made", "income", "received", "profit", "+", "earning", "earn", "got", "amount received", "cash in",
            // Tamil
            "vittom", "vitrrom", "vaangi", "kadai", "kittiyuchu", "kidachiduchu", "perappattadhu", "kedaichiruku",
            // Hindi
            "mila", "bikri", "aaya", "income"
        )
        val isIncome = incomeKeywords.any { normalized.contains(it) } ||
            (_pendingMessage.value != null && incomeKeywords.any { _pendingMessage.value!!.lowercase().contains(it) })

        val type = if (isIncome) "Income" else "Expense"
        val category = autoCategorize(normalized, _pendingMessage.value?.lowercase())

        val transaction = Transaction(
            amount = detectedAmount,
            type = type,
            category = category,
            description = _pendingMessage.value ?: rawInput,
            source = "chat_input"
        )

        viewModelScope.launch { dao.insertTransaction(transaction) }

        // 3. Build reply with micro-insight
        val icon = if (isIncome) "🟢" else "🔴"
        val header = if (isTamil) "📊 பதிவு செய்யப்பட்டது\n────────────\n" else "📊 RECORDED\n────────────\n"
        val body   = "$icon ${if (isIncome) "+" else "-"}₹$detectedAmount\n📁 $category · ${type}"
        val insight = getMicroInsight(detectedAmount, isIncome, isTamil)

        onReply(header + body + if (insight.isNotEmpty()) "\n\n💡 $insight" else "")
        _pendingMessage.value = null
    }

    // ── DAILY OPENING CASH prompt ──────────────────────────────────────────────
    fun getDailyOpeningPrompt(isTamil: Boolean): String {
        return if (isTamil)
            "🌅 காலை வணக்கம்! இன்று தொடங்கும் முன்,\nகையில் எவ்வளவு ரொக்கம் இருக்கிறது?\n\nதொகையை தட்டச்சு செய்யுங்கள் அல்லது பேசுங்கள் 👇"
        else
            "🌅 Good morning! Before we start today —\nHow much cash do you have in hand right now?\n\nType the amount or tap 🎤 to speak 👇"
    }

    // ── DAILY CLOSING / EOD summary ────────────────────────────────────────────
    fun getEndOfDaySummary(isTamil: Boolean, onSummary: (String) -> Unit) {
        viewModelScope.launch {
            val income    = dao.getTodayIncome() ?: 0
            val expense   = dao.getTodayExpense() ?: 0
            val net       = income - expense
            val profitIcon = if (net >= 0) "📈" else "📉"
            if (isTamil) {
                onSummary(
                    "🌙 இன்றைய அறிக்கை\n" +
                    "════════════════\n" +
                    "💚 வருமானம்: ₹$income\n" +
                    "🔴 செலவு:    ₹$expense\n" +
                    "────────────\n" +
                    "$profitIcon நிகர லாபம்: ₹$net\n\n" +
                    if (net > 0) "அருமை! இன்று நல்ல நாள் 🎉" else "நாளை இன்னும் சிறந்ததாக இருக்கும் 💪"
                )
            } else {
                onSummary(
                    "🌙 End of Day Report\n" +
                    "════════════════\n" +
                    "💚 Total Income:  ₹$income\n" +
                    "🔴 Total Expense: ₹$expense\n" +
                    "────────────\n" +
                    "$profitIcon Net Profit:    ₹$net\n\n" +
                    if (net > 0) "Great day! Keep it up 🎉" else "Tomorrow will be better 💪"
                )
            }
        }
    }

    // ── MICRO-INSIGHTS generator ───────────────────────────────────────────────
    private fun getMicroInsight(amount: Int, isIncome: Boolean, isTamil: Boolean): String {
        return when {
            amount >= 10000 && isIncome ->
                if (isTamil) "பெரிய விற்பனை! சிறந்த நாள் 🎉" else "Big sale! Great day 🎉"
            amount >= 5000 && !isIncome ->
                if (isTamil) "பெரிய செலவு. இது தேவையானதா என்று யோசியுங்கள் 🤔" else "Large expense. Was this planned? 🤔"
            !isIncome ->
                if (isTamil) "செலவு பதிவு செய்யப்பட்டது ✅" else "Expense logged ✅"
            else -> ""
        }
    }

    // ── AUTO-CATEGORIZER ───────────────────────────────────────────────────────
    private fun autoCategorize(currentInput: String, pendingInput: String?): String {
        val combined = "$currentInput ${pendingInput ?: ""}"
        val personalKeywords = listOf(
            "home", "house", "personal", "kids", "gift", "eggs", "milk",
            "movie", "clothes", "dinner", "food", "grocery", "rent",
            // Tamil personal words
            "veedu", "kudumbam", "saapadu", "palasaram"
        )
        return if (personalKeywords.any { combined.contains(it) }) "Personal" else "Business"
    }

    // ── TRILINGUAL number-word → digit converter ───────────────────────────────
    private fun textToNumeric(text: String): String {
        var processed = text.lowercase()
        val wordMap = mapOf(
            // English
            "one" to "1", "two" to "2", "three" to "3", "four" to "4", "five" to "5",
            "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
            "twenty" to "20", "thirty" to "30", "forty" to "40", "fifty" to "50",
            "hundred" to "00", "thousand" to "000",
            // Hindi
            "ek" to "1", "do" to "2", "teen" to "3", "char" to "4", "paanch" to "5",
            "sau" to "100", "hazaar" to "1000",
            // Tamil
            "onru" to "1", "oru" to "1", "rendu" to "2", "moonru" to "3",
            "naanku" to "4", "anju" to "5", "pathu" to "10",
            "nooru" to "100", "ayiram" to "1000"
        )
        wordMap.forEach { (word, num) -> processed = processed.replace("\\b$word\\b".toRegex(), num) }
        return processed
    }
}
