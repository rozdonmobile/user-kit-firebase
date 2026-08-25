package io.github.rozd.userkit.firebase

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import io.github.rozd.userkit.UserInfo
import io.github.rozd.userkit.UserSynchronizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

/**
 * Bridges Firebase's listener into a cold, cancel-safe [Flow].
 *
 * It listens to the **ID token**, not just the auth state: a custom claim such
 * as `role` reaches the client on a token refresh, and that is exactly the moment
 * `isAdmin` has to flip. An `AuthStateListener` would only ever report sign-in
 * and sign-out. Claims are decoded in the `map` stage rather than inside the
 * callback so emissions stay in order.
 */
class FirebaseUserSynchronizer(
    private val auth: FirebaseAuth = Firebase.auth,
) : UserSynchronizer {

    override fun install(): Flow<UserInfo?> = auth.currentUsers().map { it?.toUserInfo() }

    override fun dispose() {
        // No-op: `awaitClose` removes the listener when collection stops.
    }
}

internal fun FirebaseAuth.currentUsers(): Flow<FirebaseUser?> = callbackFlow {
    // An explicit object rather than a SAM lambda: the SDK annotates the listener's
    // parameter with a checker-framework type that Kotlin cannot see, and the
    // lambda's inferred type trips over it.
    val listener = object : FirebaseAuth.IdTokenListener {
        override fun onIdTokenChanged(auth: FirebaseAuth) {
            trySend(auth.currentUser)
        }
    }
    addIdTokenListener(listener)
    awaitClose { removeIdTokenListener(listener) }
}
