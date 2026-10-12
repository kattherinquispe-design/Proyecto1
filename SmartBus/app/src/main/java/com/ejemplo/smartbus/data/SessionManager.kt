package com.ejemplo.smartbus.data

import android.content.Context
import com.ejemplo.smartbus.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Como entro el usuario.
 */
enum class LoginProvider {
    GOOGLE,
    EMAIL,
    GUEST
}

/**
 * Sesion activa.
 */
data class SmartBusSession(
    val userId: String,
    val name: String,
    val email: String,
    val photoUri: String? = null,
    val provider: LoginProvider = LoginProvider.GUEST,
    val googleIdToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Que esta haciendo la persona en la app. El pasajero es el rol
     * de siempre; el conductor es el que registra vehiculo, sube
     * documentos y se pone online. Por defecto sigue siendo pasajero,
     * asi que las sesiones viejas siguen funcionando sin tocar nada.
     */
    val role: UserRole = UserRole.PASSENGER
) {

    val isGuest: Boolean get() = provider == LoginProvider.GUEST

    val isDriver: Boolean get() = role == UserRole.DRIVER

    /**
     * Identificador estable para separar los datos de cada cuenta.
     *
     * El rol NO entra aqui a proposito: los puntos y el historial son
     * de la persona, no de si esta conduciendo o viajando.
     */
    val storageKey: String
        get() = when (provider) {
            LoginProvider.GOOGLE -> "google_${userId.takeIf { it.isNotBlank() } ?: email}"
            LoginProvider.EMAIL -> "email_${email}"
            LoginProvider.GUEST -> "guest_${userId}"
        }.lowercase()
}

/**
 * Sesion del usuario, observable por Compose.
 *
 * Antes el login vivia en dos repositorios distintos que nadie usaba y
 * el perfil mostraba siempre "Usuario SmartBus" con un correo falso.
 * Aqui hay una sola fuente de verdad.
 */
class SessionManager(context: Context) {

    private val preferences =
        context.getSharedPreferences("smartbus_session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(load())

    val session: StateFlow<SmartBusSession?> = _session.asStateFlow()

    val isSignedIn: Boolean get() = _session.value != null

    val currentRole: UserRole get() = _session.value?.role ?: UserRole.PASSENGER

    private fun load(): SmartBusSession? {

        if (!preferences.getBoolean(KEY_SIGNED_IN, false)) {
            return null
        }

        val providerName =
            preferences.getString(KEY_PROVIDER, null) ?: return null

        val provider =
            runCatching { LoginProvider.valueOf(providerName) }
                .getOrNull() ?: return null

        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        val name = preferences.getString(KEY_NAME, null) ?: return null

        val roleName = preferences.getString(KEY_ROLE, null)

        val role =
            runCatching { UserRole.valueOf(roleName.orEmpty()) }
                .getOrDefault(UserRole.PASSENGER)

        return SmartBusSession(
            userId = userId,
            name = name,
            email = preferences.getString(KEY_EMAIL, "").orEmpty(),
            photoUri = preferences.getString(KEY_PHOTO, null),
            provider = provider,
            googleIdToken = preferences.getString(KEY_ID_TOKEN, null),
            createdAt = preferences.getLong(KEY_CREATED_AT, System.currentTimeMillis()),
            role = role
        )
    }

    private fun persist(session: SmartBusSession) {

        preferences.edit()
            .putBoolean(KEY_SIGNED_IN, true)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_NAME, session.name)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_PHOTO, session.photoUri)
            .putString(KEY_PROVIDER, session.provider.name)
            .putString(KEY_ID_TOKEN, session.googleIdToken)
            .putLong(KEY_CREATED_AT, session.createdAt)
            .putString(KEY_ROLE, session.role.name)
            .apply()

        _session.value = session
    }

    /**
     * Cambia entre pasajero y conductor sin perder la sesion ni los
     * puntos. La sesion es de la persona, el rol es lo que hace.
     */
    fun setRole(role: UserRole) {

        val current = _session.value ?: return

        persist(current.copy(role = role))
    }

    fun toggleRole() {

        val current = _session.value ?: return

        setRole(current.role.opposite())
    }

    /**
     * Entra con una cuenta de Google ya verificada.
     */
    fun signInWithGoogle(
        userId: String,
        name: String,
        email: String,
        photoUri: String?,
        idToken: String
    ): SmartBusSession {

        val session = SmartBusSession(
            userId = userId.ifBlank { email },
            name = name.ifBlank { email.substringBefore("@") },
            email = email,
            photoUri = photoUri,
            provider = LoginProvider.GOOGLE,
            googleIdToken = idToken
        )

        persist(session)
        return session
    }

    /**
     * Entra con correo y contrasena guardados en el telefono.
     * En produccion esto se sustituye por la API del operador.
     */
    fun signInWithEmail(email: String, name: String): SmartBusSession {

        val cleanEmail = email.trim().lowercase()

        val session = SmartBusSession(
            userId = cleanEmail,
            name = name.ifBlank { cleanEmail.substringBefore("@") },
            email = cleanEmail,
            provider = LoginProvider.EMAIL
        )

        persist(session)
        return session
    }

    /**
     * Modo invitado: la app funciona sin cuenta y los puntos quedan
     * guardados en un perfil propio.
     */
    fun signInAsGuest(name: String): SmartBusSession {

        val session = SmartBusSession(
            userId = preferences.getString(KEY_GUEST_ID, null)
                ?: UUID.randomUUID().toString(),
            name = name.ifBlank { "Viajero" },
            email = "",
            provider = LoginProvider.GUEST
        )

        preferences.edit()
            .putString(KEY_GUEST_ID, session.userId)
            .apply()

        persist(session)
        return session
    }

    fun updateName(name: String) {
        val current = _session.value ?: return
        persist(current.copy(name = name))
    }

    fun signOut() {
        preferences.edit().remove(KEY_SIGNED_IN).apply()
        _session.value = null
    }

    companion object {
        private const val KEY_SIGNED_IN = "signed_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHOTO = "photo_uri"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_ID_TOKEN = "google_id_token"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_GUEST_ID = "guest_id"
        private const val KEY_ROLE = "role"

        @Volatile
        private var instance: SessionManager? = null

        fun get(context: Context): SessionManager =
            instance ?: synchronized(this) {
                instance ?: SessionManager(context).also { instance = it }
            }

        /**
         * Permite empezar una sesion de conductor en una sola llamada,
         * reutilizando el metodo de entrada que ya exista.
         */
        fun signInWithRole(
            context: Context,
            provider: LoginProvider,
            userId: String,
            name: String,
            email: String,
            role: UserRole
        ): SessionManager {

            val manager = get(context)

            manager.signIn(
                provider = provider,
                userId = userId,
                name = name,
                email = email,
                role = role
            )

            return manager
        }
    }

    /**
     * Entra con un proveedor cualquiera, con el rol que se pida.
     */
    fun signIn(
        provider: LoginProvider,
        userId: String,
        name: String,
        email: String,
        role: UserRole = UserRole.PASSENGER
    ): SmartBusSession {

        val session = SmartBusSession(
            userId = userId.ifBlank { email },
            name = name.ifBlank { email.substringBefore("@") },
            email = email,
            provider = provider,
            role = role
        )

        persist(session)
        return session
    }
}