package com.uvg.cc3087.eztunez

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun TuningPresetsScreen(
    uiState: TuningPresetsUiState,
    onSelectFilter: (PresetFilter) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onCreateCustom: () -> Unit = {},
    onEditCustom: (Int) -> Unit = {},
    onDeleteCustom: (Int) -> Unit = {},
    onRetrySelection: () -> Unit = {}
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val searchMessage = stringResource(R.string.search)
    val createDescription = stringResource(R.string.create_custom_tuning)
    val density = LocalDensity.current
    var createButtonHeight by remember { mutableStateOf(56.dp) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ScreenHeader(title = stringResource(R.string.tuning_presets), trailingContent = {
                IconButton(onClick = { scope.launch { snackbar.showSnackbar(searchMessage) } }) {
                    Icon(Icons.Default.Search, contentDescription = searchMessage)
                }
            })
        },
        bottomBar = { EzTunezNavigationBar(AppScreen.PRESETS, onNavigate) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateCustom,
                modifier = Modifier
                    .onSizeChanged { createButtonHeight = with(density) { it.height.toDp() } }
                    .semantics { contentDescription = createDescription },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(stringResource(R.string.create_custom), fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            // Filters can scroll out of the way in short windows. Bottom space
            // lets the last card's actions scroll clear of the floating button.
            LazyColumn(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = createButtonHeight + 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.selectionSaveFailed) {
                    item {
                        Text(stringResource(R.string.custom_selection_error), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetrySelection) { Text(stringResource(R.string.retry)) }
                    }
                }
                item {
                    PresetFilterBar(uiState.selectedFilter, onSelectFilter)
                }
                if (uiState.presets.isEmpty()) item { Text(stringResource(R.string.custom_empty)) }
                items(uiState.presets, key = { it.id }) { preset ->
                    val selectedMessage = stringResource(R.string.preset_selected, preset.localizedName())
                    TuningPresetCard(
                        preset, preset.id == uiState.selectedPresetId,
                        onClick = {
                            onSelectPreset(preset.id)
                            scope.launch { snackbar.showSnackbar(selectedMessage) }
                        },
                        onEdit = if (preset.category == PresetCategory.CUSTOM) ({ onEditCustom(preset.id) }) else null,
                        onDelete = if (preset.category == PresetCategory.CUSTOM) ({ onDeleteCustom(preset.id) }) else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetFilterBar(
    selectedFilter: PresetFilter,
    onSelectFilter: (PresetFilter) -> Unit
) {
    val segmentShape = RoundedCornerShape(10.dp)
    val minimumSegmentWidth = 96.dp * LocalDensity.current.fontScale
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for ((filter, label) in listOf(
                PresetFilter.ALL to R.string.all_presets,
                PresetFilter.GUITAR to R.string.filter_guitar,
                PresetFilter.CUSTOM to R.string.filter_custom
            )) {
                val isSelected = selectedFilter == filter
                // Equal-width segments in each row; additional filters and
                // enlarged labels wrap instead of squeezing the text.
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = minimumSegmentWidth)
                        .heightIn(min = 48.dp)
                        .clip(segmentShape)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelectFilter(filter) }
                        ),
                    shape = segmentShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                        else androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                ) {
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(label),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TuningPresetCard(
    preset: TuningPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.preset_title, preset.localizedName(),
                        if (preset.category == PresetCategory.CUSTOM) {
                            preset.midiNotes.joinToString(" ") { noteNameForMidi(it) }
                        } else preset.tuning),
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) Icon(Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.currently_selected_description),
                    tint = MaterialTheme.colorScheme.primary)
            }
            if (preset.localizedDescription().isNotBlank()) {
                Text(preset.localizedDescription(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onEdit != null && onDelete != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.custom_edit)) }
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.custom_delete)) }
                }
            }
            Text(
                stringResource(if (isSelected) R.string.currently_selected else R.string.select_tuning),
                modifier = Modifier.align(Alignment.End), fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
