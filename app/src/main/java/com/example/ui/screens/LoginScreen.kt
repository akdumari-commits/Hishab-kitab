package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.HisabViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: HisabViewModel) {
    val language by viewModel.appLanguage.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    var showRegisterDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language Selection Header on the very first screen before login!
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 24.dp)
                    .testTag("login_language_selector")
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isHindi = language == AppLanguage.HINDI
                    FilterChip(
                        selected = isHindi,
                        onClick = { viewModel.setLanguage(AppLanguage.HINDI) },
                        label = { Text("हिंदी (Hindi)", fontWeight = if (isHindi) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isHindi) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("lang_chip_hindi")
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    FilterChip(
                        selected = !isHindi,
                        onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                        label = { Text("English", fontWeight = if (!isHindi) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (!isHindi) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("lang_chip_english")
                    )
                }
            }

            // App Branding Icon & Title
            Surface(
                shape = CircleShape,
                shadowElevation = 8.dp,
                color = Color(0xFF0A5C36),
                modifier = Modifier
                    .size(80.dp)
                    .testTag("app_logo_branding")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_hisab_logo),
                    contentDescription = "Hisab Pro Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = AppStrings.get("app_name", language),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Text(
                text = AppStrings.get("app_tagline", language),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Card Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = AppStrings.get("login_title", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = AppStrings.get("login_subtitle", language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!authError.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = authError!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        label = { Text(AppStrings.get("mobile_or_email", language)) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_identifier_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(AppStrings.get("password", language)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = AppStrings.get("remember_me", language),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        TextButton(onClick = { showForgotPasswordDialog = true }) {
                            Text(
                                text = AppStrings.get("forgot_password", language),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.login(identifier, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = AppStrings.get("login_btn", language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Demo / Instant Experience button
                    OutlinedButton(
                        onClick = { viewModel.loginDemo() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("login_demo_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("demo_login_btn", language),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Create Account Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = AppStrings.get("no_account_prompt", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showRegisterDialog = true }) {
                    Text(
                        text = AppStrings.get("create_account", language),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Terms & Privacy links
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showPrivacyDialog = true }) {
                    Text(
                        text = AppStrings.get("privacy_policy", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { showTermsDialog = true }) {
                    Text(
                        text = AppStrings.get("terms_conditions", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Register Dialog
    if (showRegisterDialog) {
        RegisterDialog(
            language = language,
            onDismiss = { showRegisterDialog = false },
            onNavigateToForgot = {
                showRegisterDialog = false
                showForgotPasswordDialog = true
            },
            onRegister = { name, biz, phone, email, pass, addr, upi ->
                viewModel.register(name, biz, phone, email, pass, addr, upi)
                showRegisterDialog = false
            }
        )
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            language = language,
            initialIdentifier = identifier,
            viewModel = viewModel,
            onDismiss = { showForgotPasswordDialog = false },
            onPasswordResetSuccess = { resetId ->
                identifier = resetId
                showForgotPasswordDialog = false
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(AppStrings.get("privacy_policy", language)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Hisab Pro values your financial privacy. All customer contact details, ledger balances, and credit/debit records are stored securely in local database storage on your device with option to export encrypted backup files. We never sell, transmit, or share your business data with unauthorized third parties."
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text(AppStrings.get("done", language))
                }
            }
        )
    }

    // Terms Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text(AppStrings.get("terms_conditions", language)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Hisab Pro is a professional digital khata and ledger accounting tool designed for retailers, wholesalers, and individuals. You are responsible for maintaining accurate entries and keeping your security PIN safe."
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) {
                    Text(AppStrings.get("done", language))
                }
            }
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    language: AppLanguage,
    initialIdentifier: String,
    viewModel: HisabViewModel,
    onDismiss: () -> Unit,
    onPasswordResetSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) } // 1: enter identifier, 2: verify OTP & new pass
    var enteredId by remember { mutableStateOf(initialIdentifier) }
    var generatedOtp by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.HINDI) "पासवर्ड रीसेट करें" else "Reset Password",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (step == 1) {
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "अपना पंजीकृत मोबाइल नंबर या ईमेल पता दर्ज करें। हम आपको 6-अंकीय सत्यापन कोड (OTP) भेजेंगे।"
                        else
                            "Enter your registered mobile number or email. We will generate and send a 6-digit verification OTP code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = enteredId,
                        onValueChange = {
                            enteredId = it
                            errorMessage = null
                        },
                        label = { Text(AppStrings.get("mobile_or_email", language)) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_identifier_input")
                    )
                } else {
                    // Step 2: OTP verification and new password
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "सुरक्षा सत्यापन कोड (OTP)" else "Security Verification OTP",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = generatedOtp,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 6.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "(ईमेल ऐप पर भी भेजा गया और यहाँ सीधे उपलब्ध है)"
                                else
                                    "(Dispatched via email intent & displayed for instant access)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = {
                            if (it.length <= 6) {
                                enteredOtp = it.filter { char -> char.isDigit() }
                                errorMessage = null
                            }
                        },
                        label = { Text(if (language == AppLanguage.HINDI) "6-अंकों का OTP कोड" else "6-Digit OTP Code") },
                        leadingIcon = {
                            Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_otp_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            errorMessage = null
                        },
                        label = { Text(if (language == AppLanguage.HINDI) "नया पासवर्ड" else "New Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_new_pass_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text(AppStrings.get("confirm_password", language)) },
                        leadingIcon = {
                            Icon(Icons.Default.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle confirm password visibility"
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_confirm_pass_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = {
                            step = 1
                            errorMessage = null
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "नंबर बदलें / पुनः भेजें" else "Change ID / Resend Code",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(
                    onClick = {
                        val trimmedId = enteredId.trim()
                        if (trimmedId.isBlank()) {
                            errorMessage = if (language == AppLanguage.HINDI)
                                "कृपया मोबाइल नंबर या ईमेल दर्ज करें"
                            else
                                "Please enter your mobile number or email"
                            return@Button
                        }
                        isLoading = true
                        coroutineScope.launch {
                            val userExists = viewModel.checkUserExists(trimmedId)
                            isLoading = false
                            if (userExists) {
                                val otp = (100000..999999).random().toString()
                                generatedOtp = otp
                                step = 2
                                errorMessage = null

                                // Dispatch email intent
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    if (trimmedId.contains("@")) {
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(trimmedId))
                                    }
                                    putExtra(Intent.EXTRA_SUBJECT, "Hisab Pro - Password Reset OTP Code: $otp")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Hello,\n\nYour 6-digit Hisab Pro password reset verification code is:\n\n$otp\n\nUse this code to set a new password. If you did not make this request, please contact support.\n\nBest regards,\nHisab Pro Team"
                                    )
                                }
                                try {
                                    context.startActivity(Intent.createChooser(emailIntent, "Send Password Reset OTP"))
                                } catch (_: Exception) {}
                            } else {
                                errorMessage = if (language == AppLanguage.HINDI)
                                    "इस मोबाइल या ईमेल से कोई खाता पंजीकृत नहीं है। कृपया नया खाता बनाएँ।"
                                else
                                    "No registered account found with this mobile or email. Please register or verify the number."
                            }
                        }
                    },
                    modifier = Modifier.testTag("forgot_send_code_btn")
                ) {
                    Text(if (language == AppLanguage.HINDI) "कोड प्राप्त करें" else "Send Reset Code")
                }
            } else {
                Button(
                    onClick = {
                        val trimmedId = enteredId.trim()
                        if (enteredOtp.trim() != generatedOtp) {
                            errorMessage = if (language == AppLanguage.HINDI) "अमान्य OTP कोड" else "Invalid OTP code"
                            return@Button
                        }
                        if (newPassword.length < 4) {
                            errorMessage = if (language == AppLanguage.HINDI)
                                "पासवर्ड कम से कम 4 अक्षरों का होना चाहिए"
                            else
                                "Password must be at least 4 characters"
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            errorMessage = if (language == AppLanguage.HINDI)
                                "पासवर्ड मेल नहीं खा रहे हैं"
                            else
                                "Passwords do not match"
                            return@Button
                        }

                        isLoading = true
                        viewModel.resetPassword(trimmedId, newPassword) { success, msg ->
                            isLoading = false
                            if (success) {
                                onPasswordResetSuccess(trimmedId)
                            } else {
                                errorMessage = msg
                            }
                        }
                    },
                    modifier = Modifier.testTag("forgot_save_pass_btn")
                ) {
                    Text(if (language == AppLanguage.HINDI) "पासवर्ड सेव करें" else "Set New Password")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}

@Composable
fun RegisterDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onNavigateToForgot: () -> Unit = {},
    onRegister: (name: String, bizName: String, phone: String, email: String, pass: String, addr: String, upi: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bizName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var upi by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.get("register_title", language),
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
                    label = { Text(AppStrings.get("full_name", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bizName,
                    onValueChange = { bizName = it },
                    label = { Text(AppStrings.get("business_name", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reg_biz_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(AppStrings.get("phone_number", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reg_phone_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(AppStrings.get("email", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text(AppStrings.get("password", language)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reg_pass_input")
                )
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
                    value = upi,
                    onValueChange = { upi = it },
                    label = { Text(AppStrings.get("upi_id", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = {
                        onDismiss()
                        onNavigateToForgot()
                    }) {
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "पासवर्ड भूल गए? यहाँ रीसेट करें"
                            else
                                "Forgot password? Reset here",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRegister(name, bizName, phone, email, pass, address, upi)
                },
                modifier = Modifier.testTag("reg_confirm_btn")
            ) {
                Text(AppStrings.get("register_btn", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}
