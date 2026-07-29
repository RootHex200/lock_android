package com.example.lock.data.net

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * U10 / KTD-3: the canonical string MUST byte-match the server's DeviceAuth
 * (METHOD\n/path\ntimestamp\nsha256hex(body)).
 */
class RequestSignerTest {

    @Test
    fun `canonical string matches the server format`() {
        val canonical = RequestSigner.canonicalString(
            method = "post",
            path = "api/v1/devices/abc/heartbeat",
            timestamp = "1700000000",
            body = """{"reported_state":"LOCKED"}""".toByteArray(),
        )

        val expectedBodyHash = RequestSigner.sha256Hex("""{"reported_state":"LOCKED"}""".toByteArray())
        assertEquals(
            "POST\n/api/v1/devices/abc/heartbeat\n1700000000\n$expectedBodyHash",
            canonical,
        )
    }

    @Test
    fun `empty body hashes to the sha256 of empty string`() {
        val hash = RequestSigner.sha256Hex(ByteArray(0))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash)
    }

    @Test
    fun `path is normalised with a single leading slash`() {
        val a = RequestSigner.canonicalString("GET", "/x/y", "1", ByteArray(0))
        val b = RequestSigner.canonicalString("GET", "x/y", "1", ByteArray(0))
        assertEquals(a, b)
    }
}
