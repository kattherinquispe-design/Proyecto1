package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejemplo.smartbus.ui.components.GoogleSignInButton
import com.ejemplo.smartbus.ui.components.GoogleSignInResult
import com.ejemplo.smartbus.ui.theme.AppPalette
import com.ejemplo.smartbus.ui.theme.SmartBusBackground
import com.ejemplo.smartbus.ui.theme.SmartBusBody
import com.ejemplo.smartbus.ui.theme.SmartBusDisplay
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary
import androidx.compose.ui.platform.LocalContext
import com.ejemplo.smartbus.data.PointsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * Pantalla de Acceso y Registro de MistiGo.
 * Sincronizada 100% con Firebase Authentication y Firestore (colecciones 'usuarios' y 'users').
 * Permite alternar estéticamente entre Iniciar Sesión y Registro con validación completa.
 */
@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onGoogleResult: (GoogleSignInResult) -> Unit,
    onSignInWithEmail: (email: String, name: String) -> Unit,
    onContinueAsGuest: (name: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 0 = Iniciar Sesión, 1 = Registrarse
    var authMode by remember { mutableStateOf(0) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var guestName by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SmartBusBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Botón volver
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = SmartBusText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cabecera estética
            Text(
                text = if (authMode == 0) "Iniciar Sesión" else "Crear Cuenta",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = SmartBusDisplay,
                fontWeight = FontWeight.Bold,
                color = SmartBusText
            )

            Text(
                text = if (authMode == 0) {
                    "Accede a tu cuenta de MistiGo para ver tus rutas, saldo y canjes."
                } else {
                    "Regístrate para acumular puntos por tus viajes y canjear premios."
                },
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = SmartBusBody,
                color = SmartBusTextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Selector Segmentado Estético (Tabs)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFE2E8F0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Tab 1: Iniciar Sesión
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (authMode == 0) AppPalette.primary else Color.Transparent)
                            .clickable {
                                authMode = 0
                                error = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Iniciar Sesión",
                            fontFamily = SmartBusDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (authMode == 0) Color.White else SmartBusTextSecondary
                        )
                    }

                    // Tab 2: Registrarse
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (authMode == 1) AppPalette.primary else Color.Transparent)
                            .clickable {
                                authMode = 1
                                error = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Registrarse",
                            fontFamily = SmartBusDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (authMode == 1) Color.White else SmartBusTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Formulario
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (authMode == 1) {
                        // Nombre solo en registro
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre completo") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = AppPalette.primary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Correo
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = AppPalette.primary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Contraseña
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AppPalette.primary)
                        },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (authMode == 1) ImeAction.Next else ImeAction.Done
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (authMode == 1) {
                        // Confirmar Contraseña
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirmar contraseña") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = AppPalette.primary)
                            },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Mensaje de Error
                    if (error != null) {
                        Text(
                            text = error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = SmartBusBody,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Botón Principal
                    Button(
                        onClick = {
                            val cleanEmail = email.trim().lowercase()
                            val cleanPass = password.trim()
                            val cleanName = name.trim()

                            if (cleanEmail.isBlank()) {
                                error = "Por favor ingresa tu correo electrónico."
                                return@Button
                            }
                            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                error = "Ingresa un formato de correo válido."
                                return@Button
                            }
                            if (cleanPass.length < 6) {
                                error = "La contraseña debe tener al menos 6 caracteres."
                                return@Button
                            }

                            if (authMode == 1) {
                                // MODO REGISTRO
                                if (cleanName.isBlank()) {
                                    error = "Por favor ingresa tu nombre completo."
                                    return@Button
                                }
                                if (cleanPass != confirmPassword.trim()) {
                                    error = "Las contraseñas no coinciden."
                                    return@Button
                                }

                                loading = true
                                error = null

                                val auth = FirebaseAuth.getInstance()
                                auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
                                    .addOnSuccessListener { result ->
                                        val uid = result.user?.uid ?: cleanEmail
                                        val now = System.currentTimeMillis()
                                        val userData = hashMapOf(
                                            "uid" to uid,
                                            "nombre" to cleanName,
                                            "correo" to cleanEmail,
                                            "rol" to "pasajero",
                                            "puntos" to 0,
                                            "fechaRegistro" to now,
                                            "updatedAt" to now,
                                            "proveedor" to "EMAIL",
                                            "activo" to true
                                        )

                                        val db = FirebaseFirestore.getInstance()
                                        // Guardamos unicamente en la coleccion 'usuarios' para evitar duplicados
                                        db.collection("usuarios").document(uid)
                                            .set(userData, com.google.firebase.firestore.SetOptions.merge())
                                        db.collection("users").document(uid).delete()

                                        loading = false
                                        onSignInWithEmail(cleanEmail, cleanName)
                                    }
                                    .addOnFailureListener { e ->
                                        loading = false
                                        val msg = e.message ?: "Error al registrar"
                                        error = if (msg.contains("already in use", ignoreCase = true)) {
                                            "Este correo ya está registrado. Selecciona 'Iniciar Sesión'."
                                        } else {
                                            msg
                                        }
                                    }
                            } else {
                                // MODO INICIAR SESIÓN
                                loading = true
                                error = null

                                val auth = FirebaseAuth.getInstance()
                                auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
                                    .addOnSuccessListener { result ->
                                        val uid = result.user?.uid ?: cleanEmail
                                        val emailKey = cleanEmail.lowercase().replace("@", "_").replace(".", "_")
                                        val accountKey = "email_${cleanEmail.lowercase()}"
                                        val db = FirebaseFirestore.getInstance()

                                        // Leer datos y puntos existentes del usuario desde Firestore ('usuarios')
                                        db.collection("usuarios").document(uid).get()
                                            .addOnSuccessListener { doc ->
                                                val savedName = doc.getString("nombre")
                                                    ?: doc.getString("name")
                                                val cloudPts = (doc.getLong("puntos") ?: doc.getLong("points"))?.toInt()

                                                if (cloudPts != null) {
                                                    PointsRepository(context).setCloudPointsForAccount(accountKey, cloudPts)
                                                    loading = false
                                                    onSignInWithEmail(cleanEmail, savedName ?: cleanEmail.substringBefore("@"))
                                                } else {
                                                    db.collection("usuarios").document(emailKey).get()
                                                        .addOnSuccessListener { emailDoc ->
                                                            val fallbackName = savedName
                                                                ?: emailDoc.getString("nombre")
                                                                ?: emailDoc.getString("name")
                                                                ?: cleanEmail.substringBefore("@")
                                                            val fallbackPts = (emailDoc.getLong("puntos") ?: emailDoc.getLong("points"))?.toInt()
                                                            if (fallbackPts != null) {
                                                                PointsRepository(context).setCloudPointsForAccount(accountKey, fallbackPts)
                                                            }
                                                            loading = false
                                                            onSignInWithEmail(cleanEmail, fallbackName)
                                                        }
                                                        .addOnFailureListener {
                                                            loading = false
                                                            onSignInWithEmail(cleanEmail, savedName ?: cleanEmail.substringBefore("@"))
                                                        }
                                                }
                                            }
                                            .addOnFailureListener {
                                                loading = false
                                                onSignInWithEmail(cleanEmail, cleanEmail.substringBefore("@"))
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        loading = false
                                        val msg = e.message ?: "Error al iniciar sesión"
                                        error = if (msg.contains("user-not-found", ignoreCase = true) || msg.contains("wrong-password", ignoreCase = true) || msg.contains("invalid-credential", ignoreCase = true)) {
                                            "Correo o contraseña incorrectos."
                                        } else {
                                            msg
                                        }
                                    }
                            }
                        },
                        enabled = !loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPalette.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (authMode == 0) "Iniciar Sesión" else "Crear Cuenta en MistiGo",
                                fontFamily = SmartBusDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Separador Google
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFCBD5E1))
                )
                Text(
                    text = "  o accede con Google  ",
                    color = SmartBusTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = SmartBusBody
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFCBD5E1))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botón de Google Sign-In
            GoogleSignInButton(
                onResult = onGoogleResult
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta de Modo Invitado
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Entrar como Invitado",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = SmartBusDisplay,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText
                    )
                    Text(
                        text = "Puedes probar MistiGo sin cuenta. Tus puntos y rutas se guardarán en este teléfono.",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = SmartBusBody,
                        color = SmartBusTextSecondary
                    )

                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { guestName = it },
                        label = { Text("¿Cómo te llamamos?") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = AppPalette.primary)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = { onContinueAsGuest(guestName.trim()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Continuar como invitado →",
                            fontFamily = SmartBusDisplay,
                            fontWeight = FontWeight.Bold,
                            color = AppPalette.primary
                        )
                    }
                }
            }
        }
    }
}
