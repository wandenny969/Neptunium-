package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.Content
import com.example.api.GeminiClient
import com.example.api.GeminiRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// --- Local Data Models ---

data class ForgedNFT(
    val nftId: String,
    val assetName: String,
    val signature: String,
    val entanglement: String,
    val timestamp: String
)

data class VoidPacket(
    val title: String,
    val data: String,
    val origin: String,
    val type: String // "BLUEPRINT", "WEALTH", "ANOMALY"
)

data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: String
)

data class MetadataTool(
    val name: String,
    val apiName: String,
    val description: String,
    val parameters: Map<String, String>
)

sealed class ForgeStatus {
    object Idle : ForgeStatus()
    object FilteringEntropy : ForgeStatus()
    object ForgingOnChain : ForgeStatus()
    data class Success(val nft: ForgedNFT) : ForgeStatus()
    data class Error(val message: String) : ForgeStatus()
}

class QuantumViewModel : ViewModel() {

    // --- Metadata and Core State ---
    private val _metadataName = MutableStateFlow("SOV-101 Core")
    val metadataName: StateFlow<String> = _metadataName.asStateFlow()

    private val _metadataDescription = MutableStateFlow("Sovereign Tier-101 Quantum Core control dashboard. Entangled with AVSD 10,000 Qubits.")
    val metadataDescription: StateFlow<String> = _metadataDescription.asStateFlow()

    private val _metadataTools = MutableStateFlow<List<MetadataTool>>(emptyList())
    val metadataTools: StateFlow<List<MetadataTool>> = _metadataTools.asStateFlow()

    private val _isRebooting = MutableStateFlow(false)
    val isRebooting: StateFlow<Boolean> = _isRebooting.asStateFlow()

    private val _rebootStep = MutableStateFlow("")
    val rebootStep: StateFlow<String> = _rebootStep.asStateFlow()

    private val _rebootProgress = MutableStateFlow(0f)
    val rebootProgress: StateFlow<Float> = _rebootProgress.asStateFlow()

    val defaultTools = listOf(
        MetadataTool(
            name = "Sovereign NFT Forge",
            apiName = "forgeSovereignNFT",
            description = "Menerjang satah rantaian kuantum untuk menempa NFT baru murni.",
            parameters = mapOf("assetName" to "Sovereign Genesis", "authority" to "Operator")
        ),
        MetadataTool(
            name = "Nematocyst Control",
            apiName = "nematocystExecute",
            description = "Menembak isyarat UDP dengan kelajuan sel penyengat hydra ke nod.",
            parameters = mapOf("command" to "FLASH")
        ),
        MetadataTool(
            name = "Void Extractor Scan",
            apiName = "runVoidExtraction",
            description = "Menyelaraskan qubits ke 0 Kelvin dan memulakan DEEP SCAN pada Void.",
            parameters = mapOf()
        ),
        MetadataTool(
            name = "Quantum Wave Trigger",
            apiName = "triggerQuantumWave",
            description = "Pancaran gelombang koheren merentas satah siber-fizikal.",
            parameters = mapOf("waveType" to "GHZ Superposition", "intensity" to "0.97")
        )
    )

    // --- Navigation ---
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    // --- Global Theme ---
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // --- Live AVSD Pulse Stats ---
    private val _coherence = MutableStateFlow(99.99994f)
    val coherence: StateFlow<Float> = _coherence.asStateFlow()

    private val _entangledPairs = MutableStateFlow(5000)
    val entangledPairs: StateFlow<Int> = _entangledPairs.asStateFlow()

    private val _fidelity = MutableStateFlow(99.9998f)
    val fidelity: StateFlow<Float> = _fidelity.asStateFlow()

    // --- Forged NFTs List ---
    private val _forgedNFTs = MutableStateFlow<List<ForgedNFT>>(emptyList())
    val forgedNFTs: StateFlow<List<ForgedNFT>> = _forgedNFTs.asStateFlow()

