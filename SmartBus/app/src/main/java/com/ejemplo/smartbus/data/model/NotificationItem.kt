package com.ejemplo.smartbus.data.model

data class NotificationItem(
    val id: Long,
    val title: String,
    val message: String,
    val type: String = "general",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val actionUrl: String? = null
)