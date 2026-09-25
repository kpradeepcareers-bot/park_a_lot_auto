package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisArcReactorCyan
import com.example.ui.theme.JarvisGold
import com.example.voice.AssistantState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisWaveformOverlay(
    visible: Boolean,
    state: AssistantState,
    spokenText: String,
    responseText: String,
    audioLevel: Float, // 0f to 1f
    onDismiss: () -> Unit,
    onMicTap: () -> Unit,
    onReplayVoice: () -> Unit = {},
    onCommandSelected: (String) -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6050C1A))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0A1526)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = JarvisArcReactorCyan.copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Charles AI brand & dismiss
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = JarvisArcReactorCyan.copy(alpha = 0.15f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = JarvisArcReactorCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CHARLES // AI ASSISTANT",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = JarvisArcReactorCyan
                                )
                                Text(
                                    text = when (state) {
                                        AssistantState.LISTENING -> "LIVE // LISTENING FOR COMMAND..."
                                        AssistantState.PROCESSING -> "PROCESSING QUERY..."
                                        AssistantState.SPEAKING -> "SPEAKING (MALE VOICE)..."
                                        AssistantState.IDLE -> "STANDBY // SAY \"CHARLES\""
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = JarvisGold
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Arc Reactor Holographic HUD
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ArcReactorCanvas(state = state, audioLevel = audioLevel)

                        // Central Mic or Speaker Icon
                        Surface(
                            shape = CircleShape,
                            color = when (state) {
                                AssistantState.SPEAKING -> JarvisGold.copy(alpha = 0.25f)
                                AssistantState.LISTENING -> JarvisArcReactorCyan.copy(alpha = 0.25f)
                                else -> Color(0xFF132238)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (state == AssistantState.SPEAKING) JarvisGold else JarvisArcReactorCyan
                            ),
                            modifier = Modifier
                                .size(64.dp)
                                .clickable { onMicTap() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state == AssistantState.SPEAKING) Icons.Default.VolumeUp else Icons.Default.Mic,
                                    contentDescription = "Microphone",
                                    tint = if (state == AssistantState.SPEAKING) JarvisGold else JarvisArcReactorCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Spoken user input
                    if (spokenText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF101F33),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "“$spokenText”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Charles response with voice replay option
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F1E33),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F0FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = responseText,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = Color(0xFFCFD8DC),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(JarvisArcReactorCyan.copy(alpha = 0.12f))
                                    .clickable { onReplayVoice() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Replay Male Voice",
                                    tint = JarvisArcReactorCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Replay Voice (Male)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = JarvisArcReactorCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Quick Command Chips
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickChip(label = "Where is my car?", modifier = Modifier.weight(1f)) {
                                onCommandSelected("where is my car")
                            }
                            QuickChip(label = "Status report", modifier = Modifier.weight(1f)) {
                                onCommandSelected("status report")
                            }
                            QuickChip(label = "Park vehicle", modifier = Modifier.weight(1f)) {
                                onCommandSelected("park vehicle")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickChip(label = "Office bays", modifier = Modifier.weight(1f)) {
                                onCommandSelected("office parking")
                            }
                            QuickChip(label = "360 inspection", modifier = Modifier.weight(1f)) {
                                onCommandSelected("360 view")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Alexa-style wakeword: Charles only listens when you say “Charles”. You can also tap the center mic to ask immediately.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFF90A4AE),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF132238),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F0FF)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = JarvisArcReactorCyan,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ArcReactorCanvas(state: AssistantState, audioLevel: Float) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(state) {
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f - 4.dp.toPx()

        // Outer glow circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x3300F0FF),
                    Color(0x0500F0FF),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius
            )
        )

        // Outer Arc Reactor Ring with segments
        drawCircle(
            color = JarvisArcReactorCyan.copy(alpha = 0.4f),
            radius = maxRadius,
            style = Stroke(width = 2f)
        )

        // Radial ticks
        val numTicks = 24
        val tickLength = 8.dp.toPx() + (audioLevel * 14.dp.toPx())
        val angleStep = (2 * PI / numTicks).toFloat()
        val currentRotRad = Math.toRadians(rotation.value.toDouble()).toFloat()

        for (i in 0 until numTicks) {
            val angle = i * angleStep + currentRotRad
            val innerR = maxRadius - tickLength
            val start = Offset(center.x + innerR * cos(angle), center.y + innerR * sin(angle))
            val end = Offset(center.x + maxRadius * cos(angle), center.y + maxRadius * sin(angle))
            val tickColor = if (i % 3 == 0) JarvisGold else JarvisArcReactorCyan
            drawLine(
                color = tickColor.copy(alpha = 0.7f),
                start = start,
                end = end,
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }

        // Inner pulsing ring
        val innerPulseRadius = (maxRadius * 0.65f) + (audioLevel * 12.dp.toPx())
        drawCircle(
            color = JarvisArcReactorCyan.copy(alpha = 0.6f + (audioLevel * 0.4f)),
            radius = innerPulseRadius,
            style = Stroke(width = 2.5f)
        )
    }
}
