package com.ejemplo.smartbus.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Configuracion de acceso con Google.
 *
 * El Client ID se lee del manifiesto, que a su vez lo toma de
 * local.properties (GOOGLE_WEB_CLIENT_ID). Asi no hay ningun secreto
 * escrito en el codigo.
 *
 * Como obtenerlo:
 *  1. Google Cloud Console > APIs y servicios > Credenciales
 *  2. Crear credenciales > ID de cliente OAuth 2.0 > Aplicacion web
 *  3. Copiar el "ID de cliente" y ponerlo en local.properties
 */
object AuthConfig {

    private const val META_CLIENT_ID =
        "com.ejemplo.smartbus.GOOGLE_CLIENT_ID"

    @Volatile
    private var cachedClientId: String? = null

    fun webClientId(context: Context): String {

        cachedClientId?.let { return it }

        val value =
            try {
                val info = context.packageManager
                    .getApplicationInfo(
                        context.packageName,
                        PackageManager.GET_META_DATA
                    )

                info.metaData?.getString(META_CLIENT_ID).orEmpty()
            } catch (_: Exception) {
                Log.w("SmartBusAuth", "No se pudo leer el Client ID de Google")
                ""
            }

        cachedClientId = value
        return value
    }

    /** True si el acceso con Google esta listo para usarse. */
    fun isGoogleConfigured(context: Context): Boolean =
        webClientId(context).isNotBlank()
}