package cloud.trustpin.android.sample.data

import cloud.trustpin.android.sample.data.config.SampleApiConfig
import cloud.trustpin.android.sample.domain.model.DomainError
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SampleApiConfigTest {

    private fun json(url: String? = "https://api.example.com/", user: String? = "demo", password: String? = "secret") =
        buildString {
            append('{')
            listOfNotNull(
                url?.let { "\"base_url\":\"$it\"" },
                user?.let { "\"username\":\"$it\"" },
                password?.let { "\"password\":\"$it\"" },
            ).joinTo(this, ",")
            append('}')
        }

    @Test
    fun parsesAndTrimsTheTrailingSlash() {
        val config = SampleApiConfig.parse(json())
        assertEquals("https://api.example.com", config.baseUrl)
        assertEquals("demo", config.username)
        assertEquals("secret", config.password)
    }

    @Test
    fun missingKeyIsNamed() {
        val error = assertFailsWith<DomainError.Validation> { SampleApiConfig.parse(json(password = null)) }
        assertTrue(error.message!!.contains("\"password\""))
    }

    @Test
    fun blankValueCountsAsMissing() {
        assertFailsWith<DomainError.Validation> { SampleApiConfig.parse(json(user = "  ")) }
    }

    @Test
    fun plainHttpIsRefused() {
        val error = assertFailsWith<DomainError.Validation> { SampleApiConfig.parse(json(url = "http://api.example.com")) }
        assertTrue(error.message!!.contains("https"))
    }

    @Test
    fun invalidJsonIsAValidationError() {
        assertFailsWith<DomainError.Validation> { SampleApiConfig.parse("{not json") }
        assertFailsWith<DomainError.Validation> { SampleApiConfig.parse("[1, 2]") }
    }

    @Test
    fun toStringNeverPrintsThePassword() {
        assertFalse(SampleApiConfig.parse(json()).toString().contains("secret"))
    }

    @Test
    fun theTemplateHasEveryKey() {
        // The public template must stay parseable so users only replace values.
        val template = File("test-api.json.example").takeIf { it.isFile } ?: File("AndroidSample/test-api.json.example")
        SampleApiConfig.parse(template.readText())
    }
}
