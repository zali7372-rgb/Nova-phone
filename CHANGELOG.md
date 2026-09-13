# Changelog

## Unreleased

### Fixed
- **Settings API key field appeared to "lose" the saved key.** The Remote AI and Web
  search API key fields on the Settings screen always initialized to an empty string
  and never read back what was actually saved, so navigating away and back made it
  look like the key had disappeared. The key itself was always saved correctly to
  local `DataStore` and used normally by `RemoteAIProvider`/`WebSearchRepository` -
  only the on-screen display was wrong. `SettingsUiState` now carries the saved
  `remoteApiKey`, `remoteApiBaseUrl`, and `searchApiKey`, and the Settings screen
  seeds its text fields from that state.

### Added
- **Background wake-word listening.** Turning on "Wake word" in Settings now actually
  starts `NovaVoiceService` as a real Android foreground service. While it's running:
  - NOVA keeps listening for "Nova" even after you leave the app (Home button,
    switching to another app, screen off) - this works because a foreground service
    with a visible, ongoing notification is explicitly allowed to keep using the
    microphone in the background on Android; it is not a workaround or a hack.
  - A persistent notification is always shown while this is active, with a "Stop"
    action, so you always know when NOVA is listening and can turn it off with one
    tap from the notification shade, not just from inside the app.
  - Turning the Settings switch back off stops the service immediately.
  - The new `BackgroundListeningEngine` (`voice/BackgroundListeningEngine.kt`) drives
    the actual loop: listen for "Nova" -> say "Igen?"/"Yes?" -> listen for the
    command -> route it through the same `CommandRouter`/`AIProvider` path as the
    in-app chat -> speak the reply -> go back to listening for the wake word.
  - Requests `RECORD_AUDIO` and, on Android 13+, `POST_NOTIFICATIONS` runtime
    permissions before starting, and turns the setting back off with an explanatory
    message if either is denied, rather than silently failing.

  **What this update does _not_ do, on purpose:** it does not listen after you
  force-stop the app from Android's App Info screen, after certain OEM launchers'
  "clear all recents" kills background services outright, or after the OS kills the
  process under memory pressure faster than `START_STICKY` can restart it. True
  "app fully closed, still always listening" wake word isn't achievable by a normal
  app on stock Android without a licensed hotword SDK (e.g. Picovoice Porcupine) or
  OEM-level privileges - this is documented in full in `voice/WakeWordManager.kt` and
  in the README's "Known Android limitations" section, and the in-app copy on the
  Settings screen states this plainly rather than implying unlimited background
  listening.

### Changed
- `NovaVoiceService` no longer just shows an empty notification - it owns the
  background listening loop's lifecycle (`onCreate`/`onStartCommand`/`onDestroy`) and
  updates its notification text to reflect whether it's idly listening for the wake
  word or actively handling a command.
