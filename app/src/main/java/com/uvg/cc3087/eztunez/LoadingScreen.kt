package com.uvg.cc3087.eztunez

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoadingScreen() {

    val infiniteTransition = rememberInfiniteTransition(
        label = "loadingAnimation"
    )

    // Logo gently moves up and down
    val logoOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoOffset"
    )

    // Very subtle logo breathing/pulse
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    // Calibration dot pulse
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 700
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E10))
    ) {

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-32).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ACTUAL LOGO FROM YOUR PROTOTYPE
            Image(
                painter = painterResource(
                    id = R.drawable.eztunez_logo
                ),
                contentDescription = "EzTunez logo",
                modifier = Modifier
                    .size(120.dp)
                    .offset(y = logoOffset.dp)
                    .scale(logoScale)
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "EzTunez",
                color = Color(0xFFF4F1F5),
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Perfect pitch, every time",
                color = Color(0xFF969199),
                fontSize = 16.sp
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(dotAlpha)
                    .background(
                        color = Color(0xFF50D0C8),
                        shape = CircleShape
                    )
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Text(
                text = "CALIBRATING MIC...",
                color = Color(0xFF50D0C8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}