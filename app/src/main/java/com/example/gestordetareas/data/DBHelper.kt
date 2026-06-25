package com.example.gestordetareas.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.gestordetareas.Task

/**
 * Helper de SQLite para la tabla de tareas con cifrado en reposo.
 *
 * Los campos sensibles [Task.title] y [Task.description] se cifran con
 * AES-256-GCM (a través de [CryptoManager]) antes de escribirse en la base
 * de datos, y se descifran automáticamente al leerlos.  Los demás campos
 * (estado, coordenadas) se almacenan en claro porque no contienen información
 * personal identificable.
 *
 * Gestiona la creación, migración y operaciones CRUD de la base de datos local.
 * Sigue el patrón de SQLiteOpenHelper de Android.
 *
 * @param context Contexto de la aplicación.
 */
class DBHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    /** Gestor de cifrado AES-256-GCM vinculado al Android Keystore. */
    private val crypto = CryptoManager(context)

    companion object {
        const val DATABASE_NAME    = "gestor_tareas.db"
        const val DATABASE_VERSION = 2          // v2: título y descripción cifrados

        // Tabla y columnas
        const val TABLE_TASKS   = "tasks"
        const val COL_ID        = "id"
        const val COL_TITLE     = "title"        // almacenado cifrado
        const val COL_DESC      = "description"  // almacenado cifrado
        const val COL_COMPLETED = "is_completed"
        const val COL_LAT       = "latitude"
        const val COL_LNG       = "longitude"
    }

    /** Crea la tabla de tareas la primera vez que se abre la base de datos. */
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_TASKS (
                $COL_ID        INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE     TEXT    NOT NULL,
                $COL_DESC      TEXT,
                $COL_COMPLETED INTEGER DEFAULT 0,
                $COL_LAT       REAL,
                $COL_LNG       REAL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    /**
     * Migra la base de datos cuando DATABASE_VERSION aumenta.
     *
     * De v1 a v2: se cifran los registros existentes.
     * De cualquier versión desconocida: recrear tabla.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) {
            // Migrar registros existentes cifrando título y descripción
            val cursor = db.rawQuery("SELECT $COL_ID, $COL_TITLE, $COL_DESC FROM $TABLE_TASKS", null)
            cursor.use {
                while (it.moveToNext()) {
                    val id    = it.getLong(it.getColumnIndexOrThrow(COL_ID))
                    val title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)) ?: ""
                    val desc  = it.getString(it.getColumnIndexOrThrow(COL_DESC))  ?: ""

                    val values = ContentValues().apply {
                        put(COL_TITLE, crypto.encrypt(title))
                        put(COL_DESC,  crypto.encrypt(desc))
                    }
                    db.update(TABLE_TASKS, values, "$COL_ID=?", arrayOf(id.toString()))
                }
            }
        } else {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
            onCreate(db)
        }
    }

    // -------------------------------------------------------------------------
    // Operaciones CRUD
    // -------------------------------------------------------------------------

    /**
     * Inserta una nueva tarea cifrando título y descripción antes de guardar.
     *
     * Prevención de inyección SQL: se usa [ContentValues] en lugar de
     * concatenación de strings. Android pasa los valores como parámetros
     * enlazados (?), lo que impide cualquier inyección SQL independientemente
     * del contenido ingresado por el usuario.
     *
     * @param task Tarea a guardar (el campo [Task.id] es ignorado).
     * @return ID de la fila insertada, o -1 si hubo un error.
     */
    fun insertTask(task: Task): Long {
        // ContentValues → parámetros enlazados internamente por SQLite (anti SQL injection)
        val values = ContentValues().apply {
            put(COL_TITLE,     crypto.encrypt(task.title))
            put(COL_DESC,      crypto.encrypt(task.description))
            put(COL_COMPLETED, if (task.isCompleted) 1 else 0)
            task.latitude?.let  { put(COL_LAT, it) } ?: putNull(COL_LAT)
            task.longitude?.let { put(COL_LNG, it) } ?: putNull(COL_LNG)
        }
        return writableDatabase.insert(TABLE_TASKS, null, values)
    }

    /**
     * Recupera todas las tareas almacenadas, descifrando título y descripción.
     *
     * La consulta usa columnas definidas como constantes (no interpolación de
     * input del usuario), eliminando el riesgo de inyección SQL en la lectura.
     *
     * @return Lista de tareas con campos sensibles en texto plano.
     */
    fun getAllTasks(): List<Task> {
        val tasks  = mutableListOf<Task>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_TASKS ORDER BY $COL_ID DESC", null
        )
        cursor.use {
            while (it.moveToNext()) {
                val latIdx = it.getColumnIndexOrThrow(COL_LAT)
                val lngIdx = it.getColumnIndexOrThrow(COL_LNG)
                tasks.add(
                    Task(
                        id          = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        title       = crypto.decrypt(
                            it.getString(it.getColumnIndexOrThrow(COL_TITLE)) ?: ""
                        ),
                        description = crypto.decrypt(
                            it.getString(it.getColumnIndexOrThrow(COL_DESC)) ?: ""
                        ),
                        isCompleted = it.getInt(it.getColumnIndexOrThrow(COL_COMPLETED)) == 1,
                        latitude    = if (it.isNull(latIdx)) null else it.getDouble(latIdx),
                        longitude   = if (it.isNull(lngIdx)) null else it.getDouble(lngIdx)
                    )
                )
            }
        }
        return tasks
    }

    /**
     * Actualiza el estado de completada de una tarea.
     *
     * @param id ID de la tarea a actualizar.
     * @param isCompleted Nuevo estado.
     * @return Número de filas afectadas.
     */
    fun updateCompleted(id: Long, isCompleted: Boolean): Int {
        val values = ContentValues().apply {
            put(COL_COMPLETED, if (isCompleted) 1 else 0)
        }
        return writableDatabase.update(TABLE_TASKS, values, "$COL_ID=?", arrayOf(id.toString()))
    }

    /**
     * Elimina la tarea con el ID indicado.
     *
     * @param id ID de la tarea a eliminar.
     * @return Número de filas afectadas.
     */
    fun deleteTask(id: Long): Int =
        writableDatabase.delete(TABLE_TASKS, "$COL_ID=?", arrayOf(id.toString()))
}
