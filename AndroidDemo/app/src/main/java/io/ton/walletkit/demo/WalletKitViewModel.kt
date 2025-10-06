package io.ton.walletkit.demo

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.ton.walletkit.bridge.WalletKitEngine
import io.ton.walletkit.bridge.config.WalletKitBridgeConfig
import io.ton.walletkit.bridge.listener.WalletKitBridgeListener
import io.ton.walletkit.bridge.model.WalletAccount
import io.ton.walletkit.bridge.model.WalletKitEvent
import io.ton.walletkit.demo.model.ConnectPermissionUi
import io.ton.walletkit.demo.model.ConnectRequestUi
import io.ton.walletkit.demo.model.PendingWalletRecord
import io.ton.walletkit.demo.model.SessionSummary
import io.ton.walletkit.demo.model.SignDataRequestUi
import io.ton.walletkit.demo.model.TonNetwork
import io.ton.walletkit.demo.model.TransactionMessageUi
import io.ton.walletkit.demo.model.TransactionRequestUi
import io.ton.walletkit.demo.model.WalletMetadata
import io.ton.walletkit.demo.model.WalletSummary
import io.ton.walletkit.demo.state.SheetState
import io.ton.walletkit.demo.state.WalletUiState
import io.ton.walletkit.demo.util.optNullableString
import io.ton.walletkit.storage.WalletKitStorage
import io.ton.walletkit.storage.impl.InMemoryWalletKitStorage
import io.ton.walletkit.storage.model.StoredSessionHint
import io.ton.walletkit.storage.model.StoredWalletRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.Closeable
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import kotlin.collections.ArrayDeque

