package com.example.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.OperatingMode
import com.example.ui.components.PinVerificationDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToBlockedPage: () -> Unit,
    onNavigateToPinSetup: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isVpnRunning by viewModel.isVpnRunning.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val operatingMode by viewModel.operatingMode.collectAsState()
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()
    val totalBlockedCount by viewModel.totalBlockedCount.collectAsState()
    val sessionBlockedCount by viewModel.sessionBlockedCount.collectAsState()
    val sessionTotalQueries by viewModel.sessionTotalQueries.collectAsState()
    val domainCount by viewModel.domainCount.collectAsState()
    val lastBlockedDomain by viewModel.lastBlockedDomain.collectAsState()

    var showPinDialogToStop by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Official Android VPN permission consent launcher
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startProtection(context)
        } else {
            snackbarMessage = "VPN permission is required to filter adult domains locally."
        }
    }

    // Pulsing animation for active shield aura
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isVpnRunning) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Scaffold(
        containerColor = PureDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PureDarkElevated)
                                .border(1.dp, PurePrimaryBlue, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "PURELOCK Logo",
                                tint = PurePrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PURELOCK",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = PureTextPrimary
                                )
                            )
                            Text(
                                text = "Stay Focused. Stay Protected.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PureTextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Blocked History Action
                    IconButton(
                        onClick = onNavigateToBlockedPage,
                        modifier = Modifier.testTag("nav_blocked_page_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (sessionBlockedCount > 0) {
                                    Badge(
                                        containerColor = PureErrorRed,
                                        contentColor = PureTextPrimary
                                    ) {
                                        Text(sessionBlockedCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Blocked Page & Logs",
                                tint = PureTextSecondary
                            )
                        }
                    }

                    // Settings Action
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = PureTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureDarkBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Indicator Badge
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, PureBorder, RoundedCornerShape(20.dp))
                    .testTag("mode_badge"),
                color = PureDarkSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (operatingMode == OperatingMode.FAMILY) Icons.Default.FamilyRestroom else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (operatingMode == OperatingMode.FAMILY) PureWarningAmber else PurePrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = operatingMode.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureTextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Large Protection Status Indicator
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow aura
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (isVpnRunning) {
                                    listOf(
                                        PureSuccessGreen.copy(alpha = 0.25f),
                                        PureSuccessGreen.copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        PureBorder.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                }
                            )
                        )
                )

                // Middle Ring
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .background(PureDarkSurface)
                        .border(
                            width = 3.dp,
                            brush = if (isVpnRunning) {
                                Brush.sweepGradient(listOf(PureSuccessGreen, PureSecondaryBlue, PureSuccessGreen))
                            } else {
                                Brush.linearGradient(listOf(PureBorder, PureDarkElevated))
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isVpnRunning) Icons.Default.Security else Icons.Default.Shield,
                            contentDescription = if (isVpnRunning) "Protection Active" else "Protection Inactive",
                            tint = if (isVpnRunning) PureSuccessGreen else PureTextSecondary,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isVpnRunning) "PROTECTED" else if (isConnecting) "STARTING..." else "UNPROTECTED",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = if (isVpnRunning) PureSuccessGreen else if (isConnecting) PureWarningAmber else PureTextSecondary
                            )
                        )
                        Text(
                            text = if (isVpnRunning) "DNS Filtering Active" else "Filtering Inactive",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = PureTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Start/Stop Action Button
            Button(
                onClick = {
                    if (isVpnRunning) {
                        // Requires PIN verification before turning off
                        if (isPinConfigured) {
                            showPinDialogToStop = true
                        } else {
                            // If no PIN has been set yet, prompt user to set one
                            onNavigateToPinSetup()
                        }
                    } else {
                        // Check if PIN is set first
                        if (!isPinConfigured) {
                            onNavigateToPinSetup()
                        } else {
                            // Request VPN consent or start service
                            val intent = VpnService.prepare(context)
                            if (intent != null) {
                                vpnPrepareLauncher.launch(intent)
                            } else {
                                viewModel.startProtection(context)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("toggle_protection_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVpnRunning) PureDarkElevated else PurePrimaryBlue,
                    contentColor = if (isVpnRunning) PureErrorRed else PureTextPrimary
                ),
                border = if (isVpnRunning) androidx.compose.foundation.BorderStroke(1.dp, PureErrorRed.copy(alpha = 0.6f)) else null
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isVpnRunning) Icons.Default.PowerSettingsNew else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isVpnRunning) "Disable Protection" else "Activate Protection",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Live Metrics Dashboard Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Blocked DNS Counter Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToBlockedPage() }
                        .testTag("blocked_counter_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "BLOCKED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PureErrorRed,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = PureErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${totalBlockedCount + sessionBlockedCount}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureTextPrimary
                            )
                        )
                        Text(
                            text = "Adult requests stopped",
                            style = MaterialTheme.typography.labelSmall.copy(color = PureTextSecondary)
                        )
                    }
                }

                // Filtered Queries Counter Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("queries_counter_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "FILTERED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PureSecondaryBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = PureSecondaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$sessionTotalQueries",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureTextPrimary
                            )
                        )
                        Text(
                            text = "Session DNS queries",
                            style = MaterialTheme.typography.labelSmall.copy(color = PureTextSecondary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Blocklist Capacity Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blocklist_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PureDarkElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = PurePrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Active Adult Blocklist",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureTextPrimary
                                )
                            )
                            Text(
                                text = "$domainCount verified domains loaded locally",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = PureSuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Live Alert if domain was just blocked
            AnimatedVisibility(visible = lastBlockedDomain != null && isVpnRunning) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBlockedPage() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureErrorRed.copy(alpha = 0.12f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureErrorRed.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Blocked Alert",
                                tint = PureErrorRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Recently Intercepted",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = PureErrorRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = lastBlockedDomain ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = PureTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            Text(
                                text = "View Logs",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PureSecondaryBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Transparent OS boundaries explanation
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureDarkElevated.copy(alpha = 0.6f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PureTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (operatingMode == OperatingMode.PERSONAL) {
                            "Personal Mode Note: Standard Android apps cannot prevent system Settings force-stop. For tamper-proof uninstall locks, use Family Mode with Device Owner."
                        } else {
                            "Family Mode Note: For full uninstall protection, authorize PURELOCK as Android Device Owner via official DevicePolicyManager APIs in Settings."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PureTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }

    // PIN Verification Dialog before disabling protection
    if (showPinDialogToStop) {
        PinVerificationDialog(
            title = "Enter PIN to Deactivate",
            subtitle = "Verification prevents accidental or compulsive deactivation",
            onDismiss = { showPinDialogToStop = false },
            onVerify = { pin -> viewModel.verifyPin(pin) },
            onSuccess = {
                coroutineScope.launch {
                    viewModel.stopProtection()
                    showPinDialogToStop = false
                }
            },
            onForgotPassword = {
                showPinDialogToStop = false
                onNavigateToPinSetup()
            }
        )
    }

    // Snackbar for error/permission notifications
    if (snackbarMessage != null) {
        AlertDialog(
            onDismissRequest = { snackbarMessage = null },
            confirmButton = {
                TextButton(onClick = { snackbarMessage = null }) {
                    Text("OK", color = PurePrimaryBlue)
                }
            },
            title = { Text("Notice", color = PureTextPrimary) },
            text = { Text(snackbarMessage ?: "", color = PureTextSecondary) },
            containerColor = PureDarkSurface
        )
    }
}
