package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.SmartBusSession
import com.ejemplo.smartbus.data.SmartBusStatistics
import com.ejemplo.smartbus.ui.components.*
import com.ejemplo.smartbus.ui.theme.*

/**
 * Iniciales del usuario para el avatar.
 */
private fun initialsOf(session: SmartBusSession?): String {

    val name = session?.name?.trim().orEmpty()

    if (name.isBlank()) {
        return "S"
    }

    val parts = name.split(" ").filter { it.isNotBlank() }

    return when {
        parts.size >= 2 ->
            "${parts[0].first()}${parts[1].first()}".uppercase()

        else ->
            parts.first().take(2).uppercase()
    }
}

@Composable
fun ProfileScreen(
    points: Int,
    statistics: SmartBusStatistics,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onRedeemedClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    session: SmartBusSession? = null,
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onBecomeDriverClick: () -> Unit = {},
    onTabSelected: (SmartBusTab) -> Unit = {}
) {

    val level =
        (points / 1000) + 1

    val progress =
        ((points % 1000)
            .toFloat() / 1000f)
            .coerceIn(0f, 1f)

    Scaffold(
        containerColor =
            SmartBusBackground,

        bottomBar = {
            SmartBusBottomBar(
                selectedTab =
                    SmartBusTab.PROFILE,
                onTabSelected = { tab -> onTabSelected(tab) }
            )
        }
    ) { paddingValues ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(18.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Mi perfil",
                    style =
                        MaterialTheme.typography
                            .headlineMedium
                )

                IconButton(
                    onClick = onSettingsClick
                ) {

                    Icon(
                        Icons.Default.Settings,
                        contentDescription =
                            "Configuración"
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(75.dp)
                            .clip(CircleShape)
                            .background(
                                SmartBusBlue
                                    .copy(
                                        alpha = 0.12f
                                    )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    // La foto de Google queda guardada en la sesion; el avatar
                    // muestra iniciales hasta que se cargue desde disco.
                    Text(
                        text = initialsOf(session),
                        style =
                            MaterialTheme.typography
                                .headlineSmall,
                        color = SmartBusBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    // Sin sesion todavia no hay un nombre que mostrar, asi
                    // que se ofrece uno provisional y se explica que puede
                    // entrar con su cuenta para no perder sus puntos.
                    Text(
                        text = session?.name
                            ?.takeIf { it.isNotBlank() }
                            ?: "Usuario SmartBus",
                        style =
                            MaterialTheme.typography
                                .titleLarge
                    )

                    Text(
                        text = when {

                            session == null ->
                                "Entra para guardar tus puntos"

                            session.email.isNotBlank() ->
                                session.email

                            else ->
                                "Modo invitado"
                        },
                        color =
                            SmartBusTextSecondary
                    )
                }

                if (session != null) {

                    IconButton(
                        onClick = onSignOutClick
                    ) {

                        Icon(
                            Icons.Default.Logout,
                            contentDescription =
                                "Cerrar sesion",
                            tint = SmartBusTextSecondary
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            if (session == null) {

                Button(
                    onClick = onSignInClick,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Icon(
                        Icons.Default.Login,
                        contentDescription = null,
                        modifier =
                            Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text("Iniciar sesion")
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )
            }

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(18.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            SmartBusBlue
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "$points puntos",
                        style =
                            MaterialTheme.typography
                                .headlineMedium,
                        color = Color.White
                    )

                    Text(
                        text =
                            "Nivel $level • Viajero frecuente",
                        color =
                            Color.White.copy(
                                alpha = 0.9f
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )

                    LinearProgressIndicator(
                        progress = {
                            progress
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(
                                    RoundedCornerShape(
                                        10.dp
                                    )
                                ),
                        color = Color.White,
                        trackColor =
                            Color.White.copy(
                                alpha = 0.25f
                            )
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            DriverModeButton(onClick = onBecomeDriverClick)

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Text(
                text = "Resumen",
                style =
                    MaterialTheme.typography
                        .labelMedium,
                color =
                    SmartBusTextSecondary,
                modifier =
                    Modifier.padding(
                        start = 4.dp,
                        bottom = 8.dp
                    )
            )

            Row(horizontalArrangement =
                Arrangement.spacedBy(10.dp)) {

                StatBox(
                    value = "${statistics.boardingCount}",
                    label = "Abordajes",
                    modifier = Modifier.weight(1f)
                )

                StatBox(
                    value = "${statistics.rewardsRedeemed}",
                    label = "Canjes",
                    modifier = Modifier.weight(1f)
                )

                StatBox(
                    value = "${statistics.stopsUsed}",
                    label = "Paraderos",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            ProfileOption(
                icon = Icons.Default.History,
                title = "Historial de visitas",
                subtitle = "${statistics.boardingCount} abordajes registrados",
                onClick = onHistoryClick
            )

            ProfileOption(
                icon = Icons.Default.CardGiftcard,
                title = "Recompensas canjeadas",
                subtitle = "${statistics.rewardsRedeemed} canjes realizados",
                onClick = onRedeemedClick
            )

            ProfileOption(
                icon = Icons.Default.EmojiEvents,
                title = "Mis logros",
                subtitle = "Nivel $level alcanzado",
                onClick = onAchievementsClick
            )

            ProfileOption(
                icon = Icons.Default.Settings,
                title = "Configuración",
                subtitle = "Seguimiento, avisos y datos",
                onClick = onSettingsClick
            )

            ProfileOption(
                icon = Icons.Default.HelpOutline,
                title = "Ayuda",
                subtitle = "Preguntas frecuentes",
                onClick = onHelpClick
            )
        }
    }
}

/**
 * Acceso al modo conductor.
 *
 * Es el boton mas visible del perfil a proposito: quien quiera
 * trabajar tiene que encontrarlo sin dudar. El amarillo de la marca
 * lo separa del resto de tarjetas blancas.
 */
@Composable
private fun DriverModeButton(onClick: () -> Unit) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SmartBusYellow
        )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.30f)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = SmartBusText,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = "Modo conductor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmartBusText
                )

                Text(
                    text = "Registra tu vehículo y sal a trabajar",
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusText.copy(alpha = 0.75f)
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = SmartBusText,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun StatBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = value,
                style = MaterialTheme.typography
                    .headlineSmall,
                color = SmartBusBlue
            )

            Text(
                text = label,
                style = MaterialTheme.typography
                    .bodySmall,
                color = SmartBusTextSecondary
            )
        }
    }
}

@Composable
private fun ProfileOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp
                )
                .clickable(onClick = onClick),
        shape =
            RoundedCornerShape(13.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SmartBusBlue
            )

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = title
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography
                        .bodySmall,
                    color =
                        SmartBusTextSecondary
                )
            }

            Icon(
                imageVector =
                    Icons.Default
                        .ArrowForwardIos,
                contentDescription = null,
                modifier =
                    Modifier.size(15.dp),
                tint =
                    SmartBusTextLight
            )
        }
    }
}
