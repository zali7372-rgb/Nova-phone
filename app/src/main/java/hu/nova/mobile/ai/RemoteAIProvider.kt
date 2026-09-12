package hu.nova.mobile.ai

import hu.nova.mobile.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Calls a remote, user-configured AI HTTP endpoint (Anthropic-Messages-API shaped by
 * default, but the base URL is user-editable in Settings so this is not locked to one
 * vendor). The API key is supplied by the user at runtime via Settings and stored only
 * in local DataStore - it is NEVER hard-coded, checked into source, or bundled with the
 * app. If no key has been configured, this provider fails fast with
 * [AIFailureReason.INVALID_CONFIG] rather than silently falling back or pretending to
 * have answered.
 */
class RemoteAIProvider(
    private val settingsRepository: SettingsRepository
) : AIProvider {

    override val id: String = "remote"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class RemoteMessage(val role: String, val content: String)

    @Serializable
    private data class RemoteRequestBody(
        val model: String = "claude-sonnet-4-6",
        val max_tokens: Int = 1000,
        val system: String,
        val messages: List<RemoteMessage>
    )

    @Serializable
    private data class RemoteContentBlock(val type: String, val text: String? = null)

    @Serializable
    private data class RemoteResponseBody(val content: List<RemoteContentBlock> = emptyList())

    override suspend fun generateReply(request: AIRequest): AIResult = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.remoteApiKey.first()
        val baseUrl = settingsRepository.remoteApiBaseUrl.first()

        if (apiKey.isBlank()) {
            return@withContext AIResult.Failure(
                AIFailureReason.INVALID_CONFIG,
                "No remote API key configured. Add one in Settings to use a remote AI provider."
            )
        }

        val systemPrompt = buildSystemPrompt(request)
        val messages = request.history.map {
            RemoteMessage(role = if (it.sender.name == "USER") "user" else "assistant", content = it.text)
        } + RemoteMessage(role = "user", content = request.userMessage)

        val bodyJson = json.encodeToString(RemoteRequestBody(system = systemPrompt, messages = messages))

        val httpRequest = Request.Builder()
            .url(baseUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(httpRequest).execute().use { response ->
                when {
                    response.code == 401 || response.code == 403 ->
                        AIResult.Failure(AIFailureReason.INVALID_CONFIG, "The API key was rejected (HTTP ${response.code}).")
                    response.code == 429 ->
                        AIResult.Failure(AIFailureReason.QUOTA_EXHAUSTED, "Rate limit / quota exhausted.")
                    !response.isSuccessful ->
                        AIResult.Failure(AIFailureReason.PROVIDER_UNAVAILABLE, "Remote provider returned HTTP ${response.code}.")
                    else -> {
                        val raw = response.body?.string().orEmpty()
                        val parsed = json.decodeFromString<RemoteResponseBody>(raw)
                        val text = parsed.content.firstOrNull { it.type == "text" }?.text
                        if (text.isNullOrBlank()) {
                            AIResult.Failure(AIFailureReason.UNKNOWN, "Empty response from remote provider.")
                        } else {
                            AIResult.Success(text)
                        }
                    }
                }
            }
        } catch (io: IOException) {
            AIResult.Failure(AIFailureReason.NO_INTERNET, io.message ?: "Network error.")
        } catch (e: Exception) {
            AIResult.Failure(AIFailureReason.UNKNOWN, e.message ?: "Unknown error.")
        }
    }

    private fun buildSystemPrompt(request: AIRequest): String {
        val memoryBlock = if (request.memorySnippets.isEmpty()) "" else
            "Known facts/preferences about the user:\n" + request.memorySnippets.joinToString("\n") { "- $it" }
        val language = if (request.languageTag == "hu") "Respond in Hungarian." else "Respond in English."
        return "You are NOVA, a helpful personal assistant embedded in an Android app. $language\n$memoryBlock"
    }
}
