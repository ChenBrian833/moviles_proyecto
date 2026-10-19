package com.uvg.cc3087.eztunez

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun LoadingScreen() {
    val teal = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    val primaryText = MaterialTheme.colorScheme.onBackground
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val transition = rememberInfiniteTransition(label = "loadingAnimation")
    val rippleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "expandingRipples"
    )
    val dotAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "dotPulse"
    )

    Box(Modifier.fillMaxSize().background(background)) {
        Column(
            modifier = Modifier.align(Alignment.Center).offset(y = (-32).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Draw the X and its rings separately so the waves really expand.
            // The original bitmap remains available to the rest of the application.
            Canvas(
                Modifier.size(152.dp).semantics { contentDescription = "EzTunez logo" }
            ) {
                val tint = Color(0xFF50EEE3)
                val maxRadius = size.minDimension * 0.46f
                val origin = center
                val pulse = (0.5 + 0.5 * sin(2.0 * PI * rippleProgress)).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(teal.copy(alpha = 0.13f + pulse * 0.035f), Color.Transparent),
                        center = origin,
                        radius = maxRadius
                    ),
                    radius = maxRadius,
                    center = origin
                )
                repeat(3) { index ->
                    val phase = (rippleProgress + index / 3f) % 1f
                    val radius = 18.dp.toPx() + (maxRadius - 18.dp.toPx()) * phase
                    // Fade in at birth and disappear before wrapping to the center.
                    val opacity = sin(PI * phase).toFloat().coerceIn(0f, 1f) * (1f - 0.45f * phase)
                    drawCircle(
                        color = tint.copy(alpha = opacity * 0.075f),
                        radius = radius,
                        center = origin,
                        style = Stroke(width = 6.5.dp.toPx())
                    )
                    drawCircle(
                        color = tint.copy(alpha = opacity * 0.85f),
                        radius = radius,
                        center = origin,
                        style = Stroke(width = (1.9f - phase * 0.8f).dp.toPx())
                    )
                }
                val arm = 9.dp.toPx()
                for (direction in listOf(-1f, 1f)) {
                    val start = origin + Offset(-arm, -arm * direction)
                    val end = origin + Offset(arm, arm * direction)
                    drawLine(tint.copy(alpha = 0.12f), start, end, 7.dp.toPx(), StrokeCap.Round)
                    drawLine(tint, start, end, 2.8.dp.toPx(), StrokeCap.Round)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "EzTunez",
                color = primaryText,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Perfect pitch, every time",
                color = secondaryText,
                fontSize = 16.sp
            )
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).alpha(dotAlpha).background(teal, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(
                text = "CALIBRATING MIC...",
                color = teal,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}
