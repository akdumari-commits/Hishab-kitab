package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.localization.AppStrings
import com.example.ui.components.HisabTopAppBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.HisabViewModel
import com.example.util.rememberSingleKeyFilter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(viewModel: HisabViewModel) {
    val language by viewModel.appLanguage.collectAsState()
    val customersWithBalances by viewModel.customersWithBalances.collectAsState()

    var selectedCustomerId by remember { mutableStateOf(customersWithBalances.firstOrNull()?.customer?.id ?: "") }
    var txnType by remember { mutableStateOf("GAVE") } // GAVE or GOT
    var amountStr by remember { mutableStateOf("") }
    val amountFilter = rememberSingleKeyFilter()
    var remarks by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var billNo by remember { mutableStateOf("") }

    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var showQuickAmountDialog by remember { mutableStateOf(false) }
    var showKeypad by remember { mutableStateOf(false) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    var lastPresetClickMillis by remember { mutableLongStateOf(0L) }

    val onAddPreset: (Int) -> Unit = { preset ->
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastPresetClickMillis >= 400L) {
            lastPresetClickMillis = now
            val current = amountStr.toDoubleOrNull() ?: 0.0
            val newAmount = (current + preset).toLong()
            amountStr = newAmount.toString()
        }
    }

    val isGot = txnType == "GOT"
    val accentColor = if (isGot) CreditGreen else DebitRed

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            HisabTopAppBar(
                title = AppStrings.get("nav_add", language),
                subtitle = if (isGot) AppStrings.get("got_money_sub", language) else AppStrings.get("gave_money_sub", language),
                currentLanguage = language,
                onLanguageChange = { viewModel.setLanguage(it) },
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Type Toggle (GAVE vs GOT)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                // Gave (Debit / Red)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isGot) DebitRed else Color.Transparent)
                        .clickable { txnType = "GAVE" }
                        .testTag("toggle_type_gave"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CallMade,
                            contentDescription = null,
                            tint = if (!isGot) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppStrings.get("gave_money", language),
                            fontWeight = FontWeight.Bold,
                            color = if (!isGot) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Got (Credit / Green)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGot) CreditGreen else Color.Transparent)
                        .clickable { txnType = "GOT" }
                        .testTag("toggle_type_got"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CallReceived,
                            contentDescription = null,
                            tint = if (isGot) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppStrings.get("got_money", language),
                            fontWeight = FontWeight.Bold,
                            color = if (isGot) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Customer Selector Card with Prominent Add Customer Button
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.get("customer_name", language),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )

                        // Prominent "+ Add Customer" button
                        Button(
                            onClick = { showNewCustomerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_add_customer_in_txn")
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ " + AppStrings.get("add_customer", language),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val selectedCust = customersWithBalances.find { it.customer.id == selectedCustomerId }?.customer
                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCust?.name ?: if (customersWithBalances.isEmpty()) "No Customers (+ Add New Customer)" else "Select Customer",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("txn_customer_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            // Top explicit option in dropdown: Add New Customer!
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                text = {
                                    Text(
                                        text = "+ " + AppStrings.get("add_customer", language),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    customerDropdownExpanded = false
                                    showNewCustomerDialog = true
                                }
                            )
                            HorizontalDivider()

                            customersWithBalances.forEach { cwb ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(cwb.customer.name, fontWeight = FontWeight.Bold)
                                            Text("${cwb.customer.phone} • ${cwb.customer.customerType}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    onClick = {
                                        selectedCustomerId = cwb.customer.id
                                        customerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input Card with Revived Pop-up up to ₹2000
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.get("amount", language) + " (₹)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )

                        // Revived Quick Amount Pop-up trigger button (up to 2000)
                        Surface(
                            onClick = { showQuickAmountDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            color = accentColor.copy(alpha = 0.12f),
                            modifier = Modifier.testTag("btn_trigger_amount_popup")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "⚡ Pop-up (up to ₹2000)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = amountFilter.filter(amountStr, it) },
                        placeholder = { Text("0.00") },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        ),
                        leadingIcon = {
                            Text(
                                "₹",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                ),
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        trailingIcon = {
                            if (amountStr.isNotBlank()) {
                                IconButton(onClick = { amountStr = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Amount")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            autoCorrectEnabled = false
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("txn_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Preset Chips (up to 2000 only) - debounced so 1 click = 1 add
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf(100, 200, 500, 1000, 2000)) { preset ->
                            Surface(
                                onClick = { onAddPreset(preset) },
                                shape = RoundedCornerShape(12.dp),
                                color = accentColor.copy(alpha = 0.09f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                                modifier = Modifier.testTag("preset_btn_$preset")
                            ) {
                                Text(
                                    text = "+₹$preset",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                        item {
                            Surface(
                                onClick = {
                                    val now = android.os.SystemClock.uptimeMillis()
                                    if (now - lastPresetClickMillis >= 400L) {
                                        lastPresetClickMillis = now
                                        showQuickAmountDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.testTag("preset_btn_popup")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pop-up",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                        item {
                            Surface(
                                onClick = { showKeypad = !showKeypad },
                                shape = RoundedCornerShape(12.dp),
                                color = if (showKeypad) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (showKeypad) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else null,
                                modifier = Modifier.testTag("preset_btn_keypad")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showKeypad) Icons.Default.KeyboardHide else Icons.Default.Dialpad,
                                        contentDescription = null,
                                        tint = if (showKeypad) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showKeypad) "Keypad ✓" else "Keypad",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (showKeypad) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                        if (amountStr.isNotBlank()) {
                            item {
                                Surface(
                                    onClick = { amountStr = "" },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.testTag("preset_btn_clear")
                                ) {
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Interactive On-Screen Number Pad
                    if (showKeypad) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val keypadRows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf(".", "0", "⌫")
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (row in keypadRows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (key in row) {
                                        Surface(
                                            onClick = {
                                                val now = System.currentTimeMillis()
                                                if (now - lastPresetClickMillis >= 250L) {
                                                    lastPresetClickMillis = now
                                                    when (key) {
                                                        "⌫" -> if (amountStr.isNotEmpty()) amountStr = amountStr.dropLast(1)
                                                        "." -> if (!amountStr.contains(".")) amountStr += "."
                                                        else -> {
                                                            if (amountStr == "0") amountStr = key else amountStr += key
                                                        }
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (key == "⌫") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("keypad_btn_$key")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (key == "⌫") {
                                                    Icon(
                                                        imageVector = Icons.Default.Backspace,
                                                        contentDescription = "Backspace",
                                                        tint = DebitRed,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        text = key,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Mode & Details Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.get("payment_method", language),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "CASH" to "method_cash",
                            "UPI" to "method_upi",
                            "BANK_TRANSFER" to "method_bank",
                            "CHEQUE" to "method_cheque"
                        ).forEach { (code, labelKey) ->
                            FilterChip(
                                selected = paymentMethod == code,
                                onClick = { paymentMethod = code },
                                label = { Text(AppStrings.get(labelKey, language)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = billNo,
                        onValueChange = { billNo = it },
                        label = { Text(AppStrings.get("bill_no", language)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text(AppStrings.get("remarks", language)) },
                        placeholder = { Text("E.g. Groceries, payment receipt, bill adjustment...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (selectedCustomerId.isNotBlank() && amount != null && amount > 0) {
                        viewModel.addTransaction(
                            customerId = selectedCustomerId,
                            type = txnType,
                            amount = amount,
                            remarks = remarks,
                            paymentMethod = paymentMethod
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_new_transaction"),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isGot) Icons.Default.CallReceived else Icons.Default.CallMade,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.get("save_transaction", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showNewCustomerDialog) {
        AddOrEditCustomerDialog(
            customer = null,
            language = language,
            onDismiss = { showNewCustomerDialog = false },
            onSave = { newCust ->
                viewModel.addCustomer(newCust)
                selectedCustomerId = newCust.id
                showNewCustomerDialog = false
            }
        )
    }

    if (showQuickAmountDialog) {
        AlertDialog(
            onDismissRequest = { showQuickAmountDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Quick Amount Pop-up",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select note amount (Up to ₹2000)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val presets = listOf(50, 100, 200, 500, 1000, 2000)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (rowItems in presets.chunked(3)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (p in rowItems) {
                                    Button(
                                        onClick = {
                                            onAddPreset(p)
                                            showQuickAmountDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentColor.copy(alpha = 0.15f),
                                            contentColor = accentColor
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) {
                                        Text("₹$p", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickAmountDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
