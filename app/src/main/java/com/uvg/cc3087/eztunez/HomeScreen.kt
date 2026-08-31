package com.uvg.cc3087.eztunez

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val HomeBackground = Color(0xFF0D0D0F)
private val HomeHeader = Color(0xFF1B191E)
private val HomeCard = Color(0xFF242328)
private val HomeTeal = Color(0xFF50D0C8)
private val HomeText = Color(0xFFF4F1F5)
private val HomeSecondaryText = Color(0xFF99959E)
private val HomeBorder = Color(0xFF464149)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (AppScreen) -> Unit
) {

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val scope = rememberCoroutineScope()

    /*
     * Temporary instrument state.
     *
     * Later this can be replaced with a proper
     * instrument selection screen.
     */
    val instruments = remember {
        listOf(
            "Acoustic Guitar",
            "Electric Guitar",
            "Bass Guitar"
        )
    }

    var selectedInstrumentIndex by remember {
        mutableIntStateOf(0)
    }

    val selectedInstrument =
        instruments[selectedInstrumentIndex]

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
                        text = "TunePro",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = HomeText
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
                onNavigate = onNavigate,
            )
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = 16.dp
                )
        ) {

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            /*
             * INSTRUMENT
             */

            Text(
                text = selectedInstrument,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = HomeText
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Standard Tuning (E A D G B E)",
                style = MaterialTheme.typography.bodyLarge,
                color = HomeSecondaryText
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            /*
             * CHANGE INSTRUMENT
             */

            OutlinedButton(
                onClick = {

                    selectedInstrumentIndex =
                        (selectedInstrumentIndex + 1) %
                                instruments.size

                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message =
                                "Changed to ${instruments[selectedInstrumentIndex]}",
                            duration = SnackbarDuration.Short
                        )
                    }
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),

                shape = RoundedCornerShape(12.dp),

                border = BorderStroke(
                    width = 1.dp,
                    color = HomeBorder
                ),

                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HomeText
                )
            ) {

                Text(
                    text = "Change Instrument",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            /*
             * TUNING MODES
             */

            Text(
                text = "Tuning Modes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = HomeText
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            /*
             * AUTO + MANUAL
             */

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                TuningModeCard(
                    title = "Auto Tune",
                    description = "Hands-free automatic string detection",
                    symbol = "A",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Auto Tune selected",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )

                TuningModeCard(
                    title = "Manual Tune",
                    description = "Select and tune strings individually",
                    symbol = "M",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Manual Tune selected",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            /*
             * CHROMATIC + METRONOME
             */

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                TuningModeCard(
                    title = "Chromatic Tuner",
                    description = "Tune to any of the 12 chromatic pitches",
                    symbol = "C",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Chromatic Tuner selected",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )

                TuningModeCard(
                    title = "Metronome",
                    description = "Keep rhythm with visual and audio beats",
                    symbol = "♩",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Metronome selected",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }
        }
    }
}


/*
 * Individual mode card.
 *
 * Kept separate from HomeScreen so the four
 * options share the same design.
 */
@Composable
private fun TuningModeCard(
    title: String,
    description: String,
    symbol: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .height(180.dp)
            .clickable(
                onClick = onClick
            ),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = HomeCard
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            /*
             * Simple graphic without adding
             * more Material icon dependencies.
             */
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = HomeTeal.copy(
                    alpha = 0.15f
                )
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = HomeTeal
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HomeText,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = HomeSecondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}