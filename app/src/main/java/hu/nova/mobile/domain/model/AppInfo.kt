package hu.nova.mobile.domain.model

data class AppInfo(
    val packageName: String,
    val label: String,
    /** Extra Hungarian/English aliases this app can be referred to by, in addition to its label. */
    val aliases: List<String> = emptyList()
)
