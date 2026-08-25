# UserKit Firebase adapter for Compose

A [Firebase Authentication](https://firebase.google.com/docs/auth) provider adapter for [UserKit for Compose](https://github.com/rozdonmobile/user-kit) — the provider-neutral "current authenticated user" layer for Jetpack Compose. The Kotlin sibling of [`rozd/user-kit-firebase`](https://github.com/rozd/user-kit-firebase).

The core ships only interfaces and a `User` orchestrator. This build fills the seam with Firebase: `FirebaseUserService` / `FirebaseUserStorage` / `FirebaseUserSynchronizer` and `FirebaseUserInfo` / `Session` / `Profile`, backed by `firebase-auth` and [FirebaseUI-Android 10](https://github.com/firebase/FirebaseUI-Android) — the Compose rewrite of FirebaseUI, the counterpart of `FirebaseAuthSwiftUI` on iOS.

## Requirements

- Kotlin 2.4, AGP 9.3, Compose BOM 2026.08, minSdk 26
- Firebase BOM 34.17 (what FirebaseUI 10 declares; a lower pin in the app is lifted to it)
- A configured Firebase project (`google-services.json`, the `google-services` plugin in the app)

## Installation

Served by [JitPack](https://jitpack.io/#rozdonmobile/user-kit-firebase) straight from this repository's tags; the core comes along as a transitive dependency from the same group.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") { content { includeGroup("com.github.rozdonmobile") } }
    }
}

// app/build.gradle.kts
implementation("com.github.rozdonmobile:user-kit-firebase:0.1.0")   // brings user-kit, firebase-auth, firebase-ui-auth
```

To develop against local checkouts instead, `includeBuild("../user-kit")` / `includeBuild("../user-kit-firebase")` in the app's `settings.gradle.kts` substitutes the working trees for those coordinates.

## Usage

Assemble the pieces and hand them to the core's `User`. The app owns the instance (one per process) and injects environment values through `FirebaseUserServiceConfiguration` — the library cannot read your `BuildConfig`.

```kotlin
import io.github.rozd.userkit.User
import io.github.rozd.userkit.firebase.*

val user = User(
    service = FirebaseUserService(
        context = applicationContext,
        configuration = FirebaseUserServiceConfiguration(
            authDomain = AppEnvironment.firebaseAuthDomain,        // "myapp.firebaseapp.com"
            packageName = BuildConfig.APPLICATION_ID,
            tosUrl = AppEnvironment.legalTermsOfServiceUrl,
            privacyPolicyUrl = AppEnvironment.legalPrivacyPolicyUrl,
            googleServerClientId = getString(R.string.default_web_client_id), // null → no Google button
        ),
    ),
    storage = FirebaseUserStorage(),
    synchronizer = FirebaseUserSynchronizer(),
)
```

Then provide it and host the sign-in UI once, at the root:

```kotlin
setContent {
    CompositionLocalProvider(LocalUser provides user) {
        AppRoot()
        FirebaseAuthHost()   // a modal bottom sheet that appears on user.authenticate()
    }
}
```

From there, drive auth through the neutral UserKit API — `user.authenticate()`, `user.withAuthentication { … }`, `user.isAuthenticated`, `user.isAdmin`. A successful sign-in closes the sheet and leaves the member where they were; a swipe-down cancels and fails any pending `withAuthentication` with `AuthenticationCancelledException`.

### Hosting the sign-in UI yourself

`FirebaseAuthHost` is a convenience. To present the flow in a container of your own — a full-screen dialog, a navigation destination — key it on the service's `isPresented` and drop in `FirebaseSignInScreen`:

```kotlin
val service = LocalUser.current.firebaseUserService
if (service.isPresented) {
    Dialog(onDismissRequest = { service.finishSignIn(authenticated = false) }) {
        FirebaseSignInScreen(service)
    }
}
```

Whatever the container, end the flow through `finishSignIn(authenticated)` — that is what releases a waiting `withAuthentication`.

### What the adapter surfaces

- **`FirebaseUserService`** — sign in / out (FirebaseUI's sign-out, which also clears Credential Manager state), email verification, and the presentation state.
- **`FirebaseUserStorage`** — seeds the restored user after a forced token refresh, so claims are current; `store`/`clear` are intentional no-ops because Firebase owns persistence.
- **`FirebaseUserSynchronizer`** — an `IdTokenListener` bridged into a `Flow<UserInfo?>`. ID token, not auth state, deliberately: a `role` claim arrives on a token refresh, and that is when `isAdmin` has to flip.
- **`FirebaseUserInfo`** — exposes the JWT `role` custom claim, which is what the core's `isAdmin` reads; equality covers uid, claims, display name, email and verification so a refresh that changed nothing does not recompose the app.
- **`User.firebaseUserService`** — escape hatch to the underlying service and, through it, FirebaseUI's `FirebaseAuthUI`.

## Building & testing

```sh
./gradlew testDebugUnitTest assembleRelease          # the adapter is the root project
./gradlew :sample:installDebug                        # the sample app, against the Auth emulator at 10.0.2.2:9099
```

Needs `local.properties` → `sdk.dir` (or `ANDROID_HOME`). The core resolves from JitPack; to build against an uncommitted `../user-kit` add `--include-build ../user-kit` (or uncomment the `includeBuild` line in `settings.gradle.kts`). Unit tests are plain JVM; nothing that touches a `FirebaseUser` is unit-tested here, exactly as on iOS.

## License

[MIT](LICENSE) © Max Rozdobudko
