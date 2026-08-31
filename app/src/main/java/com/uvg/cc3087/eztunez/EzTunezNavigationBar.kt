package com.uvg.cc3087.eztunez

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
        containerColor = Color(0xFF1B191E)
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
                    imageVector = Icons.Default.List,
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
        selectedIconColor = Color(0xFF50D0C8),
        selectedTextColor = Color(0xFF50D0C8),
        indicatorColor = Color(0xFF006B63),
        unselectedIconColor = Color(0xFF99959E),
        unselectedTextColor = Color(0xFF99959E)
    )
}
//