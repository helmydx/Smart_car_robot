@file:Suppress("SpellCheckingInspection")
package com.example.robotikm1lkt

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.robotikm1lkt.ui.theme.ROBOTIKM1LKTTheme
import kotlin.math.roundToInt

// Logo Colors
val LogoGreen = Color(0xFF7ED956)
val LogoBlue = Color(0xFF065A86)
val DarkBlueBg = Color(0xFF021420)
val DarkBlueGradient = listOf(
    Color(0xFF021420), // Very dark navy blue
    Color(0xFF05344E), // Mid navy blue base on LogoBlue
    Color(0xFF021420)  // Very dark navy blue
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Lock screen orientation to Landscape
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // Start the background pinging to monitor ESP8266 connection
        RobotManager.startMonitoring(lifecycleScope)
        
        enableEdgeToEdge()
        setContent {
            ROBOTIKM1LKTTheme {
                var showSplash by remember { mutableStateOf(true) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(DarkBlueGradient))
                            .padding(innerPadding)
                    ) {
                        ControlDashboard()

                        // Splash Screen Overlay
                        AnimatedVisibility(
                            visible = showSplash,
                            enter = fadeIn(),
                            exit = fadeOut(animationSpec = tween(800))
                        ) {
                            SplashScreen(onTimeout = { showSplash = false })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2200)
        onTimeout()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF021420),
                        Color(0xFF065A86),
                        Color(0xFF010A10)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.robotik),
                contentDescription = "Logo Robotik",
                modifier = Modifier
                    .size(170.dp)
                    .scale(scale)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "ROBOTIK MAN 1 LANGKAT",
                color = LogoGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                color = LogoGreen,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ControlDashboard() {
    val isConnected by RobotManager.isConnected.collectAsState()
    val isPreview = LocalInspectionMode.current

    // Motion Control States (with sliding gesture state tracking)
    var isForwardPressed by remember { mutableStateOf(false) }
    var isBackwardPressed by remember { mutableStateOf(false) }
    var isLeftPressed by remember { mutableStateOf(false) }
    var isRightPressed by remember { mutableStateOf(false) }

    // Combined command logic
    LaunchedEffect(isForwardPressed, isBackwardPressed, isLeftPressed, isRightPressed) {
        if (isPreview) return@LaunchedEffect
        val command = when {
            isForwardPressed && isRightPressed -> "I"  // Maju + Kanan (Ahead Right)
            isForwardPressed && isLeftPressed -> "G"   // Maju + Kiri (Ahead Left)
            isBackwardPressed && isRightPressed -> "J" // Mundur + Kanan (Back Right)
            isBackwardPressed && isLeftPressed -> "H"  // Mundur + Kiri (Back Left)
            isForwardPressed -> "F"                    // Maju
            isBackwardPressed -> "B"                   // Mundur
            isLeftPressed -> "L"                       // Kiri (Spin Left)
            isRightPressed -> "R"                      // Kanan (Spin Right)
            else -> "S"                                // Stop
        }
        RobotManager.sendCommand(command)
    }

    // Speed values corresponding to levels 0..9 in Arduino
    val speedLevels = remember { intArrayOf(60, 90, 110, 130, 150, 170, 190, 210, 230, 250) }
    var speedIndex by remember { mutableFloatStateOf(3f) } // Default is level 3 (speedCar = 130)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- CONTROLS ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 1. LEFT COLUMN: STATUS CARD & VERTICAL DIRECTIONAL CONTROLS ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.28f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Connection Indicator Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x15065A86)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .border(1.dp, Color(0x22065A86), RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 0.85f,
                                targetValue = 1.25f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(if (isConnected) LogoGreen.copy(alpha = 0.35f) else Color(0x55FF1744))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) LogoGreen else Color(0xFFFF1744))
                                )
                            }

                            Text(
                                text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                                color = if (isConnected) LogoGreen else Color(0xFFFF1744),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Vertical Sliding controls (MAJU & MUNDUR)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val activeChanges = event.changes.filter { it.pressed }
                                    if (activeChanges.isEmpty()) {
                                        isForwardPressed = false
                                        isBackwardPressed = false
                                    } else {
                                        val change = activeChanges.first()
                                        val y = change.position.y
                                        val height = size.height
                                        val padding = height * 0.25f
                                        if (y >= -padding && y <= height + padding) {
                                            if (y < height / 2f) {
                                                isForwardPressed = true
                                                isBackwardPressed = false
                                            } else {
                                                isForwardPressed = false
                                                isBackwardPressed = true
                                            }
                                        } else {
                                            isForwardPressed = false
                                            isBackwardPressed = false
                                        }
                                        activeChanges.forEach { it.consume() }
                                    }
                                }
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FuturisticButton(
                        text = "MAJU",
                        icon = Icons.Default.KeyboardArrowUp,
                        color = LogoGreen,
                        isPressed = isForwardPressed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    FuturisticButton(
                        text = "MUNDUR",
                        icon = Icons.Default.KeyboardArrowDown,
                        color = LogoGreen,
                        isPressed = isBackwardPressed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            // --- 2. CENTER COLUMN: LOGO & SPEED CONTROL ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.44f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Logo in the middle to top of the page
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.robotik),
                        contentDescription = "Logo Robotik",
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.9f)
                            .padding(vertical = 4.dp)
                    )
                }

                // Speed Control Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1F065A86)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .border(1.dp, Color(0x22065A86), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val currentSpeedVal = speedLevels[speedIndex.roundToInt()]
                        val currentSpeedPercent = ((currentSpeedVal / 255f) * 100).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KECEPATAN UTAMA",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$currentSpeedVal ($currentSpeedPercent%)",
                                color = LogoGreen,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Slider(
                            value = speedIndex,
                            onValueChange = {
                                speedIndex = it
                                if (!isPreview) {
                                    RobotManager.sendSpeedIndex(it.roundToInt())
                                }
                            },
                            valueRange = 0f..9f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = LogoGreen,
                                activeTrackColor = LogoGreen,
                                inactiveTrackColor = Color(0x22FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // --- 3. RIGHT COLUMN: BRAND/IP CARD & HORIZONTAL DIRECTIONAL CONTROLS ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.28f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand and IP Target Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x15065A86)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .border(1.dp, Color(0x22065A86), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ROBOTIK MAN 1 LANGKAT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "IP Target: 192.168.4.1",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Horizontal Sliding Controls (KIRI & KANAN)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val activeChanges = event.changes.filter { it.pressed }
                                    if (activeChanges.isEmpty()) {
                                        isLeftPressed = false
                                        isRightPressed = false
                                    } else {
                                        val change = activeChanges.first()
                                        val x = change.position.x
                                        val width = size.width
                                        val padding = width * 0.25f
                                        if (x >= -padding && x <= width + padding) {
                                            if (x < width / 2f) {
                                                isLeftPressed = true
                                                isRightPressed = false
                                            } else {
                                                isLeftPressed = false
                                                isRightPressed = true
                                            }
                                        } else {
                                            isLeftPressed = false
                                            isRightPressed = false
                                        }
                                        activeChanges.forEach { it.consume() }
                                    }
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FuturisticButton(
                        text = "KIRI",
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        color = Color(0xFF29B6F6),
                        isPressed = isLeftPressed,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    FuturisticButton(
                        text = "KANAN",
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        color = Color(0xFF29B6F6),
                        isPressed = isRightPressed,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun FuturisticButton(
    text: String,
    icon: ImageVector,
    color: Color,
    isPressed: Boolean,
    modifier: Modifier = Modifier
) {
    // Smooth press scaling and border/background transitions
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.93f else 1f, label = "press_scale")
    val buttonBg = if (isPressed) color.copy(alpha = 0.25f) else Color(0x0A065A86)
    val borderAlpha = if (isPressed) 1f else 0.3f

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(buttonBg)
            .border(5.dp, color.copy(alpha = borderAlpha), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Preview(name = "Cyberpunk Controller Landscape", device = "spec:parent=pixel_4,orientation=landscape", showBackground = true)
@Composable
fun ControlDashboardLandscapePreview() {
    ROBOTIKM1LKTTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(DarkBlueGradient))
        ) {
            ControlDashboard()
        }
    }
}