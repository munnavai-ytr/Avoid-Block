package com.example.core.security

import android.content.Context
import android.util.Base64
import com.example.core.model.PinVerificationResult
import com.example.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinManager(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val random = SecureRandom()

    val isPinConfigured: Flow<Boolean> = preferencesRepository.isPinConfigured
    val recoveryQuestion: Flow<String?> = preferencesRepository.recoveryQuestion

    suspend fun setPin(pin: String, question: String, answer: String): Boolean {
        if (!isValidPinFormat(pin)) return false
        if (question.isBlank() || answer.isBlank()) return false

        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        val saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)

        val pinHash = hashWithSalt(pin, saltBytes)
        val answerHash = hashWithSalt(answer.trim().lowercase(), saltBytes)

        preferencesRepository.savePin(pinHash, saltBase64, question.trim(), answerHash)
        return true
    }

    suspend fun verifyPin(enteredPin: String): PinVerificationResult {
        val (failedAttempts, lockoutUntil) = preferencesRepository.getThrottlingData()
        val now = System.currentTimeMillis()

        if (lockoutUntil > now) {
            val remainingSec = (lockoutUntil - now) / 1000
            return PinVerificationResult.LockedOut(remainingSec.coerceAtLeast(1))
        }

        val (savedHash, savedSaltBase64) = preferencesRepository.getPinSecurityData()
        if (savedHash == null || savedSaltBase64 == null) {
            // No PIN is set
            return PinVerificationResult.Success
        }

        val saltBytes = Base64.decode(savedSaltBase64, Base64.NO_WRAP)
        val enteredHash = hashWithSalt(enteredPin, saltBytes)

        return if (slowEquals(savedHash.toByteArray(Charsets.UTF_8), enteredHash.toByteArray(Charsets.UTF_8))) {
            preferencesRepository.resetThrottling()
            PinVerificationResult.Success
        } else {
            val newFailed = failedAttempts + 1
            val lockoutDurationSec = calculateLockoutDurationSeconds(newFailed)
            val newLockoutUntil = if (lockoutDurationSec > 0) now + (lockoutDurationSec * 1000) else 0L

            preferencesRepository.updateThrottling(newFailed, newLockoutUntil)

            val remainingBeforeLockout = (MAX_ATTEMPTS_BEFORE_THROTTLE - (newFailed % MAX_ATTEMPTS_BEFORE_THROTTLE)).coerceAtLeast(0)
            PinVerificationResult.Incorrect(
                remainingAttempts = remainingBeforeLockout,
                isLockedOut = lockoutDurationSec > 0,
                lockoutSeconds = lockoutDurationSec
            )
        }
    }

    suspend fun verifyRecoveryAnswer(enteredAnswer: String): Boolean {
        val (_, savedSaltBase64) = preferencesRepository.getPinSecurityData()
        val savedAnswerHash = preferencesRepository.getRecoveryAnswerHash() ?: return false
        if (savedSaltBase64 == null) return false

        val saltBytes = Base64.decode(savedSaltBase64, Base64.NO_WRAP)
        val enteredHash = hashWithSalt(enteredAnswer.trim().lowercase(), saltBytes)

        val isMatch = slowEquals(savedAnswerHash.toByteArray(Charsets.UTF_8), enteredHash.toByteArray(Charsets.UTF_8))
        if (isMatch) {
            preferencesRepository.resetThrottling()
        }
        return isMatch
    }

    fun isValidPinFormat(pin: String): Boolean {
        return pin.length in 4..6 && pin.all { it.isDigit() }
    }

    private fun hashWithSalt(text: String, salt: ByteArray): String {
        val spec = PBEKeySpec(text.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private fun slowEquals(a: ByteArray, b: ByteArray): Boolean {
        var diff = a.size xor b.size
        for (i in 0 until a.size.coerceAtMost(b.size)) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
        }
        return diff == 0
    }

    private fun calculateLockoutDurationSeconds(attempts: Int): Long {
        return when {
            attempts < MAX_ATTEMPTS_BEFORE_THROTTLE -> 0L
            attempts == 3 -> 30L
            attempts == 4 -> 60L
            else -> 300L // 5 minutes
        }
    }

    companion object {
        private const val PBKDF2_ITERATIONS = 10_000
        private const val KEY_LENGTH = 256
        const val MAX_ATTEMPTS_BEFORE_THROTTLE = 3
    }
}
