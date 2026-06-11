package com.yashwanthsurabhi.shielddns.vpn

import com.yashwanthsurabhi.shielddns.data.entity.BlockedQueryEntity
import com.yashwanthsurabhi.shielddns.dns.UpstreamResolver
import com.yashwanthsurabhi.shielddns.filter.BlockDecision
import com.yashwanthsurabhi.shielddns.filter.BlocklistRepository
import com.yashwanthsurabhi.shielddns.firewall.AppRuleRepository
import com.yashwanthsurabhi.shielddns.data.store.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DnsPacketHandler(
    private val context: android.content.Context,
    private val blocklistRepository: BlocklistRepository,
    private val upstreamResolver: UpstreamResolver,
    private val appRuleRepository: AppRuleRepository,
    private val networkWatcher: com.yashwanthsurabhi.shielddns.rules.NetworkConditionWatcher,
    private val scope: CoroutineScope,
    private val onBlocked: suspend (BlockedQueryEntity) -> Unit,
    private val settingsProvider: () -> AppSettings,
) {

    private val dnsCache = DnsCache()

    data class HandlerResult(
        val dnsPayload: ByteArray,
        val sourcePort: Int,
        val destPort: Int,
        val clientIp: String,
        val uid: Int,
    )

    suspend fun handleUdpDns(
        ipPacket: ByteArray,
        ipHeaderLength: Int,
        uid: Int,
    ): HandlerResult? {
        val udpOffset = ipHeaderLength
        if (ipPacket.size < udpOffset + 8) return null
        val srcPort = readU16(ipPacket, udpOffset)
        val dstPort = readU16(ipPacket, udpOffset + 2)
        if (dstPort != 53 && srcPort != 53) return null

        val udpLength = readU16(ipPacket, udpOffset + 4)
        val dnsOffset = udpOffset + 8
        if (ipPacket.size < dnsOffset + 12) return null
        val dnsLength = udpLength - 8
        if (dnsLength <= 0 || dnsOffset + dnsLength > ipPacket.size) return null

        val dnsQuery = ipPacket.copyOfRange(dnsOffset, dnsOffset + dnsLength)
        val domain = DnsResponseBuilder.extractQueryDomain(dnsQuery) ?: return null
        val settings = settingsProvider()
        val normalizedDomain = domain.lowercase().removeSuffix(".")

        if (!com.yashwanthsurabhi.shielddns.rules.ScheduleManager.shouldProtectionBeActive(settings)) {
            return forward(dnsQuery, srcPort, dstPort, ipPacket, uid, riskScore = 0)
        }
        if (!networkWatcher.shouldBlockOnCurrentNetwork(settings)) {
            return forward(dnsQuery, srcPort, dstPort, ipPacket, uid, riskScore = 0)
        }

        // 1. Security check
        if (com.yashwanthsurabhi.shielddns.security.SecurityGuard.isDebuggerConnected(context)) {
            return block(dnsQuery, domain, "Security Guard", srcPort, dstPort, ipPacket, uid, riskScore = 100, dnssecStatus = "NONE", reason = "Debugger connected")
        }

        val repResult = com.yashwanthsurabhi.shielddns.reputation.ReputationEngine.evaluate(domain)
        val riskScore = repResult.riskScore

        // 2. App rule bypass/force block
        val appDecision = appRuleRepository.evaluateUid(uid)
        if (appDecision == AppRuleRepository.UidDecision.BYPASS) {
            return forward(dnsQuery, srcPort, dstPort, ipPacket, uid, riskScore)
        }
        if (appDecision == AppRuleRepository.UidDecision.FORCE_BLOCK) {
            return block(dnsQuery, domain, "App rule", srcPort, dstPort, ipPacket, uid, riskScore, reason = "Blocked by per-app firewall rule")
        }

        // 3. Custom Allowlist check
        if (settings.allowlist.any { normalizedDomain == it || normalizedDomain.endsWith(".$it") }) {
            return forward(dnsQuery, srcPort, dstPort, ipPacket, uid, riskScore)
        }

        // 4. Custom Deny check
        if (settings.customDeny.any { normalizedDomain == it || normalizedDomain.endsWith(".$it") }) {
            return block(dnsQuery, domain, "Custom Deny", srcPort, dstPort, ipPacket, uid, riskScore, reason = "Blocked by custom blocklist")
        }

        // 5. Query the blocklist database for the matched category
        val matchedCategory = blocklistRepository.getMatchedCategory(domain)

        // 6. Policy Engine evaluation
        val packageName = appRuleRepository.packageForUid(uid)
        val profile = com.yashwanthsurabhi.shielddns.policy.PolicyProfile.fromKey(settings.activeProfile)
        val policyResult = com.yashwanthsurabhi.shielddns.policy.PolicyEngine.evaluate(
            domain = domain,
            packageName = packageName,
            profile = profile,
            maxAllowedRiskScore = settings.maxAllowedRiskScore,
            adsEnabled = settings.adsListEnabled,
            trackersEnabled = settings.trackersListEnabled,
            malwareEnabled = settings.malwareListEnabled,
            matchedCategory = matchedCategory
        )

        if (policyResult.shouldBlock) {
            return block(
                dnsQuery, domain, policyResult.listName, srcPort, dstPort, ipPacket, uid,
                riskScore = riskScore, reason = policyResult.reason
            )
        } else {
            return forward(dnsQuery, srcPort, dstPort, ipPacket, uid, riskScore)
        }
    }

    private fun block(
        dnsQuery: ByteArray,
        domain: String,
        listName: String,
        srcPort: Int,
        dstPort: Int,
        ipPacket: ByteArray,
        uid: Int,
        riskScore: Int = 0,
        dnssecStatus: String = "NONE",
        reason: String? = null
    ): HandlerResult? {
        val response = DnsResponseBuilder.buildBlockedResponse(dnsQuery) ?: return null
        scope.launch {
            onBlocked(
                BlockedQueryEntity(
                    domain = domain,
                    listName = listName,
                    uid = uid,
                    packageName = appRuleRepository.packageForUid(uid),
                    latencyMs = 0,
                    isBlocked = true,
                    riskScore = riskScore,
                    dnssecStatus = dnssecStatus,
                    blockedReason = reason
                ),
            )
        }
        return HandlerResult(
            dnsPayload = response,
            sourcePort = if (dstPort == 53) 53 else srcPort,
            destPort = if (dstPort == 53) srcPort else dstPort,
            clientIp = clientIp(ipPacket),
            uid = uid,
        )
    }

    private suspend fun forward(
        dnsQuery: ByteArray,
        srcPort: Int,
        dstPort: Int,
        ipPacket: ByteArray,
        uid: Int,
        riskScore: Int = 0,
    ): HandlerResult? {
        val settings = settingsProvider()
        val queriedResolver = IpPacketSupport.ipv4Address(ipPacket, 16)
        val upstreamHost = queriedResolver?.takeIf { it != "0.0.0.0" }
            ?: settings.upstreamDns.ifBlank { "1.1.1.1" }

        // Serve a fresh cached answer without touching the network when possible.
        val cached = dnsCache.get(dnsQuery)
        if (cached != null) {
            val domain = DnsResponseBuilder.extractQueryDomain(dnsQuery) ?: "unknown"
            scope.launch {
                onBlocked(
                    BlockedQueryEntity(
                        domain = domain,
                        listName = "Allowed",
                        uid = uid,
                        packageName = appRuleRepository.packageForUid(uid),
                        latencyMs = 0,
                        isBlocked = false,
                        riskScore = riskScore,
                        dnssecStatus = DnsResponseBuilder.parseDnssecStatus(cached),
                        blockedReason = "Served from DNS cache"
                    ),
                )
            }
            return HandlerResult(
                dnsPayload = cached,
                sourcePort = if (dstPort == 53) 53 else srcPort,
                destPort = if (dstPort == 53) srcPort else dstPort,
                clientIp = clientIp(ipPacket),
                uid = uid,
            )
        }

        val startTime = System.currentTimeMillis()
        val response = upstreamResolver.resolve(
            query = dnsQuery,
            useDoh = settings.useDoh,
            upstreamHost = upstreamHost,
        ) ?: return servfail(dnsQuery, srcPort, dstPort, ipPacket, uid, riskScore)
        val latency = System.currentTimeMillis() - startTime

        dnsCache.put(dnsQuery, response)

        val domain = DnsResponseBuilder.extractQueryDomain(dnsQuery) ?: "unknown"
        val dnssec = DnsResponseBuilder.parseDnssecStatus(response)

        scope.launch {
            onBlocked(
                BlockedQueryEntity(
                    domain = domain,
                    listName = "Allowed",
                    uid = uid,
                    packageName = appRuleRepository.packageForUid(uid),
                    latencyMs = latency,
                    isBlocked = false,
                    riskScore = riskScore,
                    dnssecStatus = dnssec,
                    blockedReason = "Passed all checks"
                ),
            )
        }
        return HandlerResult(
            dnsPayload = response,
            sourcePort = if (dstPort == 53) 53 else srcPort,
            destPort = if (dstPort == 53) srcPort else dstPort,
            clientIp = clientIp(ipPacket),
            uid = uid,
        )
    }

    private fun servfail(
        dnsQuery: ByteArray,
        srcPort: Int,
        dstPort: Int,
        ipPacket: ByteArray,
        uid: Int,
        riskScore: Int = 0,
    ): HandlerResult? {
        val response = DnsResponseBuilder.buildServFailResponse(dnsQuery) ?: return null
        val domain = DnsResponseBuilder.extractQueryDomain(dnsQuery) ?: "unknown"
        scope.launch {
            onBlocked(
                BlockedQueryEntity(
                    domain = domain,
                    listName = "ServFail",
                    uid = uid,
                    packageName = appRuleRepository.packageForUid(uid),
                    latencyMs = 0,
                    isBlocked = true,
                    riskScore = riskScore,
                    dnssecStatus = "NONE",
                    blockedReason = "Upstream resolution failure"
                ),
            )
        }
        return HandlerResult(
            dnsPayload = response,
            sourcePort = if (dstPort == 53) 53 else srcPort,
            destPort = if (dstPort == 53) srcPort else dstPort,
            clientIp = clientIp(ipPacket),
            uid = uid,
        )
    }

    private fun clientIp(ipPacket: ByteArray): String {
        if (ipPacket.size < 20) return "0.0.0.0"
        return "${ipPacket[12].toInt() and 0xFF}.${ipPacket[13].toInt() and 0xFF}." +
            "${ipPacket[14].toInt() and 0xFF}.${ipPacket[15].toInt() and 0xFF}"
    }

    private fun readU16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
}
