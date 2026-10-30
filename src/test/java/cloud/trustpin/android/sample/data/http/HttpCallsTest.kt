package cloud.trustpin.android.sample.data.http

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.Test
import kotlin.test.assertTrue

class HttpCallsTest {

    // The reported headers are the ones sent on the network, interceptors included
    @Test
    fun reportsHeadersSentOnTheNetwork() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("ok"))
        server.start()
        try {
            val client = OkHttpClient.Builder()
                .addNetworkInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().header("X-Test", "added").build())
                }
                .build()

            val result = runBlocking {
                client.newCall(Request.Builder().url(server.url("/")).build()).executeAsync()
            }

            assertTrue("X-Test" in result.sentHeaderNames, "got ${result.sentHeaderNames}")
        } finally {
            server.shutdown()
        }
    }
}
