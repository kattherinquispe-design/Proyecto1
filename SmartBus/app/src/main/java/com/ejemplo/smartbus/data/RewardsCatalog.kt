package com.ejemplo.smartbus.data

import com.ejemplo.smartbus.data.model.Reward

/**
 * Catálogo de recompensas disponibles con fotos reales y datos oficiales.
 */
object RewardsCatalog {

    val rewards: List<Reward> = listOf(
        Reward(
            id = 1,
            name = "Pasaje Urbano MistiGo",
            description = "Vale de descuento directo para 1 viaje en bus urbano en cualquier ruta de MistiGo",
            points = 20,
            stock = 100,
            category = "pasajes",
            isAvailable = true,
            imageUrl = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=600&q=80"
        ),
        Reward(
            id = 2,
            name = "Café Americano",
            description = "Café espresso seleccionado con notas de cacao y agua purificada en cafetería asociada",
            points = 40,
            stock = 50,
            category = "cafeteria",
            isAvailable = true,
            imageUrl = "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=600&q=80"
        ),
        Reward(
            id = 3,
            name = "Café Latte",
            description = "Café espresso cremoso con leche vaporizada y latte art de barista",
            points = 60,
            stock = 40,
            category = "cafeteria",
            isAvailable = true,
            imageUrl = "https://images.unsplash.com/photo-1534778101976-62847782c213?auto=format&fit=crop&w=600&q=80"
        ),
        Reward(
            id = 4,
            name = "Bocadillo & Jugo",
            description = "Sandwich gourmet de jamón y queso artesanal con jugo de naranja natural",
            points = 100,
            stock = 30,
            category = "cafeteria",
            isAvailable = true,
            imageUrl = "https://images.unsplash.com/photo-1509722747041-616f39b57569?auto=format&fit=crop&w=600&q=80"
        ),
        Reward(
            id = 5,
            name = "Pase Diario",
            description = "Viajes ilimitados durante 24 horas en todas las rutas MistiGo de Arequipa",
            points = 160,
            stock = 20,
            category = "pasajes",
            isAvailable = true,
            imageUrl = "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?auto=format&fit=crop&w=600&q=80"
        ),
        Reward(
            id = 6,
            name = "20% de Descuento",
            description = "Bono de 20% de descuento directo aplicable en tu próxima recarga de tarjeta de bus",
            points = 80,
            stock = 25,
            category = "beneficios",
            isAvailable = true,
            imageUrl = "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?auto=format&fit=crop&w=600&q=80"
        )
    )

    fun byId(id: Int): Reward? = rewards.firstOrNull { it.id == id }
}
