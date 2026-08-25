# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The **Firebase provider adapter** for UserKit for Compose — the provider-neutral core is the separate repository [`rozdonmobile/user-kit`](https://github.com/rozdonmobile/user-kit) (package `io.github.rozd.userkit`), consumed as `com.github.rozdonmobile:user-kit:<userKit>` from JitPack (version in `gradle/libs.versions.toml`). This build implements the core's six interfaces over `firebase-auth` and FirebaseUI-Android 10 (Compose). The dependency only ever points adapter → core; never put Firebase code in the core.

The Swift original is `~/dev/rozd/user-kit-firebase`; this is its Kotlin sibling and follows the same shape and the same deliberate no-ops.

## Build & test

- Needs `local.properties` with `sdk.dir=…` (gitignored) or `ANDROID_HOME`. The core is a published artifact from JitPack (repository declared in `settings.gradle.kts`); to compile against an uncommitted `../user-kit`, run with `--include-build ../user-kit` or uncomment the `includeBuild` line in `settings.gradle.kts` — never commit it uncommented.
- The adapter **is the root project** (sources in `src/`); `:sample` is the only subproject. That is what gives the short JitPack coordinates `com.github.rozdonmobile:user-kit-firebase:<tag>`.
- Build: `./gradlew assembleRelease`
- Test: `./gradlew testDebugUnitTest` — JVM JUnit 4. Only `FirebaseUserServiceConfiguration` is unit-tested; anything holding a `FirebaseUser` needs a device.
- Sample: `./gradlew :sample:installDebug` — initialises Firebase by hand (`demo-userkit`) against the Auth emulator at `10.0.2.2:9099`; no `google-services.json`.
- Release: bump `VERSION_NAME` in `gradle.properties` (and `userKit` in the version catalog if the core moved), commit, tag with exactly that string, push tags; JitPack builds on first request (`jitpack.yml`).
- First cold build resolves the whole Firebase + FirebaseUI graph and is slow.

Toolchain: AGP 9.3 with built-in Kotlin (no `kotlin-android` plugin), Gradle 9.5, Compose BOM 2026.08, Firebase BOM 34.17 — **the BOM FirebaseUI 10.0.0-beta04 declares**; pinning lower is pointless because Gradle lifts it. When bumping FirebaseUI, re-check its POM and move the BOM with it.

## FirebaseUI 10 (beta) — the API this is written against

`com.firebaseui:firebase-ui-auth:10.0.0-beta04` is the Compose rewrite; the 9.x line is Activity/XML and is not what this adapter uses. The surface used here, verified from the AAR:

- `authUIConfiguration { context; providers { provider(AuthProvider.Email(...)); provider(AuthProvider.Google(serverClientId = …)) }; tosUrl; privacyPolicyUrl; isMfaEnabled; isAnonymousUpgradeEnabled; passwordResetActionCodeSettings }`
- `FirebaseAuthScreen(configuration, onSignInSuccess, onSignInFailure, onSignInCancelled, modifier, authUI, …)`
- `FirebaseAuthUI.getInstance()`, `.signOut(context)` (suspend), `.authStateFlow()`

It is a beta: expect renames. If a bump breaks `FirebaseAuthHost.kt` or `FirebaseUserService.buildAuthConfiguration()`, those two are the only places that touch it.

## Design (do not break)

- **Presentation is a state, not a call.** `FirebaseUserService.isPresented` is snapshot state; `presentSignIn()` raises it, `finishSignIn(authenticated)` lowers it *and* settles `withAuthentication` through a `MutableSharedFlow<Boolean>`. Any host — `FirebaseAuthHost` or the app's own — must end the flow through `finishSignIn`, or a pending `withAuthentication` hangs.
- `withAuthentication` subscribes to the outcome stream **before** presenting (`onSubscription { presentSignIn() }`) so a fast result cannot be missed. Keep that order.
- `FirebaseUserSynchronizer` uses an **`IdTokenListener`**, not an `AuthStateListener`: custom claims (`role`) reach the client on token refresh. Claims are decoded in `map`, after `callbackFlow`, so emissions stay ordered.
- `FirebaseUserInfo.equals` is over `(uid, displayName, email, isEmailVerified, claims)` — `User.infos` is a `StateFlow` and every token refresh re-emits; equality is what stops no-op refreshes from recomposing the app.
- `FirebaseUserStorage.store`/`clear` are **intentional no-ops**; `fetch()` forces a token refresh so seeded claims are current. `FirebaseUserSynchronizer.dispose()` is a no-op because `awaitClose` removes the listener.
- `FirebaseUserSession.refreshToken` is `null`: the Android SDK does not expose it. `isAuthenticated` is `true` whenever a `FirebaseUser` exists.
- `googleServerClientId == null` leaves the Google provider out rather than registering a button that fails — the same gate the iOS app applies with `isGoogleSignInAvailable`.
- Role *policy* (`== "admin"`) lives in the core; this adapter only surfaces the raw claim string.
