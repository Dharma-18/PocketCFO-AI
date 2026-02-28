package com.example.pocketcfo1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pocketcfo1.data.AppDatabase
import com.example.pocketcfo1.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * HACKATHON KILLER FEATURE: UPI SMS Auto-Capture
 * 
 * Listens for UPI debit SMS alerts from Paytm/PhonePe/GPay and automatically
 * extracts and categorizes expenses. Zero friction for the shop owner.
 *
 * Example SMS patterns handled:
 * - "₹450.00 debited via UPI to Fresh Milk Dairy on 28-02-26"
 * - "INR 250 debited from your account via UPI"
 * - "₹100 Cr to your account via UPI from CustomerMerchant"
 */
class UpiSmsReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "pocketcfo_upi"

        // Merchant → Category smart mapping
        val MERCHANT_CATEGORY_MAP = mapOf(
            // Inventory / Supplies
            "milk" to "🥛 Inventory",  "dairy" to "🥛 Inventory",
            "flour" to "🌾 Inventory",  "rice" to "🌾 Inventory",
            "sugar" to "🌾 Inventory",  "oil" to "🌾 Inventory",
            "vegetable" to "🥦 Inventory", "sabzi" to "🥦 Inventory",
            "wholesale" to "📦 Inventory", "mart" to "📦 Inventory",
            // Utilities
            "electricity" to "⚡ Utilities", "eb " to "⚡ Utilities",
            "tangedco" to "⚡ Utilities", "bescom" to "⚡ Utilities",
            "broadband" to "📶 Utilities", "airtel" to "📶 Utilities",
            "jio" to "📶 Utilities", "vi " to "📶 Utilities",
            "water" to "💧 Utilities",
            // Rent/ Property
            "rent" to "🏠 Rent", "landlord" to "🏠 Rent",
            // Personal
            "restaurant" to "🍽 Personal", "zomato" to "🍽 Personal",
            "swiggy" to "🍽 Personal", "amazon" to "🛒 Personal",
            "flipkart" to "🛒 Personal", "movie" to "🎬 Personal",
            "pvr" to "🎬 Personal", "uber" to "🚗 Personal",
            "ola" to "🚗 Personal", "rapido" to "🚗 Personal"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("pocketcfo_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sms_tracking_enabled", true)) {
            return // Tracking explicitly disabled by user
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (msg in messages) {
            val body = msg.messageBody ?: continue
            parseSms(context, body)
        }
    }

    private fun parseSms(context: Context, body: String) {
        val lowerBody = body.lowercase()

        // Only process UPI-related messages
        val isUpi = lowerBody.contains("upi") || lowerBody.contains("paytm") ||
                    lowerBody.contains("phonepe") || lowerBody.contains("gpay") ||
                    lowerBody.contains("debited") || lowerBody.contains("credited")
        if (!isUpi) return

        // Extract amount using regex
        val amountPattern = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
        val matcher = amountPattern.matcher(body)
        if (!matcher.find()) return

        val amountStr = matcher.group(1)?.replace(",", "") ?: return
        val amount = amountStr.toDoubleOrNull()?.toInt() ?: return

        // Is this a credit (income) or debit (expense)?
        val isCredit = lowerBody.contains("credit") || lowerBody.contains("received") ||
                       lowerBody.contains("cr ") || lowerBody.contains("credited")
        val isDebit = lowerBody.contains("debit") || lowerBody.contains("debited") ||
                      lowerBody.contains("dr ") || lowerBody.contains("paid")

        if (!isCredit && !isDebit) return

        // Extract merchant name (after "to" for debits, "from" for credits)
        val merchantPattern = Pattern.compile("(?:to|from)\\s+([A-Za-z\\s]{3,25}?)(?:\\s+on|\\.|$)", Pattern.CASE_INSENSITIVE)
        val merchantMatcher = merchantPattern.matcher(body)
        val merchant = if (merchantMatcher.find()) merchantMatcher.group(1)?.trim() else null

        // Auto-categorize based on merchant name
        val category = detectCategory(merchant ?: "", lowerBody)
        val txType = if (isCredit) "Income" else "Expense"
        val txCategory = if (isCredit) "Business" else {
            if (listOf("Personal", "Rent").any { category.contains(it.first().uppercase()) }) "Personal" else "Business"
        }

        // Save to Room DB
        val transaction = Transaction(
            amount = amount,
            type = txType,
            category = txCategory,
            description = "[UPI Auto] ${merchant ?: "UPI Payment"} - ₹$amount",
            source = "upi_sms"
        )

        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(context).transactionDao().insertTransaction(transaction)
        }

        // Show smart notification
        val emoji = if (isCredit) "💚" else "🔴"
        val verb = if (isCredit) "received" else "spent"
        showNotification(
            context,
            title = "$emoji Auto-logged ₹$amount $verb",
            body = "${merchant ?: "UPI"} → $category\nTap to review"
        )
    }

    private fun detectCategory(merchant: String, body: String): String {
        val combined = "${merchant.lowercase()} $body"
        for ((keyword, category) in MERCHANT_CATEGORY_MAP) {
            if (combined.contains(keyword)) return category
        }
        return "📋 Business"
    }

    private fun showNotification(context: Context, title: String, body: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "PocketCFO Auto-Logs", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val pending = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
