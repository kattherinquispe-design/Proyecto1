package com.ejemplo.smartbus.data.model

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val photoUri: String? = null,
    val emergencyContact: String? = null,
    val hasDisability: Boolean = false,
    val accessibilityPreferences: List<String> = emptyList(),
    val favoritesStops: List<Int> = emptyList(),
    val favoritesRoutes: List<Int> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
)