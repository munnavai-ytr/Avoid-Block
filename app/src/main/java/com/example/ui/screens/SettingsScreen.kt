package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.OperatingMode
import com.example.core.model.UpstreamDnsServer
import com.example.core.security.LauncherIconManager
import com.example.deviceadmin.DeviceOwnerManager
import com.example.ui.components.PinVerificationDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPinSetup: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val operatingMode by viewModel.operatingMode.collectAsState()
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()
    val upstreamDnsIp by viewModel.upstreamDnsIp.collectAsState()
    val autoStartOnBoot by viewModel.autoStartOnBoot.collectAsState()
    val domainCount by viewModel.domainCount.collectAsState()
    val customDomains by viewModel.customDomains.collectAsState()
    val isUpdatingBlocklist by viewModel.isUpdatingBlocklist.collectAsState()
    val updateResultMessage by viewModel.updateResultMessage.collectAsState()

    val isDeviceOwner by viewModel.isDeviceOwner.collectAsState()
    val isDeviceAdmin by viewModel.isDeviceAdmin.collectAsState()
    val isUninstallBlocked by viewModel.isUninstallBlocked.collectAsState()

    var showPinDialogForModeSwitch by remember { mutableStateOf(false) }
    var pendingModeSwitch by remember { mutableStateOf<OperatingMode?>(null) }

    var showAddDomainDialog by remember { mutableStateOf(false) }
    var newCustomDomainText by remember { mutableStateOf("") }
    var customDomainError by remember { mutableStateOf<String?>(null) }

    var domainToDelete by remember { mutableStateOf<String?>(null) }
    var showPinDialogForDomainDelete by remember { mutableStateOf(false) }

    var showAdbInstructionsDialog by remember { mutableStateOf(false) }

    val actualComponentHidden by viewModel.actualComponentHidden.collectAsState()
    var showHideIconConfirmationDialog by remember { mutableStateOf(false) }
    var showPinDialogForHideIcon by remember { mutableStateOf(false) }
    var showPinDialogForRestoreIcon by remember { mutableStateOf(false) }
    var showRecoveryGuideDialog by remember { mutableStateOf(false) }
    var launcherActionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshDeviceOwnerStatus()
        viewModel.refreshLauncherIconStatus()
    }

    Scaffold(
        containerColor = PureDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Control",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureTextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Section 1: Operating Mode
            Text(
                text = "OPERATING MODE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureSecondaryBlue,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Personal Mode Radio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (operatingMode != OperatingMode.PERSONAL) {
                                    if (isPinConfigured) {
                                        pendingModeSwitch = OperatingMode.PERSONAL
                                        showPinDialogForModeSwitch = true
                                    } else {
                                        viewModel.setOperatingMode(OperatingMode.PERSONAL)
                                    }
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = operatingMode == OperatingMode.PERSONAL,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = PurePrimaryBlue)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Personal Self-Control Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PureTextPrimary
                                )
                            )
                            Text(
                                text = "PIN protected deactivation for individual self-control.",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary)
                            )
                        }
                    }

                    HorizontalDivider(color = PureBorder, modifier = Modifier.padding(vertical = 4.dp))

                    // Family Mode Radio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (operatingMode != OperatingMode.FAMILY) {
                                    if (isPinConfigured) {
                                        pendingModeSwitch = OperatingMode.FAMILY
                                        showPinDialogForModeSwitch = true
                                    } else {
                                        viewModel.setOperatingMode(OperatingMode.FAMILY)
                                    }
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = operatingMode == OperatingMode.FAMILY,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = PureWarningAmber)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Family & Guardian Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PureTextPrimary
                                )
                            )
                            Text(
                                text = "Parental control with Device Owner uninstall lock support.",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Blocklist System Management
            Text(
                text = "DOMAIN BLOCKLIST ENGINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureSecondaryBlue,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Active Adult Domains",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureTextPrimary
                                )
                            )
                            Text(
                                text = "$domainCount verified adult domains active",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary)
                            )
                        }

                        Button(
                            onClick = { viewModel.updateBlocklist() },
                            enabled = !isUpdatingBlocklist,
                            colors = ButtonDefaults.buttonColors(containerColor = PureDarkElevated),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder),
                            modifier = Modifier.testTag("update_blocklist_button")
                        ) {
                            if (isUpdatingBlocklist) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = PurePrimaryBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = PurePrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Update", color = PureTextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (updateResultMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = updateResultMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (updateResultMessage!!.contains("failed")) PureWarningAmber else PureSuccessGreen
                            )
                        )
                    }

                    HorizontalDivider(color = PureBorder, modifier = Modifier.padding(vertical = 12.dp))

                    // Custom Domains Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Custom Blocked Domains (${customDomains.size})",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PureTextPrimary
                            )
                        )
                        TextButton(
                            onClick = {
                                newCustomDomainText = ""
                                customDomainError = null
                                showAddDomainDialog = true
                            },
                            modifier = Modifier.testTag("add_custom_domain_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = PurePrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Domain", color = PurePrimaryBlue, fontSize = 12.sp)
                        }
                    }

                    if (customDomains.isEmpty()) {
                        Text(
                            text = "No custom domains added. Verified adult domains are filtered automatically.",
                            style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary)
                        )
                    } else {
                        customDomains.forEach { domainEntity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = domainEntity.domain,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = PureTextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        domainToDelete = domainEntity.domain
                                        if (isPinConfigured) {
                                            showPinDialogForDomainDelete = true
                                        } else {
                                            coroutineScope.launch {
                                                viewModel.removeCustomDomain(domainEntity.domain)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove",
                                        tint = PureErrorRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Upstream DNS Resolver
            Text(
                text = "UPSTREAM DNS RESOLVER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureSecondaryBlue,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    UpstreamDnsServer.entries.forEach { server ->
                        val isSelected = server.primaryIp == upstreamDnsIp
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.setUpstreamDns(server.primaryIp) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = PurePrimaryBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = server.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = PureTextPrimary
                                    )
                                )
                                Text(
                                    text = "IP: ${server.primaryIp}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = PureTextSecondary)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 4: Android Enterprise & Family Device Owner
            Text(
                text = "MANAGED DEVICE & ANTI-UNINSTALL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureSecondaryBlue,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Device Owner Status",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureTextPrimary
                                )
                            )
                            Text(
                                text = if (isDeviceOwner) "Provisioned as Android Device Owner" else "Not Provisioned (Standard Mode)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDeviceOwner) PureSuccessGreen else PureTextSecondary
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDeviceOwner) PureSuccessGreen.copy(alpha = 0.2f) else PureDarkElevated
                        ) {
                            Text(
                                text = if (isDeviceOwner) "ACTIVE" else "STANDARD",
                                color = if (isDeviceOwner) PureSuccessGreen else PureTextSecondary,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Uninstall Lock Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Block Application Uninstall",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureTextPrimary
                                )
                            )
                            Text(
                                text = if (isDeviceOwner) "Uses official DevicePolicyManager.setUninstallBlocked" else "Requires Device Owner provisioning",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary)
                            )
                        }

                        Switch(
                            checked = isUninstallBlocked,
                            enabled = isDeviceOwner,
                            onCheckedChange = { checked ->
                                viewModel.setUninstallBlocked(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureTextPrimary,
                                checkedTrackColor = PurePrimaryBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ADB Instructions Button
                    OutlinedButton(
                        onClick = { showAdbInstructionsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_owner_guide_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureSecondaryBlue),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Device Owner ADB Setup Guide")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 5: Stealth Launcher Visibility (Hide App Icon)
            Text(
                text = "STEALTH LAUNCHER VISIBILITY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureSecondaryBlue,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stealth_launcher_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Hide App Icon Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide App Icon from Launcher",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Removes PURELOCK from home screen and app drawer. Protection and VPN filtering continue running.",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary)
                            )
                        }

                        Switch(
                            checked = actualComponentHidden,
                            onCheckedChange = { shouldHide ->
                                if (!isPinConfigured) {
                                    Toast.makeText(context, "Please set a security PIN first", Toast.LENGTH_SHORT).show()
                                    onNavigateToPinSetup()
                                } else if (shouldHide) {
                                    showHideIconConfirmationDialog = true
                                } else {
                                    showPinDialogForRestoreIcon = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureTextPrimary,
                                checkedTrackColor = PureWarningAmber,
                                uncheckedThumbColor = PureTextSecondary,
                                uncheckedTrackColor = PureDarkElevated
                            ),
                            modifier = Modifier.testTag("hide_app_icon_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = PureBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Actual Visibility Status Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Actual Launcher State",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureTextPrimary
                                )
                            )
                            Text(
                                text = if (actualComponentHidden) "Disabled via Android PackageManager" else "Active on Home Screen",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (actualComponentHidden) PureWarningAmber.copy(alpha = 0.15f) else PureSuccessGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (actualComponentHidden) PureWarningAmber.copy(alpha = 0.4f) else PureSuccessGreen.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (actualComponentHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = if (actualComponentHidden) PureWarningAmber else PureSuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (actualComponentHidden) "HIDDEN" else "VISIBLE",
                                    color = if (actualComponentHidden) PureWarningAmber else PureSuccessGreen,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    // Restore Button if hidden
                    if (actualComponentHidden) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (isPinConfigured) {
                                    showPinDialogForRestoreIcon = true
                                } else {
                                    viewModel.setAppIconHidden(false)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_app_icon_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurePrimaryBlue)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Restore App Icon", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recovery & Compatibility Guide Button
                    OutlinedButton(
                        onClick = { showRecoveryGuideDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recovery_guide_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureSecondaryBlue),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("How to Re-open PURELOCK When Hidden")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 6: Security & Preferences
            Text(
                text = "SECURITY & REBOOT RESTORATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureSecondaryBlue,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Auto-start on boot switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Restore Protection on Device Boot",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureTextPrimary
                                )
                            )
                            Text(
                                text = "Restarts VPN filtering automatically when phone starts",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary)
                            )
                        }

                        Switch(
                            checked = autoStartOnBoot,
                            onCheckedChange = { viewModel.setAutoStartOnBoot(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureTextPrimary,
                                checkedTrackColor = PurePrimaryBlue
                            )
                        )
                    }

                    HorizontalDivider(color = PureBorder, modifier = Modifier.padding(vertical = 12.dp))

                    // Change PIN action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPinSetup() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Password,
                                contentDescription = null,
                                tint = PurePrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isPinConfigured) "Change Security PIN" else "Set Security PIN",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = PureTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = PureTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 6: Privacy Statement
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = PureSuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Privacy & Zero-Tracking Guarantee",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureTextPrimary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PURELOCK processes all DNS filtering directly on this device. No browsing history, queries, or logs are uploaded to any external server. Upstream queries are encrypted where supported and filtered locally.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PureTextSecondary,
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialog for Adding Custom Domain
    if (showAddDomainDialog) {
        AlertDialog(
            onDismissRequest = { showAddDomainDialog = false },
            containerColor = PureDarkSurface,
            title = {
                Text(
                    text = "Add Custom Blocked Domain",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PureTextPrimary
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a domain (e.g. adultsite.com). Subdomains will automatically be blocked.",
                        style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCustomDomainText,
                        onValueChange = { newCustomDomainText = it },
                        label = { Text("Domain name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurePrimaryBlue,
                            unfocusedBorderColor = PureBorder,
                            focusedTextColor = PureTextPrimary,
                            unfocusedTextColor = PureTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("custom_domain_text_input")
                    )
                    if (customDomainError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = customDomainError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = PureErrorRed)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = newCustomDomainText.trim()
                        if (input.isBlank() || !input.contains('.')) {
                            customDomainError = "Please enter a valid domain (e.g., example.com)"
                            return@Button
                        }
                        coroutineScope.launch {
                            val success = viewModel.addCustomDomain(input)
                            if (success) {
                                showAddDomainDialog = false
                            } else {
                                customDomainError = "Invalid domain name"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurePrimaryBlue)
                ) {
                    Text("Add", color = PureTextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDomainDialog = false }) {
                    Text("Cancel", color = PureTextSecondary)
                }
            }
        )
    }

    // PIN Dialog for switching Operating Mode
    if (showPinDialogForModeSwitch && pendingModeSwitch != null) {
        PinVerificationDialog(
            title = "Confirm Mode Change",
            subtitle = "Enter PIN to switch to ${pendingModeSwitch?.displayName}",
            onDismiss = {
                showPinDialogForModeSwitch = false
                pendingModeSwitch = null
            },
            onVerify = { pin -> viewModel.verifyPin(pin) },
            onSuccess = {
                pendingModeSwitch?.let { viewModel.setOperatingMode(it) }
                showPinDialogForModeSwitch = false
                pendingModeSwitch = null
            }
        )
    }

    // PIN Dialog for deleting a custom domain
    if (showPinDialogForDomainDelete && domainToDelete != null) {
        PinVerificationDialog(
            title = "Confirm Removal",
            subtitle = "Enter PIN to unblock '${domainToDelete}'",
            onDismiss = {
                showPinDialogForDomainDelete = false
                domainToDelete = null
            },
            onVerify = { pin -> viewModel.verifyPin(pin) },
            onSuccess = {
                domainToDelete?.let { domain ->
                    coroutineScope.launch {
                        viewModel.removeCustomDomain(domain)
                    }
                }
                showPinDialogForDomainDelete = false
                domainToDelete = null
            }
        )
    }

    // ADB Device Owner Instructions Dialog
    if (showAdbInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showAdbInstructionsDialog = false },
            containerColor = PureDarkSurface,
            title = {
                Text(
                    text = "Device Owner Provisioning",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PureTextPrimary
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Android Enterprise Device Owner privileges allow parents and guardians to prevent uninstallation via official DevicePolicyManager APIs.",
                        style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary, lineHeight = 18.sp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Official ADB Command:",
                        style = MaterialTheme.typography.labelSmall.copy(color = PurePrimaryBlue, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PureDarkElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                    ) {
                        Text(
                            text = DeviceOwnerManager.ADB_PROVISION_COMMAND,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = PureTextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Note: Android requires that no other accounts are configured on the device prior to provisioning, or that this is provisioned during factory reset.",
                        style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary, fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", DeviceOwnerManager.ADB_PROVISION_COMMAND))
                        Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                        showAdbInstructionsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurePrimaryBlue)
                ) {
                    Text("Copy Command", color = PureTextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdbInstructionsDialog = false }) {
                    Text("Close", color = PureTextSecondary)
                }
            }
        )
    }

    // Confirmation Dialog before Hiding App Icon
    if (showHideIconConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showHideIconConfirmationDialog = false },
            containerColor = PureDarkSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = PureWarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Hide App Icon?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureTextPrimary
                        )
                    )
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "This will remove the PURELOCK icon from your home screen and app drawer using official Android PackageManager component settings.",
                        style = MaterialTheme.typography.bodySmall.copy(color = PureTextPrimary, lineHeight = 18.sp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PureDarkElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Important Operating Notes:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PureSecondaryBlue
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Protection remains ACTIVE. The VPN DNS filtering engine continues running uninterrupted.\n" +
                                        "• PURELOCK will NOT terminate or restart when hiding the icon.\n" +
                                        "• Android Transparency: PURELOCK is not invisible to the operating system; it remains visible in Settings > Apps and VPN settings.\n" +
                                        "• Realme UI & OEM note: Some custom launchers take a few seconds or a screen lock/unlock to refresh cached app icons.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PureTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "How to Re-open When Hidden:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PurePrimaryBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Tap the persistent VPN notification in the notification bar\n" +
                                "2. Type purelock://open in any browser\n" +
                                "3. Open via Android Settings > Apps > PURELOCK > Open\n" +
                                "4. Use ADB command on a computer",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PureTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your PURELOCK PIN is required to confirm.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PureWarningAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHideIconConfirmationDialog = false
                        showPinDialogForHideIcon = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PureWarningAmber),
                    modifier = Modifier.testTag("confirm_proceed_hide_button")
                ) {
                    Text("Proceed with PIN", color = PureDarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showHideIconConfirmationDialog = false },
                    modifier = Modifier.testTag("cancel_hide_dialog_button")
                ) {
                    Text("Cancel", color = PureTextSecondary)
                }
            }
        )
    }

    // PIN Verification Dialog to Hide Icon
    if (showPinDialogForHideIcon) {
        PinVerificationDialog(
            title = "Confirm Hide Icon",
            subtitle = "Enter PIN to hide the launcher icon",
            onDismiss = { showPinDialogForHideIcon = false },
            onVerify = { pin -> viewModel.verifyPin(pin) },
            onSuccess = {
                showPinDialogForHideIcon = false
                val result = viewModel.setAppIconHidden(true)
                if (result.isSuccess) {
                    Toast.makeText(context, "PURELOCK icon hidden from launcher. Protection remains active.", Toast.LENGTH_LONG).show()
                } else {
                    launcherActionError = result.exceptionOrNull()?.message ?: "Failed to hide icon."
                }
            }
        )
    }

    // PIN Verification Dialog to Restore Icon
    if (showPinDialogForRestoreIcon) {
        PinVerificationDialog(
            title = "Restore App Icon",
            subtitle = "Enter PIN to restore PURELOCK to the launcher",
            onDismiss = { showPinDialogForRestoreIcon = false },
            onVerify = { pin -> viewModel.verifyPin(pin) },
            onSuccess = {
                showPinDialogForRestoreIcon = false
                val result = viewModel.setAppIconHidden(false)
                if (result.isSuccess) {
                    Toast.makeText(context, "PURELOCK icon restored to launcher.", Toast.LENGTH_LONG).show()
                } else {
                    launcherActionError = result.exceptionOrNull()?.message ?: "Failed to restore icon."
                }
            }
        )
    }

    // Recovery & Access Guide Dialog
    if (showRecoveryGuideDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryGuideDialog = false },
            containerColor = PureDarkSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = PurePrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Access & Recovery Guide",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureTextPrimary
                        )
                    )
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "When the PURELOCK icon is hidden from your home screen, you can re-open the application through any of these 4 legitimate methods:",
                        style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary, lineHeight = 18.sp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Method 1: Notification
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PureDarkElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Method 1: Persistent VPN Notification",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PureSuccessGreen
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "While protection is active, PURELOCK keeps an ongoing notification. Simply tap the notification to immediately open the app.",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary, fontSize = 11.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Method 2: Browser Deep Link
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PureDarkElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Method 2: Browser Recovery URL",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PureSecondaryBlue
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Type this link into any web browser (Chrome, Samsung Internet, etc.):",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = LauncherIconManager.RECOVERY_DEEP_LINK,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PurePrimaryBlue,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Method 3: System Settings
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PureDarkElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Method 3: Android Settings > Apps",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PurePrimaryBlue
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Go to phone Settings > Apps > PURELOCK and tap 'Open'. Android preserves this button even when the launcher icon is hidden.",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureTextSecondary, fontSize = 11.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Method 4: ADB Command
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PureDarkElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Method 4: Terminal / ADB Command",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PureWarningAmber
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = LauncherIconManager.ADB_LAUNCH_COMMAND,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = PureTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Compatibility Notes (Android 8–15 & Realme UI / ColorOS):\n" +
                                "• Some custom launchers cache home screen icons. If the icon remains visible for a few moments, lock and unlock your screen or restart the launcher app.\n" +
                                "• Hiding the launcher icon uses official PackageManager component-enabled settings with DONT_KILL_APP, ensuring the VPN service is never interrupted.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PureTextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Recovery URL", LauncherIconManager.RECOVERY_DEEP_LINK))
                        Toast.makeText(context, "Recovery URL copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurePrimaryBlue)
                ) {
                    Text("Copy URL", color = PureTextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryGuideDialog = false }) {
                    Text("Close", color = PureTextSecondary)
                }
            }
        )
    }

    // Error Dialog
    if (launcherActionError != null) {
        AlertDialog(
            onDismissRequest = { launcherActionError = null },
            containerColor = PureDarkSurface,
            title = { Text("Launcher Setting Error", color = PureErrorRed, fontWeight = FontWeight.Bold) },
            text = { Text(launcherActionError ?: "", color = PureTextSecondary) },
            confirmButton = {
                TextButton(onClick = { launcherActionError = null }) {
                    Text("OK", color = PurePrimaryBlue)
                }
            }
        )
    }
}
