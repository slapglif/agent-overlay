# Changelog

## v0.1.6 — 2026-06-11

### Added
- LAN gateway auto-detection: the app probes :8642 across the local /24 for running Hermes gateways, auto-fills the URL on first launch, and Settings gains a "Scan LAN" card listing discovered gateways with auth/latency info.
- Real agentic tool loop: phone tool calls are executed on-device and their results are sent back to the model as `tool` role messages (up to 4 rounds), so multi-step phone automation can react to outcomes.
- Conversation history (last 24 messages) is now replayed to `/v1/chat/completions` instead of sending each message context-free.
- Chat settings (model, reasoning, tools, passthrough) persist via DataStore and are honored by the floating overlay instead of hardcoded defaults.

### Changed
- One shared repository/HTTP/preferences graph (`AppGraph`) across the activity, view model, and overlay service.
- The overlay agent list now shows real gateway sessions from `/api/jobs` instead of three hardcoded placeholder agents, and its palette matches the design tokens.
- Gateway capabilities are probed (`/api/jobs`) instead of hardcoded after any 200.
- Navigation section and chat draft survive rotation (`rememberSaveable`); system Back returns to Chat before exiting.
- Accessibility pass: content descriptions and button roles on glyph/text controls, ≥44–48dp touch targets, low-contrast small text upgraded from Subtle to Muted.
- Cleartext HTTP is now permitted via the network security config base policy (Hermes gateways are plain-HTTP LAN services); previously LAN IPs were silently blocked.

### Fixed
- Burrow registry WebSocket is always closed on timeout/failure.
- Overlay chat history is bounded (80 messages) and the gateway URL field flags malformed values.

### Validation
- `ANDROID_HOME=/opt/android-sdk ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug` plus Android CI.

## v0.1.5 — 2026-06-10

### Changed
- Polished the chat surface: circular ↑ send affordance, per-message HH:mm timestamps, and tappable starter suggestion chips on the empty transcript.
- Moved the progress pill to the bottom of the screen so it no longer covers the top bar, and renamed it "Working…" to match the activity language.
- Made the global error strip dismissible with an accessible ✕ control.
- Removed the non-functional ⋯/↗ glyphs from the chat header; full-screen chat no longer advertises affordances it does not have.

### Fixed
- Phone-screen context is now attached to outgoing prompts only when tool calls are enabled, instead of leaking screen contents into every message.

### Removed
- ~190 lines of dead dashboard composables (HeroHeader, BubbleClusterMark, FlowHintStrip, ThreadList, StatPill) and unused imports.

### Validation
- `ANDROID_HOME=/opt/android-sdk ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`
- First green Android CI run for the repository after the Actions runner fix.

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
