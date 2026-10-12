package com.ejemplo.smartbus.data

import android.content.Context
import com.ejemplo.smartbus.data.model.User
import java.security.MessageDigest
import java.util.UUID

class UserRepository(context: Context) {

    private val preferences =
        context.getSharedPreferences(
            "smartbus_users",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val KEY_USERS = "users"
        private const val KEY_CURRENT_USER = "current_user"
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }

    fun register(
        name: String,
        email: String,
        password: String
    ): Result<User> {

        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Ingresa tu nombre.")
            )
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(cleanEmail)
                .matches()
        ) {
            return Result.failure(
                IllegalArgumentException("Ingresa un correo válido.")
            )
        }

        if (password.length < 6) {
            return Result.failure(
                IllegalArgumentException(
                    "La contraseña debe tener al menos 6 caracteres."
                )
            )
        }

        val users = getUsers().toMutableList()

        if (users.any {
                it.second.equals(cleanEmail, true)
            }
        ) {
            return Result.failure(
                IllegalArgumentException(
                    "Ya existe una cuenta con ese correo."
                )
            )
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            email = cleanEmail
        )

        users.add(
            Triple(
                user.id,
                cleanEmail,
                hashPassword(password)
            )
        )

        saveUsers(users)

        preferences.edit()
            .putString(
                "name_${user.id}",
                cleanName
            )
            .apply()

        return Result.success(user)
    }

    fun login(
        email: String,
        password: String
    ): Result<User> {

        val cleanEmail = email.trim().lowercase()
        val passwordHash = hashPassword(password)

        val users = getUsers()

        val account = users.firstOrNull {
            it.first.equals(cleanEmail, true) &&
                    it.third == passwordHash
        }

        if (account == null) {
            return Result.failure(
                IllegalArgumentException(
                    "Correo o contraseña incorrectos."
                )
            )
        }

        val user = User(
            id = account.first,
            name = getUserName(account.first),
            email = account.second
        )

        preferences.edit()
            .putString(KEY_CURRENT_USER, user.id)
            .apply()

        return Result.success(user)
    }

    fun logout() {
        preferences.edit()
            .remove(KEY_CURRENT_USER)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return !preferences
            .getString(KEY_CURRENT_USER, null)
            .isNullOrBlank()
    }

    fun getCurrentUser(): User? {

        val id =
            preferences.getString(
                KEY_CURRENT_USER,
                null
            ) ?: return null

        return getUserById(id)
    }

    private fun getUserName(id: String): String {
        return preferences.getString(
            "name_$id",
            "Usuario SmartBus"
        ) ?: "Usuario SmartBus"
    }

    private fun getUserById(id: String): User? {
        val account =
            getUsers().firstOrNull { it.first == id }
                ?: return null

        return User(
            id = account.first,
            name = getUserName(account.first),
            email = account.second
        )
    }

    private fun getUsers():
            List<Triple<String, String, String>> {

        val raw =
            preferences.getString(
                KEY_USERS,
                ""
            ) ?: ""

        if (raw.isBlank()) {
            return emptyList()
        }

        return raw.split("|||")
            .mapNotNull { item ->
                val parts = item.split(":::")

                if (parts.size == 3) {
                    Triple(
                        parts[0],
                        parts[1],
                        parts[2]
                    )
                } else {
                    null
                }
            }
    }

    private fun saveUsers(
        users: List<Triple<String, String, String>>
    ) {

        val raw = users.joinToString("|||") {
            "${it.first}:::${it.second}:::${it.third}"
        }

        users.forEach {
            preferences.edit()
                .putString(
                    "name_${it.first}",
                    preferences.getString(
                        "name_${it.first}",
                        "Usuario SmartBus"
                    )
                )
                .apply()
        }

        preferences.edit()
            .putString(KEY_USERS, raw)
            .apply()
    }
}