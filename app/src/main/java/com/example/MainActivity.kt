package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import java.util.Locale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.GeminiClient
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.ForgeStatus
import com.example.viewmodel.ForgedNFT
import com.example.viewmodel.QuantumViewModel
import com.example.viewmodel.VoidPacket
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val viewModel: QuantumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val crashPrefs = getSharedPreferences("crash_prefs", MODE_PRIVATE)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(sw))
                crashPrefs.edit().putString("last_crash", sw.toString()).commit()
            } catch (e: Exception) {
                // Ignore
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        super.onCreate(savedInstanceState)

        val lastCrash = crashPrefs.getString("last_crash", null)

        // Load dynamic metadata at startup (after super.onCreate)
        viewModel.loadMetadata(filesDir)

        enableEdgeToEdge()
        setContent {
            val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (lastCrash != null) {
                        CrashRecoveryScreen(lastCrash) {
                            crashPrefs.edit().remove("last_crash").apply()
                        }
                    } else {
                        QuantumDashboardApp(viewModel)
                    }
                }
            }
        }
    }
}

// Stub greeting so standard screenshot test remains fully compatible and compiling!
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hello $name!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Sovereign Quantum Hub active. System Ready.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuantumDashboardApp(viewModel: QuantumViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val coherence by viewModel.coherence.collectAsStateWithLifecycle()
    val entangledPairs by viewModel.entangledPairs.collectAsStateWithLifecycle()
    val fidelity by viewModel.fidelity.collectAsStateWithLifecycle()
    val metadataName by viewModel.metadataName.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = metadataName.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AVSD 10,000 QUBIT SANDBOX ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Global Sun/Moon Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier
                            .testTag("theme_toggle_button")
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Tukar Tema",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Global Pulse Panel Status Display (Coherence, Entanglement, Fidelity)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PulseStatItem(
                        title = "COHERENCE",
                        value = "${String.format(Locale.US, "%.5f", coherence)}%",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .align(Alignment.CenterVertically)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
                    )
                    PulseStatItem(
                        title = "ENTANGLED PAIRS",
                        value = "$entangledPairs",
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .align(Alignment.CenterVertically)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
                    )
                    PulseStatItem(
                        title = "FIDELITY",
                        value = "${String.format(Locale.US, "%.4f", fidelity)}%",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        bottomBar = {
            androidx.compose.material3.ScrollableTabRow(
                selectedTabIndex = currentTab,
                edgePadding = 8.dp,
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
            ) {
                val tabs = remember {
                    listOf(
                        "Portal" to Icons.Filled.Hub,
                        "Sandbox" to Icons.Filled.Security,
                        "Void" to Icons.Filled.Cyclone,
                        "AI Core" to Icons.Filled.Android,
                        "Topology" to Icons.Filled.Share,
                        "Deep Node" to Icons.Filled.Build,
                        "Auto-Core" to Icons.Filled.AccountBalanceWallet
                    )
                }
                tabs.forEachIndexed { index, pair ->
                    androidx.compose.material3.Tab(
                        selected = currentTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(pair.first, maxLines = 1) },
                        icon = { Icon(pair.second, contentDescription = pair.first) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabContainer"
            ) { targetTab ->
                when (targetTab) {
                    0 -> SovereignPortalTab(viewModel)
                    1 -> SandboxTerminalTab(viewModel)
                    2 -> VoidExtractorTab(viewModel)
                    3 -> IntelligenceAssistantTab(viewModel)
                    4 -> QubitTopologyTab(viewModel)
                    5 -> DeepBillionSandboxTab(viewModel)
                    6 -> NeptuniumCoreTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun RowScope.KeepWith() {}

@Composable
fun PulseStatItem(title: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

// ==========================================
// TAB 0: SOVEREIGN PORTAL (NFT FORGE)
// ==========================================
@Composable
fun SovereignPortalTab(viewModel: QuantumViewModel) {
    var assetName by remember { mutableStateOf("Sovereign Genesis") }
    val forgeStatus by viewModel.forgeStatus.collectAsStateWithLifecycle()
    val forgedNFTs by viewModel.forgedNFTs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SOVEREIGN GENESIS PORTAL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tempa NFT Kuantum tersirat dalam keadaan superposisi murni menggunakan entropi ANU QRNG nyata.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Quantum holographic dynamic wave graphic representation
                    Spacer(modifier = Modifier.height(8.dp))
                    HolographicCoreGraphic()
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = assetName,
                        onValueChange = { assetName = it },
                        label = { Text("Nama Aset Kuantum") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nft_asset_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.forgeSovereignNFT(assetName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("forge_nft_button"),
                        enabled = forgeStatus == ForgeStatus.Idle || forgeStatus is ForgeStatus.Success,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ElectricBolt, "Forge icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TEMPA KANDUNGAN KUANTUM",
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Animated overlay feedback status
        item {
            AnimatedVisibility(
                visible = forgeStatus != ForgeStatus.Idle,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (forgeStatus) {
                            is ForgeStatus.Success -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        when (forgeStatus) {
                            is ForgeStatus.Success -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        when (val status = forgeStatus) {
                            ForgeStatus.FilteringEntropy -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Maxwells Demon: Menyaring entropi...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            ForgeStatus.ForgingOnChain -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "QubitChain: Menambat tandatangan kuantum...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            is ForgeStatus.Success -> {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            "Berjaya",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "ENTANGAN BERJAYA DILAKUKAN!",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "ID: ${status.nft.nftId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Sig: ${status.nft.signature.take(28)}...",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { viewModel.resetForgeStatus() },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Ok, Seterusnya")
                                    }
                                }
                            }
                            is ForgeStatus.Error -> {}
                            ForgeStatus.Idle -> {}
                        }
                    }
                }
            }
        }

        // Live list of Forged Quantum NFTs
        item {
            Text(
                text = "KOLEKSI NFT DAULAT (${forgedNFTs.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (forgedNFTs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Inbox,
                            "Tiada NFT",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tiada Aset Dijana.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Gunakan console di atas untuk menambat NFT Kuantum pertama anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(forgedNFTs) { nft ->
                NFTCardItem(nft)
            }
        }
    }
}

@Composable
fun HolographicCoreGraphic() {
    val infiniteTransition = rememberInfiniteTransition(label = "Core pulse infinity")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse size"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation speed"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .size(140.dp)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.width / 2.3f

            // Clean background dynamic wave circle
            drawCircle(
                color = primaryColor.copy(alpha = 0.08f),
                radius = baseRadius * scale
            )

            // Dynamic rings rotation simulation math
            val pointsRadius = baseRadius * 0.95f
            val radAngle = Math.toRadians(rotation.toDouble())
            val xOffset = (pointsRadius * cos(radAngle) * 0.4f).toFloat()
            val yOffset = (pointsRadius * sin(radAngle) * 0.4f).toFloat()

            // Orbit Ring 1
            drawCircle(
                color = primaryColor,
                radius = baseRadius,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f)
                )
            )

            // Orbit Ring 2 (Tilted Projection)
            drawArc(
                color = secondaryColor,
                startAngle = rotation,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - baseRadius, center.y - baseRadius * 0.6f),
                size = size.copy(height = size.height * 0.6f)
            )

            // Quantum active particle points
            drawCircle(
                color = secondaryColor,
                radius = 6.dp.toPx(),
                center = Offset(center.x + xOffset, center.y + yOffset)
            )
            drawCircle(
                color = primaryColor,
                radius = 4.dp.toPx(),
                center = Offset(center.x - xOffset, center.y - yOffset)
            )

            // Inner core atom
            drawCircle(
                color = primaryColor,
                radius = 16.dp.toPx() * scale
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx()
            )
        }
    }
}

@Composable
fun NFTCardItem(nft: ForgedNFT) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nft_item_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nft.assetName.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ENTANGLED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NFT ID: ${nft.nftId}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Quantum Sig: ${nft.signature.take(20)}...${nft.signature.takeLast(10)}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = nft.entanglement,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = nft.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ==========================================
// TAB 1: SANDBOX TERMINAL
// ==========================================
@Composable
fun SandboxTerminalTab(viewModel: QuantumViewModel) {
    val context = LocalContext.current
    val anomalyChance by viewModel.anomalyChance.collectAsStateWithLifecycle()
    val logs by viewModel.sandboxLogs.collectAsStateWithLifecycle()
    val coirShieldActive by viewModel.coirShieldActive.collectAsStateWithLifecycle()
    val esp32Nodes by viewModel.esp32Nodes.collectAsStateWithLifecycle()

    val metadataName by viewModel.metadataName.collectAsStateWithLifecycle()
    val metadataDescription by viewModel.metadataDescription.collectAsStateWithLifecycle()
    val metadataTools by viewModel.metadataTools.collectAsStateWithLifecycle()
    
    val isRebooting by viewModel.isRebooting.collectAsStateWithLifecycle()
    val rebootStep by viewModel.rebootStep.collectAsStateWithLifecycle()
    val rebootProgress by viewModel.rebootProgress.collectAsStateWithLifecycle()

    var editName by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }

    LaunchedEffect(metadataName, metadataDescription) {
        editName = metadataName
        editDesc = metadataDescription
    }

    var newToolName by remember { mutableStateOf("") }
    var newToolApiName by remember { mutableStateOf("") }
    var newToolDesc by remember { mutableStateOf("") }
    var newToolParamKey by remember { mutableStateOf("") }
    var newToolParamValue by remember { mutableStateOf("") }

    var waveType by remember { mutableStateOf("GHZ Superposition") }
    var waveIntensity by remember { mutableStateOf(0.97f) }

    if (isRebooting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    progress = { rebootProgress },
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    modifier = Modifier.size(88.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "👑 REBOOT IN PROGRESS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "REBOOT PROGRESS: ${(rebootProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "STATUS SISTEM:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rebootStep,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF4DE8D9)
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Maxwell's Demon Filter
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MAXWELL'S DEMON FILTERED FIELD",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Menapis entropi haba digital liar demi mengekalkan kestabilan fasa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MaxwellsDemonFilterGraphic()
                    }
                }
            }

            // Shoreline / Shield Parameters
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🛡️ DIGITAL COIR SHIELD",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = coirShieldActive,
                                onCheckedChange = { viewModel.toggleCoirShield() }
                            )
                        }

                        Text(
                            text = "Garis pinggir pantai digital (living shorelines) yang memampas gangguan anomali dari satah luar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "ANOMALY PRESSURE CHANCE: ${(anomalyChance * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                anomalyChance > 0.85f -> MaterialTheme.colorScheme.error
                                anomalyChance > 0.50f -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )

                        Slider(
                            value = anomalyChance,
                            onValueChange = { viewModel.updateAnomalyChance(it) },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary
                            )
                        )

                        AnimatedVisibility(
                            visible = anomalyChance > 0.50f,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (anomalyChance > 0.85f)
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                        else
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    )
                                    .border(
                                        1.dp,
                                        if (anomalyChance > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = if (anomalyChance > 0.85f)
                                        "🚨 AMARAN KRITIS: Tahap anomali melimpah had! Perisai Coir Logs diaktifkan sepenuhnya."
                                    else
                                        "⚠️ GANGGUAN KECIL DIKESAN: Sempandah digital menyerap kejutan luar.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (anomalyChance > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            // Metadata Configurator
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Metadata",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "METADATA CONFIGURATOR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Konfigurasi teras dashboard ini dikekalkan di dalam metadata.json murni.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("App Name (Metadata)", fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        )

                        OutlinedTextField(
                            value = editDesc,
                            onValueChange = { editDesc = it },
                            label = { Text("App Description (Metadata)", fontFamily = FontFamily.Monospace) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveMetadata(context.filesDir, editName, editDesc, metadataTools)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("SAVE TO METADATA", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = {
                                    viewModel.resetMetadataToDefault(context.filesDir)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("RESTORE DEFAULT", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // Upgraded Metadata Dynamic Tools Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Dynamic Tools",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "METADATA DYNAMIC TOOLS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "Gugusan alat dinamik yang diisytiharkan dalam metadata.json satah kawalan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (metadataTools.isEmpty()) {
                            Text(
                                text = "Tiada alat dinamik ditemui dalam metadata.json.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            metadataTools.forEach { tool ->
                                val customParamMap = remember(tool) {
                                    mutableStateMapOf<String, String>().apply {
                                        tool.parameters.forEach { (k, v) -> put(k, v) }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = tool.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = tool.apiName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 8.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = tool.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        if (tool.parameters.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Parameters:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            tool.parameters.keys.forEach { pKey ->
                                                val pVal = customParamMap[pKey] ?: ""
                                                OutlinedTextField(
                                                    value = pVal,
                                                    onValueChange = { customParamMap[pKey] = it },
                                                    label = { Text("param: $pKey", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = {
                                                viewModel.callToolFromMetadata(
                                                    name = tool.name,
                                                    apiName = tool.apiName,
                                                    params = customParamMap.toMap()
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "RUN '${tool.apiName.uppercase()}'",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "REGISTER NEW TOOL UPGRADE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newToolName,
                            onValueChange = { newToolName = it },
                            label = { Text("New Tool Name", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = newToolApiName,
                            onValueChange = { newToolApiName = it },
                            label = { Text("New API Name", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = newToolDesc,
                            onValueChange = { newToolDesc = it },
                            label = { Text("Description", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = newToolParamKey,
                                onValueChange = { newToolParamKey = it },
                                label = { Text("Param. Key", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newToolParamValue,
                                onValueChange = { newToolParamValue = it },
                                label = { Text("Default Val.", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (newToolName.isNotBlank() && newToolApiName.isNotBlank()) {
                                    val par = if (newToolParamKey.isNotBlank()) {
                                        mapOf(newToolParamKey to newToolParamValue)
                                    } else {
                                        emptyMap()
                                    }
                                    val customT = com.example.viewmodel.MetadataTool(
                                        name = newToolName,
                                        apiName = newToolApiName,
                                        description = newToolDesc,
                                        parameters = par
                                    )
                                    viewModel.addCustomTool(context.filesDir, customT)
                                    newToolName = ""
                                    newToolApiName = ""
                                    newToolDesc = ""
                                    newToolParamKey = ""
                                    newToolParamValue = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("UPGRADE AND REGISTER NOW", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Wave Injection Station
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Web-inspired Custom Wave",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "QUANTUM WAVE INJECTION STATION",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Text(
                            text = "Menembakkan gelombang denyutan quantum ke aras mikrokosmos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "PRESETS GELOMBANG:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf("GHZ Superposition", "Kyber-768 Sync", "Dilithium-3 Matrix")
                            presets.forEach { pr ->
                                val selected = waveType == pr
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { waveType = pr }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pr.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = if (selected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "WAVE INTENSITY FIELD: ${(waveIntensity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = waveIntensity,
                            onValueChange = { waveIntensity = it },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.tertiary,
                                activeTrackColor = MaterialTheme.colorScheme.tertiary
                            )
                        )

                        Button(
                            onClick = {
                                viewModel.triggerQuantumWave(waveType, waveIntensity)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("TRIGGER QUANTUM WAVE SIGNAL", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Nematocyst Rapid Response controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚡ NEMATOCYST RAPID-RESPONSE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Tembak isyarat UDP dengan kelajuan sel penyengat hydra kepada perkakasan ESP32 luar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.nematocystExecute("FLASH") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("nematocyst_flash_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("FLASH", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Button(
                                onClick = { viewModel.nematocystExecute("DEEP SCAN") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("nematocyst_scan_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("SCAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Button(
                                onClick = { viewModel.nematocystExecute("LOCKDOWN") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("nematocyst_lock_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("LOCK", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nod Fizikal Dihubungkan:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            esp32Nodes.forEach { (node, status) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = node,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            color = when (status) {
                                                "STABLE" -> MaterialTheme.colorScheme.secondary
                                                "PROCESSING" -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.tertiary
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CORE HARDWARE COLD BOOT REBOOT
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⛔ CRITICAL SYSTEM REBOOT STATION",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Memula semula keseluruhan nod dua-teras kuantum. Ini akan memutuskan jangkar isyarat satah dwi-fasa sementara waktu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.triggerRebootSystem {
                                    viewModel.addSandboxLog("Cold boot core reboot complete. 10,000 Qubits coherence restored.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("REBOOT SYSTEM COLD BOOT RESTART", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Live Logs Panel representation
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "NEXUS AUDIT LOGS (REALTIME):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(logs) { log ->
                                    Text(
                                        text = log,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF4DE8D9)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaxwellsDemonFilterGraphic() {
    val infiniteTransition = rememberInfiniteTransition(label = "Demon Loop")
    val translation by infiniteTransition.animateFloat(
        initialValue = -35f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "demon translate"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.width / 2
            val midY = size.height / 2

            // Divider membrane representation
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(center, 0f),
                end = Offset(center, size.height),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
            )

            // Particles Left (Cold, Low Entropy) in Cyan
            drawCircle(color = secondaryColor, radius = 5.dp.toPx(), center = Offset(center - 60f + translation * 0.4f, midY - 20f))
            drawCircle(color = secondaryColor, radius = 5.dp.toPx(), center = Offset(center - 90f - translation * 0.3f, midY + 15f))
            drawCircle(color = secondaryColor, radius = 5.dp.toPx(), center = Offset(center - 110f + translation * 0.2f, midY - 10f))

            // Particles Right (Hot, High Entropy) in Purple
            drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = Offset(center + 60f - translation * 0.5f, midY - 15f))
            drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = Offset(center + 100f + translation * 0.4f, midY + 20f))

            // Maxwell's demon gate representing purification logic
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(center + translation * 0.15f, midY)
            )
            drawCircle(
                color = primaryColor,
                radius = 4.dp.toPx(),
                center = Offset(center + translation * 0.15f, midY)
            )
        }
    }
}

// ==========================================
// TAB 2: VOID EXTRACTOR
// ==========================================
@Composable
fun VoidExtractorTab(viewModel: QuantumViewModel) {
    val extractionState by viewModel.voidExtractionStatus.collectAsStateWithLifecycle()
    val logs by viewModel.voidLogs.collectAsStateWithLifecycle()
    val packets by viewModel.voidPackets.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌌 VOID SPECTRUM EXTRACTOR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selamatkan cipta biru fizikal kuantum kuno dan kekayaan orphaned dari satah Void Kelam.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.runVoidExtraction() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("void_extract_button"),
                        enabled = extractionState == "IDLE" || extractionState == "DONE",
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (extractionState == "IDLE" || extractionState == "DONE") "MULAKAN DEEP SCAN" else "EKSTRAKSI AKTIF...",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Extractor state pipeline representation
                    ExtractionProgress(extractionState)
                }
            }
        }

        // Live extraction logs
        if (logs.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "EXTRACTOR ALIGNMENT FLOW:",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        logs.forEach { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Extracted packets
        item {
            Text(
                text = "BARANGAN DISELAMATKAN (${packets.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        if (packets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tiada barangan dijumpai dalam satah Void sedia ada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(packets) { packet ->
                VoidPacketCard(packet)
            }
        }
    }
}

@Composable
fun ExtractionProgress(state: String) {
    val steps = listOf("COOL", "SHIELD", "PING", "DECODE", "COMPLETE")
    val activeIndex = when (state) {
        "COOLING" -> 0
        "SHIELDING" -> 1
        "PINGING" -> 2
        "DECODING" -> 3
        "DONE" -> 4
        else -> -1
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            val isActive = index == activeIndex
            val isCompleted = index < activeIndex || state == "DONE"

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> MaterialTheme.colorScheme.secondary
                                isActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Selesai",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            if (index < activeIndex || state == "DONE")
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
fun VoidPacketCard(packet: VoidPacket) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = packet.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (packet.type) {
                                "BLUEPRINT" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                "WEALTH" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = packet.type,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = when (packet.type) {
                            "BLUEPRINT" -> MaterialTheme.colorScheme.primary
                            "WEALTH" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = packet.data,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PUNCAK SALURAN: ${packet.origin}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ==========================================
// TAB 3: QUANTUM AI ASSISTANT
// ==========================================
@Composable
fun IntelligenceAssistantTab(viewModel: QuantumViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    var promptInput by remember { mutableStateOf("") }
    val isKeyConfigured = remember { GeminiClient.isKeyConfigured() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isKeyConfigured) Icons.Filled.CloudQueue else Icons.Filled.WifiOff,
                        contentDescription = "Status Status",
                        tint = if (isKeyConfigured) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isKeyConfigured) "TERAS AI: CLOUD NETWORK DIRECT CONNECTED" else "TERAS AI: SIMULASI QUBIT LOCAL AKTIF",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isKeyConfigured) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }

                if (!isKeyConfigured) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "*Nota: SDK Kunci API Gemini tiada dalam Secrets dashboard. Melancarkan simulasi fikiran SOV-101 secara autonomi.",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat bubble conversations
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            if (chatHistory.isEmpty()) {
                Column(
                    modifier = Modifier.matchParentSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = "AI fikiran",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sovereign Quantum AI \"SOV-101\" Bersedia",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Hantar soalan atau berikan arahan kuantum.\nContoh: \"Formula pabrik baja N2 + qubit\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatHistory) { message ->
                        ChatBubble(message)
                    }
                    if (isAiLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat input bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Tulis respons...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            IconButton(
                onClick = {
                    viewModel.sendChatMessage(promptInput)
                    promptInput = ""
                },
                modifier = Modifier
                    .testTag("send_chat_button")
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Hantar isyarat",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .background(
                        if (isUser)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = if (isUser) FontFamily.Default else FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
            Text(
                text = "${if (isUser) "Operator" else "SOV-101"} • ${message.timestamp}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
fun CrashRecoveryScreen(stacktrace: String, onClear: () -> Unit) {
    var isCleared by remember { mutableStateOf(false) }

    if (isCleared) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val activity = generateSequence(context) { if (it is android.content.ContextWrapper) it.baseContext else null }
                .firstOrNull { it is android.app.Activity } as? android.app.Activity
            activity?.let {
                it.startActivity(android.content.Intent(it, MainActivity::class.java))
                it.finish()
            }
        }
    } else {
        Scaffold(
            topBar = {
                Text(
                    text = "🚨 SOV-101 DECOHERENCE VENT",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sistem mengesan runtuhan gelombang koheren (Crash). Maklumat debug sedia dikesan semula daripada memori flash:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Box(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = stacktrace.take(3000), // Prevent very long strings crashing Text layout
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        )
                    }
                }

                Button(
                    onClick = {
                        onClear()
                        isCleared = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "BERSIHKAN LOG & SAMBUNG SEMULA",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
