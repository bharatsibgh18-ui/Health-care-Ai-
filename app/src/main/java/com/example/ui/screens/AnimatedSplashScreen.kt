package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.UltraCyanEnd
import com.example.ui.theme.UltraIndigoStart
import com.example.ui.theme.UltraTealStart
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Engaging animated startup splash screen displaying:
 * - Top: "Created by Shailesh and Ruturaj"
 * - Center: Pulsing App Logo with rotating clinical halo & animated ECG heartbeat wave
 * - Bottom: "Health Care AI" + "Model School Sayla"
 */
@Composable
fun AnimatedSplashScreen(
    durationMillis: Long = 2200L,
    onSplashFinished: () -> Unit
) {
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
        delay(durationMillis)
        onSplashFinished()
    }

    val logoScale by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0.55f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_logo_scale"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "splash_text_alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")

    val radarPulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse_1"
    )
    val radarPulse2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse_2"
    )
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )
    val ecgSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ecg_sweep"
    )
    val loadingProgress by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis.toInt().coerceAtLeast(600), easing = FastOutSlowInEasing),
        label = "splash_progress"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06283D),
                        UltraTealStart,
                        UltraCyanEnd,
                        UltraIndigoStart
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onSplashFinished() })
            }
            .testTag("animated_splash_screen")
    ) {
        val isCompactHeight = maxHeight < 600.dp
        val outerVerticalPadding = if (isCompactHeight) 14.dp else 32.dp
        val orbitBoxSize = if (isCompactHeight) 146.dp else 216.dp
        val logoSize = if (isCompactHeight) 92.dp else 134.dp
        val ecgHeight = if (isCompactHeight) 24.dp else 34.dp
        val sectionGap = if (isCompactHeight) 8.dp else 14.dp

        // Ambient animated background glow circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val floatOffset = sin(ecgSweep * 2f * PI.toFloat()) * 24f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(w * 0.18f, h * 0.22f + floatOffset),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.18f, h * 0.22f + floatOffset)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2DD4BF).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.78f - floatOffset),
                    radius = w * 0.60f
                ),
                radius = w * 0.60f,
                center = Offset(w * 0.85f, h * 0.78f - floatOffset)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = outerVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION: Created by Shailesh and Ruturaj (above the logo)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = if (isCompactHeight) 2.dp else 12.dp)
                    .alpha(textAlpha)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(50)
                        )
                        .testTag("splash_created_by_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF67E8F9),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Created by Shailesh and Ruturaj",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.4.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // CENTER SECTION: Pulsing App Logo + Rotating Halo + Animated ECG Wave + Model School Sayla
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(orbitBoxSize)
                ) {
                    // Expanding sonar heartbeat rings + rotating orbital halo
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = size.minDimension * 0.30f
                        val maxRadius = size.minDimension * 0.49f

                        val p1 = radarPulse1
                        val r1 = baseRadius + (maxRadius - baseRadius) * p1
                        drawCircle(
                            color = Color(0xFF67E8F9).copy(alpha = (1f - p1) * 0.50f),
                            radius = r1,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        val p2 = radarPulse2 % 1f
                        val r2 = baseRadius + (maxRadius - baseRadius) * p2
                        drawCircle(
                            color = Color(0xFF5EEAD4).copy(alpha = (1f - p2) * 0.42f),
                            radius = r2,
                            center = center,
                            style = Stroke(width = 2.5.dp.toPx())
                        )

                        // Rotating segmented clinical halo
                        rotate(degrees = haloRotation, pivot = center) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF67E8F9),
                                        Color.White,
                                        Color(0xFF2DD4BF),
                                        Color.Transparent
                                    )
                                ),
                                startAngle = 0f,
                                sweepAngle = 220f,
                                useCenter = false,
                                topLeft = Offset(
                                    center.x - size.minDimension * 0.36f,
                                    center.y - size.minDimension * 0.36f
                                ),
                                size = androidx.compose.ui.geometry.Size(
                                    size.minDimension * 0.72f,
                                    size.minDimension * 0.72f
                                ),
                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Glowing App Logo Medallion
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 18.dp,
                        modifier = Modifier
                            .size(logoSize)
                            .scale(logoScale * breatheScale)
                            .border(
                                width = 3.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF67E8F9), Color.White, Color(0xFF2DD4BF))
                                ),
                                shape = CircleShape
                            )
                            .testTag("splash_app_logo")
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_asset),
                            contentDescription = "Health Care AI Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(sectionGap))

                // App Name
                Text(
                    text = "Health Care AI",
                    style = if (isCompactHeight) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.alpha(textAlpha)
                )

                Spacer(modifier = Modifier.height(if (isCompactHeight) 4.dp else 6.dp))

                // Animated ECG Heartbeat Pulse Line
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(ecgHeight)
                        .alpha(textAlpha)
                ) {
                    val w = size.width
                    val midY = size.height / 2f
                    val path = Path().apply {
                        moveTo(0f, midY)
                        lineTo(w * 0.24f, midY)
                        lineTo(w * 0.31f, midY - size.height * 0.18f)
                        lineTo(w * 0.37f, midY + size.height * 0.20f)
                        lineTo(w * 0.44f, midY - size.height * 0.46f)
                        lineTo(w * 0.51f, midY + size.height * 0.42f)
                        lineTo(w * 0.58f, midY - size.height * 0.14f)
                        lineTo(w * 0.65f, midY)
                        lineTo(w, midY)
                    }
                    // Base subtle ECG track
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.24f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    // Moving bright pulse highlight along the ECG wave
                    if (w > 1f) {
                        val highlightX = w * ecgSweep
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF67E8F9),
                                    Color.White,
                                    Color(0xFF5EEAD4),
                                    Color.Transparent
                                ),
                                startX = (highlightX - w * 0.22f).coerceAtLeast(0f),
                                endX = (highlightX + w * 0.22f).coerceAtMost(w)
                            ),
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(sectionGap))

                // BELOW LOGO: Model School Sayla
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier
                        .alpha(textAlpha)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.32f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .testTag("splash_school_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Model School Sayla",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // BOTTOM SECTION: Smooth animated loading bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .alpha(textAlpha)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.20f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(loadingProgress.coerceIn(0.05f, 1f))
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF67E8F9), Color.White, Color(0xFF2DD4BF))
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Loading Smart Clinical AI...",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
