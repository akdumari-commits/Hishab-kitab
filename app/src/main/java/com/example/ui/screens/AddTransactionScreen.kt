package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(viewModel: HisabViewModel) {
    val language by viewModel.appLanguage.collectAsState()
    val customersWithBalances by viewModel.customersWithBalances.collectAsState()

    var selectedCustomerId by remember { mutableStateOf(customersWithBalances.firstOrNull()?.customer?.id ?: "") }
    var txnType by remember { mutableStateOf("GAVE") } // GAVE or GOT
    var amountStr by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var billNo by remember { mutableStateOf("") }

    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

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

            // Customer Selector Card
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
                        TextButton(onClick = { showNewCustomerDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.get("add_customer", language))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val selectedCust = customersWithBalances.find { it.customer.id == selectedCustomerId }?.customer
                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCust?.name ?: "Select Customer",
                            onValueChange = {},
                            readOnly = true,
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

            // Amount Input Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.get("amount", language) + " (₹)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("txn_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(100, 500, 1000, 2000, 5000).forEach { preset ->
                            SuggestionChip(
                                onClick = {
                                    val current = amountStr.toDoubleOrNull() ?: 0.0
                                    amountStr = (current + preset).toInt().toString()
                                },
                                label = { Text("+$preset") }
                            )
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
}
