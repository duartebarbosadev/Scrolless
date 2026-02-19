# Repository Guidelines

## Project Structure & Module Organization
- `mobile/` hosts the Jetpack Compose app, Gradle scripts, and launcher assets; start UI work in `mobile/src/main/kotlin`.
- `core/data`, `core/domain`, `core/designsystem`, and `core/logging` expose reusable layers; keep platform-agnostic logic here.
- Shared tests currently live in `core/domain/src/test/kotlin`; add module-specific tests under the matching `src/test` tree.
- Marketing and design references sit in `art/`, while Gradle build logic and Spotless configs live in `buildscripts/` and `spotless/`.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` – builds the debug APK with Compose metrics enabled.
- `./gradlew :core:domain:test` – runs JVM unit tests; substitute the module path when adding more test suites.
- `./gradlew connectedDebugAndroidTest` – executes instrumentation tests on a USB/emulator device; ensure `adb devices` returns exactly one target.
- `./gradlew spotlessCheck` / `spotlessApply` – verify or auto-format Kotlin and XML sources before pushing.

## Coding Style & Naming Conventions
- Kotlin is formatted via Spotless + ktlint (Kotlin 2.2, JVM target 17); use 4-space indentation and trailing commas for multiline collections.
- Prefer `camelCase` for methods/values, `PascalCase` for composables and classes, and name files after their primary type (e.g., `BlockerService.kt`).
- Keep Compose UI state hoisted; place theme tokens in `core/designsystem`, DI modules under `core/data` or `core/domain`, and avoid Android dependencies in domain layer.
- Preserve license headers; copy from existing files when creating new sources or resources.

## Testing Guidelines
- Default to JUnit4 with `kotlinx-coroutines-test` for coroutine logic; mock time via `StandardTestDispatcher`.
- Name unit tests with the pattern `FunctionUnderTest_State_ExpectedResult` and instrumentation tests with the suffix `AndroidTest`.
- Capture coverage for new features with at least one JVM test per branch decision; hook them into the relevant module’s `src/test` or `src/androidTest`.
- Run `./gradlew testDebugUnitTest connectedDebugAndroidTest` before opening a PR touching business logic or UI flows.

## Commit & Pull Request Guidelines
- Follow the existing Conventional Commit style (`feat:`, `fix:`, `chore(ci):`, etc.) with optional scopes (`module`, `ci`, `ui`).
- Reference GitHub issues in the PR body and request review from at least one module owner; include screenshots or screen recordings when changing UI.
- Note release-impacting changes (signing, permissions, new services) explicitly, and ensure CI artifacts such as translation reports are generated.

## Security & Configuration Tips
- The release signing config falls back to `~/.android/debug.keystore`; set `compose_store_password`, `compose_key_alias`, and `compose_key_password` in your environment for production builds.
- Keep sensitive values out of VCS; prefer `local.properties` or Gradle properties files ignored by Git when testing third-party keys.
