package com.uvg.cc3087.eztunez

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaInicio()
            }
        }
    }
}

@Composable
fun PantallaInicio() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)), // fondo negro
        contentAlignment = Alignment.Center
    ) {
        // el dibujo de fondo microfono/cabezal de guitarra
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centroX = size.width / 2
            val colorGuitarra = Color(0xFF2C2C2C)

            // cuello linea vertical desde el centro hacia abajo
            drawLine(
                color = colorGuitarra,
                start = Offset(centroX, size.height * 0.35f),
                end = Offset(centroX, size.height),
                strokeWidth = 90f,
                cap = StrokeCap.Round
            )

            // rectángulo ancho y grueso arriba del cuello
            drawRoundRect(
                color = colorGuitarra,
                topLeft = Offset(centroX - 180f, size.height * 0.12f),
                size = androidx.compose.ui.geometry.Size(360f, 260f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(60f, 60f)
            )

            // 6 clavijas
            val yInicial = size.height * 0.17f
            val espacio = 70f
            for (i in 0..2) {
                val y = yInicial + (i * espacio)
                // clavijas del lado izquierdo
                drawCircle(
                    color = Color(0xFF3A3A3A),
                    radius = 22f,
                    center = Offset(centroX - 140f, y)
                )
                // clavijas del lado derecho
                drawCircle(
                    color = Color(0xFF3A3A3A),
                    radius = 22f,
                    center = Offset(centroX + 140f, y)
                )
            }

            // Cuerdas
            for (i in -2..2) {
                drawLine(
                    color = Color(0xFF4A4A4A),
                    start = Offset(centroX + (i * 12f), size.height * 0.3f),
                    end = Offset(centroX + (i * 4f), size.height),
                    strokeWidth = 3f
                )
            }
        }

        Text(
            text = "EZTUNEZ",
            color = Color(0xFFE0E0E0), // tono más claro que el fondo, no blanco puro
            fontSize = 34.sp,
            fontWeight = FontWeight.Light, // fuente delgada = look minimalista
            letterSpacing = 6.sp
        )
    }
}