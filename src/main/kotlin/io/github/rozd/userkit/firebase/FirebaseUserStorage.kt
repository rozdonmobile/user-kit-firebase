package io.github.rozd.userkit.firebase

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import io.github.rozd.userkit.UserInfo
import io.github.rozd.userkit.UserStorage
import kotlinx.coroutines.tasks.await

/**
 * Firebase owns session persistence, so [store] and [clear] are deliberate no-ops.
 * [fetch] seeds the app with the restored user, forcing a token refresh first so
 * the claims — and with them `isAdmin` — are current rather than cached.
 */
class FirebaseUserStorage(
    private val auth: FirebaseAuth = Firebase.auth,
) : UserStorage {

    override suspend fun fetch(): UserInfo? {
        val user = auth.currentUser ?: return null
        runCatching { user.getIdToken(true).await() }
        return user.toUserInfo()
    }

    override suspend fun store(info: UserInfo) {
        // No-op: Firebase persists the session itself.
    }

    override suspend fun clear() {
        // No-op: sign-out through FirebaseAuth clears it.
    }
}
