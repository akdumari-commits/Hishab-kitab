package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CustomerWithBalance
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.theme.*

@Composable
fun WhatsAppReminderDialog(
    customerWithBalance: CustomerWithBalance,
    currentUser: UserEntity?,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onReminderSent: (message: String) -> Unit
) {
    val context = LocalContext.current
    var selectedLang by remember { mutableStateOf(currentLanguage) }

    val customer = customerWithBalance.customer
    val amountStr = formatCurrency(customerWithBalance.netBalance)
    val bizName = currentUser?.businessName ?: "Our Store"
    val upiPart = if (!currentUser?.upiId.isNullOrBlank()) "\nUPI: ${currentUser?.upiId}" else ""

    val reminderMessage = remember(selectedLang, customerWithBalance, currentUser) {
        if (selectedLang == AppLanguage.HINDI) {
            "नमस्ते ${customer.name} जी,\n\n${bizName} के खाते में आपका ₹$amountStr का बकाया है। कृपया सुविधानुसार इसका भुगतान करने का कष्ट करें।$upiPart\n\nधन्यवाद,\n${bizName}\n(हिसाब प्रो द्वारा प्रेषित)"
        } else {
            "Hello ${customer.name},\n\nThis is a gentle payment reminder regarding your outstanding balance of ₹$amountStr at ${bizName}. Kindly settle the amount at your earliest convenience.$upiPart\n\nThank you,\n${bizName}\n(Sent via Hisab Pro)"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = CreditGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.get("reminder_title", currentLanguage),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = AppStrings.get("reminder_lang_prompt", currentLanguage),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedLang == AppLanguage.HINDI,
                        onClick = { selectedLang = AppLanguage.HINDI },
                        label = { Text("हिंदी (Hindi)") },
                        modifier = Modifier.testTag("reminder_hindi_chip")
                    )
                    FilterChip(
                        selected = selectedLang == AppLanguage.ENGLISH,
                        onClick = { selectedLang = AppLanguage.ENGLISH },
                        label = { Text("English") },
                        modifier = Modifier.testTag("reminder_english_chip")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = AppStrings.get("reminder_preview", currentLanguage),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = reminderMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onReminderSent(reminderMessage)
                    val phone = customer.whatsapp.ifBlank { customer.phone }
                    val cleanPhone = phone.replace(Regex("[^0-9]"), "")
                    val targetPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

                    try {
                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$targetPhone&text=${Uri.encode(reminderMessage)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, reminderMessage)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Send Reminder"))
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CreditGreen),
                modifier = Modifier.testTag("btn_send_whatsapp")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.get("reminder_send_whatsapp", currentLanguage))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Hisab Pro Reminder", reminderMessage)
                    clipboard.setPrimaryClip(clip)
                    onReminderSent(reminderMessage)
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_copy_reminder")
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.get("reminder_copy", currentLanguage))
            }
        }
    )
}

@Composable
fun ReceiptDialog(
    transaction: TransactionEntity,
    customerName: String,
    currentUser: UserEntity?,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isGot = transaction.type == "GOT"
    val accentColor = if (isGot) CreditGreen else DebitRed
    val bizName = currentUser?.businessName ?: "Hisab Pro Merchant"

    val receiptText = buildString {
        appendLine("===============================")
        appendLine("      HISAB PRO RECEIPT        ")
        appendLine("===============================")
        appendLine("Business: $bizName")
        appendLine("Customer: $customerName")
        appendLine("Txn ID: ${transaction.id.take(8).uppercase()}")
        appendLine("Date: ${formatDate(transaction.dateMillis)}")
        appendLine("Type: ${if (isGot) "GOT / RECEIVED (जमा)" else "GAVE / CREDIT (नामे)"}")
        appendLine("Payment Mode: ${transaction.paymentMethod}")
        if (transaction.billNumber.isNotBlank()) appendLine("Bill No: ${transaction.billNumber}")
        appendLine("-------------------------------")
        appendLine("AMOUNT: ₹${formatCurrency(transaction.amount)}")
        if (transaction.remarks.isNotBlank()) appendLine("Notes: ${transaction.remarks}")
        appendLine("===============================")
        appendLine("Generated by Hisab Pro Digital Khata")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isGot) CreditGreenContainer else DebitRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGot) Icons.Default.CheckCircle else Icons.Default.CallMade,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = AppStrings.get("receipt_title", currentLanguage),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = bizName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Receipt Ticket Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = AppStrings.get("customer_name", currentLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = customerName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = AppStrings.get("receipt_id", currentLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "#${transaction.id.take(8).uppercase()}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = AppStrings.get("date_time", currentLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatShortDate(transaction.dateMillis),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = AppStrings.get("payment_method", currentLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = transaction.paymentMethod,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        // Big Amount row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isGot) AppStrings.get("got_money", currentLanguage) else AppStrings.get("gave_money", currentLanguage),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = accentColor)
                            )
                            Text(
                                text = "₹${formatCurrency(transaction.amount)}",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                            )
                        }

                        if (transaction.remarks.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Note: ${transaction.remarks}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(AppStrings.get("cancel", currentLanguage))
                    }

                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, receiptText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Hisab Receipt"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_share_receipt")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.get("share_receipt", currentLanguage))
                    }
                }
            }
        }
    }
}

@Composable
fun PinUnlockOverlay(
    onUnlockSuccess: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    currentLanguage: AppLanguage
) {
    var pinText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { /* Cannot dismiss without pin */ }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = AppStrings.get("enter_pin", currentLanguage),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = AppStrings.get("security_pin", currentLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                            pinText = it
                            isError = false
                            if (it.length == 4) {
                                if (onVerifyPin(it)) {
                                    onUnlockSuccess()
                                } else {
                                    isError = true
                                    pinText = ""
                                }
                            }
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = isError,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .testTag("pin_input_field")
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = AppStrings.get("wrong_pin", currentLanguage),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
