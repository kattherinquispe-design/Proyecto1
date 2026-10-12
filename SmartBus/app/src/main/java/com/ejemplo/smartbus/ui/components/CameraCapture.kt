package com.ejemplo.smartbus.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Abre la camara del celular y devuelve la ruta del archivo de la
 * foto, o null si el usuario cancela o falla.
 *
 * La foto se guarda en el cache de la app y luego se copia al
 * almacenamiento interno (igual que las de la galeria), de modo que
 * todos los documentos del conductor tengan el mismo ciclo de vida.
 */
@Composable
fun rememberCameraCapture(onResult: (String?) -> Unit): () -> Unit {

    val context = LocalContext.current

    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = pendingUri
            onResult(if (uri != null) copyCameraPhoto(context, uri) else null)
        } else {
            onResult(null)
        }
    }

    return {
        val folder = File(context.cacheDir, ".camera").apply { mkdirs() }
        val file = File(folder, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        pendingUri = uri
        launcher.launch(uri)
    }
}

private fun copyCameraPhoto(context: Context, uri: Uri): String? {

    return try {

        val folder = File(context.filesDir, "camera_documents")
        if (!folder.exists() && !folder.mkdirs()) {
            return null
        }

        val target = File(folder, "photo_${System.currentTimeMillis()}.jpg")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: return null

        target.absolutePath

    } catch (exception: Exception) {
        null
    }
}