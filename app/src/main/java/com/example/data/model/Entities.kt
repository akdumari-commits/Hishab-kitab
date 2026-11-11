package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val businessName: String,
    val phone: String,
    val email: String,
    val passwordHash: String,
    val address: String = "",
    val upiId: String = "",
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val whatsapp: String = "",
    val address: String = "",
    val customerType: String = "CUSTOMER", // CUSTOMER, SUPPLIER, OTHER
    val openingBalance: Double = 0.0,
    val openingBalanceType: String = "GET", // GET (Receivable), GIVE (Payable), ZERO
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val customerId: String,
    val type: String, // GAVE (Debit/नामे - You gave money), GOT (Credit/जमा - You received money)
    val amount: Double,
    val dateMillis: Long,
    val remarks: String = "",
    val paymentMethod: String = "CASH", // CASH, UPI, BANK_TRANSFER, CHEQUE, OTHER
    val billNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminder_logs")
data class ReminderLogEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val phone: String,
    val amount: Double,
    val message: String,
    val sentAtMillis: Long = System.currentTimeMillis()
)

// UI and Aggregate Data Classes
data class CustomerWithBalance(
    val customer: CustomerEntity,
    val totalGave: Double,
    val totalGot: Double,
    val netBalance: Double, // positive = customer owes you (You Will Get), negative = you owe customer (You Will Give)
    val lastTransactionMillis: Long?
)

data class TransactionWithBalance(
    val transaction: TransactionEntity,
    val runningBalance: Double
)

data class DashboardSummary(
    val totalCustomers: Int,
    val totalReceivable: Double, // You will get (> 0)
    val totalPayable: Double, // You will give (< 0)
    val netBalance: Double,
    val todaysCollection: Double, // GOT today
    val todaysPayment: Double, // GAVE today
    val monthlyGave: Double,
    val monthlyGot: Double
)