class WalletKitViewModel(
    private val engine: WalletKitEngine,
    private val storage: WalletKitStorage = InMemoryWalletKitStorage(),
) : ViewModel() {

    private val _state = MutableStateFlow(
        WalletUiState(
            status = "Initializing WalletKit…",
        ),
    )
    val state: StateFlow<WalletUiState> = _state.asStateFlow()

    private var bridgeListener: Closeable? = null
    private var balanceJob: Job? = null
    private var currentNetwork: TonNetwork = DEFAULT_NETWORK

    private val walletMetadata = mutableMapOf<String, WalletMetadata>()
    private val pendingWallets = ArrayDeque<PendingWalletRecord>()
    private val sessionHints = mutableMapOf<String, SessionHint>()

    init {
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        _state.update { it.copy(status = "Initializing WalletKit…", error = null) }

        val storedWallets = storage.loadAllWallets()
        val storedSessionHints = storage.loadSessionHints()
        storedSessionHints.forEach { (key, hint) ->
            val normalized = normalizeHint(SessionHint(hint.manifestUrl, hint.dAppUrl, hint.iconUrl))
            if (normalized != null) {
                Log.d(LOG_TAG, "Restored session hint key=$key manifest=${normalized.manifestUrl} url=${normalized.dAppUrl}")
                sessionHints[key] = normalized
            }
        }
        val initialNetwork = storedWallets.values.firstOrNull()?.network?.let { TonNetwork.fromBridge(it) } ?: DEFAULT_NETWORK
        currentNetwork = initialNetwork

        val initResult = runCatching { reinitializeForNetwork(initialNetwork, storedWallets, storedSessionHints) }

        if (initResult.isFailure) {
            _state.update {
                it.copy(
                    status = "WalletKit failed to initialize",
                    error = initResult.exceptionOrNull()?.message ?: "Initialization error",
                )
            }
            return
        }

        bridgeListener = engine.addListener(WalletKitBridgeListener(::onBridgeEvent))
        _state.update { it.copy(initialized = true, status = "WalletKit ready", error = null) }

        refreshAll()
        startBalancePolling()
    }

    private fun startBalancePolling() {
        balanceJob?.cancel()
        balanceJob = viewModelScope.launch {
            while (true) {
                delay(BALANCE_REFRESH_MS)
                refreshWallets()
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            refreshWallets()
            refreshSessions()
        }
    }

    fun refreshWallets() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingWallets = true) }
            val summaries = runCatching { loadWalletSummaries() }
            summaries.onSuccess { wallets ->
                val now = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        wallets = wallets,
                        lastUpdated = now,
                        error = null,
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to load wallets") }
            }
            _state.update { it.copy(isLoadingWallets = false) }
        }
    }

    fun refreshSessions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingSessions = true) }
            hydrateSessionHintsFromStorage()
            val result = runCatching { engine.listSessions() }
            result.onSuccess { sessions ->
                val persistedHints = try {
                    storage.loadSessionHints()
                } catch (error: Throwable) {
                    Log.w(LOG_TAG, "Failed to load persisted session hints", error)
                    emptyMap()
                }

                fun resolveHintForKey(rawKey: String?): SessionHint? {
                    if (rawKey.isNullOrBlank()) return null
                    sessionHints[rawKey]?.let { return it }
                    val stored = persistedHints[rawKey] ?: return null
                    val normalized = normalizeHint(SessionHint(stored.manifestUrl, stored.dAppUrl, stored.iconUrl)) ?: return null
                    sessionHints[rawKey] = normalized
                    return normalized
                }

                fun resolveHint(vararg keys: String?): SessionHint? {
                    keys.forEach { key ->
                        val hint = resolveHintForKey(key)
                        if (hint != null) return hint
                    }
                    return null
                }

                fun resolvePersistedHint(vararg keys: String?): SessionHint? {
                    keys.forEach { key ->
                        if (key.isNullOrBlank()) return@forEach
                        val stored = persistedHints[key] ?: return@forEach
                        val normalized = normalizeHint(SessionHint(stored.manifestUrl, stored.dAppUrl, stored.iconUrl))
                        if (normalized != null) {
                            sessionHints[key] = normalized
                            return normalized
                        }
                    }
                    return null
                }

                if (sessionHints.isNotEmpty()) {
                    Log.d(
                        LOG_TAG,
                        "Session hints cache has ${sessionHints.size} entries: ${sessionHints.keys.joinToString()}",
                    )
                    sessionHints.forEach { (key, hintValue) ->
                        Log.d(
                            LOG_TAG,
                            "hint[$key]: manifest=${hintValue.manifestUrl} url=${hintValue.dAppUrl} icon=${hintValue.iconUrl}",
                        )
                    }
                } else {
                    Log.d(LOG_TAG, "Session hints cache is empty before mapping sessions")
                }
                sessions.forEach { session ->
                    Log.d(
                        LOG_TAG,
                        "Raw session from bridge: id=${session.sessionId}, dApp=${session.dAppName}, " +
                            "wallet=${session.walletAddress}, url=${session.dAppUrl}, manifest=${session.manifestUrl}, " +
                            "icon=${session.iconUrl}, urlIsNull=${session.dAppUrl == null}, urlLength=${session.dAppUrl?.length}",
                    )
                }
                val mapped = sessions.mapNotNull { session ->
                    val nameTrimmed = session.dAppName.trim()
                    val walletTrimmed = session.walletAddress.trim()
                    val nameLower = nameTrimmed.lowercase()
                    val walletLower = walletTrimmed.lowercase()
                    val hintKey = sessionHintKey(nameTrimmed, walletTrimmed)
                    val sessionIdKey = session.sessionId
                    val hintKeys = arrayOf(
                        sessionIdKey,
                        hintKey,
                        sessionHintKey(nameTrimmed, null),
                        sessionHintKey(nameLower, walletTrimmed),
                        sessionHintKey(nameLower, walletLower),
                        sessionHintKey(nameTrimmed, walletLower),
                    )
                    val hint = resolveHint(*hintKeys)
                        ?: resolvePersistedHint(*hintKeys)
                    if (hint != null) {
                        Log.d(LOG_TAG, "Resolved hint for session ${session.sessionId}: url=${hint.dAppUrl} manifest=${hint.manifestUrl}")
                    } else {
                        Log.w(
                            LOG_TAG,
                            "No hint resolved for session ${session.sessionId} (keys checked: ${hintKeys.joinToString()})",
                        )
                    }
                    val hintUrl = sanitizeUrl(hint?.dAppUrl)
                    val hintManifest = sanitizeUrl(hint?.manifestUrl)
                    val hintIcon = sanitizeUrl(hint?.iconUrl)
                    val sessionUrl = sanitizeUrl(session.dAppUrl)
                    val sessionManifest = sanitizeUrl(session.manifestUrl)
                    val sessionIcon = sanitizeUrl(session.iconUrl)
                    val appearsDisconnected = sessionUrl == null && sessionManifest == null && hintUrl == null && hintManifest == null
                    if (appearsDisconnected && session.sessionId.isNotBlank()) {
                        Log.d(
                            LOG_TAG,
                            "Bridge returned empty metadata for session ${session.sessionId}; removing stale entry",
                        )
                        removeSessionHintKeys(session.sessionId)
                        viewModelScope.launch { runCatching { engine.disconnectSession(session.sessionId) } }
                        return@mapNotNull null
                    }

                    Log.d(
                        LOG_TAG,
                        "Merging session ${session.sessionId}: sessionUrl=$sessionUrl hintUrl=$hintUrl " +
                            "sessionManifest=$sessionManifest hintManifest=$hintManifest",
                    )
                    val mergedUrl = sessionUrl ?: hintUrl
                    val mergedManifest = sessionManifest ?: hintManifest
                    val mergedIcon = sessionIcon ?: hintIcon
                    val summary = SessionSummary(
                        sessionId = session.sessionId,
                        dAppName = session.dAppName.ifBlank { "Unknown dApp" },
                        walletAddress = session.walletAddress,
                        dAppUrl = mergedUrl,
                        manifestUrl = mergedManifest,
                        iconUrl = mergedIcon,
                        createdAt = parseTimestamp(session.createdAtIso),
                        lastActivity = parseTimestamp(session.lastActivityIso),
                    )
                    Log.d(
                        LOG_TAG,
                        "Summary built for session ${session.sessionId}: url=$mergedUrl manifest=$mergedManifest " +
                            "(fallback url=${hint?.dAppUrl})",
                    )
                    val derivedHint = SessionHint(
                        manifestUrl = mergedManifest,
                        dAppUrl = mergedUrl,
                        iconUrl = mergedIcon,
                    )
                    if (sessionIdKey.isNotBlank()) {
                        updateSessionHint(sessionIdKey, derivedHint)
                    }
                    updateSessionHint(hintKey, derivedHint)
                    updateSessionHint(sessionHintKey(session.dAppName, null), derivedHint)
                    summary
                }
                val finalSessions = mapped
                finalSessions.forEach { summary ->
                    Log.d(
                        LOG_TAG,
                        "Mapped session summary: id=${summary.sessionId}, dApp=${summary.dAppName}, " +
                            "url=${summary.dAppUrl}, manifest=${summary.manifestUrl}, icon=${summary.iconUrl}",
                    )
                }
                _state.update { it.copy(sessions = finalSessions, error = null) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to load sessions") }
            }
            _state.update { it.copy(isLoadingSessions = false) }
        }
    }

    fun openAddWalletSheet() {
        setSheet(SheetState.AddWallet)
    }

    fun showWalletDetails(address: String) {
        val target = state.value.wallets.firstOrNull { it.address == address }
        if (target != null) {
            setSheet(SheetState.WalletDetails(target))
        }
    }

    fun dismissSheet() {
        setSheet(SheetState.None)
    }

    fun showUrlPrompt() {
        _state.update { it.copy(isUrlPromptVisible = true) }
    }

    fun hideUrlPrompt() {
        _state.update { it.copy(isUrlPromptVisible = false) }
    }

    fun importWallet(name: String, network: TonNetwork, words: List<String>) {
        val cleaned = words.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        if (cleaned.size != 24) {
            _state.update { it.copy(error = "Recovery phrase must contain 24 words") }
            return
        }

        val pending = PendingWalletRecord(
            metadata = WalletMetadata(name.ifBlank { defaultWalletName(state.value.wallets.size) }, network, DEFAULT_WALLET_VERSION),
            mnemonic = cleaned,
        )

        viewModelScope.launch {
            switchNetworkIfNeeded(network)
            pendingWallets.addLast(pending)
            val result = runCatching {
                engine.addWalletFromMnemonic(cleaned, DEFAULT_WALLET_VERSION, network.asBridgeValue())
            }
            if (result.isSuccess) {
                refreshWallets()
                dismissSheet()
                logEvent("Imported wallet '${pending.metadata.name}'")
            } else {
                pendingWallets.removeLastOrNull()
                _state.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to import wallet") }
            }
        }
    }

    fun generateWallet(name: String, network: TonNetwork) {
        val words = generateMnemonic()
        val pending = PendingWalletRecord(
            metadata = WalletMetadata(name.ifBlank { defaultWalletName(state.value.wallets.size) }, network, DEFAULT_WALLET_VERSION),
            mnemonic = words,
        )
        viewModelScope.launch {
            switchNetworkIfNeeded(network)
            pendingWallets.addLast(pending)
            val result = runCatching {
                engine.addWalletFromMnemonic(words, DEFAULT_WALLET_VERSION, network.asBridgeValue())
            }
            if (result.isSuccess) {
                refreshWallets()
                dismissSheet()
                logEvent("Generated wallet '${pending.metadata.name}'")
            } else {
                pendingWallets.removeLastOrNull()
                _state.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to generate wallet") }
            }
        }
    }

    fun handleTonConnectUrl(url: String) {
        viewModelScope.launch {
            val trimmed = url.trim()
            val result = runCatching { engine.handleTonConnectUrl(trimmed) }
            result.onSuccess {
                hideUrlPrompt()
                logEvent("Handled TON Connect URL")
            }.onFailure { error ->
                val message = error.message ?: "Ton Connect error"
                val handled = if (message.contains("wallet is required", ignoreCase = true)) {
                    val fallbackNetworks = listOf(TonNetwork.MAINNET, TonNetwork.TESTNET)
                    fallbackNetworks.any { candidate ->
                        if (candidate == currentNetwork) return@any false
                        val retry = runCatching {
                            switchNetworkIfNeeded(candidate)
                            engine.handleTonConnectUrl(trimmed)
                        }
                        if (retry.isSuccess) {
                            hideUrlPrompt()
                            logEvent("Handled TON Connect URL")
                            true
                        } else {
                            false
                        }
                    }
                } else {
                    false
                }

                if (!handled) {
                    _state.update { it.copy(error = message) }
                }
            }
        }
    }

    fun approveConnect(request: ConnectRequestUi, wallet: WalletSummary) {
        viewModelScope.launch {
            val result = runCatching { engine.approveConnect(request.id, wallet.address) }
            result.onSuccess {
                dismissSheet()
                refreshSessions()
                logEvent("Approved connect for ${request.dAppName}")
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to approve connect request") }
            }
        }
    }

    fun rejectConnect(request: ConnectRequestUi, reason: String = "User rejected") {
        viewModelScope.launch {
            val result = runCatching { engine.rejectConnect(request.id, reason) }
            result.onSuccess {
                dismissSheet()
                logEvent("Rejected connect for ${request.dAppName}")
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to reject connect request") }
            }
        }
    }

    fun approveTransaction(request: TransactionRequestUi) {
        viewModelScope.launch {
            val result = runCatching { engine.approveTransaction(request.id) }
            result.onSuccess {
                dismissSheet()
                refreshSessions()
                logEvent("Approved transaction ${request.id}")
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to approve transaction") }
            }
        }
    }

    fun rejectTransaction(request: TransactionRequestUi, reason: String = "User rejected") {
        viewModelScope.launch {
            val result = runCatching { engine.rejectTransaction(request.id, reason) }
            result.onSuccess {
                dismissSheet()
                logEvent("Rejected transaction ${request.id}")
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to reject transaction") }
            }
        }
    }

    fun approveSignData(request: SignDataRequestUi) {
        viewModelScope.launch {
            val result = runCatching { engine.approveSignData(request.id) }
            result.onSuccess {
                dismissSheet()
                logEvent("Approved sign request ${request.id}")
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to approve sign request") }
            }
        }
    }

    fun rejectSignData(request: SignDataRequestUi, reason: String = "User rejected") {
        viewModelScope.launch {
            val result = runCatching { engine.rejectSignData(request.id, reason) }
            result.onSuccess {
                dismissSheet()
                logEvent("Rejected sign request ${request.id}")
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to reject sign request") }
            }
        }
    }

    fun disconnectSession(sessionId: String) {
        viewModelScope.launch {
            val result = runCatching { engine.disconnectSession(sessionId) }
            result.onSuccess {
                refreshSessions()
                logEvent("Disconnected session $sessionId")
                sessionHints.remove(sessionId)
                storage.clearSessionHint(sessionId)
                val summary = state.value.sessions.firstOrNull { it.sessionId == sessionId }
                if (summary != null) {
                    val keyByWallet = sessionHintKey(summary.dAppName, summary.walletAddress)
                    sessionHints.remove(keyByWallet)
                    storage.clearSessionHint(keyByWallet)
                    val fallbackKey = sessionHintKey(summary.dAppName, null)
                    sessionHints.remove(fallbackKey)
                    storage.clearSessionHint(fallbackKey)
                }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to disconnect session") }
            }
        }
    }

    private suspend fun loadWalletSummaries(): List<WalletSummary> {
        val accounts = engine.getWallets()
        Log.d(LOG_TAG, "loadWalletSummaries: got ${accounts.size} accounts")
        val knownAddresses = accounts.map { it.address }.toSet()
        walletMetadata.keys.retainAll(knownAddresses)

        val result = mutableListOf<WalletSummary>()
        for (account in accounts) {
            val metadata = ensureMetadata(account)
            Log.d(LOG_TAG, "loadWalletSummaries: fetching state for ${account.address}")
            val state = runCatching {
                engine.getWalletState(account.address)
            }.onFailure {
                Log.e(LOG_TAG, "loadWalletSummaries: getWalletState failed for ${account.address}", it)
            }.getOrNull()
            val balance = state?.balance
            Log.d(LOG_TAG, "loadWalletSummaries: balance for ${account.address} = $balance")
            val formatted = balance?.let(::formatTon)
            val summary = WalletSummary(
                address = account.address,
                name = metadata.name,
                network = metadata.network,
                version = metadata.version.ifBlank { account.version },
                publicKey = account.publicKey,
                balanceNano = balance,
                balance = formatted,
                transactions = state?.transactions,
                lastUpdated = System.currentTimeMillis(),
            )
            result.add(summary)
        }
        return result
    }

    private suspend fun ensureMetadata(account: WalletAccount): WalletMetadata {
        walletMetadata[account.address]?.let { return it }

        val pending = pendingWallets.removeLastOrNull()
        val storedRecord = storage.loadWallet(account.address)
        val metadata = pending?.metadata
            ?: storedRecord?.let {
                WalletMetadata(
                    name = it.name ?: defaultWalletName(account.index),
                    network = TonNetwork.fromBridge(it.network ?: account.network),
                    version = it.version ?: account.version.ifBlank { DEFAULT_WALLET_VERSION },
                )
            }
            ?: WalletMetadata(
                name = defaultWalletName(account.index),
                network = TonNetwork.fromBridge(account.network),
                version = account.version.ifBlank { DEFAULT_WALLET_VERSION },
            )
        walletMetadata[account.address] = metadata

        if (pending?.mnemonic != null) {
            val record = StoredWalletRecord(
                mnemonic = pending.mnemonic,
                name = metadata.name,
                network = metadata.network.asBridgeValue(),
                version = metadata.version,
            )
            runCatching { storage.saveWallet(account.address, record) }
        } else if (storedRecord != null) {
            val needsUpdate = storedRecord.name != metadata.name ||
                storedRecord.network != metadata.network.asBridgeValue() ||
                storedRecord.version != metadata.version
            if (needsUpdate) {
                val record = StoredWalletRecord(
                    mnemonic = storedRecord.mnemonic,
                    name = metadata.name,
                    network = metadata.network.asBridgeValue(),
                    version = metadata.version,
                )
                runCatching { storage.saveWallet(account.address, record) }
            }
        }

        return metadata
    }

    private suspend fun ensureWallet() {
        val existing = runCatching { engine.getWallets() }.getOrDefault(emptyList())
        if (existing.isNotEmpty()) {
            existing.forEach { ensureMetadata(it) }
            return
        }

        val metadata = WalletMetadata(
            name = DEMO_WALLET_NAME,
            network = currentNetwork,
            version = DEFAULT_WALLET_VERSION,
        )
        val pendingRecord = PendingWalletRecord(metadata = metadata, mnemonic = DEMO_MNEMONIC)
        pendingWallets.addLast(pendingRecord)
        runCatching {
            engine.addWalletFromMnemonic(DEMO_MNEMONIC, DEFAULT_WALLET_VERSION, currentNetwork.asBridgeValue())
        }.onFailure { error ->
            pendingWallets.remove(pendingRecord)
            _state.update { it.copy(error = error.message ?: "Failed to prepare demo wallet") }
        }
    }

    private suspend fun restorePersistedWallets(stored: Map<String, StoredWalletRecord>) {
        if (stored.isEmpty()) return
        val existing = runCatching { engine.getWallets() }.getOrDefault(emptyList())
        val existingAddresses = existing.map { it.address }.toSet()

        stored.forEach { (accountId, record) ->
            if (existingAddresses.contains(accountId)) {
                return@forEach
            }
            if (record.mnemonic.isEmpty()) {
                return@forEach
            }
            val network = record.network?.let { TonNetwork.fromBridge(it) } ?: DEFAULT_NETWORK
            val pendingRecord = PendingWalletRecord(
                metadata = WalletMetadata(
                    name = record.name ?: defaultWalletName(state.value.wallets.size + pendingWallets.size),
                    network = network,
                    version = record.version ?: DEFAULT_WALLET_VERSION,
                ),
                mnemonic = record.mnemonic,
            )
            pendingWallets.addLast(pendingRecord)
            runCatching {
                engine.addWalletFromMnemonic(record.mnemonic, record.version ?: DEFAULT_WALLET_VERSION, network.asBridgeValue())
            }.onFailure {
                pendingWallets.remove(pendingRecord)
            }
        }
    }

    private suspend fun reinitializeForNetwork(
        target: TonNetwork,
        storedOverride: Map<String, StoredWalletRecord>? = null,
        storedHintsOverride: Map<String, StoredSessionHint>? = null,
    ) {
        val endpoints = networkEndpoints(target)
        engine.init(
            WalletKitBridgeConfig(
                network = target.asBridgeValue(),
                tonClientEndpoint = endpoints.tonClientEndpoint,
                tonApiUrl = endpoints.tonApiUrl,
            ),
        )

        currentNetwork = target
        walletMetadata.clear()
        pendingWallets.clear()
        sessionHints.clear()
        val hints = storedHintsOverride ?: storage.loadSessionHints()
        hints.forEach { (key, hint) ->
            val normalized = normalizeHint(SessionHint(hint.manifestUrl, hint.dAppUrl, hint.iconUrl))
            if (normalized != null) {
                sessionHints[key] = normalized
            }
        }
        if (sessionHints.isNotEmpty()) {
            Log.d(LOG_TAG, "Hydrated ${sessionHints.size} session hints for ${target.name.lowercase()} -> ${sessionHints.keys.joinToString()}")
        } else {
            Log.d(LOG_TAG, "No persisted session hints available for ${target.name.lowercase()}")
        }

        val stored = storedOverride ?: storage.loadAllWallets()
        val filtered = stored.filter { TonNetwork.fromBridge(it.value.network) == target }
        restorePersistedWallets(filtered)
        ensureWallet()
    }

    private suspend fun switchNetworkIfNeeded(target: TonNetwork) {
        if (target == currentNetwork) return
        reinitializeForNetwork(target)
        refreshAll()
    }

    private fun setSheet(sheet: SheetState) {
        _state.update { it.copy(sheetState = sheet) }
    }

    private fun onBridgeEvent(event: WalletKitEvent) {
        when (event.type) {
            "ready" -> {
                val wasInitialized = state.value.initialized
                val networkValue = event.data.optNullableString("network")
                val resolvedNetwork = networkValue?.let { TonNetwork.fromBridge(it) }
                resolvedNetwork?.let { currentNetwork = it }
                val networkLabel = resolvedNetwork?.name
                    ?.lowercase(Locale.getDefault())
                    ?.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                val tonApiUrl = event.data.optNullableString("tonApiUrl")
                val tonClientEndpoint = event.data.optNullableString("tonClientEndpoint")
                    ?: event.data.optNullableString("apiUrl")
                val statusMessage = buildString {
                    append("WalletKit ready")
                    networkLabel?.let {
                        append(" • ")
                        append(it)
                    }
                }
                val shouldRefresh = !wasInitialized || state.value.wallets.isEmpty()
                _state.update {
                    it.copy(
                        initialized = true,
                        status = statusMessage,
                        error = null,
                    )
                }
                val extraDetails = buildList {
                    networkLabel?.let { add(it) }
                    tonApiUrl?.let { add("tonapi: $it") }
                    tonClientEndpoint?.let { add("rpc: $it") }
                }.takeIf { it.isNotEmpty() }
                if (extraDetails != null) {
                    logEvent("WalletKit ready (${extraDetails.joinToString(" · ")})")
                } else {
                    logEvent("WalletKit ready")
                }
                if (shouldRefresh) {
                    refreshAll()
                }
            }

            "connectRequest" -> parseConnectRequest(event.data)?.let { request ->
                setSheet(SheetState.Connect(request))
                logEvent("Connect request from ${request.dAppName}")
            }

            "transactionRequest" -> parseTransactionRequest(event.data)?.let { request ->
                setSheet(SheetState.Transaction(request))
                logEvent("Transaction request ${request.id}")
            }

            "signDataRequest" -> parseSignDataRequest(event.data)?.let { request ->
                setSheet(SheetState.SignData(request))
                logEvent("Sign data request ${request.id}")
            }

            "disconnect" -> {
                Log.d(LOG_TAG, "Received disconnect event: ${event.data}")
                viewModelScope.launch {
                    val sessionId = event.data.optNullableString("sessionId")
                        ?: event.data.optNullableString("id")
                    val walletAddress = event.data.optJSONObject("wallet")?.optNullableString("address")
                        ?: event.data.optNullableString("walletAddress")

                    val sessionIdsToClear = mutableSetOf<String>()
                    if (!sessionId.isNullOrBlank()) {
                        sessionIdsToClear += sessionId
                    }
                    if (!walletAddress.isNullOrBlank()) {
                        state.value.sessions.filter {
                            it.walletAddress.equals(walletAddress, ignoreCase = true)
                        }.forEach { summary ->
                            sessionIdsToClear += summary.sessionId
                        }
                    }

                    sessionIdsToClear.forEach { removeSessionHintKeys(it) }

                    sessionIdsToClear.forEach { id ->
                        runCatching { engine.disconnectSession(id) }
                    }

                    refreshSessions()
                    logEvent("Session disconnected")
                }
            }
        }
    }

    private fun parseConnectRequest(json: JSONObject): ConnectRequestUi? {
        val id = json.opt("id")?.toString() ?: json.opt("requestId")?.toString() ?: return null
        val preview = json.optJSONObject("preview")
        val manifest = preview?.optJSONObject("manifest")
        val permissionsSource = preview?.optJSONArray("permissions") ?: json.optJSONArray("permissions")
        val requestedItemsSource = preview?.optJSONArray("requestedItems") ?: json.optJSONArray("requestedItems")

        val permissions = buildList {
            if (permissionsSource != null) {
                for (index in 0 until permissionsSource.length()) {
                    val item = permissionsSource.optJSONObject(index) ?: continue
                    add(
                        ConnectPermissionUi(
                            name = item.optString("name"),
                            title = item.optString("title"),
                            description = item.optString("description"),
                        ),
                    )
                }
            }
        }

        val requestedItems = buildList {
            if (requestedItemsSource != null) {
                for (index in 0 until requestedItemsSource.length()) {
                    add(requestedItemsSource.optString(index))
                }
            }
        }

        val manifestUrl = manifest?.optNullableString("url")
        val request = ConnectRequestUi(
            id = id,
            dAppName = json.optString("dAppName", manifest?.optString("name") ?: "Unknown dApp"),
            dAppUrl = json.optString("dAppUrl", manifestUrl ?: ""),
            manifestUrl = json.optString("manifestUrl", manifestUrl ?: ""),
            iconUrl = json.optNullableString("dAppIconUrl") ?: manifest?.optNullableString("iconUrl"),
            permissions = permissions,
            requestedItems = requestedItems,
            raw = json,
        )
        Log.d(
            LOG_TAG,
            "Connect request received: id=${request.id}, dApp=${request.dAppName}, " +
                "url=${request.dAppUrl}, manifest=${request.manifestUrl}, icon=${request.iconUrl}",
        )
        val hintKey = sessionHintKey(request.dAppName, null)
        val hint = SessionHint(
            manifestUrl = request.manifestUrl.takeIf { it.isNotBlank() },
            dAppUrl = request.dAppUrl.takeIf { it.isNotBlank() },
            iconUrl = request.iconUrl,
        )
        updateSessionHint(hintKey, hint)
        return request
    }

    private fun parseTransactionRequest(json: JSONObject): TransactionRequestUi? {
        val id = json.opt("id")?.toString() ?: return null
        val messages = json.optJSONArray("messages")
        val preview = json.optJSONObject("preview")

        val parsedMessages = buildList {
            if (messages != null) {
                for (index in 0 until messages.length()) {
                    val item = messages.optJSONObject(index) ?: continue
                    val payload = item.optNullableString("payload")
                    val stateInit = item.optNullableString("stateInit")
                    add(
                        TransactionMessageUi(
                            to = item.optString("to"),
                            amount = item.optString("amount"),
                            payload = payload,
                            stateInit = stateInit,
                        ),
                    )
                }
            }
        }

        val previewJson = preview?.toString(2) ?: json.optJSONObject("request")?.toString(2)

        return TransactionRequestUi(
            id = id,
            walletAddress = json.optString("walletAddress"),
            dAppName = json.optString("dAppName", "Unknown dApp"),
            validUntil = json.optLong("validUntil", 0L).takeIf { it > 0 },
            messages = parsedMessages,
            preview = previewJson,
            raw = json,
        )
    }

    private fun parseSignDataRequest(json: JSONObject): SignDataRequestUi? {
        val id = json.opt("id")?.toString() ?: return null
        val preview = json.optJSONObject("preview")
        val payload = json.optJSONObject("data") ?: JSONObject()

        return SignDataRequestUi(
            id = id,
            walletAddress = json.optString("from"),
            payloadType = payload.optString("type", "unknown"),
            payloadContent = payload.toString(2),
            preview = preview?.toString(2),
            raw = json,
        )
    }

    private fun logEvent(message: String) {
        _state.update {
            val events = listOf(message) + it.events
            it.copy(events = events.take(MAX_EVENT_LOG))
        }
    }

    private fun networkEndpoints(network: TonNetwork): NetworkEndpoints = when (network) {
        TonNetwork.MAINNET -> NetworkEndpoints(
            tonClientEndpoint = "https://toncenter.com/api/v2/jsonRPC",
            tonApiUrl = "https://tonapi.io",
        )
        TonNetwork.TESTNET -> NetworkEndpoints(
            tonClientEndpoint = "https://testnet.toncenter.com/api/v2/jsonRPC",
            tonApiUrl = "https://testnet.tonapi.io",
        )
    }

    override fun onCleared() {
        balanceJob?.cancel()
        bridgeListener?.close()
        viewModelScope.launch {
            runCatching { engine.destroy() }
        }
        super.onCleared()
    }

    private fun defaultWalletName(index: Int): String = "Wallet ${index + 1}"

    private fun parseTimestamp(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            when {
                value.matches(NUMERIC_PATTERN) -> value.toLong()
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> Instant.parse(value).toEpochMilli()
                else -> parseIso8601Timestamp(value)
            }
        }.getOrNull()
    }

    private fun parseIso8601Timestamp(value: String): Long {
        val candidates = buildList {
            add(value)
            normalizeIso8601Fraction(value)?.let { add(it) }
        }

        candidates.forEach { candidate ->
            ISO8601_PATTERNS.forEach { pattern ->
                val parsed = runCatching {
                    SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.parse(candidate)?.time
                }.getOrNull()

                if (parsed != null) {
                    return parsed
                }
            }
        }

        throw IllegalArgumentException("Unsupported timestamp format: $value")
    }

    private fun normalizeIso8601Fraction(value: String): String? {
        val timeSeparator = value.indexOf('T')
        if (timeSeparator == -1) return null

        val fractionStart = value.indexOf('.', startIndex = timeSeparator)
        if (fractionStart == -1) return null

        val zoneStart = value.indexOfAny(charArrayOf('Z', '+', '-'), startIndex = fractionStart)
        if (zoneStart == -1) return null

        val fraction = value.substring(fractionStart + 1, zoneStart)
        if (fraction.isEmpty()) return null

        val normalized = when {
            fraction.length == 3 -> return null
            fraction.length < 3 -> fraction.padEnd(3, '0')
            else -> fraction.substring(0, 3)
        }

        val prefix = value.substring(0, fractionStart)
        val suffix = value.substring(zoneStart)
        return "$prefix.$normalized$suffix"
    }

    private fun formatTon(raw: String): String = runCatching {
        BigDecimal(raw)
            .movePointLeft(9)
            .setScale(4, RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString()
    }.getOrElse { raw }

    private data class NetworkEndpoints(
        val tonClientEndpoint: String,
        val tonApiUrl: String,
    )

    private data class SessionHint(
        val manifestUrl: String?,
        val dAppUrl: String?,
        val iconUrl: String?,
    )

    private fun sessionHintKey(dAppName: String, walletAddress: String?): String {
        val normalizedName = dAppName.trim()
        val normalizedWallet = walletAddress?.trim().orEmpty()
        return listOf(normalizedName, normalizedWallet).joinToString("::")
    }

    private fun updateSessionHint(key: String, newHint: SessionHint?) {
        val normalizedNew = normalizeHint(newHint)
        val normalizedExisting = normalizeHint(sessionHints[key])
        val merged = SessionHint(
            manifestUrl = normalizedNew?.manifestUrl ?: normalizedExisting?.manifestUrl,
            dAppUrl = normalizedNew?.dAppUrl ?: normalizedExisting?.dAppUrl,
            iconUrl = normalizedNew?.iconUrl ?: normalizedExisting?.iconUrl,
        )
        if (merged.manifestUrl == null && merged.dAppUrl == null && merged.iconUrl == null) {
            return
        }
        sessionHints[key] = merged
        viewModelScope.launch {
            val record = StoredSessionHint(merged.manifestUrl, merged.dAppUrl, merged.iconUrl)
            runCatching { storage.saveSessionHint(key, record) }
        }
    }

    private suspend fun hydrateSessionHintsFromStorage() {
        if (sessionHints.isNotEmpty()) return
        val persisted = runCatching { storage.loadSessionHints() }.getOrDefault(emptyMap())
        if (persisted.isEmpty()) return
        persisted.forEach { (key, hint) ->
            val normalized = normalizeHint(SessionHint(hint.manifestUrl, hint.dAppUrl, hint.iconUrl))
            if (normalized != null) {
                sessionHints[key] = normalized
            }
        }
        Log.d(LOG_TAG, "Hydrated session hints from storage at runtime: ${sessionHints.keys.joinToString()}")
    }

    private fun removeSessionHintKeys(sessionId: String) {
        val summary = state.value.sessions.firstOrNull { it.sessionId == sessionId }
        val keys = buildList {
            add(sessionId)
            summary?.let {
                add(sessionHintKey(it.dAppName, it.walletAddress))
                add(sessionHintKey(it.dAppName, null))
            }
        }
        keys.forEach { key ->
            sessionHints.remove(key)
            viewModelScope.launch { storage.clearSessionHint(key) }
        }
    }

    private fun generateMnemonic(): List<String> = List(24) { DEMO_WORDS.random() }

    private fun normalizeHint(hint: SessionHint?): SessionHint? {
        if (hint == null) return null
        val manifest = sanitizeUrl(hint.manifestUrl)
        val dApp = sanitizeUrl(hint.dAppUrl)
        val icon = sanitizeUrl(hint.iconUrl)
        if (manifest == null && dApp == null && icon == null) return null
        return SessionHint(manifest, dApp, icon)
    }

    private fun sanitizeUrl(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (value.equals("null", ignoreCase = true)) return null
        return value
    }

    companion object {
        private val NUMERIC_PATTERN = Regex("^-?\\d+")
        private val ISO8601_PATTERNS = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
        )
        private const val BALANCE_REFRESH_MS = 20_000L
        private const val MAX_EVENT_LOG = 12
        private const val DEFAULT_WALLET_VERSION = "v5r1"
        private val DEFAULT_NETWORK = TonNetwork.MAINNET
        private const val LOG_TAG = "WalletKitVM"

        private val DEMO_WORDS = listOf(
            "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract",
            "absurd", "abuse", "access", "accident", "account", "accuse", "achieve", "acid",
            "acoustic", "acquire", "across", "act", "action", "actor", "actress", "actual",
            "adapt", "addict", "address", "adjust", "admit", "adult", "advance", "advice",
        )

        private val DEMO_MNEMONIC = listOf(
            "canvas", "puzzle", "ski", "divide", "crime", "arrow",
            "object", "canvas", "point", "cover", "method", "bargain",
            "siren", "bean", "shrimp", "found", "gravity", "vivid",
            "pelican", "replace", "tuition", "screen", "orange", "album",
        )

        private const val DEMO_WALLET_NAME = "Demo Wallet"

        fun factory(
            engine: WalletKitEngine,
            storage: WalletKitStorage = InMemoryWalletKitStorage(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(WalletKitViewModel::class.java))
                return WalletKitViewModel(engine, storage) as T
            }
        }
    }
}
