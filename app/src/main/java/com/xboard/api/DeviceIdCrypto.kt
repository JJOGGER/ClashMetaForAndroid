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
    0x45, 0x48, 0x02, 0x91.toByte(), 0xdd.toByte(), 0x75, 0x99.toByte(), 0xd1.toByte(),
    0x6c, 0x7c, 0x3e, 0x7d, 0x33, 0x41, 0x18, 0x75,
    0xee.toByte(), 0xf2.toByte(), 0xc0.toByte(), 0x5c, 0x16, 0x0b, 0x78, 0x06,
    0x50, 0x5a, 0x1e, 0xdc.toByte(), 0xee.toByte(), 0x29, 0x68, 0x83.toByte()
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


