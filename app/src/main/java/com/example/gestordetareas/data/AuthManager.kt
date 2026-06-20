package com.example.gestordetareas.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Gestiona la autenticación del usuario con Firebase Authentication.
 *
 * Además de delegar en Firebase, persiste el último email usado en
 * [EncryptedSharedPreferences] (a través de [CryptoManager.securePrefs])
 * para pre-llenar el campo de email en la pantalla de login.
 *
 * @param context Contexto de la aplicación, necesario para EncryptedSharedPreferences.
 */
class AuthManager(context: Context) {

    private val auth   : FirebaseAuth = FirebaseAuth.getInstance()
    private val crypto : CryptoManager = CryptoManager(context)

    companion object {
        private const val KEY_LAST_EMAIL    = "last_email"
        private const val KEY_LOCAL_EMAIL   = "local_email"
        private const val KEY_LOCAL_PWHASH  = "local_pw_hash"
    }

    /** Usuario actualmente autenticado, o null si no hay sesión activa. */
    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Indica si hay una sesión activa. */
    val isLoggedIn: Boolean get() = auth.currentUser != null

    /**
     * Último email usado con login exitoso, leído de [EncryptedSharedPreferences].
     * Devuelve null si nunca se ha iniciado sesión en este dispositivo.
     */
    val savedEmail: String?
        get() = crypto.securePrefs.getString(KEY_LAST_EMAIL, null)

    /**
     * Indica si existen credenciales locales guardadas en este dispositivo.
     * Si es false, la verificación local se omite y se va directo a Firebase.
     */
    val hasLocalCredentials: Boolean
        get() = crypto.securePrefs.getString(KEY_LOCAL_PWHASH, null) != null

    /**
     * Verifica las credenciales ingresadas contra las almacenadas localmente.
     *
     * La contraseña nunca se guarda en texto plano: se compara el SHA-256
     * del input con el hash almacenado en [EncryptedSharedPreferences].
     *
     * @return true si el email y el hash de contraseña coinciden con los guardados.
     */
    fun verifyLocalCredentials(email: String, password: String): Boolean {
        val storedEmail  = crypto.securePrefs.getString(KEY_LOCAL_EMAIL,  null) ?: return false
        val storedHash   = crypto.securePrefs.getString(KEY_LOCAL_PWHASH, null) ?: return false
        val inputHash    = crypto.hashSHA256(password)
        return email.trim().lowercase() == storedEmail && inputHash == storedHash
    }

    /** Persiste las credenciales localmente (email en claro + SHA-256 de la contraseña). */
    private fun saveLocalCredentials(email: String, password: String) {
        crypto.securePrefs.edit()
            .putString(KEY_LOCAL_EMAIL,  email.trim().lowercase())
            .putString(KEY_LOCAL_PWHASH, crypto.hashSHA256(password))
            .apply()
    }

    /**
     * Registra un nuevo usuario con email y contraseña.
     *
     * @param email Correo electrónico del usuario.
     * @param password Contraseña (mínimo 6 caracteres por requerimiento de Firebase).
     * @return [Result.success] con el [FirebaseUser] creado, o [Result.failure] con el error.
     */
    suspend fun register(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        // Guardar email (pre-llenado) y credenciales locales (hash SHA-256)
        crypto.securePrefs.edit().putString(KEY_LAST_EMAIL, email).apply()
        saveLocalCredentials(email, password)
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Inicia sesión con email y contraseña.
     *
     * Tras un login exitoso, persiste el email en [EncryptedSharedPreferences]
     * para que la próxima apertura de la app muestre el campo pre-llenado.
     *
     * @param email Correo electrónico del usuario.
     * @param password Contraseña del usuario.
     * @return [Result.success] con el [FirebaseUser] autenticado, o [Result.failure] con el error.
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        // Persistir email (pre-llenado) y credenciales locales (hash SHA-256)
        crypto.securePrefs.edit().putString(KEY_LAST_EMAIL, email).apply()
        saveLocalCredentials(email, password)
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Cierra la sesión del usuario actual.
     * El email guardado se conserva para pre-llenar el login la próxima vez.
     */
    fun logout() = auth.signOut()
}
