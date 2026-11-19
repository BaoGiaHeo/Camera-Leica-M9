package com.example.camui

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the Developer log password encrypted with an Android Keystore AES key.
 * This protects the saved preference at rest on this device; it is not a substitute
 * for server-side authentication because this is a client-side Developer build.
 */
internal object DeveloperPasswordStore {
    private const val PREFS = "developer_access_private"
    private const val CIPHER_TEXT = "password_ciphertext"
    private const val INITIALIZATION_VECTOR = "password_iv"
    private const val KEY_ALIAS = "fujifilm_mode_developer_log_access"
    private const val INITIAL_PASSWORD = "HuynGiaBao2011?"

    @Synchronized
    fun getOrInitialize(context: Context): String {
        val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val encodedCiphertext = preferences.getString(CIPHER_TEXT, null)
        val encodedIv = preferences.getString(INITIALIZATION_VECTOR, null)

        if (!encodedCiphertext.isNullOrBlank() && !encodedIv.isNullOrBlank()) {
            val restored = runCatching {
                decrypt(
                    Base64.decode(encodedCiphertext, Base64.NO_WRAP),
                    Base64.decode(encodedIv, Base64.NO_WRAP)
                )
            }.getOrNull()
            if (!restored.isNullOrEmpty()) return restored
        }

        val (ciphertext, iv) = encrypt(INITIAL_PASSWORD)
        preferences.edit()
            .putString(CIPHER_TEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(INITIALIZATION_VECTOR, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
        return INITIAL_PASSWORD
    }

    fun matches(context: Context, entered: String): Boolean {
        val expected = getOrInitialize(context).toByteArray(Charsets.UTF_8)
        val actual = entered.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return ciphertext to cipher.iv
    }

    private fun decrypt(ciphertext: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }
}

@Composable
internal fun LunaDreamDeveloperLogGate() {
    val context = LocalContext.current
    var enteredPassword by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(false) }
    var showSavedPassword by remember { mutableStateOf(false) }
    var invalidPassword by remember { mutableStateOf(false) }
    val savedPassword = remember(context) {
        runCatching { DeveloperPasswordStore.getOrInitialize(context) }
            .getOrElse { "Không thể giải mã mật khẩu đã lưu trên thiết bị này." }
    }

    if (unlocked) {
        LunaDreamDeveloperLogPanel()
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            "DEVELOPER AUTHENTICATION",
            color = FujiSkin.lcdGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Nhập mật khẩu Developer để mở LOG LunaDream Working. Quyền truy cập chỉ được mở trong phiên màn hình hiện tại.",
            color = CamColors.Sub,
            fontSize = 11.sp
        )
        OutlinedTextField(
            value = enteredPassword,
            onValueChange = {
                enteredPassword = it
                invalidPassword = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mật khẩu Developer") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FujiChip(
                "MỞ LOG",
                false,
                {
                    if (runCatching { DeveloperPasswordStore.matches(context, enteredPassword) }.getOrDefault(false)) {
                        unlocked = true
                        enteredPassword = ""
                        invalidPassword = false
                    } else {
                        invalidPassword = true
                    }
                },
                Modifier.weight(1f)
            )
            FujiChip(
                if (showSavedPassword) "ẨN MẬT KHẨU" else "HIỆN MẬT KHẨU ĐÃ LƯU",
                false,
                { showSavedPassword = !showSavedPassword },
                Modifier.weight(1f)
            )
        }
        if (invalidPassword) {
            Text("Mật khẩu không đúng. Hãy thử lại.", color = Color(0xFFFF806F), fontSize = 11.sp)
        }
        if (showSavedPassword) {
            Text(
                "MẬT KHẨU DEVELOPER ĐÃ LƯU: $savedPassword",
                color = FujiSkin.orange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            "Mật khẩu được lưu mã hóa cục bộ bằng Android Keystore trên thiết bị.",
            color = CamColors.Sub,
            fontSize = 10.sp
        )
    }
}
