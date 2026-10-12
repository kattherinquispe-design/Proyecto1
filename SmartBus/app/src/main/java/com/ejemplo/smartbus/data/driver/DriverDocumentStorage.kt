package com.ejemplo.smartbus.data.driver

import android.content.Context
import android.net.Uri
import com.ejemplo.smartbus.data.model.DriverDocumentStatus
import com.ejemplo.smartbus.data.model.DriverDocumentType
import java.io.File
import java.io.FileOutputStream

/**
 * Guardado local de los documentos que sube el conductor.
 *
 * Se copia el archivo al almacenamiento interno de la app en vez de
 * quedarnos con la Uri: una Uri de la galeria o de la camara puede
 * desaparecer, y entonces el revisor veria un documento roto.
 *
 * OJO: esto es solo el almacen local. En produccion el archivo
 * tiene que subir a un almacen cifrado del backend; aqui se deja la
 * ruta en la base para que el flujo sea real de punta a punta.
 */
object DriverDocumentStorage {

    private const val FOLDER = "driver_documents"

    /**
     * Copia el contenido de la Uri a un archivo interno y devuelve la
     * ruta absoluta. Devuelve null si no se pudo leer.
     */
    fun copyIntoAppStorage(
        context: Context,
        accountId: String,
        type: DriverDocumentType,
        source: Uri
    ): String? {

        return try {

            val folder = File(context.filesDir, "$FOLDER/$accountId")

            if (!folder.exists() && !folder.mkdirs()) {
                return null
            }

            val extension = extensionOf(source)

            val file = File(
                folder,
                "${type.name}_${System.currentTimeMillis()}$extension"
            )

            val input = context.contentResolver.openInputStream(source)
                ?: return null

            input.use { sourceStream ->

                FileOutputStream(file).use { target ->
                    sourceStream.copyTo(target)
                }
            }

            file.absolutePath

        } catch (exception: Exception) {
            null
        }
    }

/**
 * Borra un documento del disco. Se llama cuando el conductor
     * quita un archivo mal subido, para no dejar copias huerfanas.
     */
    fun delete(context: Context, accountId: String, type: DriverDocumentType) {

        try {

            File(context.filesDir, "$FOLDER/$accountId")
                .listFiles { file ->
                    file.name.startsWith("${type.name}_")
                }
                ?.forEach { it.delete() }

        } catch (exception: Exception) {
        }
    }

    private fun extensionOf(uri: Uri): String {

        val type = uri.lastPathSegment
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() && it.length <= 4 }

        return if (type != null) ".$type" else ".jpg"
    }
}

/**
 * Simula la revision documental de un operador.
 *
 * Existe solo para poder probar el flujo completo en debug. En una
 * build de release esta clase desaparece del arbol de codigo junto con
 * los botones que la llaman: la aprobacion real la hace el backend
 * revisando el documento, nunca el conductor.
 */
class DriverReviewSimulator(
    private val repository: DriverRepository
) {

    suspend fun approveAllPending(accountId: String) {

        repository.getDocuments(accountId)
            .filter { it.status == DriverDocumentStatus.UPLOADED }
            .forEach {
                repository.approveDocument(accountId, it.type, "Revisado en modo debug")
            }
    }
}