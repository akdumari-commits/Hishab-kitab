package com.example

import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CustomerWithBalance
import com.example.ui.components.PinUnlockOverlay
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.WhatsAppReminderDialog
import com.example.ui.screens.*
import com.example.ui.theme.HisabProTheme
import com.example.ui.viewmodel.HisabViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    private var lastKeyDownTime = 0L
    private var lastKeyDownCode = -1

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_MULTIPLE) {
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            val now = SystemClock.uptimeMillis()
            // In browser streaming emulators, duplicate hardware KeyDown events fire within ~200ms
            if (event.keyCode == lastKeyDownCode && (now - lastKeyDownTime) < 200L) {
                return true
            }
            lastKeyDownTime = now
            lastKeyDownCode = event.keyCode
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable true immersive edge-to-edge with transparent status & navigation bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            HisabProTheme {
                val viewModel: HisabViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isUnlocked by viewModel.isAppUnlocked.collectAsState()
                val isPinEnabled by viewModel.isPinLockEnabled.collectAsState()
                val reminderCustomer by viewModel.reminderCustomer.collectAsState()
                val receiptTxn by viewModel.viewReceiptTxn.collectAsState()
                val snackbarMsg by viewModel.snackbarMessage.collectAsState()
                val language by viewModel.appLanguage.collectAsState()
                val user by viewModel.currentUser.collectAsState()
                val customersWithBalances by viewModel.customersWithBalances.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(snackbarMsg) {
                    snackbarMsg?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearSnackbar()
                    }
                }

                // Fullscreen container without premature insets consumption
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "screen_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { screen ->
                        when (screen) {
                            Screen.LOGIN -> LoginScreen(viewModel = viewModel)
                            Screen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                            Screen.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
                            Screen.CUSTOMER_DETAIL -> CustomerDetailScreen(viewModel = viewModel)
                            Screen.ADD_TRANSACTION -> AddTransactionScreen(viewModel = viewModel)
                            Screen.REPORTS -> ReportsScreen(viewModel = viewModel)
                            Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }

                    // Snackbar placed safely above system navigation bars or bottom navigation
                    val hasBottomNav = currentScreen in listOf(
                        Screen.DASHBOARD,
                        Screen.CUSTOMERS,
                        Screen.REPORTS,
                        Screen.SETTINGS
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = if (hasBottomNav) 84.dp else 16.dp)
                    )

                    // PIN Lock Screen overlay if locked
                    if (isPinEnabled && !isUnlocked && currentScreen != Screen.LOGIN) {
                        PinUnlockOverlay(
                            onUnlockSuccess = { /* unblocks automatically */ },
                            onVerifyPin = { viewModel.verifyPin(it) },
                            currentLanguage = language
                        )
                    }

                    // Global WhatsApp Reminder Dialog
                    if (reminderCustomer != null) {
                        WhatsAppReminderDialog(
                            customerWithBalance = reminderCustomer!!,
                            currentUser = user,
                            currentLanguage = language,
                            onDismiss = { viewModel.closeReminderDialog() },
                            onReminderSent = { msg ->
                                viewModel.logReminderSent(reminderCustomer!!, msg)
                            }
                        )
                    }

                    // Global Receipt Dialog
                    if (receiptTxn != null) {
                        val custName = customersWithBalances.find { it.customer.id == receiptTxn!!.customerId }?.customer?.name
                            ?: "Customer"
                        ReceiptDialog(
                            transaction = receiptTxn!!,
                            customerName = custName,
                            currentUser = user,
                            currentLanguage = language,
                            onDismiss = { viewModel.closeReceiptDialog() }
                        )
                    }
                }
            }
        }
    }
}
