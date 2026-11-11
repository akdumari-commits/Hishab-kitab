package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.localization.AppLanguage
import com.example.ui.theme.*
import com.example.ui.viewmodel.Screen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val formatted = formatter.format(kotlin.math.abs(amount))
    return formatted.replace("INR", "₹").replace("Rs.", "₹").trim()
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun formatShortDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabTopAppBar(
    title: String,
    subtitle: String? = null,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        windowInsets = TopAppBarDefaults.windowInsets,
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            } else {
                // Attractive custom app logo in Top Bar
                Surface(
                    shape = CircleShape,
                    shadowElevation = 2.dp,
                    color = Color(0xFF0A5C36),
                    modifier = Modifier
                        .padding(start = 12.dp, end = 8.dp)
                        .size(36.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_hisab_logo),
                        contentDescription = "Hisab Pro Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(CircleShape)
                    )
                }
            }
        },
        actions = {
            LanguageToggleChip(
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange
            )
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun LanguageToggleChip(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = {
            val next = if (currentLanguage == AppLanguage.HINDI) AppLanguage.ENGLISH else AppLanguage.HINDI
            onLanguageChange(next)
        },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        modifier = modifier
            .padding(horizontal = 6.dp)
            .height(34.dp)
            .testTag("language_toggle_chip")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Language",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (currentLanguage == AppLanguage.HINDI) "हिंदी (EN)" else "English (हिं)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

/**
 * Composite avatar for customers with photo or upload/camera icon,
 * plus a small directional arrow badge (↑ gave/debit, ↓ got/credit) near the image icon.
 */
@Composable
fun CustomerAvatarBadge(
    photoUri: String?,
    customerName: String,
    modifier: Modifier = Modifier,
    isGot: Boolean? = null, // null = no arrow; true = Got (green ↓); false = Gave (red ↑)
    size: Dp = 42.dp,
    badgeSize: Dp = 16.dp,
    showUploadIndicator: Boolean = false,
    onAvatarClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val hasPhoto = !photoUri.isNullOrBlank()

        // Main customer circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (hasPhoto) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (hasPhoto) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = customerName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val initial = customerName.trim().firstOrNull()?.uppercase() ?: "C"
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.42f).sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Image upload icon indicator overlay when specified
            if (showUploadIndicator) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Upload Photo",
                        tint = Color.White,
                        modifier = Modifier.size((size.value * 0.38f).dp)
                    )
                }
            }
        }

        // Small directional arrow badge attached near the image circle (bottom-end corner)
        if (isGot != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(if (isGot) CreditGreen else DebitRed)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGot) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = if (isGot) "Got" else "Gave",
                    tint = Color.White,
                    modifier = Modifier.size((badgeSize.value * 0.65f).dp)
                )
            }
        }
    }
}

/**
 * Thinner, modern and beautiful footer navigation bar.
 * Prevents text wrapping onto multiple lines and provides a balanced, compact aesthetic.
 */
@Composable
fun HisabBottomNav(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    strings: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bottom_nav_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Subtle top hairline separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            )

            // Sleek, compact Row for footer buttons (height: 54dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Home
                BottomNavItem(
                    selected = currentScreen == Screen.DASHBOARD,
                    onClick = { onNavigate(Screen.DASHBOARD) },
                    icon = if (currentScreen == Screen.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                    label = "Home",
                    testTag = "nav_home_tab",
                    modifier = Modifier.weight(1f)
                )

                // Tab 2: Khata (Parties) - Short 5-letter label so it's never cut off
                BottomNavItem(
                    selected = currentScreen == Screen.CUSTOMERS,
                    onClick = { onNavigate(Screen.CUSTOMERS) },
                    icon = if (currentScreen == Screen.CUSTOMERS) Icons.Filled.People else Icons.Outlined.People,
                    label = "Khata",
                    testTag = "nav_customers_tab",
                    modifier = Modifier.weight(1f)
                )

                // Tab 3: Sleek Center Add Entry Button
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(Screen.ADD_TRANSACTION) }
                        .testTag("nav_add_entry_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 3.dp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Entry",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add Entry",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }

                // Tab 4: Reports
                BottomNavItem(
                    selected = currentScreen == Screen.REPORTS,
                    onClick = { onNavigate(Screen.REPORTS) },
                    icon = if (currentScreen == Screen.REPORTS) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                    label = "Reports",
                    testTag = "nav_reports_tab",
                    modifier = Modifier.weight(1f)
                )

                // Tab 5: Settings
                BottomNavItem(
                    selected = currentScreen == Screen.SETTINGS,
                    onClick = { onNavigate(Screen.SETTINGS) },
                    icon = if (currentScreen == Screen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    label = "Settings",
                    testTag = "nav_settings_tab",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) activeColor else inactiveColor,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) activeColor else inactiveColor
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}
