package com.vision.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Encrypted settings: API key, voice, and the in-app phone-control switch. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(c: Context) {
        if (::sp.isInitialized) return
        val app = c.applicationContext
        val key = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        sp = EncryptedSharedPreferences.create(
            app, "vision_prefs", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var apiKey: String
        get() = sp.getString("api", "") ?: ""
        set(v) = sp.edit().putString("api", v.trim()).apply()

    var voice: String
        get() = sp.getString("voice", "Charon") ?: "Charon"
        set(v) = sp.edit().putString("voice", v.trim()).apply()

    /** Soft switch: when false, every phone-control tool refuses to act. */
    var controlEnabled: Boolean
        get() = sp.getBoolean("control", true)
        set(v) = sp.edit().putBoolean("control", v).apply()
}
