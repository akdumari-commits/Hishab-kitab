package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.localization.AppStrings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.HisabViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(viewModel: HisabViewModel) {
    val context = LocalContext.current
    val language by viewModel.appLanguage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedCustomerWithBal by viewModel.selectedCustomer.collectAsState()
    val transactionsWithRunning by viewModel.selectedCustomerTransactions.collectAsState()

    var selectedDateFilter by remember { mutableStateOf("ALL") } // ALL, TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH
    var showEditCustomerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddTxnType by remember { mutableStateOf<String?>(null) } // "GAVE" or "GOT"

    // Intercept back button to return to Customers screen
    BackHandler {
        viewModel.navigateBack()
    }

    if (selectedCustomerWithBal == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val customer = selectedCustomerWithBal!!.customer
    val netBalance = selectedCustomerWithBal!!.netBalance
    val isGet = netBalance > 0.01
    val isSettled = kotlin.math.abs(netBalance) <= 0.01
    val netColor = if (isSettled) MaterialTheme.colorScheme.onSurfaceVariant else if (isGet) CreditGreen else DebitRed

    // Filter transactions by date range
    val filteredTxns = remember(transactionsWithRunning, selectedDateFilter) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val yesterdayStart = todayStart - 86400000L

        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val weekStart = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = cal.timeInMillis

        when (selectedDateFilter) {
            "TODAY" -> transactionsWithRunning.filter { it.transaction.dateMillis >= todayStart }
            "YESTERDAY" -> transactionsWithRunning.filter { it.transaction.dateMillis in yesterdayStart until todayStart }
            "THIS_WEEK" -> transactionsWithRunning.filter { it.transaction.dateMillis >= weekStart }
            "THIS_MONTH" -> transactionsWithRunning.filter { it.transaction.dateMillis >= monthStart }
            else -> transactionsWithRunning
        }
    }

    Scaffold(
        topBar = {
            HisabTopAppBar(
                title = customer.name,
                subtitle = customer.phone,
                currentLanguage = language,
                onLanguageChange = { viewModel.setLanguage(it) },
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() },
                actions = {
                    IconButton(onClick = { showEditCustomerDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Customer")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Customer")
                    }
                }
            )
        },
        bottomBar = {
            // Prominent GAVE / GOT buttons at the bottom of the ledger
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // GAVE MONEY (Red button)
                    Button(
                        onClick = { showAddTxnType = "GAVE" },
                        colors = ButtonDefaults.buttonColors(containerColor = DebitRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_gave_money")
                    ) {
                        Icon(Icons.Default.CallMade, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = AppStrings.get("gave_money", language),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    // GOT MONEY (Green button)
                    Button(
                        onClick = { showAddTxnType = "GOT" },
                        colors = ButtonDefaults.buttonColors(containerColor = CreditGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_got_money")
                    ) {
                        Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = AppStrings.get("got_money", language),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("ledger_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Header Info Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSettled) "₹0.00" else formatCurrency(netBalance),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = netColor
                                    )
                                )
                                Text(
                                    text = if (isSettled) {
                                        AppStrings.get("zero_balance", language)
                                    } else if (isGet) {
                                        AppStrings.get("you_will_get", language)
                                    } else {
                                        AppStrings.get("you_will_give", language)
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = netColor
                                )
                            }

                            // Action buttons: Call, WhatsApp Reminder, Share Statement
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Call Button
                                IconButton(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                        context.startActivity(dialIntent)
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(RoyalBlueContainer)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = RoyalBlue, modifier = Modifier.size(20.dp))
                                }

                                // WhatsApp Reminder Button
                                IconButton(
                                    onClick = { viewModel.openReminderDialog(selectedCustomerWithBal!!) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(CreditGreenContainer)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "Reminder", tint = CreditGreen, modifier = Modifier.size(20.dp))
                                }

                                // Statement Share Button
                                IconButton(
                                    onClick = {
                                        shareCustomerStatement(context, customer, transactionsWithRunning, netBalance, currentUser?.businessName)
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(AmberGoldContainer)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Statement", tint = AmberGold, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // Opening balance info if non-zero
                        if (customer.openingBalance > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = AppStrings.get("opening_balance", language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${formatCurrency(customer.openingBalance)} (${if (customer.openingBalanceType == "GET") "Get" else "Give"})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }

            // Date Filters Row
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedDateFilter == "ALL",
                            onClick = { selectedDateFilter = "ALL" },
                            label = { Text(AppStrings.get("filter_all_time", language)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDateFilter == "TODAY",
                            onClick = { selectedDateFilter = "TODAY" },
                            label = { Text(AppStrings.get("filter_today", language)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDateFilter == "YESTERDAY",
                            onClick = { selectedDateFilter = "YESTERDAY" },
                            label = { Text(AppStrings.get("filter_yesterday", language)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDateFilter == "THIS_WEEK",
                            onClick = { selectedDateFilter = "THIS_WEEK" },
                            label = { Text(AppStrings.get("filter_this_week", language)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDateFilter == "THIS_MONTH",
                            onClick = { selectedDateFilter = "THIS_MONTH" },
                            label = { Text(AppStrings.get("filter_this_month", language)) }
                        )
                    }
                }
            }

            // Ledger Header Column Labels
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = AppStrings.get("date_time", language) + " / " + AppStrings.get("remarks", language),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(
                            text = AppStrings.get("gave_money", language),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DebitRed
                        )
                        Text(
                            text = AppStrings.get("got_money", language),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CreditGreen
                        )
                    }
                }
            }

            // Transactions List
            if (filteredTxns.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AppStrings.get("empty_transactions", language),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(filteredTxns) { item ->
                    val txn = item.transaction
                    val isGot = txn.type == "GOT"

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openReceiptDialog(txn) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = formatShortDate(txn.dateMillis),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${txn.paymentMethod}${if (txn.billNumber.isNotBlank()) " • Bill #${txn.billNumber}" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (txn.remarks.isNotBlank()) {
                                        Text(
                                            text = txn.remarks,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = (if (isGot) "+" else "-") + formatCurrency(txn.amount),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isGot) CreditGreen else DebitRed
                                        )
                                    )
                                    Text(
                                        text = "Bal: ${formatCurrency(item.runningBalance)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Customer Dialog
    if (showEditCustomerDialog) {
        AddOrEditCustomerDialog(
            customer = customer,
            language = language,
            onDismiss = { showEditCustomerDialog = false },
            onSave = { updated ->
                viewModel.updateCustomer(updated)
                showEditCustomerDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(AppStrings.get("delete_customer", language)) },
            text = { Text(AppStrings.get("delete_customer_confirm", language)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DebitRed)
                ) {
                    Text(AppStrings.get("confirm", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    // Quick Add Transaction Dialog
    if (showAddTxnType != null) {
        QuickAddTransactionDialog(
            customerId = customer.id,
            customerName = customer.name,
            transactionType = showAddTxnType!!,
            language = language,
            onDismiss = { showAddTxnType = null },
            onSave = { amount, remarks, method ->
                viewModel.addTransaction(
                    customerId = customer.id,
                    type = showAddTxnType!!,
                    amount = amount,
                    remarks = remarks,
                    paymentMethod = method
                )
                showAddTxnType = null
            }
        )
    }
}

@Composable
fun QuickAddTransactionDialog(
    customerId: String,
    customerName: String,
    transactionType: String, // GAVE or GOT
    language: com.example.localization.AppLanguage,
    onDismiss: () -> Unit,
    onSave: (amount: Double, remarks: String, method: String) -> Unit
) {
    val isGot = transactionType == "GOT"
    var amountStr by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("CASH") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isGot) CreditGreenContainer else DebitRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGot) Icons.Default.CallReceived else Icons.Default.CallMade,
                        contentDescription = null,
                        tint = if (isGot) CreditGreen else DebitRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isGot) AppStrings.get("got_money", language) else AppStrings.get("gave_money", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = customerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(AppStrings.get("amount", language) + " (₹)") },
                    placeholder = { Text("0.00") },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_txn_amount_input")
                )

                // Quick Amount Presets (+100, +500, +1000, +2000)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(100, 500, 1000, 2000).forEach { preset ->
                        AssistChip(
                            onClick = {
                                val current = amountStr.toDoubleOrNull() ?: 0.0
                                amountStr = (current + preset).toInt().toString()
                            },
                            label = { Text("+$preset") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Method Chips
                Text(
                    text = AppStrings.get("payment_method", language),
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("CASH" to "method_cash", "UPI" to "method_upi", "BANK" to "method_bank").forEach { (code, key) ->
                        FilterChip(
                            selected = method == code,
                            onClick = { method = code },
                            label = { Text(AppStrings.get(key, language)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text(AppStrings.get("remarks", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onSave(amount, remarks, method)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGot) CreditGreen else DebitRed
                ),
                modifier = Modifier.testTag("quick_txn_save_btn")
            ) {
                Text(AppStrings.get("save_transaction", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}

fun shareCustomerStatement(
    context: android.content.Context,
    customer: com.example.data.model.CustomerEntity,
    txns: List<com.example.data.model.TransactionWithBalance>,
    netBalance: Double,
    bizName: String?
) {
    val store = bizName ?: "Hisab Pro Merchant"
    val report = buildString {
        appendLine("==========================================")
        appendLine("       CUSTOMER ACCOUNT STATEMENT         ")
        appendLine("==========================================")
        appendLine("Business: $store")
        appendLine("Customer: ${customer.name} (${customer.phone})")
        appendLine("Generated: ${formatDate(System.currentTimeMillis())}")
        appendLine("------------------------------------------")
        appendLine(String.format("%-12s %-8s %-8s %-8s", "DATE", "GAVE", "GOT", "BALANCE"))
        appendLine("------------------------------------------")
        for (item in txns.reversed()) {
            val t = item.transaction
            val gaveStr = if (t.type == "GAVE") "₹${t.amount.toInt()}" else "-"
            val gotStr = if (t.type == "GOT") "₹${t.amount.toInt()}" else "-"
            val balStr = "₹${item.runningBalance.toInt()}"
            val dStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(t.dateMillis))
            appendLine(String.format("%-12s %-8s %-8s %-8s", dStr, gaveStr, gotStr, balStr))
        }
        appendLine("==========================================")
        appendLine("NET OUTSTANDING BALANCE: ₹${formatCurrency(netBalance)}")
        appendLine(if (netBalance >= 0) "Status: Customer owes $store" else "Status: $store owes Customer")
        appendLine("==========================================")
        appendLine("Generated by Hisab Pro Digital Ledger")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${customer.name} - Statement from $store")
        putExtra(Intent.EXTRA_TEXT, report)
    }
    context.startActivity(Intent.createChooser(intent, "Share Statement"))
}
