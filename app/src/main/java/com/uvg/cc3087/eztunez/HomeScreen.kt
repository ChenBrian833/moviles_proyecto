package com.uvg.cc3087.eztunez

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val HomeBackground: Color @Composable get() = MaterialTheme.colorScheme.background
private val HomeHeader: Color @Composable get() = MaterialTheme.colorScheme.surface
private val HomeCard: Color @Composable get() = MaterialTheme.colorScheme.surface
private val HomeIconBox: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val HomeTeal: Color @Composable get() = MaterialTheme.colorScheme.primary
private val HomeSelected: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
private val HomeText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val HomeSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    preset: TuningPreset = BuiltInTuningPresets.standard,
    notationSystem: NotationSystem = NotationSystem.STANDARD,
    onNavigate: (AppScreen) -> Unit,
    onStartTuning: (TuningMode) -> Unit
) {

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = HomeBackground,

        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "EzTunez",
                        color = HomeText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },

                actions = {

                    Text(
                        text = "♬",
                        color = HomeText,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 20.dp)
                    )
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HomeHeader
                )
            )
        },

        bottomBar = {
            EzTunezNavigationBar(
                selectedScreen = AppScreen.HOME,
                onNavigate = onNavigate
            )
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // instriment card

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HomeCard
                )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {


                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = HomeIconBox
                        ) {

                            Box(
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    text = "♬",
                                    color = HomeTeal,
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Column {

                            Text(
                                text = "Acoustic Guitar",
                                color = HomeText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(2.dp)
                            )

                            Text(
                                text = stringResource(
                                    R.string.current_tuning,
                                    preset.name,
                                    preset.midiNotes.joinToString(" ") {
                                        formatNote(
                                            noteNameForMidi(it, preset.preferFlats, includeOctave = false),
                                            notationSystem
                                        )
                                    }
                                ),
                                color = HomeSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    // change instruments

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Change Instrument",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),

                        shape = RoundedCornerShape(24.dp),

                        border = BorderStroke(
                            width = 1.dp,
                            color = HomeTeal
                        ),

                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = HomeTeal
                        ),

                        contentPadding = PaddingValues(
                            horizontal = 12.dp
                        )
                    ) {

                        Text(
                            text = "▣",
                            color = HomeTeal,
                            fontSize = 16.sp
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text(
                            text = "Change Instrument",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // tuning modes title
            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "TUNING MODES",
                color = HomeSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // Auto tune

            HomeModeCard(
                title = "Auto Tune",
                description = "Hands-free automatic string detection",
                symbol = "♩",
                selected = true,
                onClick = {
                    onStartTuning(TuningMode.AUTO)
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            //manual

            HomeModeCard(
                title = "Manual Tune",
                description = "Select and tune strings individually",
                symbol = "〰",
                onClick = {
                    onStartTuning(TuningMode.MANUAL)
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            //chromatic

            HomeModeCard(
                title = "Chromatic Tuner",
                description = "Tune to any of the 12 chromatic pitches",
                symbol = "♪",
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Chromatic Tuner coming soon",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            /*
             * ============================
             * METRONOME
             * ============================
             */

            HomeModeCard(
                title = "Metronome",
                description = "Keep rhythm with visual and audio beats",
                symbol = "♩",
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Metronome coming soon",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }
    }
}




@Composable
private fun HomeModeCard(
    title: String,
    description: String,
    symbol: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(
                onClick = onClick
            ),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                HomeSelected
            } else {
                HomeCard
            }
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 16.dp
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            /*
             * ICON CONTAINER
             */

            Surface(
                modifier = Modifier.size(40.dp),

                shape = RoundedCornerShape(12.dp),

                color = if (selected) {
                    HomeTeal
                } else {
                    HomeIconBox
                }
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = symbol,

                        color = if (selected) {
                            HomeBackground
                        } else {
                            HomeTeal
                        },

                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            /*
             * TEXT
             */

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    color = HomeText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = description,

                    color = if (selected) {
                        HomeText.copy(alpha = 0.78f)
                    } else {
                        HomeSecondary
                    },

                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            /*
             * ARROW
             */

            Text(
                text = "›",

                color = if (selected) {
                    HomeText
                } else {
                    HomeSecondary
                },

                fontSize = 28.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}
