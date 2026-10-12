package com.ejemplo.smartbus.data.model

data class Reward(
    val id: Int,
    val name: String,
    val description: String,
    val points: Int,
    val stock: Int = 0,
    val category: String = "general",
    val isAvailable: Boolean = true
)