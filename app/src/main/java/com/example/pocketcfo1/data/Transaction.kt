package com.example.pocketcfo1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Int,
    val type: String, // "Income" or "Expense"
    val category: String, // "Business" or "Personal"
    val description: String, // Raw input
    val source: String, // e.g. "chat_input"
    val timestamp: Long = System.currentTimeMillis()
)
