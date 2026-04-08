package com.day.antsschool.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(FirebaseAuth.getInstance().currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Firebase 인증 상태 변화 자동 감지 (앱 재시작 후 로그인 유지 등)
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        _currentUser.value = auth.currentUser
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    fun onSignInResult(client: GoogleAuthClient, data: Intent?) {
        viewModelScope.launch {
            try {
                client.handleSignInResult(data)
                // authStateListener가 _currentUser 자동 갱신
            } catch (e: Exception) {
                _errorMessage.value = "로그인 실패: ${e.message}"
            }
        }
    }

    fun signOut(client: GoogleAuthClient) {
        viewModelScope.launch {
            client.signOut()
        }
    }

    override fun onCleared() {
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }
}
