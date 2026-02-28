package com.example.pocketcfo1

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketcfo1.data.AppDatabase
import com.example.pocketcfo1.data.Transaction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment() {

    private lateinit var adapter: TxAdapter
    private var allTransactions = listOf<Transaction>()
    private var currentFilter = "all"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.rvTransactions)
        adapter = TxAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val tvIncome  = view.findViewById<TextView>(R.id.tvTxIncome)
        val tvExpense = view.findViewById<TextView>(R.id.tvTxExpense)
        val tvProfit  = view.findViewById<TextView>(R.id.tvTxProfit)

        // Search
        view.findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterAndUpdate(s.toString(), tvIncome, tvExpense, tvProfit) }
        })

        // Filters
        view.findViewById<TextView>(R.id.chipAll).setOnClickListener     { currentFilter = "all"; filterAndUpdate("", tvIncome, tvExpense, tvProfit) }
        view.findViewById<TextView>(R.id.chipIncome).setOnClickListener  { currentFilter = "income"; filterAndUpdate("", tvIncome, tvExpense, tvProfit) }
        view.findViewById<TextView>(R.id.chipExpense).setOnClickListener { currentFilter = "expense"; filterAndUpdate("", tvIncome, tvExpense, tvProfit) }
        view.findViewById<TextView>(R.id.chipUpi).setOnClickListener     { currentFilter = "upi"; filterAndUpdate("", tvIncome, tvExpense, tvProfit) }

        // PDF Download
        view.findViewById<ImageButton>(R.id.btnDownloadPdf).setOnClickListener {
            generatePdf()
        }

        loadData(tvIncome, tvExpense, tvProfit)
    }

    override fun onResume() { super.onResume(); loadData(view?.findViewById(R.id.tvTxIncome), view?.findViewById(R.id.tvTxExpense), view?.findViewById(R.id.tvTxProfit)) }

    private fun loadData(tvIncome: TextView?, tvExpense: TextView?, tvProfit: TextView?) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            allTransactions = db.transactionDao().getAllTransactionsList()
            filterAndUpdate("", tvIncome, tvExpense, tvProfit)
        }
    }

    private fun filterAndUpdate(query: String, tvIncome: TextView?, tvExpense: TextView?, tvProfit: TextView?) {
        var filtered = when (currentFilter) {
            "income"  -> allTransactions.filter { it.type == "Income" }
            "expense" -> allTransactions.filter { it.type == "Expense" }
            "upi"     -> allTransactions.filter { it.source == "sms" || it.source == "upi" }
            else      -> allTransactions
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.description.contains(query, true) || it.category.contains(query, true) }
        }
        adapter.submitList(filtered)

        val inc = filtered.filter { it.type == "Income" }.sumOf { it.amount }
        val exp = filtered.filter { it.type == "Expense" }.sumOf { it.amount }
        tvIncome?.text  = "Income: ₹$inc"
        tvExpense?.text = "Expense: ₹$exp"
        tvProfit?.text  = "Profit: ₹${inc - exp}"
    }

    private fun generatePdf() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val txs = db.transactionDao().getAllTransactionsList()
            val prefs = requireActivity().getSharedPreferences("pocketcfo_prefs", android.app.Activity.MODE_PRIVATE)
            val shop = prefs.getString("greeting_name", "My Shop") ?: "My Shop"
            try {
                val path = PdfGenerator.generateReport(requireContext(), txs, shop, "Transaction Ledger")
                Toast.makeText(requireContext(), "✅ PDF saved: $path", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── RecyclerView Adapter ────────────────────────────────────────────────
    inner class TxAdapter : RecyclerView.Adapter<TxAdapter.VH>() {
        private var items = listOf<Transaction>()
        fun submitList(list: List<Transaction>) { items = list; notifyDataSetChanged() }

        inner class VH(val view: View) : RecyclerView.ViewHolder(view) {
            val tvAmount: TextView = view.findViewById(R.id.tvCardAmount)
            val tvDesc: TextView   = view.findViewById(R.id.tvCardDescription)
            val tvTime: TextView   = view.findViewById(R.id.tvCardTime)
            val tvSource: TextView = view.findViewById(R.id.tvCardSource)
            val tvCategory: TextView = view.findViewById(R.id.tvCardCategory)
            val tvType: TextView   = view.findViewById(R.id.tvCardType)
            val tvIcon: TextView   = view.findViewById(R.id.tvCardIcon)
            val accent: View       = view.findViewById(R.id.cardAccentBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction_card, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val tx = items[position]
            val isIncome = tx.type == "Income"
            val green = Color.parseColor("#22C55E"); val red = Color.parseColor("#EF4444")
            val color = if (isIncome) green else red

            holder.accent.setBackgroundColor(color)
            holder.tvIcon.text = if (isIncome) "💵" else "💸"
            holder.tvDesc.text = tx.description.take(32)
            holder.tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
            holder.tvAmount.text = "${if (isIncome) "+" else "-"}₹${tx.amount}"
            holder.tvAmount.setTextColor(color)
            holder.tvSource.text = tx.source
            holder.tvCategory.text = tx.category
            holder.tvType.text = if (isIncome) "INCOME" else "EXPENSE"
            holder.tvType.setTextColor(color)
        }

        override fun getItemCount() = items.size
    }
}
