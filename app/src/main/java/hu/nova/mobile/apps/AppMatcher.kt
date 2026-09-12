package hu.nova.mobile.apps

import hu.nova.mobile.domain.model.AppInfo

/**
 * Fuzzy-matches a spoken/typed app name against the installed app list: exact match on
 * label or alias first, then substring containment, then a small Levenshtein-distance
 * tolerance so minor mis-transcriptions ("krom" -> "chrome") still resolve.
 */
object AppMatcher {

    fun findBestMatch(query: String, apps: List<AppInfo>): AppInfo? {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return null

        apps.firstOrNull { app ->
            normalize(app.label) == normalizedQuery || app.aliases.any { normalize(it) == normalizedQuery }
        }?.let { return it }

        apps.firstOrNull { app ->
            normalize(app.label).contains(normalizedQuery) ||
                app.aliases.any { normalize(it).contains(normalizedQuery) } ||
                normalizedQuery.contains(normalize(app.label))
        }?.let { return it }

        return apps
            .map { app -> app to bestDistance(normalizedQuery, app) }
            .filter { (_, distance) -> distance <= FUZZY_THRESHOLD }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    private fun bestDistance(query: String, app: AppInfo): Int {
        val candidates = listOf(app.label) + app.aliases
        return candidates.minOf { levenshtein(query, normalize(it)) }
    }

    private fun normalize(text: String): String = text.trim().lowercase()
        .replace("ö", "o").replace("ő", "o")
        .replace("ü", "u").replace("ű", "u")
        .replace("ó", "o").replace("é", "e").replace("á", "a").replace("í", "i").replace("ú", "u")

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }

    private const val FUZZY_THRESHOLD = 2
}
