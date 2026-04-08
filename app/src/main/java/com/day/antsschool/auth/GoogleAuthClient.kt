package com.day.antsschool.auth

import android.content.Context
import android.content.Intent
import com.day.antsschool.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GoogleAuthClient(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val googleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // google-services.json 추가 시 자동 생성되는 리소스
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    )

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun getSignedInUser(): FirebaseUser? = firebaseAuth.currentUser

    // Google 로그인 결과를 Firebase 인증으로 연결
    suspend fun handleSignInResult(data: Intent?): FirebaseUser? = suspendCoroutine { cont ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener { result -> cont.resume(result.user) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        } catch (e: ApiException) {
            cont.resumeWithException(e)
        }
    }

    suspend fun signOut(): Unit = suspendCoroutine { cont ->
        firebaseAuth.signOut()
        googleSignInClient.signOut().addOnCompleteListener { cont.resume(Unit) }
    }
}
