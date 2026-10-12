package com.ejemplo.smartbus.data

import com.ejemplo.smartbus.data.model.Reward

/**
 * Catalogo de recompensas disponibles.
 * Cuando se conecte la base de datos real, este catalogo se reemplaza
 * por la respuesta del backend manteniendo el mismo contrato.
 */
object RewardsCatalog {

    val rewards: List<Reward> = listOf(
        Reward(
            id = 1,
            name = "Cafe-Americano",
            description = "Cafe americano en la cafeteria del terminal",
            points = 150,
            stock = 50,
            category = "cafeteria",
            isAvailable = true
        ),
        Reward(
            id = 2,
            name = "Cafe-Latte",
            description = "Cafe con leche de la cafeteria del terminal",
            points = 250,
            stock = 40,
            category = "cafeteria",
            isAvailable = true
        ),
        Reward(
            id = 3,
            name = "Bocadillo-y-Jugo",
            description = "Bocadillo de pan con jugo de naranja",
            points = 400,
            stock = 30,
            category = "cafeteria",
            isAvailable = true
        ),
        Reward(
            id = 4,
            name = "Pase-Dia",
            description = "Pase de viaje de un dia en cualquier ruta",
            points = 600,
            stock = 20,
            category = "pasajes",
            isAvailable = true
        ),
        Reward(
            id = 5,
            name = "Pase-Semanal",
            description = "Pase de viaje de siete dias en cualquier ruta",
            points = 1200,
            stock = 10,
            category = "pasajes",
            isAvailable = true
        ),
        Reward(
            id = 6,
            name = "Descuento-20",
            description = "20% de descuento en tu proxima recarga de tarjeta",
            points = 300,
            stock = 25,
            category = "beneficios",
            isAvailable = true
        )
    )

    fun byId(id: Int): Reward? = rewards.firstOrNull { it.id == id }
}