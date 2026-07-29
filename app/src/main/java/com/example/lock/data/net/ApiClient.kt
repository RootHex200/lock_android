package com.example.lock.data.net

import android.content.Context
import com.example.lock.BuildConfig
import com.example.lock.data.keystore.DeviceKeyStore
import com.example.lock.data.local.CredentialStore
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * U10: builds the Retrofit client with the signing interceptor + a
 * CertificatePinner (public-key pin) so the credential/command exchange can't
 * be MITM'd via a rogue CA (AND-003).
 */
object ApiClient {

    fun create(context: Context, baseUrl: String = BuildConfig.SERVER_URL): ApiService {
        val keyStore = DeviceKeyStore()
        val credentials = CredentialStore(context)

        val host = baseUrl.toHttpHostOrNull()
        val pinner = CertificatePinner.Builder().apply {
            if (host != null && BuildConfig.CERT_PIN_SHA256.isNotBlank()) {
                add(host, BuildConfig.CERT_PIN_SHA256)
            }
        }.build()

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .certificatePinner(pinner)
            .addInterceptor(SigningInterceptor(keyStore, credentials))
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun String.toHttpHostOrNull(): String? = runCatching {
        java.net.URI(this).host
    }.getOrNull()
}
