package com.uvg.cc3087.eztunez

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val HomeBackground: Color @Composable get() = MaterialTheme.colorScheme.background
private val HomeCard: Color @Composable get() = MaterialTheme.colorScheme.surface
private val HomeIconBox: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val HomeTeal: Color @Composable get() = MaterialTheme.colorScheme.primary
private val HomeSelected: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
private val HomeText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val HomeSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

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

    val changeInstrumentMessage = stringResource(R.string.change_instrument)
    val chromaticMessage = stringResource(R.string.chromatic_coming_soon)
    val metronomeMessage = stringResource(R.string.metronome_coming_soon)
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = HomeBackground,

        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },

        topBar = {
            ScreenHeader(
                title = stringResource(R.string.app_name),
                trailingContent = {
                    Text(
                        text = stringResource(R.string.symbol_music),
                        color = HomeText,
                        fontSize = 24.sp
                    )
                }
            )
        },

        bottomBar = {
            EzTunezNavigationBar(
                selectedScreen = AppScreen.HOME,
                onNavigate = onNavigate
            )
        }

    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp)
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
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                shape = CircleShape,
                                color = HomeIconBox
                            ) {

                                Box(
                                    contentAlignment = Alignment.Center
                                ) {

                                    Text(
                                        text = stringResource(R.string.symbol_music),
                                        color = HomeTeal,
                                        fontSize = 24.sp
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.width(12.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {

                                Text(
                                    text = stringResource(R.string.instrument_acoustic_guitar),
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
                                        preset.localizedName(),
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
                                        message = changeInstrumentMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),

                            shape = RoundedCornerShape(24.dp),

                            border = BorderStroke(
                                width = 1.dp,
                                color = HomeTeal
                            ),

                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HomeTeal
                            ),

                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            )
                        ) {

                            Text(
                                text = stringResource(R.string.symbol_instrument),
                                color = HomeTeal,
                                fontSize = 16.sp
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text = stringResource(R.string.change_instrument),
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                    text = stringResource(R.string.tuning_modes),
                    color = HomeSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                // Auto tune

                HomeModeCard(
                    title = stringResource(R.string.mode_auto),
                    description = stringResource(R.string.mode_auto_description),
                    symbol = stringResource(R.string.symbol_auto),
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
                    title = stringResource(R.string.mode_manual),
                    description = stringResource(R.string.mode_manual_description),
                    symbol = stringResource(R.string.symbol_manual),
                    onClick = {
                        onStartTuning(TuningMode.MANUAL)
                    }
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                //chromatic

                HomeModeCard(
                    title = stringResource(R.string.mode_chromatic),
                    description = stringResource(R.string.mode_chromatic_description),
                    symbol = stringResource(R.string.symbol_chromatic),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = chromaticMessage,
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
                    title = stringResource(R.string.metronome),
                    description = stringResource(R.string.metronome_description),
                    symbol = stringResource(R.string.symbol_auto),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = metronomeMessage,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }
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
            .heightIn(min = 68.dp)
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
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            /*
             * ICON CONTAINER
             */

            Surface(
                modifier = Modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp),

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

                    fontSize = 12.sp
                )
            }

            /*
             * ARROW
             */

            Text(
                text = stringResource(R.string.symbol_chevron),

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
