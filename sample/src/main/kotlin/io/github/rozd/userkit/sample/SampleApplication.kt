package io.github.rozd.userkit.sample

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.auth
import io.github.rozd.userkit.User
import io.github.rozd.userkit.firebase.FirebaseUserService
import io.github.rozd.userkit.firebase.FirebaseUserServiceConfiguration
import io.github.rozd.userkit.firebase.FirebaseUserStorage
import io.github.rozd.userkit.firebase.FirebaseUserSynchronizer

class SampleApplication : Application() {

    /** The app-wide user — the counterpart of the iOS app's `User.current`. */
    lateinit var user: User
        private set

    override fun onCreate() {
        super.onCreate()

        // A `demo-` project id is emulator-only by convention: the Auth emulator
        // serves it without a real project behind it, and nothing here can reach
        // production. Real apps use google-services.json and skip this block.
        FirebaseApp.initializeApp(
            this,
            FirebaseOptions.Builder()
                .setProjectId("demo-userkit")
                .setApplicationId("1:000000000000:android:0000000000000000")
                .setApiKey("demo-userkit-api-key")
                .build(),
        )
        Firebase.auth.useEmulator(BuildConfig.AUTH_EMULATOR_HOST, BuildConfig.AUTH_EMULATOR_PORT)

        user = User(
            service = FirebaseUserService(
                context = this,
                configuration = FirebaseUserServiceConfiguration(
                    authDomain = "demo-userkit.firebaseapp.com",
                    packageName = packageName,
                    tosUrl = "https://example.com/terms",
                    privacyPolicyUrl = "https://example.com/privacy",
                    mfaEnabled = false,
                ),
            ),
            storage = FirebaseUserStorage(),
            synchronizer = FirebaseUserSynchronizer(),
        )
    }
}
