package com.example.pocketcfo1

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.pocketcfo1.data.AppDatabase
import kotlinx.coroutines.launch
import java.util.*

class ProfileFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("pocketcfo_prefs", android.app.Activity.MODE_PRIVATE)
        val shopName = prefs.getString("greeting_name", "My Shop") ?: "My Shop"
        val shopType = prefs.getString("shop_type", "Not set") ?: "Not set"

        view.findViewById<TextView>(R.id.tvShopName).text = shopName
        view.findViewById<TextView>(R.id.tvShopType).text = "Type: ${shopType.replaceFirstChar { it.uppercase() }}"

        val switchSms = view.findViewById<Switch>(R.id.switchSmsTracking)
        switchSms.isChecked = prefs.getBoolean("sms_tracking_enabled", true)
        switchSms.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sms_tracking_enabled", isChecked).apply()
            val msg = if (isChecked) "📲 SMS Auto-Capture Enabled" else "🔕 SMS Auto-Capture Disabled"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val count = db.transactionDao().getAllTransactionsList().size
            view.findViewById<TextView>(R.id.tvTotalTx).text = "Total Transactions: $count"
        }

        // Daily PDF
        view.findViewById<LinearLayout>(R.id.btnDailyPdf).setOnClickListener {
            generatePdf("Daily Report") { db ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                db.transactionDao().getTransactionsBetween(cal.timeInMillis, System.currentTimeMillis())
            }
        }

        // Weekly PDF
        view.findViewById<LinearLayout>(R.id.btnWeeklyPdf).setOnClickListener {
            generatePdf("Weekly Report") { db ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                db.transactionDao().getTransactionsBetween(cal.timeInMillis, System.currentTimeMillis())
            }
        }

        // Full PDF
        view.findViewById<LinearLayout>(R.id.btnFullPdf).setOnClickListener {
            generatePdf("Full Financial Report") { db ->
                db.transactionDao().getAllTransactionsList()
            }
        }

        view.findViewById<Button>(R.id.btnClearData).setOnClickListener {
            val builder = android.app.AlertDialog.Builder(requireContext())
            builder.setTitle("⚠️ Clear All Data")
            builder.setMessage("This will delete all logged data permanently. Are you sure you want to proceed?")
            builder.setPositiveButton("Clear Data") { _, _ ->
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.transactionDao().deleteAll()
                    
                    // Clear shop preferences and go back to Setup
                    val prefs = requireActivity().getSharedPreferences("pocketcfo_prefs", android.app.Activity.MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    
                    Toast.makeText(requireContext(), "All data cleared successfully.", Toast.LENGTH_LONG).show()
                    val intent = android.content.Intent(requireActivity(), OnboardingActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }

        view.findViewById<Button>(R.id.btnExitBusiness).setOnClickListener {
            // Clear shop preferences and go back to Setup
            val prefs = requireActivity().getSharedPreferences("pocketcfo_prefs", android.app.Activity.MODE_PRIVATE)
            prefs.edit().clear().apply()
            
            val intent = android.content.Intent(requireActivity(), OnboardingActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun generatePdf(title: String, fetchTx: suspend (AppDatabase) -> List<com.example.pocketcfo1.data.Transaction>) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val txs = fetchTx(db)
            val prefs = requireActivity().getSharedPreferences("pocketcfo_prefs", android.app.Activity.MODE_PRIVATE)
            val shop = prefs.getString("greeting_name", "My Shop") ?: "My Shop"
            try {
                val path = PdfGenerator.generateReport(requireContext(), txs, shop, title)
                Toast.makeText(requireContext(), "✅ PDF saved to Downloads!\n$path", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
