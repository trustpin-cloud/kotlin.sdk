package cloud.trustpin.android.sample.data

import cloud.trustpin.android.sample.data.remote.SampleApiResponses
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SampleApiResponsesTest {

    @Test
    fun loginBodyEscapesValues() {
        val body = Json.parseToJsonElement(SampleApiResponses.loginBody("de\"mo", "p\\w")).jsonObject
        assertEquals("de\"mo", body["username"]!!.jsonPrimitive.content)
        assertEquals("p\\w", body["password"]!!.jsonPrimitive.content)
    }

    @Test
    fun loginToken() {
        assertEquals("abc123", SampleApiResponses.loginToken("""{"token":"abc123","tokenType":"Bearer","expiresIn":3600}"""))
        assertNull(SampleApiResponses.loginToken("""{"error":"invalid_credentials"}"""))
        assertNull(SampleApiResponses.loginToken("<html>"))
    }

    @Test
    fun homeWithVerifiedAttestation() {
        val home = SampleApiResponses.home(
            """{"greeting":"Welcome back, Demo!","attestation":{"platform":"android","app":"x","env":"production","result":"pass","reasons":[],"bound":true,"sub":"abcd1234"}}"""
        )!!
        assertEquals("Welcome back, Demo!", home.greeting)
        assertEquals("android/production pass, bound", home.attestationSummary)
    }

    @Test
    fun homeInMonitorModeListsReasons() {
        val home = SampleApiResponses.home(
            """{"attestation":{"platform":"android","env":"production","result":"fail","reasons":["verified_boot","patch_level"],"bound":false}}"""
        )!!
        assertEquals("android/production fail [verified_boot, patch_level]", home.attestationSummary)
    }

    @Test
    fun rejectionReasonIsShown() {
        val home = SampleApiResponses.home("""{"error":"attestation_required","reason":"no TrustPin-Attestation header"}""")!!
        assertNull(home.greeting)
        assertEquals("rejected: no TrustPin-Attestation header", home.attestationSummary)
    }

    @Test
    fun nonJsonBodyIsNull() {
        assertNull(SampleApiResponses.home("Not Found"))
    }
}
