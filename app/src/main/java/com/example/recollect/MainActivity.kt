package com.example.recollect

import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.recollect.ui.screens.AuthScreen
import com.example.recollect.ui.screens.SaveScreen
import com.example.recollect.viewmodel.AuthViewModel
import com.example.recollect.viewmodel.SaveViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val saveViewModel: SaveViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private var sharedText by mutableStateOf<String?>(null)
    private var sharedImageUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleSharedContent(intent)

        setContent {
            MaterialTheme {
                Surface {
                    var signedIn by remember {
                        mutableStateOf(
                            FirebaseAuth.getInstance().currentUser != null
                        )
                    }

                    var authMessage by remember {
                        mutableStateOf("")
                    }

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
                            },
                            sharedImageUri = sharedImageUri,
                            onSharedImageHandled = {
                                sharedImageUri = null
                            }
                        )
                    } else {
                        AuthScreen(
                            onSignIn = { email, password ->
                                authViewModel.signIn(
                                    email,
                                    password
                                ) { error ->
                                    if (error == null) {
                                        signedIn = true
                                        authMessage = ""
                                    } else {
                                        authMessage = error
                                    }
                                }
                            },
                            onSignUp = { email, password ->
                                authViewModel.signUp(
                                    email,
                                    password
                                ) { error ->
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
        handleSharedContent(intent)
    }

    private fun handleSharedContent(intent: Intent?) {
        // Clear previous shared data before handling a new share.
        sharedText = null
        sharedImageUri = null

        if (intent?.action != Intent.ACTION_SEND) {
            return
        }

        val mimeType = intent.type ?: return

        when {
            mimeType == "text/plain" -> {
                sharedText = intent
                    .getStringExtra(Intent.EXTRA_TEXT)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            }

            mimeType.startsWith("image/") -> {
                sharedImageUri = try {
                    intent.getParcelableExtra(
                        Intent.EXTRA_STREAM,
                        Uri::class.java
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}