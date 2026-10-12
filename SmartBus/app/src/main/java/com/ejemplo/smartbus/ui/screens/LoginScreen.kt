package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.ui.theme.*

@Composable
fun LoginScreen(
    errorMessage: String?,
    onLogin: (String, String) -> Unit,
    onRegister: () -> Unit
) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var showPassword by remember {
        mutableStateOf(false)
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    SmartBusBackground
                )
                .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(55.dp)
        )

        Text(
            text = "Ruta",
            style =
                MaterialTheme.typography
                    .displayLarge
        )

        Text(
            text = "SmartBus",
            style =
                MaterialTheme.typography
                    .headlineLarge,
            color = SmartBusBlueDark
        )

        Text(
            text =
                "Tu ruta, tus puntos, tus recompensas",
            color =
                SmartBusTextSecondary
        )

        Spacer(
            modifier =
                Modifier.height(45.dp)
        )

        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Iniciar sesión",
                style =
                    MaterialTheme.typography
                        .headlineMedium,
                color = SmartBusText
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Text(
                text = "Correo electrónico",
                color = SmartBusText
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                modifier =
                    Modifier.fillMaxWidth(),
                placeholder = {
                    Text("tu@correo.com")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Email,
                        contentDescription =
                            null
                    )
                },
                singleLine = true,
                shape =
                    RoundedCornerShape(12.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Text(
                text = "Contraseña",
                color = SmartBusText
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                modifier =
                    Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription =
                            null
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            showPassword =
                                !showPassword
                        }
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription =
                                null
                        )
                    }
                },
                visualTransformation =
                    if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                singleLine = true,
                shape =
                    RoundedCornerShape(12.dp)
            )

            if (
                !errorMessage
                    .isNullOrBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text = errorMessage,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Button(
                onClick = {
                    onLogin(
                        email,
                        password
                    )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                shape =
                    RoundedCornerShape(14.dp)
            ) {

                Text(
                    text = "Iniciar sesión"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.Center,
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    text =
                        "¿No tienes una cuenta? "
                )

                TextButton(
                    onClick =
                        onRegister
                ) {

                    Text(
                        text = "Regístrate",
                        color =
                            SmartBusBlue
                    )
                }
            }
        }
    }
}