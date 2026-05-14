# Agent Overlay Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Build a production-quality Android app named Agent Overlay that connects to a Hermes Agent gateway, shows running/scheduled agents, and provides a floating C&C icon plus Discord-with-threads-style chat.

**Architecture:** Android Kotlin + Jetpack Compose app with a repository/client layer for Hermes API Server endpoints, a foreground overlay service for the floating icon, and a thread-oriented UI around Hermes sessions/jobs/runs. The current MVP uses documented `/v1/chat/completions`, `/v1/models`, and `/api/jobs`, with a roadmap to consume `/v1/runs/{run_id}/events` SSE.

**Tech Stack:** Kotlin, Android Gradle Plugin, Jetpack Compose Material3, DataStore Preferences, OkHttp, OkHttp SSE, coroutines, GitHub Actions.

---

## Task 1: Scaffold Android project

**Objective:** Create a buildable Android/Kotlin/Compose repository.

**Files:** `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `.github/workflows/android.yml`.

**Verification:** `./gradlew :app:assembleDebug` in CI.

## Task 2: Implement Hermes API client

**Objective:** Connect to official Hermes API Server endpoints.

**Files:** `app/src/main/java/com/slapglif/agentoverlay/hermes/HermesGatewayClient.kt`.

**Verification:** `./gradlew :app:testDebugUnitTest` with URL normalization unit tests.

## Task 3: Implement settings persistence

**Objective:** Store gateway URL and bearer key locally.

**Files:** `data/AppPreferences.kt`, `data/AgentOverlayRepository.kt`.

**Verification:** Manual app launch; values persist across activity recreation.

## Task 4: Implement dashboard UI

**Objective:** Provide configuration, thread list, and chat pane.

**Files:** `MainActivity.kt`, `MainViewModel.kt`, `ui/AgentOverlayAppUi.kt`, `ui/theme/Theme.kt`.

**Verification:** Compose preview/build plus manual emulator test.

## Task 5: Implement floating overlay

**Objective:** Provide draggable floating C&C affordance.

**Files:** `overlay/OverlayService.kt`, `AndroidManifest.xml`.

**Verification:** Grant overlay permission and start service; icon appears and expands.

## Task 6: Production hardening

**Objective:** Add CI, minification, docs, license, and safety notes.

**Files:** `.github/workflows/android.yml`, `README.md`, `LICENSE`, `proguard-rules.pro`.

**Verification:** GitHub Actions build succeeds.
