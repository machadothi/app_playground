package com.machadothi.templateapp.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("heliostat")

/**
 * What the app remembers between launches: where the heliostat is on the network,
 * which BLE device it is, and -- encrypted -- the WiFi password it was given, so
 * re-provisioning after a router change does not mean typing it again.
 */
@Singleton
class HeliostatPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val host = stringPreferencesKey("host")
    private val deviceAddress = stringPreferencesKey("device_address")
    private val ssid = stringPreferencesKey("ssid")
    private val encryptedPassword = stringPreferencesKey("wifi_password")

    val hostFlow: Flow<String?> = context.dataStore.data.map { it[host] }

    suspend fun host(): String? = hostFlow.first()
    suspend fun ssid(): String? = context.dataStore.data.first()[ssid]

    suspend fun saveProvisioned(hostIp: String, address: String, networkSsid: String, password: String) {
        context.dataStore.edit {
            it[host] = hostIp
            it[deviceAddress] = address
            it[ssid] = networkSsid
            it[encryptedPassword] = SecretBox.encrypt(password)
        }
    }

    suspend fun setHost(hostIp: String) {
        context.dataStore.edit { it[host] = hostIp }
    }

    /** The remembered password for [networkSsid], or null. */
    suspend fun password(networkSsid: String): String? {
        val prefs = context.dataStore.data.first()
        if (prefs[ssid] != networkSsid) return null
        return prefs[encryptedPassword]?.let { runCatching { SecretBox.decrypt(it) }.getOrNull() }
    }

    suspend fun forgetHeliostat() {
        context.dataStore.edit {
            it.remove(host)
            it.remove(deviceAddress)
        }
    }
}

/**
 * AES-GCM with a key that never leaves the Android Keystore. Replaces
 * EncryptedSharedPreferences, which androidx has deprecated.
 */
private object SecretBox {
    private const val ALIAS = "heliostat_wifi"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val sealed = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, IV_BYTES))
        }
        return String(cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }
}
