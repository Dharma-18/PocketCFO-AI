package com.example.pocketcfo1

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.pocketcfo1.data.AppDatabase
import com.example.pocketcfo1.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class AssistantFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private var isTamil = false
    private lateinit var chatLayout: LinearLayout
    private lateinit var chatScroll: NestedScrollView

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: return@registerForActivityResult
            processQuery(spoken)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_assistant, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatLayout = view.findViewById(R.id.assistantChat)
        chatScroll = view.findViewById(R.id.assistantScroll)
        val etInput = view.findViewById<EditText>(R.id.etAssistantInput)

        // Welcome
        addBubble("🤖 I'm your AI CFO! Ask me anything:\n• \"How much profit today?\"\n• \"Show my expenses\"\n• \"Did I spend too much this week?\"", false)

        view.findViewById<ImageButton>(R.id.btnAssistantSend).setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) { processQuery(text); etInput.setText("") }
        }

        view.findViewById<ImageButton>(R.id.btnAssistantVoice).setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isTamil) "ta-IN" else "en-IN")
            }
            try { speechLauncher.launch(intent) } catch (_: Exception) {}
        }

        view.findViewById<ImageButton>(R.id.btnGenerateReport).setOnClickListener {
            generateReportPdf()
        }
    }

    private fun processQuery(query: String) {
        addBubble(query, true)
        viewModel.processInput(query, isTamil) { reply -> addBubble(reply, false) }
    }

    private fun addBubble(text: String, isUser: Boolean) {
        val tv = TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setTextColor(if (isUser) Color.WHITE else Color.parseColor("#0F172A"))
            setBackgroundResource(if (isUser) R.drawable.bg_user_bubble else R.drawable.bg_bot_bubble_pro)
            setPadding(36, 24, 36, 24)
        }
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 8, 0, 8)
        params.gravity = if (isUser) Gravity.END else Gravity.START
        tv.layoutParams = params
        chatLayout.addView(tv)
        chatScroll.postDelayed({ chatScroll.fullScroll(View.FOCUS_DOWN) }, 100)
    }

    private fun generateReportPdf() {
        lifecycleScope.launch {
            addBubble("📄 Generating Financial Report PDF...", false)
            val db = AppDatabase.getDatabase(requireContext())
            val txs = db.transactionDao().getAllTransactionsList()
            val prefs = requireActivity().getSharedPreferences("pocketcfo_prefs", Activity.MODE_PRIVATE)
            val shop = prefs.getString("greeting_name", "My Shop") ?: "My Shop"
            try {
                val path = PdfGenerator.generateReport(requireContext(), txs, shop, "AI Financial Summary")
                addBubble("✅ Report saved!\n📁 $path\n\nOpen your Downloads folder to view it.", false)
            } catch (e: Exception) {
                addBubble("❌ Error generating PDF: ${e.message}", false)
            }
        }
    }
}
