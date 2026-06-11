# Full audit — visual · logical · structural (2026-06-11)

Three-axis audit of Agent Overlay v0.1.5, with resolution status as of v0.1.6.
Severity reflects user impact; ✅ = fixed in v0.1.6, ▢ = open.

## Systemic

| # | Finding | Sev | Status |
|---|---------|-----|--------|
| S1 | No conversation history sent — every `/v1/chat/completions` call carried only `[system, user]`; UI showed continuity the model never saw | High | ✅ last 24 thread messages replayed |
| S2 | Tool results never returned to the model — phone tool calls executed locally with no `tool` role feedback, making multi-step automation impossible | High | ✅ tool round-trip loop (≤4 rounds) in `HermesGatewayClient` |
| S3 | System prompt rebuilt per message from mutable toggles | Med | ▢ partially inherent (Hermes options are per-request); documented in code |

## Logical

| # | Finding | Sev | Status |
|---|---------|-----|--------|
| L1 | `capabilities()` hardcoded an 8-capability set after any 200 | High | ✅ `/api/jobs` probed; jobs/runs/commands/skills only claimed when reachable |
| L2 | Overlay sent `ChatOptions()` defaults, ignoring user settings | High | ✅ options persisted in DataStore, honored by overlay |
| L3 | Overlay/app shared thread id but never synced state; overlay list unbounded | High | ✅ shared `AppGraph` repository; overlay history bounded to 80 |
| L4 | `phone.tap(ref)` resolves against current snapshot (staleness) and refs are traversal-ordered | Med | ▢ open — needs snapshot generation ids / stable refs |
| L5 | LAN gateways silently blocked: network security config overrode the manifest cleartext flag, whitelisting only emulator hosts | High | ✅ base-config cleartext (documented LAN tradeoff) + LAN auto-discovery added |
| L6 | Burrow WebSocket leaked on timeout | Low | ✅ try/finally close |
| L7 | Burrow hosts display-only (no routing) | Med | ▢ open — needs upstream session routing |
| L8 | API key plaintext in DataStore | Med | ▢ open — Keystore is roadmap #3 |
| L9 | `tools_enabled`/`session_id` are non-standard fields | Low | ✅ documented as Hermes extensions in code |

## Visual

| # | Finding | Sev | Status |
|---|---------|-----|--------|
| V1 | Overlay hardcoded a drifted palette (PANEL/INDIGO/MUTED/WARN off-token; "RAY_RED" actually red where token is orange) | High | ✅ token-aligned constants, red→`ACCENT` orange |
| V2 | Three fake hardcoded agents presented as real | High | ✅ overlay lists real `/api/jobs` sessions |
| V3 | ~12 glyph-only controls without contentDescription; clickable Texts without button roles | High | ✅ a11y pass in both UIs |
| V4 | Touch targets down to ~30dp vs DESIGN.md's 48dp rule | High | ✅ ≥44–48dp minimums |
| V5 | `Subtle` (#7B8290) small text ≈4.8:1 contrast (borderline AA) | Med | ✅ meaningful small text upgraded to Muted |
| V6 | Radius/padding drift off the 8/16/24/28 token scale | Low | ✅ light-touch normalization |
| V7 | No error styling on gateway URL field | Med | ✅ isError + guidance |

## Structural

| # | Finding | Sev | Status |
|---|---------|-----|--------|
| T1 | Navigation/draft in plain `remember`; lost on rotation; Back exits app from any tab | High | ✅ `rememberSaveable` + `BackHandler` |
| T2 | Three independent repository/OkHttp/DataStore stacks (ViewModel, OverlayService) | High | ✅ `AppGraph` singletons |
| T3 | `navigation-compose` and `okhttp-sse` dependencies declared but unused | Low | ▢ kept — SSE is roadmap #2; nav migration tracked |
| T4 | Monolithic `AgentOverlayUiState` + 18-callback prop drilling | Med | ▢ open — refactor candidate |
| T5 | Release APK unsigned; no signing pipeline | Med | ▢ open — needs keystore secret |
| T6 | Test coverage: gateway client only; Maestro not in CI | Med | ▢ partially — discovery + tool-loop tests added |
| T7 | ProGuard rules minimal (low practical risk: manual org.json parsing, no reflection) | Low | ▢ verify minified build on-device |

Corrected during verification: the accessibility service **does** declare
`BIND_ACCESSIBILITY_SERVICE` permission (auditor false positive), and the
`FOREGROUND_SERVICE_DATA_SYNC` permission is declared so there is no API-34
crash from the foreground service type.
