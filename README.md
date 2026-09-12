# NOVA Mobile

NOVA is a personal AI assistant for Android — text and voice conversation, local
memory, app launching, Android settings shortcuts, and web search — built with Kotlin
and Jetpack Compose. Hungarian is the default language; English is fully supported.

This is a real, buildable Android Studio project: Gradle files, manifest, Room
database, and Compose UI are all present in this repository, not just snippets.

## Features

- **Conversation** — text chat and voice conversation, in Hungarian or English, with
  persisted history (Room) and NOVA's four visible states: idle, listening, thinking,
  speaking.
- **AI abstraction** — NOVA is never locked to one AI backend. `AIProvider` is an
  interface; `LocalAIProvider` (fully offline, rule-based) and `RemoteAIProvider`
  (calls a user-configured HTTP endpoint with a user-supplied API key) both implement
  it, selected at runtime in Settings.
- **Memory** — "Jegyezd meg, hogy szeretem az autókat" saves a local memory. Memories
  are stored in Room, shown in a dedicated screen (list, category, date, edit, delete,
  clear all), and injected as context for the AI provider.
- **App launching** — discovers installed apps dynamically via `PackageManager`
  (never a hard-coded app list) and matches spoken/typed names against labels plus a
  curated Hungarian/English alias table, with fuzzy (Levenshtein) matching for minor
  mis-transcriptions. If an app can't be found, NOVA says so — it never claims to have
  opened something it didn't.
- **Android settings shortcuts** — Wi-Fi, Bluetooth, display, sound, notifications,
  battery, apps, accessibility, date/time, addressable in Hungarian or English.
- **Web search** — a separate layer (`WebSearchRepository`) from AI reasoning, so NOVA
  can truthfully say whether it actually searched the web. Uses a user-supplied search
  API key; without one it says so rather than fabricating results.
- **Offline mode** — app launching, Android settings, memory, and conversation history
  all work with no internet. `LocalAIProvider` gives real (if simple) offline answers
  and is honest about what it can't do offline.
