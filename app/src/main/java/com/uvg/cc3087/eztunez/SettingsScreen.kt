package com.uvg.cc3087.eztunez

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SettingsBackground: Color @Composable get() = MaterialTheme.colorScheme.background
private val SettingsHeader: Color @Composable get() = MaterialTheme.colorScheme.surface
private val SettingsCard: Color @Composable get() = MaterialTheme.colorScheme.surface
private val SettingsControl: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val SettingsTeal: Color @Composable get() = MaterialTheme.colorScheme.primary
private val SettingsPrimaryText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val SettingsSecondaryText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val SettingsDivider: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: TunerSettings,
    onMicSensitivityChange: (Float) -> Unit,
    onMicSensitivityChangeFinished: () -> Unit,
    onReferencePitchAdjust: (Int) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onShowRealTimeHzChange: (Boolean) -> Unit,
    onNotationSystemChange: (NotationSystem) -> Unit,
    saveError: Boolean,
    onRetrySave: () -> Unit,
    onNavigate: (AppScreen) -> Unit
) {

    Scaffold(
        containerColor = SettingsBackground,
        snackbarHost = {
            if (saveError) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    actionOnNewLine = true,
                    action = {
                        TextButton(onClick = onRetrySave) {
                            Text(stringResource(R.string.retry_save))
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_save_error))
                }
            }
        },

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_settings),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SettingsPrimaryText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsHeader
                )
            )
        },

        bottomBar = {
            EzTunezNavigationBar(
                selectedScreen = AppScreen.SETTINGS,
                onNavigate = onNavigate
            )
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            //  AUDIO & SENSITIVITY


            SettingsSectionCard(
                title = stringResource(R.string.settings_audio)
            ) {

                Text(
                    text = stringResource(R.string.mic_sensitivity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsPrimaryText
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Slider(
                        value = settings.micSensitivity,
                        onValueChange = onMicSensitivityChange,
                        onValueChangeFinished = onMicSensitivityChangeFinished,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = SettingsTeal,
                            activeTrackColor = SettingsTeal,
                            inactiveTrackColor = SettingsControl
                        )
                    )

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Text(
                        text = stringResource(R.string.percent_value, (settings.micSensitivity * 100).roundToInt()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SettingsSecondaryText
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(
                    text = stringResource(R.string.detection_algorithm),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsPrimaryText
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    SettingsChoiceButton(
                        text = stringResource(R.string.auto_detect),
                        selected = true,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // YIN is the only documented and implemented detector.
                        }
                    )

                    SettingsChoiceButton(
                        text = stringResource(R.string.advanced_fft_soon),
                        selected = false,
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // Intentionally disabled until an FFT detector is implemented and tested.
                        }
                    )
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )


                //  REFERENCE PITCH


                Text(
                    text = stringResource(R.string.reference_pitch),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsPrimaryText
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = stringResource(R.string.default_concert_pitch),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SettingsSecondaryText
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {



                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable {
                                onReferencePitchAdjust(-1)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsControl
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.symbol_minus),
                                style = MaterialTheme.typography.titleLarge,
                                color = SettingsPrimaryText
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.frequency_hz, settings.referencePitch),
                        modifier = Modifier.padding(
                            horizontal = 16.dp
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SettingsPrimaryText
                    )


                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable {
                                onReferencePitchAdjust(1)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsControl
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.symbol_plus),
                                style = MaterialTheme.typography.titleLarge,
                                color = SettingsPrimaryText
                            )
                        }
                    }
                }
            }


            //  DISPLAY OPTIONS


            SettingsSectionCard(
                title = stringResource(R.string.display_options)
            ) {

                SettingsSwitchRow(
                    title = stringResource(R.string.dark_theme),
                    checked = settings.darkTheme,
                    onCheckedChange = onDarkThemeChange
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                HorizontalDivider(
                    color = SettingsDivider
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                SettingsSwitchRow(
                    title = stringResource(R.string.show_realtime_hz),
                    checked = settings.showRealTimeHz,
                    onCheckedChange = onShowRealTimeHzChange
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Text(
                    text = stringResource(R.string.notation_system),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsPrimaryText
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    SettingsChoiceButton(
                        text = stringResource(R.string.notation_standard),
                        selected =
                            settings.notationSystem == NotationSystem.STANDARD,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onNotationSystemChange(NotationSystem.STANDARD)
                        }
                    )

                    SettingsChoiceButton(
                        text = stringResource(R.string.notation_solfege),
                        selected =
                            settings.notationSystem == NotationSystem.SOLFEGE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onNotationSystemChange(NotationSystem.SOLFEGE)
                        }
                    )
                }
            }


            //  APP INFORMATION


            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SettingsCard
                )
            ) {

                Column(
                    modifier = Modifier.padding(
                        horizontal = 20.dp
                    )
                ) {

                    SettingsInfoRow(
                        title = stringResource(R.string.app_version),
                        trailingText = stringResource(R.string.app_version_value)
                    )

                    HorizontalDivider(
                        color = SettingsDivider
                    )

                    SettingsInfoRow(
                        title = stringResource(R.string.rate_app, stringResource(R.string.app_name)),
                        trailingText = stringResource(R.string.symbol_chevron),
                        onClick = {

                        }
                    )

                    HorizontalDivider(
                        color = SettingsDivider
                    )

                    SettingsInfoRow(
                        title = stringResource(R.string.support_feedback),
                        trailingText = stringResource(R.string.symbol_chevron),
                        onClick = {

                        }
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }
    }
}



@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SettingsCard
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SettingsTeal
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            HorizontalDivider(
                color = SettingsDivider
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            content()
        }
    }
}


//  SELECTABLE BUTTON


@Composable
private fun SettingsChoiceButton(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),

        color = if (selected) {
            SettingsTeal
        } else {
            SettingsControl
        },

        shape = RoundedCornerShape(12.dp)
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,

                fontWeight = if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },

                color = if (selected) {
                    SettingsBackground
                } else {
                    SettingsPrimaryText
                }
            )
        }
    }
}


//  SWITCH ROW


@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = SettingsPrimaryText
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,

            colors = SwitchDefaults.colors(
                checkedThumbColor = SettingsBackground,
                checkedTrackColor = SettingsTeal,
                uncheckedThumbColor = SettingsSecondaryText,
                uncheckedTrackColor = SettingsControl
            )
        )
    }
}


//  BOTTOM INFORMATION ROW


@Composable
private fun SettingsInfoRow(
    title: String,
    trailingText: String,
    onClick: (() -> Unit)? = null
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(
                vertical = 16.dp
            ),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = SettingsPrimaryText
        )

        Text(
            text = trailingText,
            style = MaterialTheme.typography.bodyLarge,
            color = SettingsSecondaryText
        )
    }
}
