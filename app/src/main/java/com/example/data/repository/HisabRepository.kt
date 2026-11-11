package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.HisabDatabase
import com.example.data.model.*
import com.example.localization.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class HisabRepository(private val context: Context) {

    private val db = HisabDatabase.getDatabase(context)
    private val userDao = db.userDao()
    private val customerDao = db.customerDao()
    private val transactionDao = db.transactionDao()
    private val reminderLogDao = db.reminderLogDao()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hisab_pro_prefs", Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow(prefs.getString("active_user_id", null))
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        try {
            AppLanguage.valueOf(prefs.getString("app_language", AppLanguage.HINDI.name) ?: AppLanguage.HINDI.name)
        } catch (e: Exception) {
            AppLanguage.HINDI
        }
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _isPinLockEnabled = MutableStateFlow(prefs.getBoolean("pin_lock_enabled", false))
    val isPinLockEnabled: StateFlow<Boolean> = _isPinLockEnabled.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(!prefs.getBoolean("pin_lock_enabled", false))
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _currentUserId.value?.let { uid ->
                _currentUser.value = userDao.getUserById(uid)
            }
        }
    }

    // Language management
    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
        prefs.edit().putString("app_language", language.name).apply()
    }

    // Security PIN
    fun setPin(pin: String) {
        val hashed = hashPassword(pin)
        prefs.edit()
            .putString("security_pin_hash", hashed)
            .putBoolean("pin_lock_enabled", true)
            .apply()
        _isPinLockEnabled.value = true
        _isAppUnlocked.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString("security_pin_hash", "") ?: ""
        val inputHash = hashPassword(pin)
        val valid = storedHash == inputHash
        if (valid) {
            _isAppUnlocked.value = true
        }
        return valid
    }

    fun removePin() {
        prefs.edit()
            .remove("security_pin_hash")
            .putBoolean("pin_lock_enabled", false)
            .apply()
        _isPinLockEnabled.value = false
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        if (_isPinLockEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    // Authentication
    suspend fun registerUser(
        name: String,
        businessName: String,
        phone: String,
        email: String,
        password: String,
        address: String = "",
        upiId: String = ""
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val existing = userDao.getUserByEmailOrPhone(if (email.isNotBlank()) email else phone)
            if (existing != null) {
                return@withContext Result.failure(Exception("Account already exists with this mobile/email"))
            }

            val newUser = UserEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                businessName = businessName.trim(),
                phone = phone.trim(),
                email = email.trim(),
                passwordHash = hashPassword(password),
                address = address.trim(),
                upiId = upiId.trim(),
                isDemo = false
            )
            userDao.insertUser(newUser)
            setActiveUser(newUser)
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(identifier: String, password: String): Result<UserEntity> =
        withContext(Dispatchers.IO) {
            try {
                val user = userDao.getUserByEmailOrPhone(identifier.trim())
                    ?: return@withContext Result.failure(Exception("User not found"))
                if (user.passwordHash != hashPassword(password)) {
                    return@withContext Result.failure(Exception("Incorrect password"))
                }
                setActiveUser(user)
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createDemoAccount(): UserEntity = withContext(Dispatchers.IO) {
        val existingDemo = userDao.getUserByEmailOrPhone("demo@hisabpro.in")
        if (existingDemo != null) {
            setActiveUser(existingDemo)
            return@withContext existingDemo
        }

        val demoUser = UserEntity(
            id = "demo_user_001",
            name = "Rajesh Sharma",
            businessName = "Sharma General & Kirana Store",
            phone = "9876543210",
            email = "demo@hisabpro.in",
            passwordHash = hashPassword("demo123"),
            address = "Shop 12, Main Market, Delhi",
            upiId = "rajeshsharma@upi",
            isDemo = true
        )
        userDao.insertUser(demoUser)
        seedDemoData(demoUser.id)
        setActiveUser(demoUser)
        demoUser
    }

    private suspend fun seedDemoData(userId: String) {
        val now = System.currentTimeMillis()
        val oneDay = 86400000L

        val cust1 = CustomerEntity(
            id = "demo_c1",
            userId = userId,
            name = "Amit Kumar",
            phone = "9811223344",
            whatsapp = "9811223344",
            address = "Sector 14, Delhi",
            customerType = "CUSTOMER",
            openingBalance = 500.0,
            openingBalanceType = "GET",
            notes = "Regular grocery customer",
            createdAt = now - 10 * oneDay,
            updatedAt = now - 1 * oneDay
        )

        val cust2 = CustomerEntity(
            id = "demo_c2",
            userId = userId,
            name = "Pooja Verma",
            phone = "9822334455",
            whatsapp = "9822334455",
            address = "Green Park, Delhi",
            customerType = "CUSTOMER",
            openingBalance = 0.0,
            openingBalanceType = "ZERO",
            notes = "Monthly ration customer",
            createdAt = now - 7 * oneDay,
            updatedAt = now
        )

        val cust3 = CustomerEntity(
            id = "demo_c3",
            userId = userId,
            name = "Kishore Traders (Supplier)",
            phone = "9833445566",
            whatsapp = "9833445566",
            address = "Chandni Chowk Wholesale Market",
            customerType = "SUPPLIER",
            openingBalance = 15000.0,
            openingBalanceType = "GIVE",
            notes = "FMCG Wholesale Distributor",
            createdAt = now - 15 * oneDay,
            updatedAt = now - 2 * oneDay
        )

        customerDao.insertCustomer(cust1)
        customerDao.insertCustomer(cust2)
        customerDao.insertCustomer(cust3)

        // Transactions for cust1
        transactionDao.insertTransaction(
            TransactionEntity(
                id = "demo_t1",
                userId = userId,
                customerId = cust1.id,
                type = "GAVE",
                amount = 1200.0,
                dateMillis = now - 5 * oneDay,
                remarks = "Atta, Dal, Mustard Oil",
                paymentMethod = "CASH"
            )
        )
        transactionDao.insertTransaction(
            TransactionEntity(
                id = "demo_t2",
                userId = userId,
                customerId = cust1.id,
                type = "GOT",
                amount = 1000.0,
                dateMillis = now - 1 * oneDay,
                remarks = "Partial cash payment",
                paymentMethod = "UPI"
            )
        )

        // Transactions for cust2
        transactionDao.insertTransaction(
            TransactionEntity(
                id = "demo_t3",
                userId = userId,
                customerId = cust2.id,
                type = "GAVE",
                amount = 2450.0,
                dateMillis = now - 3 * oneDay,
                remarks = "Spices, dry fruits & dairy items",
                paymentMethod = "CASH"
            )
        )
        transactionDao.insertTransaction(
            TransactionEntity(
                id = "demo_t4",
                userId = userId,
                customerId = cust2.id,
                type = "GOT",
                amount = 1500.0,
                dateMillis = now,
                remarks = "UPI payment via GPay",
                paymentMethod = "UPI"
            )
        )

        // Transactions for cust3 (Supplier)
        transactionDao.insertTransaction(
            TransactionEntity(
                id = "demo_t5",
                userId = userId,
                customerId = cust3.id,
                type = "GOT",
                amount = 5000.0,
                dateMillis = now - 2 * oneDay,
                remarks = "Goods received / Bill #4092",
                paymentMethod = "BANK_TRANSFER"
            )
        )
    }

    private fun setActiveUser(user: UserEntity) {
        _currentUserId.value = user.id
        _currentUser.value = user
        prefs.edit().putString("active_user_id", user.id).apply()
    }

    suspend fun updateProfile(
        name: String,
        businessName: String,
        phone: String,
        address: String,
        upiId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        val updated = current.copy(
            name = name.trim(),
            businessName = businessName.trim(),
            phone = phone.trim(),
            address = address.trim(),
            upiId = upiId.trim()
        )
        userDao.updateUser(updated)
        _currentUser.value = updated
        Result.success(Unit)
    }

    fun logout() {
        _currentUserId.value = null
        _currentUser.value = null
        prefs.edit().remove("active_user_id").apply()
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = _currentUserId.value ?: return@withContext Result.failure(Exception("Not logged in"))
        transactionDao.deleteAllTransactionsForUser(uid)
        customerDao.deleteAllCustomersForUser(uid)
        userDao.deleteUser(uid)
        logout()
        Result.success(Unit)
    }

    // Customer Streams & Operations
    fun getCustomersWithBalances(): Flow<List<CustomerWithBalance>> {
        return currentUserId.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList())
            else {
                combine(
                    customerDao.getCustomersForUser(uid),
                    transactionDao.getTransactionsForUser(uid)
                ) { customers, transactions ->
                    val txnsByCustomer = transactions.groupBy { it.customerId }
                    customers.map { customer ->
                        val customerTxns = txnsByCustomer[customer.id] ?: emptyList()
                        val totalGave = customerTxns.filter { it.type == "GAVE" }.sumOf { it.amount }
                        val totalGot = customerTxns.filter { it.type == "GOT" }.sumOf { it.amount }

                        val openingDelta = when (customer.openingBalanceType) {
                            "GET" -> customer.openingBalance
                            "GIVE" -> -customer.openingBalance
                            else -> 0.0
                        }

                        val netBalance = openingDelta + totalGave - totalGot
                        val lastTxnTime = customerTxns.maxOfOrNull { it.dateMillis } ?: customer.createdAt

                        CustomerWithBalance(
                            customer = customer,
                            totalGave = totalGave,
                            totalGot = totalGot,
                            netBalance = netBalance,
                            lastTransactionMillis = lastTxnTime
                        )
                    }
                }
            }
        }
    }

    suspend fun getCustomerById(customerId: String): CustomerEntity? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(customerId)
    }

    suspend fun saveCustomer(customer: CustomerEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = _currentUserId.value ?: return@withContext Result.failure(Exception("User not authenticated"))
            val custWithUser = customer.copy(userId = uid, updatedAt = System.currentTimeMillis())
            customerDao.insertCustomer(custWithUser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteTransactionsForCustomer(customer.id)
            customerDao.deleteCustomer(customer)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Transactions and Ledger
    fun getCustomerTransactionsWithRunningBalance(customerId: String): Flow<List<TransactionWithBalance>> {
        return flow {
            val customer = customerDao.getCustomerById(customerId)
            val openingDelta = if (customer != null) {
                when (customer.openingBalanceType) {
                    "GET" -> customer.openingBalance
                    "GIVE" -> -customer.openingBalance
                    else -> 0.0
                }
            } else 0.0

            transactionDao.getTransactionsForCustomer(customerId).collect { txns ->
                var running = openingDelta
                val list = mutableListOf<TransactionWithBalance>()
                for (txn in txns) {
                    if (txn.type == "GAVE") {
                        running += txn.amount
                    } else {
                        running -= txn.amount
                    }
                    list.add(TransactionWithBalance(txn, running))
                }
                // Return latest first for ledger screen view
                emit(list.reversed())
            }
        }
    }

    fun getAllUserTransactions(): Flow<List<TransactionEntity>> {
        return currentUserId.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList())
            else transactionDao.getTransactionsForUser(uid)
        }
    }

    suspend fun saveTransaction(txn: TransactionEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = _currentUserId.value ?: return@withContext Result.failure(Exception("Not logged in"))
            val toSave = txn.copy(userId = uid)
            transactionDao.insertTransaction(toSave)
            // Update customer updatedAt
            val customer = customerDao.getCustomerById(txn.customerId)
            if (customer != null) {
                customerDao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(txn: TransactionEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteTransaction(txn)
            val customer = customerDao.getCustomerById(txn.customerId)
            if (customer != null) {
                customerDao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Dashboard calculations
    fun getDashboardSummary(): Flow<DashboardSummary> {
        return getCustomersWithBalances().combine(getAllUserTransactions()) { customersWithBal, allTxns ->
            var receivable = 0.0
            var payable = 0.0

            for (c in customersWithBal) {
                if (c.netBalance > 0.0) {
                    receivable += c.netBalance
                } else if (c.netBalance < 0.0) {
                    payable += kotlin.math.abs(c.netBalance)
                }
            }

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfToday = calendar.timeInMillis

            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = calendar.timeInMillis

            var todayGot = 0.0
            var todayGave = 0.0
            var monthGot = 0.0
            var monthGave = 0.0

            for (t in allTxns) {
                if (t.dateMillis >= startOfToday) {
                    if (t.type == "GOT") todayGot += t.amount
                    else todayGave += t.amount
                }
                if (t.dateMillis >= startOfMonth) {
                    if (t.type == "GOT") monthGot += t.amount
                    else monthGave += t.amount
                }
            }

            DashboardSummary(
                totalCustomers = customersWithBal.size,
                totalReceivable = receivable,
                totalPayable = payable,
                netBalance = receivable - payable,
                todaysCollection = todayGot,
                todaysPayment = todayGave,
                monthlyGave = monthGave,
                monthlyGot = monthGot
            )
        }
    }

    // Reminder logging
    suspend fun logReminder(customerId: String, customerName: String, phone: String, amount: Double, message: String) {
        withContext(Dispatchers.IO) {
            reminderLogDao.insertReminderLog(
                ReminderLogEntity(
                    id = UUID.randomUUID().toString(),
                    customerId = customerId,
                    customerName = customerName,
                    phone = phone,
                    amount = amount,
                    message = message
                )
            )
        }
    }

    // Backup & Restore
    suspend fun exportBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uid = _currentUserId.value ?: return@withContext Result.failure(Exception("No user logged in"))
            val user = userDao.getUserById(uid) ?: return@withContext Result.failure(Exception("User not found"))
            val customers = customerDao.getCustomersForUser(uid).first()
            val transactions = transactionDao.getTransactionsForUser(uid).first()

            val root = JSONObject()
            root.put("version", "1.0")
            root.put("exportedAt", System.currentTimeMillis())

            val userObj = JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("businessName", user.businessName)
                put("phone", user.phone)
                put("email", user.email)
                put("address", user.address)
                put("upiId", user.upiId)
            }
            root.put("user", userObj)

            val customersArr = JSONArray()
            for (c in customers) {
                customersArr.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("phone", c.phone)
                    put("whatsapp", c.whatsapp)
                    put("address", c.address)
                    put("customerType", c.customerType)
                    put("openingBalance", c.openingBalance)
                    put("openingBalanceType", c.openingBalanceType)
                    put("notes", c.notes)
                    put("createdAt", c.createdAt)
                    put("updatedAt", c.updatedAt)
                })
            }
            root.put("customers", customersArr)

            val txnsArr = JSONArray()
            for (t in transactions) {
                txnsArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("customerId", t.customerId)
                    put("type", t.type)
                    put("amount", t.amount)
                    put("dateMillis", t.dateMillis)
                    put("remarks", t.remarks)
                    put("paymentMethod", t.paymentMethod)
                    put("billNumber", t.billNumber)
                    put("createdAt", t.createdAt)
                })
            }
            root.put("transactions", txnsArr)

            prefs.edit().putLong("last_backup_time", System.currentTimeMillis()).apply()
            Result.success(root.toString(2))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val uid = _currentUserId.value ?: return@withContext Result.failure(Exception("No user logged in"))
            val root = JSONObject(jsonString)
            val customersArr = root.optJSONArray("customers") ?: JSONArray()
            val txnsArr = root.optJSONArray("transactions") ?: JSONArray()

            var count = 0
            for (i in 0 until customersArr.length()) {
                val cObj = customersArr.getJSONObject(i)
                val customer = CustomerEntity(
                    id = cObj.getString("id"),
                    userId = uid,
                    name = cObj.getString("name"),
                    phone = cObj.optString("phone", ""),
                    whatsapp = cObj.optString("whatsapp", ""),
                    address = cObj.optString("address", ""),
                    customerType = cObj.optString("customerType", "CUSTOMER"),
                    openingBalance = cObj.optDouble("openingBalance", 0.0),
                    openingBalanceType = cObj.optString("openingBalanceType", "ZERO"),
                    notes = cObj.optString("notes", ""),
                    createdAt = cObj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = cObj.optLong("updatedAt", System.currentTimeMillis())
                )
                customerDao.insertCustomer(customer)
                count++
            }

            for (i in 0 until txnsArr.length()) {
                val tObj = txnsArr.getJSONObject(i)
                val txn = TransactionEntity(
                    id = tObj.getString("id"),
                    userId = uid,
                    customerId = tObj.getString("customerId"),
                    type = tObj.getString("type"),
                    amount = tObj.getDouble("amount"),
                    dateMillis = tObj.getLong("dateMillis"),
                    remarks = tObj.optString("remarks", ""),
                    paymentMethod = tObj.optString("paymentMethod", "CASH"),
                    billNumber = tObj.optString("billNumber", ""),
                    createdAt = tObj.optLong("createdAt", System.currentTimeMillis())
                )
                transactionDao.insertTransaction(txn)
                count++
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLastBackupTime(): Long {
        return prefs.getLong("last_backup_time", 0L)
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
