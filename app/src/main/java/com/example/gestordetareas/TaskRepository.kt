package com.example.gestordetareas

import android.content.Context
import com.example.gestordetareas.data.DBHelper

/**
 * Repositorio que gestiona la persistencia de tareas usando SQLite.
 *
 * Encapsula toda la lógica de acceso a datos a través de [DBHelper],
 * de modo que el resto de la app no depende del mecanismo de almacenamiento.
 *
 * @param context Contexto de la aplicación (necesario para abrir la BD).
 */
class TaskRepository(context: Context) {

    private val db = DBHelper(context)

    /**
     * Recupera la lista completa de tareas almacenadas.
     *
     * @return Lista de objetos [Task] ordenados por ID descendente.
     */
    fun loadTasks(): List<Task> = db.getAllTasks()

    /**
     * Agrega una nueva tarea al almacenamiento local.
     *
     * @param task Objeto [Task] a guardar.
     * @return ID generado para la tarea insertada.
     */
    fun addTask(task: Task): Long = db.insertTask(task)

    /**
     * Actualiza el estado de completada de una tarea.
     *
     * @param id ID de la tarea.
     * @param isCompleted Nuevo estado de completada.
     */
    fun updateCompleted(id: Long, isCompleted: Boolean) {
        db.updateCompleted(id, isCompleted)
    }

    /**
     * Elimina la tarea con el ID indicado.
     *
     * @param id ID de la tarea a eliminar.
     */
    fun deleteTask(id: Long) {
        db.deleteTask(id)
    }
}
