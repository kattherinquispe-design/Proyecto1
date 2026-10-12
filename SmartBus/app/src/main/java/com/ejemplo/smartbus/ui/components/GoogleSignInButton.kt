package com.ejemplo.smartbus.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.AuthConfig
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

/**
 * Resultado de intentar entrar con Google.
 */
sealed interface GoogleSignInResult {
    data class Success(
        val userId: String,
        val name: String,
        val email: String,
        val photoUri: String?,
        val idToken: String
    ) : GoogleSignInResult

    data class Error(val message: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
}

/**
 * Boton de inicio de sesion con Google.
 *
 * Se pide un ID token de tipo Web (el Client ID de OAuth 2.0) porque es
 * el unico que Google acepta como "audience" del lado del servidor.
 * Si el Client ID no esta en local.properties, el boton lo dice en vez
 * de fallar en silencio.
 */
@Composable
fun GoogleSignInButton(
    onResult: (GoogleSignInResult) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {

    val context = LocalContext.current
    val clientId = remember(context) { AuthConfig.webClientId(context) }
    val isConfigured = clientId.isNotBlank()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->

        if (activityResult.resultCode != Activity.RESULT_OK) {
            onResult(GoogleSignInResult.Cancelled)
            return@rememberLauncherForActivityResult
        }

        val task: Task<GoogleSignInAccount> =
            GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)

        val account = runCatching { task.getResult(ApiException::class.java) }
            .getOrElse { error ->

                val code = (error as? ApiException)?.statusCode

                if (code == 12501) {
                    onResult(GoogleSignInResult.Cancelled)
                } else {
                    onResult(
                        GoogleSignInResult.Error(
                            "No se pudo iniciar sesion con Google (codigo $code)"
                        )
                    )
                }
                return@rememberLauncherForActivityResult
            }

        val idToken = account.idToken

        if (idToken.isNullOrBlank()) {
            onResult(
                GoogleSignInResult.Error(
                    "Google no devolvio un token. Revisa el Client ID Web."
                )
            )
            return@rememberLauncherForActivityResult
        }

        onResult(
            GoogleSignInResult.Success(
                userId = account.id ?: account.email.orEmpty(),
                name = account.displayName.orEmpty().ifBlank { account.email.orEmpty() },
                email = account.email.orEmpty(),
                photoUri = account.photoUrl?.toString(),
                idToken = idToken
            )
        )
    }

    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFFDADCE0), shape)
            .clickable(enabled = enabled && isConfigured) {

                val options = GoogleSignInOptions.Builder(
                    GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                    .requestIdToken(clientId)
                    .requestEmail()
                    .build()

                launcher.launch(
                    GoogleSignIn.getClient(context, options).signInIntent
                )
            }
            .padding(vertical = 13.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        GoogleMark()

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = if (isConfigured) {
                "Continuar con Google"
            } else {
                "Google sin configurar"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isConfigured) SmartBusText else SmartBusTextSecondary
        )
    }
}

@Composable
private fun GoogleMark() {

    val blue = Color(0xFF4285F4)
    val red = Color(0xFFEA4335)
    val yellow = Color(0xFFFBBC05)
    val green = Color(0xFF34A853)

    val arcSize = Size(18f, 18f)
    val topLeft = Offset(0f, 0f)

    Box(modifier = Modifier.size(18.dp)) {

        Canvas(modifier = Modifier.size(18.dp)) {

            drawArc(
                color = blue,
                startAngle = -90f,
                sweepAngle = 120f,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
            drawArc(
                color = green,
                startAngle = 30f,
                sweepAngle = 120f,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
            drawArc(
                color = yellow,
                startAngle = 150f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
            drawArc(
                color = red,
                startAngle = 240f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
        }
    }
}