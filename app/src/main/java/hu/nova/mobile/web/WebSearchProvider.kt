package hu.nova.mobile.web

data class WebSearchResult(val title: String, val snippet: String, val url: String)

sealed class WebSearchOutcome {
    data class Success(val results: List<WebSearchResult>) : WebSearchOutcome()
    object NoInternet : WebSearchOutcome()
    object NotConfigured : WebSearchOutcome()
    data class Error(val message: String) : WebSearchOutcome()
}

/**
 * Abstraction over web search, deliberately kept separate from [hu.nova.mobile.ai.AIProvider]
 * (see WebSearchRepository) so NOVA's reasoning is never conflated with actually reaching
 * the internet, and so the app can truthfully tell the user when it did or did not
 * search the web.
 */
interface WebSearchProvider {
    suspend fun search(query: String): WebSearchOutcome
}
