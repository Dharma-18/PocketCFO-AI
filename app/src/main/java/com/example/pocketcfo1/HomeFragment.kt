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
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private var isTamil = false

    private lateinit var tvProfit: TextView
    private lateinit var etInput: EditText
    private lateinit var chatLayout: LinearLayout
    private lateinit var chatScroll: NestedScrollView
    private lateinit var btnNotification: ImageButton

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: return@registerForActivityResult
            etInput.setText(spoken)
            processUserInput(spoken)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvProfit    = view.findViewById(R.id.tvProfit)
        etInput     = view.findViewById(R.id.etInput)
        chatLayout  = view.findViewById(R.id.chatLayout)
        chatScroll  = view.findViewById(R.id.homeScroll)

        val btnReceiveCash = view.findViewById<LinearLayout>(R.id.btnReceiveCash)
        val btnSpendCash   = view.findViewById<LinearLayout>(R.id.btnSpendCash)
        val btnUpiAuto     = view.findViewById<LinearLayout>(R.id.btnUpiAuto)
        val btnReports     = view.findViewById<LinearLayout>(R.id.btnReports)
        
        val btnSend        = view.findViewById<ImageButton>(R.id.btnSendHome)
        val btnVoice       = view.findViewById<ImageButton>(R.id.btnVoiceHome)
        val tvLang         = view.findViewById<TextView>(R.id.tvLangToggle)
        
        btnNotification    = view.findViewById(R.id.btnNotificationHome)
        btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "No new notifications", Toast.LENGTH_SHORT).show()
            setNotificationActive(false)
        }

        // Language toggle
        tvLang.setOnClickListener {
            isTamil = !isTamil
            tvLang.text = if (isTamil) "த" else "EN"
        }

        // Voice mic
        btnVoice.setOnClickListener { launchVoice() }

        // Send text
        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) { processUserInput(text); etInput.setText("") }
        }

        // Quick cash buttons
        btnReceiveCash.setOnClickListener { showNumpad(true) }
        btnSpendCash.setOnClickListener   { showNumpad(false) }

        // Voice Log / UPI Auto -> Dialog parsing money via voice
        btnUpiAuto.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("🎙 Soundbox Listener")
                .setMessage("Keep phone near the Paytm box...\nWaiting for payment audio.")
                .setPositiveButton("Simulate Receive") { _, _ -> launchVoice() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Reports -> Switch to History/Ledger tab
        btnReports.setOnClickListener {
            val bNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            bNav?.selectedItemId = R.id.nav_history
        }

        // Daily greeting
        val prefs = requireActivity().getSharedPreferences("pocketcfo_prefs", Activity.MODE_PRIVATE)
        val shopName = prefs.getString("greeting_name", "Boss") ?: "Boss"
        addBubble("🙏 Good morning $shopName!\nReady to track today's cash flow.", false)

        // Observe profit
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalBusinessProfit.collect { profit ->
                tvProfit.text = "₹ ${profit ?: 0}"
            }
        }
    }

    fun setNotificationActive(active: Boolean) {
        if (::btnNotification.isInitialized) {
            btnNotification.setImageResource(
                if (active) android.R.drawable.stat_notify_chat
                else android.R.drawable.ic_popup_reminder
            )
        }
    }

    private fun processUserInput(text: String) {
        addBubble(text, true)
        viewModel.processInput(text, isTamil) { reply -> addBubble(reply, false) }
    }

    private fun launchVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isTamil) "ta-IN" else "en-IN")
        }
        try { speechLauncher.launch(intent) } catch (_: Exception) {}
    }

    private fun showNumpad(isReceive: Boolean) {
        val dialog = BottomSheetDialog(requireContext())
        val v = LayoutInflater.from(requireContext()).inflate(R.layout.layout_numpad, null)
        dialog.setContentView(v)

        val display = v.findViewById<TextView>(R.id.tvNumpadDisplay)
        val title   = v.findViewById<TextView>(R.id.tvNumpadTitle)
        title.text  = if (isReceive) "💵 Cash Received" else "💸 Cash Spent"
        title.setTextColor(if (isReceive) Color.parseColor("#16A34A") else Color.parseColor("#DC2626"))
        var amount = ""

        fun update() { display.text = if (amount.isEmpty()) "₹ 0" else "₹ $amount" }

        val ids = listOf(R.id.btn0,R.id.btn1,R.id.btn2,R.id.btn3,R.id.btn4,R.id.btn5,R.id.btn6,R.id.btn7,R.id.btn8,R.id.btn9)
        ids.forEachIndexed { i, id ->
            v.findViewById<Button>(id).setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                if (amount.length < 7) { amount += "$i"; update() }
            }
        }
        v.findViewById<Button>(R.id.btnBackspace).setOnClickListener { if (amount.isNotEmpty()) { amount = amount.dropLast(1); update() } }
        v.findViewById<Button>(R.id.btnNumpadConfirm).setOnClickListener {
            val value = amount.toIntOrNull() ?: 0
            if (value > 0) {
                dialog.dismiss()
                val entry = if (isReceive) "Received ₹$value" else "Spent ₹$value"
                addTransactionCard(entry, value, isReceive, "💵 Cash")
                viewModel.processInput(entry, isTamil) { }
            }
        }
        dialog.show()
    }

    fun addBubble(text: String, isUser: Boolean) {
        val tv = TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            // Set chat font color to pure black regardless of user/bot
            setTextColor(Color.parseColor("#000000"))
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

    private fun addTransactionCard(desc: String, amount: Int, isIncome: Boolean, source: String) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_transaction_card, null)
        val green = Color.parseColor("#22C55E"); val red = Color.parseColor("#EF4444")
        val color = if (isIncome) green else red

        view.findViewById<View>(R.id.cardAccentBar).setBackgroundColor(color)
        view.findViewById<TextView>(R.id.tvCardIcon).text = if (isIncome) "💵" else "💸"
        view.findViewById<TextView>(R.id.tvCardDescription).text = desc.take(32)
        view.findViewById<TextView>(R.id.tvCardTime).text = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        view.findViewById<TextView>(R.id.tvCardAmount).apply { text = "${if (isIncome) "+" else "-"}₹$amount"; setTextColor(color) }
        view.findViewById<TextView>(R.id.tvCardSource).text = source
        view.findViewById<TextView>(R.id.tvCardCategory).text = if (isIncome) "Income" else "Expense"
        view.findViewById<TextView>(R.id.tvCardType).apply { text = if (isIncome) "INCOME" else "EXPENSE"; setTextColor(color) }

        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 8, 0, 8)
        params.gravity = if (isIncome) Gravity.START else Gravity.END
        view.layoutParams = params
        chatLayout.addView(view)
        chatScroll.postDelayed({ chatScroll.fullScroll(View.FOCUS_DOWN) }, 100)
    }

}
