package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.model.PinVerificationResult
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PinVerificationDialog(
    title: String = "Enter PIN",
    subtitle: String = "Verification is required to proceed",
    onDismiss: () -> Unit,
    onVerify: suspend (String) -> PinVerificationResult,
    onSuccess: () -> Unit,
    onForgotPassword: (() -> Unit)? = null
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var lockoutSeconds by remember { mutableStateOf<Long?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Countdown timer for lockout
    LaunchedEffect(lockoutSeconds) {
        val seconds = lockoutSeconds ?: return@LaunchedEffect
        if (seconds > 0) {
            var current = seconds
            while (current > 0) {
                kotlinx.coroutines.delay(1000)
                current -= 1
                lockoutSeconds = current
            }
            lockoutSeconds = null
            errorMessage = null
        }
    }

    Dialog(onDismissRequest = { if (!isVerifying) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("pin_verification_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PureDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, PureBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PureDarkElevated)
                        .border(1.dp, PureBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = PurePrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PureTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = PureTextSecondary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN Input Field
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("4–6 Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    enabled = lockoutSeconds == null && !isVerifying,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurePrimaryBlue,
                        unfocusedBorderColor = PureBorder,
                        focusedTextColor = PureTextPrimary,
                        unfocusedTextColor = PureTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input_field")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = PureErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                if (lockoutSeconds != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Too many failed attempts. Try again in ${lockoutSeconds}s",
                        color = PureWarningAmber,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_pin_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureTextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (pin.length < 4) {
                                errorMessage = "PIN must be at least 4 digits"
                                return@Button
                            }
                            isVerifying = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = onVerify(pin)
                                isVerifying = false
                                when (result) {
                                    is PinVerificationResult.Success -> {
                                        onSuccess()
                                        onDismiss()
                                    }
                                    is PinVerificationResult.Incorrect -> {
                                        if (result.isLockedOut) {
                                            lockoutSeconds = result.lockoutSeconds
                                            errorMessage = "Account locked for ${result.lockoutSeconds}s"
                                        } else {
                                            errorMessage = "Incorrect PIN. ${result.remainingAttempts} attempts before delay."
                                        }
                                    }
                                    is PinVerificationResult.LockedOut -> {
                                        lockoutSeconds = result.remainingSeconds
                                        errorMessage = "Too many attempts. Wait ${result.remainingSeconds}s."
                                    }
                                }
                            }
                        },
                        enabled = pin.length >= 4 && lockoutSeconds == null && !isVerifying,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("verify_pin_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurePrimaryBlue,
                            contentColor = PureTextPrimary
                        )
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PureTextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Confirm")
                        }
                    }
                }

                if (onForgotPassword != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = onForgotPassword,
                        modifier = Modifier.testTag("forgot_pin_button")
                    ) {
                        Text(
                            text = "Forgot PIN? Use Recovery Answer",
                            style = MaterialTheme.typography.bodySmall.copy(color = PureSecondaryBlue)
                        )
                    }
                }
            }
        }
    }
}
