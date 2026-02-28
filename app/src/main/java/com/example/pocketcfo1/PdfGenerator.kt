package com.example.pocketcfo1

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.pocketcfo1.data.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Professional PDF financial report generator.
 * Uses Android's native PdfDocument API — zero external dependencies.
 */
object PdfGenerator {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // Colors
    private val PRIMARY    = Color.parseColor("#0D3B66")
    private val GREEN      = Color.parseColor("#16A34A")
    private val RED        = Color.parseColor("#DC2626")
    private val GREY       = Color.parseColor("#64748B")
    private val LIGHT_GREY = Color.parseColor("#F1F5F9")
    private val WHITE      = Color.WHITE

    /**
     * Generate a professional financial report PDF.
     * @return File path of the generated PDF
     */
    fun generateReport(
        context: Context,
        transactions: List<Transaction>,
        shopName: String,
        reportTitle: String = "Daily Report"
    ): String {
        val document = PdfDocument()
        val pageWidth = 595  // A4
        val pageHeight = 842

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = 40f

        // ── HEADER ──────────────────────────────────────────────
        val headerPaint = Paint().apply { color = PRIMARY; textSize = 24f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("PocketCFO", 40f, y, headerPaint)

        val datePaint = Paint().apply { color = GREY; textSize = 12f }
        canvas.drawText(dateFormat.format(Date()), pageWidth - 140f, y, datePaint)
        y += 10f

        // Accent line
        val linePaint = Paint().apply { color = PRIMARY; strokeWidth = 3f }
        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 30f

        // Shop name + report title
        val shopPaint = Paint().apply { color = PRIMARY; textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText(shopName, 40f, y, shopPaint)
        y += 22f

        val titlePaint = Paint().apply { color = GREY; textSize = 14f }
        canvas.drawText(reportTitle, 40f, y, titlePaint)
        y += 35f

        // ── SUMMARY BOX ─────────────────────────────────────────
        val totalIncome  = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val profit = totalIncome - totalExpense

        val boxPaint = Paint().apply { color = LIGHT_GREY; style = Paint.Style.FILL }
        canvas.drawRoundRect(40f, y, pageWidth - 40f, y + 90f, 12f, 12f, boxPaint)

        val labelPaint = Paint().apply { color = GREY; textSize = 11f }
        val valuePaint = Paint().apply { textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

        // Income
        canvas.drawText("Total Income", 60f, y + 25f, labelPaint)
        valuePaint.color = GREEN
        canvas.drawText("₹$totalIncome", 60f, y + 50f, valuePaint)

        // Expense
        canvas.drawText("Total Expense", 230f, y + 25f, labelPaint)
        valuePaint.color = RED
        canvas.drawText("₹$totalExpense", 230f, y + 50f, valuePaint)

        // Profit
        canvas.drawText("Net Profit", 400f, y + 25f, labelPaint)
        valuePaint.color = if (profit >= 0) GREEN else RED
        canvas.drawText("₹$profit", 400f, y + 50f, valuePaint)

        // Source breakdown
        val cashCount = transactions.count { it.source == "chat_input" || it.source == "voice" }
        val upiCount  = transactions.count { it.source == "sms" || it.source == "upi" }
        val soundboxCount = transactions.count { it.source == "soundbox" }
        val breakdownPaint = Paint().apply { color = GREY; textSize = 10f }
        canvas.drawText("Cash: $cashCount  |  UPI: $upiCount  |  SoundBox: $soundboxCount", 60f, y + 78f, breakdownPaint)

        y += 110f

        // ── TRANSACTION TABLE ────────────────────────────────────
        // Header row
        val thPaint = Paint().apply { color = WHITE; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val headerBg = Paint().apply { color = PRIMARY; style = Paint.Style.FILL }
        canvas.drawRoundRect(40f, y, pageWidth - 40f, y + 28f, 6f, 6f, headerBg)
        canvas.drawText("Time", 50f, y + 19f, thPaint)
        canvas.drawText("Description", 130f, y + 19f, thPaint)
        canvas.drawText("Source", 350f, y + 19f, thPaint)
        canvas.drawText("Amount", 460f, y + 19f, thPaint)
        y += 32f

        // Rows
        val rowPaint = Paint().apply { textSize = 10f }
        val altBg = Paint().apply { color = Color.parseColor("#FAFAFA"); style = Paint.Style.FILL }

        val sortedTx = transactions.sortedByDescending { it.timestamp }
        for ((i, tx) in sortedTx.withIndex()) {
            if (y > pageHeight - 60f) break // Don't overflow page

            if (i % 2 == 0) canvas.drawRect(40f, y - 4f, pageWidth - 40f, y + 18f, altBg)

            rowPaint.color = GREY
            canvas.drawText(timeFormat.format(Date(tx.timestamp)), 50f, y + 12f, rowPaint)

            rowPaint.color = Color.parseColor("#0F172A")
            val desc = if (tx.description.length > 30) tx.description.take(30) + "…" else tx.description
            canvas.drawText(desc, 130f, y + 12f, rowPaint)

            rowPaint.color = GREY
            canvas.drawText(tx.source, 350f, y + 12f, rowPaint)

            rowPaint.color = if (tx.type == "Income") GREEN else RED
            rowPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val sign = if (tx.type == "Income") "+" else "-"
            canvas.drawText("${sign}₹${tx.amount}", 460f, y + 12f, rowPaint)
            rowPaint.typeface = Typeface.DEFAULT

            y += 22f
        }

        // ── FOOTER ──────────────────────────────────────────────
        y = pageHeight - 40f
        val footerPaint = Paint().apply { color = GREY; textSize = 9f }
        canvas.drawText("Generated by PocketCFO • ${dateFormat.format(Date())} • Confidential", 40f, y, footerPaint)

        document.finishPage(page)

        // Save to Downloads
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "PocketCFO_${reportTitle.replace(" ", "_")}_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
        val file = File(downloadsDir, fileName)
        document.writeTo(FileOutputStream(file))
        document.close()

        return file.absolutePath
    }
}
