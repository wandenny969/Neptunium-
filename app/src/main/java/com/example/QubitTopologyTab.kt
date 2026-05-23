package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.QuantumViewModel
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

data class QubitNode(
    var position: Offset,
    var velocity: Offset,
    var phase: Float,
    var baseRadius: Float
)

@Composable
fun QubitTopologyTab(viewModel: QuantumViewModel) {
    val coherence by viewModel.coherence.collectAsStateWithLifecycle()
    val fidelity by viewModel.fidelity.collectAsStateWithLifecycle()
    val entangledPairs by viewModel.entangledPairs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "QUANTUM TOPOLOGY MATRIX",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Live monitoring of 10,000 qubit entanglement mesh.",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            QubitVisualizer(
                coherence = coherence,
                fidelity = fidelity,
                entangledCount = entangledPairs
            )
        }
    }
}

@Composable
fun QubitVisualizer(coherence: Float, fidelity: Float, entangledCount: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
    val nodeCount = 60 // Visual representation of 10k
    
    // Initialize nodes
    val nodes = remember {
        List(nodeCount) {
            QubitNode(
                position = Offset(Random.nextFloat(), Random.nextFloat()), // normalized 0..1
                velocity = Offset(
                    (Random.nextFloat() - 0.5f) * 0.005f,
                    (Random.nextFloat() - 0.5f) * 0.005f
                ),
                phase = Random.nextFloat() * 100f,
                baseRadius = Random.nextFloat() * 4f + 2f
            )
        }
    }
    
    // Tick engine for animation
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // ~60 FPS
            tick++
            
            // Speed relates to coherence (lower coherence = faster jitter)
            val jitter = if (coherence < 99f) 0.002f else 0.0005f
            
            nodes.forEach { node ->
                node.position += node.velocity
                // Add jitter
                node.position += Offset(
                    (Random.nextFloat() - 0.5f) * jitter,
                    (Random.nextFloat() - 0.5f) * jitter
                )
                
                // Bounce off edges (0..1 normalized space)
                if (node.position.x < 0f || node.position.x > 1f) {
                    node.velocity = node.velocity.copy(x = -node.velocity.x)
                    node.position = node.position.copy(x = node.position.x.coerceIn(0f, 1f))
                }
                if (node.position.y < 0f || node.position.y > 1f) {
                    node.velocity = node.velocity.copy(y = -node.velocity.y)
                    node.position = node.position.copy(y = node.position.y.coerceIn(0f, 1f))
                }
                
                node.phase += 0.05f
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        // Use tick to force redraw
        val currentTick = tick
        
        // Map normalized coordinates to screen size
        val mappedNodes = nodes.map { node ->
            Offset(node.position.x * w, node.position.y * h)
        }
        
        // Draw entanglement lines
        val distanceThreshold = (w + h) / 8f
        
        for (i in 0 until nodeCount) {
            for (j in i + 1 until nodeCount) {
                val dx = mappedNodes[i].x - mappedNodes[j].x
                val dy = mappedNodes[i].y - mappedNodes[j].y
                val distSq = dx * dx + dy * dy
                val threshSq = distanceThreshold * distanceThreshold
                
                if (distSq < threshSq) {
                    val dist = sqrt(distSq)
                    val alpha = (1f - (dist / distanceThreshold)).coerceIn(0f, 1f)
                    
                    val lineColor = if (fidelity > 99.99f) secondaryColor else tertiaryColor
                    
                    drawLine(
                        color = lineColor.copy(alpha = alpha * 0.6f),
                        start = mappedNodes[i],
                        end = mappedNodes[j],
                        strokeWidth = ((2f * alpha) - 0.5f).coerceAtLeast(0.1f),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        
        // Draw qubits
        nodes.forEachIndexed { index, node ->
            val mappedPos = mappedNodes[index]
            val pulsingRadius = node.baseRadius + kotlin.math.sin(node.phase + currentTick * 0.1f) * 2f
            
            // Qubit core
            drawCircle(
                color = primaryColor,
                radius = pulsingRadius.coerceAtLeast(1f),
                center = mappedPos
            )
            // Qubit halo
            drawCircle(
                color = secondaryColor.copy(alpha = 0.3f),
                radius = pulsingRadius * 2f,
                center = mappedPos
            )
        }
    }
}
