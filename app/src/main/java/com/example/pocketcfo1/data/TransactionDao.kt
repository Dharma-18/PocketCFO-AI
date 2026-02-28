package com.example.pocketcfo1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Income' AND category = 'Business'")
    fun getBusinessIncome(): Flow<Int?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Expense' AND category = 'Business'")
    fun getBusinessExpense(): Flow<Int?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Income' AND category = 'Personal'")
    fun getPersonalIncome(): Flow<Int?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Expense' AND category = 'Personal'")
    fun getPersonalExpense(): Flow<Int?>
    
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'Income' 
        AND date(timestamp / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')
    """)
    suspend fun getTodayIncome(): Int?

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'Expense' 
        AND date(timestamp / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')
    """)
    suspend fun getTodayExpense(): Int?

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startMs AND :endMs ORDER BY timestamp DESC")
    suspend fun getTransactionsBetween(startMs: Long, endMs: Long): List<Transaction>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTransactions(limit: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE source = :source ORDER BY timestamp DESC")
    suspend fun getTransactionsBySource(source: String): List<Transaction>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<Transaction>
}
