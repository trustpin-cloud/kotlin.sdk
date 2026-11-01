package cloud.trustpin.android.sample.data

import cloud.trustpin.android.sample.data.remote.SampleApiResponses
import cloud.trustpin.android.sample.domain.model.ProtectedCallOutcome
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
        assertEquals("rejected: attestation_required (no TrustPin-Attestation header)", home.attestationSummary)
    }

    @Test
    fun transferWithVerifiedAssertion() {
        val home = SampleApiResponses.home(
            """{"greeting":"Transfer of 1.00 to demo accepted","transfer":{"amount":"1.00","to":"demo","status":"accepted"},""" +
                """"attestation":{"platform":"android","env":"production","result":"pass","reasons":[],"bound":true,"sub":"abcd1234","assertion":"verified"}}"""
        )!!
        assertEquals("Transfer of 1.00 to demo accepted", home.greeting)
        assertEquals("android/production pass, bound, assertion verified", home.attestationSummary)
    }

    @Test
    fun assertionRejectionShowsTheVerifierVerdict() {
        val home = SampleApiResponses.home(
            """{"error":"assertion_rejected","verifier":{"error":"attestation_failed","message":"assertion rejected","reasons":["assertion_invalid"]}}"""
        )!!
        assertEquals("rejected: assertion_rejected; verifier said attestation_failed (assertion rejected) [assertion_invalid]", home.attestationSummary)
        val tokenRejected = SampleApiResponses.home(
            """{"error":"assertion_rejected","verifier":{"error":"invalid_token","message":"platform, app or environment no longer allowed by the policy"}}"""
        )!!
        assertEquals(
            "rejected: assertion_rejected; verifier said invalid_token (platform, app or environment no longer allowed by the policy)",
            tokenRejected.attestationSummary,
        )
    }

    @Test
    fun responseExcerptIsOneBoundedLine() {
        assertEquals("{ \"a\": 1 }", ProtectedCallOutcome.excerpt("  {\n  \"a\": 1 }\n"))
        assertNull(ProtectedCallOutcome.excerpt("   "))
        assertEquals(ProtectedCallOutcome.EXCERPT_LENGTH, ProtectedCallOutcome.excerpt("x".repeat(5_000))!!.length)
    }

    @Test
    fun nonJsonBodyIsNull() {
        assertNull(SampleApiResponses.home("Not Found"))
    }
}
