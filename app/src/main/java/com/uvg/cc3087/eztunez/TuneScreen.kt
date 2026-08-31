/*
package com.uvg.cc3087.eztunez

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val TuneBackground = Color(0xFF0D0D0F)
private val TuneHeader = Color(0xFF1B191E)
private val TuneCard = Color(0xFF242328)
private val TuneControl = Color(0xFF302E36)
private val TuneTeal = Color(0xFF50D0C8)
private val TuneText = Color(0xFFF4F1F5)
private val TuneSecondaryText = Color(0xFF99959E)
private val TuneMeterInactive = Color(0xFF464149)

data class GuitarString(
    val id: Int,
    val note: String,
    val frequency: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuneScreen(
    onNavigate: (AppScreen) -> Unit
) {

    val guitarStrings = remember {
        listOf(
            GuitarString(
                id = 1,
                note = "E2",
                frequency = 82.4f
            ),
            GuitarString(
                id = 2,
                note = "A2",
                frequency = 110.0f
            ),
            GuitarString(
                id = 3,
                note = "D3",
                frequency = 146.8f
            ),
            GuitarString(
                id = 4,
                note = "G3",
                frequency = 196.0f
            ),
            GuitarString(
                id = 5,
                note = "B3",
                frequency = 246.9f
            ),
            GuitarString(
                id = 6,
                note = "E4",
                frequency = 329.6f
            )
        )
    }

    var selectedStringId by remember {
        mutableIntStateOf(1)
    }

    val selectedString = guitarStrings.first {
        it.id == selectedStringId
    }

    /*
     * This will eventually come from your
     * microphone / pitch detection.
     *
     * For now 0 means perfectly in tune.
     */
    var centsOffset by remember {
        mutableFloatStateOf(0f)
    }

    val isInTune = centsOffset in -5f..5f

    Scaffold(
        containerColor = TuneBackground,

        topBar = {
            TopAppBar(
                title = {
                    Column {

                        Text(
                            text = "Acoustic",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TuneText
                        )

                        Text(
                            text = "Standard",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TuneSecondaryText
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TuneHeader
                )
            )
        },

        bottomBar = {
            EzTunezNavigationBar(
                selectedScreen = AppScreen.TUNE,
                onNavigate = onNavigate
            )
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            /*
             * DETECTED NOTE
             */

            Text(
                text = selectedString.note,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = TuneText
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "%.1f Hz".format(selectedString.frequency),
                style = MaterialTheme.typography.titleLarge,
                color = TuneSecondaryText
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            /*
             * TUNING METER
             */

            TuningMeter(
                centsOffset = centsOffset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            /*
             * STATUS
             */

            Text(
                text = if (isInTune) {
                    "In Tune ✓"
                } else if (centsOffset < 0) {
                    "Tune Up"
                } else {
                    "Tune Down"
                },

                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,

                color = if (isInTune) {
                    TuneTeal
                } else {
                    TuneText
                }
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Perfect pitch offset: %.1f cents".format(centsOffset),
                style = MaterialTheme.typography.bodyLarge,
                color = TuneSecondaryText
            )

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            /*
             * STRING SELECTOR
             */

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TuneCard
                )
            ) {

                Column(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 16.dp
                    )
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                        guitarStrings.forEach { guitarString ->

                            GuitarStringButton(
                                note = guitarString.note,
                                selected =
                                    guitarString.id == selectedStringId,
                                onClick = {
                                    selectedStringId =
                                        guitarString.id

                                    /*
                                     * Reset simulated pitch when
                                     * selecting another string.
                                     */
                                    centsOffset = 0f
                                }
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    /*
                     * Simulated accuracy/progress shown
                     * in the prototype.
                     */
                    LinearProgressIndicator(
                        progress = { 0.54f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),

                        color = TuneTeal,
                        trackColor = TuneControl
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "54%",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TuneSecondaryText
                    )
                }
            }

            /*
             * TEMPORARY SIMULATION CONTROLS
             *
             * These let you test the tuning meter before
             * microphone detection is implemented.
             */

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Pitch simulation",
                style = MaterialTheme.typography.labelLarge,
                color = TuneSecondaryText
            )

            Slider(
                value = centsOffset,
                onValueChange = {
                    centsOffset = it
                },
                valueRange = -50f..50f,

                colors = SliderDefaults.colors(
                    thumbColor = TuneTeal,
                    activeTrackColor = TuneTeal,
                    inactiveTrackColor = TuneControl
                )
            )
        }
    }
}

@Composable
private fun GuitarStringButton(
    note: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .size(48.dp)
            .clickable(
                onClick = onClick
            ),

        shape = CircleShape,

        color = if (selected) {
            TuneTeal
        } else {
            TuneControl
        }
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = note,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,

                color = if (selected) {
                    TuneBackground
                } else {
                    TuneText
                }
            )
        }
    }
}

@Composable
private fun TuningMeter(
    centsOffset: Float,
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        val centerX = size.width / 2f
        val centerY = size.height * 0.92f

        val radius = size.width * 0.38f

        /*
         * Main semicircle.
         */
        drawArc(
            color = TuneMeterInactive,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,

            topLeft = Offset(
                x = centerX - radius,
                y = centerY - radius
            ),

            size = androidx.compose.ui.geometry.Size(
                width = radius * 2,
                height = radius * 2
            ),

            style = Stroke(
                width = 12f,
                cap = StrokeCap.Round
            )
        )

        /*
         * Tick marks from -50 to +50 cents.
         */
        for (i in 0..10) {

            val angleDegrees =
                180f + (i * 18f)

            val angleRadians =
                Math.toRadians(
                    angleDegrees.toDouble()
                )

            val outerRadius = radius + 18f
            val innerRadius = radius - 6f

            val outerX =
                centerX +
                        cos(angleRadians).toFloat() *
                        outerRadius

            val outerY =
                centerY +
                        sin(angleRadians).toFloat() *
                        outerRadius

            val innerX =
                centerX +
                        cos(angleRadians).toFloat() *
                        innerRadius

            val innerY =
                centerY +
                        sin(angleRadians).toFloat() *
                        innerRadius

            drawLine(
                color = if (i == 5) {
                    TuneTeal
                } else {
                    TuneSecondaryText
                },

                start = Offset(
                    innerX,
                    innerY
                ),

                end = Offset(
                    outerX,
                    outerY
                ),

                strokeWidth = if (i == 5) {
                    6f
                } else {
                    3f
                },

                cap = StrokeCap.Round
            )
        }

        /*
         * Needle position.
         *
         * -50 cents = left
         * 0 cents   = center
         * +50 cents = right
         */
        val safeOffset =
            centsOffset.coerceIn(
                -50f,
                50f
            )

        val normalized =
            (safeOffset + 50f) / 100f

        val needleDegrees =
            180f + normalized * 180f

        val needleRadians =
            Math.toRadians(
                needleDegrees.toDouble()
            )

        val needleLength =
            radius * 0.72f

        val needleX =
            centerX +
                    cos(needleRadians).toFloat() *
                    needleLength

        val needleY =
            centerY +
                    sin(needleRadians).toFloat() *
                    needleLength

        drawLine(
            color = TuneTeal,

            start = Offset(
                centerX,
                centerY
            ),

            end = Offset(
                needleX,
                needleY
            ),

            strokeWidth = 8f,

            cap = StrokeCap.Round
        )

        /*
         * Center pivot.
         */
        drawCircle(
            color = TuneTeal,
            radius = 12f,
            center = Offset(
                centerX,
                centerY
            )
        )
    }
}

*/