package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerWithBalance
import com.example.localization.AppStrings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.HisabViewModel
import com.example.ui.viewmodel.Screen
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(viewModel: HisabViewModel) {
    val language by viewModel.appLanguage.collectAsState()
    val customersWithBalances by viewModel.customersWithBalances.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, GET, GIVE, SETTLED
    var selectedSort by remember { mutableStateOf("RECENT") } // RECENT, AMOUNT, NAME
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    // Filter and Sort Customers
    val filteredCustomers = remember(customersWithBalances, searchQuery, selectedFilter, selectedSort) {
        customersWithBalances.filter { cwb ->
            val matchesQuery = cwb.customer.name.contains(searchQuery, ignoreCase = true) ||
                    cwb.customer.phone.contains(searchQuery) ||
                    cwb.customer.whatsapp.contains(searchQuery)

            val matchesFilter = when (selectedFilter) {
                "GET" -> cwb.netBalance > 0.01
                "GIVE" -> cwb.netBalance < -0.01
                "SETTLED" -> kotlin.math.abs(cwb.netBalance) <= 0.01
                else -> true
            }

            matchesQuery && matchesFilter
        }.sortedWith { a, b ->
            when (selectedSort) {
                "AMOUNT" -> kotlin.math.abs(b.netBalance).compareTo(kotlin.math.abs(a.netBalance))
                "NAME" -> a.customer.name.compareTo(b.customer.name, ignoreCase = true)
                else -> (b.lastTransactionMillis ?: 0L).compareTo(a.lastTransactionMillis ?: 0L)
            }
        }
    }

    Scaffold(
        topBar = {
            HisabTopAppBar(
                title = AppStrings.get("customers_title", language),
                subtitle = "${filteredCustomers.size} ${AppStrings.get("total_customers", language)}",
                currentLanguage = language,
                onLanguageChange = { viewModel.setLanguage(it) },
                canNavigateBack = false
            )
        },
        bottomBar = {
            HisabBottomNav(
                currentScreen = Screen.CUSTOMERS,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(AppStrings.get("search_customers", language)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("customer_search_input")
            )

            // Filter Chips (All, You Will Get, You Will Give, Settled)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text(AppStrings.get("filter_all", language)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "GET",
                        onClick = { selectedFilter = "GET" },
                        label = { Text(AppStrings.get("filter_get", language)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CreditGreenContainer,
                            selectedLabelColor = CreditGreen
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "GIVE",
                        onClick = { selectedFilter = "GIVE" },
                        label = { Text(AppStrings.get("filter_give", language)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DebitRedContainer,
                            selectedLabelColor = DebitRed
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "SETTLED",
                        onClick = { selectedFilter = "SETTLED" },
                        label = { Text(AppStrings.get("filter_settled", language)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sort Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredCustomers.size} Records",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Sort: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (selectedSort) {
                            "AMOUNT" -> AppStrings.get("sort_amount", language)
                            "NAME" -> AppStrings.get("sort_name", language)
                            else -> AppStrings.get("sort_recent", language)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .clickable {
                                selectedSort = when (selectedSort) {
                                    "RECENT" -> "AMOUNT"
                                    "AMOUNT" -> "NAME"
                                    else -> "RECENT"
                                }
                            }
                            .padding(4.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Customer List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = AppStrings.get("empty_customers", language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers) { cwb ->
                        CustomerListItem(
                            cwb = cwb,
                            language = language,
                            onClick = { viewModel.selectCustomer(cwb.customer.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        AddOrEditCustomerDialog(
            customer = null,
            language = language,
            onDismiss = { showAddCustomerDialog = false },
            onSave = { newCust ->
                viewModel.addCustomer(newCust)
                showAddCustomerDialog = false
            }
        )
    }
}

@Composable
fun CustomerListItem(
    cwb: CustomerWithBalance,
    language: com.example.localization.AppLanguage,
    onClick: () -> Unit
) {
    val cust = cwb.customer
    val net = cwb.netBalance
    val isSettled = kotlin.math.abs(net) <= 0.01
    val isGet = net > 0.01 // Customer owes you (You will get)
    val statusColor = if (isSettled) MaterialTheme.colorScheme.onSurfaceVariant else if (isGet) CreditGreen else DebitRed

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_item_${cust.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar circle with first letter
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cust.name.firstOrNull()?.uppercase() ?: "C",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cust.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (cust.customerType == "SUPPLIER") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = RoyalBlueContainer
                            ) {
                                Text(
                                    text = AppStrings.get("type_supplier", language),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = RoyalBlue),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = cust.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (cwb.lastTransactionMillis != null) {
                        Text(
                            text = formatShortDate(cwb.lastTransactionMillis),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Balance column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isSettled) "₹0.00" else formatCurrency(net),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = statusColor
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
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun AddOrEditCustomerDialog(
    customer: CustomerEntity?,
    language: com.example.localization.AppLanguage,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var whatsapp by remember { mutableStateOf(customer?.whatsapp ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var customerType by remember { mutableStateOf(customer?.customerType ?: "CUSTOMER") }
    var openingBalStr by remember { mutableStateOf(if (customer != null && customer.openingBalance > 0) customer.openingBalance.toString() else "") }
    var openingType by remember { mutableStateOf(customer?.openingBalanceType ?: "GET") } // GET, GIVE, ZERO
    var notes by remember { mutableStateOf(customer?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (customer == null) AppStrings.get("add_customer", language) else AppStrings.get("edit_customer", language),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.get("customer_name", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cust_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        if (whatsapp.isEmpty()) whatsapp = it
                    },
                    label = { Text(AppStrings.get("phone_number", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cust_phone_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text(AppStrings.get("whatsapp_number", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Customer Type selection
                Text(
                    text = AppStrings.get("customer_type", language),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = customerType == "CUSTOMER",
                        onClick = { customerType = "CUSTOMER" },
                        label = { Text(AppStrings.get("type_customer", language)) }
                    )
                    FilterChip(
                        selected = customerType == "SUPPLIER",
                        onClick = { customerType = "SUPPLIER" },
                        label = { Text(AppStrings.get("type_supplier", language)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Opening balance
                OutlinedTextField(
                    value = openingBalStr,
                    onValueChange = { openingBalStr = it },
                    label = { Text(AppStrings.get("opening_balance", language) + " (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (openingBalStr.toDoubleOrNull() != null && openingBalStr.toDouble() > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = openingType == "GET",
                            onClick = { openingType = "GET" },
                            label = { Text(AppStrings.get("you_will_get", language)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CreditGreenContainer,
                                selectedLabelColor = CreditGreen
                            )
                        )
                        FilterChip(
                            selected = openingType == "GIVE",
                            onClick = { openingType = "GIVE" },
                            label = { Text(AppStrings.get("you_will_give", language)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DebitRedContainer,
                                selectedLabelColor = DebitRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(AppStrings.get("business_address", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppStrings.get("notes", language)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val obAmount = openingBalStr.toDoubleOrNull() ?: 0.0
                        val newCust = CustomerEntity(
                            id = customer?.id ?: UUID.randomUUID().toString(),
                            userId = customer?.userId ?: "",
                            name = name.trim(),
                            phone = phone.trim(),
                            whatsapp = whatsapp.trim().ifBlank { phone.trim() },
                            address = address.trim(),
                            customerType = customerType,
                            openingBalance = obAmount,
                            openingBalanceType = if (obAmount > 0) openingType else "ZERO",
                            notes = notes.trim(),
                            createdAt = customer?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(newCust)
                    }
                },
                modifier = Modifier.testTag("cust_save_btn")
            ) {
                Text(AppStrings.get("save", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}