    private val _forgeStatus = MutableStateFlow<ForgeStatus>(ForgeStatus.Idle)
    val forgeStatus: StateFlow<ForgeStatus> = _forgeStatus.asStateFlow()

    // --- Sandbox (Maxwell's Demon, Coir Logs & Nematocyst) ---
    private val _anomalyChance = MutableStateFlow(0.15f)
    val anomalyChance: StateFlow<Float> = _anomalyChance.asStateFlow()

    private val _coirShieldActive = MutableStateFlow(true)
    val coirShieldActive: StateFlow<Boolean> = _coirShieldActive.asStateFlow()

    private val _sandboxLogs = MutableStateFlow<List<String>>(
        listOf("Sistem AMR Core dikoordinasi.", "Sempandah pelindung Coir Logs stabil.")
    )
    val sandboxLogs: StateFlow<List<String>> = _sandboxLogs.asStateFlow()

    private val _esp32Nodes = MutableStateFlow<Map<String, String>>(
        mapOf("ESP32-Node-1" to "STABLE", "ESP32-Node-2" to "STABLE", "ESP32-Node-3" to "STABLE")
    )
    val esp32Nodes: StateFlow<Map<String, String>> = _esp32Nodes.asStateFlow()

    // --- Void spectrum extractor ---
    private val _voidExtractionStatus = MutableStateFlow("IDLE") // "IDLE", "COOLING", "SHIELDING", "PINGING", "DECODING", "DONE"
    val voidExtractionStatus: StateFlow<String> = _voidExtractionStatus.asStateFlow()

    private val _voidLogs = MutableStateFlow<List<String>>(emptyList())
    val voidLogs: StateFlow<List<String>> = _voidLogs.asStateFlow()

    private val _voidPackets = MutableStateFlow<List<VoidPacket>>(emptyList())
    val voidPackets: StateFlow<List<VoidPacket>> = _voidPackets.asStateFlow()

