package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Stylish Animated Opening Splash Screen for Park a Lot.
 * Displays every time the app opens with futuristic neon EV geometry,
 * expanding energy rings, and smooth scale/alpha choreography.
 */
@Composable
fun ParkALotAnimatedSplash(
    onAnimationFinished: () -> Unit
) {
    val scaleAnim = remember { Animatable(0.4f) }
    val alphaAnim = remember { Animatable(0f) }
    val textAlphaAnim = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(Unit) {
        // Step 1: Smooth spring-like scale & fade in
        launch {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 600)
            )
        }
        delay(350)
        launch {
            textAlphaAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 600)
            )
        }
        launch {
            progressAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
            )
        }

        // Hold and complete after 2.1 seconds
        delay(1900)
        alphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350)
        )
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F1B2E),
                        Color(0xFF090D16),
                        Color(0xFF04060A)
                    ),
                    radius = 1200f
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // User can tap anywhere to immediately jump to the app
                onAnimationFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        // Decorative ambient glow backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f - 40.dp.toPx())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.12f), Color.Transparent),
                    center = center,
                    radius = 240.dp.toPx()
                ),
                center = center,
                radius = 240.dp.toPx()
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .alpha(alphaAnim.value)
        ) {
            // Stylish Glowing Animated Logo Mark
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .scale(scaleAnim.value)
            ) {
                // Outer Pulse Waves (Radar Energy Waves)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = pulseAlpha * 0.5f),
                        radius = (size.width / 2.2f) * pulseScale,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = pulseAlpha * 0.35f),
                        radius = (size.width / 2.6f) * (pulseScale * 1.15f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Geometric Neon Emblem
                Canvas(modifier = Modifier.size(130.dp)) {
                    val w = size.width
                    val h = size.height
                    val primaryColor = Color(0xFF00E5FF)
                    val secondaryColor = Color(0xFF00E676)

                    // Rounded Octagon/Shield Background
                    val shieldPath = Path().apply {
                        val corner = 22.dp.toPx()
                        moveTo(w * 0.5f, 0f)
                        lineTo(w - corner, 0f)
                        quadraticTo(w, 0f, w, corner)
                        lineTo(w, h * 0.65f)
                        lineTo(w * 0.5f, h)
                        lineTo(0f, h * 0.65f)
                        lineTo(0f, corner)
                        quadraticTo(0f, 0f, corner, 0f)
                        close()
                    }

                    // Fill shield with dark luxury gradient
                    drawPath(
                        path = shieldPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF102038), Color(0xFF0A1220)),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )

                    // Neon outline stroke
                    drawPath(
                        path = shieldPath,
                        brush = Brush.linearGradient(
                            colors = listOf(primaryColor, secondaryColor, primaryColor),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        ),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Stylized "P" Letter Geometry + EV Energy Accent
                    val pStemX = w * 0.36f
                    val pTopY = h * 0.22f
                    val pBottomY = h * 0.76f

                    // Vertical Stem
                    drawLine(
                        brush = Brush.verticalGradient(listOf(Color.White, primaryColor)),
                        start = Offset(pStemX, pTopY),
                        end = Offset(pStemX, pBottomY),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // "P" Head Loop
                    val pHeadPath = Path().apply {
                        moveTo(pStemX, pTopY)
                        lineTo(w * 0.60f, pTopY)
                        cubicTo(
                            w * 0.78f, pTopY,
                            w * 0.78f, h * 0.50f,
                            w * 0.60f, h * 0.50f
                        )
                        lineTo(pStemX, h * 0.50f)
                    }
                    drawPath(
                        path = pHeadPath,
                        brush = Brush.horizontalGradient(listOf(Color.White, primaryColor)),
                        style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Integrated Electric Lightning Bolt in Neon Lime
                    val boltPath = Path().apply {
                        moveTo(w * 0.68f, h * 0.40f)
                        lineTo(w * 0.58f, h * 0.62f)
                        lineTo(w * 0.65f, h * 0.62f)
                        lineTo(w * 0.54f, h * 0.86f)
                        lineTo(w * 0.72f, h * 0.58f)
                        lineTo(w * 0.64f, h * 0.58f)
                        close()
                    }
                    drawPath(
                        path = boltPath,
                        brush = Brush.verticalGradient(listOf(Color(0xFF76FF03), Color(0xFF00E676)))
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Animated Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlphaAnim.value)
            ) {
                Text(
                    text = "Park a Lot",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SMART EV & WORKPLACE PARKING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color(0xFF00E5FF).copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(42.dp))

            // Sleek Loading Progress Runner
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .alpha(textAlphaAnim.value)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim.value)
                        .height(4.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF00E676))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap to enter",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.alpha(textAlphaAnim.value)
            )
        }
    }
}

/**
 * Compact, stylish Park a Lot monogram icon for app headers and cards.
 */
@Composable
fun ParkALotMiniLogo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 32.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val primaryColor = Color(0xFF00E5FF)
            val secondaryColor = Color(0xFF00E676)

            val shieldPath = Path().apply {
                val corner = 5.dp.toPx()
                moveTo(w * 0.5f, 0f)
                lineTo(w - corner, 0f)
                quadraticTo(w, 0f, w, corner)
                lineTo(w, h * 0.65f)
                lineTo(w * 0.5f, h)
                lineTo(0f, h * 0.65f)
                lineTo(0f, corner)
                quadraticTo(0f, 0f, corner, 0f)
                close()
            }

            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0D1B2E), Color(0xFF070D18)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Stroke(width = 1.5.dp.toPx())
            )

            val pStemX = w * 0.36f
            drawLine(
                color = Color.White,
                start = Offset(pStemX, h * 0.22f),
                end = Offset(pStemX, h * 0.78f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            val pHeadPath = Path().apply {
                moveTo(pStemX, h * 0.22f)
                lineTo(w * 0.60f, h * 0.22f)
                cubicTo(w * 0.75f, h * 0.22f, w * 0.75f, h * 0.50f, w * 0.60f, h * 0.50f)
                lineTo(pStemX, h * 0.50f)
            }
            drawPath(
                path = pHeadPath,
                color = Color.White,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Lightning Bolt Accent
            val boltPath = Path().apply {
                moveTo(w * 0.66f, h * 0.42f)
                lineTo(w * 0.58f, h * 0.64f)
                lineTo(w * 0.64f, h * 0.64f)
                lineTo(w * 0.54f, h * 0.85f)
                lineTo(w * 0.70f, h * 0.60f)
                lineTo(w * 0.64f, h * 0.60f)
                close()
            }
            drawPath(
                path = boltPath,
                color = secondaryColor
            )
        }
    }
}
