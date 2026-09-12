package hu.nova.mobile.ai

/**
 * Fully offline, on-device implementation of [AIProvider].
 *
 * IMPORTANT / HONEST LIMITATION: this is NOT a local large language model. Shipping a
 * multi-gigabyte LLM by default would bloat the APK and most phones cannot run one at
 * usable speed. Instead this is a genuine rule-based conversational engine: it does
 * intent matching (greetings, thanks, memory recall, simple Q&A about NOVA itself) and
 * is deliberately honest with the user when a request is outside what it can do
 * offline, telling them to enable a remote provider or connect to the internet.
 *
 * The [AIProvider] interface is what makes it possible to later drop in a real local
 * model (e.g. via MediaPipe LLM Inference / llama.cpp bindings) without touching any
 * other layer of the app - a new class would just implement this same interface and be
 * registered in [AIProviderFactory].
 */
class LocalAIProvider : AIProvider {

    override val id: String = "local"

    override suspend fun generateReply(request: AIRequest): AIResult {
        val text = request.userMessage.trim().lowercase()
        val isHungarian = request.languageTag == "hu"

        val reply = when {
            text.isEmpty() -> if (isHungarian) "Nem hallottalak jól, mondanád még egyszer?" else "I didn't catch that, could you repeat it?"

            containsAny(text, "szia", "hello", "helló", "hi ", "hey") ->
                if (isHungarian) "Szia! Miben segíthetek?" else "Hi! How can I help?"

            containsAny(text, "köszönöm", "koszonom", "thanks", "thank you") ->
                if (isHungarian) "Szívesen!" else "You're welcome!"

            containsAny(text, "ki vagy", "who are you", "mi vagy") ->
                if (isHungarian)
                    "A NOVA vagyok, a telefonodon futó személyes asszisztensed. Offline módban egyszerűbb kérdésekre, alkalmazás- és beállítás-parancsokra, illetve a memóriádra tudok válaszolni."
                else
                    "I'm NOVA, your on-device assistant. In offline mode I can help with simple questions, app/settings commands, and things you've asked me to remember."

            request.memorySnippets.isNotEmpty() && containsAny(text, "emlékszel", "remember", "mit tudsz rólam", "what do you know about me") ->
                buildMemoryRecall(request.memorySnippets, isHungarian)

            else ->
                if (isHungarian)
                    "Offline módban vagyok, ezért ezt a kérést csak korlátozottan tudom értelmezni. Kapcsolj be internetet és válassz egy távoli AI szolgáltatót a Beállításokban a teljes válaszokhoz, vagy próbálj egy alkalmazás- vagy beállítás-parancsot."
                else
                    "I'm running offline, so I can only partly understand this request. Turn on internet and pick a remote AI provider in Settings for full answers, or try an app/settings command instead."
        }

        return AIResult.Success(reply)
    }

    private fun buildMemoryRecall(snippets: List<String>, isHungarian: Boolean): String {
        val bulletList = snippets.joinToString(separator = "\n") { "• $it" }
        return if (isHungarian) "Ezeket jegyeztem meg rólad:\n$bulletList" else "Here's what I remember about you:\n$bulletList"
    }

    private fun containsAny(text: String, vararg needles: String): Boolean = needles.any { text.contains(it) }
}
