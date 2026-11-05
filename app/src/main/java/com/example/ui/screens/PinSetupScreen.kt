package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.OperatingMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val operatingMode by viewModel.operatingMode.collectAsState()
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()
    val savedRecoveryQuestion by viewModel.recoveryQuestion.collectAsState()

    var isRecoveryMode by remember { mutableStateOf(false) }

    // PIN fields
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    // Recovery fields
    val defaultQuestions = listOf(
        "What was the name of your first school?",
        "What is your mother's maiden name?",
        "What was your childhood nickname?",
        "What was the make of your first car?",
        "In what city were your parents married?"
    )
    var selectedQuestion by remember { mutableStateOf(defaultQuestions[0]) }
    var recoveryAnswer by remember { mutableStateOf("") }
    var enteredRecoveryAnswer by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PureDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isRecoveryMode) "PIN Recovery" else if (isPinConfigured) "Change Security PIN" else "Set Security PIN",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureTextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("pin_setup_back_button")
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PureDarkElevated)
                    .border(1.dp, PurePrimaryBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecoveryMode) Icons.Default.HelpOutline else Icons.Default.LockReset,
                    contentDescription = null,
                    tint = PurePrimaryBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isRecoveryMode) {
                    "Emergency PIN Recovery"
                } else if (operatingMode == OperatingMode.FAMILY) {
                    "Guardian Control PIN"
                } else {
                    "Personal Self-Control PIN"
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isRecoveryMode) {
                    "Answer your recovery question to reset your PIN."
                } else if (operatingMode == OperatingMode.FAMILY) {
                    "This PIN will prevent children or unauthorized users from disabling adult content filtering or modifying settings."
                } else {
                    "This PIN locks deactivation to add friction and prevent compulsive pornography access on your device."
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = PureTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (!isRecoveryMode) {
                // PIN Setup Flow
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Choose 4–6 Digit PIN",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PureTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                            label = { Text("New PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurePrimaryBlue,
                                unfocusedBorderColor = PureBorder,
                                focusedTextColor = PureTextPrimary,
                                unfocusedTextColor = PureTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_pin_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                            label = { Text("Confirm PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurePrimaryBlue,
                                unfocusedBorderColor = PureBorder,
                                focusedTextColor = PureTextPrimary,
                                unfocusedTextColor = PureTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_pin_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        HorizontalDivider(color = PureBorder)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Emergency Recovery Question",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PureTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Question selector
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedQuestion,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurePrimaryBlue,
                                    unfocusedBorderColor = PureBorder,
                                    focusedTextColor = PureTextPrimary,
                                    unfocusedTextColor = PureTextPrimary
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(PureDarkElevated)
                            ) {
                                defaultQuestions.forEach { question ->
                                    DropdownMenuItem(
                                        text = { Text(question, color = PureTextPrimary) },
                                        onClick = {
                                            selectedQuestion = question
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = recoveryAnswer,
                            onValueChange = { recoveryAnswer = it },
                            label = { Text("Secret Recovery Answer") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurePrimaryBlue,
                                unfocusedBorderColor = PureBorder,
                                focusedTextColor = PureTextPrimary,
                                unfocusedTextColor = PureTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recovery_answer_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = PureErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (successMessage != null) {
                    Text(
                        text = successMessage ?: "",
                        color = PureSuccessGreen,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        errorMessage = null
                        if (newPin.length < 4 || newPin.length > 6) {
                            errorMessage = "PIN must be between 4 and 6 digits."
                            return@Button
                        }
                        if (newPin != confirmPin) {
                            errorMessage = "PIN confirmation does not match."
                            return@Button
                        }
                        if (recoveryAnswer.isBlank()) {
                            errorMessage = "Please enter an emergency recovery answer."
                            return@Button
                        }

                        isSaving = true
                        coroutineScope.launch {
                            val success = viewModel.setupPin(newPin, selectedQuestion, recoveryAnswer)
                            isSaving = false
                            if (success) {
                                successMessage = "PIN and recovery credentials secured with PBKDF2."
                                kotlinx.coroutines.delay(1000)
                                onNavigateBack()
                            } else {
                                errorMessage = "Failed to save PIN. Check format."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_pin_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurePrimaryBlue,
                        contentColor = PureTextPrimary
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PureTextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isPinConfigured) "Update PIN" else "Save & Activate PIN",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                if (isPinConfigured) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { isRecoveryMode = true },
                        modifier = Modifier.testTag("switch_to_recovery_button")
                    ) {
                        Text("Forgot current PIN? Use Recovery Answer", color = PureSecondaryBlue)
                    }
                }
            } else {
                // Recovery Flow
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Security Question",
                            style = MaterialTheme.typography.labelMedium.copy(color = PureTextSecondary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = savedRecoveryQuestion ?: "What was your childhood nickname?",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PureTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = enteredRecoveryAnswer,
                            onValueChange = { enteredRecoveryAnswer = it },
                            label = { Text("Your Recovery Answer") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurePrimaryBlue,
                                unfocusedBorderColor = PureBorder,
                                focusedTextColor = PureTextPrimary,
                                unfocusedTextColor = PureTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("entered_recovery_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = PureErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        errorMessage = null
                        if (enteredRecoveryAnswer.isBlank()) {
                            errorMessage = "Please enter your recovery answer."
                            return@Button
                        }
                        isSaving = true
                        coroutineScope.launch {
                            val matches = viewModel.verifyRecoveryAnswer(enteredRecoveryAnswer)
                            isSaving = false
                            if (matches) {
                                isRecoveryMode = false
                                successMessage = "Recovery verified. Please create your new PIN below."
                            } else {
                                errorMessage = "Incorrect recovery answer."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("verify_recovery_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurePrimaryBlue,
                        contentColor = PureTextPrimary
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PureTextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Verify Answer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { isRecoveryMode = false },
                    modifier = Modifier.testTag("cancel_recovery_button")
                ) {
                    Text("Back to PIN Entry", color = PureTextSecondary)
                }
            }
        }
    }
}
