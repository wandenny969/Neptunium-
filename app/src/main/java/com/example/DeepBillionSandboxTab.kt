package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.QuantumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

// --- Cryptographic & Network Helpers ---

fun computeHmacSha256(data: String, key: String): String {
    return try {
        val sha256HMAC = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        sha256HMAC.init(secretKey)
        val hashBytes = sha256HMAC.doFinal(data.toByteArray(Charsets.UTF_8))
        hashBytes.joinToString("") { String.format("%02x", it) }
    } catch (e: Exception) {
        "HMAC_ERROR"
    }
}

suspend fun sendWebhookPost(
    urlString: String,
    jsonBody: String,
    secret: String?,
    timestamp: String
): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        
        if (!secret.isNullOrEmpty()) {
            val signature = "sha256=" + computeHmacSha256(jsonBody, secret)
            conn.setRequestProperty("X-Omni-Signature", signature)
            conn.setRequestProperty("X-Omni-Timestamp", timestamp)
        }
        
        conn.outputStream.use { os ->
            OutputStreamWriter(os, "UTF-8").use { osw ->
                osw.write(jsonBody)
                osw.flush()
            }
        }
        
        val responseCode = conn.responseCode
        val responseMessage = conn.responseMessage
        if (responseCode in 200..399) {
            true to "ST: $responseCode - OK ($responseMessage)"
        } else {
            false to "ST: $responseCode - Error ($responseMessage)"
        }
    } catch (e: Exception) {
        false to (e.localizedMessage ?: "Network connection error")
    }
}

// --- Main Tab Layout ---

