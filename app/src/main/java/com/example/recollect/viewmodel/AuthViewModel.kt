package com.example.recollect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    fun signUp(email: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email.trim(), password).await()
                onResult(null)
            } catch (e: Exception) {
                onResult(e.localizedMessage ?: "Could not create account")
            }
        }
    }

    fun signIn(email: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                onResult(null)
            } catch (e: Exception) {
                onResult(e.localizedMessage ?: "Could not sign in")
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
