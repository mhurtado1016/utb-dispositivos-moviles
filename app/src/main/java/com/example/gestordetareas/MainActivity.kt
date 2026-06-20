package com.example.gestordetareas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.gestordetareas.data.AuthManager
import com.example.gestordetareas.data.FirebaseTaskRepository
import com.example.gestordetareas.ui.LoginScreen
import com.example.gestordetareas.ui.MapActivity
import com.example.gestordetareas.ui.RegisterScreen
import com.example.gestordetareas.ui.theme.GestorDeTareasTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: TaskRepository
    private lateinit var authManager: AuthManager
    private val firebaseRepo = FirebaseTaskRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authManager  = AuthManager(this)
        repository = TaskRepository(this)

        setContent {
            GestorDeTareasTheme {
                GestorDeTareasApp(
                    repository   = repository,
                    authManager  = authManager,
                    firebaseRepo = firebaseRepo,
                    onTaskSaved  = {
                        Toast.makeText(this, "Tarea guardada", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

/** Representa las pantallas disponibles en la aplicación. */
sealed class AppScreen {
    object Login    : AppScreen()
    object Register : AppScreen()
    object Main     : AppScreen()
    object Form     : AppScreen()
    data class Detail(val task: Task) : AppScreen()
}

@Composable
fun GestorDeTareasApp(
    repository: TaskRepository,
    authManager: AuthManager,
    firebaseRepo: FirebaseTaskRepository,
    onTaskSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var currentScreen by remember {
        mutableStateOf<AppScreen>(
            if (authManager.isLoggedIn) AppScreen.Main else AppScreen.Login
        )
    }

    var tasks by remember { mutableStateOf(repository.loadTasks()) }

    // Estado del formulario elevado
    var formTitle       by remember { mutableStateOf("") }
    var formDescription by remember { mutableStateOf("") }
    var formLatitude    by remember { mutableStateOf<Double?>(null) }
    var formLongitude   by remember { mutableStateOf<Double?>(null) }

    // Estado de autenticación
    var authLoading by remember { mutableStateOf(false) }
    var authError   by remember { mutableStateOf<String?>(null) }

    // Launcher para recibir coordenadas de MapActivity
    val mapLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            formLatitude  = result.data?.getDoubleExtra(MapActivity.EXTRA_LATITUDE, 0.0)
            formLongitude = result.data?.getDoubleExtra(MapActivity.EXTRA_LONGITUDE, 0.0)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (val screen = currentScreen) {

            // ── Login ──────────────────────────────────────────────────────
            AppScreen.Login -> LoginScreen(
                initialEmail = authManager.savedEmail,
                onLogin = { email, password ->
                    authLoading = true
                    authError   = null
                    scope.launch {
                        // Paso 1: verificación local (SHA-256 contra EncryptedSharedPreferences)
                        if (authManager.hasLocalCredentials &&
                            !authManager.verifyLocalCredentials(email, password)) {
                            authLoading = false
                            authError   = "Credenciales incorrectas"
                            return@launch
                        }
                        // Paso 2: autenticación remota con Firebase
                        authManager.login(email, password)
                            .onSuccess {
                                authLoading   = false
                                tasks         = repository.loadTasks()
                                currentScreen = AppScreen.Main
                            }
                            .onFailure { e ->
                                authLoading = false
                                authError   = e.message
                            }
                    }
                },
                onGoToRegister = { authError = null; currentScreen = AppScreen.Register },
                isLoading    = authLoading,
                errorMessage = authError
            )

            // ── Registro ───────────────────────────────────────────────────
            AppScreen.Register -> RegisterScreen(
                onRegister = { email, password ->
                    authLoading = true
                    authError   = null
                    scope.launch {
                        authManager.register(email, password)
                            .onSuccess { authLoading = false; currentScreen = AppScreen.Main }
                            .onFailure { e -> authLoading = false; authError = e.message }
                    }
                },
                onGoToLogin  = { authError = null; currentScreen = AppScreen.Login },
                isLoading    = authLoading,
                errorMessage = authError
            )

            // ── Lista principal ────────────────────────────────────────────
            AppScreen.Main -> MainScreen(
                modifier   = Modifier.padding(innerPadding),
                tasks      = tasks,
                onAddTask  = { currentScreen = AppScreen.Form },
                onViewTask = { task -> currentScreen = AppScreen.Detail(task) },
                onDeleteTask = { task ->
                    repository.deleteTask(task.id)
                    tasks = repository.loadTasks()
                    scope.launch {
                        try { firebaseRepo.deleteTask(task.id.toString()) } catch (_: Exception) {}
                    }
                },
                onToggleCompleted = { task ->
                    repository.updateCompleted(task.id, !task.isCompleted)
                    tasks = repository.loadTasks()
                },
                onLogout = {
                    authManager.logout()
                    currentScreen = AppScreen.Login
                }
            )

            // ── Formulario nueva tarea ─────────────────────────────────────
            AppScreen.Form -> FormScreen(
                modifier            = Modifier.padding(innerPadding),
                title               = formTitle,
                description         = formDescription,
                latitude            = formLatitude,
                longitude           = formLongitude,
                onTitleChange       = { formTitle = it },
                onDescriptionChange = { formDescription = it },
                onPickLocation      = {
                    mapLauncher.launch(Intent(context, MapActivity::class.java))
                },
                onClearLocation = { formLatitude = null; formLongitude = null },
                onSave = { newTask ->
                    repository.addTask(newTask)
                    tasks = repository.loadTasks()
                    scope.launch {
                        try { firebaseRepo.addTask(newTask) } catch (_: Exception) {}
                    }
                    formTitle = ""; formDescription = ""
                    formLatitude = null; formLongitude = null
                    onTaskSaved()
                    currentScreen = AppScreen.Main
                },
                onBack = {
                    formTitle = ""; formDescription = ""
                    formLatitude = null; formLongitude = null
                    currentScreen = AppScreen.Main
                }
            )

            // ── Detalle de tarea ───────────────────────────────────────────
            is AppScreen.Detail -> {
                // Buscar la versión más actualizada de la tarea en la lista
                val currentTask = tasks.find { it.id == screen.task.id } ?: screen.task

                DetailScreen(
                    task = currentTask,
                    onBack = { currentScreen = AppScreen.Main },
                    onDelete = { task ->
                        repository.deleteTask(task.id)
                        tasks = repository.loadTasks()
                        scope.launch {
                            try { firebaseRepo.deleteTask(task.id.toString()) } catch (_: Exception) {}
                        }
                        currentScreen = AppScreen.Main
                    },
                    onToggleCompleted = { task ->
                        repository.updateCompleted(task.id, !task.isCompleted)
                        tasks = repository.loadTasks()
                        currentScreen = AppScreen.Detail(
                            tasks.find { it.id == task.id } ?: task
                        )
                    }
                )
            }
        }
    }
}
