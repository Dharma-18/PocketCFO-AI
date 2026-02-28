package com.example.pocketcfo1

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.pocketcfo1.data.AppDatabase
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class InsightsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_insights, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvSpending = view.findViewById<TextView>(R.id.tvSpendingInsight)
        val tvIncome   = view.findViewById<TextView>(R.id.tvIncomeInsight)
        val tvCashFlow = view.findViewById<TextView>(R.id.tvCashFlowInsight)
        val tvCategory = view.findViewById<TextView>(R.id.tvCategoryInsight)
        val tvSuggestion = view.findViewById<TextView>(R.id.tvAiSuggestion)
        val lineChart = view.findViewById<LineChart>(R.id.insightsLineChart)

        view.findViewById<ImageButton>(R.id.btnExportInsights).setOnClickListener { exportInsightsPdf() }

        loadInsights(tvSpending, tvIncome, tvCashFlow, tvCategory, tvSuggestion, lineChart)
    }

    override fun onResume() { super.onResume(); view?.let { onViewCreated(it, null) } }

    private fun loadInsights(tvSpending: TextView, tvIncome: TextView, tvCashFlow: TextView, tvCategory: TextView, tvSuggestion: TextView, lineChart: LineChart) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val allTx = db.transactionDao().getAllTransactionsList()
            if (allTx.isEmpty()) {
                lineChart.setNoDataText("Log transactions to see 7-day trend")
                lineChart.invalidate()
                return@launch
            }

            val incomes  = allTx.filter { it.type == "Income" }
            val expenses = allTx.filter { it.type == "Expense" }
            val totalInc = incomes.sumOf { it.amount }
            val totalExp = expenses.sumOf { it.amount }

            // Today's data
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
            val todayStart = cal.timeInMillis
            val todayTx = allTx.filter { it.timestamp >= todayStart }
            val todayInc = todayTx.filter { it.type == "Income" }.sumOf { it.amount }
            val todayExp = todayTx.filter { it.type == "Expense" }.sumOf { it.amount }

            // Spending insight
            val avgExpense = if (expenses.isNotEmpty()) totalExp / expenses.size else 0
            tvSpending.text = "📊 Total expenses: ₹$totalExp across ${expenses.size} transactions\n" +
                "Average expense: ₹$avgExpense\n" +
                "Today's spending: ₹$todayExp"

            // Income insight
            val avgIncome = if (incomes.isNotEmpty()) totalInc / incomes.size else 0
            tvIncome.text = "💰 Total income: ₹$totalInc across ${incomes.size} transactions\n" +
                "Average income: ₹$avgIncome\n" +
                "Today's earnings: ₹$todayInc"

            // Cash flow
            val cashTx = allTx.filter { it.source == "chat_input" || it.source == "voice" }
            val upiTx  = allTx.filter { it.source == "sms" || it.source == "upi" }
            val soundboxTx = allTx.filter { it.source == "soundbox" }
            tvCashFlow.text = "💵 Cash transactions: ${cashTx.size}\n" +
                "📲 UPI transactions: ${upiTx.size}\n" +
                "🎙 SoundBox auto-logs: ${soundboxTx.size}\n" +
                "Cash vs UPI ratio: ${if (upiTx.isNotEmpty()) "${(cashTx.size * 100 / (cashTx.size + upiTx.size))}% Cash" else "No UPI yet"}"

            // Category
            val categories = allTx.groupBy { it.category }
            val catText = categories.entries.joinToString("\n") { (cat, txs) ->
                val total = txs.sumOf { it.amount }
                "• $cat: ₹$total (${txs.size} transactions)"
            }
            tvCategory.text = catText.ifEmpty { "No categories yet" }

            // AI suggestion
            val profit = totalInc - totalExp
            tvSuggestion.text = when {
                profit > 0  -> "✅ Great! You're profitable (+₹$profit). Keep tracking daily to grow your business!"
                profit == 0 -> "⚖️ You're breaking even. Try to reduce expenses or increase sales."
                else        -> "⚠️ You're in loss (₹$profit). Review your top expenses and cut unnecessary spending."
            }
            
            // 7-Day Line Chart logic
            val entries = ArrayList<Entry>()
            val labels = ArrayList<String>()
            val sdf = SimpleDateFormat("EEE", Locale.getDefault()) // Mon, Tue, etc.

            val cal7 = Calendar.getInstance()
            cal7.add(Calendar.DAY_OF_YEAR, -6)
            cal7.set(Calendar.HOUR_OF_DAY, 0); cal7.set(Calendar.MINUTE, 0); cal7.set(Calendar.SECOND, 0)
            
            for (i in 0..6) {
                val dayStart = cal7.timeInMillis
                val dayLabel = sdf.format(cal7.time)
                labels.add(dayLabel)
                
                cal7.add(Calendar.DAY_OF_YEAR, 1)
                val dayEnd = cal7.timeInMillis
                
                val dayTx = allTx.filter { it.timestamp in dayStart until dayEnd }
                val dInc = dayTx.filter { it.type == "Income" }.sumOf { it.amount }
                val dExp = dayTx.filter { it.type == "Expense" }.sumOf { it.amount }
                val dProfit = (dInc - dExp).toFloat()
                
                entries.add(Entry(i.toFloat(), dProfit))
            }
            
            val dataSet = LineDataSet(entries, "Daily Net Profit")
            // Make graph line blue, filled with light blue
            dataSet.color = android.graphics.Color.parseColor("#38BDF8")
            dataSet.valueTextColor = android.graphics.Color.parseColor("#0F172A")
            dataSet.lineWidth = 3f
            dataSet.circleRadius = 5f
            dataSet.setCircleColor(android.graphics.Color.parseColor("#0ea5e9"))
            dataSet.setDrawFilled(true)
            dataSet.fillColor = android.graphics.Color.parseColor("#bae6fd")
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curves
            
            val lineData = LineData(dataSet)
            lineChart.data = lineData
            lineChart.description.isEnabled = false
            lineChart.legend.isEnabled = false
            lineChart.axisRight.isEnabled = false
            
            val xAxis = lineChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            
            lineChart.animateX(1000)
            lineChart.invalidate()
        }
    }

    private fun exportInsightsPdf() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val txs = db.transactionDao().getAllTransactionsList()
            val prefs = requireActivity().getSharedPreferences("pocketcfo_prefs", android.app.Activity.MODE_PRIVATE)
            val shop = prefs.getString("greeting_name", "My Shop") ?: "My Shop"
            try {
                val path = PdfGenerator.generateReport(requireContext(), txs, shop, "AI Insights Report")
                Toast.makeText(requireContext(), "✅ PDF saved: $path", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
