
package com.uvg.cc3087.eztunez

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private val TuneBackground: Color @Composable get() = MaterialTheme.colorScheme.background
private val TuneHeader: Color @Composable get() = MaterialTheme.colorScheme.surface
private val TuneCard: Color @Composable get() = MaterialTheme.colorScheme.surface
private val TuneControl: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val TuneTeal: Color @Composable get() = MaterialTheme.colorScheme.primary
private val TuneText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val TuneSecondaryText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val TuneMeterInactive: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuneScreen(
    mode: TuningMode = TuningMode.AUTO,
    settings: TunerSettings = TunerSettings(),
    onNavigate: (AppScreen) -> Unit
) {

    val context = LocalContext.current

    var hasMicrophonePermission by remember {

        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var microphoneState by remember {
        mutableStateOf(
            if (hasMicrophonePermission) MicrophoneState.PERMISSION_GRANTED
            else MicrophoneState.PERMISSION_PENDING
        )
    }
    var retryToken by remember { mutableIntStateOf(0) }
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasMicrophonePermission = granted
            microphoneState = if (granted) {
                MicrophoneState.PERMISSION_GRANTED
            } else if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                ) && permissionRequested
            ) {
                MicrophoneState.PERMISSION_PERMANENTLY_DENIED
            } else {
                MicrophoneState.PERMISSION_DENIED
            }
        }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted && !hasMicrophonePermission) {
                    hasMicrophonePermission = true
                    microphoneState = MicrophoneState.PERMISSION_GRANTED
                } else if (!granted && hasMicrophonePermission) {
                    hasMicrophonePermission = false
                    microphoneState = MicrophoneState.PERMISSION_DENIED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {

        if (!hasMicrophonePermission) {
            permissionRequested = true
            permissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    val guitarStrings = remember(settings.referencePitch) {
        guitarStringsForReference(settings.referencePitch)
    }
    val tracker = remember(mode, settings.referencePitch) {
        TuningTracker(mode, settings.referencePitch)
    }
    var tuningState by remember(tracker) { mutableStateOf(tracker.state) }
    val selectedString = tuningState.selectedString
    val selectedStringId = selectedString.id
    val detectedPitch = tuningState.pitch
    val centsOffset = tuningState.cents
    val isInTune = tuningState.isInTune
    val detectionConfidence = detectedPitch?.probability?.coerceIn(0f, 1f) ?: 0f

    DisposableEffect(
        context,
        hasMicrophonePermission,
        tracker,
        settings.micSensitivity,
        retryToken
    ) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var active = true
        val detector = GuitarPitchDetector(
            context = context,
            rmsThreshold = sensitivityToRmsThreshold(settings.micSensitivity),
            referencePitch = settings.referencePitch,
            onPitchDetected = { pitch ->
                handler.post {
                    if (active) {
                        tuningState = tracker.update(
                            pitch,
                            android.os.SystemClock.elapsedRealtime()
                        )
                    }
                }
            },
            onStateChanged = { state ->
                handler.post { if (active) microphoneState = state }
            }
        )
        if (hasMicrophonePermission) detector.start()
        onDispose {
            active = false
            detector.stop()
            handler.removeCallbacksAndMessages(null)
        }
    }


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
                            text = "Standard · ${if (mode == TuningMode.AUTO) "Auto Tune" else "Manual Tune"}",
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

            if (microphoneState != MicrophoneState.LISTENING) {
                MicrophoneStatusCard(
                    state = microphoneState,
                    onRetry = {
                        if (!hasMicrophonePermission) {
                            permissionRequested = true
                            microphoneState = MicrophoneState.PERMISSION_PENDING
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            microphoneState = MicrophoneState.PERMISSION_GRANTED
                            retryToken++
                        }
                    },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            /*
             * DETECTED NOTE
             */

            Text(
                text = formatNote(selectedString.note, settings.notationSystem),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = TuneText
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = formatPitchDisplay(
                    detectedPitch,
                    settings.showRealTimeHz,
                    settings.notationSystem
                ),

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
                hasSignal = detectedPitch != null,
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
                text = when {

                    detectedPitch == null -> {
                        "Play a string"
                    }

                    isInTune -> {
                        "In Tune ✓"
                    }

                    centsOffset < 0 -> {
                        "Tune Up"
                    }

                    else -> {
                        "Tune Down"
                    }
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
                text = if (detectedPitch != null) {
                    "${selectedString.note} offset: %.1f cents".format(centsOffset)
                } else {
                    microphoneState.message()
                },

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
                                note = formatNote(guitarString.note, settings.notationSystem),
                                selected =
                                    guitarString.id == selectedStringId,
                                enabled = mode == TuningMode.MANUAL,
                                onClick = {
                                    tuningState = tracker.selectString(guitarString.id)
                                }
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    /*
                     * Confidence reported by Tarsos for the retained reading.
                     */
                    LinearProgressIndicator(
                        progress = {
                            detectionConfidence
                        },
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
                        text = "${(detectionConfidence * 100).toInt()}%",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TuneSecondaryText
                    )
                }
            }


        }
    }
}

@Composable
private fun MicrophoneStatusCard(
    state: MicrophoneState,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TuneCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(state.message(), color = TuneText)
            if (state.canRetry || state == MicrophoneState.PERMISSION_PENDING) {
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            if (state == MicrophoneState.PERMISSION_PERMANENTLY_DENIED) {
                TextButton(onClick = onOpenSettings) { Text("Open app settings") }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun GuitarStringButton(
    note: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .size(48.dp)
            .clickable(
                enabled = enabled,
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
    hasSignal: Boolean,
    modifier: Modifier = Modifier
) {
    val meterInactive = TuneMeterInactive
    val teal = TuneTeal
    val secondaryText = TuneSecondaryText
    // A damped spring interpolates audio frames without overshoot or abrupt jumps.
    val needleOffset by animateFloatAsState(
        targetValue = centsOffset.coerceIn(-50f, 50f),
        animationSpec = spring(dampingRatio = 1f, stiffness = 180f),
        label = "Tuning needle"
    )

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
            color = meterInactive,
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
                    teal
                } else {
                    secondaryText
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
            needleOffset.coerceIn(
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
            color = if (hasSignal) teal else meterInactive,

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
            color = if (hasSignal) teal else meterInactive,
            radius = 12f,
            center = Offset(
                centerX,
                centerY
            )
        )
    }
}


//
