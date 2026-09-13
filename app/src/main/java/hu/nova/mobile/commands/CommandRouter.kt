package hu.nova.mobile.commands

import android.content.Context
import hu.nova.mobile.apps.AppDiscoveryRepository
import hu.nova.mobile.apps.AppMatcher
import hu.nova.mobile.apps.SystemSettingsLauncher
import hu.nova.mobile.apps.SystemSettingsTarget
import hu.nova.mobile.data.repository.MemoryRepository
import hu.nova.mobile.data.repository.SettingsRepository
import hu.nova.mobile.domain.model.AppLanguage
import hu.nova.mobile.domain.model.MemoryCategory
import hu.nova.mobile.web.WebSearchOutcome
import hu.nova.mobile.web.WebSearchRepository
import kotlinx.coroutines.flow.first

/**
 * Central intent router: given a raw transcript/typed message, decides whether it's an
 * app launch, a system-settings shortcut, a memory save/read/delete, a web-search
 * request, a language switch, or none of the above (in which case the caller should
 * hand the message to the active [hu.nova.mobile.ai.AIProvider] instead).
 *
 * Matching is pattern + alias based on purpose: it's meant to be fast, transparent, and
 * fully offline-capable (app launching, settings, and memory all work with no
 * internet), not a black box.
 */
