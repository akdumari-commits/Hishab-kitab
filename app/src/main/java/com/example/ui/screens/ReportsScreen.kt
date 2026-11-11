package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.CustomerWithBalance
import com.example.data.model.TransactionEntity
import com.example.localization.AppStrings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.HisabViewModel
import com.example.ui.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(viewModel: HisabViewModel) {
    val context = LocalContext.current
    val language by viewModel.appLanguage.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val customersWithBalances by viewModel.customersWithBalances.collectAsState()

    var selectedPeriod by remember { mutableStateOf("THIS_MONTH") } // TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, ALL

    // Filter transactions based on selected period
    val periodTransactions = remember(allTransactions, selectedPeriod) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val todayStart = cal.timeInMillis

        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val weekStart = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = cal.timeInMillis

        cal.set(Calendar.DAY_OF_YEAR, 1)
        val yearStart = cal.timeInMillis

        when (selectedPeriod) {
            "TODAY" -> allTransactions.filter { it.dateMillis >= todayStart }
            "THIS_WEEK" -> allTransactions.filter { it.dateMillis >= weekStart }
            "THIS_MONTH" -> allTransactions.filter { it.dateMillis >= monthStart }
            "THIS_YEAR" -> allTransactions.filter { it.dateMillis >= yearStart }
            else -> allTransactions
        }
    }

    val totalGot = remember(periodTransactions) {
        periodTransactions.filter { it.type == "GOT" }.sumOf { it.amount }
    }
    val totalGave = remember(periodTransactions) {
        periodTransactions.filter { it.type == "GAVE" }.sumOf { it.amount }
    }
    val totalVolume = totalGot + totalGave
    val recoveryRate = if (totalGave > 0) ((totalGot / totalGave) * 100).toInt().coerceAtMost(100) else 100

    // Payment methods breakdown
    val paymentModes = remember(periodTransactions) {
        periodTransactions.groupBy { it.paymentMethod }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
    }

    // Top Debtors (Receivable)
    val topDebtors = remember(customersWithBalances) {
        customersWithBalances.filter { it.netBalance > 0 }.sortedByDescending { it.netBalance }.take(5)
    }

    Scaffold(
        topBar = {
            HisabTopAppBar(
                title = AppStrings.get("reports_title", language),
                subtitle = user?.businessName,
                currentLanguage = language,
                onLanguageChange = { viewModel.setLanguage(it) },
                canNavigateBack = false
            )
        },
        bottomBar = {
            HisabBottomNav(
                currentScreen = Screen.REPORTS,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("reports_scroll_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Filter Selector
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedPeriod == "TODAY",
                            onClick = { selectedPeriod = "TODAY" },
                            label = { Text(AppStrings.get("filter_today", language)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedPeriod == "THIS_WEEK",
                            onClick = { selectedPeriod = "THIS_WEEK" },
                            label = { Text(AppStrings.get("filter_this_week", language)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedPeriod == "THIS_MONTH",
                            onClick = { selectedPeriod = "THIS_MONTH" },
                            label = { Text(AppStrings.get("filter_this_month", language)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedPeriod == "THIS_YEAR",
                            onClick = { selectedPeriod = "THIS_YEAR" },
                            label = { Text("This Year") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedPeriod == "ALL",
                            onClick = { selectedPeriod = "ALL" },
                            label = { Text(AppStrings.get("filter_all_time", language)) }
                        )
                    }
                }
            }

            // Turnover & Volume Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = AppStrings.get("total_turnover", language),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(totalVolume),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual Proportional Bar (Green vs Red)
                        val greenRatio = if (totalVolume > 0) (totalGot / totalVolume).toFloat() else 0.5f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(greenRatio.coerceAtLeast(0.02f))
                                    .background(CreditGreen)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight((1f - greenRatio).coerceAtLeast(0.02f))
                                    .background(DebitRed)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CreditGreen))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = AppStrings.get("got_money", language),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(
                                    text = formatCurrency(totalGot),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CreditGreen)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DebitRed))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = AppStrings.get("gave_money", language),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(
                                    text = formatCurrency(totalGave),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DebitRed)
                                )
                            }
                        }
                    }
                }
            }

            // Payment Mode Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = AppStrings.get("payment_modes_split", language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (paymentModes.isEmpty()) {
                            Text(
                                text = "No transactions in this period",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            paymentModes.forEach { (mode, amt) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when (mode) {
                                                "UPI" -> Icons.Default.QrCode
                                                "BANK_TRANSFER" -> Icons.Default.AccountBalance
                                                "CHEQUE" -> Icons.Default.Description
                                                else -> Icons.Default.Payments
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = mode, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(
                                        text = formatCurrency(amt),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Debtors Section
            if (topDebtors.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = AppStrings.get("top_debtors", language),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            topDebtors.forEach { cwb ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cwb.customer.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                        Text(cwb.customer.phone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = formatCurrency(cwb.netBalance),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CreditGreen)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Export Actions (PDF / Statement Share & CSV)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            exportPeriodReport(context, user?.businessName, selectedPeriod, totalGot, totalGave, periodTransactions)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_export_pdf_report")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.get("export_pdf", language))
                    }

                    OutlinedButton(
                        onClick = {
                            exportCsvReport(context, user?.businessName, periodTransactions)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_export_csv_report")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.get("export_csv", language))
                    }
                }
            }
        }
    }
}

fun exportPeriodReport(
    context: Context,
    bizName: String?,
    period: String,
    totalGot: Double,
    totalGave: Double,
    txns: List<TransactionEntity>
) {
    val store = bizName ?: "Hisab Pro Merchant"
    val content = buildString {
        appendLine("==========================================")
        appendLine("       BUSINESS FINANCIAL REPORT          ")
        appendLine("==========================================")
        appendLine("Business: $store")
        appendLine("Period: $period")
        appendLine("Generated: ${formatDate(System.currentTimeMillis())}")
        appendLine("------------------------------------------")
        appendLine("Total Collection (जमा / Got): ₹${formatCurrency(totalGot)}")
        appendLine("Total Credit Given (नामे / Gave): ₹${formatCurrency(totalGave)}")
        appendLine("Total Volume: ₹${formatCurrency(totalGot + totalGave)}")
        appendLine("Total Transaction Count: ${txns.size}")
        appendLine("==========================================")
        appendLine("Generated by Hisab Pro Digital Khata")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "$store - Business Report ($period)")
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, "Share Business Report"))
}

fun exportCsvReport(context: Context, bizName: String?, txns: List<TransactionEntity>) {
    val csv = buildString {
        appendLine("TxnID,Date,Type,Amount,PaymentMethod,BillNo,Remarks")
        for (t in txns) {
            val dStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(t.dateMillis))
            appendLine("${t.id},$dStr,${t.type},${t.amount},${t.paymentMethod},\"${t.billNumber}\",\"${t.remarks}\"")
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "${bizName ?: "HisabPro"}_transactions.csv")
        putExtra(Intent.EXTRA_TEXT, csv)
    }
    context.startActivity(Intent.createChooser(intent, "Export CSV"))
}
