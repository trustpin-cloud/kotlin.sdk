package cloud.trustpin.android.sample.data.config

import cloud.trustpin.android.sample.domain.model.DomainError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The test API (`cloud-console-attest-sample-apps/api-mock`) the Attestation
 * tab logs in to, as in the iOS sample's `TrustPinAttestRepository.swift`.
 *
 * Read from `assets/test-api.json` (`base_url`, `username`, `password`). That
 * file is tracked in the private repository only: the release pipeline removes
 * it from the published sample, whose users start from `test-api.json.example`.
 * Without it the tab reports "not configured" instead of crashing.
 */
class SampleApiConfig(
    val baseUrl: String,
    val username: String,
    val password: String,
) {
    override fun toString(): String = "SampleApiConfig(baseUrl=$baseUrl)" // never print the password

    companion object {
        const val ASSET = "test-api.json"
        const val KEY_BASE_URL = "base_url"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"

        /** Parses [ASSET]'s text. Throws [DomainError.Validation] naming the missing or invalid key. */
        fun parse(text: String): SampleApiConfig {
            val root = try {
                Json.parseToJsonElement(text) as? JsonObject
            } catch (_: SerializationException) {
                null
            } ?: throw DomainError.Validation("$ASSET is not a JSON object")

            fun required(key: String): String =
                (root[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw DomainError.Validation("Test API is not configured: add \"$key\" to $ASSET")

            val baseUrl = required(KEY_BASE_URL).trimEnd('/')
            if (!baseUrl.startsWith("https://", ignoreCase = true)) {
                throw DomainError.Validation("\"$KEY_BASE_URL\" in $ASSET must be an https URL")
            }
            return SampleApiConfig(baseUrl, required(KEY_USERNAME), required(KEY_PASSWORD))
        }

        /** The message for a missing [ASSET]. */
        const val MISSING_MESSAGE = "Test API is not configured: add assets/$ASSET (start from test-api.json.example)"
    }
}
