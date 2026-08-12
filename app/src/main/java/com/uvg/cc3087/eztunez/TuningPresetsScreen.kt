package com.uvg.cc3087.eztunez

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val Background = Color(0xFF0D0D0F)
private val HeaderBackground = Color(0xFF1B191E)
private val CardBackground = Color(0xFF242328)
private val TabBackground = Color(0xFF242328)
private val Teal = Color(0xFF50D0C8)
private val PrimaryText = Color(0xFFF4F1F5)
private val SecondaryText = Color(0xFF99959E)

enum class PresetFilter {
    ALL,
    GUITAR,
    CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuningPresetsScreen(
    onNavigate: (AppScreen) -> Unit
) {
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val scope = rememberCoroutineScope()

    var selectedFilter by remember {
        mutableStateOf(PresetFilter.ALL)
    }

    // Standard tuning starts selected.
    // When another preset is clicked, this ID changes.
    var selectedPresetId by remember {
        mutableIntStateOf(1)
    }

    val presets = remember {
        listOf(
            TuningPreset(
                id = 1,
                name = "Standard",
                tuning = "EADGBE",
                description = "Common layout suitable for most acoustic & electric guitars",
                category = PresetCategory.GUITAR
            ),
            TuningPreset(
                id = 2,
                name = "Drop D",
                tuning = "DADGBE",
                description = "Heavy low response, lower the 6th string a whole step",
                category = PresetCategory.GUITAR
            ),
            TuningPreset(
                id = 3,
                name = "Open G",
                tuning = "DGDGBD",
                description = "Popular for slide blues playing & folk compositions",
                category = PresetCategory.GUITAR
            ),
            TuningPreset(
                id = 4,
                name = "DADGAD Tuning",
                tuning = "",
                description = "Celtic folk tuning with rich natural resonance",
                category = PresetCategory.GUITAR
            ),
            TuningPreset(
                id = 5,
                name = "Half Step Down",
                tuning = "Eb Ab Db Gb Bb Eb",
                description = "Standard relative pitches, lowered by one semitone",
                category = PresetCategory.GUITAR
            )
        )
    }

    /*
     * Filter the list BEFORE giving it to LazyColumn.
     * This keeps calculations out of the item Composable.
     */
    val filteredPresets = remember(
        selectedFilter,
        presets
    ) {
        when (selectedFilter) {

            PresetFilter.ALL -> {
                presets
            }

            PresetFilter.GUITAR -> {
                presets.filter {
                    it.category == PresetCategory.GUITAR
                }
            }

            PresetFilter.CUSTOM -> {
                presets.filter {
                    it.category == PresetCategory.CUSTOM
                }
            }
        }
    }

    Scaffold(
        containerColor = Background,

        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },

        bottomBar = {
            EzTunezNavigationBar(
                selectedScreen = AppScreen.PRESETS,
                onNavigate = onNavigate
            )
        },

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tuning Presets",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },




                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Search"
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PrimaryText,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBackground
                )
            )
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Create Custom"
                        )
                    }
                },

                containerColor = Teal,
                contentColor = Background,
                shape = RoundedCornerShape(16.dp),

                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create custom tuning"
                    )
                },

                text = {
                    Text(
                        text = "Create Custom",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            PresetFilterBar(
                selectedFilter = selectedFilter,
                onFilterSelected = {
                    selectedFilter = it
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),

                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 24.dp,
                    bottom = 120.dp
                ),

                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                items(
                    items = filteredPresets,


                    key = { preset ->
                        preset.id
                    }
                ) { preset ->

                    TuningPresetCard(
                        preset = preset,

                        // The card checks whether its ID matches the currently selected ID.
                        isSelected = preset.id == selectedPresetId,

                        onClick = {

                            selectedPresetId = preset.id

                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "${preset.name} selected",
                                    duration = SnackbarDuration.Short

                                )
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun PresetFilterBar(
    selectedFilter: PresetFilter,
    onFilterSelected: (PresetFilter) -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),

        shape = RoundedCornerShape(12.dp),

        colors = CardDefaults.cardColors(
            containerColor = TabBackground
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {

            FilterButton(
                text = "All Presets",
                selected = selectedFilter == PresetFilter.ALL,
                modifier = Modifier.weight(1f),
                onClick = {
                    onFilterSelected(
                        PresetFilter.ALL
                    )
                }
            )

            FilterButton(
                text = "Guitar",
                selected = selectedFilter == PresetFilter.GUITAR,
                modifier = Modifier.weight(1f),
                onClick = {
                    onFilterSelected(
                        PresetFilter.GUITAR
                    )
                }
            )

            FilterButton(
                text = "Custom",
                selected = selectedFilter == PresetFilter.CUSTOM,
                modifier = Modifier.weight(1f),
                onClick = {
                    onFilterSelected(
                        PresetFilter.CUSTOM
                    )
                }
            )
        }
    }
}

@Composable
private fun FilterButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(
                onClick = onClick
            ),

        color = if (selected) {
            Teal
        } else {
            Color.Transparent
        },

        shape = RoundedCornerShape(10.dp)
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = text,

                style = MaterialTheme.typography.bodyLarge,

                fontWeight = if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },

                color = if (selected) {
                    Background
                } else {
                    PrimaryText
                }
            )
        }
    }
}


@Composable
fun TuningPresetCard(
    preset: TuningPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            ),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),

        // Only the currently selected card receives the teal border.
        border = if (isSelected) {
            BorderStroke(
                width = 1.dp,
                color = Teal
            )
        } else {
            null
        }
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = if (preset.tuning.isBlank()) {
                        preset.name
                    } else {
                        "${preset.name} — ${preset.tuning}"
                    },

                    modifier = Modifier.weight(1f),

                    style = MaterialTheme.typography.titleLarge,

                    fontWeight = FontWeight.Bold,

                    color = PrimaryText
                )

                // Checkmark only appears  on the currently selected preset.
                if (isSelected) {

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Currently selected",
                        tint = Teal,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = preset.description,
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryText
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = if (isSelected) {
                    "Currently Selected"
                } else {
                    "Select Tuning"
                },

                modifier = Modifier.align(
                    Alignment.End
                ),

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.Bold,

                color = if (isSelected) {
                    Teal
                } else {
                    SecondaryText
                }
            )
        }
    }
}