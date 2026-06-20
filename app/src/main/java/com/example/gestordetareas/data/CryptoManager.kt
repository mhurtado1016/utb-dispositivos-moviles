package com.example.gestordetareas.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Gestor de cifrado para la aplicación GestorDeTareas.
 *
 * Proporciona dos mecanismos de seguridad:
 *  1. [EncryptedSharedPreferences] (AES256-SIV/AES256-GCM) para almacenar
 *     preferencias seguras del usuario (credenciales, tokens, etc.).
 *  2. Cifrado AES-256-GCM vía Android Keystore para proteger campos de texto
 *     sensibles (título y descripción de tareas) antes de escribirlos en SQLite.
 *
 * Todos los valores se codifican en Base64 URL-safe sin padding para
 * facilitar su almacenamiento como cadenas en la base de datos.
 *
 * @param context Contexto de la aplicación (se usa applicationContext internamente).
 */
class CryptoManager(context: Context) {

    private val appContext = context.applicationContext

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------
    companion object {
        private const val KEYSTORE_PROVIDER  = "AndroidKeyStore"
        private const val KEY_ALIAS          = "GestorTareasAES256Key"
        private const val TRANSFORMATION     = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH     = 128            // bits
        private const val IV_SIZE_BYTES      = 12
        private const val PREFS_FILE_NAME    = "secure_prefs"
    }

    // -------------------------------------------------------------------------
    // EncryptedSharedPreferences (AES256-SIV para claves, AES256-GCM para valores)
    // -------------------------------------------------------------------------

    /**
     * Instancia de [EncryptedSharedPreferences] lista para usar.
     * Útil para guardar/leer credenciales de usuario u otros datos sensibles
     * que deban persistir de forma cifrada entre sesiones.
     *
     * Ejemplo de uso:
     * ```kotlin
     * cryptoManager.securePrefs.edit().putString("email", userEmail).apply()
     * val email = cryptoManager.securePrefs.getString("email", null)
     * ```
     */
    val securePrefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // -------------------------------------------------------------------------
    // Cifrado AES-256-GCM vía Android Keystore (para campos en SQLite)
    // -------------------------------------------------------------------------

    /**
     * Obtiene (o genera si no existe) la clave AES-256 en el Android Keystore.
     * La clave nunca sale del hardware/enclave de seguridad del dispositivo.
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        // Devolver clave existente si ya fue generada
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        // Generar nueva clave AES-256-GCM en el Keystore
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    /**
     * Cifra un texto plano con AES-256-GCM.
     *
     * El resultado incluye el IV (12 bytes) concatenado con el texto cifrado,
     * codificado en Base64 URL-safe sin padding.
     *
     * @param plainText Texto a cifrar.
     * @return Cadena Base64 que contiene IV + ciphertext, o el texto original
     *         si [plainText] está vacío.
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return plainText

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val iv         = cipher.iv                              // 12 bytes generados por el sistema
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Concatenar IV + ciphertext → Base64
        val combined = iv + cipherText
        return Base64.encodeToString(combined, Base64.URL_SAFE or Base64.NO_PADDING)
    }

    // -------------------------------------------------------------------------
    // Hashing SHA-256 (para contraseñas locales)
    // -------------------------------------------------------------------------

    /**
     * Genera el hash SHA-256 de un texto y lo devuelve en hexadecimal.
     *
     * Se usa para almacenar contraseñas de forma segura: nunca se guarda el
     * texto plano, solo su huella irreversible.
     *
     * @param input Texto a hashear (contraseña en texto plano).
     * @return Cadena hexadecimal de 64 caracteres.
     */
    fun hashSHA256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes  = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Descifra un texto previamente cifrado con [encrypt].
     *
     * @param encryptedText Cadena Base64 (IV + ciphertext).
     * @return Texto plano original, o [encryptedText] si la cadena está vacía
     *         o no pudo descifrarse.
     */
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return encryptedText

        return try {
            val combined   = Base64.decode(encryptedText, Base64.URL_SAFE or Base64.NO_PADDING)
            val iv         = combined.copyOfRange(0, IV_SIZE_BYTES)
            val cipherText = combined.copyOfRange(IV_SIZE_BYTES, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH, iv)
            )
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            // Si el valor no está cifrado (migración desde versión anterior),
            // devolver el texto tal cual para no perder datos.
            encryptedText
        }
    }
}
