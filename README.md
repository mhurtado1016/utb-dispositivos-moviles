# GestorDeTareas

Aplicación Android para la gestión de tareas personales con autenticación de usuarios, cifrado de datos sensibles y ubicación geográfica integrada.

Desarrollada en **Kotlin** con **Jetpack Compose** como parte del curso de Dispositivos Móviles — UTB.

---

## Características principales

- **Autenticación de usuarios** con Firebase Authentication (email/contraseña)
- **Verificación local** de credenciales con hash SHA-256 y EncryptedSharedPreferences
- **Cifrado AES-256-GCM** de datos sensibles (título y descripción de tareas) almacenados en SQLite
- **Sincronización** de tareas con Firebase Realtime Database
- **Ubicación geográfica** integrada: asigna y visualiza la ubicación de cada tarea en un mapa interactivo (Google Maps)
- **Interfaz moderna** con Material Design 3 y paleta de colores personalizada

---

## Seguridad implementada

| Mecanismo | Descripción |
|-----------|-------------|
| Firebase Authentication | Login y registro con email/contraseña; contraseñas hasheadas con bcrypt en servidores de Google |
| Verificación local (SHA-256) | Las credenciales se guardan localmente como hash SHA-256 dentro de EncryptedSharedPreferences |
| EncryptedSharedPreferences | AES-256-SIV para claves, AES-256-GCM para valores; protege preferencias del usuario en el dispositivo |
| Cifrado AES-256-GCM en SQLite | Título y descripción de cada tarea se cifran antes de guardarse en la base de datos local |
| Android Keystore | La clave criptográfica nunca abandona el enclave de hardware del dispositivo |
| TLS 1.3 | Toda comunicación con Firebase viaja cifrada |

---

## Stack tecnológico

- **Lenguaje:** Kotlin 2.2
- **UI:** Jetpack Compose + Material Design 3
- **Autenticación:** Firebase Authentication
- **Base de datos remota:** Firebase Realtime Database
- **Base de datos local:** SQLite (SQLiteOpenHelper)
- **Cifrado:** androidx.security:security-crypto (EncryptedSharedPreferences + Android Keystore AES-256-GCM)
- **Mapas:** Google Maps SDK + maps-compose 4.3.3
- **Ubicación:** FusedLocationProviderClient
- **Build:** Android Gradle Plugin 9.2.1, minSdk 24, targetSdk 36

---

## Estructura del proyecto

```
app/src/main/java/com/example/gestordetareas/
├── data/
│   ├── AuthManager.kt            # Autenticación Firebase + verificación local SHA-256
│   ├── CryptoManager.kt          # AES-256-GCM, EncryptedSharedPreferences, SHA-256
│   ├── DBHelper.kt               # SQLite con cifrado en reposo
│   └── FirebaseTaskRepository.kt
├── ui/
│   ├── LoginScreen.kt
│   ├── RegisterScreen.kt
│   ├── MapActivity.kt
│   └── theme/
├── MainActivity.kt               # Navegación principal
├── MainScreen.kt                 # Lista de tareas
├── FormScreen.kt                 # Crear tarea
├── DetailScreen.kt               # Detalle con mapa
├── Task.kt                       # Modelo de datos
└── TaskRepository.kt             # Repositorio local
```

---

## Configuración para ejecutar

1. Clona el repositorio
2. Abre el proyecto en **Android Studio Ladybug** o superior
3. Agrega tu archivo `google-services.json` en `app/` (obtenido desde Firebase Console)
4. Agrega tu clave de Google Maps en `AndroidManifest.xml`:
   ```xml
   <meta-data android:name="com.google.android.geo.API_KEY" android:value="TU_API_KEY"/>
   ```
5. Sincroniza Gradle y ejecuta en un dispositivo/emulador con API 24+

---

## Autor

**Manu Hurtado** — Universidad Tecnológica de Bolívar (UTB)  
Curso: Dispositivos Móviles · Junio 2026
