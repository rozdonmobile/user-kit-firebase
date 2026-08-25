package io.github.rozd.userkit.firebase

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.google.firebase.Firebase
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import io.github.rozd.userkit.AuthenticationCancelledException
import io.github.rozd.userkit.User
import io.github.rozd.userkit.UserService
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.tasks.await

/**
 * [UserService] over Firebase Authentication, with FirebaseUI's Compose sign-in
 * flow behind [authenticate].
 *
 * The service owns the *decision* to show sign-in — [isPresented] — and the
 * app owns the *presentation*: drop [FirebaseAuthHost] at the root for a modal
 * sheet, or read [isPresented] and place [FirebaseSignInScreen] in a container
 * of your own. Either way, finish the flow through [finishSignIn]; that is what
 * releases a pending [withAuthentication].
 */
@Stable
class FirebaseUserService(
    context: Context,
    val configuration: FirebaseUserServiceConfiguration,
    private val auth: FirebaseAuth = Firebase.auth,
    val authUI: FirebaseAuthUI = FirebaseAuthUI.getInstance(),
) : UserService {

    private val context: Context = context.applicationContext

    /** The FirebaseUI configuration [FirebaseSignInScreen] renders; built from [configuration]. */
    val authConfiguration: AuthUIConfiguration by lazy { buildAuthConfiguration() }

    // MARK: - Presentation

    /** Whether sign-in UI should be on screen. Snapshot state, so a host recomposes on it. */
    var isPresented: Boolean by mutableStateOf(false)
        private set

    /** Fed by [finishSignIn]; a `replay` of 0 and a buffer of 1 so a fast result is never lost. */
    private val outcomes = MutableSharedFlow<Boolean>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Ask for the sign-in UI. Idempotent while already presented. */
    fun presentSignIn() {
        isPresented = true
    }

    /**
     * Take the sign-in UI down and settle any coroutine waiting in [withAuthentication]:
     * `authenticated = true` lets it run its operation, `false` fails it with
     * [AuthenticationCancelledException].
     */
    fun finishSignIn(authenticated: Boolean) {
        if (!isPresented) return
        isPresented = false
        outcomes.tryEmit(authenticated)
    }

    // MARK: - UserService

    override val isEmailVerified: Flow<Boolean> =
        auth.currentUsers().map { it?.isEmailVerified ?: false }.distinctUntilChanged()

    override suspend fun signIn() = presentSignIn()

    override suspend fun signOut() {
        // FirebaseUI's sign-out also clears Credential Manager state, so the
        // next sign-in does not auto-select the account that just left.
        authUI.signOut(context)
    }

    override suspend fun sendVerificationEmail() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    override fun authenticate() = presentSignIn()

    override fun authenticateIfNeeded(): Boolean {
        if (auth.currentUser != null) return true
        presentSignIn()
        return false
    }

    override suspend fun <T> withAuthentication(operation: suspend () -> T): T {
        if (auth.currentUser != null) return operation()

        // Subscribe before presenting, so an outcome that lands between the two
        // cannot slip past. Sharing one outcome stream also means a second caller
        // while the sheet is up simply waits on the same result.
        val authenticated = outcomes.onSubscription { presentSignIn() }.first()
        if (!authenticated) throw AuthenticationCancelledException()
        return operation()
    }

    // MARK: - FirebaseUI configuration

    private fun buildAuthConfiguration(): AuthUIConfiguration {
        val config = configuration
        val linkSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://${config.authDomain}")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(config.packageName, true, null)
            .setLinkDomain(config.authDomain)
            .build()

        return authUIConfiguration {
            context = this@FirebaseUserService.context
            providers {
                provider(
                    AuthProvider.Email(
                        isEmailLinkSignInEnabled = config.emailLinkSignInEnabled,
                        emailLinkActionCodeSettings = linkSettings,
                        passwordValidationRules = emptyList(),
                    ),
                )
                config.googleServerClientId?.let { clientId ->
                    provider(AuthProvider.Google(scopes = emptyList(), serverClientId = clientId))
                }
            }
            tosUrl = config.tosUrl
            privacyPolicyUrl = config.privacyPolicyUrl
            isMfaEnabled = config.mfaEnabled
            isAnonymousUpgradeEnabled = config.shouldAutoUpgradeAnonymousUsers
            passwordResetActionCodeSettings = linkSettings
        }
    }
}

/**
 * Escape hatch that hands the app the underlying [FirebaseUserService] — for
 * hosting the sign-in UI, or for reaching FirebaseUI's [FirebaseAuthUI] directly.
 */
val User.firebaseUserService: FirebaseUserService
    get() = service as? FirebaseUserService
        ?: error("User.service is ${service::class.simpleName}, not FirebaseUserService")
