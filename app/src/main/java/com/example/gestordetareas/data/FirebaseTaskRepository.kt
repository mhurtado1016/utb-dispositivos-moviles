package com.example.gestordetareas.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.gestordetareas.Task
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de tareas en Firebase Realtime Database.
 *
 * Estructura de datos en Firebase:
 * ```
 * users/
 *   {uid}/
 *     tasks/
 *       {taskKey}/
 *         title: String
 *         description: String
 *         isCompleted: Boolean
 *         latitude: Double?
 *         longitude: Double?
 * ```
 *
 * Uso recomendado: patrón híbrido con SQLite.
 * SQLite es la fuente de verdad local; este repositorio sincroniza los cambios
 * en la nube cuando hay conexión disponible.
 */
class FirebaseTaskRepository {

    /** Referencia al nodo de tareas del usuario autenticado. */
    private val tasksRef by lazy {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("No hay usuario autenticado")
        FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(uid)
            .child("tasks")
    }

    /**
     * Sube una tarea a Firebase Realtime Database.
     *
     * @param task Tarea a guardar.
     * @return Clave generada por Firebase para la tarea.
     */
    suspend fun addTask(task: Task): String? {
        val key = tasksRef.push().key ?: return null
        val taskMap = mapOf(
            "id"          to task.id,
            "title"       to task.title,
            "description" to task.description,
            "isCompleted" to task.isCompleted,
            "latitude"    to task.latitude,
            "longitude"   to task.longitude
        )
        tasksRef.child(key).setValue(taskMap).await()
        return key
    }

    /**
     * Recupera todas las tareas del usuario desde Firebase.
     *
     * @return Lista de tareas almacenadas en la nube.
     */
    suspend fun getTasks(): List<Task> {
        val snapshot = tasksRef.get().await()
        return snapshot.children.mapNotNull { child ->
            try {
                Task(
                    id          = child.child("id").getValue(Long::class.java) ?: 0L,
                    title       = child.child("title").getValue(String::class.java) ?: return@mapNotNull null,
                    description = child.child("description").getValue(String::class.java) ?: "",
                    isCompleted = child.child("isCompleted").getValue(Boolean::class.java) ?: false,
                    latitude    = child.child("latitude").getValue(Double::class.java),
                    longitude   = child.child("longitude").getValue(Double::class.java)
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Elimina una tarea de Firebase usando su clave.
     *
     * @param key Clave generada por Firebase al crear la tarea.
     */
    suspend fun deleteTask(key: String) {
        tasksRef.child(key).removeValue().await()
    }

    /**
     * Sincroniza una lista de tareas locales hacia Firebase.
     * Sobreescribe los datos existentes del usuario.
     *
     * @param tasks Lista de tareas a sincronizar.
     */
    suspend fun syncTasks(tasks: List<Task>) {
        tasks.forEach { addTask(it) }
    }
}
