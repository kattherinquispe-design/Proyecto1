package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    onRegister: (
        String,
        String,
        String
    ) -> Boolean,
    onBack: () -> Unit
) {

    var name by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    var registrationSuccess by remember {
        mutableStateOf(false)
    }


    // =========================================================
    // INTERFAZ
    // =========================================================

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text =
                "Crear cuenta",

            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )


        Spacer(
            modifier =
                Modifier.height(20.dp)
        )


        // =====================================================
        // NOMBRE
        // =====================================================

        OutlinedTextField(

            value =
                name,

            onValueChange = {

                name = it
                message = ""
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Nombre completo")
            },

            singleLine = true
        )


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        // =====================================================
        // CORREO
        // =====================================================

        OutlinedTextField(

            value =
                email,

            onValueChange = {

                email = it
                message = ""
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Correo electrónico")
            },

            singleLine = true
        )


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        // =====================================================
        // CONTRASEÑA
        // =====================================================

        OutlinedTextField(

            value =
                password,

            onValueChange = {

                password = it
                message = ""
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Contraseña")
            },

            visualTransformation =
                PasswordVisualTransformation(),

            singleLine = true
        )


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        Text(
            text =
                "La contraseña debe tener al menos 8 caracteres.",

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        // =====================================================
        // CONFIRMAR CONTRASEÑA
        // =====================================================

        OutlinedTextField(

            value =
                confirmPassword,

            onValueChange = {

                confirmPassword = it
                message = ""
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Confirmar contraseña")
            },

            visualTransformation =
                PasswordVisualTransformation(),

            singleLine = true
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // =====================================================
        // MENSAJE
        // =====================================================

        if (
            message.isNotEmpty()
        ) {

            Text(

                text =
                    message,

                color =
                    if (
                        registrationSuccess
                    ) {

                        MaterialTheme
                            .colorScheme
                            .primary

                    } else {

                        MaterialTheme
                            .colorScheme
                            .error
                    }
            )


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )
        }


        // =====================================================
        // REGISTRAR
        // =====================================================

        Button(

            onClick = {

                when {

                    name.trim().isEmpty() -> {

                        message =
                            "Escribe tu nombre."
                    }


                    !android.util.Patterns
                        .EMAIL_ADDRESS
                        .matcher(
                            email.trim()
                        )
                        .matches() -> {

                        message =
                            "Escribe un correo válido."
                    }


                    password.length < 8 -> {

                        message =
                            "La contraseña debe tener al menos 8 caracteres."
                    }


                    password !=
                            confirmPassword -> {

                        message =
                            "Las contraseñas no coinciden."
                    }


                    else -> {

                        val success =
                            onRegister(
                                name.trim(),
                                email.trim(),
                                password
                            )


                        if (success) {

                            registrationSuccess =
                                true

                            message =
                                "Cuenta creada correctamente. Ahora inicia sesion."

                            // Limpiar formulario
                            name = ""
                            email = ""
                            password = ""
                            confirmPassword = ""

                        } else {

                            registrationSuccess =
                                false

                            message =
                                "Ese correo ya tiene una cuenta. Ese correo ya tiene una cuenta."
                        }
                    }
                }
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Crear cuenta"
            )
        }


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        // =====================================================
        // IR AL LOGIN
        // =====================================================

        OutlinedButton(

            onClick =
                onBack,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Ya tengo una cuenta"
            )
        }
    }
}