package com.uvg.cc3087.eztunez

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

enum class AppScreen {
    HOME,
    TUNE,
    PRESETS,
    SETTINGS
}

@Composable
fun EzTunezNavigationBar(
    selectedScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {

        NavigationBarItem(
            selected = selectedScreen == AppScreen.HOME,
            onClick = {
                onNavigate(AppScreen.HOME)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Tune"
                )
            },




            label = {
                Text("Home")
            },
            colors = navigationColors()
        )

        NavigationBarItem(
            selected = selectedScreen == AppScreen.TUNE,
            onClick = {
                onNavigate(AppScreen.TUNE)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Tune"
                )
            },
            label = {
                Text("Tune")
            },
            colors = navigationColors()
        )

        NavigationBarItem(
            selected = selectedScreen == AppScreen.PRESETS,
            onClick = {
                onNavigate(AppScreen.PRESETS)
            },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Presets"
                )
            },
            label = {
                Text("Presets")
            },
            colors = navigationColors()
        )

        NavigationBarItem(
            selected = selectedScreen == AppScreen.SETTINGS,
            onClick = {
                onNavigate(AppScreen.SETTINGS)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            },
            label = {
                Text("Settings")
            },
            colors = navigationColors()
        )
    }
}

@Composable
private fun navigationColors(): NavigationBarItemColors {
    return NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
