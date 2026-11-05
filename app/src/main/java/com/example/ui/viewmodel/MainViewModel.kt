package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PureLockApp
import com.example.core.model.OperatingMode
import com.example.core.model.PinVerificationResult
import com.example.data.local.BlockedEventEntity
import com.example.data.local.BlocklistDomainEntity
import com.example.core.security.LauncherIconManager
import com.example.deviceadmin.DeviceOwnerManager
import com.example.vpn.PureLockVpnService
import com.example.vpn.VpnStateHolder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PureLockApp
    private val prefs = app.userPreferencesRepository
    private val blocklistRepo = app.blocklistRepository
    private val pinManager = app.pinManager
    private val deviceOwnerManager = DeviceOwnerManager(application)

    val isVpnRunning: StateFlow<Boolean> = VpnStateHolder.isVpnRunning
    val isConnecting: StateFlow<Boolean> = VpnStateHolder.isConnecting
    val sessionBlockedCount: StateFlow<Int> = VpnStateHolder.blockedCountSession
    val sessionTotalQueries: StateFlow<Int> = VpnStateHolder.totalQueriesSession
    val lastBlockedDomain: StateFlow<String?> = VpnStateHolder.lastBlockedDomain

    val operatingMode: StateFlow<OperatingMode> = prefs.operatingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OperatingMode.PERSONAL)

    val isPinConfigured: StateFlow<Boolean> = pinManager.isPinConfigured
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val recoveryQuestion: StateFlow<String?> = pinManager.recoveryQuestion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val upstreamDnsIp: StateFlow<String> = prefs.upstreamDnsIp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1.1.1.3")

    val autoStartOnBoot: StateFlow<Boolean> = prefs.autoStartOnBoot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val domainCount: StateFlow<Int> = blocklistRepo.domainCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalBlockedCount: StateFlow<Int> = blocklistRepo.totalBlockedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentBlockedEvents: StateFlow<List<BlockedEventEntity>> = blocklistRepo.recentEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customDomains: StateFlow<List<BlocklistDomainEntity>> = blocklistRepo.customDomains
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isUpdatingBlocklist = MutableStateFlow(false)
    val isUpdatingBlocklist: StateFlow<Boolean> = _isUpdatingBlocklist.asStateFlow()

    private val _updateResultMessage = MutableStateFlow<String?>(null)
    val updateResultMessage: StateFlow<String?> = _updateResultMessage.asStateFlow()

    private val _isDeviceOwner = MutableStateFlow(deviceOwnerManager.isDeviceOwner())
    val isDeviceOwner: StateFlow<Boolean> = _isDeviceOwner.asStateFlow()

    private val _isDeviceAdmin = MutableStateFlow(deviceOwnerManager.isDeviceAdmin())
    val isDeviceAdmin: StateFlow<Boolean> = _isDeviceAdmin.asStateFlow()

    private val _isUninstallBlocked = MutableStateFlow(deviceOwnerManager.isUninstallBlocked())
    val isUninstallBlocked: StateFlow<Boolean> = _isUninstallBlocked.asStateFlow()

    fun refreshDeviceOwnerStatus() {
        _isDeviceOwner.value = deviceOwnerManager.isDeviceOwner()
        _isDeviceAdmin.value = deviceOwnerManager.isDeviceAdmin()
        _isUninstallBlocked.value = deviceOwnerManager.isUninstallBlocked()
        _actualComponentHidden.value = LauncherIconManager.isIconHidden(getApplication())
    }

    val isAppIconHiddenPref: StateFlow<Boolean> = prefs.isAppIconHidden
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _actualComponentHidden = MutableStateFlow(LauncherIconManager.isIconHidden(application))
    val actualComponentHidden: StateFlow<Boolean> = _actualComponentHidden.asStateFlow()

    fun refreshLauncherIconStatus() {
        _actualComponentHidden.value = LauncherIconManager.isIconHidden(getApplication())
    }

    fun setAppIconHidden(hide: Boolean): Result<Boolean> {
        val result = LauncherIconManager.setIconHidden(getApplication(), hide)
        if (result.isSuccess) {
            _actualComponentHidden.value = hide
            viewModelScope.launch {
                prefs.setAppIconHidden(hide)
            }
        }
        return result
    }

    fun isVpnPermissionGranted(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    fun startProtection(context: Context) {
        PureLockVpnService.start(context)
    }

    suspend fun stopProtection(): Boolean {
        PureLockVpnService.stop(getApplication())
        return true
    }

    suspend fun verifyPin(pin: String): PinVerificationResult {
        return pinManager.verifyPin(pin)
    }

    suspend fun setupPin(pin: String, question: String, answer: String): Boolean {
        return pinManager.setPin(pin, question, answer)
    }

    suspend fun verifyRecoveryAnswer(answer: String): Boolean {
        return pinManager.verifyRecoveryAnswer(answer)
    }

    fun setOperatingMode(mode: OperatingMode) {
        viewModelScope.launch {
            prefs.setOperatingMode(mode)
        }
    }

    fun setUpstreamDns(ip: String) {
        viewModelScope.launch {
            prefs.setUpstreamDnsIp(ip)
        }
    }

    fun setAutoStartOnBoot(autoStart: Boolean) {
        viewModelScope.launch {
            prefs.setAutoStartOnBoot(autoStart)
        }
    }

    suspend fun addCustomDomain(domain: String): Boolean {
        return blocklistRepo.addCustomDomain(domain)
    }

    suspend fun removeCustomDomain(domain: String): Boolean {
        return blocklistRepo.removeCustomDomain(domain)
    }

    fun updateBlocklist() {
        if (_isUpdatingBlocklist.value) return
        _isUpdatingBlocklist.value = true
        _updateResultMessage.value = null

        viewModelScope.launch {
            val result = blocklistRepo.fetchRemoteBlocklistUpdate()
            _isUpdatingBlocklist.value = false
            result.onSuccess { count ->
                val now = System.currentTimeMillis()
                prefs.setLastBlocklistUpdate(now)
                _updateResultMessage.value = "Successfully synchronized $count adult-content domains."
            }.onFailure { err ->
                _updateResultMessage.value = "Update failed (${err.message}). Retained last verified blocklist."
            }
        }
    }

    fun clearUpdateMessage() {
        _updateResultMessage.value = null
    }

    fun setUninstallBlocked(blocked: Boolean): Boolean {
        val success = deviceOwnerManager.setUninstallBlocked(blocked)
        if (success) {
            _isUninstallBlocked.value = blocked
        }
        return success
    }

    fun clearBlockedHistory() {
        viewModelScope.launch {
            blocklistRepo.clearHistory()
        }
    }
}
