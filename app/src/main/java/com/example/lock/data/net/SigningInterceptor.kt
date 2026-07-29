package com.example.lock.data.net

import com.example.lock.data.keystore.DeviceKeyStore
import com.example.lock.data.local.DeviceCredentials
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

/**
 * U10 / KTD-3: signs every device→server request with the Keystore key.
 * Adds X-Credential-Id, X-Timestamp and X-Signature (base64 DER ECDSA over the
 * canonical string). The activation call has no credential yet and is skipped.
 */
class SigningInterceptor(
    private val keyStore: DeviceKeyStore,
    private val credentials: DeviceCredentials,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000 },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val credentialId = credentials.credentialId()
        // No credential (enrollment/activation) → send unsigned.
        if (credentialId == null || !keyStore.hasKey()) {
            return chain.proceed(request)
        }

        val bodyBytes = request.body?.let { body ->
            Buffer().also { body.writeTo(it) }.readByteArray()
        } ?: ByteArray(0)

        val timestamp = clock().toString()
        val path = request.url.encodedPath
        val canonical = RequestSigner.canonicalString(request.method, path, timestamp, bodyBytes)
        val signature = keyStore.signBase64(canonical.toByteArray())

        val signed = request.newBuilder()
            .header("X-Credential-Id", credentialId)
            .header("X-Timestamp", timestamp)
            .header("X-Signature", signature)
            .build()

        return chain.proceed(signed)
    }
}