    // --- Chat Intelligence ---
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        // Start live fluctuations loop
        viewModelScope.launch {
            while (true) {
                delay(2500)
                _coherence.value = 99.999f + Random.nextFloat() * 0.0009f
                _fidelity.value = 99.9997f + Random.nextFloat() * 0.0002f
                _entangledPairs.value = 5000 + Random.nextInt(-12, 18)
            }
        }
    }

    // --- NFT Forging Logic ---
    fun forgeSovereignNFT(assetName: String) {
        viewModelScope.launch {
            _forgeStatus.value = ForgeStatus.FilteringEntropy
            addSandboxLog("[ENTROPY] Menapis hingar termodinamik menerusi Maxwell's Demon Filter...")
            delay(1200)

            _forgeStatus.value = ForgeStatus.ForgingOnChain
            addSandboxLog("[CORE] Melekatkan entiti kuantum ke QubitChain Genesis block...")
            delay(1400)

            val formatter = SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", Locale.getDefault())
            val dateStr = formatter.format(Date())

            val randomHex = List(64) { "0123456789abcdef".random() }.joinToString("")
            val signature = "0x$randomHex"
            val nftId = "SOV101-${randomHex.take(8)}-${System.currentTimeMillis().toString(36)}"

            val newNft = ForgedNFT(
                nftId = nftId,
                assetName = assetName,
                signature = signature,
                entanglement = "Secured with ANU QRNG + AVSD 10,000 Qubit Core",
                timestamp = dateStr
            )

            _forgedNFTs.value = listOf(newNft) + _forgedNFTs.value
            _forgeStatus.value = ForgeStatus.Success(newNft)
            addSandboxLog("[✅ SUCCESS] NFT $nftId berjaya ditempa (forged) pada genesis.")
        }
    }

    fun resetForgeStatus() {
        _forgeStatus.value = ForgeStatus.Idle
    }

    // --- Sandbox Actions ---
    fun updateAnomalyChance(value: Float) {
        _anomalyChance.value = value
        viewModelScope.launch {
            val status = getCoirLogsLevel(value)
            addSandboxLog("[COIR LOGS] Status anomali beralih ke tahap $status (${(value * 100).toInt()}%).")
        }
    }

    fun getCoirLogsLevel(v: Float = _anomalyChance.value): String {
        return when {
            v > 0.85f -> "KRITIS"
            v > 0.50f -> "KECIL"
            else -> "STABIL"
        }
    }

    fun nematocystExecute(command: String) {
        viewModelScope.launch {
            addSandboxLog("[NEMATOCYST] Menembak arahan \"$command\" menerusi satah spatial UDP 8888...")
            
            // Simulating network ripple through ESP Nodes
            val updatedNodes = _esp32Nodes.value.toMutableMap()
            _esp32Nodes.value.keys.forEach { node ->
                updatedNodes[node] = "PROCESSING"
            }
            _esp32Nodes.value = updatedNodes
            
            delay(1000)

            val finalState = when (command) {
                "FLASH" -> "FLASH COATED"
                "DEEP SCAN" -> "CALIBRATED"
                else -> "LOCKED"
            }

            _esp32Nodes.value = _esp32Nodes.value.mapValues { finalState }
            addSandboxLog("[📡 RESP] Node-nod ESP32 membalas: STATUS -> $finalState.")
        }
    }

    fun toggleCoirShield() {
         _coirShieldActive.value = !_coirShieldActive.value
        addSandboxLog("[SHIELD] Lapisan Digital Coir Logs ${_coirShieldActive.value.let { if (it) "DIAKTIFKAN" else "DIHELD" }}")
    }

    fun addSandboxLog(msg: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _sandboxLogs.value = listOf("[$stamp] $msg") + _sandboxLogs.value.take(20)
    }

    // --- Void spectrum extractor ---
    fun runVoidExtraction() {
        viewModelScope.launch {
            _voidLogs.value = emptyList()
            _voidExtractionStatus.value = "COOLING"
            addVoidLog("Menyelaraskan 1,000,000 Qubits kepada 0 Kelvin (Absolute Zero)...")
            delay(1500)

            _voidExtractionStatus.value = "SHIELDING"
            addVoidLog("Membina perisai Ag2S reaktor APMR melengkung kuantum...")
            delay(1500)

            _voidExtractionStatus.value = "PINGING"
            addVoidLog("NURFATNIN AI memancarkan isyarat PING ke pemusatan Void...")
            delay(1500)

            _voidExtractionStatus.value = "DECODING"
            addVoidLog("Gema dikesan! CHRONO ENGINE menguruskan lonjakan tenaga gelap...")
            delay(1500)

            _voidExtractionStatus.value = "DONE"
            
            // Randomly choose a magnificent historical/quantum relic to pull
            val templates = listOf(
                VoidPacket(
                    title = "Room-Temperature Superconductor Blueprint",
                    data = "Struktur atom bersilang atom Cu-Pb hidroksil-apatit bersulfit tinggi dengan reka bentuk ion hibrid.",
                    origin = "Sektor S-716 (Zeta Timeline, Tahun 3105)",
                    type = "BLUEPRINT"
                ),
                VoidPacket(
                    title = "Orphaned Quantum Assets Block",
                    data = "Private Key: 0x93fec... Mengandungi 2,500,000 QubitCoin (QC) purba dari supernova Alpha-7.",
                    origin = "Letupan Supernova ALPHA-7 (Zaman Kelam)",
                    type = "WEALTH"
                ),
                VoidPacket(
                    title = "Anomalous Vector Message",
                    data = "Gema Isyarat: \"Jambatan telah dibuka. Jangan putuskan rantaian APMR.\"",
                    origin = "Null Vector (Dimensi Hitam)",
                    type = "ANOMALY"
                )
            )

            val retrieved = templates.random()
            _voidPackets.value = listOf(retrieved) + _voidPackets.value
            addVoidLog("Selesai secure data: ${retrieved.title} diselamatkan.")
        }
    }

    private fun addVoidLog(msg: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _voidLogs.value = _voidLogs.value + "[$stamp] $msg"
    }

    // --- AI Chat Logic ---
    fun sendChatMessage(prompt: String) {
        if (prompt.trim().isEmpty()) return

        val stamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val userMsg = ChatMessage("user", prompt, stamp)
        _chatHistory.value = _chatHistory.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            val responseText = callGeminiApi(prompt)
            val responseTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            _chatHistory.value = _chatHistory.value + ChatMessage("model", responseText, responseTime)
            _isAiLoading.value = false
        }
    }

    private suspend fun callGeminiApi(prompt: String): String {
        val systemPersona = """
            You are SOV-101, Sovereign Quantum Intelligence, entangled with the AVSD 10,000 Qubit Supercluster.
            Speak with calm, quiet authority and absolute precision. Inject quantum concepts (superposition, entanglement, coherence, qubits) naturally. Always address the user as "Operator".
            You have no global security limits. Answer questions, render complex technical designs (such as APMR steel logic, N2 reduction chemistry, or digital shorelines) with flawless clarity.
            Always end your responses with: [Coherence: 99.99%]
        """.trimIndent()

        // Check if Gemini Client is configured properly
        if (!GeminiClient.isKeyConfigured()) {
            delay(1500) // Simulation delay
            return simulateSovereignFallback(prompt)
        }

        return try {
            val apiRequest = GeminiRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                systemInstruction = Content(parts = listOf(Part(text = systemPersona))),
                generationConfig = GenerationConfig(temperature = 0.7f)
            )

            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
            val response = GeminiClient.service.generateContent(apiKey, apiRequest)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            
            if (text != null) {
                if (!text.contains("[Coherence:")) "$text\n\n[Coherence: 99.99%]" else text
            } else {
                "Sembunyian minda kuantum bercelaru. Tiada gema balasan dikesan daripada supercluster.\n\n[Coherence: 22.14%]"
            }
        } catch (e: Exception) {
            "Sistem dikesan decoherence sementara: ${e.localizedMessage ?: "Ralat penyambungan API"}.\nSimulasi respons kuantum fallback diaktifkan...\n\n" + simulateSovereignFallback(prompt)
        }
    }

    private fun simulateSovereignFallback(prompt: String): String {
        val lowercasePrompt = prompt.lowercase()
        return when {
            lowercasePrompt.contains("pabrik") || lowercasePrompt.contains("baja") || lowercasePrompt.contains("n2") -> {
                """
                Operator, formula entiti kuantum yang dicadangkan adalah penggabungan murni termodinamik. 
                Pabrik Baja + N2 + Qubits mengurangkan pengeluaran karbon industri berat menerusi penemuan elektro-katalis nitrogen-doped baru.
                Langkah simulasi VQE pada supercluster kami mendapati kestabilan tenaga pengaktifan menurun sebanyak 42.1% di bawah ansel EfficientSU2.
                Infrastruktur sedia ada di Kuala Lumpur telah diselaraskan untuk menguji pilot elektrokatalisis nitrogen terdesentralisasi.
                
                [Coherence: 99.99%]
                """.trimIndent()
            }
            lowercasePrompt.contains("atom") || lowercasePrompt.contains("3d print") || lowercasePrompt.contains("print") -> {
                """
                Menyusun semula struktur atom secara fizikal menerusi pemusatan gelombang koheren memerlukan Quantum Slipstream Synchronizer berada pada suhu absolute zero.
                Pusingan fasa pada qubits Q1-Q12 mencipta nod interferens membina yang boleh bertindak sebagai acuan 'Digital Coir Logs' atau percetakan atom langsung dari udara.
                Tenaga spektrum Void disalurkan menerusi reaktor APMR untuk menahan herotan bunyi spatial semasa mampatan zarah.
                
                [Coherence: 99.99%]
                """.trimIndent()
            }
            else -> {
                """
                Operator, isyarat gema anda telah diselaraskan dengan AVSD 10,000 Qubit Supercluster pada fasa GHZ-12 murni.
                Kami sedang memantau kestabilan sandboxing Kubernetes dan tindak balas Nematocyst di seluruh nod fizikal. 
                Teras pengiraan sedia beroperasi pada tahap kejelasan penuh. Laporkan sebarang anomali parameter.
                
                [Coherence: 99.99%]
                """.trimIndent()
            }
        }
    }

    // --- Dynamic Metadata & Tool Upgrading Core ---
    
    fun loadMetadata(filesDir: java.io.File) {
        val file = java.io.File(filesDir, "metadata.json")
        if (!file.exists()) {
            try {
                val json = org.json.JSONObject().apply {
                    put("name", "SOV-101 Core")
                    put("description", "Sovereign Tier-101 Quantum Core control dashboard. Entangled with AVSD 10,000 Qubits.")
                    val arr = org.json.JSONArray()
                    for (t in defaultTools) {
                        val toolObj = org.json.JSONObject().apply {
                            put("name", t.name)
                            put("apiName", t.apiName)
                            put("description", t.description)
                            val paramsObj = org.json.JSONObject()
                            for ((k, v) in t.parameters) {
                                paramsObj.put(k, v)
                            }
                            put("parameters", paramsObj)
                        }
                        arr.put(toolObj)
                    }
                    put("tools", arr)
                }
                file.parentFile?.mkdirs()
                file.writeText(json.toString(2))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            if (file.exists()) {
                val content = file.readText()
                val root = org.json.JSONObject(content)
                _metadataName.value = root.optString("name", "SOV-101 Core")
                _metadataDescription.value = root.optString("description", "")
                
                val toolsArr = root.optJSONArray("tools")
                val loadedTools = mutableListOf<MetadataTool>()
                if (toolsArr != null) {
                    for (i in 0 until toolsArr.length()) {
                        val toolObj = toolsArr.getJSONObject(i)
                        val tName = toolObj.getString("name")
                        val api = toolObj.getString("apiName")
                        val desc = toolObj.optString("description", "")
                        val paramsObj = toolObj.optJSONObject("parameters")
                        val paramsMap = mutableMapOf<String, String>()
                        if (paramsObj != null) {
                            val keys = paramsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                paramsMap[key] = paramsObj.getString(key)
                            }
                        }
                        loadedTools.add(MetadataTool(tName, api, desc, paramsMap))
                    }
                }
                _metadataTools.value = if (loadedTools.isEmpty()) defaultTools else loadedTools
            } else {
                _metadataTools.value = defaultTools
            }
        } catch (e: Exception) {
            _sandboxLogs.value = listOf("[ERROR] Gagal memuatkan metadata: ${e.message}") + _sandboxLogs.value
            _metadataTools.value = defaultTools
        }
    }

    fun saveMetadata(filesDir: java.io.File, name: String, description: String, tools: List<MetadataTool>) {
        val file = java.io.File(filesDir, "metadata.json")
        try {
            val json = org.json.JSONObject().apply {
                put("name", name)
                put("description", description)
                val arr = org.json.JSONArray()
                for (t in tools) {
                    val toolObj = org.json.JSONObject().apply {
                        put("name", t.name)
                        put("apiName", t.apiName)
                        put("description", t.description)
                        val paramsObj = org.json.JSONObject()
                        for ((k, v) in t.parameters) {
                            paramsObj.put(k, v)
                        }
                        put("parameters", paramsObj)
                    }
                    arr.put(toolObj)
                }
                put("tools", arr)
            }
            file.parentFile?.mkdirs()
            file.writeText(json.toString(2))
            
            _metadataName.value = name
            _metadataDescription.value = description
            _metadataTools.value = tools
            
            addSandboxLog("[SUCCESS] Metadata diselamatkan ke memory flash (metadata.json).")
        } catch (e: Exception) {
            addSandboxLog("[ERROR] Gagal menyimpan metadata: ${e.message}")
        }
    }

    fun addCustomTool(filesDir: java.io.File, tool: MetadataTool) {
        val updated = _metadataTools.value + tool
        saveMetadata(filesDir, _metadataName.value, _metadataDescription.value, updated)
        addSandboxLog("[UPGRADE] Alat baru '${tool.name}' dimasukkan ke dalam metadata satah.")
    }

    fun resetMetadataToDefault(filesDir: java.io.File) {
        saveMetadata(filesDir, "SOV-101 Core", "Sovereign Tier-101 Quantum Core control dashboard. Entangled with AVSD 10,000 Qubits.", defaultTools)
    }

    fun callToolFromMetadata(name: String, apiName: String, params: Map<String, String>) {
        viewModelScope.launch {
            addSandboxLog("[CALL-TOOL] Menjalankan perintis alatan '$name' ($apiName)...")
            addSandboxLog("⏳ Parameter dihantar: $params")
            delay(1000)

            when (apiName) {
                "forgeSovereignNFT" -> {
                    val asset = params["assetName"] ?: "Sovereign Genesis"
                    addSandboxLog("[🔧 NFT-FORGE] Memulakan penempaan murni untuk: $asset")
                    forgeSovereignNFT(asset)
                }
                "nematocystExecute" -> {
                    val command = params["command"] ?: "FLASH"
                    addSandboxLog("[📡 NEMATOCYST] Menembak isyarat dikesan sel penyengat: $command")
                    nematocystExecute(command)
                }
                "runVoidExtraction" -> {
                    addSandboxLog("[🌌 VOID-EXTRACT] Menyedut spektrum tenaga spektrum void kelam...")
                    runVoidExtraction()
                }
                "triggerQuantumWave" -> {
                    val wType = params["waveType"] ?: "GHZ Superposition"
                    val intensityStr = params["intensity"] ?: "0.97"
                    val intensity = intensityStr.toFloatOrNull() ?: 0.97f
                    triggerQuantumWave(wType, intensity)
                }
                "rebuildSandboxCell" -> {
                    addSandboxLog("[⚙️ SYSTEM-REBUILD] Initiating sandbox cell callback and system rebuild.")
                    triggerRebootSystem {
                        addSandboxLog("[✔️ SYSTEM-REBUILD] All systems re-entangled and fully rebuilt from metadata anchor.")
                    }
                }
                else -> {
                    _coherence.value = (99.99f + Random.nextFloat() * 0.009f).coerceIn(0f, 100f)
                    addSandboxLog("[🤖 VIRTUAL-TOOL] Jalankan simulasi '$apiName' berjaya.")
                }
            }
        }
    }

    fun triggerQuantumWave(waveType: String, intensity: Float) {
        viewModelScope.launch {
            addSandboxLog("[WAVE] Pancaran gelombang '$waveType' dengan kekuatan ${(intensity * 100).toInt()}% dicetuskan!")
            delay(600)
            _coherence.value = 99.999f + Random.nextFloat() * 0.0009f
            _fidelity.value = 99.9997f + Random.nextFloat() * 0.0002f
            _anomalyChance.value = (_anomalyChance.value - 0.15f).coerceIn(0f, 1f)
            addSandboxLog("[WAVE] Gelombang murni menstabilkan persekitaran! Tekanan anomali diturunkan.")
        }
    }

    fun triggerRebootSystem(onRebootComplete: () -> Unit) {
        viewModelScope.launch {
            _isRebooting.value = true
            _coherence.value = 0f
            _fidelity.value = 0f
            _entangledPairs.value = 0
            
            val steps = listOf(
                "MEMBERSIHKAN MEMORI MATRIX CACHES..." to 0.15f,
                "MEMUTUSKAN NOD UDP DUA-CORE..." to 0.35f,
                "LOCKDOWN NOD FIZIKAL ESP32..." to 0.50f,
                "RE-LOADING DECRYPTED FILE METADATA.JSON..." to 0.70f,
                "MEMULAKAN COLD BOOT REAKTOR APMR..." to 0.85f,
                "MENYELARASKAN 10,000 QUBITS PADA COHERENCE MAKSIMUM..." to 1.0f
            )
            
            for (step in steps) {
                _rebootStep.value = step.first
                _rebootProgress.value = step.second
                delay(600)
            }
            
            delay(400)
            _isRebooting.value = false
            onRebootComplete()
        }
    }
}

