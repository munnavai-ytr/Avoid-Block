package com.example.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnStateHolder {
    private val _isVpnRunning = MutableStateFlow(false)
    val isVpnRunning: StateFlow<Boolean> = _isVpnRunning.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _blockedCountSession = MutableStateFlow(0)
    val blockedCountSession: StateFlow<Int> = _blockedCountSession.asStateFlow()

    private val _totalQueriesSession = MutableStateFlow(0)
    val totalQueriesSession: StateFlow<Int> = _totalQueriesSession.asStateFlow()

    private val _lastBlockedDomain = MutableStateFlow<String?>(null)
    val lastBlockedDomain: StateFlow<String?> = _lastBlockedDomain.asStateFlow()

    fun setRunning(running: Boolean) {
        _isVpnRunning.value = running
        if (!running) {
            _isConnecting.value = false
        }
    }

    fun setConnecting(connecting: Boolean) {
        _isConnecting.value = connecting
    }

    fun incrementBlocked(domain: String) {
        _blockedCountSession.value += 1
        _totalQueriesSession.value += 1
        _lastBlockedDomain.value = domain
    }

    fun incrementQuery() {
        _totalQueriesSession.value += 1
    }

    fun resetSession() {
        _blockedCountSession.value = 0
        _totalQueriesSession.value = 0
        _lastBlockedDomain.value = null
    }
}
