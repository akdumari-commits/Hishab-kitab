package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.HisabRepository
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    LOGIN,
    DASHBOARD,
    CUSTOMERS,
    CUSTOMER_DETAIL,
    ADD_TRANSACTION,
    REPORTS,
    SETTINGS
}

class HisabViewModel(application: Application) : AndroidViewModel(application) {

    val repository = HisabRepository(application)

    // Language
    val appLanguage: StateFlow<AppLanguage> = repository.appLanguage

    fun setLanguage(language: AppLanguage) {
        repository.setLanguage(language)
    }

    fun string(key: String): String {
        return AppStrings.get(key, appLanguage.value)
    }

    // Navigation Stack for smooth back handling
    private val _screenStack = MutableStateFlow(listOf(Screen.LOGIN))
    val currentScreen: StateFlow<Screen> = _screenStack.map { it.lastOrNull() ?: Screen.LOGIN }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Screen.LOGIN)

    fun navigateTo(screen: Screen) {
        val currentList = _screenStack.value.toMutableList()
        // If bottom nav tab, reset stack to that tab or avoid infinite deep stacks
        if (screen in listOf(Screen.DASHBOARD, Screen.CUSTOMERS, Screen.REPORTS, Screen.SETTINGS)) {
            _screenStack.value = listOf(screen)
        } else {
            currentList.add(screen)
            _screenStack.value = currentList
        }
    }

    fun navigateBack(): Boolean {
        val currentList = _screenStack.value.toMutableList()
        return if (currentList.size > 1) {
            currentList.removeAt(currentList.size - 1)
            _screenStack.value = currentList
            true
        } else {
            false
        }
    }

    // User & Auth State
    val currentUser: StateFlow<UserEntity?> = repository.currentUser
    val isAppUnlocked: StateFlow<Boolean> = repository.isAppUnlocked
    val isPinLockEnabled: StateFlow<Boolean> = repository.isPinLockEnabled

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    init {
        viewModelScope.launch {
            repository.currentUserId.collect { uid ->
                if (uid != null) {
                    if (currentScreen.value == Screen.LOGIN) {
                        _screenStack.value = listOf(Screen.DASHBOARD)
                    }
                } else {
                    _screenStack.value = listOf(Screen.LOGIN)
                }
            }
        }
    }

    fun login(identifier: String, pass: String) {
        if (identifier.isBlank() || pass.isBlank()) {
            _authError.value = string("required_field")
            return
        }
        viewModelScope.launch {
            _authError.value = null
            val res = repository.loginUser(identifier, pass)
            res.onSuccess {
                _screenStack.value = listOf(Screen.DASHBOARD)
            }.onFailure {
                _authError.value = it.message ?: string("error")
            }
        }
    }

    fun loginDemo() {
        viewModelScope.launch {
            _authError.value = null
            repository.createDemoAccount()
            _screenStack.value = listOf(Screen.DASHBOARD)
            showMessage(string("success"))
        }
    }

    fun register(
        name: String,
        businessName: String,
        phone: String,
        email: String,
        pass: String,
        address: String,
        upiId: String
    ) {
        if (name.isBlank() || businessName.isBlank() || phone.isBlank() || pass.isBlank()) {
            _authError.value = string("required_field")
            return
        }
        viewModelScope.launch {
            _authError.value = null
            val res = repository.registerUser(name, businessName, phone, email, pass, address, upiId)
            res.onSuccess {
                _screenStack.value = listOf(Screen.DASHBOARD)
                showMessage(string("success"))
            }.onFailure {
                _authError.value = it.message ?: string("error")
            }
        }
    }

    fun logout() {
        repository.logout()
        _screenStack.value = listOf(Screen.LOGIN)
    }

    fun deleteAccount() {
        viewModelScope.launch {
            repository.deleteAccount()
            _screenStack.value = listOf(Screen.LOGIN)
        }
    }

    // PIN Lock
    fun verifyPin(pin: String): Boolean {
        return repository.verifyPin(pin)
    }

    fun setPin(pin: String) {
        repository.setPin(pin)
        showMessage(string("success"))
    }

    fun removePin() {
        repository.removePin()
        showMessage(string("success"))
    }

    // Customer & Ledger State
    val customersWithBalances: StateFlow<List<CustomerWithBalance>> =
        repository.getCustomersWithBalances()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardSummary: StateFlow<DashboardSummary> =
        repository.getDashboardSummary()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                DashboardSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
            )

    val allTransactions: StateFlow<List<TransactionEntity>> =
        repository.getAllUserTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Customer & Ledger
    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomerId: StateFlow<String?> = _selectedCustomerId.asStateFlow()

    val selectedCustomer: StateFlow<CustomerWithBalance?> =
        combine(_selectedCustomerId, customersWithBalances) { id, list ->
            list.find { it.customer.id == id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedCustomerTransactions: StateFlow<List<TransactionWithBalance>> =
        _selectedCustomerId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getCustomerTransactionsWithRunningBalance(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCustomer(customerId: String) {
        _selectedCustomerId.value = customerId
        navigateTo(Screen.CUSTOMER_DETAIL)
    }

    fun addCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showMessage(string("success"))
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showMessage(string("success"))
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            if (_selectedCustomerId.value == customer.id) {
                navigateBack()
            }
            showMessage(string("success"))
        }
    }

    // Transaction Management
    fun addTransaction(
        customerId: String,
        type: String, // GAVE or GOT
        amount: Double,
        remarks: String,
        paymentMethod: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val txn = TransactionEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = repository.currentUserId.value ?: "",
                customerId = customerId,
                type = type,
                amount = amount,
                dateMillis = dateMillis,
                remarks = remarks,
                paymentMethod = paymentMethod
            )
            repository.saveTransaction(txn)
            showMessage(string("success"))
            // If in AddTransaction screen, pop back
            if (currentScreen.value == Screen.ADD_TRANSACTION) {
                navigateBack()
            }
        }
    }

    fun deleteTransaction(txn: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(txn)
            showMessage(string("success"))
        }
    }

    fun logReminderSent(customer: CustomerWithBalance, message: String) {
        viewModelScope.launch {
            repository.logReminder(
                customerId = customer.customer.id,
                customerName = customer.customer.name,
                phone = customer.customer.phone,
                amount = customer.netBalance,
                message = message
            )
        }
    }

    // Active Dialogs: WhatsApp Reminder & Receipt
    private val _reminderCustomer = MutableStateFlow<CustomerWithBalance?>(null)
    val reminderCustomer: StateFlow<CustomerWithBalance?> = _reminderCustomer.asStateFlow()

    fun openReminderDialog(customer: CustomerWithBalance) {
        _reminderCustomer.value = customer
    }

    fun closeReminderDialog() {
        _reminderCustomer.value = null
    }

    private val _viewReceiptTxn = MutableStateFlow<TransactionEntity?>(null)
    val viewReceiptTxn: StateFlow<TransactionEntity?> = _viewReceiptTxn.asStateFlow()

    fun openReceiptDialog(txn: TransactionEntity) {
        _viewReceiptTxn.value = txn
    }

    fun closeReceiptDialog() {
        _viewReceiptTxn.value = null
    }

    // Profile & Business update
    fun updateBusinessProfile(name: String, bizName: String, phone: String, address: String, upiId: String) {
        viewModelScope.launch {
            repository.updateProfile(name, bizName, phone, address, upiId)
            showMessage(string("success"))
        }
    }

    // Backup & Restore
    fun exportBackup(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.exportBackupJson()
            res.onSuccess {
                showMessage(string("backup_success"))
                onResult(it)
            }.onFailure {
                showMessage(it.message ?: string("error"))
                onResult(null)
            }
        }
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            val res = repository.restoreBackupJson(jsonString)
            res.onSuccess { count ->
                showMessage("${string("restore_success")} ($count records)")
            }.onFailure {
                showMessage(it.message ?: string("error"))
            }
        }
    }
}
