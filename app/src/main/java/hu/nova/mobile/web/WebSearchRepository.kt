package hu.nova.mobile.web

import android.content.Context
import hu.nova.mobile.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Performs a real web search when a search API key is configured (via Settings) and
 * internet is reachable; otherwise it returns [WebSearchOutcome.NotConfigured] /
 * [WebSearchOutcome.NoInternet] rather than fabricating results or silently falling
 * back to the AI's own (potentially stale) knowledge. This keeps
 * "AI reasoning" cleanly separate from "web search" per the project requirement.
 *
 * No search API key is bundled with the app; the user supplies their own (e.g. a
 * Brave Search / Bing / SerpAPI key) in Settings.
 */
class WebSearchRepository(
    private val settingsRepository: SettingsRepository
) : WebSearchProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class BraveWebResult(val title: String = "", val description: String = "", val url: String = "")

    @Serializable
    private data class BraveWebSection(val results: List<BraveWebResult> = emptyList())

    @Serializable
    private data class BraveSearchResponse(val web: BraveWebSection? = null)

    override suspend fun search(query: String): WebSearchOutcome = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.searchApiKey.first()
        if (apiKey.isBlank()) return@withContext WebSearchOutcome.NotConfigured

        val encoded = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://api.search.brave.com/res/v1/web/search?q=$encoded")
            .addHeader("Accept", "application/json")
            .addHeader("X-Subscription-Token", apiKey)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext WebSearchOutcome.Error("Search provider returned HTTP ${response.code}.")
                }
                val body = response.body?.string().orEmpty()
                val parsed = json.decodeFromString<BraveSearchResponse>(body)
                val results = parsed.web?.results.orEmpty().map {
                    WebSearchResult(title = it.title, snippet = it.description, url = it.url)
                }
                WebSearchOutcome.Success(results)
            }
        } catch (io: IOException) {
            WebSearchOutcome.NoInternet
        } catch (e: Exception) {
            WebSearchOutcome.Error(e.message ?: "Unknown search error.")
        }
    }

    companion object {
        fun hasNetworkConnection(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
}
