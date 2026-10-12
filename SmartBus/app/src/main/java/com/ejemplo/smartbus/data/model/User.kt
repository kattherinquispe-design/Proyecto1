package com.ejemplo.smartbus.data.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String? = null,
    val photoUri: String? = null,
    val level: Int = 1,
    val totalTrips: Int = 0,
    val totalBoardings: Int = 0,
    val isActive: Boolean = true,
    val twoFactorEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis()
)