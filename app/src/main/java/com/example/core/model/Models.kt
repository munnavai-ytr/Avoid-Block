package com.example.core.model

enum class OperatingMode(val displayName: String, val description: String) {
    PERSONAL(
        displayName = "Personal Mode",
        description = "Self-control protection with your personal PIN. Filters adult domains system-wide."
    ),
    FAMILY(
        displayName = "Family Mode",
        description = "Parental protection with a guardian PIN, with optional Android Device Owner uninstall restrictions."
    )
}

enum class ProtectionStatus {
    ACTIVE,
    INACTIVE,
    CONNECTING
}

sealed class PinVerificationResult {
    data object Success : PinVerificationResult()
    data class Incorrect(val remainingAttempts: Int, val isLockedOut: Boolean, val lockoutSeconds: Long = 0) : PinVerificationResult()
    data class LockedOut(val remainingSeconds: Long) : PinVerificationResult()
}

data class BlockedDnsEvent(
    val id: Long = 0,
    val domain: String,
    val timestamp: Long,
    val queryType: String,
    val clientInfo: String,
    val mode: OperatingMode
)

enum class UpstreamDnsServer(val displayName: String, val primaryIp: String, val secondaryIp: String) {
    CLOUDFLARE_FAMILY("Cloudflare 1.1.1.3 (Malware + Adult Filter)", "1.1.1.3", "1.0.0.3"),
    CLOUDFLARE_STANDARD("Cloudflare 1.1.1.1 (Standard)", "1.1.1.1", "1.0.0.1"),
    GOOGLE("Google DNS (8.8.8.8)", "8.8.8.8", "8.8.4.4"),
    QUAD9("Quad9 (9.9.9.9)", "9.9.9.9", "149.112.112.112");

    companion object {
        fun fromIp(ip: String): UpstreamDnsServer {
            return entries.find { it.primaryIp == ip } ?: CLOUDFLARE_FAMILY
        }
    }
}
