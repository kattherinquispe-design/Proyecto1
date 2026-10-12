package com.ejemplo.smartbus.data

import android.content.Context
import com.ejemplo.smartbus.data.model.UserProfile
import java.security.MessageDigest

class AuthRepository(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "smartbus_auth",
            Context.MODE_PRIVATE
        )

    companion object {

        private const val KEY_NAME =
            "user_name"

        private const val KEY_EMAIL =
            "user_email"

        private const val KEY_PASSWORD_HASH =
            "user_password_hash"

        private const val KEY_PHOTO =
            "user_photo"

        private const val KEY_LOGGED_IN =
            "logged_in"
    }

    // =========================================================
    // REGISTRAR CUENTA
    // =========================================================

    fun register(
        name: String,
        email: String,
        password: String
    ): Boolean {

        val cleanName =
            name.trim()

        val cleanEmail =
            email.trim()
                .lowercase()

        // -----------------------------------------------------
        // VALIDACIONES
        // -----------------------------------------------------

        if (cleanName.isBlank()) {
            return false
        }

        if (!isValidEmail(cleanEmail)) {
            return false
        }

        if (password.length < 8) {
            return false
        }

        // -----------------------------------------------------
        // NO PERMITIR REGISTRAR OTRA CUENTA SOBRE LA EXISTENTE
        // -----------------------------------------------------

        val savedEmail =
            preferences.getString(
                KEY_EMAIL,
                null
            )

        if (
            savedEmail != null &&
            savedEmail == cleanEmail
        ) {
            return false
        }

        // -----------------------------------------------------
        // GUARDAR CUENTA
        // -----------------------------------------------------

        preferences.edit()
            .putString(
                KEY_NAME,
                cleanName
            )
            .putString(
                KEY_EMAIL,
                cleanEmail
            )
            .putString(
                KEY_PASSWORD_HASH,
                hashPassword(password)
            )
            .putBoolean(
                KEY_LOGGED_IN,
                false
            )
            .apply()

        return true
    }


    // =========================================================
    // INICIAR SESIÓN
    // =========================================================

    fun login(
        email: String,
        password: String
    ): Boolean {

        val cleanEmail =
            email.trim()
                .lowercase()


        val savedEmail =
            preferences.getString(
                KEY_EMAIL,
                null
            )


        val savedPasswordHash =
            preferences.getString(
                KEY_PASSWORD_HASH,
                null
            )


        if (
            savedEmail == null ||
            savedPasswordHash == null
        ) {
            return false
        }


        val passwordHash =
            hashPassword(password)


        val valid =
            savedEmail == cleanEmail &&
                    savedPasswordHash ==
                    passwordHash


        if (valid) {

            preferences.edit()
                .putBoolean(
                    KEY_LOGGED_IN,
                    true
                )
                .apply()
        }


        return valid
    }


    // =========================================================
    // COMPROBAR SESIÓN
    // =========================================================

    fun isLoggedIn(): Boolean {

        return preferences.getBoolean(
            KEY_LOGGED_IN,
            false
        )
    }


    // =========================================================
    // OBTENER USUARIO
    // =========================================================

    fun getUser(): UserProfile? {

        val name =
            preferences.getString(
                KEY_NAME,
                null
            )


        val email =
            preferences.getString(
                KEY_EMAIL,
                null
            )


        if (
            name == null ||
            email == null
        ) {
            return null
        }


        val photoUri =
            preferences.getString(
                KEY_PHOTO,
                null
            )


        return UserProfile(
            name = name,
            email = email,
            photoUri = photoUri
        )
    }


    // =========================================================
    // ACTUALIZAR FOTO
    // =========================================================

    fun updatePhotoUri(
        photoUri: String
    ) {

        preferences.edit()
            .putString(
                KEY_PHOTO,
                photoUri
            )
            .apply()
    }


    // =========================================================
    // CERRAR SESIÓN
    // =========================================================

    fun logout() {

        preferences.edit()
            .putBoolean(
                KEY_LOGGED_IN,
                false
            )
            .apply()
    }


    // =========================================================
    // BORRAR CUENTA LOCAL
    // =========================================================

    fun clearAccount() {

        preferences.edit()
            .clear()
            .apply()
    }


    // =========================================================
    // VALIDAR CORREO
    // =========================================================

    private fun isValidEmail(
        email: String
    ): Boolean {

        return android.util.Patterns
            .EMAIL_ADDRESS
            .matcher(email)
            .matches()
    }


    // =========================================================
    // HASH DE CONTRASEÑA
    // =========================================================

    private fun hashPassword(
        password: String
    ): String {

        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )


        val hash =
            digest.digest(
                password.toByteArray(
                    Charsets.UTF_8
                )
            )


        return hash.joinToString("") {
            "%02x".format(it)
        }
    }
}