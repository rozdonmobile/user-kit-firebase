package io.github.rozd.userkit.firebase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseUserServiceConfigurationTest {

    @Test fun `defaults match the iOS adapter`() {
        val config = FirebaseUserServiceConfiguration(
            authDomain = "example.firebaseapp.com",
            packageName = "com.example.app",
        )
        assertEquals("example.firebaseapp.com", config.authDomain)
        assertEquals("com.example.app", config.packageName)
        assertNull(config.tosUrl)
        assertNull(config.privacyPolicyUrl)
        assertNull(config.googleServerClientId)
        assertTrue(config.shouldAutoUpgradeAnonymousUsers)
        assertTrue(config.mfaEnabled)
        assertFalse(config.emailLinkSignInEnabled)
    }

    @Test fun `explicit values round-trip`() {
        val config = FirebaseUserServiceConfiguration(
            authDomain = "d",
            packageName = "p",
            tosUrl = "https://example.com/tos",
            privacyPolicyUrl = "https://example.com/privacy",
            googleServerClientId = "client-id",
            shouldAutoUpgradeAnonymousUsers = false,
            mfaEnabled = false,
            emailLinkSignInEnabled = true,
        )
        assertEquals("https://example.com/tos", config.tosUrl)
        assertEquals("https://example.com/privacy", config.privacyPolicyUrl)
        assertEquals("client-id", config.googleServerClientId)
        assertFalse(config.shouldAutoUpgradeAnonymousUsers)
        assertFalse(config.mfaEnabled)
        assertTrue(config.emailLinkSignInEnabled)
    }
}
