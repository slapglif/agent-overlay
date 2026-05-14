# Agent Overlay

Agent Overlay is a production-oriented Android command-and-control client for Hermes Agent gateways. It gives you a persistent floating ☤ icon, a compact C&C panel, and a Discord-with-threads-style chat dashboard for managing Hermes gateway sessions, scheduled jobs, and long-running agents from your phone.

## Hermes gateway contract

This project is grounded against the official Hermes Agent docs:

- API Server docs: https://hermes-agent.nousresearch.com/docs/user-guide/features/api-server
- Messaging Gateway docs: https://hermes-agent.nousresearch.com/docs/user-guide/messaging/

Relevant gateway facts from the docs:

- Enable the API server with `API_SERVER_ENABLED=true` and `API_SERVER_KEY=...` in `~/.hermes/.env`.
- Start Hermes with `hermes gateway`; the API server listens on `http://127.0.0.1:8642` by default.
- OpenAI-compatible endpoints are exposed under `/v1`, including `/v1/chat/completions` and `/v1/responses`.
- The API server also exposes a runs API: `POST /v1/runs`, `GET /v1/runs/{run_id}`, `GET /v1/runs/{run_id}/events`, and `POST /v1/runs/{run_id}/stop`.
- Scheduled/background work is exposed through `/api/jobs` CRUD and job control endpoints.
- All API server endpoints use bearer auth via the configured `API_SERVER_KEY`.

## MVP implemented

- Android/Kotlin/Compose application scaffold.
- Secure-ish local settings via Jetpack DataStore for gateway URL and API key.
- Hermes API client for:
  - `/v1/models` capability smoke check.
  - `/api/jobs` scheduled/background-agent list.
  - `/v1/chat/completions` message sending.
- Floating overlay service with draggable ☤ icon and compact C&C menu.
- Main dashboard with:
  - gateway configuration card,
  - agents/threads list,
  - Discord-style chat panel.
- Release hardening basics: minification, shrink resources, ProGuard config, CI.

## Gateway setup

On the machine running Hermes:

```bash
hermes config set API_SERVER_ENABLED true
hermes config set API_SERVER_KEY "replace-with-a-long-random-token"
hermes gateway
```

For an Android emulator, use `http://10.0.2.2:8642` as the gateway URL. For a physical device, expose the gateway over your LAN or a trusted tunnel and use `http://<host-ip>:8642` or HTTPS via a reverse proxy.

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Visual validation

The repo includes Maestro flows and a local mock Hermes gateway for repeatable emulator validation:

```bash
python3 scripts/mock_hermes_gateway.py
adb install -r app/build/outputs/apk/debug/app-debug.apk
maestro test .maestro/smoke.yaml
adb shell appops set com.slapglif.agentoverlay.debug SYSTEM_ALERT_WINDOW allow
maestro test .maestro/overlay.yaml
```

Validated flows:

- `.maestro/smoke.yaml` — launch, connect to mock Hermes, list jobs as threads, send chat, receive assistant reply.
- `.maestro/overlay.yaml` — grant overlay app-op, start floating icon, expand C&C menu, verify Open dashboard affordance.

CI runs the same build and JVM unit tests on every push/PR.

## Production roadmap

1. Replace best-effort `/api/jobs` mapping with a first-class Hermes gateway agent/session inventory endpoint once upstream exposes active gateway sessions.
2. Add SSE support for `/v1/runs/{run_id}/events` using OkHttp SSE so tool calls stream into the thread UI live.
3. Add encrypted API-key storage through Android Keystore.
4. Add notification actions for stop/pause/resume.
5. Add tablet/two-pane polish and accessibility labels.
6. Add instrumented Compose UI tests and a release signing workflow.

## Security notes

Agent Overlay controls agents that can use terminal, file, browser, and other high-impact tools. Do not expose a Hermes gateway publicly without TLS, a strong bearer token, and network allowlisting.
