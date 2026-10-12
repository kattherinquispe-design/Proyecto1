package com.ejemplo.smartbus.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class SmartBusTab {
    HOME,
    MAP,
    REWARDS,
    RANKING,
    PROFILE
}

@Composable
fun SmartBusBottomBar(
    selectedTab: SmartBusTab,
    onTabSelected: (SmartBusTab) -> Unit
) {
    NavigationBar {

        NavigationBarItem(
            selected = selectedTab == SmartBusTab.HOME,
            onClick = {
                onTabSelected(SmartBusTab.HOME)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Inicio"
                )
            },
            label = {
                Text("Inicio")
            }
        )

        NavigationBarItem(
            selected = selectedTab == SmartBusTab.MAP,
            onClick = {
                onTabSelected(SmartBusTab.MAP)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Mapa"
                )
            },
            label = {
                Text("Mapa")
            }
        )

        NavigationBarItem(
            selected = selectedTab == SmartBusTab.REWARDS,
            onClick = {
                onTabSelected(SmartBusTab.REWARDS)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Premios"
                )
            },
            label = {
                Text("Premios")
            }
        )

        NavigationBarItem(
            selected = selectedTab == SmartBusTab.RANKING,
            onClick = {
                onTabSelected(SmartBusTab.RANKING)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Ranking"
                )
            },
            label = {
                Text("Ranking")
            }
        )

        NavigationBarItem(
            selected = selectedTab == SmartBusTab.PROFILE,
            onClick = {
                onTabSelected(SmartBusTab.PROFILE)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil"
                )
            },
            label = {
                Text("Perfil")
            }
        )
    }
}