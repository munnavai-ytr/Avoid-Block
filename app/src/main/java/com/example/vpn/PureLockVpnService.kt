package com.example.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.PureLockApp
import com.example.core.model.OperatingMode
import com.example.dns.DnsPacket
import com.example.dns.IpPacketHelper
import com.example.dns.UpstreamDnsResolver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PureLockVpnService : VpnService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private var upstreamResolver: UpstreamDnsResolver? = null
    private var activeUpstreamIp: String = "1.1.1.3"
    private var activeOperatingMode: String = "PERSONAL"

    override fun onCreate() {
        super.onCreate()
        upstreamResolver = UpstreamDnsResolver(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> startFiltering()
            ACTION_STOP -> stopFiltering()
        }
        return START_STICKY
    }

    private fun startFiltering() {
        if (isRunning) return
        VpnStateHolder.setConnecting(true)

        serviceScope.launch {
            try {
                val app = PureLockApp.instance
                activeUpstreamIp = app.userPreferencesRepository.upstreamDnsIp.first()
                val mode = app.userPreferencesRepository.operatingMode.first()
                activeOperatingMode = mode.name

                // Ensure local blocklist cache is warm
                app.blocklistRepository.refreshInMemoryCache()

                val notification = buildForegroundNotification(0)
                startForeground(NOTIFICATION_ID, notification)

                val vpnFd = establishVpnInterface()
                if (vpnFd == null) {
                    Log.e(TAG, "Failed to establish VPN TUN interface")
                    stopSelf()
                    return@launch
                }

                vpnInterface = vpnFd
                isRunning = true
                VpnStateHolder.setRunning(true)
                app.userPreferencesRepository.setProtectionActive(true)

                // Start packet processing loop on dedicated IO thread
                runPacketLoop(vpnFd)
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting VPN: ${e.message}", e)
                stopFiltering()
            }
        }
    }

    private fun establishVpnInterface(): ParcelFileDescriptor? {
        return try {
            val builder = Builder()
                .setSession("PURELOCK Protection")
                .setMtu(1500)
                // Local virtual DNS gateway
                .addAddress(LOCAL_DNS_IPV4, 32)
                .addDnsServer(LOCAL_DNS_IPV4)
                .addRoute(LOCAL_DNS_IPV4, 32)
                .setBlocking(true)

            // Attempt IPv6 gateway where supported
            try {
                builder.addAddress(LOCAL_DNS_IPV6, 128)
                builder.addDnsServer(LOCAL_DNS_IPV6)
                builder.addRoute(LOCAL_DNS_IPV6, 128)
            } catch (e: Exception) {
                Log.w(TAG, "IPv6 address assignment omitted: ${e.message}")
            }

            // Prevent self-traffic from routing into the VPN
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "Could not disallow self package: ${e.message}")
            }

            // Configure configure intent
            val configureIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                configureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setConfigureIntent(pendingIntent)

            builder.establish()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure VPN builder: ${e.message}", e)
            null
        }
    }

    private fun runPacketLoop(descriptor: ParcelFileDescriptor) {
        val inputStream = FileInputStream(descriptor.fileDescriptor)
        val outputStream = FileOutputStream(descriptor.fileDescriptor)
        val packetBuffer = ByteArray(4096)
        val app = PureLockApp.instance

        Log.i(TAG, "PURELOCK DNS filtering engine running")

        while (isRunning && !serviceScope.coroutineContext[Job]!!.isCancelled) {
            try {
                val bytesRead = inputStream.read(packetBuffer)
                if (bytesRead <= 0) continue

                val parsedIp = IpPacketHelper.parsePacket(packetBuffer, bytesRead) ?: continue

                // Check if this is a DNS query targeted at port 53
                if (parsedIp.dstPort == 53) {
                    val dnsQuery = DnsPacket.parseQuery(
                        packetBuffer,
                        parsedIp.transportPayloadOffset,
                        parsedIp.transportPayloadLength
                    )

                    if (dnsQuery != null) {
                        val qname = dnsQuery.qname
                        val isBlocked = app.blocklistRepository.isDomainBlocked(qname)

                        if (isBlocked) {
                            // Synthesize RFC-compliant blocked response (0.0.0.0 sinkhole)
                            val dnsBlockedPayload = DnsPacket.buildBlockedResponse(dnsQuery)
                            val responseIpPacket = IpPacketHelper.buildUdpResponsePacket(parsedIp, dnsBlockedPayload)

                            outputStream.write(responseIpPacket)
                            outputStream.flush()

                            VpnStateHolder.incrementBlocked(qname)
                            val currentBlocked = VpnStateHolder.blockedCountSession.value
                            updateNotification(currentBlocked)

                            // Asynchronously persist event to Room database
                            serviceScope.launch {
                                app.blocklistRepository.recordBlockedEvent(
                                    domain = qname,
                                    queryType = if (dnsQuery.qtype == 28) "AAAA" else "A",
                                    clientInfo = if (parsedIp.isIpv6) "IPv6" else "IPv4",
                                    mode = activeOperatingMode
                                )
                                app.userPreferencesRepository.incrementTotalQueries()
                            }
                        } else {
                            // Allowed domain: query upstream resolver
                            serviceScope.launch {
                                val queryCopy = ByteArray(parsedIp.transportPayloadLength)
                                System.arraycopy(
                                    packetBuffer,
                                    parsedIp.transportPayloadOffset,
                                    queryCopy,
                                    0,
                                    parsedIp.transportPayloadLength
                                )

                                val upstreamResponse = upstreamResolver?.resolve(
                                    dnsQueryPayload = queryCopy,
                                    upstreamIp = activeUpstreamIp
                                )

                                if (upstreamResponse != null) {
                                    val responseIpPacket = IpPacketHelper.buildUdpResponsePacket(parsedIp, upstreamResponse)
                                    try {
                                        synchronized(outputStream) {
                                            outputStream.write(responseIpPacket)
                                            outputStream.flush()
                                        }
                                        VpnStateHolder.incrementQuery()
                                        app.userPreferencesRepository.incrementTotalQueries()
                                    } catch (e: IOException) {
                                        Log.w(TAG, "Error writing upstream response to TUN: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                if (!isRunning) break
                Log.w(TAG, "TUN I/O error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in packet processing loop: ${e.message}", e)
            }
        }
    }

    private fun stopFiltering() {
        if (!isRunning) return
        isRunning = false
        VpnStateHolder.setRunning(false)

        serviceScope.launch {
            PureLockApp.instance.userPreferencesRepository.setProtectionActive(false)
        }

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing VPN interface: ${e.message}")
        }
        vpnInterface = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopFiltering()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun buildForegroundNotification(blockedCount: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (blockedCount == 0) {
            "Active and filtering adult content"
        } else {
            "Active • $blockedCount adult queries blocked"
        }

        return NotificationCompat.Builder(this, PureLockApp.VPN_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("PURELOCK Protection Active")
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(blockedCount: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildForegroundNotification(blockedCount))
    }

    companion object {
        const val ACTION_START = "com.purelock.action.START"
        const val ACTION_STOP = "com.purelock.action.STOP"
        private const val TAG = "PureLockVpnService"
        private const val NOTIFICATION_ID = 1001

        const val LOCAL_DNS_IPV4 = "10.254.1.1"
        const val LOCAL_DNS_IPV6 = "fd00::1"

        fun start(context: Context) {
            val intent = Intent(context, PureLockVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PureLockVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
