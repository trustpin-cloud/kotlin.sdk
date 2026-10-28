package cloud.trustpin.android.sample.data.remote

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Pure parsers for the sample API's JSON bodies. Tolerant by design: a body
 * that does not have the expected shape yields `null`, never an exception.
 */
object SampleApiResponses {

    /** The login request body. */
    fun loginBody(username: String, password: String): String =
        JsonObject(mapOf("username" to JsonPrimitive(username), "password" to JsonPrimitive(password))).toString()

    /** `POST /login` → `{"token": "...", "tokenType": "Bearer", ...}`. */
    fun loginToken(body: String): String? = parseObject(body)?.string("token")

    /** What `GET /home` returned, reduced to what the sample shows. */
    class Home(val greeting: String?, val attestationSummary: String?)

    /**
     * `GET /home` → `{"greeting": ..., "attestation": {"platform", "env", "result",
     * "reasons", "bound"}}` on success, `{"error": ..., "reason": ...}` on a 401.
     */
    fun home(body: String): Home? {
        val root = parseObject(body) ?: return null
        val attestation = (root["attestation"] as? JsonObject)?.let(::describeAttestation)
        val rejection = root.string("reason")?.let { "rejected: $it" }
        return Home(greeting = root.string("greeting"), attestationSummary = attestation ?: rejection)
    }

    /** `android/production pass, bound [reason, …]`, matching the iOS sample's summary. */
    private fun describeAttestation(attestation: JsonObject): String = buildString {
        append(attestation.string("platform") ?: "?").append('/').append(attestation.string("env") ?: "?")
        append(' ').append(attestation.string("result") ?: "?")
        if ((attestation["bound"] as? JsonPrimitive)?.booleanOrNull == true) append(", bound")
        val reasons = (attestation["reasons"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
            .orEmpty()
        if (reasons.isNotEmpty()) append(reasons.joinToString(prefix = " [", postfix = "]"))
    }

    private fun parseObject(body: String): JsonObject? = try {
        Json.parseToJsonElement(body) as? JsonObject
    } catch (_: SerializationException) {
        null
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotEmpty() }

}
