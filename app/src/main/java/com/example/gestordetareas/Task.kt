package com.example.gestordetareas

/**
 * Modelo de datos que representa una tarea en el gestor.
 *
 * @property id Identificador único generado por SQLite (AUTOINCREMENT).
 * @property title Nombre corto de la tarea.
 * @property description Descripción detallada de la tarea.
 * @property isCompleted Indica si la tarea ya fue completada.
 * @property latitude Latitud de la ubicación asociada (null si no tiene).
 * @property longitude Longitud de la ubicación asociada (null si no tiene).
 */
data class Task(
    val id: Long = 0,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    /** Indica si la tarea tiene una ubicación geográfica asociada. */
    val hasLocation: Boolean get() = latitude != null && longitude != null
}
