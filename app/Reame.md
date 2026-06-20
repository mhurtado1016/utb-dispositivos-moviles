# 📘 Guía Completa: Aplicación de Tareas con Firebase, Google Maps y Almacenamiento

> **Objetivo:** Mejorar una aplicación de tareas existente en Android (Kotlin) integrando autenticación con Firebase Authentication, servicios de ubicación con Google Maps API y almacenamiento de datos con SQLite/Firebase Realtime Database.

---

## 📋 Tabla de Contenidos

1. [Resumen de la actividad](#1-resumen-de-la-actividad)
2. [Requisitos previos](#2-requisitos-previos)
3. [Configuración inicial del proyecto](#3-configuración-inicial-del-proyecto)
4. [Paso 1: Mejora de la UI/UX](#4-paso-1-mejora-de-la-uiux)
5. [Paso 2: Autenticación con Firebase](#5-paso-2-autenticación-con-firebase)
6. [Paso 3: Servicios de ubicación (Google Maps)](#6-paso-3-servicios-de-ubicación-google-maps)
7. [Paso 4: Almacenamiento de datos (SQLite + Firebase)](#7-paso-4-almacenamiento-de-datos-sqlite--firebase)
8. [Paso 5: Pruebas de funcionalidad y estabilidad](#8-paso-5-pruebas-de-funcionalidad-y-estabilidad)
9. [Estructura del informe técnico](#9-estructura-del-informe-técnico)
10. [Checklist de entrega final](#10-checklist-de-entrega-final)
11. [Prompt sugerido para el agente IA](#11-prompt-sugerido-para-el-agente-ia)
12. [Recursos adicionales](#12-recursos-adicionales)

---

## 1. Resumen de la actividad

| Aspecto | Detalle |
|---|---|
| **Tecnología** | Android Studio (Kotlin) |
| **Servicios externos** | Firebase Authentication, Google Maps API, SQLite / Firebase Realtime Database |
| **Producto final** | APK funcional + Informe técnico en PDF + Evidencia (video/capturas) |
| **Duración estimada** | 10–15 horas |

---

## 2. Requisitos previos

### 🛠️ Herramientas
- [Android Studio](https://developer.android.com/studio) (última versión estable)
- JDK 17 o superior
- Dispositivo físico Android o emulador (Google Play Services habilitado)

### 🔑 Cuentas y APIs necesarias
1. **Cuenta de Google** → [Firebase Console](https://console.firebase.google.com/)
2. **Google Cloud Console** → habilitar:
    - Maps SDK for Android
    - Places API (opcional)
    - Generar una **API Key** para Maps

---

## 3. Configuración inicial del proyecto

### 3.1 Crear el proyecto en Firebase
1. Ir a Firebase Console → **Add project** → nombre: `TasksApp`.
2. Agregar app Android con el `package name` exacto de tu proyecto.
3. Descargar `google-services.json` y colocarlo en `app/`.

### 3.2 Habilitar servicios en Firebase
- **Authentication** → Sign-in method → habilitar **Email/Password**.
- **Realtime Database** → Create Database → modo **test** (para desarrollo).

### 3.3 `AndroidManifest.xml` (permisos esenciales)

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.tuempresa.tasksapp">

    <!-- Permisos de red -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Permisos de ubicación -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.TasksApp">

        <!-- API Key de Google Maps -->
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="TU_API_KEY_AQUI" />

        <activity android:name=".ui.LoginActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".ui.MainActivity" />
        <activity android:name=".ui.RegisterActivity" />
        <activity android:name=".ui.MapActivity" />

    </application>
</manifest>
```

### 3.4 `build.gradle` (Project-level)

```gradle
plugins {
    id 'com.android.application' version '8.2.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.20' apply false
    id 'com.google.gms.google-services' version '4.4.0' apply false
}
```

### 3.5 `app/build.gradle` (dependencias)

```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.gms.google-services'
}

android {
    namespace 'com.tuempresa.tasksapp'
    compileSdk 34

    defaultConfig {
        applicationId "com.tuempresa.tasksapp"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildFeatures {
        viewBinding true
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // Firebase
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-auth-ktx'
    implementation 'com.google.firebase:firebase-database-ktx'

    // Google Maps
    implementation 'com.google.android.gms:play-services-maps:18.2.0'
    implementation 'com.google.android.gms:play-services-location:21.0.1'

    // SQLite
    implementation 'androidx.sqlite:sqlite:2.4.0'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
```

---

## 4. Paso 1: Mejora de la UI/UX

### 🎨 Principios aplicados
- **Material Design 3** con componentes `MaterialButton`, `TextInputLayout`, `CardView`.
- **Colores accesibles** (contraste mínimo AA, usar [Material Theme Builder](https://m3.material.io/theme-builder)).
- **Navegación intuitiva** con `BottomNavigationView` o `Navigation Component`.
- **Feedback visual** (progress bars, toasts, snackbars).

### 📄 Layout ejemplo: `activity_login.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="24dp">

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/tilEmail"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:hint="Correo electrónico"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etEmail"
            android:inputType="textEmailAddress"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"/>
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/tilPassword"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:hint="Contraseña"
        app:passwordToggleEnabled="true"
        app:layout_constraintTop_toBottomOf="@id/tilEmail"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="16dp">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etPassword"
            android:inputType="textPassword"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"/>
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnLogin"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="Iniciar sesión"
        app:layout_constraintTop_toBottomOf="@id/tilPassword"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="24dp"/>

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnRegister"
        style="@style/Widget.Material3.Button.TextButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="¿No tienes cuenta? Regístrate"
        app:layout_constraintTop_toBottomOf="@id/btnLogin"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 📄 Layout ejemplo: `activity_main.xml` (lista de tareas)

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:title="Mis tareas"
            app:menu="@menu/main_menu"/>
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvTasks"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior"/>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAddTask"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:src="@drawable/ic_add"
        android:contentDescription="Agregar tarea"/>

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### ♿ Accesibilidad
- Usar `android:contentDescription` en imágenes/iconos.
- Tamaño de fuente mínimo 14sp.
- Contraste de colores validado con [Accessibility Scanner](https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.auditor).

---

## 5. Paso 2: Autenticación con Firebase

### 5.1 Clase `AuthManager.kt`

```kotlin
package com.tuempresa.tasksapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun register(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() = auth.signOut()
}
```

### 5.2 `LoginActivity.kt`

```kotlin
package com.tuempresa.tasksapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tuempresa.tasksapp.data.AuthManager
import com.tuempresa.tasksapp.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Si ya está autenticado, ir al MainActivity
        if (authManager.currentUser != null) {
            navigateToMain()
            return
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            authManager.login(email, password)
                .onSuccess { navigateToMain() }
                .onFailure { e ->
                    Toast.makeText(this@LoginActivity,
                        "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
```

### 5.3 `RegisterActivity.kt`

```kotlin
package com.tuempresa.tasksapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tuempresa.tasksapp.data.AuthManager
import com.tuempresa.tasksapp.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            lifecycleScope.launch {
                authManager.register(email, password)
                    .onSuccess {
                        Toast.makeText(this@RegisterActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .onFailure { e ->
                        Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
}
```

---

## 6. Paso 3: Servicios de ubicación (Google Maps)

### 6.1 `MapActivity.kt`

```kotlin
package com.tuempresa.tasksapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.tuempresa.tasksapp.R

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION
            )
            return
        }
        googleMap.isMyLocationEnabled = true
        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                googleMap.addMarker(
                    MarkerOptions().position(latLng).title("Tu ubicación")
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission()
        }
    }
}
```

### 6.2 `activity_map.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<fragment xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/map"
    android:name="com.google.android.gms.maps.SupportMapFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

---

## 7. Paso 4: Almacenamiento de datos (SQLite + Firebase)

### 7.1 Modelo `Task.kt`

```kotlin
package com.tuempresa.tasksapp.model

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)
```

### 7.2 Opción A: SQLite local — `DBHelper.kt`

```kotlin
package com.tuempresa.tasksapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.tuempresa.tasksapp.model.Task

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "tasks.db"
        const val DATABASE_VERSION = 1
        const val TABLE_TASKS = "tasks"
        const val COL_ID = "id"
        const val COL_TITLE = "title"
        const val COL_DESC = "description"
        const val COL_DONE = "is_completed"
        const val COL_LAT = "latitude"
        const val COL_LNG = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val create = """
            CREATE TABLE $TABLE_TASKS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_DESC TEXT,
                $COL_DONE INTEGER DEFAULT 0,
                $COL_LAT REAL,
                $COL_LNG REAL
            )
        """.trimIndent()
        db.execSQL(create)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        onCreate(db)
    }

    fun insertTask(task: Task): Long {
        val values = ContentValues().apply {
            put(COL_TITLE, task.title)
            put(COL_DESC, task.description)
            put(COL_DONE, if (task.isCompleted) 1 else 0)
            put(COL_LAT, task.latitude)
            put(COL_LNG, task.longitude)
        }
        return writableDatabase.insert(TABLE_TASKS, null, values)
    }

    fun getAllTasks(): List<Task> {
        val list = mutableListOf<Task>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_TASKS", null)
        while (cursor.moveToNext()) {
            list.add(
                Task(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESC)),
                    isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_DONE)) == 1,
                    latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_LAT))) null
                               else cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LAT)),
                    longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_LNG))) null
                                else cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LNG))
                )
            )
        }
        cursor.close()
        return list
    }

    fun deleteTask(id: Long): Int =
        writableDatabase.delete(TABLE_TASKS, "$COL_ID=?", arrayOf(id.toString()))
}
```

### 7.3 Opción B: Firebase Realtime Database — `FirebaseTaskRepository.kt`

```kotlin
package com.tuempresa.tasksapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.tuempresa.tasksapp.model.Task
import kotlinx.coroutines.tasks.await

class FirebaseTaskRepository {
    private val db = FirebaseDatabase.getInstance()
        .reference.child("users")
        .child(FirebaseAuth.getInstance().currentUser?.uid ?: "")
        .child("tasks")

    suspend fun addTask(task: Task) {
        val key = db.push().key ?: return
        db.child(key).setValue(task).await()
    }

    suspend fun getTasks(): List<Task> {
        val snapshot = db.get().await()
        return snapshot.children.mapNotNull { it.getValue(Task::class.java) }
    }

    suspend fun deleteTask(id: String) {
        db.child(id).removeValue().await()
    }
}
```

> 💡 **Recomendación:** usar SQLite para acceso offline + Firebase para sincronización en la nube (patrón híbrido).

---

## 8. Paso 5: Pruebas de funcionalidad y estabilidad

### ✅ Checklist de pruebas

| Prueba | Criterio de éxito | Resultado |
|---|---|---|
| Registro de usuario | Se crea cuenta en Firebase | ⬜ |
| Inicio de sesión | Redirige a `MainActivity` | ⬜ |
| Cerrar sesión | Vuelve a `LoginActivity` | ⬜ |
| Permisos de ubicación | Se solicitan y conceden | ⬜ |
| Mapa carga | Muestra ubicación actual con marcador | ⬜ |
| Guardar tarea (SQLite) | Aparece en el RecyclerView | ⬜ |
| Guardar tarea (Firebase) | Visible en Firebase Console | ⬜ |
| Eliminar tarea | Se elimina de la lista y BD | ⬜ |
| Rotación de pantalla | No pierde datos | ⬜ |
| Sin conexión | SQLite sigue funcionando | ⬜ |
| Estabilidad (5 min de uso) | Sin crashes ni ANR | ⬜ |

### 🧪 Pruebas unitarias sugeridas (`src/test/`)

```kotlin
class AuthManagerTest {
    @Test
    fun `email vacio debe fallar validacion`() {
        // Implementar validación de email vacío
    }
}
```

### 📱 Pruebas instrumentadas (`src/androidTest/`)

```kotlin
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {
    @Test
    fun loginConCredencialesValidas_navegaAMain() {
        // Implementar con Espresso
    }
}
```

---

## 9. Estructura del informe técnico

### 📄 Plantilla del informe (PDF)

```
1. PORTADA
   - Título del proyecto
   - Nombre del estudiante
   - Fecha
   - Curso / materia

2. INTRODUCCIÓN
   - Objetivo de la actividad
   - Descripción general de la app

3. PROCEDIMIENTO DE INTEGRACIÓN
   3.1 Configuración del proyecto
   3.2 Integración de Firebase Authentication
   3.3 Integración de Google Maps API
   3.4 Configuración del almacenamiento (SQLite / Firebase)

4. RETOS ENCONTRADOS
   - Problemas con SHA-1 y firma de la app
   - Permisos de ubicación en Android 13+
   - Sincronización offline/online
   - Errores de dependencias en Gradle

5. SOLUCIONES IMPLEMENTADAS
   - Código clave que resolvió cada reto
   - Optimizaciones de UI/UX
   - Manejo de errores con try/catch y Result<T>

6. EVIDENCIA DE FUNCIONAMIENTO
   - Capturas de pantalla de cada funcionalidad
   - Enlace a video demostrativo (YouTube/Drive)

7. CONCLUSIONES
   - Aprendizajes obtenidos
   - Mejoras futuras

8. ANEXOS
   - Enlace al repositorio GitHub
   - Referencias / bibliografía
```

---

## 10. Checklist de entrega final

- [ ] Código fuente completo (Android Studio) comentado
- [ ] Repositorio en GitHub (público o privado compartido)
- [ ] `google-services.json` incluido (o instrucciones para generarlo)
- [ ] Informe técnico en PDF
- [ ] Video demostrativo (2–5 min) mostrando:
    - Registro e inicio de sesión
    - Creación de tarea con ubicación
    - Visualización del mapa
    - Cierre de sesión
- [ ] Capturas de pantalla de las pantallas principales
- [ ] APK firmado (opcional, en `/release`)

---

## 11. Prompt sugerido para el agente IA

Copia y pega este prompt cuando trabajes con tu agente IA (ChatGPT, Claude, Gemini, Qwen, etc.):

```
Actúa como un desarrollador Android experto en Kotlin. Tengo que entregar una
aplicación de tareas que integre:
1. Firebase Authentication (email/password)
2. Google Maps API (mostrar ubicación actual del usuario)
3. Almacenamiento local con SQLite y opcionalmente sincronización con
   Firebase Realtime Database
4. UI moderna con Material Design 3

Necesito que me ayudes con:
- Estructura del proyecto (carpetas y paquetes)
- Código completo en Kotlin de las Activities, ViewModels, repositorios y
  adaptadores
- Archivos XML de los layouts (Login, Registro, Lista de tareas, Mapa)
- AndroidManifest.xml con permisos
- Configuración de build.gradle (project y app)
- Explicación paso a paso para configurar Firebase y Google Maps
- Plantilla del informe técnico en PDF
- Checklist de pruebas

Usa ViewBinding, Coroutines, arquitectura MVVM y buenas prácticas.
El código debe estar comentado en español.
```

---

## 12. Recursos adicionales

- [Documentación Firebase Android](https://firebase.google.com/docs/android/setup)
- [Documentación Google Maps Android](https://developers.google.com/maps/documentation/android-sdk/start)
- [Material Design 3](https://m3.material.io/)
- [Android Developers - Permissions](https://developer.android.com/training/permissions)
- [Codelab Firebase Auth](https://codelabs.developers.google.com/codelabs/firebase-auth)

---

> ✨ **Consejo final:** Guarda cada paso en commits de Git con mensajes claros (ej: `feat: add firebase auth`, `feat: integrate google maps`). Esto te ayudará tanto para el informe como para no perder progreso.

¡Éxito con tu actividad! 🚀