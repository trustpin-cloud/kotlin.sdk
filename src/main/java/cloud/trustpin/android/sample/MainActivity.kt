package cloud.trustpin.android.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinConfiguration
import cloud.trustpin.kotlin.sdk.TrustPinError
import cloud.trustpin.kotlin.sdk.TrustPinLogLevel
import cloud.trustpin.kotlin.sdk.TrustPinMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val TrustPinPrimary = Color(0xFF429488)

private val TrustPinLightColorScheme = lightColorScheme(
    primary = TrustPinPrimary,
    onPrimary = Color.White,
)

private val TrustPinDarkColorScheme = darkColorScheme(
    primary = TrustPinPrimary,
    onPrimary = Color.White,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        TrustPin.default.setLogLevel(TrustPinLogLevel.DEBUG)

        setContent {
            MaterialTheme(colorScheme = TrustPinLightColorScheme) {
                TrustPinSampleApp()
            }
        }
    }
}

@Composable
private fun TrustPinSampleApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    var organizationId by remember { mutableStateOf(context.getString(R.string.test_organization_id)) }
    var projectId by remember { mutableStateOf(context.getString(R.string.test_project_id)) }
    var publicKey by remember { mutableStateOf(context.getString(R.string.test_public_key)) }
    var testUrl by remember { mutableStateOf(context.getString(R.string.default_url)) }

    var isConfigured by remember { mutableStateOf(false) }
    var isConfiguring by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf(context.getString(R.string.status_not_configured)) }
    var logOutput by remember { mutableStateOf("Welcome to TrustPin Android Sample\nConfigure TrustPin and test connections...\n") }

    fun logMessage(message: String) {
        val timestamp = dateFormat.format(Date())
        logOutput += "[$timestamp] $message\n"
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(Unit) {
        logMessage("TrustPin Android Sample started")
        logMessage("TrustPin configured for debug logging")
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = context.getString(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )

            // TrustPin Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TrustPin Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    OutlinedTextField(
                        value = organizationId,
                        onValueChange = { organizationId = it },
                        label = { Text(context.getString(R.string.organization_id)) },
                        enabled = !isConfigured && !isConfiguring,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = projectId,
                        onValueChange = { projectId = it },
                        label = { Text(context.getString(R.string.project_id)) },
                        enabled = !isConfigured && !isConfiguring,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = publicKey,
                        onValueChange = { publicKey = it },
                        label = { Text(context.getString(R.string.public_key)) },
                        enabled = !isConfigured && !isConfiguring,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        minLines = 3,
                    )

                    Button(
                        onClick = {
                            if (isConfigured) {
                                showToast("TrustPin is already configured")
                                logMessage("Setup attempt ignored: TrustPin already configured")
                                return@Button
                            }

                            val orgId = organizationId.trim()
                            val projId = projectId.trim()
                            val pubKey = publicKey.trim()

                            if (orgId.isEmpty() || projId.isEmpty() || pubKey.isEmpty()) {
                                showToast("Please fill in all configuration fields")
                                logMessage("Configuration failed: Missing required fields")
                                return@Button
                            }

                            isConfiguring = true

                            scope.launch {
                                try {
                                    logMessage("Configuring TrustPin...")
                                    logMessage("   Organization ID: $orgId")
                                    logMessage("   Project ID: $projId")
                                    logMessage("   Public Key: ${pubKey.take(20)}...")

                                    TrustPin.default.setup(
                                        TrustPinConfiguration(
                                            orgId, projId, pubKey, mode = TrustPinMode.STRICT
                                        )
                                    )

                                    isConfigured = true
                                    isConfiguring = false
                                    statusText = context.getString(R.string.status_configured)
                                    showToast("TrustPin configured successfully!")
                                    logMessage("TrustPin configuration successful")
                                    logMessage("Configuration fields locked - TrustPin is now configured")
                                } catch (e: TrustPinError) {
                                    isConfigured = false
                                    isConfiguring = false
                                    statusText = context.getString(R.string.status_not_configured)
                                    showToast("Configuration failed: ${e.message}")
                                    logMessage("TrustPin configuration failed: ${e.message}")
                                    logMessage("   Error type: ${e::class.simpleName}")
                                } catch (e: Exception) {
                                    isConfigured = false
                                    isConfiguring = false
                                    statusText = context.getString(R.string.status_not_configured)
                                    showToast("Unexpected error: ${e.message}")
                                    logMessage("Unexpected error: ${e.message}")
                                }
                            }
                        },
                        enabled = !isConfigured && !isConfiguring,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (isConfiguring) "Configuring..."
                            else if (isConfigured) context.getString(R.string.setup_trustpin_configured)
                            else context.getString(R.string.setup_trustpin)
                        )
                    }
                }
            }

            // Connection Testing Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Connection Testing",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    OutlinedTextField(
                        value = testUrl,
                        onValueChange = { testUrl = it },
                        label = { Text(context.getString(R.string.test_url)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                if (!isConfigured) {
                                    showToast("Please configure TrustPin first")
                                    logMessage("Test connection failed: TrustPin not configured")
                                    return@Button
                                }
                                val url = testUrl.trim()
                                if (url.isEmpty()) {
                                    showToast("Please enter a test URL")
                                    logMessage("Test connection failed: No URL provided")
                                    return@Button
                                }

                                scope.launch {
                                    try {
                                        statusText = context.getString(R.string.status_testing)
                                        logMessage("Testing connection to: $url")

                                        val result = withContext(Dispatchers.IO) {
                                            performNetworkRequest(url) { logMessage(it) }
                                        }

                                        statusText = context.getString(R.string.status_configured)
                                        logMessage("Connection test successful!")
                                        logMessage("   Response: ${result.take(200)}${if (result.length > 200) "..." else ""}")
                                        showToast("Connection test successful!")
                                    } catch (e: TrustPinError) {
                                        statusText = context.getString(R.string.status_configured)
                                        logMessage("TrustPin validation failed: ${e.message}")
                                        logMessage("   Error type: ${e::class.simpleName}")
                                        showToast("TrustPin validation failed: ${e.message}")
                                    } catch (e: Exception) {
                                        statusText = context.getString(R.string.status_configured)
                                        logMessage("Connection failed: ${e.message}")
                                        showToast("Connection failed: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(context.getString(R.string.test_connection))
                        }

                        Button(
                            onClick = {
                                val url = testUrl.trim()
                                if (url.isEmpty()) {
                                    showToast("Please enter a test URL")
                                    logMessage("Fetch certificate failed: No URL provided")
                                    return@Button
                                }
                                val host = try {
                                    URL(url).host
                                } catch (e: Exception) {
                                    showToast("Invalid URL")
                                    logMessage("Fetch certificate failed: Invalid URL")
                                    return@Button
                                }

                                scope.launch {
                                    try {
                                        statusText = context.getString(R.string.status_fetching_cert)
                                        logMessage("Fetching certificate for: $host")

                                        val pem = TrustPin.fetchCertificate(host)

                                        val sha256 = MessageDigest.getInstance("SHA-256")
                                            .digest(pem.toByteArray())
                                            .joinToString("") { "%02x".format(it) }

                                        logMessage("Certificate PEM:\n$pem")
                                        logMessage("SHA-256 checksum: $sha256")

                                        statusText = if (isConfigured) context.getString(R.string.status_configured)
                                        else context.getString(R.string.status_not_configured)
                                        showToast("Certificate fetched successfully!")
                                    } catch (e: TrustPinError) {
                                        statusText = if (isConfigured) context.getString(R.string.status_configured)
                                        else context.getString(R.string.status_not_configured)
                                        logMessage("Fetch certificate failed: ${e.message}")
                                        showToast("Fetch certificate failed: ${e.message}")
                                    } catch (e: Exception) {
                                        statusText = if (isConfigured) context.getString(R.string.status_configured)
                                        else context.getString(R.string.status_not_configured)
                                        logMessage("Fetch certificate failed: ${e.message}")
                                        showToast("Fetch certificate failed: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(context.getString(R.string.fetch_certificate))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            logOutput = "Welcome to TrustPin Android Sample\nConfigure TrustPin and test connections...\n"
                            logMessage("Log cleared")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    ) {
                        Text(context.getString(R.string.clear_log))
                    }

                    // Status bar
                    Text(
                        text = statusText,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TrustPinPrimary)
                            .padding(8.dp),
                    )
                }
            }

            // Log Output Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = context.getString(R.string.log_output),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    val logScrollState = rememberScrollState()

                    LaunchedEffect(logOutput) {
                        logScrollState.animateScrollTo(logScrollState.maxValue)
                    }

                    Text(
                        text = logOutput,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp)
                            .background(Color(0xFFF5F5F5))
                            .verticalScroll(logScrollState)
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}

private suspend fun performNetworkRequest(url: String, log: (String) -> Unit): String =
    withContext(Dispatchers.IO) {
        val userAgent = "TrustPin-Android-Sample/1.0.0"
        val sslSocketFactory = TrustPin.default.makeSSLSocketFactory()
        val trustManager = TrustPin.default.makeTrustManager()
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory, trustManager)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", userAgent)
            .build()

        log("Making HTTP request...")
        log("   Method: GET")
        log("   URL: $url")
        log("   User-Agent: $userAgent")

        client.newCall(request).execute().use { response ->
            log("Response received:")
            log("   Status: ${response.code} ${response.message}")
            log("   Headers: ${response.headers.size} headers")

            return@withContext response.body.string()
        }
    }
