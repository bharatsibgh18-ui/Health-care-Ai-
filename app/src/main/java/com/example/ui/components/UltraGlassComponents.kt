package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassTintDark
import com.example.ui.theme.GlassTintLight
import com.example.ui.theme.UltraCyanEnd
import com.example.ui.theme.UltraEmeraldStart
import com.example.ui.theme.UltraIndigoStart
import com.example.ui.theme.UltraMintEnd
import com.example.ui.theme.UltraTealStart
import com.example.utils.QrMatrix

@Composable
fun isUltraDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
}

/**
 * Translucent Clinical Glass Card with subtle 1dp luminous gradient border
 * supporting both Light and Dark theme modes.
 */
@Composable
fun UltraGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    accentStart: Color = UltraTealStart,
    accentEnd: Color = UltraCyanEnd,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glassGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                GlassSurfaceDark,
                GlassTintDark.copy(alpha = 0.85f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GlassSurfaceLight,
                GlassTintLight.copy(alpha = 0.72f)
            )
        )
    }
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            accentStart.copy(alpha = if (isDark) 0.55f else 0.38f),
            accentEnd.copy(alpha = if (isDark) 0.35f else 0.22f),
            if (isDark) GlassBorderDark else GlassBorderLight
        )
    )
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 2.dp,
        shadowElevation = if (isDark) 4.dp else 6.dp,
        modifier = modifier
            .clip(shape)
            .background(glassGradient, shape)
            .border(BorderStroke(1.dp, borderBrush), shape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * High-impact Gradient Hero Card for Ultra Command Center headers and Medical ID banners.
 */
@Composable
fun UltraGradientHeroCard(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(UltraTealStart, UltraCyanEnd, UltraIndigoStart),
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(colors = colors))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    )
                ),
                shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Smooth animated circular progress ring for the 0-100 Daily Health Score.
 */
@Composable
fun AnimatedHealthScoreRing(
    score: Int,
    tierLabel: String,
    modifier: Modifier = Modifier,
    ringSize: Dp = 148.dp,
    strokeWidth: Dp = 14.dp
) {
    val targetFraction = (score.coerceIn(0, 100)) / 100f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "health_score_sweep"
    )

    val arcColors = when {
        score >= 85 -> listOf(UltraMintEnd, Color(0xFF9EEFFD), Color.White)
        score >= 68 -> listOf(Color(0xFF38BDF8), Color(0xFF9EEFFD), UltraMintEnd)
        score >= 48 -> listOf(Color(0xFFFBBF24), Color(0xFFFDE68A), Color(0xFF38BDF8))
        else -> listOf(Color(0xFFFB7185), Color(0xFFFBBF24), Color(0xFFFDA4AF))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(ringSize)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Frosted background track
            drawArc(
                color = Color.White.copy(alpha = 0.20f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Luminous animated sweep arc
            drawArc(
                brush = Brush.sweepGradient(colors = arcColors),
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 40.sp
            )
            Text(
                text = tierLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UltraMetricPill(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.26f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun UltraSectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.15f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Renders a genuine scannable ISO/IEC 18004 QR matrix with a 4-module quiet zone.
 */
@Composable
fun ScannableQrCodeCanvas(
    matrix: QrMatrix,
    modifier: Modifier = Modifier,
    darkColor: Color = Color(0xFF0A192F),
    lightColor: Color = Color.White
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = lightColor,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, UltraEmeraldStart.copy(alpha = 0.25f)),
        modifier = modifier.aspectRatio(1f)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            val quietZone = 2
            val totalModules = matrix.size + (quietZone * 2)
            val moduleSize = minOf(size.width, size.height) / totalModules.toFloat()
            val offsetX = (size.width - (moduleSize * totalModules)) / 2f
            val offsetY = (size.height - (moduleSize * totalModules)) / 2f

            drawRect(color = lightColor)

            for (y in 0 until matrix.size) {
                for (x in 0 until matrix.size) {
                    if (matrix.isDark(x, y)) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(
                                x = offsetX + (x + quietZone) * moduleSize,
                                y = offsetY + (y + quietZone) * moduleSize
                            ),
                            size = Size(moduleSize + 0.5f, moduleSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}
