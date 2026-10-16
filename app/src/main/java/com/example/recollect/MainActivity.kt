
package com.example.recollect

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.recollect.ui.screens.AuthScreen
import com.example.recollect.ui.screens.SaveScreen
import com.example.recollect.viewmodel.AuthViewModel
import com.example.recollect.viewmodel.SaveViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val saveViewModel: SaveViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    // Holds text received from Android's Share menu.
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedText = getSharedText(intent)

        setContent {
            MaterialTheme {
                Surface {
                    var signedIn by mutableStateOf(
                        FirebaseAuth.getInstance().currentUser != null
                    )
                    var authMessage by mutableStateOf("")

                    if (signedIn) {
                        SaveScreen(
                            viewModel = saveViewModel,
                            onSignOut = {
                                authViewModel.signOut()
                                signedIn = false
                            },
                            sharedText = sharedText,
                            onSharedTextHandled = {
                                sharedText = null
                            }
                        )
                    } else {
                        AuthScreen(
                            onSignIn = { email, password ->
                                authViewModel.signIn(email, password) { error ->
                                    if (error == null) {
                                        signedIn = true
                                        authMessage = ""
                                    } else {
                                        authMessage = error
                                    }
                                }
                            },
                            onSignUp = { email, password ->
                                authViewModel.signUp(email, password) { error ->
                                    if (error == null) {
                                        signedIn = true
                                        authMessage = ""
                                    } else {
                                        authMessage = error
                                    }
                                }
                            }
                        )
                    }

                    if (authMessage.isNotBlank()) {
                        AlertDialog(
                            onDismissRequest = {
                                authMessage = ""
                            },
                            title = {
                                Text("Authentication")
                            },
                            text = {
                                Text(authMessage)
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        authMessage = ""
                                    }
                                ) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = getSharedText(intent)
    }

    private fun getSharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) {
            return null
        }

        if (intent.type != "text/plain") {
            return null
        }

        return intent
            .getStringExtra(Intent.EXTRA_TEXT)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}
