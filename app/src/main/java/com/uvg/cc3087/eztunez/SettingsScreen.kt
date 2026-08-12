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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SettingsBackground = Color(0xFF0D0D0F)
private val SettingsHeader = Color(0xFF1B191E)
private val SettingsCard = Color(0xFF242328)
private val SettingsControl = Color(0xFF302E36)
private val SettingsTeal = Color(0xFF50D0C8)
private val SettingsPrimaryText = Color(0xFFF4F1F5)
private val SettingsSecondaryText = Color(0xFF99959E)
private val SettingsDivider = Color(0xFF464149)

enum class DetectionAlgorithm {
    AUTO,
    FFT
}

enum class NotationSystem {
    STANDARD,
    SOLFEGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (AppScreen) -> Unit
) {

    var micSensitivity by remember {
        mutableFloatStateOf(0.78f)
    }

    var detectionAlgorithm by remember {
        mutableStateOf(DetectionAlgorithm.AUTO)
    }

    var referencePitch by remember {
        mutableIntStateOf(440)
    }

    var darkTheme by remember {
        mutableStateOf(true)
    }

    var showRealTimeHz by remember {
        mutableStateOf(true)
    }

    var notationSystem by remember {
        mutableStateOf(NotationSystem.STANDARD)
    }

    Scaffold(
        containerColor = SettingsBackground,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
                title = "Audio & Sensitivity"
            ) {

                Text(
                    text = "Mic Sensitivity",
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
                        value = micSensitivity,
                        onValueChange = {
                            micSensitivity = it
                        },
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
                        text = "${(micSensitivity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SettingsSecondaryText
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(
                    text = "Detection Algorithm",
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
                        text = "Auto-detect",
                        selected =
                            detectionAlgorithm == DetectionAlgorithm.AUTO,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            detectionAlgorithm =
                                DetectionAlgorithm.AUTO
                        }
                    )

                    SettingsChoiceButton(
                        text = "Advanced FFT",
                        selected =
                            detectionAlgorithm == DetectionAlgorithm.FFT,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            detectionAlgorithm =
                                DetectionAlgorithm.FFT
                        }
                    )
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )


                //  REFERENCE PITCH


                Text(
                    text = "Reference Pitch A4",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsPrimaryText
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Default concert pitch",
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
                                if (referencePitch > 400) {
                                    referencePitch--
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsControl
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.titleLarge,
                                color = SettingsPrimaryText
                            )
                        }
                    }

                    Text(
                        text = "$referencePitch Hz",
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
                                if (referencePitch < 480) {
                                    referencePitch++
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsControl
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge,
                                color = SettingsPrimaryText
                            )
                        }
                    }
                }
            }


            //  DISPLAY OPTIONS


            SettingsSectionCard(
                title = "Display Options"
            ) {

                SettingsSwitchRow(
                    title = "Dark Theme Mode",
                    checked = darkTheme,
                    onCheckedChange = {
                        darkTheme = it
                    }
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
                    title = "Show Real-time Hz",
                    checked = showRealTimeHz,
                    onCheckedChange = {
                        showRealTimeHz = it
                    }
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Text(
                    text = "Notation System",
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
                        text = "Standard (C-D-E)",
                        selected =
                            notationSystem == NotationSystem.STANDARD,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            notationSystem =
                                NotationSystem.STANDARD
                        }
                    )

                    SettingsChoiceButton(
                        text = "Solfège (Do-Re-Mi)",
                        selected =
                            notationSystem == NotationSystem.SOLFEGE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            notationSystem =
                                NotationSystem.SOLFEGE
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
                        title = "App Version",
                        trailingText = "v1.0 (Pro)"
                    )

                    HorizontalDivider(
                        color = SettingsDivider
                    )

                    SettingsInfoRow(
                        title = "Rate TunePro App",
                        trailingText = "›",
                        onClick = {

                        }
                    )

                    HorizontalDivider(
                        color = SettingsDivider
                    )

                    SettingsInfoRow(
                        title = "Send Support Feedback",
                        trailingText = "›",
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
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