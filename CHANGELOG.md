# Changelog

## v0.1.4 — 2026-05-15

### Added
- Burrow registry host discovery from `wss://reg.ai-smith.net` using an in-app WebSocket registry client.
- Dedicated Hosts screen for scanning live peers and inspecting advertised models, tools, skills, tags, and status.
- Capability constellation on the Hosts screen showing reasoning, tool-call, and phone-control readiness.

### Changed
- Reworked screen navigation to include Chat, Agents, Phone, Hosts, and Settings as first-class destinations.
- Strengthened the app's agentic-phone-control UX around refs, tool calls, slash commands, and host routing.

### Validation
- `ANDROID_HOME=/home/mikeb/android-sdk ./gradlew --no-daemon --rerun-tasks :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`

## v0.1.3 — 2026-05-15

### Changed
- Replaced the full-screen dashboard/sidebar with separate mobile-native Chat, Agents, Phone, and Settings screens.
- Promoted bottom navigation as the primary full-screen structure while keeping Chat as the first-class landing surface.
- Moved gateway setup into Settings, session selection into Agents, and phone inspection/actions into Phone.
- Updated smoke validation for the new screen split and bottom navigation labels.

### Validation
- `ANDROID_HOME=/home/mikeb/android-sdk ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug`

## v0.1.2 — 2026-05-15

### Changed
- Incremented Android version to `0.1.2` / `versionCode 3` for the latest distributable APK.
- Refreshed release packaging for GitHub Releases and the Cloudflare Pages direct-download mirror.

### Includes
- Quiet agentic chat UX with model switching, contextual `/commands`, and activity disclosure.
- Hermes phone automation tool-call contract for snapshot, accessibility tree, OCR/vision snapshot, tap, type, swipe, back, home, and recents.
- On-device slash-command execution path for `/phone`, `/tap`, `/type`, `/swipe`, `/back`, `/home`, and `/recents`.

### Validation
- `ANDROID_HOME=/home/mikeb/android-sdk ./gradlew --no-daemon --rerun-tasks :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`

## v0.1.1 — 2026-05-15

### Added
- Phone automation tool-call contract for Hermes chat completions, including snapshot, accessibility tree, OCR/vision snapshot, tap, type, swipe, back, home, and recents tools.
- On-device slash-command execution path for `/phone`, `/tap`, `/type`, `/swipe`, `/back`, `/home`, and `/recents`.
- Quiet agentic chat UX with model switching, contextual `/commands`, activity disclosure, and overlay status affordances.
- Mock Hermes gateway coverage for OpenAI-style `tool_calls` responses.

### Changed
- Incremented Android version to `0.1.1` / `versionCode 2`.
- Updated Maestro smoke and overlay flows for the latest chat and phone-control surfaces.

### Validation
- `ANDROID_HOME=/home/mikeb/android-sdk ./gradlew --no-daemon --rerun-tasks :app:testDebugUnitTest :app:assembleDebug`