- **Voice** — Android's built-in `SpeechRecognizer` (Hungarian `hu-HU` / English
  `en-US`) and `TextToSpeech`, wrapped in `SpeechRecognitionManager` /
  `TextToSpeechManager`, plus a documented wake-word architecture — see
  [Known Android limitations](#known-android-limitations).
- **Settings** — AI provider, language, theme (light/dark/system), wake word,
  continuous conversation, push-to-talk, remote API key, search API key, privacy.
- **Privacy** — conversation history and memory are local-first (Room, excluded from
  Android auto-backup). No API key is ever hard-coded or bundled with the app.

## Architecture

MVVM + repository pattern, Jetpack Compose, Room, Coroutines/StateFlow, Navigation
Compose. No DI framework — a single `NovaViewModelFactory` wires everything from a
plain dependency graph in `NovaApplication`, which keeps the whole graph readable in
one file for a project this size.

```
app/src/main/java/hu/nova/mobile/
├── ai/            AIProvider interface, LocalAIProvider, RemoteAIProvider, factory
├── apps/          App discovery + fuzzy matching, Android settings shortcuts
├── commands/      CommandRouter — routes text to apps/settings/memory/search vs AI
├── data/          DataStore-backed preferences, repositories (chat, memory, settings)
├── database/      Room entities, DAOs, database, type converters
├── domain/model/  Plain Kotlin domain models used by UI/ViewModels
├── navigation/    Navigation Compose graph + destinations
├── service/       NovaVoiceService (foreground service for active voice sessions)
├── ui/            ConversationEngine (shared chat/voice logic) + per-screen
│                  Composables/ViewModels: home, chat, memory, settings, theme
├── utils/         Permission + misc utilities
├── voice/         SpeechRecognitionManager, TextToSpeechManager, WakeWordManager
└── web/           WebSearchProvider interface + repository
```

## Requirements

- Android Studio Koala (2024.1.1) or newer
- JDK 17
- Android SDK 34 (compileSdk/targetSdk), minSdk 26
- A physical device or emulator with Google Play Services if you want Google's TTS
  engine and best-quality Hungarian speech recognition

## How to build

```bash
git clone <your-repo-url>
cd nova-mobile

# This repo does not ship the gradle-wrapper.jar binary. Generate it once
# (requires a local Gradle install, or run this from Android Studio's
# "Sync Project with Gradle Files" instead, which does the same thing):
gradle wrapper --gradle-version 8.7

./gradlew assembleDebug
```

Or simply open the project root in Android Studio and let it sync — Android Studio
will generate the wrapper jar and download the SDK components automatically.

## How to run

- From Android Studio: select a device/emulator and click Run.
- From the command line: `./gradlew installDebug` after connecting a device with USB
  debugging enabled.

## How to configure an AI provider

NOVA never ships with an API key baked in.

1. Open **Settings → AI provider**.
2. Leave it on **Local** for a fully offline, rule-based assistant, or switch to
   **Remote**.
3. If you choose Remote, paste your own API key into the field that appears. The
   default endpoint (`https://api.anthropic.com/v1/messages`) and request shape match
   the Anthropic Messages API, but the base URL is editable, so any Messages-API-shaped
   endpoint works.
4. The key is stored only in local DataStore on your device (`PreferencesManager`) and
   is sent only in the `x-api-key` header of your own requests — it is never logged,
   never bundled with the app, and never leaves your device except in that request.

Web search works the same way: add a search-provider key (the code defaults to Brave
Search's API shape) under **Settings → Web search**. Without a key, NOVA tells you it
can't search rather than guessing.

## Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Speech recognition |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE` | Keep an active voice session alive with the screen off |
| `POST_NOTIFICATIONS` | Show the required ongoing notification during a voice session |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Remote AI provider and web search, both opt-in |
| `<queries>` block for `ACTION_MAIN`/`CATEGORY_LAUNCHER` | Required on Android 11+ to enumerate and launch other installed apps |

If microphone permission is denied, NOVA falls back gracefully: text chat, app
launching, settings shortcuts, and memory all keep working; only voice input is
unavailable, and the app says so instead of crashing.

## Offline mode

With no internet and `LocalAIProvider` selected, NOVA can still: launch apps, open
Android settings, save/read/clear memory, hold a full conversation history, and give
simple built-in answers. `RemoteAIProvider` and web search fail fast with a clear
Hungarian/English message (`error_no_internet` / `WebSearchOutcome.NoInternet`) rather
than pretending to have reached the internet.

## GitHub Actions

`.github/workflows/android.yml` runs on every push/PR to `main`: checks out the repo,
sets up JDK 17 and the Android SDK, restores the Gradle cache, runs
`./gradlew testDebugUnitTest`, then `./gradlew assembleDebug`, and uploads the debug
APK as a workflow artifact. Since this repo doesn't ship a `gradle-wrapper.jar`
binary, add one (`gradle wrapper --gradle-version 8.7`, then commit the generated
`gradle/wrapper/gradle-wrapper.jar` and `gradlew`/`gradlew.bat` scripts) before the
workflow will succeed — see [How to build](#how-to-build).

## Known Android limitations

**True always-on wake word ("Nova" with the screen off / app killed) is not
implemented, and this is documented rather than faked.** Android's public
`SpeechRecognizer` API cannot listen continuously in the background: it does single
utterance sessions, background microphone access is restricted on Android 10+ without
a foreground service, and real hotword detection (like "Hey Google") requires either a
licensed low-power DSP hotword SDK (e.g. Picovoice Porcupine) or OEM-level privileges
that ordinary apps don't have. `voice/WakeWordManager.kt` documents this in full and
implements the best legitimate alternative available on stock Android: while a voice
session is active (started by tapping the mic, or via "Continuous conversation" in
Settings) NOVA keeps a foreground service + visible notification running and re-listens
for "Nova" between turns, with a session timeout, so a conversation can flow ("Nova" →
"Igen?" → command → reply → next command) without re-tapping the mic each time. Manual
activation (tapping the mic) always works regardless. Swapping in a dedicated hotword
SDK later would only mean changing `WakeWordManager` — no other layer depends on how
wake-word detection is implemented.

Other notes:
- Hungarian speech recognition and TTS quality depend on the language packs installed
  on the device (Google's engine supports `hu-HU`, but availability varies by device).
- `LocalAIProvider` is an honest rule-based offline responder, not a local LLM —
  shipping a multi-gigabyte model by default isn't practical for most phones. The
  `AIProvider` interface is designed so a real on-device model (e.g. via MediaPipe LLM
  Inference or llama.cpp bindings) can be added later without touching any other layer.
- NOVA cannot toggle system state like Wi-Fi directly on modern Android (that requires
  platform-signature permissions); it opens the relevant system settings screen
  instead, which is the correct, legitimate behavior for a normal app.

## Troubleshooting

- **"Nem találom ezt az alkalmazást" / "I can't find this app"** — the app name/alias
  didn't match closely enough to any installed launchable app. Try the exact label
  shown in the launcher, or add an alias to `AppDiscoveryRepository.KNOWN_ALIASES`.
- **Speech recognition returns nothing** — check `RECORD_AUDIO` permission is granted,
  and that a speech-recognition service (Google app) is installed/enabled on the
  device; `SpeechRecognizer.isRecognitionAvailable()` returning `false` means the OS has
  no recognizer at all.
- **TTS speaks in the wrong language or not at all** — the selected locale's voice
  data may not be installed; `TextToSpeechManager` reports this as `TtsEvent.Error`
  instead of silently mis-speaking.
- **Remote AI provider always fails** — check the API key and base URL in Settings;
  `RemoteAIProvider` distinguishes an invalid key (`401`/`403`) from rate limiting
  (`429`) from a generic provider outage in its error messages.
- **Gradle sync fails on `gradlew: not found` / missing wrapper jar** — see
  [How to build](#how-to-build); this repo intentionally doesn't commit the wrapper
  binary jar, generate it once locally.

## Screenshots

_Add screenshots here after your first build — e.g. `docs/screenshots/home.png`,
`docs/screenshots/chat.png`, `docs/screenshots/memory.png`,
`docs/screenshots/settings.png`._

## License

MIT — see [LICENSE](LICENSE).
