package io.github.rozd.userkit.firebase

import com.google.firebase.auth.FirebaseUser
import io.github.rozd.userkit.UserId
import io.github.rozd.userkit.UserInfo
import io.github.rozd.userkit.UserProfile
import io.github.rozd.userkit.UserSession
import kotlinx.coroutines.tasks.await

/**
 * [UserInfo] over a [FirebaseUser] plus the claims of its ID token.
 *
 * The identity-bearing fields are copied out at construction so two snapshots
 * of the same session compare equal: `User.infos` is a `StateFlow`, and every
 * token refresh re-emits — equality is what keeps a refresh that changed nothing
 * from recomposing the app.
 */
class FirebaseUserInfo internal constructor(
    /** The SDK user behind this snapshot; reach for it when you need Firebase itself. */
    val user: FirebaseUser,
    /** The ID token's custom claims, as decoded by the SDK. */
    val claims: Map<String, Any?>,
) : UserInfo {

    override val id: UserId = UserId(user.uid)

    /** The Firebase JWT `role` custom claim — what `UserInfo.isAdmin` reads. */
    override val role: String? = claims["role"] as? String

    override val session: UserSession = FirebaseUserSession(user)

    override val profile: UserProfile = FirebaseUserProfile(user)

    private val identity = listOf(user.uid, user.displayName, user.email, user.isEmailVerified, claims)

    override fun equals(other: Any?): Boolean = other is FirebaseUserInfo && other.identity == identity
    override fun hashCode(): Int = identity.hashCode()
    override fun toString(): String = "FirebaseUserInfo(id=$id, role=$role)"
}

class FirebaseUserProfile internal constructor(private val user: FirebaseUser) : UserProfile {
    override val displayName: String? = user.displayName
    val email: String? = user.email
    val isEmailVerified: Boolean = user.isEmailVerified
}

class FirebaseUserSession internal constructor(private val user: FirebaseUser) : UserSession {

    /** A [FirebaseUser] exists only while a session does. */
    override val isAuthenticated: Boolean = true

    /** The Android SDK does not expose the refresh token; it manages refresh itself. */
    override val refreshToken: String? = null

    /** The ID token, refreshed by the SDK when within five minutes of expiry. */
    override suspend fun accessToken(): String? = runCatching { user.getIdToken(false).await().token }.getOrNull()
}

/** Decodes the claims — `[:]` when the token cannot be fetched, as on iOS — and wraps the user. */
internal suspend fun FirebaseUser.toUserInfo(): FirebaseUserInfo {
    val claims = runCatching { getIdToken(false).await().claims }.getOrDefault(emptyMap())
    return FirebaseUserInfo(this, claims)
}
