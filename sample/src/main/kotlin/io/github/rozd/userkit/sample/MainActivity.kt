package io.github.rozd.userkit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rozd.userkit.AuthenticationCancelledException
import io.github.rozd.userkit.LocalUser
import io.github.rozd.userkit.firebase.FirebaseAuthHost
import io.github.rozd.userkit.initials
import io.github.rozd.userkit.isAdmin
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val user = (application as SampleApplication).user
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalUser provides user) {
                    SampleScreen()
                    FirebaseAuthHost()
                }
            }
        }
    }
}

@Composable
private fun SampleScreen() {
    val user = LocalUser.current
    val scope = rememberCoroutineScope()
    var outcome by remember { mutableStateOf("—") }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("UserKit sample", style = MaterialTheme.typography.headlineMedium)
            Text("authenticated: ${user.isAuthenticated}")
            Text("admin: ${user.isAdmin}")
            Text("id: ${user.info?.id ?: "—"}")
            Text("name: ${user.info?.profile?.displayName ?: "—"} (${user.info?.profile?.initials ?: ""})")
            Text("role: ${user.info?.role ?: "—"}")
            Text("guarded outcome: $outcome")

            Button(onClick = { user.authenticate() }) { Text("Sign in") }
            Button(onClick = {
                scope.launch {
                    outcome = try {
                        user.withAuthentication { "ran as ${user.info?.id}" }
                    } catch (e: AuthenticationCancelledException) {
                        "cancelled"
                    }
                }
            }) { Text("Guarded action") }
            Button(onClick = { scope.launch { user.signOut() } }) { Text("Sign out") }
        }
    }
}
