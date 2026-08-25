package io.github.rozd.userkit.firebase

/**
 * App-supplied configuration for [FirebaseUserService]. The library cannot see the
 * host app's environment, so the auth domain, package name, legal URLs and the
 * Google OAuth client are injected here rather than read from a global.
 *
 * @property authDomain The Firebase Hosting / auth domain that email links and
 *   password-reset links point back at, e.g. `"myapp.firebaseapp.com"`.
 * @property packageName The Android application id the links open, usually
 *   `BuildConfig.APPLICATION_ID` or `context.packageName`.
 * @property googleServerClientId The **web** OAuth client id of the Firebase
 *   project (`default_web_client_id` in `google-services.json`). `null` leaves the
 *   Google button out — exactly as the iOS app skips `withGoogleSignIn()` when the
 *   plist has no `CLIENT_ID` — rather than shipping a button that always fails.
 * @property emailLinkSignInEnabled Passwordless email-link sign-in, in addition
 *   to email + password. Off by default: it needs the link domain configured in
 *   the Firebase console before it works.
 */
data class FirebaseUserServiceConfiguration(
    val authDomain: String,
    val packageName: String,
    val tosUrl: String? = null,
    val privacyPolicyUrl: String? = null,
    val googleServerClientId: String? = null,
    val shouldAutoUpgradeAnonymousUsers: Boolean = true,
    val mfaEnabled: Boolean = true,
    val emailLinkSignInEnabled: Boolean = false,
)
