package com.ejemplo.smartbus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.ui.theme.SmartBusNavyBar
import com.ejemplo.smartbus.ui.theme.SmartBusPink
import com.ejemplo.smartbus.ui.theme.SmartBusPinkLight
import com.ejemplo.smartbus.ui.theme.SmartBusTextLight

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

    NavigationBar(
        containerColor = SmartBusNavyBar,
        tonalElevation = 0.dp
    ) {

        SmartBusBarItem(
            selected = selectedTab == SmartBusTab.HOME,
            onClick = { onTabSelected(SmartBusTab.HOME) },
            icon = Icons.Outlined.Home,
            label = "Inicio"
        )

        SmartBusBarItem(
            selected = selectedTab == SmartBusTab.MAP,
            onClick = { onTabSelected(SmartBusTab.MAP) },
            icon = Icons.Outlined.Map,
            label = "Mapa"
        )

        SmartBusBarItem(
            selected = selectedTab == SmartBusTab.REWARDS,
            onClick = { onTabSelected(SmartBusTab.REWARDS) },
            icon = Icons.Outlined.Star,
            label = "Premios"
        )

        SmartBusBarItem(
            selected = selectedTab == SmartBusTab.RANKING,
            onClick = { onTabSelected(SmartBusTab.RANKING) },
            icon = Icons.Outlined.EmojiEvents,
            label = "Ranking"
        )

        SmartBusBarItem(
            selected = selectedTab == SmartBusTab.PROFILE,
            onClick = { onTabSelected(SmartBusTab.PROFILE) },
            icon = Icons.Outlined.Person,
            label = "Perfil"
        )
    }
}

@Composable
private fun RowScope.SmartBusBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {

    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) SmartBusPinkLight
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        label = {
            Text(
                text = label,
                fontWeight = if (selected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                }
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = SmartBusPink,
            selectedTextColor = SmartBusPink,
            indicatorColor = Color.Transparent,
            unselectedIconColor = SmartBusTextLight,
            unselectedTextColor = SmartBusTextLight
        )
    )
}