class CommandRouter(
    private val appDiscoveryRepository: AppDiscoveryRepository,
    private val memoryRepository: MemoryRepository,
    private val webSearchRepository: WebSearchRepository,
    private val settingsRepository: SettingsRepository
) {

    private val openAppPhrases = listOf(
        "nyisd meg a ", "nyisd meg ", "indítsd el a ", "indítsd el ", "indíts ", "open ", "launch ", "start "
    )
    private val settingsPhrases = listOf(
        "nyisd meg a beállításokat", "beállítások megnyitása", "open settings"
    )
    private val memorySavePhrases = listOf(
        "jegyezd meg, hogy ", "jegyezd meg hogy ", "ne felejtsd el, hogy ", "remember that ", "remember, that "
    )
    private val memoryReadPhrases = listOf(
        "mit tudsz rólam", "mire emlékszel", "what do you remember about me", "what do you know about me"
    )
    private val memoryClearPhrases = listOf(
        "töröld a memóriát", "felejts el mindent", "clear my memory", "forget everything"
    )
    private val searchPhrases = listOf(
        "keress rá erre", "keress rá", "keress nekem", "keress meg", "mi történt ma", "search for ", "search the web for ", "look up "
    )

    suspend fun route(rawText: String, context: Context, language: AppLanguage): CommandOutcome {
        val text = rawText.trim()
        val lower = text.lowercase()

        settingsSystemMatch(lower)?.let { target ->
            return handleOpenSettings(context, target, language)
        }

        openAppPhrases.firstOrNull { lower.startsWith(it) }?.let { prefix ->
            val appQuery = text.substring(prefix.length).removeSuffix("-ot").removeSuffix("-et")
                .removeSuffix(".").trim()
            return handleOpenApp(appQuery, language)
        }

        memorySavePhrases.firstOrNull { lower.startsWith(it) }?.let { prefix ->
            val content = text.substring(prefix.length).trim().removeSuffix(".")
            return handleSaveMemory(content, language)
        }

        if (memoryReadPhrases.any { lower.contains(it) }) {
            return handleReadMemory(language)
        }

        if (memoryClearPhrases.any { lower.contains(it) }) {
            memoryRepository.clearAll()
            return CommandOutcome.Handled(
                if (language == AppLanguage.HUNGARIAN) "Töröltem mindent, amit eddig megjegyeztem rólad."
                else "I've cleared everything I remembered about you."
            )
        }

        searchPhrases.firstOrNull { lower.contains(it) }?.let { phrase ->
            val query = text.substringAfter(phrase, "").trim().ifBlank { text }
            return handleWebSearch(query, language)
        }

        return CommandOutcome.NotACommand
    }

    private fun settingsSystemMatch(lower: String): SystemSettingsTarget? {
        if (!(lower.contains("beállítás") || lower.contains("settings"))) return null
        return SystemSettingsTarget.matchByAlias(lower)
    }

    private fun handleOpenSettings(context: Context, target: SystemSettingsTarget, language: AppLanguage): CommandOutcome {
        val opened = SystemSettingsLauncher.open(context, target)
        val reply = if (opened) {
            if (language == AppLanguage.HUNGARIAN) "Megnyitottam a beállításokat." else "I've opened the settings."
        } else {
            if (language == AppLanguage.HUNGARIAN) "Nem sikerült megnyitni ezt a beállítást." else "I couldn't open that settings screen."
        }
        return CommandOutcome.Handled(reply)
    }

    private suspend fun handleOpenApp(query: String, language: AppLanguage): CommandOutcome {
        if (query.isBlank()) return CommandOutcome.NotACommand
        val apps = appDiscoveryRepository.getLaunchableApps()
        val match = AppMatcher.findBestMatch(query, apps)

        if (match == null) {
            return CommandOutcome.Handled(
                if (language == AppLanguage.HUNGARIAN) "Nem találom ezt az alkalmazást a telefonon: $query"
                else "I can't find this app on the phone: $query"
            )
        }

        val launched = appDiscoveryRepository.launchApp(match)
        val reply = if (launched) {
            if (language == AppLanguage.HUNGARIAN) "Rendben, megnyitom: ${match.label}." else "Sure, opening ${match.label}."
        } else {
            if (language == AppLanguage.HUNGARIAN) "Megtaláltam a(z) ${match.label} alkalmazást, de nem sikerült elindítani."
            else "I found ${match.label}, but I couldn't launch it."
        }
        return CommandOutcome.Handled(reply)
    }

    private suspend fun handleSaveMemory(content: String, language: AppLanguage): CommandOutcome {
        if (content.isBlank()) return CommandOutcome.NotACommand
        memoryRepository.addMemory(content, MemoryCategory.OTHER)
        return CommandOutcome.Handled(
            if (language == AppLanguage.HUNGARIAN) "Megjegyeztem: $content" else "Got it, I'll remember: $content"
        )
    }

    private suspend fun handleReadMemory(language: AppLanguage): CommandOutcome {
        val memories = memoryRepository.getAllOnce()
        if (memories.isEmpty()) {
            return CommandOutcome.Handled(
                if (language == AppLanguage.HUNGARIAN) "Még nem jegyeztem meg semmit rólad." else "I haven't remembered anything about you yet."
            )
        }
        val bulletList = memories.joinToString("\n") { "• ${it.content}" }
        return CommandOutcome.Handled(
            if (language == AppLanguage.HUNGARIAN) "Ezeket jegyeztem meg rólad:\n$bulletList" else "Here's what I remember about you:\n$bulletList"
        )
    }

    private suspend fun handleWebSearch(query: String, language: AppLanguage): CommandOutcome {
        if (query.isBlank()) return CommandOutcome.NotACommand
        return when (val outcome = webSearchRepository.search(query)) {
            is WebSearchOutcome.Success -> {
                if (outcome.results.isEmpty()) {
                    CommandOutcome.Handled(
                        if (language == AppLanguage.HUNGARIAN) "Rákerestem, de nem találtam releváns eredményt." else "I searched, but found no relevant results."
                    )
                } else {
                    val top = outcome.results.take(3).joinToString("\n") { "• ${it.title} - ${it.url}" }
                    CommandOutcome.Handled(
                        if (language == AppLanguage.HUNGARIAN) "Rákerestem, ezeket találtam:\n$top" else "I searched the web, here's what I found:\n$top"
                    )
                }
            }
            WebSearchOutcome.NotConfigured -> CommandOutcome.Handled(
                if (language == AppLanguage.HUNGARIAN) "Nincs beállítva keresési szolgáltató, ezért nem tudok rákeresni. Add meg a kulcsot a Beállításokban."
                else "No search provider is configured, so I can't search. Add a key in Settings."
            )
            WebSearchOutcome.NoInternet -> CommandOutcome.Handled(
                if (language == AppLanguage.HUNGARIAN) "Nincs internetkapcsolat, ezért nem tudok rákeresni erre."
                else "There's no internet connection, so I can't search for that."
            )
            is WebSearchOutcome.Error -> CommandOutcome.Handled(
                if (language == AppLanguage.HUNGARIAN) "A keresés közben hiba történt." else "Something went wrong while searching."
            )
        }
    }
}
