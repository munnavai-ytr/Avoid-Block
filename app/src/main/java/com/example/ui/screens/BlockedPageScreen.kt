package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PinVerificationDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedPageScreen(
    viewModel: MainViewModel,
    onReturnToSafety: () -> Unit
) {
    val recentBlockedEvents by viewModel.recentBlockedEvents.collectAsState()
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()

    var showPinDialogForClear by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss • MMM dd", Locale.getDefault()) }

    Scaffold(
        containerColor = PureDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Protection Intercept",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureTextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onReturnToSafety,
                        modifier = Modifier.testTag("blocked_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Return to Safety",
                            tint = PureTextSecondary
                        )
                    }
                },
                actions = {
                    if (recentBlockedEvents.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (isPinConfigured) {
                                    showPinDialogForClear = true
                                } else {
                                    viewModel.clearBlockedHistory()
                                }
                            },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = PureTextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureDarkBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Hero Defense Shield
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(PureErrorRed.copy(alpha = 0.15f))
                        .border(2.dp, PureErrorRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Website Blocked",
                        tint = PureErrorRed,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Website Blocked",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PureTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "PURELOCK Protection is Active",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = PureSuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Access to adult content and pornography domains is blocked at the DNS packet layer on this device.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = PureTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Return to Safety Action
                Button(
                    onClick = onReturnToSafety,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("return_to_safety_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurePrimaryBlue,
                        contentColor = PureTextPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Return to Safety",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Intercepted Logs Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Intercepted Queries",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureTextPrimary
                        )
                    )
                    Text(
                        text = "${recentBlockedEvents.size} events",
                        style = MaterialTheme.typography.labelSmall.copy(color = PureTextSecondary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (recentBlockedEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = PureSuccessGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Zero Adult Queries Intercepted",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureTextPrimary
                                )
                            )
                            Text(
                                text = "When any browser or app requests a known adult domain, it will appear here in real time.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PureTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            } else {
                items(recentBlockedEvents, key = { it.id }) { event ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PureErrorRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Blocked",
                                    tint = PureErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.domain,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PureTextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${dateFormat.format(Date(event.timestamp))} • Type ${event.queryType} • Sinkhole 0.0.0.0",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = PureTextSecondary,
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PureDarkElevated
                            ) {
                                Text(
                                    text = "BLOCKED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PureErrorRed,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showPinDialogForClear) {
        PinVerificationDialog(
            title = "Confirm Clear Logs",
            subtitle = "Enter PIN to erase blocked event history",
            onDismiss = { showPinDialogForClear = false },
            onVerify = { pin -> viewModel.verifyPin(pin) },
            onSuccess = {
                viewModel.clearBlockedHistory()
                showPinDialogForClear = false
            }
        )
    }
}
