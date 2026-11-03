package com.ejemplo.smartbus.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Acento elegido por el usuario desde Configuracion.
 *
 * Se guarda en un estado global de Compose para que al cambiarlo
 * todas las pantallas se repinten al instante, sin reiniciar.
 */
enum class AppAccent(
    val id: String,
    val label: String,
    val start: Color,
    val end: Color
) {
    AZUL("azul", "Azul Zafiro", Color(0xFF1E40AF), Color(0xFF0284C7)),
    INDIGO("indigo", "Índigo Metro", Color(0xFF4F46E5), Color(0xFF7C3AED)),
    CORAL("coral", "Coral Misti", Color(0xFFE11D48), Color(0xFFFB7185)),
    VERDE("verde", "Verde Esmeralda", Color(0xFF0D9488), Color(0xFF10B981)),
    NARANJA("naranja", "Ámbar Dorado", Color(0xFFD97706), Color(0xFFF59E0B)),
    MORADO("morado", "Amatista Real", Color(0xFF7E22CE), Color(0xFFA855F7)),
    FUCSIA("fucsia", "Fucsia Moderno", Color(0xFFDB2777), Color(0xFFF43F5E));

    companion object {
        val DEFAULT = AZUL

        fun fromId(id: String?): AppAccent =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}

object AppPalette {

    private val accentState = mutableStateOf(AppAccent.DEFAULT)

    val accent: AppAccent get() = accentState.value

    fun setAccent(value: AppAccent) {
        accentState.value = value
    }

    fun setAccent(id: String?) {
        accentState.value = AppAccent.fromId(id)
    }

    val primary: Color get() = accent.start
    val primaryDark: Color get() = accent.end
    val primarySoft: Color get() = lerp(accent.start, Color.White, 0.82f)
    val onPrimary: Color get() = Color.White
}
