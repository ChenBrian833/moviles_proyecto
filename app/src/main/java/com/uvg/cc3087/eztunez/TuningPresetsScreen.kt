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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val Background: Color @Composable get() = MaterialTheme.colorScheme.background
private val HeaderBackground: Color @Composable get() = MaterialTheme.colorScheme.surface
private val CardBackground: Color @Composable get() = MaterialTheme.colorScheme.surface
private val TabBackground: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val Teal: Color @Composable get() = MaterialTheme.colorScheme.primary
private val PrimaryText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val SecondaryText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuningPresetsScreen(
    uiState: TuningPresetsUiState,
    onSelectFilter: (PresetFilter) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val searchMessage = stringResource(R.string.search)
    val createCustomMessage = stringResource(R.string.create_custom)
    val scope = rememberCoroutineScope()

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
                        text = stringResource(R.string.tuning_presets),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },




                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(searchMessage)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search),
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
                        snackbarHostState.showSnackbar(createCustomMessage)
                    }
                },

                containerColor = Teal,
                contentColor = Background,
                shape = RoundedCornerShape(16.dp),

                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.create_custom_tuning)
                    )
                },

                text = {
                    Text(
                        text = stringResource(R.string.create_custom),
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
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = {
                    onSelectFilter(it)
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
                    items = uiState.presets,


                    key = { preset ->
                        preset.id
                    }
                ) { preset ->
                    val selectedMessage = stringResource(R.string.preset_selected, preset.localizedName())

                    TuningPresetCard(
                        preset = preset,

                        // The card checks whether its ID matches the currently selected ID.
                        isSelected = preset.id == uiState.selectedPresetId,

                        onClick = {

                            onSelectPreset(preset.id)

                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    selectedMessage,
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
                text = stringResource(R.string.all_presets),
                selected = selectedFilter == PresetFilter.ALL,
                modifier = Modifier.weight(1f),
                onClick = {
                    onFilterSelected(
                        PresetFilter.ALL
                    )
                }
            )

            FilterButton(
                text = stringResource(R.string.filter_guitar),
                selected = selectedFilter == PresetFilter.GUITAR,
                modifier = Modifier.weight(1f),
                onClick = {
                    onFilterSelected(
                        PresetFilter.GUITAR
                    )
                }
            )

            FilterButton(
                text = stringResource(R.string.filter_custom),
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
                        preset.localizedName()
                    } else {
                        stringResource(R.string.preset_title, preset.localizedName(), preset.tuning)
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
                        contentDescription = stringResource(R.string.currently_selected_description),
                        tint = Teal,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = preset.localizedDescription(),
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryText
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = if (isSelected) {
                    stringResource(R.string.currently_selected)
                } else {
                    stringResource(R.string.select_tuning)
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
