package io.github.rozd.userkit.firebase

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.rozd.userkit.LocalUser
import io.github.rozd.userkit.User

/**
 * Presents FirebaseUI's sign-in flow in a modal bottom sheet whenever the
 * service asks to be shown — the counterpart of the iOS app's `.authHost()`
 * modifier. Apply once, at the app's root, above everything that may call
 * `user.authenticate()` or `user.withAuthentication { … }`.
 *
 * ```
 * CompositionLocalProvider(LocalUser provides user) {
 *     AppRoot()
 *     FirebaseAuthHost()
 * }
 * ```
 *
 * A successful sign-in closes the sheet and returns the member to exactly where
 * they started; a swipe-down cancels, failing any pending `withAuthentication`.
 * For a different container (a full-screen dialog, a navigation destination),
 * host [FirebaseSignInScreen] yourself, keyed on [FirebaseUserService.isPresented].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseAuthHost(
    user: User = LocalUser.current,
    modifier: Modifier = Modifier,
) {
    val service = user.firebaseUserService
    if (!service.isPresented) return

    ModalBottomSheet(
        onDismissRequest = { service.finishSignIn(authenticated = false) },
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        FirebaseSignInScreen(service)
    }
}

/**
 * FirebaseUI's sign-in flow wired to [service]: success and cancellation both
 * end in [FirebaseUserService.finishSignIn]. Failures stay on screen — FirebaseUI
 * renders them inline and lets the member try again.
 */
@Composable
fun FirebaseSignInScreen(
    service: FirebaseUserService,
    modifier: Modifier = Modifier,
) {
    com.firebase.ui.auth.ui.screens.FirebaseAuthScreen(
        configuration = service.authConfiguration,
        onSignInSuccess = { service.finishSignIn(authenticated = true) },
        onSignInFailure = { /* shown inline by FirebaseUI; the flow stays up */ },
        onSignInCancelled = { service.finishSignIn(authenticated = false) },
        modifier = modifier,
        authUI = service.authUI,
    )
}
