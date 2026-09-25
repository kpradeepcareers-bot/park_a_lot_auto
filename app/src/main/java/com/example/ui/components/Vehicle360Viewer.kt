package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.entity.VehicleEntity
import com.example.ui.theme.JarvisArcReactorCyan
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Vehicle360Viewer(
    vehicle: VehicleEntity,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onColorSelected: (String) -> Unit = {}
) {
    var rotationAngle by remember { mutableFloatStateOf(45f) }
    var isAutoSpinning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val colorsList = remember(vehicle.model) {
        listOf(
            "Celestial Blue" to Color(0xFF1E88E5),
            "Firestorm Orange" to Color(0xFFFF6D00),
            "Empowered Oxide" to Color(0xFF546E7A),
            "Intensi-Teal" to Color(0xFF00897B),
            "Stealth Black" to Color(0xFF212121),
            "Arctic White" to Color(0xFFECEFF1)
        )
    }

    var activeColorName by remember(vehicle.color) {
        mutableStateOf(vehicle.color ?: "Celestial Blue")
    }

    val carPaintColor = colorsList.find { it.first == activeColorName }?.second ?: primaryColor

    // Auto-spin logic
    LaunchedEffect(isAutoSpinning) {
        if (isAutoSpinning) {
            while (true) {
                rotationAngle = (rotationAngle + 1.5f) % 360f
                kotlinx.coroutines.delay(25)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title & Auto-spin toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "360 View",
                        tint = JarvisArcReactorCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "360° Studio Showcase",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = { isAutoSpinning = !isAutoSpinning }) {
                    Icon(
                        imageVector = if (isAutoSpinning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isAutoSpinning) "Pause Rotation" else "Auto Rotate",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive 360 Canvas View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            isAutoSpinning = false
                            rotationAngle = (rotationAngle - dragAmount.x * 0.7f + 360f) % 360f
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    drawVehicle360(
                        angleDeg = rotationAngle,
                        paintColor = carPaintColor,
                        isBike = vehicle.type.equals("Bike", ignoreCase = true)
                    )
                }

                // Angle HUD Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shadowElevation = 2.dp
                ) {
                    val angleText = when ((rotationAngle / 45f).toInt() % 8) {
                        0 -> "0° Front"
                        1 -> "45° Front-Right"
                        2 -> "90° Right Profile"
                        3 -> "135° Rear-Right"
                        4 -> "180° Rear"
                        5 -> "225° Rear-Left"
                        6 -> "270° Left Profile"
                        else -> "315° Front-Left"
                    }
                    Text(
                        text = angleText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Drag hint
                Text(
                    text = "Drag to rotate 360°",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exterior Color Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exterior Paint: $activeColorName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorsList.forEach { (name, color) ->
                        val isSelected = name == activeColorName
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .pointerInput(Unit) {
                                    detectDragGestures { _, _ -> }
                                }
                        ) {
                            IconButton(
                                onClick = {
                                    activeColorName = name
                                    onColorSelected(name)
                                },
                                modifier = Modifier.matchParentSize()
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawVehicle360(
    angleDeg: Float,
    paintColor: Color,
    isBike: Boolean
) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f + 10f
    val rad = Math.toRadians(angleDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    // 1. Draw glowing floor shadow & turntable rings
    val shadowWidth = size.width * 0.65f
    val shadowHeight = 36.dp.toPx()
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x33000000), Color(0x05000000), Color.Transparent),
            center = Offset(centerX, centerY + 42.dp.toPx()),
            radius = shadowWidth / 2f
        ),
        topLeft = Offset(centerX - shadowWidth / 2f, centerY + 28.dp.toPx()),
        size = Size(shadowWidth, shadowHeight)
    )

    // Dynamic rotation turntable rings
    drawOval(
        color = Color(0x2200E5FF),
        topLeft = Offset(centerX - shadowWidth / 2f, centerY + 28.dp.toPx()),
        size = Size(shadowWidth, shadowHeight),
        style = Stroke(width = 1.5f)
    )

    if (isBike) {
        drawBike360(centerX, centerY, cosA, sinA, paintColor)
    } else {
        drawCar360(centerX, centerY, cosA, sinA, paintColor)
    }
}

private fun DrawScope.drawCar360(
    centerX: Float,
    centerY: Float,
    cosA: Float,
    sinA: Float,
    paintColor: Color
) {
    val carLength = 160.dp.toPx()
    val carWidth = 80.dp.toPx()
    val carHeight = 50.dp.toPx()

    // Projected perspective parameters
    val projScaleX = cosA
    val projDepthY = sinA * 0.35f

    val bodyOffset = Offset(centerX, centerY)

    // Wheels based on angle
    val wheelColor = Color(0xFF1A1A1A)
    val rimColor = Color(0xFFB0BEC5)

    val wheelRadius = 14.dp.toPx()
    val wheelY = centerY + carHeight * 0.45f

    // Draw rear/front wheels depending on view
    val leftWheelX = centerX - (carLength * 0.35f * projScaleX)
    val rightWheelX = centerX + (carLength * 0.35f * projScaleX)

    drawCircle(
        color = wheelColor,
        radius = wheelRadius,
        center = Offset(leftWheelX, wheelY)
    )
    drawCircle(
        color = rimColor,
        radius = wheelRadius * 0.55f,
        center = Offset(leftWheelX, wheelY)
    )

    drawCircle(
        color = wheelColor,
        radius = wheelRadius,
        center = Offset(rightWheelX, wheelY)
    )
    drawCircle(
        color = rimColor,
        radius = wheelRadius * 0.55f,
        center = Offset(rightWheelX, wheelY)
    )

    // Car Body: Aerodynamic SUV / Coupe silhouette
    val bodyPath = Path().apply {
        // Lower chassis
        val chassisLeft = centerX - (carLength * 0.48f * cosA.coerceIn(-1f, 1f).coerceAtLeast(0.5f))
        val chassisRight = centerX + (carLength * 0.48f * cosA.coerceIn(-1f, 1f).coerceAtLeast(0.5f))
        val chassisTop = centerY - carHeight * 0.3f
        val chassisBottom = centerY + carHeight * 0.4f

        moveTo(chassisLeft, chassisBottom)
        lineTo(chassisRight, chassisBottom)
        lineTo(chassisRight - 10.dp.toPx(), chassisTop)
        // Windshield and roof
        lineTo(centerX + 20.dp.toPx() * projScaleX, centerY - carHeight * 0.85f)
        lineTo(centerX - 35.dp.toPx() * projScaleX, centerY - carHeight * 0.85f)
        lineTo(chassisLeft + 15.dp.toPx(), chassisTop)
        close()
    }

    // Paint shading with angle-reactive highlight
    val highlightShift = (cosA + 1f) / 2f
    val carBrush = Brush.linearGradient(
        colors = listOf(
            paintColor.copy(alpha = 0.85f),
            paintColor,
            Color.White.copy(alpha = 0.4f),
            paintColor.copy(alpha = 0.95f),
            paintColor.copy(alpha = 0.7f)
        ),
        start = Offset(centerX - carLength * highlightShift, centerY - carHeight),
        end = Offset(centerX + carLength * (1f - highlightShift), centerY + carHeight)
    )

    drawPath(path = bodyPath, brush = carBrush)
    drawPath(path = bodyPath, color = paintColor.copy(alpha = 0.5f), style = Stroke(width = 2f))

    // Windshield & Windows (Tinted glass)
    val windowPath = Path().apply {
        moveTo(centerX - 25.dp.toPx() * projScaleX, centerY - carHeight * 0.78f)
        lineTo(centerX + 15.dp.toPx() * projScaleX, centerY - carHeight * 0.78f)
        lineTo(centerX + 28.dp.toPx() * projScaleX, centerY - carHeight * 0.35f)
        lineTo(centerX - 38.dp.toPx() * projScaleX, centerY - carHeight * 0.35f)
        close()
    }
    drawPath(
        path = windowPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF263238), Color(0xFF455A64))
        )
    )

    // Dynamic Headlights (Front is 0 deg -> cosA > 0, sinA ~ 0)
    if (sinA < 0.3f) {
        // Front Headlights Glowing Cyan/LED
        val headlightColor = Color(0xFFE0F7FA)
        drawCircle(
            color = headlightColor,
            radius = 6.dp.toPx(),
            center = Offset(centerX - carLength * 0.44f * cosA.coerceAtLeast(0.2f), centerY)
        )
        // LED Beam cone
        drawCircle(
            color = Color(0x6600E5FF),
            radius = 12.dp.toPx(),
            center = Offset(centerX - carLength * 0.44f * cosA.coerceAtLeast(0.2f), centerY)
        )
    }

    // Taillights (Rear is 180 deg)
    if (sinA > -0.3f) {
        val tailLightColor = Color(0xFFFF1744)
        drawCircle(
            color = tailLightColor,
            radius = 5.dp.toPx(),
            center = Offset(centerX + carLength * 0.44f * cosA.coerceAtLeast(0.2f), centerY)
        )
    }
}

private fun DrawScope.drawBike360(
    centerX: Float,
    centerY: Float,
    cosA: Float,
    sinA: Float,
    paintColor: Color
) {
    val bikeLength = 120.dp.toPx()
    val bikeHeight = 60.dp.toPx()
    val wheelRadius = 16.dp.toPx()
    val wheelY = centerY + bikeHeight * 0.35f

    val leftWheelX = centerX - bikeLength * 0.35f * cosA.coerceIn(-1f, 1f).coerceAtLeast(0.4f)
    val rightWheelX = centerX + bikeLength * 0.35f * cosA.coerceIn(-1f, 1f).coerceAtLeast(0.4f)

    // Wheels & Discs
    drawCircle(color = Color(0xFF212121), radius = wheelRadius, center = Offset(leftWheelX, wheelY))
    drawCircle(color = Color(0xFFB0BEC5), radius = wheelRadius * 0.45f, center = Offset(leftWheelX, wheelY))
    drawCircle(color = Color(0xFF212121), radius = wheelRadius, center = Offset(rightWheelX, wheelY))
    drawCircle(color = Color(0xFFB0BEC5), radius = wheelRadius * 0.45f, center = Offset(rightWheelX, wheelY))

    // Chassis & Seat
    val bikePath = Path().apply {
        moveTo(leftWheelX, wheelY)
        lineTo(centerX - 10.dp.toPx(), centerY - 15.dp.toPx())
        lineTo(centerX + 15.dp.toPx(), centerY - 18.dp.toPx())
        lineTo(rightWheelX - 5.dp.toPx(), centerY - 32.dp.toPx()) // Handlebar
        lineTo(rightWheelX, wheelY)
        lineTo(centerX, centerY + 10.dp.toPx())
        close()
    }
    drawPath(path = bikePath, color = paintColor)
    drawPath(path = bikePath, color = Color.Black.copy(alpha = 0.2f), style = Stroke(width = 2f))
}
