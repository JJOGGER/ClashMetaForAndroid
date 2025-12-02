package com.xboard.api

import android.util.Base64
import okhttp3.Interceptor
import okhttp3.Response
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 设备 ID 加密逻辑：
 *
 * 明文: "deviceID_" + deviceId
 * 算法: AES-256-GCM
 * 密钥: 固定 32 字节（请在实际项目中进行混淆/拆分存储）
 * IV: 随机 12 字节
 * 输出: Base64( IV || CIPHERTEXT || TAG )
 *
 * 对应 Header: X-Nonce
 */

// 实际使用时建议做混淆、拆分、运行时拼接等处理
private val RAW_KEY_BYTES = byteArrayOf(
    0x8f.toByte(), 0x2a, 0x4c, 0x6b, 0x1d, 0x93.toByte(), 0xe0.toByte(), 0xf7.toByte(),
    0xa4.toByte(), 0xb8.toByte(), 0xc3.toByte(), 0xd9.toByte(), 0x51, 0x27, 0xaa.toByte(), 0x4f,
    0x73, 0xc9.toByte(), 0xb0.toByte(), 0x82.toByte(), 0xe4.toByte(), 0xd5.toByte(), 0xf6.toByte(), 0xa3.toByte(),
    0xb0.toByte(), 0xc7.toByte(), 0xd9.toByte(), 0xe2.toByte(), 0xf1.toByte(), 0xa4.toByte(), 0xbc.toByte(), 0x03
)

private fun getSecretKey(): SecretKeySpec {
    require(RAW_KEY_BYTES.size == 32)
    return SecretKeySpec(RAW_KEY_BYTES, "AES")
}

object DeviceIdCrypto {
    private val secureRandom = SecureRandom()

    /**
     * 将设备 ID 加密为 nonce（X-Nonce）
     */
    fun encryptDeviceIdToNonce(deviceId: String): String {
        val plaintext = "deviceID_$deviceId".toByteArray(Charsets.UTF_8)

        // 1. 生成 12 字节 IV
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }

        // 2. AES/GCM/NoPadding
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = getSecretKey()
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        // 3. 加密（输出 = CIPHERTEXT || TAG）
        val cipherTextWithTag = cipher.doFinal(plaintext)

        // 4. 组合 IV || CIPHERTEXT || TAG
        val output = ByteArray(iv.size + cipherTextWithTag.size)
        System.arraycopy(iv, 0, output, 0, iv.size)
        System.arraycopy(cipherTextWithTag, 0, output, iv.size, cipherTextWithTag.size)

        // 5. Base64 无换行
        return Base64.encodeToString(output, Base64.NO_WRAP)
    }
}

/**
 * OkHttp 拦截器，在每个请求上附加 X-Nonce 头
 */
class DeviceNonceInterceptor(
    private val deviceIdProvider: () -> String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val deviceId = deviceIdProvider()
        val nonce = DeviceIdCrypto.encryptDeviceIdToNonce(deviceId)

        val newReq = original.newBuilder()
            .header("X-Nonce", nonce)
            .build()

        return chain.proceed(newReq)
    }
}


