package com.uvg.cc3087.eztunez

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Android boundary: permission UI, settings intent, and screen/session lifetime. */
@Composable
fun TunerRoute(
    viewModel: TunerViewModel,
    mode: TuningMode,
    settings: TunerSettings,
    preset: TuningPreset = BuiltInTuningPresets.standard,
    onNavigate: (AppScreen) -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var hasMicrophonePermission by remember {
        mutableStateOf(context.hasMicrophonePermission())
    }
    var isResumed by remember(lifecycleOwner) {
        mutableStateOf(false)
    }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var captureRequestToken by remember(lifecycleOwner) { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicrophonePermission = granted
        viewModel.setMicrophoneState(
            if (granted) {
                MicrophoneState.PERMISSION_GRANTED
            } else if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.RECORD_AUDIO
                ) && permissionRequested
            ) {
                MicrophoneState.PERMISSION_PERMANENTLY_DENIED
            } else {
                MicrophoneState.PERMISSION_DENIED
            }
        )
    }

    DisposableEffect(lifecycleOwner, context, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val granted = context.hasMicrophonePermission()
                    if (granted != hasMicrophonePermission) {
                        hasMicrophonePermission = granted
                        viewModel.setMicrophoneState(
                            if (granted) MicrophoneState.PERMISSION_GRANTED
                            else MicrophoneState.PERMISSION_DENIED
                        )
                    }
                    isResumed = true
                    // Also restart after a pause/resume that occurs between Compose frames.
                    captureRequestToken++
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    isResumed = false
                    // Request shutdown now; native cleanup runs off the UI thread.
                    viewModel.stopListening()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!hasMicrophonePermission && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(
        viewModel,
        lifecycleOwner,
        mode,
        settings.referencePitch,
        settings.micSensitivity,
        preset,
        hasMicrophonePermission,
        isResumed,
        captureRequestToken
    ) {
        viewModel.configure(mode, settings.referencePitch, preset)
        if (hasMicrophonePermission && isResumed &&
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
            viewModel.setMicrophoneState(MicrophoneState.PERMISSION_GRANTED)
            viewModel.startListening(settings.micSensitivity)
        }
        onDispose { viewModel.stopListening() }
    }

    TuneScreen(
        mode = mode,
        settings = settings,
        preset = preset,
        uiState = uiState,
        onSelectString = viewModel::selectString,
        onRetry = {
            if (!hasMicrophonePermission) {
                permissionRequested = true
                viewModel.setMicrophoneState(MicrophoneState.PERMISSION_PENDING)
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                viewModel.setMicrophoneState(MicrophoneState.PERMISSION_GRANTED)
                // Retry through the same lifecycle gate as every other capture start.
                captureRequestToken++
            }
        },
        onOpenSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
            )
        },
        onNavigate = onNavigate
    )
}

private fun Context.hasMicrophonePermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
