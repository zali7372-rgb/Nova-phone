package hu.nova.mobile.commands

sealed class CommandOutcome {
    data class Handled(val spokenReply: String) : CommandOutcome()
    /** Not a recognized command at all - caller should fall back to the AI provider. */
    object NotACommand : CommandOutcome()
}
