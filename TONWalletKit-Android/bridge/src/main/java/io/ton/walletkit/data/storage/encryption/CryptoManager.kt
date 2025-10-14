package io.ton.walletkit.data.storage.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages encryption and decryption of sensitive data using Android Keystore.
 * Uses AES-256-GCM encryption for security and integrity.
 *
 * Key Features:
 * - Hardware-backed encryption when available (StrongBox on supported devices)
 * - AES-256-GCM for authenticated encryption
 * - Unique IV for each encryption operation
 * - Secure memory clearing after use
 */
class CryptoManager(
    private val keystoreAlias: String = DEFAULT_KEYSTORE_ALIAS,
) {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        // Ensure encryption key exists
        if (!keyStore.containsAlias(keystoreAlias)) {
            generateKey()
        }
    }

    /**
     * Encrypts the given plaintext data.
     *
     * @param plaintext The data to encrypt
     * @return Encrypted data with IV prepended (IV_SIZE bytes + ciphertext)
     * @throws SecurityException if encryption fails
     */
    fun encrypt(plaintext: String): ByteArray = try {
        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        val result = encrypt(plaintextBytes)
        // Clear plaintext from memory
        Arrays.fill(plaintextBytes, 0.toByte())
        result
    } catch (e: Exception) {
        Log.e(TAG, "Encryption failed", e)
        throw SecurityException("Failed to encrypt data", e)
    }

    /**
     * Encrypts the given plaintext bytes.
     *
     * @param plaintext The bytes to encrypt
     * @return Encrypted data with IV prepended (IV_SIZE bytes + ciphertext)
     */
    fun encrypt(plaintext: ByteArray): ByteArray {
        try {
            val cipher = getCipher()
            val secretKey = getSecretKey()

            // Let AndroidKeyStore generate a fresh IV to satisfy randomized encryption requirement
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv ?: throw IllegalStateException("Cipher did not provide an IV")
            if (iv.size != GCM_IV_SIZE) {
                throw IllegalStateException("Unexpected IV size: ${iv.size}")
            }

            // Encrypt the data
            val ciphertext = cipher.doFinal(plaintext)

            // Combine IV + ciphertext
            return ByteBuffer.allocate(GCM_IV_SIZE + ciphertext.size)
                .put(iv)
                .put(ciphertext)
                .array()
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw SecurityException("Failed to encrypt data", e)
        }
    }

    /**
     * Decrypts the given encrypted data.
     *
     * @param encryptedData The encrypted data with IV prepended
     * @return Decrypted plaintext string
     * @throws SecurityException if decryption fails
     */
    fun decrypt(encryptedData: ByteArray): String = try {
        val plaintextBytes = decryptToBytes(encryptedData)
        val result = String(plaintextBytes, Charsets.UTF_8)
        // Clear plaintext from memory
        Arrays.fill(plaintextBytes, 0.toByte())
        result
    } catch (e: Exception) {
        Log.e(TAG, "Decryption failed", e)
        throw SecurityException("Failed to decrypt data", e)
    }

    /**
     * Decrypts the given encrypted data to bytes.
     *
     * @param encryptedData The encrypted data with IV prepended
     * @return Decrypted plaintext bytes
     */
    fun decryptToBytes(encryptedData: ByteArray): ByteArray {
        try {
            if (encryptedData.size < GCM_IV_SIZE) {
                throw IllegalArgumentException("Encrypted data too short")
            }

            val cipher = getCipher()
            val secretKey = getSecretKey()

            // Extract IV and ciphertext
            val buffer = ByteBuffer.wrap(encryptedData)
            val iv = ByteArray(GCM_IV_SIZE)
            buffer.get(iv)
            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)

            // Initialize cipher for decryption
            val spec = GCMParameterSpec(GCM_TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            // Decrypt the data
            return cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw SecurityException("Failed to decrypt data", e)
        }
    }

    /**
     * Deletes the encryption key from the keystore.
     * WARNING: This will make all encrypted data unrecoverable!
     */
    fun deleteKey() {
        try {
            keyStore.deleteEntry(keystoreAlias)
            Log.d(TAG, "Encryption key deleted from keystore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete encryption key", e)
        }
    }

    /**
     * Checks if the encryption key exists in the keystore.
     */
    fun hasKey(): Boolean = keyStore.containsAlias(keystoreAlias)

    /**
     * Generates a new AES-256 encryption key in the Android Keystore.
     * Uses hardware-backed storage when available (StrongBox).
     */
    private fun generateKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE,
            )

            val builder = KeyGenParameterSpec.Builder(
                keystoreAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_SIZE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false) // Can be enabled for additional security

            // Try to use StrongBox (hardware-backed) if available (Android 9+)
            var strongBoxSuccess = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                try {
                    val strongBoxBuilder = KeyGenParameterSpec.Builder(
                        keystoreAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setIsStrongBoxBacked(true) // Request StrongBox

                    keyGenerator.init(strongBoxBuilder.build())
                    keyGenerator.generateKey()
                    strongBoxSuccess = true
                    Log.d(TAG, "Generated StrongBox-backed encryption key")
                } catch (e: Exception) {
                    // StrongBox not available, fall back to regular keystore
                    Log.d(TAG, "StrongBox not available, using regular keystore", e)
                }
            }

            // Fall back to regular keystore if StrongBox failed or not attempted
            if (!strongBoxSuccess) {
                keyGenerator.init(builder.build()) // Use original builder without StrongBox
                keyGenerator.generateKey()
                Log.d(TAG, "Generated encryption key in Android Keystore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate encryption key", e)
            throw SecurityException("Failed to generate encryption key", e)
        }
    }

    private fun getSecretKey(): SecretKey {
        val entry = keyStore.getEntry(keystoreAlias, null)
        if (entry !is KeyStore.SecretKeyEntry) {
            throw IllegalStateException("KeyStore entry is not a SecretKeyEntry")
        }
        return entry.secretKey
    }

    private fun getCipher(): Cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)

    companion object {
        private const val TAG = "CryptoManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEFAULT_KEYSTORE_ALIAS = "walletkit_master_key"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_SIZE = 12 // 96 bits recommended for GCM
        private const val GCM_TAG_SIZE = 128 // 128 bits authentication tag

        /**
         * Securely clears sensitive data from memory.
         * This helps prevent sensitive data from being recovered from memory dumps.
         */
        fun secureClear(data: CharArray) {
            Arrays.fill(data, '\u0000')
        }

        fun secureClear(data: ByteArray) {
            Arrays.fill(data, 0.toByte())
        }

        fun secureClear(data: String?): CharArray {
            val chars = data?.toCharArray() ?: CharArray(0)
            secureClear(chars)
            return chars
        }
    }
}
