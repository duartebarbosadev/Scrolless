# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the app
./gradlew assembleDebug

# Run all tests
./gradlew test

# Run domain module tests only
./gradlew :core:domain:test

# Run a single test class
./gradlew :core:domain:test --tests "com.scrolless.app.core.blocking.handler.DayLimitBlockHandlerTest"

# Lint and format code (Spotless with ktlint)
./gradlew spotlessApply

# Check formatting without applying
./gradlew spotlessCheck

# Install debug APK
./gradlew installDebug
```

## Architecture Overview

Scrolless is an Android app that blocks "brain rot" content (Instagram Reels, TikTok, YouTube Shorts) using accessibility services. Architecture inspired by Google's Jetcaster sample.

### Module Structure

- **mobile**: Main Android app module with UI, accessibility service, and overlay
- **core/domain**: Blocking logic and business rules (testable without Android)
- **core/data**: Room database, repositories, and data persistence
- **core/designsystem**: Reusable Compose components and theming
- **core/logging**: Timber-based logging with debug/release trees

### Key Architectural Patterns

**Hilt DI**: App uses `@HiltAndroidApp`, `@AndroidEntryPoint`, and `@HiltViewModel`. DI modules in `core/domain/di/` and `core/data/di/`.

**Strategy Pattern for Blocking**: `BlockingManager` delegates to `BlockOptionHandler` implementations:
- `BlockAllBlockHandler`: Immediate blocking
- `DayLimitBlockHandler`: Daily usage limits
- `IntervalTimerBlockHandler`: Interval windows with allowance resets
- `NoBlockHandler`: No blocking

**Repository Pattern**: `UserSettingsStore` wraps Room DAO with StateFlow caches for reactive updates. `UsageTracker` abstracts usage tracking.

### Core Components

**Accessibility Service** (`mobile/.../accessibility/ScrollessBlockAccessibilityService.kt`):
- Monitors window state changes via AccessibilityEvent
- Detects blocked apps by inspecting accessibility node tree for specific view IDs
- Coordinates with BlockingManager for blocking decisions
- Manages timer overlay visibility

**Blocked Apps Detection** (`core/domain/.../model/BlockableApp.kt`):
```kotlin
enum class BlockableApp(val packageId: String, val viewId: String)
- REELS: "com.instagram.android:id/clips_viewer_view_pager"
- SHORTS: "com.google.android.youtube:id/reel_player_page_container"
- TIKTOK: "com.zhiliaoapp.musically:id/ulz"
```

**Timer Overlay** (`mobile/.../ui/overlay/TimerOverlayManager.kt`): View-based (not Compose) for performance. Drag-and-snap positioning with animations.

**HomeViewModel** (`mobile/.../ui/home/HomeViewModel.kt`): Main UI state management with reactive progress calculations via StateFlow.

### Data Flow

1. Accessibility service detects blocked app via node tree inspection
2. Calls `BlockingManager.onEnterBlockedContent()`
3. Handler checks usage via `UsageTracker`
4. Returns `BlockingResult` (BlockNow, Continue, CheckLater)
5. Service performs back navigation or starts periodic checks

### Testing

Tests in `core/domain/src/test/kotlin/` use JUnit4 + kotlinx-coroutines-test. Handler tests verify blocking logic with mock time via `StandardTestDispatcher`.

Test naming: `FunctionUnderTest_State_ExpectedResult`