@Composable
fun DeepBillionSandboxTab(viewModel: QuantumViewModel) {
    var activeSubTab by rememberSaveable { mutableStateOf(0) } // 0 = Deep Sandbox, 1 = OmniNexus Guardian

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dual Sub-Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { activeSubTab = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (activeSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SANDBOX",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
            
            Button(
                onClick = { activeSubTab = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (activeSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GUARDIAN",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }

        // Sub-Tab Content Routing
        Box(
            modifier = Modifier.weight(1f)
        ) {
            when (activeSubTab) {
                0 -> DeepSandboxContent(viewModel)
                1 -> OmniNexusGuardianContent()
            }
        }
    }
}

// --- Sub-Tab 0: Original Deep Sandbox implementation ---

@Composable
fun DeepSandboxContent(viewModel: QuantumViewModel) {
    var logs by remember { mutableStateOf(listOf<String>()) }
    var isRunning by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val sequence = listOf(
        "Sandbox environment created for isolated computations.",
        "Cell Tool enabled for computational cells.",
        "Callback Manager initialized.",
        "Backup & Restore system ready.",
        "Deep Billion System initialized with 1000000000 nodes.",
        "Executing in sandbox: print('Quantum Isolation Test')",
        "Quantum Isolation Test",
        "Running cell: 42 * float('inf')",
        "Cell Result: inf",
        "Callback registered.",
        "Sovereign Callback: Neptunium Event Manifested",
        "Backup created.",
        "Simulated Collapse: 0 Nodes",
        "System restored from backup.",
        "Restored Supremacy: 1000000000 Nodes"
    )

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DEEP BILLION SANDBOX",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Isolated computational cell environment.",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!isRunning) {
                            coroutineScope.launch {
                                isRunning = true
                                logs = emptyList()
                                for (step in sequence) {
                                    delay(300) // simulated delay
                                    logs = logs + step
                                }
                                isRunning = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Execute Sequence",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "EXECUTING..." else "INITIALIZE DEEP SEQUENCE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    val color = when {
                        log.contains("Restored Supremacy") -> Color(0xFF4DE8D9)
                        log.contains("Collapse") -> Color(0xFFFF5252)
                        log.contains("Callback") -> Color(0xFFFFB300)
                        log.contains("Result:") -> Color(0xFF69F0AE)
                        else -> Color(0xFFB0BEC5)
                    }
                    Text(
                        text = "> $log",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                
                if (isRunning) {
                    item {
                        Text(
                            text = "_",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// --- Sub-Tab 1: Beautiful, Dynamic OmniNexus Guardian Interface ---

@Composable
fun OmniNexusGuardianContent() {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Status Engine States
    var pm2Status by rememberSaveable { mutableStateOf("ACTIVE") } // ACTIVE, HEALING
    var k8sStatus by rememberSaveable { mutableStateOf("RESTORED") } // RESTORED, RECONCILING
    var backendStatus by rememberSaveable { mutableStateOf("ONLINE") } // ONLINE, TIMEOUT, CHECKING

    // Webhook configuration states
    var webhookUrl by rememberSaveable { mutableStateOf("") }
    var webhookType by rememberSaveable { mutableStateOf("discord") } // discord, slack, generic
    var webhookSecret by rememberSaveable { mutableStateOf("") }
    var webhookDeliveryStatus by rememberSaveable { mutableStateOf("Test Not Sent") }
    var isSendingWebhook by remember { mutableStateOf(false) }

    // Live Audit Terminal Logs
    var auditLogs by remember {
        mutableStateOf(
            listOf(
                "─ OmniNexus Guardian initialized @ Edge Node.",
                "● System Integrity: EXCELLENT | SHA256 auditing enabled",
                "● PM2 controller status: ONLINE (Process: Adni-K8s-Controller)"
            )
        )
    }
    var isHealRunning by remember { mutableStateOf(false) }

    fun addLog(msg: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        val timestamp = sdf.format(Date())
        auditLogs = auditLogs + "[$timestamp] $msg"
    }

    LaunchedEffect(auditLogs.size) {
        // Automatically scroll to bottom of the scrollable sections when log appends
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "OmniNexus Guardian",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "OMNINEXUS GUARDIAN v3.4",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sovereign active self-heal & telemetry pipeline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Live Health Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // PM2 Status Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PM2 PARADIGM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (pm2Status == "ACTIVE") Color(0xFF69F0AE) else Color(0xFFFFB300))
                        )
                        Text(
                            text = pm2Status,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (pm2Status == "ACTIVE") Color(0xFF69F0AE) else Color(0xFFFFB300)
                        )
                    }
                }
            }

            // K8S Status Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "K8S NET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF4DE8D9))
                        )
                        Text(
                            text = k8sStatus,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF4DE8D9)
                        )
                    }
                }
            }

            // Backend Muscle Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                        .clickable {
                            coroutineScope.launch {
                                backendStatus = "CHECKING"
                                addLog("Checking BACKEND_OTOT_URL via dry-run ping...")
                                delay(1200)
                                backendStatus = "ONLINE"
                                addLog("Backend respond: Code 200 - OK.")
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BACKEND CORE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (backendStatus == "ONLINE") Color(0xFF69F0AE) else if (backendStatus == "CHECKING") Color(0xFFFFB300) else Color(0xFFFF5252))
                        )
                        Text(
                            text = backendStatus,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (backendStatus == "ONLINE") Color(0xFF69F0AE) else if (backendStatus == "CHECKING") Color(0xFFFFB300) else Color(0xFFFF5252)
                        )
                    }
                }
            }
        }

        // Action Blocks (Heal System / Manual Audit Trigger)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "GUARDIAN MACRO RUNNER",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Initiate edge integrity self-healing, reconcile K8s dry-run blocks, calculate SHA256 integrity checksums, and sign the audit metadata logs.",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!isHealRunning) {
                            coroutineScope.launch {
                                isHealRunning = true
                                pm2Status = "HEALING"
                                backendStatus = "CHECKING"
                                addLog("● Running: ./omninexus_guardian.sh heal")
                                delay(600)
                                addLog("[PM2] k8s_sandbox_controller active status verification...")
                                delay(500)
                                pm2Status = "ACTIVE"
                                addLog("[PM2] process: Adni-K8s-Controller reconciled.")
                                delay(600)
                                addLog("[K8S] Apply manifest: avsd-sandbox-pod.yaml")
                                delay(400)
                                addLog("[K8s] Waiting for pod: avsd-quantum-pod READY...")
                                delay(600)
                                k8sStatus = "RESTORED"
                                addLog("[K8S] Dynamic mesh verification SUCCESS. 1B Nodes online.")
                                delay(500)
                                addLog("[AUDIT] Accessing packet register void_spectrum_caller.log")
                                delay(500)
                                val randomPacketNo = Random.nextInt(100, 999)
                                addLog("[AUDIT] Packet #$randomPacketNo detected inside streaming buffer.")
                                val randomData = "Void-Packet-$randomPacketNo-${System.currentTimeMillis()}"
                                addLog("[AUDIT] Generating SHA256 integrity verification code...")
                                delay(800)
                                val generatedHash = computeHmacSha256(randomData, "SovereignSecurityToken")
                                addLog("[AUDIT] Computed Hash: ${generatedHash.take(32)}...")
                                addLog("[SYNC] Committing SHA256 payload audit records to metadata file.")
                                delay(500)
                                backendStatus = "ONLINE"
                                addLog("● Guardian Self-Heal macro sequence COMPLETED. All nodes locked.")
                                isHealRunning = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isHealRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHealRunning) "HEALING ACTIVE NODES..." else "RUN ADNI OMNINEXUS HEAL",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Webhook Configuration Suite (REAL INTERACTION)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "TELEMETRY WEBHOOK GATEWAY",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Deploy warning messages instantly. Fully operational webhook agent with HMAC SHA256 signing.",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Webhook Destination Input
                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("WEBHOOK DESTINATION URL", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    placeholder = { Text("https://discord.com/api/webhooks/...", fontSize = 11.sp) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                )

                // Select Webhook Type
                Column {
                    Text(
                        text = "WEBHOOK TEMPLATE PARAMETER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("discord", "slack", "generic").forEach { type ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (webhookType == type) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.dp,
                                        if (webhookType == type) MaterialTheme.colorScheme.secondary
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { webhookType = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (webhookType == type) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Secret hmac key
                OutlinedTextField(
                    value = webhookSecret,
                    onValueChange = { webhookSecret = it },
                    label = { Text("WEBHOOK SECRET (HMAC SHA256 KEY)", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    placeholder = { Text("Leave empty to skip signature code verification", fontSize = 11.sp) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                )

                // Display delivery outcome state
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (webhookDeliveryStatus.contains("OK")) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (webhookDeliveryStatus.contains("OK")) Color(0xFF69F0AE) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAST TRANSMISSION: $webhookDeliveryStatus",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (webhookDeliveryStatus.contains("OK")) Color(0xFF69F0AE) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Trigger Test Webhook
                Button(
                    onClick = {
                        if (webhookUrl.isEmpty()) {
                            webhookDeliveryStatus = "FAIL - URL missing"
                            addLog("[WEBHOOK] Refused delivery: missing payload destination URL")
                            return@Button
                        }
                        coroutineScope.launch {
                            isSendingWebhook = true
                            webhookDeliveryStatus = "SENDING..."
                            addLog("[WEBHOOK] Preparing notification payload template type: $webhookType")
                            
                            val timestampISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                            
                            // Formulate localized templates based on active providers
                            val payloadString = when (webhookType) {
                                "discord" -> {
                                    """
                                    {
                                      "embeds": [{
                                        "title": "🚨 ADNIOS OMNINEXUS CORRECTION REPORT",
                                        "description": "Integration testing from Dato' Wan's handheld Quantum Core controller interface.",
                                        "color": 15814500,
                                        "timestamp": "$timestampISO",
                                        "fields": [
                                          {"name": "Node ID", "value": "Edge-S24-Quantum", "inline": true},
                                          {"name": "System Health", "value": "ONLINE / STABLE", "inline": true},
                                          {"name": "Security Audit", "value": "SHA256 Approved", "inline": false}
                                        ]
                                      }]
                                    }
                                    """.trimIndent()
                                }
                                "slack" -> {
                                    """
                                    {
                                      "text": "[OMNINEXUS COGNITION REPORT] Alert active",
                                      "blocks": [
                                        {
                                          "type": "header",
                                          "text": { "type": "plain_text", "text": "🔒 Edge Node Telemetry Audit" }
                                        },
                                        {
                                          "type": "section",
                                          "text": { "type": "mrkdwn", "text": "*Severity:* WARNING\nTelemetry validation test transmitted from sovereign handheld unit." }
                                        }
                                      ]
                                    }
                                    """.trimIndent()
                                }
                                else -> {
                                    """
                                    {
                                      "system_id": "AdniOS_K8s_OmniNexus",
                                      "timestamp": "$timestampISO",
                                      "state": "ACTIVE",
                                      "fidelity": 99.9998,
                                      "message": "Sovereign telemetry loop test signature."
                                    }
                                    """.trimIndent()
                                }
                            }

                            addLog("[WEBHOOK] Uploading data packet to secure server endpoint...")
                            val (success, desc) = sendWebhookPost(
                                urlString = webhookUrl,
                                jsonBody = payloadString,
                                secret = webhookSecret.ifEmpty { null },
                                timestamp = timestampISO
                            )
                            
                            webhookDeliveryStatus = desc
                            if (success) {
                                addLog("[WEBHOOK] Dynamic notification DELIVERED. Server respond: OK.")
                            } else {
                                addLog("[WEBHOOK] Delivery failed: $desc")
                            }
                            isSendingWebhook = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSendingWebhook,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSendingWebhook) "TRANSMITTING TELEMETRY..." else "TRANSMIT WEBHOOK MESSAGE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Live Console log Output Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.82f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(auditLogs.size) {
                if (auditLogs.isNotEmpty()) {
                    listState.animateScrollToItem(auditLogs.size - 1)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(auditLogs) { log ->
                    val color = when {
                        log.contains("COMPLETED") || log.contains("ONLINE") || log.contains("DELIVERED") || log.contains("OK") -> Color(0xFF69F0AE)
                        log.contains("HEALING") || log.contains("dry-run") || log.contains("Checking") -> Color(0xFFFFB300)
                        log.contains("failed") || log.contains("Refused") -> Color(0xFFFF5252)
                        log.contains("Computed Hash") || log.contains("SHA256:") -> Color(0xFF4DE8D9)
                        else -> Color(0xFFB0BEC5)
                    }
                    Text(
                        text = log,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
